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
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.example.omnilog.data.model.*
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

    init {
        refreshData()
    }

    private fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            _logs.value = logDao.getAllLogsSync()
            _inventory.value = logDao.getAllInventorySync()
            _debts.value = logDao.getDebtsSync("local_user")
            _financialProfile.value = logDao.getFinancialProfileSync("local_user")
            val account = logDao.getUserAccountSync("local_user")
            // Ensure we have a local_user if it's missing
            if (account == null) {
                logDao.updateUserAccount(UserAccount("local_user", "Guest", "guest@omnilog.com"))
            }
            _userAccount.value = logDao.getUserAccountSync("local_user")
        }
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _mealSuggestion = MutableStateFlow<String?>(null)
    val mealSuggestion: StateFlow<String?> = _mealSuggestion

    // Multimodal model
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = "AIzaSyDvzLcgZTmMbnBQlmbimzKHR_Cjk1Ygv-I" 
    )

    fun suggestMeal() {
        val currentInventory = inventory.value
        if (currentInventory.isEmpty()) {
            _mealSuggestion.value = "Your pantry is empty! Log some groceries first."
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val inventoryList = currentInventory.joinToString { "${it.name} (${it.quantity} ${it.unit})" }
                val prompt = """
                    Based on these ingredients available in my house: $inventoryList
                    Suggest 2 quick healthy meal recipes. 
                    Format: Meal Name followed by a brief 1-line instruction.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                _mealSuggestion.value = response.text
                _uiState.value = UiState.Idle
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Could not get suggestions: ${e.message}")
            }
        }
    }

    fun registerUser(name: String, email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // userId must be "local_user" so it matches the DAO query getUserAccount("local_user")
            val newUser = UserAccount("local_user", name, email)
            logDao.updateUserAccount(newUser)
            refreshData()
            _uiState.value = UiState.Success("Welcome, $name!")
        }
    }

    fun updateProfile(updatedAccount: UserAccount) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                logDao.updateUserAccount(updatedAccount)
            }
            refreshData()
            _uiState.value = UiState.Success("Profile updated.")
        }
    }

    fun generateExpenseReport(): String {
        val logs = allLogs.value.filter { it.category == LogCategory.EXPENSE }
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

    fun processInput(input: String, image: Bitmap? = null) {
        if (input.isBlank() && image == null) return
        
        viewModelScope.launch {
            val currentAccount = userAccount.value ?: UserAccount()
            if (!currentAccount.isPro && currentAccount.aiCredits <= 0) {
                _uiState.value = UiState.Error("Daily AI limit reached. Upgrade to Pro for unlimited logging!")
                return@launch
            }

            _uiState.value = UiState.Loading
            try {
                // LOCAL REGEX FALLBACK for Pantry/Expenses
                // Pattern: "Add 3 apples", "Bought 10 milk", "Spent 50 on lunch"
                val pantryRegex = "(?i)(?:add|bought|plus)\\s+(\\d+(?:\\.\\d+)?)\\s+([a-zA-Z\\s]+)".toRegex()
                val expenseRegex = "(?i)(?:spent|spent|expense)\\s+(\\d+(?:\\.\\d+)?)\\s+(?:on|for)\\s+([a-zA-Z\\s]+)".toRegex()

                val pantryMatch = pantryRegex.find(input)
                val expenseMatch = expenseRegex.find(input)

                if (pantryMatch != null) {
                    val qty = pantryMatch.groupValues[1].toDouble()
                    val item = pantryMatch.groupValues[2].trim()
                    val fallbackJson = "{\"category\": \"INVENTORY\", \"item\": \"$item\", \"quantity\": $qty, \"sentiment\": \"Neutral\"}"
                    handleProcessedResult(LogCategory.INVENTORY, input, fallbackJson)
                    return@launch
                } else if (expenseMatch != null) {
                    val amt = expenseMatch.groupValues[1].toDouble()
                    val desc = expenseMatch.groupValues[2].trim()
                    val fallbackJson = "{\"category\": \"EXPENSE\", \"amount\": $amt, \"item\": \"$desc\", \"sentiment\": \"Neutral\"}"
                    handleProcessedResult(LogCategory.EXPENSE, input, fallbackJson)
                    return@launch
                }

                // AI Processing as secondary
                val promptText = if (image != null) {
                    "Analyze this image and text: '$input'. Categorize as EXPENSE, INVENTORY, or CONSUMPTION. " +
                    "Return ONLY valid JSON with keys: 'category', 'item', 'quantity' (number), 'amount' (number), 'sentiment'. " +
                    "Example: {\"category\": \"INVENTORY\", \"item\": \"milk\", \"quantity\": 2, \"sentiment\": \"Neutral\"}"
                } else {
                    "Categorize this input: '$input'. Return ONLY valid JSON with keys: 'category', 'item', 'quantity', 'amount', 'sentiment'. " +
                    "Categories: EXPENSE, INVENTORY, CONSUMPTION."
                }

                val response = if (image != null) {
                    generativeModel.generateContent(content { image(image); text(promptText) })
                } else {
                    generativeModel.generateContent(promptText)
                }

                val responseText = response.text ?: ""
                
                val category = when {
                    responseText.contains("EXPENSE", ignoreCase = true) -> LogCategory.EXPENSE
                    responseText.contains("INVENTORY", ignoreCase = true) -> LogCategory.INVENTORY
                    responseText.contains("CONSUMPTION", ignoreCase = true) -> LogCategory.CONSUMPTION
                    else -> LogCategory.UNKNOWN
                }

                handleProcessedResult(category, if (image != null) "Image Log: $input" else input, responseText)
                
                // Update AI credits only if AI was actually used and not PRO
                if (!currentAccount.isPro) {
                    val updatedAccount = currentAccount.copy(aiCredits = (currentAccount.aiCredits - 1).coerceAtLeast(0))
                    withContext(Dispatchers.IO) {
                        logDao.updateUserAccount(updatedAccount)
                    }
                    refreshData()
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
            refreshData()
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
            refreshData()
            _uiState.value = UiState.Success("Restocked $name and logged expense.")
        }
    }

    fun purchaseCredits(amount: Int) {
        viewModelScope.launch {
            val currentAccount = userAccount.value ?: return@launch
            val updatedAccount = currentAccount.copy(aiCredits = currentAccount.aiCredits + amount)
            withContext(Dispatchers.IO) {
                logDao.updateUserAccount(updatedAccount)
            }
            refreshData()
            _uiState.value = UiState.Success("Successfully added $amount credits!")
        }
    }

    fun upgradeToPro() {
        viewModelScope.launch {
            val currentAccount = userAccount.value ?: return@launch
            val updatedAccount = UserAccount(currentAccount.userId, currentAccount.name, currentAccount.email).apply {
                profileImageUri = currentAccount.profileImageUri
                proteinGoal = currentAccount.proteinGoal
                carbsGoal = currentAccount.carbsGoal
                fatGoal = currentAccount.fatGoal
                aiCredits = currentAccount.aiCredits
                isPro = true
            }
            withContext(Dispatchers.IO) {
                logDao.updateUserAccount(updatedAccount)
            }
            refreshData()
            _uiState.value = UiState.Success("Welcome to RoutineLog Pro!")
        }
    }

    fun updateMacroGoals(p: Int, c: Int, f: Int) {
        viewModelScope.launch {
            val currentAccount = userAccount.value ?: return@launch
            val updatedAccount = currentAccount.copy(proteinGoal = p, carbsGoal = c, fatGoal = f)
            withContext(Dispatchers.IO) {
                logDao.updateUserAccount(updatedAccount)
            }
            refreshData()
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
            refreshData()
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
            refreshData()

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

    // ─── Financial Planner Logic ──────────────────────────────────────────────
    
    fun updateFinancialProfile(salary: Double, expenses: Double) {
        viewModelScope.launch {
            val profile = FinancialProfile(userId = "local_user", monthlySalary = salary, fixedExpenses = expenses)
            withContext(Dispatchers.IO) {
                logDao.updateFinancialProfile(profile)
            }
            refreshData()
            _uiState.value = UiState.Success("Financial profile updated.")
        }
    }

    fun addDebt(name: String, balance: Double, rate: Double, min: Double) {
        viewModelScope.launch {
            val debt = DebtEntry(name = name, balance = balance, interestRate = rate, minPayment = min)
            withContext(Dispatchers.IO) {
                logDao.insertDebt(debt)
            }
            refreshData()
            _uiState.value = UiState.Success("Debt '$name' added.")
        }
    }

    fun deleteDebt(id: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                logDao.deleteDebt(id)
            }
            refreshData()
        }
    }

    data class PayoffSnapshot(
        val month: Int,
        val totalBalance: Double,
        val remainingDebts: List<Pair<String, Double>>,
        val totalInterestPaid: Double
    )

    fun calculatePayoffTimeline(strategy: String): List<PayoffSnapshot> {
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
                    currentBalances[debt.name] = (currentBalances[debt.name] ?: 0.0) + interest
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
            
            var availableExtra = discretionary - totalMinPayments
            
            // Pay all minimums first
            sortedDebts.forEach { debt: DebtEntry ->
                val balance = currentBalances[debt.name] ?: 0.0
                if (balance > 0) {
                    val payment = Math.min(balance, debt.minPayment)
                    currentBalances[debt.name] = (currentBalances[debt.name] ?: 0.0) - payment
                }
            }
            
            // Apply extra to priority debt
            for (debt in sortedDebts) {
                val balance = currentBalances[debt.name] ?: 0.0
                if (balance > 0) {
                    val payment = Math.min(balance, availableExtra)
                    currentBalances[debt.name] = (currentBalances[debt.name] ?: 0.0) - payment
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
            refreshData()
            _uiState.value = UiState.Success("All data wiped for privacy.")
        }
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }
}
