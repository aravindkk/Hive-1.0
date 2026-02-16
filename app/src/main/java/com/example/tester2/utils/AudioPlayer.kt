package com.example.tester2.utils

import android.content.Context
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    
    private val _playingUrl = MutableStateFlow<String?>(null)
    val playingUrl: StateFlow<String?> = _playingUrl.asStateFlow()
    
    // Simple playing state: Loading, Playing, Error, Idle
    // For now effectively: playingUrl != null means playing or loading.
    
    fun play(url: String) {
        if (_playingUrl.value == url) {
            stop()
            return
        }
        
        stop()
        
        _playingUrl.value = url
        
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { start() }
                setOnCompletionListener { 
                    _playingUrl.value = null
                    release()
                    mediaPlayer = null
                }
                setOnErrorListener { _, _, _ ->
                    _playingUrl.value = null
                    release()
                    mediaPlayer = null
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _playingUrl.value = null
        }
    }
    
    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
            _playingUrl.value = null
        }
    }
}
