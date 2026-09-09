package com.lbo.quran.audio

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackUiState(
    val queue: List<String> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val scopeLabel: String = ""
) {
    val currentAId: String? get() = queue.getOrNull(currentIndex)
    val isActive: Boolean get() = queue.isNotEmpty()
}

/** ساخت صف پخش و فرمان‌دهی به پخش‌کننده‌ی مشترک (که در QuranAudioService هم به یک
 *  MediaSession پس‌زمینه‌دار وصل می‌شود). یک نمونه از این کلاس در QuranViewModel نگه‌داری می‌شود. */
class AudioPlaybackController(
    private val context: Context,
    private val audioRepository: AudioRepository
) {
    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val player: ExoPlayer by lazy {
        (QuranAudioPlayerHolder.player ?: ExoPlayer.Builder(context).build().also {
            QuranAudioPlayerHolder.player = it
        }).also { attachListener(it) }
    }

    private fun attachListener(p: ExoPlayer) {
        p.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.value = _state.value.copy(isBuffering = playbackState == Player.STATE_BUFFERING)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _state.value = _state.value.copy(currentIndex = p.currentMediaItemIndex)
            }
        })
    }

    private fun ensureServiceRunning() {
        // پلیر را پیش از استارت سرویس مقداردهی می‌کنیم تا سرویس همان نمونه را بردارد، نه یک نمونه‌ی جدید.
        player
        context.startService(Intent(context, QuranAudioService::class.java))
    }

    fun playQueue(aIds: List<String>, startIndex: Int, scopeLabel: String) {
        val available = aIds.filter { audioRepository.hasAudio(it) }
        if (available.isEmpty()) {
            _state.value = PlaybackUiState(scopeLabel = scopeLabel)
            return
        }
        val clampedStart = startIndex.coerceIn(0, available.lastIndex)
        ensureServiceRunning()
        val items = available.map { aId ->
            MediaItem.fromUri(Uri.fromFile(audioRepository.audioFileFor(aId)))
        }
        player.setMediaItems(items, clampedStart, 0L)
        player.prepare()
        player.playWhenReady = true
        _state.value = PlaybackUiState(
            queue = available,
            currentIndex = clampedStart,
            isPlaying = true,
            scopeLabel = scopeLabel
        )
    }

    fun playSingleAyah(aId: String) = playQueue(listOf(aId), 0, "آیه")

    fun togglePlayPause() {
        if (!_state.value.isActive) return
        player.playWhenReady = !player.playWhenReady
    }

    fun next() {
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
    }

    fun previous() {
        if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        _state.value = PlaybackUiState()
    }
}
