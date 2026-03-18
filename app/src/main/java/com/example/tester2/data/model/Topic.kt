package com.example.tester2.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Topic(
    val id: String,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Int,
    val active: Boolean = true,
    @SerialName("voice_count")
    val voiceCount: Long = 0,
    @SerialName("created_at")
    val createdAt: String = ""
)
