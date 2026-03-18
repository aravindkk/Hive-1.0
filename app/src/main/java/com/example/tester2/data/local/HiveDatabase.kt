package com.example.tester2.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PendingVoiceUpload::class], version = 1, exportSchema = false)
abstract class HiveDatabase : RoomDatabase() {
    abstract fun pendingVoiceDao(): PendingVoiceDao
}
