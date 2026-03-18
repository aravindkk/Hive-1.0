package com.example.tester2.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SummarySegment(
    val text: String,
    @SerialName("start_ms") val startMs: Long = 0L,
    @SerialName("attributed_to") val attributedTo: List<String> = emptyList()
)

@Serializable
data class TopicSummary(
    val id: String = "",
    @SerialName("topic_id") val topicId: String,
    @SerialName("audio_path") val audioPath: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Double = 0.0,
    val segments: List<SummarySegment> = emptyList(),
    @SerialName("generated_at") val generatedAt: String = ""
)
