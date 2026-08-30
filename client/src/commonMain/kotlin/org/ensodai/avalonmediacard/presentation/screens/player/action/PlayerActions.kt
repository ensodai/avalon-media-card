package org.ensodai.avalonmediacard.presentation.screens.player.action

import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.contract.plugins.VideoQuality
import org.ensodai.avalonmediacard.presentation.core.mvi.BaseActions
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlaybackStatus
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerEngine

data class PlayerActions(
    val onPlayPauseClicked: () -> Unit,
    val onSeek: (Double) -> Unit,
    val onEpisodeSelected: (MediaStream) -> Unit,
    val onAudioTrackSelected: (AudioTrack) -> Unit,
    val onSubtitleTrackSelected: (SubtitleTrack?) -> Unit,
    val onQualitySelected: (VideoQuality) -> Unit = {},
    val onToggleFullscreen: () -> Unit,
    val onFullscreenChanged: (Boolean) -> Unit = {},
    val onControlsVisibilityChanged: (Boolean) -> Unit,
    val onProgressUpdate: (Double, Double) -> Unit,
    val onPlaybackStateChanged: (PlaybackStatus) -> Unit,
    val onError: (String) -> Unit,
    val onStreamRecovery: () -> Unit,
    val onCloseClicked: () -> Unit,
    val onToggleEpisodeWatched: (MediaStream) -> Unit = {},
    val onRateEpisode: (MediaStream, Int) -> Unit = { _, _ -> },
    val onChangeDefaultPlayer: (PlayerEngine) -> Unit = {},
    val onRequestOtherSource: () -> Unit = {}
) : BaseActions()
