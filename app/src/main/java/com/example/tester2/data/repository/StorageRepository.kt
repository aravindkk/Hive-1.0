package com.example.tester2.data.repository

import java.io.File

interface StorageRepository {
    suspend fun uploadAudio(file: File): Result<String>
}
