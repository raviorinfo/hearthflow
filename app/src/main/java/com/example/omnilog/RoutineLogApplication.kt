package com.example.omnilog

import android.app.Application
import com.example.omnilog.data.firebase.FirebaseSyncManager

class RoutineLogApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase safely programmatically with sandbox fallback options
        FirebaseSyncManager.initialize(this)
    }
}
