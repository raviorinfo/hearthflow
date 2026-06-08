package com.example.hearthflow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_account")
data class UserAccount(
    @PrimaryKey
    var userId: String = "local_user",
    var name: String = "User",
    var email: String = "",
    var mobileNumber: String = "",
    var isPro: Boolean = false,
    var aiCredits: Int = 10,
    var biometricEnabled: Boolean = false,
    var profileImageUri: String? = null,
    var proteinGoal: Int = 150,
    var carbsGoal: Int = 200,
    var fatGoal: Int = 70,
    var subscriptionPlan: String? = null,
    var proExpiryTimestamp: Long = 0L
)
