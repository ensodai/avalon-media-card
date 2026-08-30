package org.ensodai.avalonmediacard.presentation.screens.player.action

import org.ensodai.avalonmediacard.presentation.screens.player.PlayerViewModel
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlaybackStatus

fun PlayerViewModel.onProgressUpdate(current: Double, duration: Double) {
    if (viewState.value.currentStreamUrl.isNullOrBlank()) return
    updateViewState { state ->
        if (state.currentStreamUrl.isNullOrBlank()) return@updateViewState state


        val updatedPlaylist = if (state.playlist.isNotEmpty()) {
            state.playlist.map { ep ->
                if (ep.url == state.currentStreamUrl || (ep.canonicalId.isNotBlank() && ep.canonicalId == state.currentStreamId)) {
                    val epDur = ep.durationSeconds ?: (if (duration > 0) duration else 0.0)
                    val isWatchedNow = epDur > 0.0 && current >= epDur * 0.9
                    ep.copy(
                        watchedProgressSeconds = current.toLong(),
                        isWatched = isWatchedNow || ep.isWatched
                    )
                } else {
                    ep
                }
            }
        } else {
            state.playlist
        }

        val newStatus = if (state.status == PlaybackStatus.BUFFERING && current > 0.0) {
            PlaybackStatus.PLAYING
        } else {
            state.status
        }

        state.copy(
            currentTime = current,
            duration = if (duration > 0) duration else state.duration,
            status = newStatus,
            playlist = updatedPlaylist
        )
    }
    checkAndStartSyncLoop()
}

