package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.playerSlot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalRootOverlay
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.viewState.DetailsViewState.PlayerState
import org.ensodai.avalonmediacard.presentation.screens.player.PlayerScreen
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerInitParams

@Composable
fun PlayerSectionSlot(
    mediaKey: MediaKey,
    seriesTitle: String,
    playerState: PlayerState,
    onClose: () -> Unit,
    onRequestOtherSource: (() -> Unit)? = null,
    onAction: ((Action) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val rootOverlay = LocalRootOverlay.current

    DisposableEffect(playerState, seriesTitle) {
        val playing = playerState as? PlayerState.Playing
        val preparing = playerState as? PlayerState.Preparing
        val error = playerState as? PlayerState.Error

        rootOverlay.value = when {
            playing != null -> {
                val content: @Composable () -> Unit = {
                    PlayerScreen(
                        params = PlayerInitParams(
                            title = playing.title,
                            seriesTitle = seriesTitle,
                            mediaKey = mediaKey,
                            streamUrl = playing.streamUrl,
                            streamId = playing.streamId,
                            durationSeconds = playing.duration,
                            startPositionSeconds = playing.startPositionSeconds,
                            playlist = playing.playlist,
                            audioTracks = playing.audioTracks,
                            subtitleTracks = playing.subtitleTracks,
                            audioTrackIndex = playing.audioTrackIndex
                        ),
                        onRequestOtherSource = {
                            rootOverlay.value = null
                            onClose()
                            onRequestOtherSource?.invoke()
                        },
                        onClose = {
                            rootOverlay.value = null
                            onClose()
                        },
                        modifier = modifier
                    )
                }
                content
            }
            preparing != null -> {
                val content: @Composable () -> Unit = {
                    PlayerScreen(
                        params = PlayerInitParams(
                            title = preparing.title,
                            seriesTitle = seriesTitle,
                            mediaKey = mediaKey,
                            streamUrl = null,
                            streamId = null,
                            targetSeason = preparing.targetSeason,
                            targetEpisode = preparing.targetEpisode,
                            durationSeconds = null,
                            startPositionSeconds = null,
                            playlist = emptyList(),
                            audioTracks = emptyList(),
                            subtitleTracks = emptyList()
                        ),
                        onRequestOtherSource = {
                            rootOverlay.value = null
                            onClose()
                            onRequestOtherSource?.invoke()
                        },
                        onClose = {
                            rootOverlay.value = null
                            onClose()
                        },
                        modifier = modifier
                    )
                }
                content
            }
            error != null -> {
                val content: @Composable () -> Unit = {
                    Box(modifier = modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                        Text(error.message, color = Color.Red, fontSize = 18.sp)
                    }
                }
                content
            }
            else -> null
        }

        onDispose {
            rootOverlay.value = null
        }
    }
}
