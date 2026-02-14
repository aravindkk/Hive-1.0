package com.example.tester2.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import java.io.File
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class StorageRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : StorageRepository {

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun uploadAudio(file: File): Result<String> {
        return try {
            val bucket = supabase.storage.from("audio-notes")
            val path = "${System.currentTimeMillis()}_${file.name}"
            val bytes = file.readBytes()
            
            bucket.upload(path, bytes)
            
            // Return public URL or path
            // For now, returning the path. 
            // In a real app, we might want the public URL: bucket.publicUrl(path)
            Result.success(path)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
