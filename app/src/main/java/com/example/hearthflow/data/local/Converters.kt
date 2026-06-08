package com.example.hearthflow.data.local

import androidx.room.TypeConverter
import com.example.hearthflow.data.model.LogCategory

class Converters {
    @TypeConverter
    fun fromLogCategory(category: LogCategory?): String? {
        return category?.name
    }

    @TypeConverter
    fun toLogCategory(value: String?): LogCategory {
        if (value == null) return LogCategory.GENERAL
        return try {
            LogCategory.valueOf(value)
        } catch (e: IllegalArgumentException) {
            LogCategory.UNKNOWN
        }
    }
}
