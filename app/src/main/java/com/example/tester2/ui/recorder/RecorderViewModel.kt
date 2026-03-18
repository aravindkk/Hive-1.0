package com.example.tester2.ui.recorder

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tester2.data.recorder.AudioRecorder
import com.example.tester2.data.repository.LocationRepository
import com.example.tester2.data.repository.StorageRepository
import com.example.tester2.data.repository.VoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import android.util.Log

data class UploadResult(
    val classification: String,
    val topicId: String?,
    val topicTitle: String?,
    val voiceCount: Long
)

enum class AuraColor { GREEN, ORANGE, RED }

@HiltViewModel
class RecorderViewModel @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val storageRepository: StorageRepository,
    private val voiceRepository: VoiceRepository,
    private val locationRepository: LocationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError = _uploadError.asStateFlow()

    private val _uploadResult = MutableStateFlow<UploadResult?>(null)
    val uploadResult = _uploadResult.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds = _recordingSeconds.asStateFlow()

    // Normalized amplitude bars (0..1 each), sampled every 50ms, keeps last 30 samples
    private val _amplitudeBars = MutableStateFlow<List<Float>>(List(30) { 0f })
    val amplitudeBars = _amplitudeBars.asStateFlow()

    // Aura color derived from recording time
    private val _auraColor = MutableStateFlow(AuraColor.GREEN)
    val auraColor = _auraColor.asStateFlow()

    private val _areaName = MutableStateFlow("Your area")
    val areaName = _areaName.asStateFlow()

    private var currentFile: File? = null
    private var currentTopicId: String? = null
    private var timerJob: Job? = null
    private var amplitudeJob: Job? = null

    init {
        viewModelScope.launch {
            val location = locationRepository.getLastLocation() ?: return@launch
            val name = resolveAreaName(location.latitude, location.longitude) ?: return@launch
            _areaName.value = name
        }
    }

    private suspend fun resolveAreaName(lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        try {
            @Suppress("DEPRECATION")
            val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lng, 1)
            val addr = addresses?.firstOrNull() ?: return@withContext null
            addr.subLocality ?: addr.locality
        } catch (e: Exception) {
            null
        }
    }

    fun setTopicId(topicId: String?) {
        currentTopicId = topicId
    }

    fun startRecording() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(context.cacheDir, "HIV_REC_$timestamp.m4a")
        currentFile = file
        _uploadResult.value = null
        _uploadError.value = null
        _recordingSeconds.value = 0
        _auraColor.value = AuraColor.GREEN
        _amplitudeBars.value = List(30) { 0f }

        try {
            audioRecorder.start(file)
            _isRecording.value = true

            // Timer + aura color + auto-stop at 120s
            timerJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    val secs = _recordingSeconds.value + 1
                    _recordingSeconds.value = secs
                    _auraColor.value = when {
                        secs < 100 -> AuraColor.GREEN
                        secs < 110 -> AuraColor.ORANGE
                        else -> AuraColor.RED
                    }
                    if (secs >= 120) {
                        stopRecording()
                        break
                    }
                }
            }

            // Amplitude sampling every 50ms
            amplitudeJob = viewModelScope.launch {
                while (true) {
                    delay(50)
                    val raw = audioRecorder.maxAmplitude()
                    val normalized = (raw / 32767f).coerceIn(0f, 1f)
                    val current = _amplitudeBars.value.toMutableList()
                    current.removeAt(0)
                    current.add(normalized)
                    _amplitudeBars.value = current
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopRecording() {
        timerJob?.cancel()
        amplitudeJob?.cancel()
        try {
            audioRecorder.stop()
            _isRecording.value = false
            currentFile?.let { uploadAudio(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun discardRecording() {
        timerJob?.cancel()
        amplitudeJob?.cancel()
        try {
            audioRecorder.stop()
        } catch (_: Exception) {}
        _isRecording.value = false
        _recordingSeconds.value = 0
        _amplitudeBars.value = List(30) { 0f }
        currentFile?.delete()
        currentFile = null
    }

    private fun uploadAudio(file: File) {
        viewModelScope.launch {
            _isUploading.value = true

            val storageResult = storageRepository.uploadAudio(file)
            storageResult.onFailure { e ->
                _isUploading.value = false
                _uploadError.value = "Upload failed: ${e.message}"
                return@launch
            }

            val path = storageResult.getOrThrow()
            val dbResult = voiceRepository.createVoiceNote(path, currentTopicId)
            _isUploading.value = false

            dbResult.onFailure { e ->
                _uploadError.value = "Save failed: ${e.message}"
                return@launch
            }

            _isProcessing.value = true
            val location = locationRepository.getLastLocation()
            val transcribeResult = voiceRepository.transcribeAudio(path, location?.latitude, location?.longitude)
            _isProcessing.value = false

            transcribeResult.onSuccess { result ->
                _uploadResult.value = UploadResult(
                    classification = result.classification,
                    topicId = result.topicId,
                    topicTitle = result.topicTitle,
                    voiceCount = result.voiceCount
                )
            }
            transcribeResult.onFailure { e ->
                Log.e("RecorderViewModel", "Transcription failed: ${e.message}", e)
                _uploadError.value = "Transcription failed: ${e.message}"
            }
        }
    }
}
