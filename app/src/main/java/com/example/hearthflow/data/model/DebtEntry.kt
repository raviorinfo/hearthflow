package com.example.hearthflow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debts")
data class DebtEntry(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var name: String = "",
    var balance: Double = 0.0,
    var interestRate: Double = 0.0,
    var minPayment: Double = 0.0,
    var userId: String = "local_user",
    var isEmiPaid: Boolean = false,
    var emiDate: Int = 1, // due day of the month (1-31)
    var paidEmiCount: Int = 0,
    var pendingEmiCount: Int = 12,
    var totalLoanAmount: Double = 0.0,
    var tenure: Int = 12
)
