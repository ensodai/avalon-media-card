package org.ensodai.avalonmediacard.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import `is`.xyz.mpv.MPVLib
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.core.player.StreamUrlResolver
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.core.player.AndroidPlaybackController
import org.ensodai.avalonmediacard.core.player.MpvPlaybackController
import org.ensodai.avalonmediacard.core.player.PlayerLifecycleHandler
import org.ensodai.avalonmediacard.core.player.TorExoPlayerFactory
import org.ensodai.avalonmediacard.data.TokenStorage
import org.ensodai.avalonmediacard.data.platformServerUrl
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.presentation.screens.player.action.PlayerActions
import org.ensodai.avalonmediacard.presentation.screens.player.component.UnifiedVideoPlayer
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerEngine
import org.ensodai.avalonmediacard.presentation.screens.player.viewState.PlayerViewState
import org.koin.core.context.GlobalContext

private val logger = AppLogging.logger("VideoPlayer")

class AndroidPlatform : Platform {
    override val name: String = "Android TV"
}

actual fun getPlatform(): Platform = AndroidPlatform()

private val settingsMap = mutableMapOf<String, String>()

actual fun saveSetting(key: String, value: String) {
    settingsMap[key] = value
}

actual fun loadSetting(key: String): String? {
    return settingsMap[key]
}

actual fun openUrl(url: String) {
}

actual fun openInExternalPlayer(streamUrl: String, title: String) {
}

actual fun getUrlQueryParameters(): Map<String, String> = emptyMap()

actual fun clearUrlQueryParameters() {}

actual fun togglePlatformFullscreen() {}

