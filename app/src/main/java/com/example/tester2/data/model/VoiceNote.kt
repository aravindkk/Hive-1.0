package com.example.tester2.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TopicRef(val title: String? = null)

@Serializable
data class VoiceNote(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("storage_path")
    val storagePath: String,
    val transcript: String? = null,
    @SerialName("topic_id")
    val topicId: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    val classification: String? = null,
    val username: String? = null,
    val topics: TopicRef? = null,
    @SerialName("mood_tags")
    val moodTags: List<String>? = null
) {
    val topicTitle: String? get() = topics?.title
}
