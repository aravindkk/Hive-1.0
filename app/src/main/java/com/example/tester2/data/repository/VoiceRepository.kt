package com.example.tester2.data.repository

import com.example.tester2.data.model.VoiceNote
import kotlinx.coroutines.flow.Flow

interface VoiceRepository {
    suspend fun createVoiceNote(storagePath: String, topicId: String? = null): Result<Unit>
    suspend fun transcribeAudio(storagePath: String): Result<Unit>
    fun getMyVoiceNotes(): Flow<List<VoiceNote>>
    fun getVoiceNotesForTopic(topicId: String): Flow<List<VoiceNote>>
    fun getAudioUrl(storagePath: String): String
}
