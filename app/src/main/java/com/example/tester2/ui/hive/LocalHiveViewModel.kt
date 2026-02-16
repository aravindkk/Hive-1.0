package com.example.tester2.ui.hive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tester2.data.model.Topic
import com.example.tester2.data.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalHiveViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val voiceRepository: com.example.tester2.data.repository.VoiceRepository,
    private val audioPlayer: com.example.tester2.utils.AudioPlayer
) : ViewModel() {

    private val _popularTopics = MutableStateFlow<List<Topic>>(emptyList())
    val popularTopics = _popularTopics.asStateFlow()

    val playingUrl = audioPlayer.playingUrl

    init {
        fetchTopics()
    }
    
    fun fetchTopics() {
        viewModelScope.launch {
            topicRepository.getPopularTopics().collect { topics ->
                _popularTopics.value = topics.ifEmpty { 
                    // Fallback to dummy data if empty for demo
                    getDummyPopularTopics() 
                }
            }
        }
    }
    
    fun toggleAudio(url: String) {
        if (playingUrl.value == url) {
            audioPlayer.stop()
        } else {
            audioPlayer.play(url)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
    
    private fun getDummyPopularTopics(): List<Topic> {
        return listOf(
            Topic("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "Traffic", 0.0, 0.0, 500, true, 83),
            Topic("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "Late Night Food", 0.0, 0.0, 500, true, 42),
            Topic("cccccccc-cccc-cccc-cccc-cccccccccccc", "Lost Dog", 0.0, 0.0, 500, true, 3),
            Topic("dddddddd-dddd-dddd-dddd-dddddddddddd", "Construction Noise", 0.0, 0.0, 500, true, 15),
            Topic("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee", "Weather", 0.0, 0.0, 500, true, 8)
        )
    }
}
