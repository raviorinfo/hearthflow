package com.example.omnilog.data.model

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
    var userId: String = "local_user"
)
