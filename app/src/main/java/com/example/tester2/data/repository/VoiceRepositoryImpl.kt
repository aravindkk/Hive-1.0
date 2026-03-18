package com.example.tester2.data.repository

import com.example.tester2.data.model.VoiceNote
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject
import io.ktor.client.request.setBody
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import com.example.tester2.data.model.TopicSummary
import com.example.tester2.data.model.TranscriptionResult
import javax.inject.Inject

class VoiceRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : VoiceRepository {

    override suspend fun createVoiceNote(storagePath: String, topicId: String?): Result<Unit> {
        return try {
            val user = supabase.auth.currentUserOrNull() ?: throw IllegalStateException("User not logged in")
            val username = user.userMetadata?.get("username")?.toString()?.removeSurrounding("\"")

            val voiceNote = buildJsonObject {
                put("user_id", user.id)
                put("storage_path", storagePath)
                if (topicId != null) put("topic_id", topicId)
                if (username != null) put("username", username)
            }

            supabase.from("voices").insert(voiceNote)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getMyVoiceNotes(): Flow<List<VoiceNote>> = flow {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return@flow emit(emptyList())

        // Initial fetch
        emit(fetchVoices(userId = userId))

        // Realtime subscription
        try {
            val channel = supabase.realtime.channel("voices-realtime-user-$userId")
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "voices"
                filter("user_id", FilterOperator.EQ, userId)
            }
            channel.subscribe()
            
            changeFlow.collect {
                // On any change, re-fetch list
                emit(fetchVoices(userId = userId))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getVoiceNotesForTopic(topicId: String): Flow<List<VoiceNote>> = flow {
         // Initial fetch
        emit(fetchVoices(topicId = topicId))

        // Realtime subscription
        try {
            val channel = supabase.realtime.channel("voices-realtime-topic-$topicId")
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "voices"
                filter("topic_id", FilterOperator.EQ, topicId)
            }
            channel.subscribe()
            
            changeFlow.collect {
                // On any change, re-fetch list
                emit(fetchVoices(topicId = topicId))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun fetchVoices(userId: String? = null, topicId: String? = null): List<VoiceNote> {
        return try {
            supabase.from("voices").select(io.github.jan.supabase.postgrest.query.Columns.raw("*, topics(title)")) {
                filter {
                    if (userId != null) eq("user_id", userId)
                    if (topicId != null) eq("topic_id", topicId)
                }
                order("created_at", Order.DESCENDING)
            }.decodeList<VoiceNote>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun transcribeAudio(storagePath: String, lat: Double?, lng: Double?, areaName: String?, topicId: String?): Result<TranscriptionResult> {
        return try {
            val response = supabase.functions.invoke("transcribe-audio") {
                headers {
                    append(io.ktor.http.HttpHeaders.ContentType, io.ktor.http.ContentType.Application.Json.toString())
                }
                setBody(buildJsonObject {
                    put("storage_path", storagePath)
                    if (lat != null) put("lat", lat)
                    if (lng != null) put("lng", lng)
                    if (!areaName.isNullOrBlank()) put("area_name", areaName)
                    if (topicId != null) put("topic_id", topicId)
                })
            }
            val json = Json { ignoreUnknownKeys = true }
            val result = json.decodeFromString<TranscriptionResult>(response.bodyAsText())
            Result.success(result)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override fun getAudioUrl(storagePath: String): String {
        val host = supabase.supabaseUrl.removePrefix("https://").removePrefix("http://")
        return "https://$host/storage/v1/object/public/audio-notes/$storagePath"
    }

    override fun getTopicSummaryFlow(topicId: String): Flow<TopicSummary?> = flow {
        // Initial fetch
        emit(fetchSummary(topicId))

        // Realtime subscription — re-fetch on any INSERT/UPDATE to topic_summaries for this topic
        try {
            val channel = supabase.realtime.channel("topic-summary-$topicId")
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "topic_summaries"
                filter("topic_id", FilterOperator.EQ, topicId)
            }
            channel.subscribe()
            changeFlow.collect {
                emit(fetchSummary(topicId))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun fetchSummary(topicId: String): TopicSummary? {
        return try {
            supabase.from("topic_summaries")
                .select { filter { eq("topic_id", topicId) } }
                .decodeList<TopicSummary>()
                .firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun triggerTopicSummary(topicId: String) {
        try {
            android.util.Log.d("VoiceRepo", "Triggering generate-topic-summary for $topicId")
            supabase.functions.invoke("generate-topic-summary") {
                headers {
                    append(io.ktor.http.HttpHeaders.ContentType, io.ktor.http.ContentType.Application.Json.toString())
                }
                setBody(buildJsonObject { put("topic_id", topicId) })
            }
            android.util.Log.d("VoiceRepo", "generate-topic-summary trigger sent")
        } catch (e: Exception) {
            android.util.Log.e("VoiceRepo", "triggerTopicSummary failed: ${e.message}", e)
        }
    }
}
