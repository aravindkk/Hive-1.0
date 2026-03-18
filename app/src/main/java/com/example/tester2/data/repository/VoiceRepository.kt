package com.example.tester2.data.repository

import com.example.tester2.data.model.TopicSummary
import com.example.tester2.data.model.VoiceNote
import kotlinx.coroutines.flow.Flow

interface VoiceRepository {
    suspend fun createVoiceNote(storagePath: String, topicId: String? = null): Result<Unit>
    suspend fun transcribeAudio(storagePath: String, lat: Double?, lng: Double?): Result<com.example.tester2.data.model.TranscriptionResult>
    fun getMyVoiceNotes(): Flow<List<VoiceNote>>
    fun getVoiceNotesForTopic(topicId: String): Flow<List<VoiceNote>>
    fun getAudioUrl(storagePath: String): String
    fun getTopicSummaryFlow(topicId: String): Flow<TopicSummary?>
    suspend fun triggerTopicSummary(topicId: String)
}
