package org.ensodai.avalonmediacard.presentation.screens.player.action

import org.ensodai.avalonmediacard.core.togglePlatformFullscreen
import org.ensodai.avalonmediacard.presentation.screens.player.PlayerViewModel
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlaybackStatus

fun PlayerViewModel.onPlayPauseClicked() {
    updateViewState { state ->
        val newStatus = if (state.isPlaying) PlaybackStatus.PAUSED else PlaybackStatus.PLAYING
        state.copy(status = newStatus)
    }
    checkAndStartSyncLoop()
}

fun PlayerViewModel.onSeek(targetSeconds: Double) {
    updateViewState { state ->
        state.copy(currentTime = targetSeconds)
    }
}

fun PlayerViewModel.onPlaybackStateChanged(status: PlaybackStatus) {
    updateViewState { state ->
        state.copy(status = status)
    }
    checkAndStartSyncLoop()
}

fun PlayerViewModel.onError(message: String) {
    updateViewState { state ->
        state.copy(
            status = PlaybackStatus.ERROR,
            errorMessage = message
        )
    }
    checkAndStartSyncLoop()
}

fun PlayerViewModel.onStreamRecovery() {
    updateViewState { state ->
        state.copy(
            status = PlaybackStatus.RECOVERING,
            errorMessage = null
        )
    }
    checkAndStartSyncLoop()
}

fun PlayerViewModel.onToggleFullscreen() {
    togglePlatformFullscreen()
    updateViewState { state ->
        state.copy(isFullscreen = !state.isFullscreen)
    }
}

fun PlayerViewModel.onFullscreenChanged(isFullscreen: Boolean) {
    updateViewState { state ->
        state.copy(isFullscreen = isFullscreen)
    }
}

fun PlayerViewModel.onControlsVisibilityChanged(visible: Boolean) {
    updateViewState { state ->
        state.copy(areControlsVisible = visible)
    }
}
