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
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val supabase: SupabaseClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _voiceNotes = MutableStateFlow<List<VoiceNote>>(emptyList())
    val voiceNotes = _voiceNotes.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _areaName = MutableStateFlow("Your area")
    val areaName = _areaName.asStateFlow()

    private val _weeklyReflection = MutableStateFlow<com.example.tester2.data.model.WeeklyReflection?>(null)
    val weeklyReflection = _weeklyReflection.asStateFlow()

    val playingUrl = audioPlayer.playingUrl

    private val weeklyAudioUrl = _weeklyReflection
        .map { it?.audioPath?.let { path -> voiceRepository.getAudioUrl(path) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isWeeklyAudioPlaying = combine(audioPlayer.playingUrl, weeklyAudioUrl) { playing, url ->
        url != null && playing == url
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var voiceJob: kotlinx.coroutines.Job? = null

    init {
        loadVoiceNotes()
        refreshAreaName()
        loadWeeklyNarrative()
    }

    private fun loadWeeklyNarrative() {
        viewModelScope.launch(Dispatchers.IO) {
            supabase.auth.sessionStatus.first { it !is SessionStatus.Initializing }
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
            voiceRepository.generateWeeklyReflection(userId).onSuccess { reflection ->
                _weeklyReflection.value = reflection
            }
        }
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
    
    fun toggleWeeklyAudio() {
        val url = weeklyAudioUrl.value ?: return
        if (audioPlayer.playingUrl.value == url) audioPlayer.stop()
        else audioPlayer.play(url)
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
