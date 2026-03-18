package com.example.tester2.utils

import android.content.Context
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionJob: Job? = null

    private val _playingUrl = MutableStateFlow<String?>(null)
    val playingUrl: StateFlow<String?> = _playingUrl.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    fun playPlaylist(urls: List<String>, playlistId: String) {
        if (urls.isEmpty()) return
        if (_playingUrl.value == playlistId) { stop(); return }
        stop()

        Log.d("AudioPlayer", "playPlaylist: $playlistId (${urls.size} items)")
        _playingUrl.value = playlistId
        _currentPositionMs.value = 0L

        scope.launch {
            val exo = ExoPlayer.Builder(context).build().also {
                it.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true
                )
            }
            player = exo

            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_READY -> startPositionTracking()
                        Player.STATE_ENDED -> {
                            stopPositionTracking()
                            _playingUrl.value = null
                            _currentPositionMs.value = 0L
                            exo.release()
                            player = null
                        }
                        else -> {}
                    }
                }
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e("AudioPlayer", "PlaylistError: ${error.message}")
                    stopPositionTracking()
                    _playingUrl.value = null
                    _currentPositionMs.value = 0L
                    exo.release()
                    player = null
                }
            })

            exo.setMediaItems(urls.map { MediaItem.fromUri(it) })
            exo.playWhenReady = true
            exo.prepare()
        }
    }

    fun play(url: String) {
        if (_playingUrl.value == url) {
            stop()
            return
        }
        stop()

        Log.d("AudioPlayer", "play: $url")
        _playingUrl.value = url
        _currentPositionMs.value = 0L

        scope.launch {
            val exo = ExoPlayer.Builder(context).build().also {
                it.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    /* handleAudioFocus = */ true
                )
            }
            player = exo

            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    Log.d("AudioPlayer", "playbackState=$state url=$url")
                    when (state) {
                        Player.STATE_READY -> {
                            Log.d("AudioPlayer", "STATE_READY, playing=${exo.playWhenReady}")
                            startPositionTracking()
                        }
                        Player.STATE_ENDED -> {
                            Log.d("AudioPlayer", "STATE_ENDED")
                            stopPositionTracking()
                            _playingUrl.value = null
                            _currentPositionMs.value = 0L
                            exo.release()
                            player = null
                        }
                        else -> {}
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e("AudioPlayer", "PlayerError: ${error.message} cause=${error.cause?.message}")
                    stopPositionTracking()
                    _playingUrl.value = null
                    _currentPositionMs.value = 0L
                    exo.release()
                    player = null
                }
            })

            exo.setMediaItem(MediaItem.fromUri(url))
            exo.playWhenReady = true
            exo.prepare()
        }
    }

    fun stop() {
        stopPositionTracking()
        try {
            player?.release()
        } catch (e: Exception) {
            Log.e("AudioPlayer", "stop error: ${e.message}")
        } finally {
            player = null
            _playingUrl.value = null
            _currentPositionMs.value = 0L
        }
    }

    private fun startPositionTracking() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (true) {
                player?.let { _currentPositionMs.value = it.currentPosition }
                delay(100)
            }
        }
    }

    private fun stopPositionTracking() {
        positionJob?.cancel()
        positionJob = null
    }
}
