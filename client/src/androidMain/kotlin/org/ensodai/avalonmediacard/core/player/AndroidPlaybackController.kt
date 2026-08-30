package org.ensodai.avalonmediacard.core.player

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack

class AndroidPlaybackController(
    val exoPlayer: ExoPlayer,
    private val onFatalError: ((Long) -> Unit)? = null
) : CommonPlaybackController() {

    private val logger = AppLogging.logger("AndroidPlaybackController")

    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var networkRetryCount = 0
    
    // Флаг для отслеживания ошибок (чтобы переключиться на MPV)
    var hasFatalError: Boolean = false
        private set

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        state.isBuffering = true
                    }
                    Player.STATE_READY -> {
                        networkRetryCount = 0
                        state.isBuffering = false
                        state.duration = (exoPlayer.duration.coerceAtLeast(0L) / 1000.0)
                    }
                    Player.STATE_ENDED -> {
                        state.isPlaying = false
                    }
                    Player.STATE_IDLE -> {
                        // Плеер остановлен или произошла ошибка
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                state.isPlaying = isPlaying
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L) / 1000.0
                updateTime(currentPos)
                val bufferedPos = exoPlayer.bufferedPosition.coerceAtLeast(0L) / 1000.0
                state.bufferAheadSeconds = (bufferedPos - currentPos).coerceAtLeast(0.0)
            }

            override fun onPlayerError(error: PlaybackException) {
                state.isBuffering = false
                state.isPlaying = false
                
                // Коды от 2000 до 2008 — это ошибки сети и IO (интернета)
                val isNetworkError = error.errorCode in 2000..2008
                
                logger.e { "onPlayerError caught: errorCode=${error.errorCode}, name=${error.errorCodeName}, isNetworkError=$isNetworkError, message=${error.message}" }
                
                if (isNetworkError) {
                    if (networkRetryCount < 3) {
                        networkRetryCount++
                        val lastValidPosMs = (state.currentTime * 1000).toLong().coerceAtLeast(exoPlayer.currentPosition)
                        logger.d { "Network reconnect attempt $networkRetryCount/3 at pos=$lastValidPosMs ms" }
                        state.isBuffering = true
                        state.playbackError = null
                        exoPlayer.prepare()
                        if (lastValidPosMs > 0L) {
                            exoPlayer.seekTo(lastValidPosMs)
                        }
                        exoPlayer.play()
                    } else {
                        logger.w { "Blocked MPV Fallback because it is NetworkError (${error.errorCode})!" }
                        state.playbackError = "Connection error: ${error.message}"
                    }
                } else {
                    hasFatalError = true
                    val lastValidPosMs = (state.currentTime * 1000).toLong().coerceAtLeast(exoPlayer.currentPosition)
                    logger.w { "Triggering MPV Fallback due to codec failure! lastValidPosMs=$lastValidPosMs" }
                    state.playbackError = "Media3 fallback (${error.errorCodeName}). Switching to MPV..."
                    onFatalError?.invoke(lastValidPosMs)
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val audioTracksList = mutableListOf<AudioTrack>()
                val subtitleTracksList = mutableListOf<SubtitleTrack>()

                var selectedAudio: AudioTrack? = null
                var selectedSubtitle: SubtitleTrack? = null

                for ((groupIndex, group) in tracks.groups.withIndex()) {
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (trackIndex in 0 until group.length) {
                            val format = group.getTrackFormat(trackIndex)
                            val language = format.language
                            val channels = format.channelCount
                            val name = format.label ?: format.language ?: "Audio ${audioTracksList.size + 1}"
                            val isSelected = group.isTrackSelected(trackIndex)

                            val audioTrack = AudioTrack(
                                id = "$groupIndex:$trackIndex",
                                name = "$name ${if (channels > 0) "($channels ch)" else ""}",
                                language = language,
                                channels = if (channels > 0) channels else null,
                                isDefault = isSelected
                            )
                            audioTracksList.add(audioTrack)
                            if (isSelected) {
                                selectedAudio = audioTrack
                            }
                        }
                    } else if (group.type == C.TRACK_TYPE_TEXT) {
                        for (trackIndex in 0 until group.length) {
                            val format = group.getTrackFormat(trackIndex)
                            val language = format.language
                            val name = format.label ?: format.language ?: "Subtitles ${subtitleTracksList.size + 1}"
                            val isSelected = group.isTrackSelected(trackIndex)

                            val subTrack = SubtitleTrack(
                                id = "$groupIndex:$trackIndex",
                                name = name,
                                language = language,
                                isExternal = false,
                                url = null
                            )
                            subtitleTracksList.add(subTrack)
                            if (isSelected) {
                                selectedSubtitle = subTrack
                            }
                        }
                    }
                }

                setTracks(audioTracksList, subtitleTracksList)
                if (selectedAudio != null) {
                    _selectedAudioTrack = selectedAudio
                }
                if (selectedSubtitle != null) {
                    _selectedSubtitleTrack = selectedSubtitle
                }
            }
        })
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L) / 1000.0
                    updateTime(currentPos)
                    
                    // Обновляем буферизацию (сколько загружено вперед)
                    val bufferedPos = exoPlayer.bufferedPosition.coerceAtLeast(0L) / 1000.0
                    state.bufferAheadSeconds = (bufferedPos - currentPos).coerceAtLeast(0.0)
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun play() {
        exoPlayer.play()
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    override fun seek(time: Double) {
        updateTime(time)
        exoPlayer.seekTo((time * 1000).toLong())
    }

    override fun setMuted(muted: Boolean) {
        state.isMuted = muted
        exoPlayer.volume = if (muted) 0f else state.volume.toFloat()
    }

    override fun setVolume(volume: Double) {
        state.volume = volume
        if (!state.isMuted) {
            exoPlayer.volume = volume.toFloat()
        }
    }
    
    override fun selectAudioTrack(track: AudioTrack) {
        super.selectAudioTrack(track)
        val parts = track.id.split(":")
        if (parts.size == 2) {
            val groupIndex = parts[0].toIntOrNull() ?: return
            val trackIndex = parts[1].toIntOrNull() ?: return
            val groups = exoPlayer.currentTracks.groups
            if (groupIndex < groups.size) {
                val group = groups[groupIndex]
                val override = TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                    .buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    .setOverrideForType(override)
                    .build()
            }
        } else {
            val trackIndex = track.id.toIntOrNull()
            if (trackIndex != null) {
                var currentAudioIndex = 0
                for (group in exoPlayer.currentTracks.groups) {
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (i in 0 until group.length) {
                            if (currentAudioIndex == trackIndex || (currentAudioIndex + 1) == trackIndex) {
                                val override = TrackSelectionOverride(group.mediaTrackGroup, i)
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                    .buildUpon()
                                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                    .setOverrideForType(override)
                                    .build()
                                return
                            }
                            currentAudioIndex++
                        }
                    }
                }
            }
        }
    }

    override fun selectSubtitleTrack(track: SubtitleTrack?) {
        super.selectSubtitleTrack(track)
        if (track == null) {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            val parts = track.id.split(":")
            if (parts.size == 2) {
                val groupIndex = parts[0].toIntOrNull() ?: return
                val trackIndex = parts[1].toIntOrNull() ?: return
                val groups = exoPlayer.currentTracks.groups
                if (groupIndex < groups.size) {
                    val group = groups[groupIndex]
                    val override = TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .setOverrideForType(override)
                        .build()
                }
            } else {
                val trackIndex = track.id.toIntOrNull()
                if (trackIndex != null) {
                    var currentTextIndex = 0
                    for (group in exoPlayer.currentTracks.groups) {
                        if (group.type == C.TRACK_TYPE_TEXT) {
                            for (i in 0 until group.length) {
                                if (currentTextIndex == trackIndex || (currentTextIndex + 1) == trackIndex) {
                                    val override = TrackSelectionOverride(group.mediaTrackGroup, i)
                                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                        .buildUpon()
                                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                        .setOverrideForType(override)
                                        .build()
                                    return
                                }
                                currentTextIndex++
                            }
                        }
                    }
                }
            }
        }
    }

    fun release() {
        stopProgressTracker()
        exoPlayer.release() // Критично освобождать кодеки
    }
}
