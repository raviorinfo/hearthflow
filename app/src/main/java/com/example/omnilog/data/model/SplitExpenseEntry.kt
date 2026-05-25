package com.example.omnilog.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "split_expenses")
data class SplitExpenseEntry(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var title: String = "",
    var totalAmount: Double = 0.0,
    var paidBy: String = "You",
    var splitWith: String = "",
    var splitShare: Double = 0.0,
    var isSettled: Boolean = false,
    var timestamp: Long = System.currentTimeMillis(),
    var groupName: String = "General"
)
