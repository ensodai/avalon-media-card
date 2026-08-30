package org.ensodai.avalonmediacard.presentation.screens.player.action

import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.presentation.screens.player.PlayerViewModel
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlaybackStatus

fun PlayerViewModel.onAudioTrackSelected(track: AudioTrack) {
    val trackIndex = track.id.toIntOrNull() ?: track.id.substringAfterLast(":").toIntOrNull()
    if (!track.url.isNullOrBlank()) {
        val currentSec = viewState.value.currentTime
        val fullUrl = resolveAbsoluteUrl(track.url!!)
        updateViewState { state ->
            state.copy(
                currentStreamUrl = fullUrl,
                currentTime = currentSec,
                selectedAudioTrackIndex = trackIndex,
                status = PlaybackStatus.BUFFERING
            )
        }
    } else {
        updateViewState { state ->
            state.copy(selectedAudioTrackIndex = trackIndex)
        }
    }
}

fun PlayerViewModel.onSubtitleTrackSelected(track: SubtitleTrack?) {
    updateViewState { state ->
        state.copy(selectedSubtitleTrack = track)
    }
}
