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
    private val voiceRepository: VoiceRepository
) : ViewModel() {

    private val _location = MutableStateFlow<Location?>(null)
    val location = _location.asStateFlow()

    private val _topics = MutableStateFlow<List<Topic>>(emptyList())
    val topics = _topics.asStateFlow()
    
    private val _selectedTopic = MutableStateFlow<Topic?>(null)
    val selectedTopic = _selectedTopic.asStateFlow()
    
    private val _topicVoices = MutableStateFlow<List<VoiceNote>>(emptyList())
    val topicVoices = _topicVoices.asStateFlow()

    fun startLocationUpdates() {
        viewModelScope.launch {
            locationRepository.getLocationUpdates().collect { loc ->
                _location.value = loc
                fetchNearbyTopics(loc)
            }
        }
    }

    private fun fetchNearbyTopics(location: Location) {
        viewModelScope.launch {
             // In a real app, we might handle loading states here
             topicRepository.getNearbyTopics(location.latitude, location.longitude, 5000) // 5km radius
                .collect { fetchedTopics ->
                    if (fetchedTopics.isEmpty()) {
                         _topics.value = getDummyTopics()
                    } else {
                         _topics.value = fetchedTopics
                    }
                }
        }
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
            Topic("demo_1", "HSR Startup Founders", hsrLat, hsrLng, 2000),
            Topic("demo_2", "HSR Foodies", hsrLat + 0.001, hsrLng + 0.001, 2000),
            Topic("demo_3", "Classic Old Bangalore", jayanagarLat, jayanagarLng, 3000),
            Topic("demo_4", "Jayanagar Shopping", jayanagarLat + 0.002, jayanagarLng, 1000),
            Topic("demo_5", "Koramangala Nightlife", koraLat, koraLng, 2000),
            Topic("demo_6", "Sony Signal Traffic", koraLat - 0.001, koraLng, 1000)
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
    }
}
