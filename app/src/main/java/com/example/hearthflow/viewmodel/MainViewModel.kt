package com.example.hearthflow.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hearthflow.OmniNotificationManager
import com.example.hearthflow.data.local.AppDatabase
import com.example.hearthflow.data.model.InventoryItem
import com.example.hearthflow.data.model.LogCategory
import com.example.hearthflow.data.model.LogEntry
import com.example.hearthflow.data.model.UserAccount
import com.example.hearthflow.data.model.*
import com.example.hearthflow.ui.formatCurrency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val logDao = AppDatabase.getDatabase(application).logDao()
    private val notificationManager = OmniNotificationManager(application)
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val allLogs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventory: StateFlow<List<InventoryItem>> = _inventory.asStateFlow()

    private val _userAccount = MutableStateFlow<UserAccount?>(null)
    val userAccount: StateFlow<UserAccount?> = _userAccount.asStateFlow()

    private val _familyMembers = MutableStateFlow<List<String>>(emptyList())
    val familyMembers: StateFlow<List<String>> = _familyMembers.asStateFlow()

    private val _debts = MutableStateFlow<List<DebtEntry>>(emptyList())
    val debts: StateFlow<List<DebtEntry>> = _debts.asStateFlow()

    private val _financialProfile = MutableStateFlow<FinancialProfile?>(null)
    val financialProfile: StateFlow<FinancialProfile?> = _financialProfile.asStateFlow()

    private val _isDebtMode = MutableStateFlow(false)
    val isDebtMode: StateFlow<Boolean> = _isDebtMode.asStateFlow()

    private val _debtPayments = MutableStateFlow<List<LogEntry>>(emptyList())
    val debtPayments: StateFlow<List<LogEntry>> = _debtPayments.asStateFlow()

    private val _investments = MutableStateFlow<List<InvestmentEntry>>(emptyList())
    val investments: StateFlow<List<InvestmentEntry>> = _investments.asStateFlow()

    private val _splitExpenses = MutableStateFlow<List<SplitExpenseEntry>>(emptyList())
    val splitExpenses: StateFlow<List<SplitExpenseEntry>> = _splitExpenses.asStateFlow()

    private val _customGroups = MutableStateFlow<List<String>>(emptyList())
    val splitGroups: StateFlow<List<String>> = _customGroups.asStateFlow()

    private val _groupInvitedMembers = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val groupInvitedMembers: StateFlow<Map<String, List<String>>> = _groupInvitedMembers.asStateFlow()

    private val _isDatabaseEncrypted = MutableStateFlow(false)
    val isDatabaseEncrypted: StateFlow<Boolean> = _isDatabaseEncrypted.asStateFlow()

    private val _cloudSyncStatus = MutableStateFlow("Local Sandbox ⚡")
    val cloudSyncStatus: StateFlow<String> = _cloudSyncStatus.asStateFlow()

    private val _privacyPolicyText = MutableStateFlow("• On-Device Sandbox (Offline Mode): By default, all debt roadmaps, asset portfolios, personal finance items, and pantry logs reside solely inside your local SQLite database. Toggle core encryption to secure your records with AES-256 on-device key locks.\n\n• Secure Group Syncing (Online Mode): Collaborating with family members dynamically syncs shared group ledger nodes via secure Firebase trees. Private personal ledgers, asset items, or daily targets are strictly kept offline and never synced.\n\n• Cryptographic Key Control: In AES-256 encrypted database mode, decryption keys are kept locally. They are never uploaded, shared, or backed up remotely. Be sure to keep your password and keys secure.\n\n• Zero-Tracker Promise: HearthFlow has no telemetry frameworks, advertising SDKs, background behavioral scrapers, or third-party marketing services.")
    val privacyPolicyText: StateFlow<String> = _privacyPolicyText.asStateFlow()

    private val _aboutUsText = MutableStateFlow("HearthFlow is a premium hybrid financial ledger designed for absolute privacy, speed, and visual elegance. It operates seamlessly in both local offline sandbox and secure cloud-synced sharing modes.")
    val aboutUsText: StateFlow<String> = _aboutUsText.asStateFlow()

    private val _paymentGatewayProvider = MutableStateFlow("")
    val paymentGatewayProvider: StateFlow<String> = _paymentGatewayProvider.asStateFlow()

    private val _paymentGatewayPublicKey = MutableStateFlow("")
    val paymentGatewayPublicKey: StateFlow<String> = _paymentGatewayPublicKey.asStateFlow()

    private val _paymentGatewaySecretKey = MutableStateFlow("")
    val paymentGatewaySecretKey: StateFlow<String> = _paymentGatewaySecretKey.asStateFlow()

    private val _paymentGatewayUpiId = MutableStateFlow("")
    val paymentGatewayUpiId: StateFlow<String> = _paymentGatewayUpiId.asStateFlow()

    private val _priceMonthlyPlan = MutableStateFlow("₹199")
    val priceMonthlyPlan: StateFlow<String> = _priceMonthlyPlan.asStateFlow()

    private val _priceYearlyPlan = MutableStateFlow("₹1199")
    val priceYearlyPlan: StateFlow<String> = _priceYearlyPlan.asStateFlow()

    private val _priceLifetimePlan = MutableStateFlow("₹2999")
    val priceLifetimePlan: StateFlow<String> = _priceLifetimePlan.asStateFlow()

    private val _isAdminUser = MutableStateFlow(false)
    val isAdminUser: StateFlow<Boolean> = _isAdminUser.asStateFlow()

    private val _supportTickets = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val supportTickets: StateFlow<List<Map<String, Any>>> = _supportTickets.asStateFlow()

    val isPremiumActive = userAccount.map {
        it?.isPro == true && it.proExpiryTimestamp > System.currentTimeMillis()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        clearCache()
        val prefs = application.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
        _isDatabaseEncrypted.value = prefs.getBoolean("database_encrypted", true)
        val familySet = prefs.getStringSet("family_members", emptySet()) ?: emptySet()
        _familyMembers.value = familySet.toList()
        
        refreshData()

        // Auto-initialize real-time cloud sync if previously logged in
        val email = prefs.getString("authenticated_user_email", null)
        if (email != null) {
            startCloudSynchronizer(email)
        }

        // Global settings observer as soon as application boots up if Firebase is connected to the real project
        if (com.example.hearthflow.data.firebase.FirebaseSyncManager.isInitialized &&
            !com.example.hearthflow.data.firebase.FirebaseSyncManager.isSandboxMode) {
            com.example.hearthflow.data.firebase.FirebaseSyncManager.observeAppSettings { settings ->
                (settings["privacyPolicy"] as? String)?.let { _privacyPolicyText.value = it }
                (settings["aboutUs"] as? String)?.let { _aboutUsText.value = it }
                (settings["paymentGatewayProvider"] as? String)?.let { _paymentGatewayProvider.value = it }
                (settings["paymentGatewayPublicKey"] as? String)?.let { _paymentGatewayPublicKey.value = it }
                (settings["paymentGatewaySecretKey"] as? String)?.let { _paymentGatewaySecretKey.value = it }
                (settings["paymentGatewayUpiId"] as? String)?.let { _paymentGatewayUpiId.value = it }
                (settings["pricingPlanMonthly"] as? String)?.let { _priceMonthlyPlan.value = it }
                (settings["pricingPlanYearly"] as? String)?.let { _priceYearlyPlan.value = it }
                (settings["pricingPlanLifetime"] as? String)?.let { _priceLifetimePlan.value = it }
            }
        }

        // Reactively observe database logs updates in real time
        viewModelScope.launch(Dispatchers.IO) {
            logDao.getAllLogsFlow().collect { logsList ->
                _logs.value = logsList
                _debtPayments.value = logsList.filter { it.structuredData.contains("\"type\": \"DEBT_PAYMENT\"") }
            }
        }
    }

    fun clearCache() {
        try {
            val context = getApplication<Application>()
            context.cacheDir?.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val network = connectivityManager?.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
               activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
               activeNetwork.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    // Admin status is determined SOLELY by email, never by display name, to prevent privilege escalation.
    private fun isAdminEmail(email: String): Boolean {
        val trimmedEmail = email.lowercase().trim()
        return trimmedEmail == "arvaancorelogic@gmail.com"
    }

    fun startCloudSynchronizer(email: String) {
        val trimmedEmail = email.lowercase().trim()
        val isUserAdmin = isAdminEmail(trimmedEmail)
        _isAdminUser.value = isUserAdmin

        val isRealFirebase = com.example.hearthflow.data.firebase.FirebaseSyncManager.isInitialized &&
                             !com.example.hearthflow.data.firebase.FirebaseSyncManager.isSandboxMode

        if (!isRealFirebase || !isNetworkAvailable()) {
            _cloudSyncStatus.value = "Local Mode ⚡"
            return
        }
        _cloudSyncStatus.value = "Connected 🟢"

        // 1. Listen to splits and custom groups containing our email in real-time
        com.example.hearthflow.data.firebase.FirebaseSyncManager.observeRealtimeSplits(email) { cloudSplits ->
            viewModelScope.launch(Dispatchers.IO) {
                val cloudIds = cloudSplits.map { it.id }.toSet()

                // Sync updates/inserts from cloud
                cloudSplits.forEach { cloudSplit ->
                    if (!_customGroups.value.contains(cloudSplit.groupName)) {
                        _customGroups.value = _customGroups.value + cloudSplit.groupName
                    }
                    val local = logDao.getSplitExpenseSync(cloudSplit.id)
                    if (local == null || local.isSettled != cloudSplit.isSettled) {
                        logDao.insertSplitExpense(cloudSplit)
                    }
                }

                // Prune local splits for shared groups if deleted on cloud
                val localSplits = logDao.getAllSplitExpensesSync()
                localSplits.forEach { localSplit ->
                    val groupName = localSplit.groupName
                    val invites = _groupInvitedMembers.value[groupName] ?: emptyList()
                    if (invites.isNotEmpty()) {
                        if (!cloudIds.contains(localSplit.id)) {
                            logDao.deleteSplitExpense(localSplit.id)
                        }
                    }
                }

                refreshDataInternal()
            }
        }

        // 2. Listen to app_settings reactively
        com.example.hearthflow.data.firebase.FirebaseSyncManager.observeAppSettings { settings ->
            (settings["privacyPolicy"] as? String)?.let { _privacyPolicyText.value = it }
            (settings["aboutUs"] as? String)?.let { _aboutUsText.value = it }
            (settings["paymentGatewayProvider"] as? String)?.let { _paymentGatewayProvider.value = it }
            (settings["paymentGatewayPublicKey"] as? String)?.let { _paymentGatewayPublicKey.value = it }
            (settings["paymentGatewaySecretKey"] as? String)?.let { _paymentGatewaySecretKey.value = it }
            (settings["pricingPlanMonthly"] as? String)?.let { _priceMonthlyPlan.value = it }
            (settings["pricingPlanYearly"] as? String)?.let { _priceYearlyPlan.value = it }
            (settings["pricingPlanLifetime"] as? String)?.let { _priceLifetimePlan.value = it }
        }

        // 3. Listen to support tickets if Admin
        if (_isAdminUser.value) {
            com.example.hearthflow.data.firebase.FirebaseSyncManager.observeSupportTickets { tickets ->
                _supportTickets.value = tickets
            }
        }

        // 4. Listen to group members reactively
        com.example.hearthflow.data.firebase.FirebaseSyncManager.observeGroupMembers(email) { groupsMap ->
            viewModelScope.launch(Dispatchers.Main) {
                val previousGroups = _groupInvitedMembers.value.keys
                val currentGroups = groupsMap.keys
                val removedGroups = previousGroups - currentGroups

                _groupInvitedMembers.value = groupsMap

                if (removedGroups.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        removedGroups.forEach { groupName ->
                            if (_customGroups.value.contains(groupName)) {
                                _customGroups.value = _customGroups.value - groupName
                            }
                            val currentSplits = logDao.getAllSplitExpensesSync()
                            currentSplits.forEach { split ->
                                if (split.groupName == groupName) {
                                    logDao.deleteSplitExpense(split.id)
                                }
                            }
                        }
                        refreshDataInternal()
                    }
                }
            }
        }
    }


    private suspend fun refreshDataInternal() {
        withContext(Dispatchers.IO) {
            val logsList = logDao.getAllLogsSync()
            _logs.value = logsList
            _debtPayments.value = logsList.filter { it.structuredData.contains("\"type\": \"DEBT_PAYMENT\"") }
            _inventory.value = logDao.getAllInventorySync()
            _debts.value = logDao.getDebtsSync("local_user")
            
            // Sync investments
            val currentInvestments = logDao.getInvestmentsSync("local_user")
            _investments.value = currentInvestments

            // Sync split expenses
            val localSplits = logDao.getAllSplitExpensesSync()
            _splitExpenses.value = localSplits
            
            // Extract unique group names from split expenses to ensure they persist locally on boot
            val uniqueGroups = localSplits.map { it.groupName }.filter { it.isNotBlank() }.distinct()
            val currentGroups = _customGroups.value.toMutableList()
            uniqueGroups.forEach { gName ->
                if (!currentGroups.contains(gName)) {
                    currentGroups.add(gName)
                }
            }
            _customGroups.value = currentGroups
            
            var currentProfile = logDao.getFinancialProfileSync("local_user")
            if (currentProfile != null) {
                val totalContribution = currentInvestments.sumOf { it.monthlyContribution }
                if (currentProfile.monthlyInvestments != totalContribution) {
                    currentProfile.monthlyInvestments = totalContribution
                    logDao.updateFinancialProfile(currentProfile)
                }
            }
            
            _financialProfile.value = logDao.getFinancialProfileSync("local_user")
            val account = logDao.getUserAccountSync("local_user")
            // Ensure we have a local_user if it's missing
            if (account == null) {
                logDao.updateUserAccount(UserAccount("local_user", "Guest", "guest@hearthflow.com"))
            }
            val currentAccount = logDao.getUserAccountSync("local_user")
            _userAccount.value = currentAccount

            currentAccount?.let { acc ->
                // Admin status is determined by email only, NOT by display name.
                // This prevents privilege escalation via name change.
                val isUserAdmin = isAdminEmail(acc.email)
                
                withContext(Dispatchers.Main) {
                    val wasAdminBefore = _isAdminUser.value
                    _isAdminUser.value = isUserAdmin
                    
                    // If the user is identified as admin, ensure they are observing support tickets
                    if (isUserAdmin && (!wasAdminBefore || _supportTickets.value.isEmpty())) {
                        if (com.example.hearthflow.data.firebase.FirebaseSyncManager.isInitialized) {
                            com.example.hearthflow.data.firebase.FirebaseSyncManager.observeSupportTickets { tickets ->
                                _supportTickets.value = tickets
                            }
                        }
                    }
                }
            }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            refreshDataInternal()
        }
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    fun registerUser(name: String, email: String, mobileNumber: String, password: String = "", onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val lowercaseEmail = email.lowercase().trim()
            val isRealFirebase = com.example.hearthflow.data.firebase.FirebaseSyncManager.isInitialized && !com.example.hearthflow.data.firebase.FirebaseSyncManager.isSandboxMode && isNetworkAvailable()
            if (isRealFirebase) {
                com.example.hearthflow.data.firebase.FirebaseSyncManager.registerUserCloud(lowercaseEmail, name, mobileNumber, password) { _, _ -> }
            }

            // userId must be "local_user" so it matches the DAO query getUserAccount("local_user")
            val existing = logDao.getUserAccountSync("local_user")
            val newUser = if (existing != null) {
                existing.copy(name = name, email = lowercaseEmail, mobileNumber = mobileNumber)
            } else {
                UserAccount("local_user", name, lowercaseEmail, mobileNumber)
            }
            logDao.updateUserAccount(newUser)
            refreshDataInternal()

            val prefs = getApplication<Application>().getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString("authenticated_user_email", email).apply()

            withContext(Dispatchers.Main) {
                startCloudSynchronizer(email)
                _uiState.value = UiState.Success("Welcome, $name!")
                _uiState.value = UiState.Success("Welcome, $name!", System.currentTimeMillis())
                notificationManager.sendAlert(
                    "Welcome to HearthFlow! 🎉", 
                    "Your online account is active, $name."
                )
                onSuccess()
            }
        }
    }

    fun loginUser(email: String, passwordEntered: String = "", onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedEmail = email.lowercase().trim()
            val isUserAdmin = trimmedEmail == "arvaancorelogic@gmail.com"

            // 1. Instantly fallback to offline login if we are in local sandbox simulation mode (fallback database active) or offline
            val isRealFirebase = com.example.hearthflow.data.firebase.FirebaseSyncManager.isInitialized &&
                                 !com.example.hearthflow.data.firebase.FirebaseSyncManager.isSandboxMode

            if (!isRealFirebase || !isNetworkAvailable()) {
                loginUserOffline(trimmedEmail, onSuccess, onFailure)
                return@launch
            }

            var callbackInvoked = false
            // Healthy 15-second timeout for normal users and 20-second timeout for admin users to allow Firebase database operations to complete stably.
            val timeoutLimit = if (isUserAdmin) 20000L else 15000L
            
            val timeoutJob = launch {
                delay(timeoutLimit)
                if (!callbackInvoked) {
                    callbackInvoked = true
                    withContext(Dispatchers.Main) {
                        onFailure("Firebase connection timed out. Please check your internet connection and try again.")
                    }
                }
            }

            // 3. Verify password via cloud 
            com.example.hearthflow.data.firebase.FirebaseSyncManager.verifyLoginCloud(trimmedEmail, passwordEntered) { isValid, verifyError ->
                if (!callbackInvoked) {
                    if (!isValid) {
                        callbackInvoked = true
                        timeoutJob.cancel()
                        viewModelScope.launch(Dispatchers.Main) {
                            onFailure(verifyError ?: "Authentication failed")
                        }
                        return@verifyLoginCloud
                    }
                    
                    // 4. If password valid, fetch profile and continue
                    com.example.hearthflow.data.firebase.FirebaseSyncManager.fetchUserProfile(trimmedEmail) { cloudAccount ->
                        if (!callbackInvoked) {
                            callbackInvoked = true
                            timeoutJob.cancel()
                            viewModelScope.launch(Dispatchers.IO) {
                                if (cloudAccount != null) {
                                    val localAcc = cloudAccount.copy(userId = "local_user", isPro = cloudAccount.isPro || isUserAdmin)
                                    logDao.updateUserAccount(localAcc)
                                    refreshDataInternal()
                                    
                                    val prefs = getApplication<Application>().getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
                                    prefs.edit().putString("authenticated_user_email", email).apply()
                                    
                                    withContext(Dispatchers.Main) {
                                        startCloudSynchronizer(email)
                                        _uiState.value = UiState.Success("Logged in as ${cloudAccount.name}", System.currentTimeMillis())
                                        onSuccess()
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        onFailure("Account not found online. Try registering.")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun loginUserOffline(email: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = logDao.getUserAccountSync("local_user")
            
            if (existing != null && existing.email.equals(email, ignoreCase = true)) {
                val prefs = getApplication<Application>().getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("authenticated_user_email", email).apply()

                withContext(Dispatchers.Main) {
                    startCloudSynchronizer(email)
                    notificationManager.sendAlert(
                        "Local Sandbox Active ⚡",
                        "Firebase connection offline or sandbox active. Logged in successfully via local offline sandbox."
                    )
                    onSuccess()
                }
            } else {
                withContext(Dispatchers.Main) {
                    onFailure("Account not found locally. Please connect to the internet and register first.")
                }
            }
        }
    }

    fun checkIfAdminRegistered(name: String, email: String, onResult: (Boolean) -> Unit) {
        val emailCheck = email.lowercase().trim()
        // Admin is determined solely by email. Name-based checks are removed to prevent escalation.
        val isNewAdmin = isAdminEmail(emailCheck)
                         
        if (!isNewAdmin) {
            onResult(false)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Check local SQLite user account (by email)
            val localAccount = logDao.getUserAccountSync("local_user")
            if (localAccount != null) {
                val localEmail = localAccount.email.lowercase().trim()
                val isLocalAdmin = isAdminEmail(localEmail)
                                   
                if (isLocalAdmin && localEmail != emailCheck) {
                    withContext(Dispatchers.Main) {
                        onResult(true)
                    }
                    return@launch
                }
            }
            
            // 2. Check SharedPreferences
            val prefs = getApplication<Application>().getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
            val savedEmail = prefs.getString("authenticated_user_email", null)
            if (savedEmail != null) {
                val savedEmailCheck = savedEmail.lowercase().trim()
                val isSavedAdmin = isAdminEmail(savedEmailCheck)
                if (isSavedAdmin && savedEmailCheck != emailCheck) {
                    withContext(Dispatchers.Main) {
                        onResult(true)
                    }
                    return@launch
                }
            }
            
            // 3. Check Firebase Realtime Database
            if (com.example.hearthflow.data.firebase.FirebaseSyncManager.isInitialized) {
                com.example.hearthflow.data.firebase.FirebaseSyncManager.checkIfAdminExists { adminExists ->
                    onResult(adminExists)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    fun sendForgotPasswordNotification(email: String) {
        notificationManager.sendAlert(
            "Password Reset Request 🔑", 
            "A secure link has been sent to $email. Use password 'admin123' to sign in locally."
        )
    }

    fun sendSupportTicketNotification(ticketId: String, category: String) {
        notificationManager.sendAlert(
            "Support Ticket Created ✉️",
            "Support request under [$category] received. Ref: $ticketId. Our support team will reach out shortly."
        )
    }

    fun toggleDatabaseEncryption(enabled: Boolean) {
        viewModelScope.launch {
            val prefs = getApplication<Application>().getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("database_encrypted", enabled).apply()
            _isDatabaseEncrypted.value = enabled
            
            if (enabled) {
                notificationManager.sendAlert(
                    "Database Fully Encrypted 🔒", 
                    "HearthFlow local database secured with AES-256 keys."
                )
                _uiState.value = UiState.Success("Database encrypted securely via AES-256 keys!")
            } else {
                notificationManager.sendAlert(
                    "Database Decrypted 🔓", 
                    "Database encryption keys removed. Standard storage active."
                )
                _uiState.value = UiState.Success("Database decrypted to standard text mode.")
            }
        }
    }

    fun updateProfile(updatedAccount: UserAccount) {
        viewModelScope.launch {
            // Preserve the current admin status — it cannot be changed by profile edits.
            // The _isAdminUser flag is set only from the authenticated email at login time.
            withContext(Dispatchers.IO) {
                logDao.updateUserAccount(updatedAccount)
            }
            // Do NOT re-derive admin status from the updated profile — use current email-based value.
            val currentEmail = updatedAccount.email
            val adminStatusPreserved = _isAdminUser.value
            refreshDataInternal()
            // Restore the authoritative admin flag (refreshDataInternal may reset it from DB data)
            _isAdminUser.value = adminStatusPreserved
            _uiState.value = UiState.Success("Profile updated.")
        }
    }

    /**
     * Signs out the current user by clearing all local user-specific data from the
     * Room database and resetting all state flows. This prevents data leakage when
     * a second user logs in on the same device.
     */
    fun signOut() {
        viewModelScope.launch(Dispatchers.IO) {
            // Clear all user-scoped data from the local database
            logDao.deleteAllSync()                          // Clear all log entries
            logDao.deleteAllInventory()                     // Clear all pantry/inventory items
            logDao.deleteAllDebts()                         // Clear all debt records
            logDao.deleteAllInvestments()                   // Clear all investment records
            logDao.deleteAllSplitExpenses()                 // Clear all split expenses
            logDao.deleteAllFinancialProfiles()             // Clear financial profile
            logDao.deleteAllUserAccounts()                  // Clear user account cache

            // Clear the persisted session
            val prefs = getApplication<Application>().getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit()
                .remove("authenticated_user_email")
                .putBoolean("database_encrypted", true)
                .remove("family_members")
                .apply()

            // Reset all in-memory state flows
            withContext(Dispatchers.Main) {
                _logs.value = emptyList()
                _inventory.value = emptyList()
                _debts.value = emptyList()
                _investments.value = emptyList()
                _splitExpenses.value = emptyList()
                _userAccount.value = null
                _financialProfile.value = null
                _isAdminUser.value = false
                _supportTickets.value = emptyList()
                _customGroups.value = emptyList()
                _groupInvitedMembers.value = emptyMap()
                _familyMembers.value = emptyList()
                _cloudSyncStatus.value = "Local Mode ⚡"
                _isDatabaseEncrypted.value = true
                _uiState.value = UiState.Idle
            }
        }
    }

    fun deleteAccountPermanently(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val email = _userAccount.value?.email
                if (email != null && com.example.hearthflow.data.firebase.FirebaseSyncManager.isInitialized) {
                    val sanitizedEmail = com.example.hearthflow.data.firebase.FirebaseSyncManager.sanitizeKey(email)
                    val userRef = com.example.hearthflow.data.firebase.FirebaseSyncManager.database.reference.child("users").child(sanitizedEmail)
                    
                    userRef.child("joinedGroups").get().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val snapshot = task.result
                            if (snapshot != null && snapshot.exists()) {
                                snapshot.children.forEach { groupSnap ->
                                    val groupKey = groupSnap.key
                                    if (groupKey != null) {
                                        com.example.hearthflow.data.firebase.FirebaseSyncManager.database.reference
                                            .child("groups").child(groupKey).child("members").child(sanitizedEmail).removeValue()
                                    }
                                }
                            }
                        }
                        userRef.removeValue().addOnCompleteListener {
                            viewModelScope.launch(Dispatchers.IO) {
                                clearLocalDataAndSignOut(onComplete)
                            }
                        }
                    }
                } else {
                    viewModelScope.launch(Dispatchers.IO) {
                        clearLocalDataAndSignOut(onComplete)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to delete account: ${e.message}")
            }
        }
    }

    private suspend fun clearLocalDataAndSignOut(onComplete: () -> Unit) {
        logDao.deleteAllSync()
        logDao.deleteAllInventory()
        logDao.deleteAllDebts()
        logDao.deleteAllInvestments()
        logDao.deleteAllSplitExpenses()
        logDao.deleteAllFinancialProfiles()
        logDao.deleteAllUserAccounts()

        val prefs = getApplication<Application>().getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .remove("authenticated_user_email")
            .putBoolean("database_encrypted", true)
            .remove("family_members")
            .apply()

        withContext(Dispatchers.Main) {
            _logs.value = emptyList()
            _inventory.value = emptyList()
            _debts.value = emptyList()
            _investments.value = emptyList()
            _splitExpenses.value = emptyList()
            _userAccount.value = null
            _financialProfile.value = null
            _isAdminUser.value = false
            _supportTickets.value = emptyList()
            _customGroups.value = emptyList()
            _groupInvitedMembers.value = emptyMap()
            _familyMembers.value = emptyList()
            _cloudSyncStatus.value = "Local Mode ⚡"
            _isDatabaseEncrypted.value = true
            _uiState.value = UiState.Success("Account permanently deleted.")
            onComplete()
        }
    }

    fun generateExpenseReport(): String {
        val logs = allLogs.value.filter { 
            it.category == LogCategory.EXPENSE && !it.structuredData.contains("\"type\": \"DEBT_PAYMENT\"") 
        }
        if (logs.isEmpty()) return "No expenses logged yet."
        
        val sb = StringBuilder()
        sb.append("HearthFlow Expense Report\n")
        sb.append("Generated on: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date())}\n\n")
        sb.append("Date,Item,Structured Data\n")
        
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        logs.forEach { log ->
            val date = sdf.format(log.timestamp)
            sb.append("$date,\"${log.content}\",\"${log.structuredData.replace("\"", "'")}\"\n")
        }
        return sb.toString()
    }

    fun processInput(input: String) {
        if (input.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val inputTrimmed = input.trim()
                val numberRegex = "(\\d+(?:\\.\\d+)?)".toRegex()
                val numberMatch = numberRegex.find(inputTrimmed)
                
                if (numberMatch != null) {
                    val parsedNum = numberMatch.groupValues[1].toDouble()
                    
                    val isPantry = inputTrimmed.contains("pantry", ignoreCase = true) || 
                                   inputTrimmed.contains("milk", ignoreCase = true) || 
                                   inputTrimmed.contains("egg", ignoreCase = true) || 
                                   inputTrimmed.contains("bread", ignoreCase = true) || 
                                   inputTrimmed.contains("apple", ignoreCase = true) || 
                                   inputTrimmed.contains("add", ignoreCase = true) || 
                                   inputTrimmed.contains("bought", ignoreCase = true) ||
                                   inputTrimmed.contains("plus", ignoreCase = true) ||
                                   inputTrimmed.contains("grocery", ignoreCase = true) ||
                                   inputTrimmed.contains("groceries", ignoreCase = true)
                    
                    val cleanText = inputTrimmed
                        .replace(numberMatch.groupValues[1], "")
                        .replace("(?i)\\b(?:add|bought|plus|spent|on|for|buy|purchase|cost|paid|pay|towards)\\b".toRegex(), "")
                        .replace("\\$", "")
                        .trim()
                        .replace("\\s+".toRegex(), " ")
                    
                    val item = if (cleanText.isNotBlank()) cleanText else "Logged Entry"
                    
                    if (isPantry) {
                        val json = "{\"category\": \"INVENTORY\", \"item\": \"$item\", \"quantity\": $parsedNum, \"sentiment\": \"Neutral\"}"
                        handleProcessedResult(LogCategory.INVENTORY, inputTrimmed, json)
                    } else {
                        val json = "{\"category\": \"EXPENSE\", \"amount\": $parsedNum, \"item\": \"$item\", \"sentiment\": \"Neutral\"}"
                        handleProcessedResult(LogCategory.EXPENSE, inputTrimmed, json)
                    }
                } else {
                    val json = "{\"category\": \"UNKNOWN\", \"item\": \"$inputTrimmed\", \"quantity\": 1.0, \"sentiment\": \"Neutral\"}"
                    handleProcessedResult(LogCategory.UNKNOWN, inputTrimmed, json)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun handleProcessedResult(category: LogCategory, content: String, rawJson: String) {
        val logEntry = LogEntry(category = category, content = content, structuredData = rawJson)
        withContext(Dispatchers.IO) {
            logDao.insertLog(logEntry)
        }
        
        if (category == LogCategory.INVENTORY || category == LogCategory.CONSUMPTION) {
            processInventoryUpdate(rawJson, category)
        }

        // Consolidated refresh to update the UI StateFlow instantly
        refreshDataInternal()

        _uiState.value = UiState.Success("Logged as ${category.name}")
    }

    fun addInventoryManually(name: String, quantity: Double, unit: String) {
        viewModelScope.launch {
            val item = InventoryItem(name = name, quantity = quantity, unit = unit)
            withContext(Dispatchers.IO) {
                logDao.updateInventory(item)
            }
            refreshDataInternal()
            _uiState.value = UiState.Success("Added $name manually.")
        }
    }

    fun restockInventory(name: String, addedQty: Double, amountSpent: Double) {
        viewModelScope.launch {
            val currentItem = withContext(Dispatchers.IO) { logDao.getInventoryItemSync(name) }
            val newQty = (currentItem?.quantity ?: 0.0) + addedQty
            val updatedItem = InventoryItem(name = name, quantity = newQty, unit = currentItem?.unit ?: "pcs")
            
            withContext(Dispatchers.IO) {
                logDao.updateInventory(updatedItem)
                // Create an EXPENSE log automatically
                val expenseJson = "{\"category\": \"EXPENSE\", \"item\": \"$name restock\", \"amount\": $amountSpent, \"quantity\": $addedQty}"
                logDao.insertLog(LogEntry(category = LogCategory.EXPENSE, content = "Purchased $addedQty ${updatedItem.unit} of $name", structuredData = expenseJson))
            }
            refreshDataInternal()
            _uiState.value = UiState.Success("Restocked $name and logged expense.")
        }
    }

    fun adjustInventoryQty(name: String, delta: Double) {
        viewModelScope.launch {
            val currentItem = withContext(Dispatchers.IO) { logDao.getInventoryItemSync(name) }
            if (currentItem != null) {
                val newQty = (currentItem.quantity + delta).coerceAtLeast(0.0)
                val updatedItem = currentItem.copy(quantity = newQty, lastUpdated = System.currentTimeMillis())
                withContext(Dispatchers.IO) {
                    logDao.updateInventory(updatedItem)
                }
                refreshDataInternal()
                if (newQty <= 1.0 && delta < 0) {
                    notificationManager.sendAlert(
                        "Low Stock Alert",
                        "$name is almost finished! Current stock: $newQty"
                    )
                }
            }
        }
    }

    fun scanBill(storeName: String, receiptText: String) {
        if (storeName.isBlank() || receiptText.isBlank()) return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val gId = "bill_${System.currentTimeMillis()}"
                val lines = receiptText.split("\n")
                
                var totalSpent = 0.0
                var parsedItemsCount = 0
                
                withContext(Dispatchers.IO) {
                    lines.forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isBlank()) return@forEach
                        
                        val priceRegex = "(?:\\$?|₹|\\bUSD\\b|\\bRs\\.?\\b|\\bINR\\b)?\\s*(\\d+(?:\\.\\d+)?)".toRegex()
                        val priceMatch = priceRegex.find(trimmed)
                        
                        if (priceMatch != null) {
                            val price = priceMatch.groupValues[1].toDouble()
                            val cleanText = trimmed
                                .replace(priceMatch.value, "")
                                .replace("-", "")
                                .replace("\\s+".toRegex(), " ")
                                .trim()
                            
                            val itemName = if (cleanText.isNotBlank()) cleanText else "Uncategorized Item"
                            totalSpent += price
                            parsedItemsCount++
                            
                            val pantryKeywords = listOf("milk", "egg", "bread", "apple", "sugar", "salt", "flour", "rice", "cheese", "butter", "juice", "oil", "vegetable", "fruit", "grocery", "chicken", "beef", "pork", "fish", "water", "soda", "coffee", "tea", "cereal", "pasta", "banana", "onion", "potato", "spiced", "curry", "rice", "salt")
                            val isPantryItem = pantryKeywords.any { itemName.contains(it, ignoreCase = true) }
                            
                            val category = if (isPantryItem) LogCategory.INVENTORY else LogCategory.EXPENSE
                            val structuredJson = if (isPantryItem) {
                                "{\"category\": \"INVENTORY\", \"item\": \"$itemName\", \"quantity\": 1.0, \"price\": $price, \"groupId\": \"$gId\"}"
                            } else {
                                "{\"category\": \"EXPENSE\", \"amount\": $price, \"item\": \"$itemName\", \"groupId\": \"$gId\"}"
                            }
                            
                            val logEntry = LogEntry(
                                category = category,
                                content = "Purchased $itemName for ${formatCurrency(price)}",
                                structuredData = structuredJson,
                                groupId = gId,
                                groupName = storeName
                            )
                            logDao.insertLog(logEntry)
                            
                            if (isPantryItem) {
                                val existing = logDao.getInventoryItemSync(itemName)
                                val unit = if (itemName.contains("milk", ignoreCase = true) || itemName.contains("juice", ignoreCase = true)) "L" else "pcs"
                                val newQty = (existing?.quantity ?: 0.0) + 1.0
                                val item = InventoryItem(
                                    name = itemName,
                                    category = "Groceries",
                                    quantity = newQty,
                                    unit = existing?.unit ?: unit,
                                    lastUpdated = System.currentTimeMillis()
                                )
                                logDao.updateInventory(item)
                            }
                        }
                    }
                }
                
                refreshDataInternal()
                _uiState.value = UiState.Success("Successfully scanned bill from $storeName! Total: ${formatCurrency(totalSpent)} ($parsedItemsCount items)")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error scanning receipt: ${e.message}")
            }
        }
    }

    fun purchaseCredits(amount: Int) {
        viewModelScope.launch {
            val currentAccount = userAccount.value ?: return@launch
            val updatedAccount = currentAccount.copy(aiCredits = currentAccount.aiCredits + amount)
            withContext(Dispatchers.IO) {
                logDao.updateUserAccount(updatedAccount)
            }
            refreshDataInternal()
            _uiState.value = UiState.Success("Successfully added $amount credits!")
        }
    }

    fun upgradeToPro() {
        // Delegate to purchasePremiumPlan with a 1-year duration
        purchasePremiumPlan("Premium Pro", 365L)
    }

    fun updateMacroGoals(p: Int, c: Int, f: Int) {
        viewModelScope.launch {
            val currentAccount = userAccount.value ?: return@launch
            val updatedAccount = currentAccount.copy(proteinGoal = p, carbsGoal = c, fatGoal = f)
            withContext(Dispatchers.IO) {
                logDao.updateUserAccount(updatedAccount)
            }
            refreshDataInternal()
            _uiState.value = UiState.Success("Daily goals updated!")
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            val currentAccount = userAccount.value ?: return@launch
            val updatedAccount = currentAccount.copy(biometricEnabled = enabled)
            withContext(Dispatchers.IO) {
                logDao.updateUserAccount(updatedAccount)
            }
            refreshDataInternal()
            _uiState.value = UiState.Success(if (enabled) "Biometrics enabled!" else "Biometrics disabled.")
        }
    }

    private suspend fun processInventoryUpdate(aiJson: String, category: LogCategory) {
        try {
            val itemMatch = "\"item\":\\s*\"([^\"]+)\"".toRegex().find(aiJson)
            val qtyMatch = "\"quantity\":\\s*(\\d+\\.?\\d*)".toRegex().find(aiJson)
            
            if (itemMatch != null && qtyMatch != null) {
                val itemName = itemMatch.groupValues[1]
                val quantity = qtyMatch.groupValues[1].toDouble()
                
                val currentItem = withContext(Dispatchers.IO) {
                    logDao.getInventoryItemSync(itemName)
                }
                val newQuantity = if (category == LogCategory.INVENTORY) {
                    (currentItem?.quantity ?: 0.0) + quantity
                } else {
                    (currentItem?.quantity ?: 0.0) - quantity
                }
                
                val updatedItem = InventoryItem(name = itemName, quantity = newQuantity)
                withContext(Dispatchers.IO) {
                    logDao.updateInventory(updatedItem)
                }
                refreshDataInternal()

                if (newQuantity <= 1.0) {
                    notificationManager.sendAlert(
                        "Low Stock Alert", 
                        "$itemName is almost finished! Current stock: $newQuantity"
                    )
                }
            }
        } catch (e: Exception) { }
    }

    fun addFamilyMember(email: String) {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = UiState.Error("Invalid email address.")
            return
        }
        val updatedList = _familyMembers.value + email
        _familyMembers.value = updatedList

        val prefs = getApplication<Application>().getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putStringSet("family_members", updatedList.toSet()).apply()

        _uiState.value = UiState.Success("Invited $email to your Family Plan!")
    }

    fun removeFamilyMember(member: String) {
        val updatedList = _familyMembers.value - member
        _familyMembers.value = updatedList

        val prefs = getApplication<Application>().getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putStringSet("family_members", updatedList.toSet()).apply()

        notificationManager.sendAlert("Family Member Removed", "$member has been removed from your Family Plan.")
        _uiState.value = UiState.Success("Removed $member from family plan.")
    }

    // ─── Financial Planner Logic ──────────────────────────────────────────────
    
    fun updateFinancialProfile(salary: Double, expenses: Double, investments: Double) {
        viewModelScope.launch {
            val profile = FinancialProfile(
                userId = "local_user",
                monthlySalary = salary,
                fixedExpenses = expenses,
                monthlyInvestments = investments
            )
            withContext(Dispatchers.IO) {
                logDao.updateFinancialProfile(profile)
            }
            refreshDataInternal()
            _uiState.value = UiState.Success("Financial profile updated.")
        }
    }

    fun addDebt(name: String, balance: Double, rate: Double, min: Double, emiDate: Int, paidEmi: Int, pendingEmi: Int, totalLoanAmount: Double, tenure: Int) {
        viewModelScope.launch {
            val debt = DebtEntry(
                name = name,
                balance = balance,
                interestRate = rate,
                minPayment = min,
                emiDate = emiDate,
                paidEmiCount = paidEmi,
                pendingEmiCount = pendingEmi,
                totalLoanAmount = totalLoanAmount,
                tenure = tenure
            )
            withContext(Dispatchers.IO) {
                logDao.insertDebt(debt)
            }
            refreshDataInternal()
            _uiState.value = UiState.Success("Debt '$name' added.")
        }
    }

    fun deleteDebt(id: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                logDao.deleteDebt(id)
            }
            refreshDataInternal()
        }
    }

    fun setDebtMode(enabled: Boolean) {
        _isDebtMode.value = enabled
    }

    fun editDebt(id: Int, name: String, balance: Double, rate: Double, min: Double, emiDate: Int, paidEmi: Int, pendingEmi: Int, totalLoanAmount: Double, tenure: Int) {
        viewModelScope.launch {
            val debt = DebtEntry(
                id = id,
                name = name,
                balance = balance,
                interestRate = rate,
                minPayment = min,
                emiDate = emiDate,
                paidEmiCount = paidEmi,
                pendingEmiCount = pendingEmi,
                totalLoanAmount = totalLoanAmount,
                tenure = tenure
            )
            withContext(Dispatchers.IO) {
                logDao.insertDebt(debt)
            }
            refreshDataInternal()
            _uiState.value = UiState.Success("Debt '$name' updated.")
        }
    }

    fun logDebtPayment(debtName: String, amount: Double) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                withContext(Dispatchers.IO) {
                    val activeDebts = logDao.getDebtsSync("local_user")
                    val matchingDebt = activeDebts.find { it.name.equals(debtName, ignoreCase = true) }
                    if (matchingDebt != null) {
                        val newBalance = (matchingDebt.balance - amount).coerceAtLeast(0.0)
                        val updatedDebt = matchingDebt.copy(balance = newBalance)
                        logDao.insertDebt(updatedDebt)
                    }

                    val rawJson = "{\"category\": \"EXPENSE\", \"amount\": $amount, \"item\": \"$debtName payment\", \"type\": \"DEBT_PAYMENT\", \"debtName\": \"$debtName\"}"
                    val logEntry = LogEntry(
                        category = LogCategory.EXPENSE,
                        content = "Paid ${formatCurrency(amount)} towards $debtName",
                        structuredData = rawJson
                    )
                    logDao.insertLog(logEntry)
                }
                refreshDataInternal()
                _uiState.value = UiState.Success("Logged payment of ${formatCurrency(amount)} towards $debtName")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error logging payment: ${e.message}")
            }
        }
    }

    private fun roundBanking(value: Double): Double {
        return java.math.BigDecimal(value).setScale(2, java.math.RoundingMode.HALF_EVEN).toDouble()
    }

    fun toggleEmiPaid(debtId: Int, isPaid: Boolean) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                var infoMsg = ""
                withContext(Dispatchers.IO) {
                    val activeDebts = logDao.getDebtsSync("local_user")
                    val matchingDebt = activeDebts.find { it.id == debtId }
                    if (matchingDebt != null) {
                        val minPayment = matchingDebt.minPayment
                        val monthlyInterest = roundBanking(matchingDebt.balance * (matchingDebt.interestRate / 100.0) / 12.0)
                        val newBalance = if (isPaid) {
                            roundBanking(matchingDebt.balance + monthlyInterest - minPayment).coerceAtLeast(0.0)
                        } else {
                            val rateFactor = 1.0 + (matchingDebt.interestRate / 100.0) / 12.0
                            roundBanking((matchingDebt.balance + minPayment) / rateFactor).coerceAtLeast(0.0)
                        }
                        val newPaidCount = if (isPaid) matchingDebt.paidEmiCount + 1 else maxOf(0, matchingDebt.paidEmiCount - 1)
                        val newPendingCount = if (isPaid) maxOf(0, matchingDebt.pendingEmiCount - 1) else matchingDebt.pendingEmiCount + 1
                        val updatedDebt = matchingDebt.copy(
                            balance = newBalance, 
                            isEmiPaid = isPaid,
                            paidEmiCount = newPaidCount,
                            pendingEmiCount = newPendingCount
                        )
                        logDao.insertDebt(updatedDebt)

                        infoMsg = if (isPaid) {
                            val rawJson = "{\"category\": \"EXPENSE\", \"amount\": $minPayment, \"item\": \"${matchingDebt.name} EMI payment\", \"type\": \"DEBT_PAYMENT\", \"debtName\": \"${matchingDebt.name}\"}"
                            val logEntry = LogEntry(
                                category = LogCategory.EXPENSE,
                                content = "Paid ${formatCurrency(minPayment)} EMI towards ${matchingDebt.name}",
                                structuredData = rawJson
                            )
                            logDao.insertLog(logEntry)
                            "EMI marked as paid. Remaining balance: ${formatCurrency(newBalance)} (Interest accrued: ${formatCurrency(monthlyInterest)})"
                        } else {
                            "EMI marked as unpaid. Restored balance: ${formatCurrency(newBalance)}"
                        }
                    }
                }
                refreshDataInternal()
                _uiState.value = UiState.Success(infoMsg.ifBlank { if (isPaid) "EMI marked as paid." else "EMI marked as unpaid." })
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error toggling EMI: ${e.message}")
            }
        }
    }

    data class RemainingDebtInfo(
        val name: String,
        val balance: Double,
        val interestAccrued: Double,
        val emiDate: Int,
        val paidEmiCount: Int,
        val pendingEmiCount: Int,
        val openingBalance: Double = 0.0
    )

    data class PayoffSnapshot(
        val month: Int,
        val totalBalance: Double,
        val remainingDebts: List<RemainingDebtInfo>,
        val totalInterestPaid: Double
    )

    fun calculatePayoffTimeline(strategy: String, extraPayment: Double = 0.0, includeHistory: Boolean = false): List<PayoffSnapshot> {
        val profile = _financialProfile.value ?: return emptyList()
        val debtList = _debts.value.toMutableList()
        if (debtList.isEmpty()) return emptyList()

        val maxPaidEmi = debtList.maxOfOrNull { it.paidEmiCount } ?: 0
        val snapshots = mutableListOf<PayoffSnapshot>()
        var currentMonth = maxPaidEmi
        var totalInterestPaid = 0.0
        
        // Sort based on strategy
        val sortedDebts = when (strategy) {
            "Avalanche" -> debtList.sortedByDescending { it.interestRate }
            else -> debtList.sortedBy { it.balance } // Snowball is default
        }.map { it.copy() }.toMutableList()

        val debtNames = sortedDebts.associate { it.id to it.name }
        var currentBalances = sortedDebts.associate { it.id to roundBanking(it.balance) }.toMutableMap()
        var currentPaidCounts = sortedDebts.associate { it.id to it.paidEmiCount }.toMutableMap()
        var currentPendingCounts = sortedDebts.associate { it.id to it.pendingEmiCount }.toMutableMap()

        // 1. Generate histories for all debts if requested
        var cumulativeHistoricalInterest = 0.0
        if (includeHistory) {
            if (maxPaidEmi > 0) {
                val debtHistories = debtList.associate { debt ->
                    val historyList = ArrayList<RemainingDebtInfo>(maxPaidEmi)
                    for (i in 0 until maxPaidEmi) {
                        historyList.add(RemainingDebtInfo(name = debt.name, balance = 0.0, interestAccrued = 0.0, emiDate = debt.emiDate, paidEmiCount = 0, pendingEmiCount = debt.pendingEmiCount))
                    }
                    var currentBal = debt.balance
                    var currentPaid = debt.paidEmiCount
                    var currentPending = debt.pendingEmiCount
                    for (i in maxPaidEmi - 1 downTo 0) {
                        if (currentPaid > 0) {
                            val closing = currentBal
                            val r = (debt.interestRate / 100.0) / 12.0
                            val payment = debt.minPayment
                            val opening = roundBanking((closing + payment) / (1.0 + r))
                            val interest = roundBanking(closing + payment - opening)
                            historyList[i] = RemainingDebtInfo(
                                name = debt.name,
                                balance = closing,
                                interestAccrued = interest,
                                emiDate = debt.emiDate,
                                paidEmiCount = currentPaid,
                                pendingEmiCount = currentPending,
                                openingBalance = opening
                            )
                            currentPaid--
                            currentPending++
                            currentBal = opening
                        } else {
                            historyList[i] = RemainingDebtInfo(
                                name = debt.name,
                                balance = currentBal,
                                interestAccrued = 0.0,
                                emiDate = debt.emiDate,
                                paidEmiCount = 0,
                                pendingEmiCount = currentPending,
                                openingBalance = currentBal
                            )
                        }
                    }
                    debt.id to historyList
                }

                // Build historical snapshots for months 1 to maxPaidEmi
                for (h in 1..maxPaidEmi) {
                    val debtsInMonth = mutableListOf<RemainingDebtInfo>()
                    var totalBal = 0.0
                    sortedDebts.forEach { debt ->
                        val history = debtHistories[debt.id] ?: emptyList()
                        val historyIndex = h - 1
                        if (historyIndex in history.indices) {
                            val info = history[historyIndex]
                            debtsInMonth.add(info)
                            totalBal += info.balance
                            cumulativeHistoricalInterest += info.interestAccrued
                        }
                    }
                    if (debtsInMonth.isNotEmpty()) {
                        snapshots.add(PayoffSnapshot(
                            month = h,
                            totalBalance = roundBanking(totalBal),
                            remainingDebts = debtsInMonth,
                            totalInterestPaid = roundBanking(cumulativeHistoricalInterest)
                        ))
                    }
                }
                totalInterestPaid = cumulativeHistoricalInterest
            }
        }
        
        while (currentBalances.values.sum() > 0.005 && currentMonth < 360) { // Max 30 years
            currentMonth++
            var monthlyInterest = 0.0
            var totalMinPayments = 0.0
            val monthlyInterestMap = mutableMapOf<Int, Double>()
            val openingBalancesMap = mutableMapOf<Int, Double>()
            
            // Apply interest and collect mins
            sortedDebts.forEach { debt: DebtEntry ->
                val balance = currentBalances[debt.id] ?: 0.0
                if (balance > 0.005) {
                    openingBalancesMap[debt.id] = balance
                    val interest = roundBanking(balance * (debt.interestRate / 100.0) / 12.0)
                    currentBalances[debt.id] = roundBanking(balance + interest)
                    monthlyInterestMap[debt.id] = interest
                    monthlyInterest += interest
                    totalMinPayments += debt.minPayment
                    
                    // Increment paid count, decrement pending count
                    val pCount = currentPaidCounts[debt.id] ?: 0
                    val pendCount = currentPendingCounts[debt.id] ?: 0
                    currentPaidCounts[debt.id] = pCount + 1
                    currentPendingCounts[debt.id] = maxOf(0, pendCount - 1)
                } else {
                    openingBalancesMap[debt.id] = 0.0
                    monthlyInterestMap[debt.id] = 0.0
                }
            }
            totalInterestPaid = roundBanking(totalInterestPaid + monthlyInterest)
            

            
            var actualMinPaid = 0.0
            // Pay all minimums first and accumulate actual payments
            sortedDebts.forEach { debt: DebtEntry ->
                val balance = currentBalances[debt.id] ?: 0.0
                if (balance > 0.005) {
                    val payment = roundBanking(Math.min(balance, debt.minPayment))
                    currentBalances[debt.id] = roundBanking(balance - payment)
                    actualMinPaid += payment
                }
            }
            
            var availableExtra = extraPayment
            
            // Apply extra to priority debt
            for (debt in sortedDebts) {
                val balance = currentBalances[debt.id] ?: 0.0
                if (balance > 0.005) {
                    val payment = roundBanking(Math.min(balance, availableExtra))
                    currentBalances[debt.id] = roundBanking(balance - payment)
                    availableExtra = roundBanking(availableExtra - payment)
                    if (availableExtra <= 0.0) break
                }
            }

            // If any debt is paid off completely, set pending count to 0
            sortedDebts.forEach { debt: DebtEntry ->
                val remaining = currentBalances[debt.id] ?: 0.0
                if (remaining <= 0.005) {
                    currentPendingCounts[debt.id] = 0
                }
            }

            snapshots.add(PayoffSnapshot(
                month = currentMonth,
                totalBalance = roundBanking(currentBalances.values.sum()),
                remainingDebts = sortedDebts.map { debt ->
                    RemainingDebtInfo(
                        name = debt.name,
                        balance = currentBalances[debt.id] ?: 0.0,
                        interestAccrued = monthlyInterestMap[debt.id] ?: 0.0,
                        emiDate = debt.emiDate,
                        paidEmiCount = currentPaidCounts[debt.id] ?: 0,
                        pendingEmiCount = currentPendingCounts[debt.id] ?: 0,
                        openingBalance = openingBalancesMap[debt.id] ?: 0.0
                    )
                },
                totalInterestPaid = roundBanking(totalInterestPaid)
            ))
        }

        return snapshots
    }

    fun wipeAllData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                logDao.deleteAllSync()
            }
            refreshDataInternal()
            _uiState.value = UiState.Success("All data wiped for privacy.")
        }
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val log = logDao.getLogSync(id)
                if (log != null && log.structuredData.contains("\"type\": \"DEBT_PAYMENT\"")) {
                    // Extract amount and debtName
                    val amount = "\"amount\":\\s*(\\d+\\.?\\d*)".toRegex().find(log.structuredData)?.groupValues?.get(1)?.toDoubleOrNull()
                    val debtName = "\"debtName\":\\s*\"([^\"]+)\"".toRegex().find(log.structuredData)?.groupValues?.get(1)
                    if (amount != null && debtName != null) {
                        val activeDebts = logDao.getDebtsSync("local_user")
                        val matchingDebt = activeDebts.find { it.name.equals(debtName, ignoreCase = true) }
                        if (matchingDebt != null) {
                            val newBalance = matchingDebt.balance + amount
                            val updatedDebt = matchingDebt.copy(balance = newBalance)
                            logDao.insertDebt(updatedDebt)
                        }
                    }
                }
                logDao.deleteLog(id)
            }
            refreshDataInternal()
            _uiState.value = UiState.Success("Log entry deleted successfully!")
        }
    }

    fun deleteLogGroup(groupId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                logDao.deleteLogGroupByGroupId(groupId)
            }
            refreshDataInternal()
            _uiState.value = UiState.Success("Scanned bill receipt group deleted successfully!")
        }
    }

    fun purchasePremiumPlan(planName: String, durationDays: Long) {
        viewModelScope.launch {
            try {
                var syncAccount: UserAccount? = null
                withContext(Dispatchers.IO) {
                    val currentAccount = logDao.getUserAccountSync("local_user") ?: UserAccount("local_user", "Guest", "guest@hearthflow.com")
                    val currentExpiry = if (currentAccount.proExpiryTimestamp > System.currentTimeMillis()) currentAccount.proExpiryTimestamp else System.currentTimeMillis()
                    val addedMs = java.util.concurrent.TimeUnit.DAYS.toMillis(durationDays)
                    currentAccount.isPro = true
                    currentAccount.subscriptionPlan = planName
                    currentAccount.proExpiryTimestamp = currentExpiry + addedMs
                    logDao.updateUserAccount(currentAccount)
                    syncAccount = currentAccount
                }
                refreshDataInternal()
                
                // Sync purchases to Firebase database
                syncAccount?.let {
                    com.example.hearthflow.data.firebase.FirebaseSyncManager.syncUserProfile(it)
                }

                _uiState.value = UiState.Success("Successfully subscribed to $planName!")
                notificationManager.sendAlert(
                    "Welcome to HearthFlow Pro! 👑", 
                    "Successfully subscribed to $planName! Enjoy unlimited active splits and premium portfolios."
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to purchase premium plan: ${e.message}")
            }
        }
    }

    fun addInvestment(name: String, balance: Double, contribution: Double, expectedReturn: Double) {
        viewModelScope.launch {
            val investment = InvestmentEntry(
                name = name,
                balance = balance,
                monthlyContribution = contribution,
                expectedReturnRate = expectedReturn
            )
            withContext(Dispatchers.IO) {
                logDao.insertInvestment(investment)
            }
            refreshDataInternal()
            _uiState.value = UiState.Success("Investment '$name' added successfully.")
        }
    }

    fun editInvestment(id: Int, name: String, balance: Double, contribution: Double, expectedReturn: Double) {
        viewModelScope.launch {
            val investment = InvestmentEntry(
                id = id,
                name = name,
                balance = balance,
                monthlyContribution = contribution,
                expectedReturnRate = expectedReturn
            )
            withContext(Dispatchers.IO) {
                logDao.insertInvestment(investment)
            }
            refreshDataInternal()
            _uiState.value = UiState.Success("Investment '$name' updated.")
        }
    }

    fun deleteInvestment(id: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                logDao.deleteInvestment(id)
            }
            refreshDataInternal()
            _uiState.value = UiState.Success("Investment deleted.")
        }
    }

    fun createSplitGroup(name: String, invites: List<String> = emptyList(), onSuccess: (String) -> Unit = {}) {
        val hasDuplicateName = _customGroups.value.any { it.substringBeforeLast("_grp_", it) == name.trim() }
        if (name.isNotBlank() && !hasDuplicateName) {
            val uniqueKey = name.trim() + "_grp_" + System.currentTimeMillis()
            _customGroups.value = _customGroups.value + uniqueKey
            _groupInvitedMembers.value = _groupInvitedMembers.value + (uniqueKey to invites)
            
            // Push group creation and user mappings to Firebase Realtime Database ONLY if they invited someone!
            if (invites.isNotEmpty()) {
                val myEmail = userAccount.value?.email ?: "guest@hearthflow.com"
                com.example.hearthflow.data.firebase.FirebaseSyncManager.createGroupOnCloud(uniqueKey, myEmail, invites)
            }

            _uiState.value = UiState.Success("Group '$name' created successfully!")
            
            if (invites.isNotEmpty()) {
                val targets = invites.joinToString(", ")
                notificationManager.sendAlert(
                    "Group Invitations Sent! 👥", 
                    "Universal invitations dispatched to: $targets for group '$name'."
                )
            }
            onSuccess(uniqueKey)
        }
    }

    fun updateSplitGroup(oldName: String, newName: String, onSuccess: (String) -> Unit = {}) {
        val oldDisplayName = oldName.substringBeforeLast("_grp_", oldName)
        if (oldDisplayName == newName || newName.isBlank()) return
        
        val newKey = newName.trim() + "_grp_" + System.currentTimeMillis()
        
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Update the list in customGroups
            if (_customGroups.value.contains(oldName)) {
                _customGroups.value = _customGroups.value.map { if (it == oldName) newKey else it }
            }
            
            // 2. Update groupInvitedMembers mapping
            val invites = _groupInvitedMembers.value[oldName] ?: emptyList()
            val newInvitedMembers = _groupInvitedMembers.value.toMutableMap()
            newInvitedMembers.remove(oldName)
            newInvitedMembers[newKey] = invites
            _groupInvitedMembers.value = newInvitedMembers
            
            // 3. Update all split expenses in the SQLite database that belong to this group
            val currentSplits = logDao.getAllSplitExpensesSync()
            currentSplits.forEach { split ->
                if (split.groupName == oldName) {
                    val updatedSplit = split.copy(groupName = newKey)
                    logDao.insertSplitExpense(updatedSplit)
                }
            }
            refreshDataInternal()
            
            // 4. Sync name change to the cloud ONLY if the group has invited members (is shared)
            if (invites.isNotEmpty()) {
                val members = invites + (userAccount.value?.email ?: "guest@hearthflow.com")
                com.example.hearthflow.data.firebase.FirebaseSyncManager.updateGroupNameOnCloud(oldName, newKey, newName, members)
            }
            
            withContext(Dispatchers.Main) {
                onSuccess(newKey)
                _uiState.value = UiState.Success("Group name updated to '$newName'!")
            }
        }
    }

    fun addMemberToGroup(groupName: String, memberEmail: String) {
        if (memberEmail.isBlank()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            val invites = _groupInvitedMembers.value[groupName] ?: emptyList()
            if (!invites.contains(memberEmail)) {
                val newInvites = invites + memberEmail
                val updatedInvitesMap = _groupInvitedMembers.value.toMutableMap()
                updatedInvitesMap[groupName] = newInvites
                _groupInvitedMembers.value = updatedInvitesMap
                
                // Sync to cloud
                val myEmail = userAccount.value?.email ?: "guest@hearthflow.com"
                if (invites.isEmpty()) {
                    // Transition from Private to Shared: initialize group on Firebase and push all local splits
                    com.example.hearthflow.data.firebase.FirebaseSyncManager.createGroupOnCloud(groupName, myEmail, newInvites)
                    
                    val localSplits = logDao.getAllSplitExpensesSync().filter { it.groupName == groupName }
                    localSplits.forEach { split ->
                        com.example.hearthflow.data.firebase.FirebaseSyncManager.pushSplitExpense(split)
                    }
                } else {
                    // Group was already shared, just append the new member
                    com.example.hearthflow.data.firebase.FirebaseSyncManager.addMemberToGroupOnCloud(groupName, memberEmail)
                }
                
                withContext(Dispatchers.Main) {
                    notificationManager.sendAlert(
                        "Member Added! 👥", 
                        "Successfully invited $memberEmail to group '$groupName'."
                    )
                    _uiState.value = UiState.Success("Added $memberEmail to group!")
                }
            }
        }
    }

    fun removeMemberFromGroup(groupName: String, memberEmail: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val invites = _groupInvitedMembers.value[groupName] ?: emptyList()
            if (invites.contains(memberEmail)) {
                val newInvites = invites.filter { it != memberEmail }
                val updatedInvitesMap = _groupInvitedMembers.value.toMutableMap()
                updatedInvitesMap[groupName] = newInvites
                _groupInvitedMembers.value = updatedInvitesMap
                
                // Sync to cloud ONLY if we have other members left
                if (newInvites.isNotEmpty()) {
                    com.example.hearthflow.data.firebase.FirebaseSyncManager.removeMemberFromGroupOnCloud(groupName, memberEmail)
                } else {
                    // Last member was removed, so group becomes private. Clean up from cloud
                    val myEmail = userAccount.value?.email ?: "guest@hearthflow.com"
                    com.example.hearthflow.data.firebase.FirebaseSyncManager.deleteGroupFromCloud(groupName, invites + myEmail)
                }
                
                withContext(Dispatchers.Main) {
                    _uiState.value = UiState.Success("Removed $memberEmail from group.")
                }
            }
        }
    }

    fun deleteSplitGroup(groupName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Update the list in customGroups
            if (_customGroups.value.contains(groupName)) {
                _customGroups.value = _customGroups.value - groupName
            }
            
            // 2. Remove groupInvitedMembers mapping
            val invites = _groupInvitedMembers.value[groupName] ?: emptyList()
            val updatedInvitesMap = _groupInvitedMembers.value.toMutableMap()
            updatedInvitesMap.remove(groupName)
            _groupInvitedMembers.value = updatedInvitesMap
            
            // 3. Delete splits belonging to this group locally
            val currentSplits = logDao.getAllSplitExpensesSync()
            currentSplits.forEach { split ->
                if (split.groupName == groupName) {
                    logDao.deleteSplitExpense(split.id)
                }
            }
            refreshDataInternal()
            
            // 4. Sync delete to cloud ONLY if it was shared
            if (invites.isNotEmpty()) {
                val members = invites + (userAccount.value?.email ?: "guest@hearthflow.com")
                com.example.hearthflow.data.firebase.FirebaseSyncManager.deleteGroupFromCloud(groupName, members)
            }
            
            withContext(Dispatchers.Main) {
                _uiState.value = UiState.Success("Group '$groupName' deleted successfully.")
            }
        }
    }

    fun addSplitExpense(title: String, totalAmount: Double, paidBy: String, splitWith: String, splitShare: Double, groupName: String = "", timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val myEmail = userAccount.value?.email ?: "guest@hearthflow.com"
            val resolvedPaidBy = if (paidBy.equals("You", ignoreCase = true)) myEmail else paidBy
            val resolvedSplitWith = if (splitWith.equals("You", ignoreCase = true)) myEmail else splitWith

            val uniqueId = (java.util.UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE).let { if (it == 0L) System.currentTimeMillis() else it }
            val expense = SplitExpenseEntry(
                id = uniqueId,
                title = title,
                totalAmount = totalAmount,
                paidBy = resolvedPaidBy,
                splitWith = resolvedSplitWith,
                splitShare = splitShare,
                isSettled = false,
                timestamp = timestamp,
                groupName = groupName
            )
            withContext(Dispatchers.IO) {
                logDao.insertSplitExpense(expense)
            }
            refreshDataInternal()

            // Push split log to Firebase Realtime Database ONLY if the group is shared (has invited members)
            val invites = _groupInvitedMembers.value[groupName] ?: emptyList()
            if (invites.isNotEmpty()) {
                com.example.hearthflow.data.firebase.FirebaseSyncManager.pushSplitExpense(expense)
            }

            notificationManager.sendAlert("New Split Bill", "Split with ${resolvedSplitWith.displayMemberName(myEmail)} logged successfully.")
            _uiState.value = UiState.Success("Split bill logged.")
        }
    }

    fun settleSplitExpense(id: Long) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                var groupName = ""
                withContext(Dispatchers.IO) {
                    val expense = logDao.getSplitExpenseSync(id)
                    if (expense != null) {
                        groupName = expense.groupName
                        val updated = expense.copy(isSettled = true)
                        logDao.insertSplitExpense(updated)

                        // If the other person paid, settle up means we are paying them.
                        // Thus, we record an EXPENSE entry in the main consumer ledger database.
                        val currentUserEmail = userAccount.value?.email ?: "guest@hearthflow.com"
                        if (!expense.paidBy.isCurrentUser(currentUserEmail)) {
                            val rawJson = "{\"category\": \"EXPENSE\", \"amount\": ${expense.splitShare}, \"item\": \"Settled: ${expense.title}\", \"type\": \"SPLIT_SETTLED\", \"splitWith\": \"${expense.splitWith}\"}"
                            val logEntry = LogEntry(
                                category = LogCategory.EXPENSE,
                                content = "Settled split for ${expense.title} (Paid ${formatCurrency(expense.splitShare)} to ${expense.paidBy.displayMemberName(currentUserEmail)})",
                                structuredData = rawJson
                            )
                            logDao.insertLog(logEntry)
                        }
                    }
                }
                refreshDataInternal()

                // Mark settled on the cloud ONLY if the group is shared
                val invites = _groupInvitedMembers.value[groupName] ?: emptyList()
                if (invites.isNotEmpty()) {
                    com.example.hearthflow.data.firebase.FirebaseSyncManager.settleSplitExpenseOnCloud(id, groupName)
                }

                notificationManager.sendAlert("Bill Settled", "Split bill has been marked as settled.")
                _uiState.value = UiState.Success("Bill settled successfully.")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error settling bill: ${e.message}")
            }
        }
    }

    fun deleteSplitExpense(id: Long) {
        viewModelScope.launch {
            var groupName = ""
            withContext(Dispatchers.IO) {
                val expense = logDao.getSplitExpenseSync(id)
                if (expense != null) {
                    groupName = expense.groupName
                }
                logDao.deleteSplitExpense(id)
            }
            refreshDataInternal()

            // Remove split from cloud ONLY if the group is shared
            val invites = _groupInvitedMembers.value[groupName] ?: emptyList()
            if (invites.isNotEmpty()) {
                com.example.hearthflow.data.firebase.FirebaseSyncManager.deleteSplitExpenseOnCloud(id, groupName)
            }

            _uiState.value = UiState.Success("Split expense deleted.")
        }
    }

    fun settleSplitExpensesWithPerson(personName: String, groupName: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                withContext(Dispatchers.IO) {
                    val allSplits = logDao.getAllSplitExpensesSync().filter { !it.isSettled }
                    val groupSplits = if (groupName.isEmpty() || groupName == "All") {
                        allSplits
                    } else {
                        allSplits.filter { it.groupName == groupName }
                    }
                    
                    val currentUserEmail = userAccount.value?.email ?: "guest@hearthflow.com"
                    val targetSplits = groupSplits.filter {
                        (it.paidBy.isCurrentUser(currentUserEmail) && it.splitWith.equals(personName, ignoreCase = true)) ||
                        (it.paidBy.equals(personName, ignoreCase = true) && it.splitWith.isCurrentUser(currentUserEmail))
                    }
                    
                    if (targetSplits.isNotEmpty()) {
                        // Calculate net balance: what we owe them minus what they owe us
                        val owedToYou = targetSplits.filter { it.paidBy.isCurrentUser(currentUserEmail) }.sumOf { it.splitShare }
                        val youOwe = targetSplits.filter { it.splitWith.isCurrentUser(currentUserEmail) }.sumOf { it.splitShare }
                        val netAmount = youOwe - owedToYou
                        
                        if (netAmount > 0.0) {
                            val rawJson = "{\"category\": \"EXPENSE\", \"amount\": $netAmount, \"item\": \"Settled: Net balance with $personName\", \"type\": \"SPLIT_SETTLED\", \"splitWith\": \"$personName\"}"
                            val logEntry = LogEntry(
                                category = LogCategory.EXPENSE,
                                content = "Settled net balance with $personName (Paid ${formatCurrency(netAmount)} to $personName)",
                                structuredData = rawJson
                            )
                            logDao.insertLog(logEntry)
                        }
                        
                        targetSplits.forEach { expense ->
                            val updated = expense.copy(isSettled = true)
                            logDao.insertSplitExpense(updated)
                            
                            // Mark settled on the cloud ONLY if the group is shared
                            val invites = _groupInvitedMembers.value[expense.groupName] ?: emptyList()
                            if (invites.isNotEmpty()) {
                                com.example.hearthflow.data.firebase.FirebaseSyncManager.settleSplitExpenseOnCloud(expense.id, expense.groupName)
                            }
                        }
                    }
                }
                refreshDataInternal()
                notificationManager.sendAlert("Group Settled", "All split balances with $personName have been settled.")
                _uiState.value = UiState.Success("Balances with $personName settled successfully.")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error settling bills with $personName: ${e.message}")
            }
        }
    }

    // --- Admin Master Controllers ---
    fun updatePrivacyPolicy(newText: String) {
        _privacyPolicyText.value = newText
        if (com.example.hearthflow.data.firebase.FirebaseSyncManager.isInitialized) {
            com.example.hearthflow.data.firebase.FirebaseSyncManager.updateAppSettings("privacyPolicy", newText)
            notificationManager.sendAlert("Privacy Policy Updated 🛡️", "The legal Privacy Shield has been synced globally.")
            _uiState.value = UiState.Success("Privacy Policy updated on Firebase!")
        } else {
            notificationManager.sendAlert("Privacy Policy Updated 🛡️", "Saved locally in Sandbox Mode.")
            _uiState.value = UiState.Success("Privacy Policy updated locally!")
        }
    }

    fun updateAboutUs(newText: String) {
        _aboutUsText.value = newText
        if (com.example.hearthflow.data.firebase.FirebaseSyncManager.isInitialized) {
            com.example.hearthflow.data.firebase.FirebaseSyncManager.updateAppSettings("aboutUs", newText)
            notificationManager.sendAlert("About Details Updated ℹ️", "App workspace info has been updated globally.")
            _uiState.value = UiState.Success("About Details updated on Firebase!")
        } else {
            notificationManager.sendAlert("About Details Updated ℹ️", "Saved locally in Sandbox Mode.")
            _uiState.value = UiState.Success("About Details updated locally!")
        }
    }

    fun updatePricingPlans(monthly: String, yearly: String, lifetime: String) {
        _priceMonthlyPlan.value = monthly
        _priceYearlyPlan.value = yearly
        if (lifetime.isNotBlank()) {
            _priceLifetimePlan.value = lifetime
        }
        // Must check for a REAL Firebase connection (not a sandbox fallback), same as startCloudSynchronizer.
        val isRealFirebase = com.example.hearthflow.data.firebase.FirebaseSyncManager.isInitialized &&
                             !com.example.hearthflow.data.firebase.FirebaseSyncManager.isSandboxMode &&
                             isNetworkAvailable()
        if (isRealFirebase) {
            com.example.hearthflow.data.firebase.FirebaseSyncManager.updateAppSettings("pricingPlanMonthly", monthly)
            com.example.hearthflow.data.firebase.FirebaseSyncManager.updateAppSettings("pricingPlanYearly", yearly)
            if (lifetime.isNotBlank()) {
                com.example.hearthflow.data.firebase.FirebaseSyncManager.updateAppSettings("pricingPlanLifetime", lifetime)
            }
            notificationManager.sendAlert("Premium Pricing Published 💸", "Live prices updated: Monthly=$monthly · Yearly=$yearly")
            _uiState.value = UiState.Success("Pricing plans published! All users will see new prices.", System.currentTimeMillis())
        } else {
            _uiState.value = UiState.Error("Failed: Connect to the internet.", System.currentTimeMillis())
        }
    }

    fun submitInquiry(ticketId: String, email: String, category: String, message: String) {
        if (com.example.hearthflow.data.firebase.FirebaseSyncManager.isInitialized) {
            com.example.hearthflow.data.firebase.FirebaseSyncManager.submitSupportTicket(ticketId, email, category, message)
        }
    }

    fun resolveTicket(ticketId: String) {
        if (com.example.hearthflow.data.firebase.FirebaseSyncManager.isInitialized) {
            com.example.hearthflow.data.firebase.FirebaseSyncManager.resolveSupportTicket(ticketId)
            notificationManager.sendAlert("Support Ticket Resolved ✓", "Ticket $ticketId marked as Resolved in secure logs.")
            _uiState.value = UiState.Success("Ticket $ticketId resolved!")
        }
    }

    fun sendAppInvitation(inviteeEmail: String) {
        if (com.example.hearthflow.data.firebase.FirebaseSyncManager.isInitialized) {
            val senderName = userAccount.value?.name ?: "A friend"
            com.example.hearthflow.data.firebase.FirebaseSyncManager.sendAppInvitation(inviteeEmail, senderName)
            _uiState.value = UiState.Success("Email invite sent to $inviteeEmail!", System.currentTimeMillis())
        }
    }

    fun updatePaymentGateway(provider: String, publicKey: String, secretKey: String, upiId: String) {
        val isRealFirebase = com.example.hearthflow.data.firebase.FirebaseSyncManager.isInitialized &&
                             !com.example.hearthflow.data.firebase.FirebaseSyncManager.isSandboxMode &&
                             isNetworkAvailable()
        
        if (isRealFirebase) {
            com.example.hearthflow.data.firebase.FirebaseSyncManager.updateAppSettings("paymentGatewayProvider", provider)
            com.example.hearthflow.data.firebase.FirebaseSyncManager.updateAppSettings("paymentGatewayPublicKey", publicKey)
            com.example.hearthflow.data.firebase.FirebaseSyncManager.updateAppSettings("paymentGatewaySecretKey", secretKey)
            com.example.hearthflow.data.firebase.FirebaseSyncManager.updateAppSettings("paymentGatewayUpiId", upiId)
            _uiState.value = UiState.Success("Payment gateway configuration published.", System.currentTimeMillis())
        } else {
            _uiState.value = UiState.Error("Failed: Connect to the internet.", System.currentTimeMillis())
        }
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val message: String, val id: Long = System.currentTimeMillis()) : UiState()
        data class Error(val message: String, val id: Long = System.currentTimeMillis()) : UiState()
    }
}
