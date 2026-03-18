package com.example.tester2.ui.hive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tester2.data.model.Topic
import com.example.tester2.data.model.TopicSummary
import com.example.tester2.data.model.VoiceNote
import com.example.tester2.data.repository.TopicRepository
import com.example.tester2.data.repository.VoiceRepository
import com.example.tester2.utils.AudioPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopicDeepDiveViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val voiceRepository: VoiceRepository,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _topic = MutableStateFlow<Topic?>(null)
    val topic = _topic.asStateFlow()

    private val _voices = MutableStateFlow<List<VoiceNote>>(emptyList())
    val voices = _voices.asStateFlow()

    private val _summary = MutableStateFlow<TopicSummary?>(null)
    val summary = _summary.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    // true while summary is null and we're waiting for it to be generated
    private val _summaryGenerating = MutableStateFlow(false)
    val summaryGenerating = _summaryGenerating.asStateFlow()

    val playingUrl = audioPlayer.playingUrl
    val currentPositionMs = audioPlayer.currentPositionMs

    private var voicesJob: Job? = null
    private var summaryJob: Job? = null
    private var loadedTopicId: String? = null

    fun loadForTopic(topicId: String) {
        if (topicId == loadedTopicId) return
        loadedTopicId = topicId

        _isLoading.value = true
        viewModelScope.launch {
            _topic.value = topicRepository.getTopicById(topicId)
            _isLoading.value = false
        }

        voicesJob?.cancel()
        voicesJob = viewModelScope.launch {
            voiceRepository.getVoiceNotesForTopic(topicId).collect { notes ->
                _voices.value = notes
            }
        }

        summaryJob?.cancel()
        summaryJob = viewModelScope.launch {
            voiceRepository.getTopicSummaryFlow(topicId).collect { summary ->
                _summary.value = summary
                if (summary != null) {
                    _summaryGenerating.value = false
                    Log.d("TopicDeepDive", "Summary loaded: ${summary.segments.size} segments, audio=${summary.audioPath}")
                }
            }
        }
    }

    // Called when we know voices exist but no summary is available yet.
    // Sends a fire-and-forget trigger to the edge function; Realtime will deliver the result.
    fun triggerSummaryGeneration(topicId: String) {
        if (_summaryGenerating.value) return
        _summaryGenerating.value = true
        Log.d("TopicDeepDive", "Triggering summary generation for $topicId")
        viewModelScope.launch {
            voiceRepository.triggerTopicSummary(topicId)
        }
    }

    fun toggleAudio(url: String) {
        Log.d("TopicDeepDive", "toggleAudio: url=$url playingUrl=${audioPlayer.playingUrl.value}")
        if (audioPlayer.playingUrl.value == url) audioPlayer.stop() else audioPlayer.play(url)
    }

    fun getAudioUrl(storagePath: String) = voiceRepository.getAudioUrl(storagePath)

    fun getSummaryAudioUrl(audioPath: String) = voiceRepository.getAudioUrl(audioPath)

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
