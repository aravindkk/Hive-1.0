package com.example.tester2.data.repository

import com.example.tester2.data.model.Topic
import kotlinx.coroutines.flow.Flow

interface TopicRepository {
    suspend fun createTopic(title: String, lat: Double, lng: Double, radius: Int = 500): Result<Unit>
    fun getNearbyTopics(lat: Double, lng: Double, radius: Int = 500): Flow<List<Topic>>
    fun getTopicsInBounds(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double): Flow<List<Topic>>
    fun getPopularTopics(): Flow<List<Topic>>
    fun getNewTopics(): Flow<List<Topic>>
    fun getMyTopics(userId: String): Flow<List<Topic>>
    suspend fun getTopicById(topicId: String): Topic?
}
