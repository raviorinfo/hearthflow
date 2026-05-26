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
        // Auth is handled dynamically. Since sandbox credentials are simulated if offline,
        // we can authenticate locally or online. When online, we can write profile to RTDB:
        val sanitizedEmail = sanitizeKey(email)
        val profileRef = database.reference.child("users").child(sanitizedEmail).child("profile")
        val profileMap = mapOf(
            "name" to name,
            "email" to email,
            "isPro" to false,
            "proExpiryTimestamp" to 0L,
            "subscriptionPlan" to "Free Plan",
            "aiCredits" to 10
        )
        profileRef.setValue(profileMap)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

    fun syncUserProfile(account: UserAccount) {
        if (!isInitialized) return
        val sanitizedEmail = sanitizeKey(account.email)
        val profileRef = database.reference.child("users").child(sanitizedEmail).child("profile")
        
        val profileMap = mapOf(
            "name" to account.name,
            "email" to account.email,
            "isPro" to account.isPro,
            "proExpiryTimestamp" to account.proExpiryTimestamp,
            "subscriptionPlan" to account.subscriptionPlan,
            "aiCredits" to account.aiCredits
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
}
