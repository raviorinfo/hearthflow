package com.example.omnilog.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_profile")
data class FinancialProfile(
    @PrimaryKey
    var userId: String = "local_user",
    var monthlySalary: Double = 0.0,
    var fixedExpenses: Double = 0.0
)