@OptIn(UnstableApi::class)
@Composable
actual fun VideoPlayer(
    state: PlayerViewState,
    actions: PlayerActions,
    modifier: Modifier
) {
    val url = state.currentStreamUrl?.takeIf { it.isNotBlank() }

    val startPositionSeconds = state.currentTime.toInt()

    val context = LocalContext.current
    
    var currentEngine by remember { 
        mutableStateOf(state.defaultPlayerEngine) 
    }
    var fallbackTimeMs by remember(url) { mutableStateOf(startPositionSeconds.toLong() * 1000) }

    val exoPlayer = remember { TorExoPlayerFactory.create(context) }
    val mpvControllerLazy = remember { lazy { MpvPlaybackController(context) } }

    // Реактивное переключение движка при смене во ViewState
    LaunchedEffect(state.defaultPlayerEngine) {
        val newEngine = state.defaultPlayerEngine
        if (currentEngine != newEngine) {
            // Сохраняем текущую позицию перед переключением
            val pos = when (currentEngine) {
                PlayerEngine.MEDIA3 -> exoPlayer.currentPosition
                else -> (state.currentTime * 1000.0).toLong()
            }
            fallbackTimeMs = pos.coerceAtLeast(0L)
            
            // Останавливаем предыдущий движок
            when (currentEngine) {
                PlayerEngine.MEDIA3 -> {
                    exoPlayer.stop()
                    exoPlayer.clearMediaItems()
                }
                PlayerEngine.MPV -> {
                    if (mpvControllerLazy.isInitialized()) mpvControllerLazy.value.pause()
                }
            }
            
            currentEngine = newEngine
        }
    }

    val tokenStorage = org.koin.compose.koinInject<TokenStorage>()
    val audioTrackIndex = state.selectedAudioTrackIndex
    val resolvedUrl = remember(url, audioTrackIndex) {
        StreamUrlResolver.resolve(
            rawUrl = url,
            serverUrl = tokenStorage.cachedServerUrl,
            fallbackServerUrl = platformServerUrl,
            audioTrackIndex = audioTrackIndex
        )
    }

    val exoController = remember(exoPlayer) {
        AndroidPlaybackController(exoPlayer, onFatalError = { posMs ->
            if (currentEngine == PlayerEngine.MEDIA3) {
                fallbackTimeMs = if (posMs > 0L) posMs else exoPlayer.currentPosition.coerceAtLeast(0L)
                logger.e { "onFatalError: saving position=${fallbackTimeMs}ms, switching engine to MPV" }
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                currentEngine = PlayerEngine.MPV
                actions.onChangeDefaultPlayer(PlayerEngine.MPV)
            }
        })
    }
    
    val activeController = when (currentEngine) {
        PlayerEngine.MEDIA3 -> exoController
        PlayerEngine.MPV -> mpvControllerLazy.value
    }

    LaunchedEffect(state.audioTracks, state.subtitleTracks) {
        if (state.audioTracks.isNotEmpty() && activeController.audioTracks.isEmpty()) {
            activeController.setTracks(state.audioTracks, state.subtitleTracks)
        }
    }

    LaunchedEffect(state.selectedAudioTrackIndex) {
        val selectedIdx = state.selectedAudioTrackIndex
        if (selectedIdx != null && !StreamUrlResolver.isGstStream(resolvedUrl)) {
            val track = activeController.audioTracks.find {
                it.id == selectedIdx.toString()
            }
            if (track != null && activeController.selectedAudioTrack?.id != track.id) {
                activeController.selectAudioTrack(track)
            }
        }
    }

    LaunchedEffect(state.selectedSubtitleTrack) {
        if (!StreamUrlResolver.isGstStream(resolvedUrl)) {
            activeController.selectSubtitleTrack(state.selectedSubtitleTrack)
        }
    }

    var lastLoadedUrl by remember { mutableStateOf<String?>(null) }

    DisposableEffect(resolvedUrl, currentEngine) {
        if (resolvedUrl != null) {
            val isNewUrl = lastLoadedUrl != resolvedUrl
            if (isNewUrl) {
                lastLoadedUrl = resolvedUrl
                val startPosMs = (state.currentTime * 1000.0).toLong().coerceAtLeast(0L)
                fallbackTimeMs = startPosMs
            }

            when (currentEngine) {
                PlayerEngine.MEDIA3 -> {
                    val currentUri = exoPlayer.currentMediaItem?.localConfiguration?.uri?.toString()
                    if (currentUri != resolvedUrl) {
                        logger.d { "ExoPlayer PREPARE & PLAY for resolvedUrl=$resolvedUrl (restorePos=${fallbackTimeMs}ms)" }
                        val mediaItem = MediaItem.fromUri(resolvedUrl)
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.prepare()
                        exoPlayer.play()
                        if (fallbackTimeMs > 0L) {
                            exoPlayer.seekTo(fallbackTimeMs)
                        }
                    } else {
                        logger.d { "ExoPlayer SKIP PREPARE because url is identical" }
                    }
                }
                PlayerEngine.MPV -> {
                    logger.d { "MPV LOAD & PLAY for resolvedUrl=$resolvedUrl at timeMs=$fallbackTimeMs" }
                    mpvControllerLazy.value.loadAndPlay(resolvedUrl, fallbackTimeMs)
                }
            }
        }
        onDispose {
            logger.d { "VideoPlayer DisposableEffect DISPOSED for engine=$currentEngine" }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoController.release()
            if (mpvControllerLazy.isInitialized()) mpvControllerLazy.value.release()
        }
    }

    PlayerLifecycleHandler(controller = activeController)

    UnifiedVideoPlayer(
        videoSurface = {
            when (currentEngine) {
                PlayerEngine.MEDIA3 -> {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                isFocusable = false
                                isFocusableInTouchMode = false
                                descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
                                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize().background(Color.Black)
                    )
                }
                PlayerEngine.MPV -> {
                    AndroidView(
                        factory = { ctx ->
                            SurfaceView(ctx).apply {
                                setZOrderMediaOverlay(true)
                                isFocusable = false
                                isFocusableInTouchMode = false
                                holder.addCallback(object : SurfaceHolder.Callback {
                                    override fun surfaceCreated(holder: SurfaceHolder) {
                                        logger.d { "MPV Surface created -> attachSurface" }
                                        MPVLib.attachSurface(holder.surface)
                                        MPVLib.setPropertyString("vo", "gpu")
                                    }

                                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                                        MPVLib.setPropertyString("android-surface-size", "${width}x$height")
                                    }

                                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                                        logger.d { "MPV Surface destroyed -> detachSurface" }
                                        MPVLib.setPropertyString("vo", "null")
                                        MPVLib.detachSurface()
                                    }
                                })
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize().background(Color.Black)
                    )
                }
            }
        },
        state = state,
        actions = actions,
        controller = activeController,
        modifier = modifier
    )
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
actual fun SystemFullscreenHandler(
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    DisposableEffect(isFullscreen, activity) {
        val window = activity?.window ?: return@DisposableEffect onDispose {}
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        // 1. Prevent screen timeout during video playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 2. Hide system bars (immersive mode with swipe to temporarily reveal)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
