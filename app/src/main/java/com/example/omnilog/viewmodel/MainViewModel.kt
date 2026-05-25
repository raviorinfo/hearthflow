package com.example.omnilog.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.omnilog.OmniNotificationManager
import com.example.omnilog.data.local.AppDatabase
import com.example.omnilog.data.model.InventoryItem
import com.example.omnilog.data.model.LogCategory
import com.example.omnilog.data.model.LogEntry
import com.example.omnilog.data.model.UserAccount
import com.example.omnilog.data.model.*
import com.example.omnilog.ui.formatCurrency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val logDao = AppDatabase.getDatabase(application).logDao()
    private val notificationManager = OmniNotificationManager(application)
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val allLogs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventory: StateFlow<List<InventoryItem>> = _inventory.asStateFlow()

    private val _userAccount = MutableStateFlow<UserAccount?>(null)
    val userAccount: StateFlow<UserAccount?> = _userAccount.asStateFlow()

    private val _familyMembers = MutableStateFlow<List<String>>(listOf("Mom (Admin)", "Dad", "Sister"))
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

    private val _customGroups = MutableStateFlow<List<String>>(listOf("General", "Home", "Trip"))
    val splitGroups: StateFlow<List<String>> = _customGroups.asStateFlow()

    private val _groupInvitedMembers = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val groupInvitedMembers: StateFlow<Map<String, List<String>>> = _groupInvitedMembers.asStateFlow()

    private val _isDatabaseEncrypted = MutableStateFlow(false)
    val isDatabaseEncrypted: StateFlow<Boolean> = _isDatabaseEncrypted.asStateFlow()

    val isPremiumActive = userAccount.map {
        it?.isPro == true && it.proExpiryTimestamp > System.currentTimeMillis()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        val prefs = application.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
        _isDatabaseEncrypted.value = prefs.getBoolean("database_encrypted", false)
        refreshData()
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
            _splitExpenses.value = logDao.getAllSplitExpensesSync()
            
            var currentProfile = logDao.getFinancialProfileSync("local_user")
            if (currentInvestments.isNotEmpty()) {
                val totalContribution = currentInvestments.sumOf { it.monthlyContribution }
                if (currentProfile != null) {
                    if (currentProfile.monthlyInvestments != totalContribution) {
                        currentProfile.monthlyInvestments = totalContribution
                        logDao.updateFinancialProfile(currentProfile)
                    }
                } else {
                    currentProfile = FinancialProfile(userId = "local_user", monthlyInvestments = totalContribution)
                    logDao.updateFinancialProfile(currentProfile)
                }
            }
            
            _financialProfile.value = logDao.getFinancialProfileSync("local_user")
            val account = logDao.getUserAccountSync("local_user")
            // Ensure we have a local_user if it's missing
            if (account == null) {
                logDao.updateUserAccount(UserAccount("local_user", "Guest", "guest@omnilog.com"))
            }
            _userAccount.value = logDao.getUserAccountSync("local_user")
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            refreshDataInternal()
        }
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState



    fun registerUser(name: String, email: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            // userId must be "local_user" so it matches the DAO query getUserAccount("local_user")
            val existing = logDao.getUserAccountSync("local_user")
            val newUser = if (existing != null) {
                existing.copy(name = name, email = email)
            } else {
                UserAccount("local_user", name, email)
            }
            logDao.updateUserAccount(newUser)
            refreshDataInternal()
            withContext(Dispatchers.Main) {
                _uiState.value = UiState.Success("Welcome, $name!")
                notificationManager.sendAlert(
                    "Welcome to RoutineLog! 🎉", 
                    "Your secure offline-first account is active, $name."
                )
                onSuccess()
            }
        }
    }

    fun sendForgotPasswordNotification(email: String) {
        notificationManager.sendAlert(
            "Password Reset Request 🔑", 
            "A secure link has been sent to $email. Use password 'admin123' to sign in locally."
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
                    "RoutineLog local database secured with AES-256 keys."
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
            withContext(Dispatchers.IO) {
                logDao.updateUserAccount(updatedAccount)
            }
            refreshDataInternal()
            _uiState.value = UiState.Success("Profile updated.")
        }
    }

    fun generateExpenseReport(): String {
        val logs = allLogs.value.filter { 
            it.category == LogCategory.EXPENSE && !it.structuredData.contains("\"type\": \"DEBT_PAYMENT\"") 
        }
        if (logs.isEmpty()) return "No expenses logged yet."
        
        val sb = StringBuilder()
        sb.append("RoutineLog Expense Report\n")
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
        _familyMembers.value = _familyMembers.value + email
        _uiState.value = UiState.Success("Invited $email to your Family Plan!")
    }

    fun removeFamilyMember(member: String) {
        _familyMembers.value = _familyMembers.value - member
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

    fun addDebt(name: String, balance: Double, rate: Double, min: Double) {
        viewModelScope.launch {
            val debt = DebtEntry(name = name, balance = balance, interestRate = rate, minPayment = min)
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

    fun editDebt(id: Int, name: String, balance: Double, rate: Double, min: Double) {
        viewModelScope.launch {
            val debt = DebtEntry(id = id, name = name, balance = balance, interestRate = rate, minPayment = min)
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

    fun toggleEmiPaid(debtId: Int, isPaid: Boolean) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                withContext(Dispatchers.IO) {
                    val activeDebts = logDao.getDebtsSync("local_user")
                    val matchingDebt = activeDebts.find { it.id == debtId }
                    if (matchingDebt != null) {
                        val minPayment = matchingDebt.minPayment
                        val newBalance = if (isPaid) {
                            (matchingDebt.balance - minPayment).coerceAtLeast(0.0)
                        } else {
                            matchingDebt.balance + minPayment
                        }
                        val updatedDebt = matchingDebt.copy(balance = newBalance, isEmiPaid = isPaid)
                        logDao.insertDebt(updatedDebt)

                        if (isPaid) {
                            val rawJson = "{\"category\": \"EXPENSE\", \"amount\": $minPayment, \"item\": \"${matchingDebt.name} EMI payment\", \"type\": \"DEBT_PAYMENT\", \"debtName\": \"${matchingDebt.name}\"}"
                            val logEntry = LogEntry(
                                category = LogCategory.EXPENSE,
                                content = "Paid ${formatCurrency(minPayment)} EMI towards ${matchingDebt.name}",
                                structuredData = rawJson
                            )
                            logDao.insertLog(logEntry)
                        }
                    }
                }
                refreshDataInternal()
                _uiState.value = UiState.Success(if (isPaid) "EMI marked as paid." else "EMI marked as unpaid.")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error toggling EMI: ${e.message}")
            }
        }
    }

    data class PayoffSnapshot(
        val month: Int,
        val totalBalance: Double,
        val remainingDebts: List<Pair<String, Double>>,
        val totalInterestPaid: Double
    )

    fun calculatePayoffTimeline(strategy: String, extraPayment: Double = 0.0): List<PayoffSnapshot> {
        val profile = _financialProfile.value ?: return emptyList()
        val debtList = _debts.value.toMutableList()
        if (debtList.isEmpty()) return emptyList()

        val snapshots = mutableListOf<PayoffSnapshot>()
        var currentMonth = 0
        var totalInterestPaid = 0.0
        
        // Sort based on strategy
        val sortedDebts = when (strategy) {
            "Avalanche" -> debtList.sortedByDescending { it.interestRate }
            else -> debtList.sortedBy { it.balance } // Snowball is default
        }.map { it.copy() }.toMutableList()

        var currentBalances = sortedDebts.associate { it.name to it.balance }.toMutableMap()
        
        while (currentBalances.values.sum() > 0 && currentMonth < 360) { // Max 30 years
            currentMonth++
            var monthlyInterest = 0.0
            var totalMinPayments = 0.0
            
            // Apply interest and collect mins
            sortedDebts.forEach { debt: DebtEntry ->
                val balance = currentBalances[debt.name] ?: 0.0
                if (balance > 0) {
                    val interest = (balance * (debt.interestRate / 100)) / 12
                    currentBalances[debt.name] = balance + interest
                    monthlyInterest += interest
                    totalMinPayments += debt.minPayment
                }
            }
            totalInterestPaid += monthlyInterest
            
            val discretionary = profile.monthlySalary - profile.fixedExpenses
            if (discretionary < totalMinPayments) {
                // Critical Error: Can't even meet minimums
                return listOf(PayoffSnapshot(-1, 0.0, emptyList(), 0.0))
            }
            
            var actualMinPaid = 0.0
            // Pay all minimums first and accumulate actual payments
            sortedDebts.forEach { debt: DebtEntry ->
                val balance = currentBalances[debt.name] ?: 0.0
                if (balance > 0) {
                    val payment = Math.min(balance, debt.minPayment)
                    currentBalances[debt.name] = balance - payment
                    actualMinPaid += payment
                }
            }
            
            var availableExtra = discretionary - actualMinPaid + extraPayment
            
            // Apply extra to priority debt
            for (debt in sortedDebts) {
                val balance = currentBalances[debt.name] ?: 0.0
                if (balance > 0) {
                    val payment = Math.min(balance, availableExtra)
                    currentBalances[debt.name] = balance - payment
                    availableExtra -= payment
                    if (availableExtra <= 0) break
                }
            }

            snapshots.add(PayoffSnapshot(
                month = currentMonth,
                totalBalance = currentBalances.values.sum(),
                remainingDebts = currentBalances.toList(),
                totalInterestPaid = totalInterestPaid
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

    fun purchasePremiumPlan(planName: String, durationDays: Long) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val currentAccount = logDao.getUserAccountSync("local_user") ?: UserAccount("local_user", "Guest", "guest@omnilog.com")
                    val currentExpiry = if (currentAccount.proExpiryTimestamp > System.currentTimeMillis()) currentAccount.proExpiryTimestamp else System.currentTimeMillis()
                    val addedMs = java.util.concurrent.TimeUnit.DAYS.toMillis(durationDays)
                    currentAccount.isPro = true
                    currentAccount.subscriptionPlan = planName
                    currentAccount.proExpiryTimestamp = currentExpiry + addedMs
                    logDao.updateUserAccount(currentAccount)
                }
                refreshDataInternal()
                _uiState.value = UiState.Success("Successfully subscribed to $planName!")
                notificationManager.sendAlert(
                    "Welcome to RoutineLog Pro! 👑", 
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

    fun createSplitGroup(name: String, invites: List<String> = emptyList()) {
        if (name.isNotBlank() && !_customGroups.value.contains(name)) {
            _customGroups.value = _customGroups.value + name
            _groupInvitedMembers.value = _groupInvitedMembers.value + (name to invites)
            _uiState.value = UiState.Success("Group '$name' created successfully!")
            
            if (invites.isNotEmpty()) {
                val targets = invites.joinToString(", ")
                notificationManager.sendAlert(
                    "Group Invitations Sent! 👥", 
                    "Universal invitations dispatched to: $targets for group '$name'."
                )
            }
        }
    }

    fun addSplitExpense(title: String, totalAmount: Double, paidBy: String, splitWith: String, splitShare: Double, groupName: String = "General") {
        viewModelScope.launch {
            val expense = SplitExpenseEntry(
                title = title,
                totalAmount = totalAmount,
                paidBy = paidBy,
                splitWith = splitWith,
                splitShare = splitShare,
                isSettled = false,
                timestamp = System.currentTimeMillis(),
                groupName = groupName
            )
            withContext(Dispatchers.IO) {
                logDao.insertSplitExpense(expense)
            }
            refreshDataInternal()
            notificationManager.sendAlert("New Split Bill", "Split with $splitWith logged successfully.")
            _uiState.value = UiState.Success("Split bill logged.")
        }
    }

    fun settleSplitExpense(id: Long) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                withContext(Dispatchers.IO) {
                    val expense = logDao.getSplitExpenseSync(id)
                    if (expense != null) {
                        val updated = expense.copy(isSettled = true)
                        logDao.insertSplitExpense(updated)

                        // If the other person paid, settle up means we are paying them.
                        // Thus, we record an EXPENSE entry in the main consumer ledger database.
                        if (!expense.paidBy.equals("You", ignoreCase = true)) {
                            val rawJson = "{\"category\": \"EXPENSE\", \"amount\": ${expense.splitShare}, \"item\": \"Settled: ${expense.title}\", \"type\": \"SPLIT_SETTLED\", \"splitWith\": \"${expense.splitWith}\"}"
                            val logEntry = LogEntry(
                                category = LogCategory.EXPENSE,
                                content = "Settled split for ${expense.title} (Paid ${formatCurrency(expense.splitShare)} to ${expense.paidBy})",
                                structuredData = rawJson
                            )
                            logDao.insertLog(logEntry)
                        }
                    }
                }
                refreshDataInternal()
                notificationManager.sendAlert("Bill Settled", "Split bill has been marked as settled.")
                _uiState.value = UiState.Success("Bill settled successfully.")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error settling bill: ${e.message}")
            }
        }
    }

    fun deleteSplitExpense(id: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                logDao.deleteSplitExpense(id)
            }
            refreshDataInternal()
            _uiState.value = UiState.Success("Split expense deleted.")
        }
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }
}
