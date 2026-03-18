package com.example.tester2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// Status progression: PENDING → UPLOADING → PROCESSING → COMPLETE / FAILED
@Entity(tableName = "pending_voice_uploads")
data class PendingVoiceUpload(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val topicId: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)
