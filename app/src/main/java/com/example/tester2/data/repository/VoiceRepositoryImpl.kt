package com.example.tester2.data.repository

import com.example.tester2.data.model.VoiceNote
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

class VoiceRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : VoiceRepository {

    override suspend fun createVoiceNote(storagePath: String): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: throw IllegalStateException("User not logged in")
            
            val voiceNote = JsonObject(
                mapOf(
                    "user_id" to JsonPrimitive(userId),
                    "storage_path" to JsonPrimitive(storagePath)
                )
            )
            
            supabase.from("voices").insert(voiceNote)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getMyVoiceNotes(): Flow<List<VoiceNote>> = flow {
        try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@flow emit(emptyList())
            
            val voices = supabase.from("voices").select {
                filter {
                    eq("user_id", userId)
                }
                order("created_at", Order.DESCENDING)
            }.decodeList<VoiceNote>()
            
            emit(voices)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }
}
