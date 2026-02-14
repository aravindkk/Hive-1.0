package com.example.tester2.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tester2.data.model.VoiceNote
import com.example.tester2.data.repository.VoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val voiceRepository: VoiceRepository
) : ViewModel() {

    private val _voiceNotes = MutableStateFlow<List<VoiceNote>>(emptyList())
    val voiceNotes = _voiceNotes.asStateFlow()

    init {
        loadVoiceNotes()
    }

    private fun loadVoiceNotes() {
        viewModelScope.launch {
            voiceRepository.getMyVoiceNotes().collect { notes ->
                _voiceNotes.value = notes
            }
        }
    }
}
