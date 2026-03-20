package com.example.tester2.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeeklyReflection(
    val teaser: String,
    @SerialName("audio_path") val audioPath: String?
)
