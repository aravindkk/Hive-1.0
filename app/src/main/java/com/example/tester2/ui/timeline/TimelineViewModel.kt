package com.example.tester2.ui.timeline

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tester2.data.model.VoiceNote
import com.example.tester2.data.repository.LocationRepository
import com.example.tester2.data.repository.VoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val voiceRepository: VoiceRepository,
    private val audioPlayer: com.example.tester2.utils.AudioPlayer,
    private val locationRepository: LocationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _voiceNotes = MutableStateFlow<List<VoiceNote>>(emptyList())
    val voiceNotes = _voiceNotes.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _areaName = MutableStateFlow("Your area")
    val areaName = _areaName.asStateFlow()

    val playingUrl = audioPlayer.playingUrl

    val isWeeklyReflectionPlaying = audioPlayer.playingUrl
        .map { it == WEEKLY_REFLECTION_ID }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var voiceJob: kotlinx.coroutines.Job? = null

    companion object {
        private const val WEEKLY_REFLECTION_ID = "weekly-reflection-playlist"
    }

    init {
        loadVoiceNotes()
        refreshAreaName()
    }

    private var locationJob: kotlinx.coroutines.Job? = null

    fun refreshAreaName() {
        if (_areaName.value != "Your area") return
        if (locationJob?.isActive == true) return
        locationJob = viewModelScope.launch {
            try {
                locationRepository.getLocationUpdates().collect { location ->
                    val name = withContext(Dispatchers.IO) {
                        try {
                            @Suppress("DEPRECATION")
                            Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)
                                ?.firstOrNull()?.let { it.subLocality ?: it.locality }
                        } catch (e: Exception) { null }
                    }
                    if (name != null) {
                        _areaName.value = name
                        locationJob?.cancel()
                    }
                }
            } catch (e: Exception) { /* SecurityException or cancellation */ }
        }
    }

    fun refresh() {
        loadVoiceNotes()
    }

    private fun loadVoiceNotes() {
        voiceJob?.cancel()
        voiceJob = viewModelScope.launch {
            voiceRepository.getMyVoiceNotes().collect { notes ->
                _voiceNotes.value = notes
                _isLoading.value = false
            }
        }
    }
    
    fun toggleWeeklyReflection() {
        val urls = _voiceNotes.value
            .take(5)
            .map { voiceRepository.getAudioUrl(it.storagePath) }
        audioPlayer.playPlaylist(urls, WEEKLY_REFLECTION_ID)
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

    fun stopAudio() {
        audioPlayer.stop()
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
