package com.example.hearthflow

import android.app.Application
import com.example.hearthflow.data.firebase.FirebaseSyncManager

class HearthFlowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase safely programmatically with sandbox fallback options
        FirebaseSyncManager.initialize(this)
    }
}
