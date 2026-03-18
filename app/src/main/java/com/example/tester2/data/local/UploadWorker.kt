package com.example.tester2.data.local

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.tester2.data.repository.StorageRepository
import com.example.tester2.data.repository.VoiceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val storageRepository: StorageRepository,
    private val voiceRepository: VoiceRepository,
    private val pendingVoiceDao: PendingVoiceDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val pending = pendingVoiceDao.getPending()
        if (pending.isEmpty()) return Result.success()

        var anyFailed = false

        for (upload in pending) {
            val file = File(upload.filePath)
            if (!file.exists()) {
                Log.w("UploadWorker", "File missing for upload ${upload.id}: ${upload.filePath}")
                pendingVoiceDao.setFailed(upload.id, "File not found")
                anyFailed = true
                continue
            }

            try {
                pendingVoiceDao.updateStatus(upload.id, "UPLOADING")

                val storageResult = storageRepository.uploadAudio(file)
                if (storageResult.isFailure) {
                    pendingVoiceDao.setFailed(upload.id, storageResult.exceptionOrNull()?.message)
                    anyFailed = true
                    continue
                }

                val path = storageResult.getOrThrow()

                val dbResult = voiceRepository.createVoiceNote(path, upload.topicId)
                if (dbResult.isFailure) {
                    pendingVoiceDao.setFailed(upload.id, dbResult.exceptionOrNull()?.message)
                    anyFailed = true
                    continue
                }

                pendingVoiceDao.updateStatus(upload.id, "PROCESSING")
                voiceRepository.transcribeAudio(path, upload.lat, upload.lng, upload.topicId)

                pendingVoiceDao.updateStatus(upload.id, "COMPLETE")
                file.delete()
                Log.d("UploadWorker", "Upload complete for ${upload.id}")
            } catch (e: Exception) {
                Log.e("UploadWorker", "Upload failed for ${upload.id}: ${e.message}", e)
                pendingVoiceDao.setFailed(upload.id, e.message)
                anyFailed = true
            }
        }

        pendingVoiceDao.deleteCompleted()
        return if (anyFailed) Result.retry() else Result.success()
    }
}
