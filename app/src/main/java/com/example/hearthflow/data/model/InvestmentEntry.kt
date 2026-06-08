package com.example.hearthflow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investments")
data class InvestmentEntry(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var name: String = "",
    var balance: Double = 0.0,
    var monthlyContribution: Double = 0.0,
    var expectedReturnRate: Double = 0.0,
    var userId: String = "local_user"
)
