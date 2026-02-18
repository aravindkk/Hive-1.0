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
import io.github.jan.supabase.storage.storage
import io.ktor.client.request.headers
import javax.inject.Inject

class VoiceRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : VoiceRepository {

    override suspend fun createVoiceNote(storagePath: String, topicId: String?): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: throw IllegalStateException("User not logged in")
            
            val voiceNote = buildJsonObject {
                put("user_id", userId)
                put("storage_path", storagePath)
                if (topicId != null) {
                    put("topic_id", topicId)
                }
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
            supabase.from("voices").select {
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

    override suspend fun transcribeAudio(storagePath: String): Result<Unit> {
        return try {
            supabase.functions.invoke("transcribe-audio") {
                // Fix for: Fail to prepare request body for sending. Content-Type: null
                headers {
                    append(io.ktor.http.HttpHeaders.ContentType, io.ktor.http.ContentType.Application.Json.toString())
                }
                setBody(buildJsonObject {
                    put("storage_path", storagePath)
                })
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAudioUrl(storagePath: String): String {
        // storage extension requires import, using manual construction to be safe
        // Format: https://project.supabase.co/storage/v1/object/public/bucket/path
        return "${supabase.supabaseUrl}/storage/v1/object/public/audio-notes/$storagePath" 
    }
}
