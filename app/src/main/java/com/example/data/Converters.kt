package com.example.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = value.toSyncStatus()
}
