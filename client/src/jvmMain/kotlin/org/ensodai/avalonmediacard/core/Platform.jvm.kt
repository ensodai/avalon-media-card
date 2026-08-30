package org.ensodai.avalonmediacard.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.ensodai.avalonmediacard.core.player.DesktopMpvPlaybackController
import org.ensodai.avalonmediacard.core.player.MpvVideoSurface
import org.ensodai.avalonmediacard.core.player.StreamUrlResolver
import org.ensodai.avalonmediacard.data.TokenStorage
import org.ensodai.avalonmediacard.data.platformServerUrl
import org.ensodai.avalonmediacard.presentation.screens.player.action.PlayerActions
import org.ensodai.avalonmediacard.presentation.screens.player.component.UnifiedVideoPlayer
import org.ensodai.avalonmediacard.presentation.screens.player.viewState.PlayerViewState
import org.koin.compose.koinInject
import java.awt.Desktop
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.KeyboardFocusManager
import java.awt.Window
import java.net.URI

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

private val jvmPrefs = mutableMapOf<String, String>()

actual fun saveSetting(key: String, value: String) {
    jvmPrefs[key] = value
}

actual fun loadSetting(key: String): String? {
    return jvmPrefs[key]
}

actual fun openUrl(url: String) {
    try {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

actual fun getUrlQueryParameters(): Map<String, String> = emptyMap()

actual fun openInExternalPlayer(streamUrl: String, title: String) {
    openUrl(streamUrl)
}

actual fun clearUrlQueryParameters() {}

actual fun togglePlatformFullscreen() {}

@Composable
actual fun VideoPlayer(
    state: PlayerViewState,
    actions: PlayerActions,
    modifier: Modifier
) {
    val tokenStorage = koinInject<TokenStorage>()
    val controller = remember { DesktopMpvPlaybackController() }

    val url = state.currentStreamUrl
    val audioTrackIndex = state.selectedAudioTrackIndex
    val resolvedUrl = remember(url, audioTrackIndex) {
        StreamUrlResolver.resolve(
            rawUrl = url,
            serverUrl = tokenStorage.cachedServerUrl,
            fallbackServerUrl = platformServerUrl,
            audioTrackIndex = audioTrackIndex
        )
    }

    LaunchedEffect(state.audioTracks, state.subtitleTracks) {
        if (state.audioTracks.isNotEmpty() && controller.audioTracks.isEmpty()) {
            controller.setTracks(state.audioTracks, state.subtitleTracks)
        }
    }

    LaunchedEffect(state.selectedAudioTrackIndex) {
        val selectedIdx = state.selectedAudioTrackIndex
        if (selectedIdx != null && !StreamUrlResolver.isGstStream(resolvedUrl)) {
            val track = controller.audioTracks.find {
                it.id == selectedIdx.toString()
            }
            if (track != null && controller.selectedAudioTrack?.id != track.id) {
                controller.selectAudioTrack(track)
            }
        }
    }

    LaunchedEffect(state.selectedSubtitleTrack) {
        if (!StreamUrlResolver.isGstStream(resolvedUrl)) {
            controller.selectSubtitleTrack(state.selectedSubtitleTrack)
        }
    }

    var lastLoadedUrl by remember { mutableStateOf<String?>(null) }

    DisposableEffect(resolvedUrl) {
        if (!resolvedUrl.isNullOrBlank() && lastLoadedUrl != resolvedUrl) {
            lastLoadedUrl = resolvedUrl
            val startPos = state.currentTime.coerceAtLeast(0.0)
            controller.loadMedia(resolvedUrl, startPos)
        } else if (resolvedUrl.isNullOrBlank()) {
            lastLoadedUrl = null
            controller.stop()
        }
        onDispose {}
    }


    DisposableEffect(Unit) {
        onDispose {
            controller.release()
        }
    }

    UnifiedVideoPlayer(
        state = state,
        actions = actions,
        controller = controller,
        videoSurface = {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                MpvVideoSurface(
                    controller = controller,
                    modifier = Modifier.fillMaxSize()
                )
            }
        },
        modifier = modifier
    )
}

@Composable
actual fun SystemFullscreenHandler(
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit
) {
    val window = remember {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow
            ?: Window.getWindows().firstOrNull { it.isShowing && it.isFocused }
            ?: Window.getWindows().firstOrNull { it.isShowing }
    }

    DisposableEffect(isFullscreen, window) {
        val frame = window as? Frame ?: return@DisposableEffect onDispose {}
        val graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice

        if (isFullscreen) {
            if (graphicsDevice.isFullScreenSupported) {
                try {
                    graphicsDevice.fullScreenWindow = frame
                } catch (_: Throwable) {
                    frame.extendedState = Frame.MAXIMIZED_BOTH
                }
            } else {
                frame.extendedState = Frame.MAXIMIZED_BOTH
            }
        } else {
            if (graphicsDevice.fullScreenWindow == frame) {
                graphicsDevice.fullScreenWindow = null
            }
            frame.extendedState = Frame.NORMAL
        }

        onDispose {
            if (graphicsDevice.fullScreenWindow == frame) {
                graphicsDevice.fullScreenWindow = null
            }
            frame.extendedState = Frame.NORMAL
        }
    }
}
