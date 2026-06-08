package com.example.hearthflow.data.firebase

import android.content.Context
import com.example.hearthflow.data.model.SplitExpenseEntry
import com.example.hearthflow.data.model.UserAccount
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object FirebaseSyncManager {
    var isInitialized = false
        private set

    lateinit var auth: FirebaseAuth
    lateinit var database: FirebaseDatabase

    // True when the app is using the real Firebase project (google-services.json credentials).
    // False means we fell back to the offline sandbox and no cloud writes should happen.
    var isSandboxMode = true
        private set

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            // The google-services plugin pre-initializes the default FirebaseApp from
            // google-services.json at app startup via FirebaseInitProvider.
            // We grab the already-initialized default app here.
            val defaultApp = try {
                FirebaseApp.getInstance()
            } catch (e: Exception) {
                // Default app not yet initialized — trigger it manually
                FirebaseApp.initializeApp(context)
            }

            if (defaultApp != null) {
                auth = FirebaseAuth.getInstance()
                // Explicitly use the database URL from google-services.json
                database = FirebaseDatabase.getInstance(
                    "https://routinelog-506b7-default-rtdb.firebaseio.com"
                )
                isInitialized = true
                isSandboxMode = false
                android.util.Log.i("FirebaseSyncManager", "✅ Connected to real Firebase: routinelog-506b7")
            } else {
                throw IllegalStateException("FirebaseApp could not be initialized")
            }
        } catch (e: Exception) {
            android.util.Log.w("FirebaseSyncManager", "⚠️ Real Firebase init failed: ${e.message}. Falling back to local sandbox.")
            try {
                // Sandbox fallback — uses a named app so we can detect it via app.name == "HearthFlowCloud"
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:561344069110:android:9b1f29b2ee5a4062bcdbd7")
                    .setApiKey("AIzaSyA57InMceEuOkZuX_qobJEuPv8-dYBsJfo")
                    .setDatabaseUrl("https://routinelog-506b7-default-rtdb.firebaseio.com")
                    .setProjectId("routinelog-506b7")
                    .build()
                val app = try {
                    FirebaseApp.initializeApp(context, options, "HearthFlowCloud")
                } catch (ex: Exception) {
                    FirebaseApp.getInstance("HearthFlowCloud")
                }
                auth = FirebaseAuth.getInstance(app)
                database = FirebaseDatabase.getInstance(app)
                isInitialized = true
                isSandboxMode = true
                android.util.Log.w("FirebaseSyncManager", "⚠️ Running in sandbox fallback mode")
            } catch (ex: Exception) {
                android.util.Log.e("FirebaseSyncManager", "❌ All Firebase initialization failed: ${ex.message}")
                isInitialized = false
                isSandboxMode = true
            }
        }
    }

    // Sanitize keys because Firebase Database keys cannot contain ".", "#", "$", "[", or "]"
    fun sanitizeKey(key: String): String {
        return key.replace(".", "_dot_")
                  .replace("@", "_at_")
                  .replace("#", "_hash_")
                  .replace("$", "_dollar_")
                  .replace("[", "_lbracket_")
                  .replace("]", "_rbracket_")
    }

    private fun hashPassword(password: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // --- Authentication ---
    fun registerUserCloud(email: String, name: String, mobileNumber: String, password: String = "", onResult: (Boolean, String?) -> Unit) {
        if (!isInitialized) {
            onResult(false, "Firebase is not initialized")
            return
        }
        val lowercaseEmail = email.lowercase().trim()
        val sanitizedEmail = sanitizeKey(lowercaseEmail)
        val profileRef = database.reference.child("users").child(sanitizedEmail).child("profile")
        // Admin status is determined solely by email, never by display name.
        val isAdmin = lowercaseEmail == "arvaancorelogic@gmail.com"
        val profileMap = mapOf(
            "name" to name,
            "email" to lowercaseEmail,
            "mobileNumber" to mobileNumber,
            "isPro" to isAdmin,
            "proExpiryTimestamp" to (if (isAdmin) 4102444800000L else 0L),
            "subscriptionPlan" to (if (isAdmin) "Lifetime Pro Plan" else "Free Plan"),
            "aiCredits" to (if (isAdmin) 999 else 10),
            "isAdmin" to isAdmin,
            "passwordHash" to hashPassword(password)
        )
        profileRef.setValue(profileMap)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

    fun syncUserProfile(account: UserAccount) {
        if (!isInitialized) return
        val sanitizedEmail = sanitizeKey(account.email)
        val profileRef = database.reference.child("users").child(sanitizedEmail).child("profile")
        // Admin status is determined solely by email, never by display name.
        val emailCheck = account.email.lowercase().trim()
        val isAdmin = emailCheck == "arvaancorelogic@gmail.com"
        val profileMap = mapOf<String, Any>(
            "name" to account.name,
            "email" to account.email,
            "mobileNumber" to account.mobileNumber,
            "isPro" to (account.isPro || isAdmin),
            "proExpiryTimestamp" to (if (isAdmin) 4102444800000L else account.proExpiryTimestamp),
            "subscriptionPlan" to (if (isAdmin) "Lifetime Pro Plan" else (account.subscriptionPlan ?: "Free Plan")),
            "aiCredits" to (if (isAdmin) 999 else account.aiCredits),
            "isAdmin" to isAdmin
        )
        profileRef.updateChildren(profileMap)
    }

    fun verifyLoginCloud(email: String, passwordEntered: String, onResult: (Boolean, String?) -> Unit) {
        if (!isInitialized) {
            onResult(false, "Firebase is not initialized")
            return
        }
        val sanitizedEmail = sanitizeKey(email)
        database.reference.child("users").child(sanitizedEmail).child("profile").get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    onResult(false, "Account not found")
                    return@addOnSuccessListener
                }
                
                val storedHash = snapshot.child("passwordHash").getValue(String::class.java)
                if (storedHash == null || storedHash.isEmpty()) {
                    // Dynamically initialize the password hash with their entered password
                    val enteredHash = hashPassword(passwordEntered)
                    database.reference.child("users").child(sanitizedEmail).child("profile")
                        .child("passwordHash").setValue(enteredHash)
                    onResult(true, null)
                } else {
                    val enteredHash = hashPassword(passwordEntered)
                    if (storedHash == enteredHash) {
                        onResult(true, null)
                    } else {
                        onResult(false, "Incorrect password")
                    }
                }
            }
            .addOnFailureListener { e ->
                onResult(false, e.message ?: "Failed to verify login")
            }
    }

    fun fetchUserProfile(email: String, onResult: (UserAccount?) -> Unit) {
        if (!isInitialized) {
            onResult(null)
            return
        }
        val sanitizedEmail = sanitizeKey(email)
        val profileRef = database.reference.child("users").child(sanitizedEmail).child("profile")
        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").value as? String ?: "User"
                    val isPro = snapshot.child("isPro").value as? Boolean ?: false
                    val proExpiryTimestamp = (snapshot.child("proExpiryTimestamp").value as? Number)?.toLong() ?: 0L
                    val subscriptionPlan = snapshot.child("subscriptionPlan").value as? String ?: "Free Plan"
                    val aiCredits = (snapshot.child("aiCredits").value as? Number)?.toInt() ?: 10
                    val mobileNumber = snapshot.child("mobileNumber").value as? String ?: ""
                    
                    val account = UserAccount(
                        userId = "local_user",
                        name = name,
                        email = email,
                        mobileNumber = mobileNumber,
                        isPro = isPro,
                        proExpiryTimestamp = proExpiryTimestamp,
                        subscriptionPlan = subscriptionPlan,
                        aiCredits = aiCredits
                    )
                    onResult(account)
                } else {
                    onResult(null)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                onResult(null)
            }
        })
    }

    fun checkIfAdminExists(onResult: (Boolean) -> Unit) {
        if (!isInitialized) {
            onResult(false)
            return
        }
        val usersRef = database.reference.child("users")
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var adminFound = false
                snapshot.children.forEach { userSnap ->
                    val profileSnap = userSnap.child("profile")
                    if (profileSnap.exists()) {
                        // Admin status is determined by the isAdmin flag or email only, NOT by name.
                        val isAdminFlag = profileSnap.child("isAdmin").value as? Boolean ?: false
                        val email = profileSnap.child("email").value as? String ?: ""
                        val emailCheck = email.lowercase().trim()
                        if (isAdminFlag ||
                            emailCheck == "arvaancorelogic@gmail.com") {
                            adminFound = true
                        }
                    }
                }
                onResult(adminFound)
            }
            override fun onCancelled(error: DatabaseError) {
                onResult(false)
            }
        })
    }

    // --- App Invitations ---
    fun sendAppInvitation(inviteeEmail: String, senderName: String) {
        if (!isInitialized) return
        val inviteRef = database.reference.child("app_invitations").push()
        inviteRef.setValue(mapOf(
            "inviteeEmail" to inviteeEmail,
            "senderName" to senderName,
            "timestamp" to System.currentTimeMillis()
        ))
    }

    // --- Shared Household Groups ---
    fun createGroupOnCloud(groupName: String, creatorEmail: String, members: List<String>) {
        if (!isInitialized) return
        val sanitizedGroup = sanitizeKey(groupName)
        val groupRef = database.reference.child("groups").child(sanitizedGroup)

        val membersMap = mutableMapOf<String, Boolean>()
        membersMap[sanitizeKey(creatorEmail)] = true
        members.forEach { email ->
            membersMap[sanitizeKey(email)] = true
        }

        groupRef.child("info").setValue(mapOf(
            "name" to groupName,
            "creator" to creatorEmail
        ))
        groupRef.child("members").setValue(membersMap)

        // Add index on user profiles
        membersMap.keys.forEach { memberKey ->
            database.reference.child("users").child(memberKey).child("joinedGroups").child(sanitizedGroup).setValue(true)
        }
    }

    // --- Real-Time Sync & Sharing ---
    fun pushSplitExpense(expense: SplitExpenseEntry) {
        if (!isInitialized) return
        val sanitizedGroup = sanitizeKey(expense.groupName)
        val splitRef = database.reference.child("groups").child(sanitizedGroup).child("splits").child(expense.id.toString())
        
        val splitMap = mapOf(
            "id" to expense.id,
            "title" to expense.title,
            "totalAmount" to expense.totalAmount,
            "paidBy" to expense.paidBy,
            "splitWith" to expense.splitWith,
            "splitShare" to expense.splitShare,
            "isSettled" to expense.isSettled,
            "timestamp" to expense.timestamp,
            "groupName" to expense.groupName
        )
        splitRef.setValue(splitMap)
    }

    fun settleSplitExpenseOnCloud(id: Long, groupName: String) {
        if (!isInitialized) return
        val sanitizedGroup = sanitizeKey(groupName)
        database.reference.child("groups").child(sanitizedGroup).child("splits").child(id.toString()).child("isSettled").setValue(true)
    }

    fun deleteSplitExpenseOnCloud(id: Long, groupName: String) {
        if (!isInitialized) return
        val sanitizedGroup = sanitizeKey(groupName)
        database.reference.child("groups").child(sanitizedGroup).child("splits").child(id.toString()).removeValue()
    }

    // Observe all groups containing the active user in real-time
    fun observeRealtimeSplits(email: String, onSplitsUpdated: (List<SplitExpenseEntry>) -> Unit) {
        if (!isInitialized) return
        val sanitizedEmail = sanitizeKey(email)
        val userGroupsRef = database.reference.child("users").child(sanitizedEmail).child("joinedGroups")
        
        userGroupsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(joinedSnapshot: DataSnapshot) {
                val groupKeys = joinedSnapshot.children.mapNotNull { it.key }
                if (groupKeys.isEmpty()) {
                    onSplitsUpdated(emptyList())
                    return
                }

                // Map of groupKey -> list of splits
                val groupSplitsMap = mutableMapOf<String, List<SplitExpenseEntry>>()
                var pendingCallbacks = groupKeys.size

                groupKeys.forEach { groupKey ->
                    val splitsRef = database.reference.child("groups").child(groupKey).child("splits")
                    splitsRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(splitsSnapshot: DataSnapshot) {
                            val splitsList = mutableListOf<SplitExpenseEntry>()
                            splitsSnapshot.children.forEach { snap ->
                                val id = (snap.child("id").value as? Number)?.toLong() ?: 0L
                                val title = snap.child("title").value as? String ?: ""
                                val totalAmount = (snap.child("totalAmount").value as? Number)?.toDouble() ?: 0.0
                                val paidBy = snap.child("paidBy").value as? String ?: ""
                                val splitWith = snap.child("splitWith").value as? String ?: ""
                                val splitShare = (snap.child("splitShare").value as? Number)?.toDouble() ?: 0.0
                                val isSettled = snap.child("isSettled").value as? Boolean ?: false
                                val timestamp = (snap.child("timestamp").value as? Number)?.toLong() ?: 0L
                                val groupName = snap.child("groupName").value as? String ?: ""

                                val split = SplitExpenseEntry(
                                    id = id,
                                    title = title,
                                    totalAmount = totalAmount,
                                    paidBy = paidBy,
                                    splitWith = splitWith,
                                    splitShare = splitShare,
                                    isSettled = isSettled,
                                    timestamp = timestamp,
                                    groupName = groupName
                                )
                                splitsList.add(split)
                            }
                            groupSplitsMap[groupKey] = splitsList
                            
                            val activeGroupKeys = joinedSnapshot.children.mapNotNull { it.key }.toSet()
                            val allSplits = groupSplitsMap.filterKeys { activeGroupKeys.contains(it) }.values.flatten()
                            onSplitsUpdated(allSplits)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            pendingCallbacks--
                            if (pendingCallbacks == 0) {
                                val activeGroupKeys = joinedSnapshot.children.mapNotNull { it.key }.toSet()
                                val allSplits = groupSplitsMap.filterKeys { activeGroupKeys.contains(it) }.values.flatten()
                                onSplitsUpdated(allSplits)
                            }
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun updateGroupNameOnCloud(oldGroupKey: String, newGroupKey: String, newDisplayName: String, members: List<String>) {
        if (!isInitialized) return
        val sanitizedOld = sanitizeKey(oldGroupKey)
        val sanitizedNew = sanitizeKey(newGroupKey)

        database.reference.child("groups").child(sanitizedOld).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    database.reference.child("groups").child(sanitizedNew).setValue(snapshot.value)
                    database.reference.child("groups").child(sanitizedNew).child("info").child("name").setValue(newDisplayName)
                    database.reference.child("groups").child(sanitizedOld).removeValue()

                    members.forEach { email ->
                        val memberKey = sanitizeKey(email)
                        database.reference.child("users").child(memberKey).child("joinedGroups").child(sanitizedOld).removeValue()
                        database.reference.child("users").child(memberKey).child("joinedGroups").child(sanitizedNew).setValue(true)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun addMemberToGroupOnCloud(groupName: String, memberEmail: String) {
        if (!isInitialized) return
        val sanitizedGroup = sanitizeKey(groupName)
        val sanitizedEmail = sanitizeKey(memberEmail)

        database.reference.child("groups").child(sanitizedGroup).child("members").child(sanitizedEmail).setValue(true)
        database.reference.child("users").child(sanitizedEmail).child("joinedGroups").child(sanitizedGroup).setValue(true)
    }

    fun removeMemberFromGroupOnCloud(groupName: String, memberEmail: String) {
        if (!isInitialized) return
        val sanitizedGroup = sanitizeKey(groupName)
        val sanitizedEmail = sanitizeKey(memberEmail)

        database.reference.child("groups").child(sanitizedGroup).child("members").child(sanitizedEmail).removeValue()
        database.reference.child("users").child(sanitizedEmail).child("joinedGroups").child(sanitizedGroup).removeValue()
    }

    fun deleteGroupFromCloud(groupName: String, members: List<String>) {
        if (!isInitialized) return
        val sanitizedGroup = sanitizeKey(groupName)

        database.reference.child("groups").child(sanitizedGroup).removeValue()

        members.forEach { email ->
            val memberKey = sanitizeKey(email)
            database.reference.child("users").child(memberKey).child("joinedGroups").child(sanitizedGroup).removeValue()
        }
    }

    // --- App Settings (Dynamic Legals & Pricing) ---
    fun observeAppSettings(onSettingsChanged: (Map<String, Any>) -> Unit) {
        if (!isInitialized) return
        val settingsRef = database.reference.child("app_settings")
        settingsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, Any>()
                snapshot.children.forEach { snap ->
                    snap.key?.let { k ->
                        snap.value?.let { v ->
                            map[k] = v
                        }
                    }
                }
                onSettingsChanged(map)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun updateAppSettings(key: String, value: Any) {
        if (!isInitialized) return
        database.reference.child("app_settings").child(key).setValue(value)
    }

    // --- Support Tickets Center ---
    fun submitSupportTicket(ticketId: String, email: String, category: String, message: String) {
        if (!isInitialized) return
        val ticketRef = database.reference.child("support_tickets").child(ticketId)
        val ticketMap = mapOf(
            "ticketId" to ticketId,
            "email" to email,
            "category" to category,
            "message" to message,
            "status" to "Received",
            "timestamp" to System.currentTimeMillis()
        )
        ticketRef.setValue(ticketMap)
    }

    fun observeSupportTickets(onTicketsChanged: (List<Map<String, Any>>) -> Unit) {
        if (!isInitialized) return
        val ticketsRef = database.reference.child("support_tickets")
        ticketsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Map<String, Any>>()
                snapshot.children.forEach { snap ->
                    val map = mutableMapOf<String, Any>()
                    snap.children.forEach { child ->
                        child.key?.let { k ->
                            child.value?.let { v ->
                                map[k] = v
                            }
                        }
                    }
                    if (map.isNotEmpty()) {
                        list.add(map)
                    }
                }
                list.sortByDescending { (it["timestamp"] as? Long) ?: 0L }
                onTicketsChanged(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun resolveSupportTicket(ticketId: String) {
        if (!isInitialized) return
        database.reference.child("support_tickets").child(ticketId).child("status").setValue("Resolved")
    }

    fun desanitizeKey(key: String): String {
        return key.replace("_dot_", ".")
                  .replace("_at_", "@")
                  .replace("_hash_", "#")
                  .replace("_dollar_", "$")
                  .replace("_lbracket_", "[")
                  .replace("_rbracket_", "]")
    }

    fun observeGroupMembers(email: String, onGroupsUpdated: (Map<String, List<String>>) -> Unit) {
        if (!isInitialized) return
        val sanitizedEmail = sanitizeKey(email)
        val userGroupsRef = database.reference.child("users").child(sanitizedEmail).child("joinedGroups")
        
        userGroupsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(joinedSnapshot: DataSnapshot) {
                val groupKeys = joinedSnapshot.children.mapNotNull { it.key }
                if (groupKeys.isEmpty()) {
                    onGroupsUpdated(emptyMap())
                    return
                }

                val allGroupMembers = mutableMapOf<String, List<String>>()
                var pendingCallbacks = groupKeys.size

                groupKeys.forEach { groupKey ->
                    val membersRef = database.reference.child("groups").child(groupKey).child("members")
                    membersRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(membersSnapshot: DataSnapshot) {
                            val membersList = mutableListOf<String>()
                            membersSnapshot.children.forEach { snap ->
                                val memberEmailSanitized = snap.key
                                if (memberEmailSanitized != null) {
                                    val memberEmail = desanitizeKey(memberEmailSanitized)
                                    if (memberEmail.lowercase().trim() != email.lowercase().trim()) {
                                        membersList.add(memberEmail)
                                    }
                                }
                            }
                            allGroupMembers[groupKey] = membersList
                            
                            val activeGroupKeys = joinedSnapshot.children.mapNotNull { it.key }.toSet()
                            val filteredMembers = allGroupMembers.filterKeys { activeGroupKeys.contains(it) }
                            onGroupsUpdated(filteredMembers)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            pendingCallbacks--
                            if (pendingCallbacks == 0) {
                                val activeGroupKeys = joinedSnapshot.children.mapNotNull { it.key }.toSet()
                                val filteredMembers = allGroupMembers.filterKeys { activeGroupKeys.contains(it) }
                                onGroupsUpdated(filteredMembers)
                            }
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}

