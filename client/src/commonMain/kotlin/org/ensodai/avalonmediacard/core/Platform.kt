package org.ensodai.avalonmediacard.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.presentation.screens.player.action.PlayerActions
import org.ensodai.avalonmediacard.presentation.screens.player.viewState.PlayerViewState

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun saveSetting(key: String, value: String)
expect fun loadSetting(key: String): String?

expect fun openUrl(url: String)
expect fun openInExternalPlayer(streamUrl: String, title: String)

expect fun getUrlQueryParameters(): Map<String, String>
expect fun clearUrlQueryParameters()

class PlaybackState(
    duration: Double = 0.0,
    currentTime: Double = 0.0,
    isPlaying: Boolean = false,
    isBuffering: Boolean = true,
    bufferAheadSeconds: Double = 0.0,
    isMuted: Boolean = false,
    volume: Double = 1.0,
    playbackError: String? = null,
    fps: Double? = null,
    currentSubtitleText: String? = null,
    audioUnsupported: Boolean = false
) {
    var duration by mutableStateOf(duration)
    var currentTime by mutableStateOf(currentTime)
    var isPlaying by mutableStateOf(isPlaying)
    var isBuffering by mutableStateOf(isBuffering)
    var bufferAheadSeconds by mutableStateOf(bufferAheadSeconds)
    var isMuted by mutableStateOf(isMuted)
    var volume by mutableStateOf(volume)
    var playbackError by mutableStateOf(playbackError)
    var fps by mutableStateOf(fps)
    var currentSubtitleText by mutableStateOf(currentSubtitleText)
    var audioUnsupported by mutableStateOf(audioUnsupported)
}

interface PlaybackController {
    val state: PlaybackState
    fun play()
    fun pause()
    fun stop() {
        pause()
        state.isBuffering = true
    }
    fun togglePlayPause()
    fun seek(time: Double)
    fun setMuted(muted: Boolean)
    fun setVolume(volume: Double)
    fun sendKeyPress(key: String) {}


    fun stepForward() {
        pause()
        val fps = state.fps ?: 24.0
        val targetTime = (state.currentTime + 1.0 / fps).coerceAtMost(state.duration.takeIf { it > 0.0 } ?: Double.MAX_VALUE)
        seek(targetTime)
    }

    fun stepBackward() {
        pause()
        val fps = state.fps ?: 24.0
        val targetTime = (state.currentTime - 1.0 / fps).coerceAtLeast(0.0)
        seek(targetTime)
    }

    val audioTracks: List<AudioTrack> get() = emptyList()
    val selectedAudioTrack: AudioTrack? get() = null
    fun selectAudioTrack(track: AudioTrack) {}

    val subtitleTracks: List<SubtitleTrack> get() = emptyList()
    val selectedSubtitleTrack: SubtitleTrack? get() = null
    fun selectSubtitleTrack(track: SubtitleTrack?) {}
    fun setTracks(audioTracks: List<AudioTrack>, subtitleTracks: List<SubtitleTrack>) {}
}

@Composable
expect fun VideoPlayer(
    state: PlayerViewState,
    actions: PlayerActions,
    modifier: Modifier = Modifier
)

expect fun togglePlatformFullscreen()

@Composable
expect fun SystemFullscreenHandler(
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit
)

