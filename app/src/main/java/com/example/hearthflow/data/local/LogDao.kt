package com.example.hearthflow.data.local

import androidx.room.*
import com.example.hearthflow.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC")
    fun getAllLogsSync(): List<LogEntry>

    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<LogEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLog(log: LogEntry)

    @Query("DELETE FROM log_entries WHERE id = :id")
    fun deleteLog(id: Long)

    @Query("SELECT * FROM log_entries WHERE id = :id LIMIT 1")
    fun getLogSync(id: Long): LogEntry?

    @Query("DELETE FROM log_entries WHERE groupId = :groupId")
    fun deleteLogGroupByGroupId(groupId: String)

    @Query("SELECT * FROM inventory")
    fun getAllInventorySync(): List<InventoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateInventory(item: InventoryItem)

    @Query("SELECT * FROM inventory WHERE name = :itemName LIMIT 1")
    fun getInventoryItemSync(itemName: String): InventoryItem?

    @Query("SELECT * FROM user_account WHERE userId = :userId LIMIT 1")
    fun getUserAccountSync(userId: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateUserAccount(account: UserAccount)

    @Query("DELETE FROM log_entries")
    fun deleteAllSync()

    @Query("DELETE FROM inventory")
    fun deleteAllInventory()

    @Query("DELETE FROM debts")
    fun deleteAllDebts()

    @Query("DELETE FROM investments")
    fun deleteAllInvestments()

    @Query("DELETE FROM split_expenses")
    fun deleteAllSplitExpenses()

    @Query("DELETE FROM financial_profile")
    fun deleteAllFinancialProfiles()

    @Query("DELETE FROM user_account")
    fun deleteAllUserAccounts()

    // Financial Planner Methods
    @Query("SELECT * FROM debts WHERE userId = :userId")
    fun getDebtsSync(userId: String): List<DebtEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDebt(debt: DebtEntry)

    @Query("DELETE FROM debts WHERE id = :debtId")
    fun deleteDebt(debtId: Int)

    @Query("SELECT * FROM financial_profile WHERE userId = :userId LIMIT 1")
    fun getFinancialProfileSync(userId: String): FinancialProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateFinancialProfile(profile: FinancialProfile)

    @Query("SELECT * FROM investments WHERE userId = :userId")
    fun getInvestmentsSync(userId: String): List<InvestmentEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertInvestment(investment: InvestmentEntry)

    @Query("DELETE FROM investments WHERE id = :investmentId")
    fun deleteInvestment(investmentId: Int)

    // Split Expense Methods
    @Query("SELECT * FROM split_expenses ORDER BY timestamp DESC")
    fun getAllSplitExpensesSync(): List<SplitExpenseEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSplitExpense(expense: SplitExpenseEntry): Long

    @Query("DELETE FROM split_expenses WHERE id = :id")
    fun deleteSplitExpense(id: Long)

    @Query("SELECT * FROM split_expenses WHERE id = :id LIMIT 1")
    fun getSplitExpenseSync(id: Long): SplitExpenseEntry?
}
