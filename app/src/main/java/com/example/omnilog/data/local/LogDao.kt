package com.example.omnilog.data.local

import androidx.room.*
import com.example.omnilog.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC")
    fun getAllLogsSync(): List<LogEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLog(log: LogEntry)

    @Query("DELETE FROM log_entries WHERE id = :id")
    fun deleteLog(id: Long)

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
