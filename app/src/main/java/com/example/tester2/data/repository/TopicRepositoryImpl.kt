package com.example.tester2.data.repository

import com.example.tester2.data.model.Topic
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class TopicRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : TopicRepository {

    override suspend fun createTopic(title: String, lat: Double, lng: Double, radius: Int): Result<Unit> {
        // Creating a topic requires constructing the GIS point.
        // Since we are inserting into a 'geography' column, we can generally rely on
        // Supabase/PostgREST to handle GeoJSON input if properly configured,
        // OR we can use an RPC to insert safely.
        // For MVP, let's assume we might need an RPC for insertion too, or raw SQL via RPC.
        // But let's try a direct RPC for creating to be safe with the GEOGRAPHY type.
        
        return try {
            supabase.postgrest.rpc("create_topic", buildJsonObject {
                put("title", title)
                put("lat", lat)
                put("long", lng)
                put("radius", radius)
            })
            Result.success(Unit)
        } catch(e: Exception) {
             Result.failure(e)
        }
    }

    override fun getNearbyTopics(lat: Double, lng: Double, radius: Int): Flow<List<Topic>> = flow {
         try {
            val topics = supabase.postgrest.rpc("get_nearby_topics", buildJsonObject {
                put("lat", lat)
                put("long", lng)
                put("radius_meters", radius)
            }).decodeList<Topic>()
            emit(topics)
         } catch(e: Exception) {
             e.printStackTrace()
             emit(emptyList()) // Fail gracefully
         }
    }

    override fun getTopicsInBounds(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double): Flow<List<Topic>> = flow {
        try {
            val topics = supabase.postgrest.rpc("get_topics_in_bounds", buildJsonObject {
                put("min_lat", minLat)
                put("min_long", minLng)
                put("max_lat", maxLat)
                put("max_long", maxLng)
            }).decodeList<Topic>()
            emit(topics)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    override fun getPopularTopics(): Flow<List<Topic>> = flow {
        try {
            val topics = supabase.postgrest.rpc("get_popular_topics").decodeList<Topic>()
            emit(topics)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    override fun getNewTopics(): Flow<List<Topic>> = flow {
        try {
            val topics = supabase.postgrest.rpc("get_popular_topics").decodeList<Topic>()
                .sortedByDescending { it.createdAt }
            emit(topics)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    override fun getMyTopics(userId: String): Flow<List<Topic>> = flow {
        try {
            // Get topic IDs from voices this user contributed to
            val voices = supabase.from("voices")
                .select {
                    filter {
                        eq("user_id", userId)
                        neq("topic_id", "null")
                    }
                }
                .decodeList<JsonObject>()

            val topicIds = voices
                .mapNotNull { it["topic_id"]?.toString()?.trim('"') }
                .distinct()

            if (topicIds.isEmpty()) {
                emit(emptyList())
                return@flow
            }

            // Use the RPC so voice_count is populated, then filter to user's topics
            val topics = supabase.postgrest.rpc("get_popular_topics").decodeList<Topic>()
                .filter { it.id in topicIds }
                .sortedByDescending { it.createdAt }

            emit(topics)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    override suspend fun getTopicById(topicId: String): Topic? {
        return try {
            supabase.postgrest.rpc("get_topic_by_id", buildJsonObject {
                put("topic_id", topicId)
            }).decodeList<Topic>().firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
