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
            topicRepository.getNearbyTopics(location.latitude, location.longitude, 5000) // 5km radius
                .collect {
                    _topics.value = it
                }
        }
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
