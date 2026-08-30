package org.ensodai.avalonmediacard.presentation.screens.player.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.core.PlaybackController
import org.ensodai.avalonmediacard.core.SystemFullscreenHandler
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.player.action.PlayerActions
import org.ensodai.avalonmediacard.presentation.screens.player.component.pc.PlayerBottomBar
import org.ensodai.avalonmediacard.presentation.screens.player.component.pc.PlayerInputHandler
import org.ensodai.avalonmediacard.presentation.screens.player.component.pc.PlayerRightPanelOverlay
import org.ensodai.avalonmediacard.presentation.screens.player.component.pc.PlayerTopBar
import org.ensodai.avalonmediacard.presentation.screens.player.component.pc.UnifiedVideoPlayerLayout
import org.ensodai.avalonmediacard.presentation.screens.player.component.tv.TvPlayerLayout
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlaybackStatus
import org.ensodai.avalonmediacard.presentation.screens.player.viewState.PlayerViewState
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun UnifiedVideoPlayer(
    state: PlayerViewState,
    actions: PlayerActions,
    controller: PlaybackController,
    videoSurface: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    SystemFullscreenHandler(
        isFullscreen = state.isFullscreen,
        onFullscreenChange = { actions.onFullscreenChanged(it) }
    )

    var mouseX by remember { mutableStateOf(0f) }
    var mouseY by remember { mutableStateOf(0f) }
    var isMouseActive by remember { mutableStateOf(true) }

    val episodesListState = rememberLazyListState()
    val episodesTabState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    val currentEpisode = state.currentEpisode

    // Регулярный репорт текущего времени из контроллера во ViewModel для обновления стейта
    LaunchedEffect(controller, state.currentStreamUrl) {
        while (true) {
            delay(1000.milliseconds)
            if (state.currentStreamUrl.isNullOrBlank()) continue
            val current = controller.state.currentTime

            val dur = controller.state.duration
            val isBuffering = controller.state.isBuffering
            val isPlaying = controller.state.isPlaying

            if (isBuffering) {
                if (state.status != PlaybackStatus.BUFFERING && state.status != PlaybackStatus.RECOVERING) {
                    actions.onPlaybackStateChanged(PlaybackStatus.BUFFERING)
                }
            } else if (isPlaying && current > 0.0) {
                if (state.status != PlaybackStatus.PLAYING) {
                    actions.onPlaybackStateChanged(PlaybackStatus.PLAYING)
                }
            } else if (!isPlaying && !isBuffering && state.status == PlaybackStatus.PLAYING) {
                actions.onPlaybackStateChanged(PlaybackStatus.PAUSED)
            }

            if (!isBuffering && dur > 0.0 && current >= 0.0) {
                actions.onProgressUpdate(current, dur)
            }
        }
    }


    // Auto-hide mouse UI in fullscreen
    LaunchedEffect(mouseX, mouseY, state.isFullscreen) {
        if (state.isFullscreen) {
            isMouseActive = true
            delay(3000.milliseconds)
            isMouseActive = false
        } else {
            isMouseActive = true
        }
    }

    val deviceTarget = LocalDeviceTarget.current

    if (deviceTarget.isTv || deviceTarget.isTouch) {
        TvPlayerLayout(
            state = state,
            actions = actions,
            controller = controller,
            videoSurface = videoSurface,
            modifier = modifier
        )
    } else {
        val showUiOverlay = !state.isFullscreen || isMouseActive
        val showRightPanel = state.hasEpisodesContext && showUiOverlay
        PlayerInputHandler(
            controller = controller,
            isFullscreen = state.isFullscreen,
            showUiOverlay = showUiOverlay,
            onFullscreenToggle = { actions.onToggleFullscreen() },
            onMouseMoved = { x, y ->
                mouseX = x
                mouseY = y
                isMouseActive = true
            },
            focusRequester = focusRequester,
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            UnifiedVideoPlayerLayout(
                isFullscreen = state.isFullscreen,
                showUiOverlay = showUiOverlay,
                showRightPanel = showRightPanel,
                hasEpisodesContext = state.hasEpisodesContext,
                videoSurface = videoSurface,
                centerOverlays = {
                    PlayerCenterOverlays(
                        controller = controller,
                        url = state.currentStreamUrl,
                        title = state.title,
                        errorOverride = state.errorMessage,
                        onTap = {
                            runCatching { focusRequester.requestFocus() }
                            if (controller.state.isPlaying) controller.pause() else controller.play()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                },
                topBar = {
                    val rightPadding = if (showRightPanel) 360.dp + 48.dp else 24.dp
                    PlayerTopBar(
                        state = state,
                        actions = actions,
                        rightPadding = rightPadding
                    )
                },
                bottomBar = {
                    PlayerBottomBar(
                        state = state,
                        actions = actions,
                        controller = controller
                    )
                },
                rightPanelOverlay = {
                    PlayerRightPanelOverlay(
                        visible = showRightPanel,
                        seasonEpisodes = state.seasonEpisodes,
                        currentStreamId = state.currentStreamId,
                        url = state.currentStreamUrl,
                        currentEpisode = state.currentEpisode,
                        isLoadingEpisodes = false,
                        listState = episodesListState,
                        tabState = episodesTabState,
                        onEpisodeClick = { actions.onEpisodeSelected(it) }
                    )
                }
            )
        }
    }
}
