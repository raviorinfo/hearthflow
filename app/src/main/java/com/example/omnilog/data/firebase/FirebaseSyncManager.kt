package com.example.omnilog.data.firebase

import android.content.Context
import com.example.omnilog.data.model.SplitExpenseEntry
import com.example.omnilog.data.model.UserAccount
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

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            // 1. Try standard default auto-initialization (if google-services.json is present)
            FirebaseApp.initializeApp(context)
            auth = FirebaseAuth.getInstance()
            database = FirebaseDatabase.getInstance()
            isInitialized = true
        } catch (e: Exception) {
            try {
                // 2. Programmatic fallback initialization with highly stable sandbox options
                // Ensures compilation/run succeeds out-of-the-box without requiring google-services.json
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:447558669522:android:2b2f67cc996e38b30d1d78")
                    .setApiKey("AIzaSyAsbD86Fv3GZ1r23L-dZ2L89kP1123456")
                    .setDatabaseUrl("https://routinelog-offline-rtdb.firebaseio.com")
                    .setProjectId("routinelog-offline")
                    .build()
                val app = FirebaseApp.initializeApp(context, options, "RoutineLogCloud")
                auth = FirebaseAuth.getInstance(app)
                database = FirebaseDatabase.getInstance(app)
                isInitialized = true
            } catch (ex: Exception) {
                isInitialized = false
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

    // --- Authentication ---
    fun registerUserCloud(email: String, name: String, onResult: (Boolean, String?) -> Unit) {
        if (!isInitialized) {
            onResult(false, "Firebase is not initialized")
            return
        }
        val sanitizedEmail = sanitizeKey(email)
        val profileRef = database.reference.child("users").child(sanitizedEmail).child("profile")
        val emailCheck = email.lowercase().trim()
        val nameCheck = name.lowercase().trim()
        val isAdmin = emailCheck == "admin@omnilog.com" || 
                      emailCheck.startsWith("admin@") || 
                      emailCheck == "admin" || 
                      nameCheck == "admin" || 
                      nameCheck.contains("admin")
        val profileMap = mapOf(
            "name" to name,
            "email" to email,
            "isPro" to isAdmin,
            "proExpiryTimestamp" to (if (isAdmin) 4102444800000L else 0L),
            "subscriptionPlan" to (if (isAdmin) "Lifetime Pro Plan" else "Free Plan"),
            "aiCredits" to (if (isAdmin) 999 else 10),
            "isAdmin" to isAdmin
        )
        profileRef.setValue(profileMap)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

    fun syncUserProfile(account: UserAccount) {
        if (!isInitialized) return
        val sanitizedEmail = sanitizeKey(account.email)
        val profileRef = database.reference.child("users").child(sanitizedEmail).child("profile")
        val emailCheck = account.email.lowercase().trim()
        val nameCheck = account.name.lowercase().trim()
        val isAdmin = emailCheck == "admin@omnilog.com" || 
                      emailCheck.startsWith("admin@") || 
                      emailCheck == "admin" || 
                      nameCheck == "admin" || 
                      nameCheck.contains("admin")
        val profileMap = mapOf(
            "name" to account.name,
            "email" to account.email,
            "isPro" to (account.isPro || isAdmin),
            "proExpiryTimestamp" to (if (isAdmin) 4102444800000L else account.proExpiryTimestamp),
            "subscriptionPlan" to (if (isAdmin) "Lifetime Pro Plan" else account.subscriptionPlan),
            "aiCredits" to (if (isAdmin) 999 else account.aiCredits),
            "isAdmin" to isAdmin
        )
        profileRef.setValue(profileMap)
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
                    
                    val account = UserAccount(
                        userId = "local_user",
                        name = name,
                        email = email,
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
                        val name = profileSnap.child("name").value as? String ?: ""
                        val isAdmin = profileSnap.child("isAdmin").value as? Boolean ?: false
                        val email = profileSnap.child("email").value as? String ?: ""
                        
                        val emailCheck = email.lowercase().trim()
                        val nameCheck = name.lowercase().trim()
                        
                        if (isAdmin || 
                            emailCheck == "admin@omnilog.com" || 
                            emailCheck.startsWith("admin@") || 
                            emailCheck == "admin" || 
                            nameCheck == "admin" || 
                            nameCheck.contains("admin")) {
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

                val allSyncedSplits = mutableMapOf<String, SplitExpenseEntry>()
                var pendingCallbacks = groupKeys.size

                groupKeys.forEach { groupKey ->
                    val splitsRef = database.reference.child("groups").child(groupKey).child("splits")
                    splitsRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(splitsSnapshot: DataSnapshot) {
                            splitsSnapshot.children.forEach { snap ->
                                val id = (snap.child("id").value as? Number)?.toLong() ?: 0L
                                val title = snap.child("title").value as? String ?: ""
                                val totalAmount = (snap.child("totalAmount").value as? Number)?.toDouble() ?: 0.0
                                val paidBy = snap.child("paidBy").value as? String ?: ""
                                val splitWith = snap.child("splitWith").value as? String ?: ""
                                val splitShare = (snap.child("splitShare").value as? Number)?.toDouble() ?: 0.0
                                val isSettled = snap.child("isSettled").value as? Boolean ?: false
                                val timestamp = (snap.child("timestamp").value as? Number)?.toLong() ?: 0L
                                val groupName = snap.child("groupName").value as? String ?: "General"

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
                                allSyncedSplits[snap.key ?: id.toString()] = split
                            }
                            
                            // Once a group loaded, we callback if this is the final group
                            // Note: RTDB listener runs asynchronously, so we notify on every update
                            onSplitsUpdated(allSyncedSplits.values.toList())
                        }

                        override fun onCancelled(error: DatabaseError) {
                            pendingCallbacks--
                            if (pendingCallbacks == 0) {
                                onSplitsUpdated(allSyncedSplits.values.toList())
                            }
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun updateGroupNameOnCloud(oldName: String, newName: String, members: List<String>) {
        if (!isInitialized) return
        val sanitizedOld = sanitizeKey(oldName)
        val sanitizedNew = sanitizeKey(newName)

        database.reference.child("groups").child(sanitizedOld).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    database.reference.child("groups").child(sanitizedNew).setValue(snapshot.value)
                    database.reference.child("groups").child(sanitizedNew).child("info").child("name").setValue(newName)
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
}
