package com.example.tester2.data.repository

import com.example.tester2.data.model.VoiceNote
import kotlinx.coroutines.flow.Flow

interface VoiceRepository {
    suspend fun createVoiceNote(storagePath: String): Result<Unit>
    fun getMyVoiceNotes(): Flow<List<VoiceNote>>
}
