package com.example.tester2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingVoiceDao {

    @Insert
    suspend fun insert(upload: PendingVoiceUpload)

    @Query("SELECT * FROM pending_voice_uploads WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPending(): List<PendingVoiceUpload>

    @Query("UPDATE pending_voice_uploads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE pending_voice_uploads SET status = 'FAILED', errorMessage = :error WHERE id = :id")
    suspend fun setFailed(id: String, error: String?)

    @Query("DELETE FROM pending_voice_uploads WHERE status = 'COMPLETE'")
    suspend fun deleteCompleted()
}
