package com.example.tester2.data.repository

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun getLocationUpdates(intervalMs: Long = 10000L): Flow<Location>
    suspend fun getLastLocation(): Location?
}
