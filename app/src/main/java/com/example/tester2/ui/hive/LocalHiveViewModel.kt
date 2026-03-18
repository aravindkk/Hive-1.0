package com.example.tester2.ui.hive

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tester2.data.model.Topic
import com.example.tester2.data.repository.LocationRepository
import com.example.tester2.data.repository.TopicRepository
import com.example.tester2.data.repository.VoiceRepository
import com.example.tester2.utils.AudioPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LocalHiveViewModel @Inject constructor(
    private val topicRepository: TopicRepository,
    private val voiceRepository: VoiceRepository,
    private val audioPlayer: AudioPlayer,
    private val supabase: SupabaseClient,
    private val locationRepository: LocationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _popularTopics = MutableStateFlow<List<Topic>>(emptyList())
    val popularTopics = _popularTopics.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab = _selectedTab.asStateFlow()

    private val _playingTopicTitle = MutableStateFlow<String?>(null)
    val playingTopicTitle = _playingTopicTitle.asStateFlow()

    private val _areaName = MutableStateFlow("Your area")
    val areaName = _areaName.asStateFlow()

    val playingUrl = audioPlayer.playingUrl

    private var topicsJob: Job? = null

    init {
        loadTopicsForTab(0)
        viewModelScope.launch {
            audioPlayer.playingUrl.collect { url ->
                if (url == null) _playingTopicTitle.value = null
            }
        }
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

    fun setTab(index: Int) {
        _selectedTab.value = index
        loadTopicsForTab(index)
    }

    private fun loadTopicsForTab(tab: Int) {
        topicsJob?.cancel()
        topicsJob = viewModelScope.launch {
            when (tab) {
                0 -> topicRepository.getPopularTopics().collect { _popularTopics.value = it }
                1 -> topicRepository.getNewTopics().collect { _popularTopics.value = it }
                2 -> {
                    val userId = supabase.auth.currentUserOrNull()?.id
                    if (userId != null) {
                        topicRepository.getMyTopics(userId).collect { _popularTopics.value = it }
                    } else {
                        _popularTopics.value = emptyList()
                    }
                }
            }
        }
    }

    fun onTopicTapped(topic: Topic) {
        _playingTopicTitle.value = topic.title
    }

    fun stopAudio() {
        audioPlayer.stop()
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
