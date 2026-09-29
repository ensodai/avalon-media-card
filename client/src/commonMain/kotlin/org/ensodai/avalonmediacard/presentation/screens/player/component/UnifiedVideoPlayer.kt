package org.ensodai.avalonmediacard.presentation.screens.player.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.ensodai.avalonmediacard.core.PlaybackController
import org.ensodai.avalonmediacard.core.SystemFullscreenHandler
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.player.action.PlayerActions
import org.ensodai.avalonmediacard.presentation.screens.player.component.pc.PlayerBottomBar
import org.ensodai.avalonmediacard.presentation.screens.player.component.pc.PlayerInputHandler
import org.ensodai.avalonmediacard.presentation.screens.player.component.pc.PlayerRightPanelOverlay
import org.ensodai.avalonmediacard.presentation.screens.player.component.pc.PlayerTopBar
import org.ensodai.avalonmediacard.presentation.screens.player.component.pc.UnifiedVideoPlayerLayout
import org.ensodai.avalonmediacard.presentation.screens.player.component.pc.chat.WatchPartyChatPanel
import org.ensodai.avalonmediacard.presentation.screens.player.component.tv.TvPlayerLayout
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerMode
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
        val showUiOverlay = !state.isFullscreen || isMouseActive || state.chatState.isInputFocused
        val showEpisodesPanel = state.hasEpisodesContext && showUiOverlay
        val showChatPanel = state.mode == PlayerMode.WATCH_PARTY && showUiOverlay
        val rightPadding = if (showEpisodesPanel) 360.dp + 48.dp else 24.dp

        PlayerInputHandler(
            actions = actions,
            controller = controller,
            isFullscreen = state.isFullscreen,
            showUiOverlay = showUiOverlay,
            isChatInputFocused = state.chatState.isInputFocused,
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
                showUiOverlay = showUiOverlay,
                videoSurface = videoSurface,
                centerOverlays = {
                    PlayerCenterOverlays(
                        controller = controller,
                        url = state.currentStreamUrl,
                        title = state.title,
                        errorOverride = state.errorMessage,
                        onTap = { actions.onPlayPauseClicked() },
                        modifier = Modifier.fillMaxSize()
                    )
                },
                topBar = {
                    val animatedRightPadding by animateDpAsState(
                        targetValue = rightPadding,
                        label = "topBarRightPadding"
                    )
                    PlayerTopBar(
                        state = state,
                        actions = actions,
                        rightPadding = animatedRightPadding
                    )
                },
                bottomBar = {
                    PlayerBottomBar(
                        state = state,
                        actions = actions,
                        controller = controller
                    )
                },
                leftPanelOverlay = {
                    AnimatedVisibility(
                        visible = showChatPanel,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 84.dp, bottom = 100.dp, start = 24.dp)
                    ) {
                        WatchPartyChatPanel(
                            chatState = state.chatState,
                            actions = actions.chat,
                            episodeTitle = state.displayTitleData.bottomText.takeIf { it.isNotBlank() && it != state.title }
                        )
                    }
                },
                rightPanelOverlay = {
                    PlayerRightPanelOverlay(
                        showEpisodes = showEpisodesPanel,
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
