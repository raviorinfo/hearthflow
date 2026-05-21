package com.example.omnilog.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log_entries")
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var category: LogCategory = LogCategory.GENERAL,
    var content: String = "",
    var structuredData: String = "{}",
    var timestamp: Long = System.currentTimeMillis()
)
