package com.example.tester2.ui.hive

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tester2.data.model.Topic
import com.example.tester2.data.repository.LocationRepository
import com.example.tester2.data.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.tester2.data.model.VoiceNote
import com.example.tester2.data.repository.VoiceRepository

@HiltViewModel
class HiveViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val topicRepository: TopicRepository,
    private val voiceRepository: VoiceRepository,
    private val audioPlayer: com.example.tester2.utils.AudioPlayer
) : ViewModel() {

    private val _location = MutableStateFlow<Location?>(null)
    val location = _location.asStateFlow()

    private val _topics = MutableStateFlow<List<Topic>>(emptyList())
    val topics = _topics.asStateFlow()
    
    private val _selectedTopic = MutableStateFlow<Topic?>(null)
    val selectedTopic = _selectedTopic.asStateFlow()
    
    private val _topicVoices = MutableStateFlow<List<VoiceNote>>(emptyList())
    val topicVoices = _topicVoices.asStateFlow()
    
    val playingUrl = audioPlayer.playingUrl

    fun startLocationUpdates() {
        viewModelScope.launch {
            locationRepository.getLocationUpdates().collect { loc ->
                _location.value = loc
                // fetchNearbyTopics(loc) -> Triggered by camera movement now
            }
        }
    }

    fun onCameraIdle(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double) {
        fetchTopicsInBounds(minLat, minLng, maxLat, maxLng)
    }

    private fun fetchTopicsInBounds(minLat: Double, minLng: Double, maxLat: Double, maxLng: Double) {
        viewModelScope.launch {
             // 500ms debounce could be good here if not handled by UI state, but onCameraIdle is already discrete.
             topicRepository.getTopicsInBounds(minLat, minLng, maxLat, maxLng)
                .collect { fetchedTopics ->
                    if (fetchedTopics.isEmpty()) {
                         // _topics.value = getDummyTopics() // Optional: keep dummy data if needed or merge
                         // For now, let's append dummy topics if list is empty to avoid empty screen in demo
                         val dummy = getDummyTopics().filter { 
                            it.latitude in minLat..maxLat && it.longitude in minLng..maxLng
                         }
                         _topics.value = dummy
                    } else {
                         _topics.value = fetchedTopics
                    }
                }
        }
    }

    private fun fetchNearbyTopics(location: Location) {
        // Deprecated in favor of viewport fetching, but kept for initial location update if needed
        // or we can just rely on camera movement.
        // Let's leave it empty or remove usage in startLocationUpdates to avoid double fetch.
    }
    
    private fun getDummyTopics(): List<Topic> {
        // HSR Layout
        val hsrLat = 12.9121
        val hsrLng = 77.6446
        // Jayanagar
        val jayanagarLat = 12.9308
        val jayanagarLng = 77.5838
        // Koramangala
        val koraLat = 12.9352
        val koraLng = 77.6247
        
        return listOf(
            Topic("11111111-1111-1111-1111-111111111111", "HSR Startup Founders", hsrLat, hsrLng, 2000),
            Topic("22222222-2222-2222-2222-222222222222", "HSR Foodies", hsrLat + 0.001, hsrLng + 0.001, 2000),
            Topic("33333333-3333-3333-3333-333333333333", "Classic Old Bangalore", jayanagarLat, jayanagarLng, 3000),
            Topic("44444444-4444-4444-4444-444444444444", "Jayanagar Shopping", jayanagarLat + 0.002, jayanagarLng, 1000),
            Topic("55555555-5555-5555-5555-555555555555", "Koramangala Nightlife", koraLat, koraLng, 2000),
            Topic("66666666-6666-6666-6666-666666666666", "Sony Signal Traffic", koraLat - 0.001, koraLng, 1000)
        )
    }

    fun createDummyTopic() {
        val loc = _location.value ?: return
        viewModelScope.launch {
            topicRepository.createTopic("Test Topic at ${System.currentTimeMillis()}", loc.latitude, loc.longitude)
        }
    }
    
    fun selectTopic(topic: Topic) {
        _selectedTopic.value = topic
        viewModelScope.launch {
            voiceRepository.getVoiceNotesForTopic(topic.id).collect {
                _topicVoices.value = it
            }
        }
    }
    
    fun dismissTopic() {
        _selectedTopic.value = null
        _topicVoices.value = emptyList()
        audioPlayer.stop()
    }
    
    fun toggleAudio(voiceNote: VoiceNote) {
        val url = voiceRepository.getAudioUrl(voiceNote.storagePath)
        if (playingUrl.value == url) {
            audioPlayer.stop()
        } else {
            audioPlayer.play(url)
        }
    }
    
    fun getAudioUrl(voiceNote: VoiceNote): String {
        return voiceRepository.getAudioUrl(voiceNote.storagePath)
    }
    
    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
