package com.example.tester2.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TranscriptionResult(
    val transcript: String = "",
    val classification: String = "personal",
    @SerialName("topic_id") val topicId: String? = null,
    @SerialName("topic_title") val topicTitle: String? = null,
    @SerialName("voice_count") val voiceCount: Long = 0
)
