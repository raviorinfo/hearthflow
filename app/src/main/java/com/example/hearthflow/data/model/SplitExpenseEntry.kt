package com.example.hearthflow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "split_expenses")
data class SplitExpenseEntry(
    @PrimaryKey(autoGenerate = false)
    var id: Long = 0,
    var title: String = "",
    var totalAmount: Double = 0.0,
    var paidBy: String = "You",
    var splitWith: String = "",
    var splitShare: Double = 0.0,
    var isSettled: Boolean = false,
    var timestamp: Long = System.currentTimeMillis(),
    var groupName: String = ""
)

fun String.isCurrentUser(currentUserEmail: String): Boolean {
    return this.trim().equals("You", ignoreCase = true) || this.trim().equals(currentUserEmail.trim(), ignoreCase = true)
}

fun String.displayMemberName(currentUserEmail: String): String {
    return if (this.isCurrentUser(currentUserEmail)) "You" else this
}
