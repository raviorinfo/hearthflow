package com.example.omnilog.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory")
data class InventoryItem(
    @PrimaryKey
    var name: String = "",
    var category: String = "General",
    var quantity: Double = 0.0,
    var unit: String = "pcs",
    var minThreshold: Double = 1.0,
    var lastUpdated: Long = System.currentTimeMillis(),
    var daysRemaining: Int = -1
)
