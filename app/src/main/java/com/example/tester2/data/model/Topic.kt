package com.example.tester2.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Topic(
    val id: String,
    val title: String,
    val latitude: Double, // Extracted from PostGIS point
    val longitude: Double,
    val radius: Int,
    val active: Boolean = true
)
