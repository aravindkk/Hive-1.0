package com.example.tester2.ui.recorder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tester2.data.recorder.AudioRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RecorderViewModel @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val storageRepository: com.example.tester2.data.repository.StorageRepository,
    private val voiceRepository: com.example.tester2.data.repository.VoiceRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _recordedFile = MutableStateFlow<File?>(null)
    val recordedFile = _recordedFile.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError = _uploadError.asStateFlow()

    private var currentFile: File? = null
    private var currentTopicId: String? = null
    
    fun setTopicId(topicId: String?) {
        currentTopicId = topicId
    }

    fun startRecording() {
        // Create file
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "HIV_REC_$timestamp.m4a"
        val cacheDir = context.cacheDir
        val file = File(cacheDir, fileName)
        
        currentFile = file
        
        try {
            audioRecorder.start(file)
            _isRecording.value = true
            _recordedFile.value = null // Reset previous file
            _uploadError.value = null
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle error state if needed
        }
    }

    fun stopRecording() {
        try {
            audioRecorder.stop()
            _isRecording.value = false
            _recordedFile.value = currentFile
            
            // Auto-upload
            currentFile?.let { uploadAudio(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun uploadAudio(file: File) {
        viewModelScope.launch {
            _isUploading.value = true
            val result = storageRepository.uploadAudio(file)
            
            result.onSuccess { path ->
                // Create DB entry
                val dbResult = voiceRepository.createVoiceNote(path, currentTopicId)
                _isUploading.value = false
                
                dbResult.onSuccess {
                    println("Voice note created in DB")
                    // Trigger transcription
                    voiceRepository.transcribeAudio(path)
                }
                dbResult.onFailure { e ->
                    _uploadError.value = "DB Error: ${e.message}"
                    e.printStackTrace()
                }
            }
            result.onFailure { e ->
                _isUploading.value = false
                _uploadError.value = "Upload Error: ${e.message}"
                e.printStackTrace()
            }
        }
    }
}
