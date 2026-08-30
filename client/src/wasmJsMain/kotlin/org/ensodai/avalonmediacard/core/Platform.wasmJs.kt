@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.ensodai.avalonmediacard.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.core.player.StreamUrlResolver
import org.ensodai.avalonmediacard.core.player.engine.WasmStreamEngine
import org.ensodai.avalonmediacard.core.player.engine.WasmStreamEngineFactory
import org.ensodai.avalonmediacard.presentation.screens.player.action.PlayerActions
import org.ensodai.avalonmediacard.presentation.screens.player.component.UnifiedVideoPlayer
import org.ensodai.avalonmediacard.presentation.screens.player.viewState.PlayerViewState
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLVideoElement
import kotlin.time.Duration.Companion.milliseconds

class WasmPlatform : Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

actual fun saveSetting(key: String, value: String) {
    window.localStorage.setItem(key, value)
}

actual fun loadSetting(key: String): String? {
    return window.localStorage.getItem(key)
}

actual fun openUrl(url: String) {
    window.open(url, "_blank")
}

actual fun getUrlQueryParameters(): Map<String, String> {
    val search = window.location.search
    if (search.isEmpty() || !search.startsWith("?")) return emptyMap()
    val params = mutableMapOf<String, String>()
    val pairs = search.substring(1).split("&")
    for (pair in pairs) {
        val parts = pair.split("=", limit = 2)
        if (parts.size == 2) {
            params[parts[0]] = parts[1]
        } else if (parts.size == 1) {
            params[parts[0]] = ""
        }
    }
    return params
}

@JsFun("() => ({ maxBufferSize: 120 * 1000 * 1000, backBufferLength: 90, maxBufferLength: 120, maxMaxBufferLength: 600, manifestLoadingTimeOut: 120000, manifestLoadingMaxRetryTimeout: 120000, levelLoadingTimeOut: 120000, fragLoadingTimeOut: 120000, fragLoadingMaxRetry: 3, manifestLoadingMaxRetry: 3, maxBufferHole: 0.5, nudgeMaxRetries: 5, highBufferWatchdogPeriod: 2, lowBufferWatchdogPeriod: 0.5 })")
external fun createHlsConfig(): JsAny

@JsName("Hls")
external class Hls(config: JsAny? = definedExternally) : JsAny {
    fun loadSource(src: String)
    fun attachMedia(media: HTMLVideoElement)
    fun on(event: String, callback: (JsAny) -> Unit)
    fun destroy()

    companion object {
        fun isSupported(): Boolean
    }
}


actual fun clearUrlQueryParameters() {
    val cleanUrl = window.location.origin + window.location.pathname
    window.history.replaceState(null, "", cleanUrl)
}

@Composable
fun VideoUnderlay(
    element: HTMLElement,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var xOffset by remember { mutableStateOf(0f) }
    var yOffset by remember { mutableStateOf(0f) }
    var width by remember { mutableStateOf(0f) }
    var height by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                val size = coordinates.size

                xOffset = position.x
                yOffset = position.y
                width = size.width.toFloat()
                height = size.height.toFloat()

                with(density) {
                    val cssLeft = "${xOffset / density.density}px"
                    val cssTop = "${yOffset / density.density}px"
                    val cssWidth = "${width / density.density}px"
                    val cssHeight = "${height / density.density}px"

                    element.setAttribute(
                        "style",
                        "position: absolute; left: $cssLeft; top: $cssTop; width: $cssWidth; height: $cssHeight; z-index: -1; pointer-events: none; background: black;"
                    )
                }
            }
            .drawBehind {
                drawRect(
                    color = Color.Transparent,
                    size = this.size,
                    blendMode = BlendMode.Clear
                )
            }
    )
}


@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
actual fun openInExternalPlayer(streamUrl: String, title: String) {
    js(
        """ {
    const ua = navigator.userAgent.toLowerCase();
    const encodedUrl = encodeURIComponent(streamUrl);
    
    if (ua.includes("android")) {
        const intentUrl = streamUrl.replace(/^https?:\/\//, "");
        const scheme = streamUrl.startsWith("https") ? "https" : "http";
        const vlcIntent = "intent://" + intentUrl + "#Intent;scheme=" + scheme + ";package=org.videolan.vlc;S.title=" + encodeURIComponent(title) + ";end;";
        window.location.href = vlcIntent;
    } else if (ua.includes("iphone") || ua.includes("ipad") || ua.includes("ipod")) {
        window.location.href = "vlc-x-callback://x-callback-url/stream?url=" + encodedUrl;
    } else if (ua.includes("macintosh") || ua.includes("mac os x")) {
        window.location.href = "iina://weblink?url=" + encodedUrl;
    } else {
        const m3uContent = "#EXTM3U\n#EXTINF:-1," + title + "\n" + streamUrl;
        const blob = new Blob([m3uContent], { type: "text/plain" });
        const blobUrl = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = blobUrl;
        a.download = title.replace(/[^a-zA-Z0-9А-Яа-я]/g, "_") + ".m3u";
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(blobUrl);
    }
} """
    )
}

@JsFun("(video) => { if (!video || !video.buffered) return 0.0; const timeRanges = video.buffered; const currentTime = video.currentTime; let bufferAhead = 0.0; const tolerance = 0.5; for (let i = 0; i < timeRanges.length; i++) { const start = timeRanges.start(i); const end = timeRanges.end(i); if (currentTime >= (start - tolerance) && currentTime <= end) { bufferAhead = end - currentTime; break; } } return Math.max(0, bufferAhead); }")
private external fun calculateBufferAheadWasm(video: HTMLVideoElement): Double

@JsFun("(video, targetTime) => { const timeRanges = video.buffered; const tolerance = 0.5; for (let i = 0; i < timeRanges.length; i++) { if (targetTime >= (timeRanges.start(i) - tolerance) && targetTime <= timeRanges.end(i)) { return true; } } return false; }")
private external fun isTimeInBufferedRangesWasm(video: HTMLVideoElement, targetTime: Double): Boolean

@JsFun("(obj) => { try { const p = obj.play(); if (p && p.catch) { p.catch(e => console.warn('safePlayWasm ignored error:', e)); } } catch (e) { console.warn('safePlayWasm synchronous error:', e); } }")
internal external fun safePlayWasm(obj: JsAny)


class VideoElementPlaybackController(
    val videoElement: HTMLVideoElement
) : org.ensodai.avalonmediacard.core.player.CommonPlaybackController() {

    private var intentToPlay = true
    var isSeeking = false
        private set

    init {
        setupEventListeners()
    }

    private fun setupEventListeners() {
        val listener: (org.w3c.dom.events.Event) -> Unit = { event ->
            when (event.type) {
                "progress" -> if (!isSeeking) evaluateBufferState()
                "waiting" -> {
                    state.isBuffering = true
                    state.isPlaying = false
                }

                "playing" -> {
                    state.isPlaying = true
                    state.isBuffering = false
                }

                "seeking" -> {
                    isSeeking = true
                }

                "seeked" -> {
                    isSeeking = false
                    updateTime(videoElement.currentTime)
                    val isReady = isTimeInBufferedRangesWasm(
                        videoElement,
                        videoElement.currentTime
                    ) || videoElement.readyState >= 2
                    if (isReady) {
                        state.isBuffering = false
                        if (intentToPlay) {
                            safePlayWasm(videoElement)
                            state.isPlaying = true
                        } else {
                            videoElement.pause()
                            state.isPlaying = false
                        }
                    }
                }

                "canplay", "canplaythrough" -> {
                    if (!isSeeking) {
                        val isReady = isTimeInBufferedRangesWasm(
                            videoElement,
                            videoElement.currentTime
                        ) || videoElement.readyState >= 2
                        if (isReady) {
                            state.isBuffering = false
                            if (intentToPlay) {
                                safePlayWasm(videoElement)
                                state.isPlaying = true
                            } else {
                                videoElement.pause()
                                state.isPlaying = false
                            }
                        }
                    }
                }

                "volumechange" -> {
                    state.volume = videoElement.volume
                    state.isMuted = videoElement.muted
                }

                "loadedmetadata", "durationchange", "timeupdate" -> {
                    val d = videoElement.duration
                    state.duration = if (d.isNaN() || !d.isFinite()) 0.0 else d

                    if (event.type == "timeupdate" && !isSeeking) {
                        updateTime(videoElement.currentTime)
                        evaluateBufferState()
                    }
                }
            }
        }

        videoElement.addEventListener("progress", listener)
        videoElement.addEventListener("waiting", listener)
        videoElement.addEventListener("playing", listener)
        videoElement.addEventListener("timeupdate", listener)
        videoElement.addEventListener("volumechange", listener)
        videoElement.addEventListener("loadedmetadata", listener)
        videoElement.addEventListener("durationchange", listener)
        videoElement.addEventListener("seeking", listener)
        videoElement.addEventListener("seeked", listener)
        videoElement.addEventListener("canplay", listener)
        videoElement.addEventListener("canplaythrough", listener)
    }

    private fun evaluateBufferState() {
        if (isSeeking) return
        val currentBufferAhead = calculateBufferAheadWasm(videoElement)
        val isVideoReady = videoElement.readyState >= 2
        state.bufferAheadSeconds = currentBufferAhead

        if (intentToPlay) {
            if (state.isBuffering) {
                if (isVideoReady && (currentBufferAhead >= REBUFFERING_GOAL || currentBufferAhead >= MIN_BUFFER_TO_PLAY)) {
                    safePlayWasm(videoElement)
                    state.isBuffering = false
                    state.isPlaying = true
                }
            } else {
                if (!isVideoReady || currentBufferAhead < CRITICAL_BUFFER_LEVEL) {
                    videoElement.pause()
                    state.isBuffering = true
                    state.isPlaying = false
                } else {
                    safePlayWasm(videoElement)
                }
            }
        }
    }

    override fun togglePlayPause() {
        intentToPlay = !intentToPlay
        if (intentToPlay) {
            evaluateBufferState()
        } else {
            videoElement.pause()
            state.isPlaying = false
        }
    }

    override fun play() {
        intentToPlay = true
        evaluateBufferState()
    }

    override fun pause() {
        intentToPlay = false
        videoElement.pause()
        state.isPlaying = false
    }

    override fun seek(time: Double) {
        if (time.isNaN() || !time.isFinite()) return
        isSeeking = true
        videoElement.currentTime = time
        state.currentTime = time
    }

    override fun setMuted(muted: Boolean) {
        videoElement.muted = muted
        state.isMuted = muted
    }

    override fun setVolume(volume: Double) {
        val clamped = volume.coerceIn(0.0, 1.0)
        videoElement.volume = clamped
        state.volume = clamped
    }

    companion object {
        const val BUFFERING_CHECK_INTERVAL = 500
        const val MIN_BUFFER_TO_PLAY = 2.0
        const val CRITICAL_BUFFER_LEVEL = 0.5
        const val REBUFFERING_GOAL = 5.0
    }
}


@Composable
actual fun VideoPlayer(
    state: PlayerViewState,
    actions: PlayerActions,
    modifier: Modifier
) {
    val url = state.currentStreamUrl?.takeIf { it.isNotBlank() }

    val audioTrackIndex = state.selectedAudioTrackIndex
    val audioTracks = state.audioTracks
    val subtitleTracks = state.subtitleTracks
    val startPositionSeconds = state.currentTime.toInt()

    val videoElement = remember(url) {
        (document.createElement("video") as HTMLVideoElement).apply {
            autoplay = true
            muted = false
            setAttribute("playsinline", "true")
        }
    }

    val container = remember {
        document.getElementById("video-underlay-container")
            ?: document.createElement("div").apply {
                id = "video-underlay-container"
                setAttribute(
                    "style",
                    "position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: -1; pointer-events: none;"
                )
                document.body?.appendChild(this)
            }
    }

    val controller = remember(videoElement) { VideoElementPlaybackController(videoElement) }

    var lastKnownTime by remember { mutableStateOf(0.0) }
    var currentUrl by remember { mutableStateOf<String?>(null) }
    var activeEngine by remember { mutableStateOf<WasmStreamEngine?>(null) }

    if (currentUrl != url) {
        lastKnownTime =
            if (startPositionSeconds > 0) startPositionSeconds.toDouble() else 0.0
        currentUrl = url
    }

    LaunchedEffect(state.selectedAudioTrackIndex, activeEngine) {
        val selectedIdx = state.selectedAudioTrackIndex
        if (selectedIdx != null && !StreamUrlResolver.isGstStream(url)) {
            activeEngine?.selectAudioTrack(selectedIdx)
            val track = controller.audioTracks.find { it.id == selectedIdx.toString() }
            if (track != null && controller.selectedAudioTrack?.id != track.id) {
                controller.selectAudioTrack(track)
            }
        }
    }

    LaunchedEffect(controller.selectedSubtitleTrack, activeEngine) {
        val track = controller.selectedSubtitleTrack
        val trackId = track?.id?.toIntOrNull() ?: -1
        activeEngine?.selectSubtitleTrack(trackId)
    }

    var activeSubtitleChunks by remember { mutableStateOf<List<SubtitleChunk>>(emptyList()) }
    
    LaunchedEffect(controller.selectedSubtitleTrack, url) {
        val track = controller.selectedSubtitleTrack
        val trackId = track?.id?.toIntOrNull()
        if (trackId == null || url == null || !StreamUrlResolver.isGstStream(url)) {
            activeSubtitleChunks = emptyList()
            controller.state.currentSubtitleText = ""
            return@LaunchedEffect
        }
        
        val baseUrl = url.substringBefore("master.m3u8")
        val queryStr = if (url.contains("?")) "?" + url.substringAfter("?") else ""
        val m3u8Url = "${baseUrl}subs/$trackId.m3u8$queryStr"
        
        activeSubtitleChunks = VttSubtitleFetcher.fetchSubtitleChunks(m3u8Url)
    }

    LaunchedEffect(activeSubtitleChunks) {
        if (activeSubtitleChunks.isEmpty()) return@LaunchedEffect
        
        while (true) {
            val currentTime = lastKnownTime
            val nowMs = kotlinx.browser.window.performance.now()
            
            val chunksToDownload = activeSubtitleChunks.filter { chunk ->
                when (val st = chunk.state) {
                    is ChunkState.Idle -> chunk.start <= currentTime + 60.0 && chunk.end >= currentTime - 10.0
                    is ChunkState.Error -> {
                        st.retries < 5 && (nowMs - st.lastAttemptMs > 1500) && chunk.start <= currentTime + 60.0 && chunk.end >= currentTime - 10.0
                    }
                    else -> false
                }
            }
            
            for (chunk in chunksToDownload) {
                val retries = (chunk.state as? ChunkState.Error)?.retries ?: 0
                chunk.state = ChunkState.Loading
                
                val rawCues = VttSubtitleFetcher.fetchAndParseVtt(chunk.url)
                if (rawCues.isNotEmpty()) {
                    val shiftedCues = rawCues.map { cue -> 
                        cue.copy(
                            start = cue.start + chunk.start, 
                            end = cue.end + chunk.start
                        ) 
                    }
                    chunk.state = ChunkState.Loaded(shiftedCues)
                } else {
                    chunk.state = ChunkState.Error(retries + 1, nowMs)
                }
            }
            delay(300.milliseconds)
        }
    }

    LaunchedEffect(state.audioTracks, state.subtitleTracks) {
        if (state.audioTracks.isNotEmpty() && controller.audioTracks.isEmpty()) {
            controller.setTracks(state.audioTracks, state.subtitleTracks)
        }
    }

    DisposableEffect(url) {
        controller.state.isBuffering = true
        container.appendChild(videoElement)

        val waitingListener: (org.w3c.dom.events.Event) -> Unit = {
            controller.state.isBuffering = true
        }
        val playingListener: (org.w3c.dom.events.Event) -> Unit = {
            controller.state.isBuffering = false
        }
        val visibilityListener: (org.w3c.dom.events.Event) -> Unit = {
            activeEngine?.onVisibilityChanged(isDocumentVisibleWasm())
        }

        videoElement.addEventListener("waiting", waitingListener)
        videoElement.addEventListener("stalled", waitingListener)
        videoElement.addEventListener("playing", playingListener)
        videoElement.addEventListener("canplay", playingListener)
        document.addEventListener("visibilitychange", visibilityListener)

        var engine: WasmStreamEngine? = null
        if (url != null) {
            val finalUrl = StreamUrlResolver.resolve(
                rawUrl = url,
                serverUrl = null,
                fallbackServerUrl = "",
                audioTrackIndex = audioTrackIndex
            ) ?: url

            engine = WasmStreamEngineFactory.create(
                url = finalUrl,
                videoElement = videoElement,
                controller = controller,
                lastKnownTimeProvider = { lastKnownTime }
            )

            if (engine != null) {
                activeEngine = engine
                engine.load(finalUrl, lastKnownTime, audioTrackIndex)
            } else {
                controller.state.playbackError =
                    "This video format is not supported in the browser. Please open in an external player."
                controller.state.isBuffering = false
            }
        }

        onDispose {
            document.removeEventListener("visibilitychange", visibilityListener)
            videoElement.removeEventListener("waiting", waitingListener)
            videoElement.removeEventListener("stalled", waitingListener)
            videoElement.removeEventListener("playing", playingListener)
            videoElement.removeEventListener("canplay", playingListener)
            engine?.destroy()
            activeEngine = null
            videoElement.pause()
            videoElement.src = ""
            videoElement.load()
            if (container.contains(videoElement)) {
                container.removeChild(videoElement)
            }
        }
    }

    val initialStartPos = remember(url) { startPositionSeconds }
    DisposableEffect(url, initialStartPos) {
        var hasSeeked = false
        val listener: (org.w3c.dom.events.Event) -> Unit = { _ ->
            if (!hasSeeked) {
                videoElement.currentTime = lastKnownTime
                hasSeeked = true
            }
        }
        videoElement.addEventListener("loadedmetadata", listener)
        onDispose {
            videoElement.removeEventListener("loadedmetadata", listener)
        }
    }

    LaunchedEffect(controller, activeSubtitleChunks) {
        while (true) {
            val state = controller.state
            if (!controller.isSeeking) {
                state.currentTime = videoElement.currentTime
                if (videoElement.readyState > 0) {
                    lastKnownTime = videoElement.currentTime
                }
            }
            if (videoElement.duration > 0 && !videoElement.duration.isNaN()) {
                state.duration = videoElement.duration
            }
            state.isMuted = videoElement.muted
            state.volume = videoElement.volume

            val currentCues = activeSubtitleChunks.flatMap { chunk ->
                (chunk.state as? ChunkState.Loaded)?.cues ?: emptyList()
            }.filter { 
                it.start <= videoElement.currentTime && it.end >= videoElement.currentTime
            }
            val newText = currentCues.joinToString("\n") { it.text }
            if (state.currentSubtitleText != newText) {
                state.currentSubtitleText = newText
            }

            delay(250.milliseconds)
        }
    }

    UnifiedVideoPlayer(
        state = state,
        actions = actions,
        controller = controller,
        videoSurface = {
            VideoUnderlay(
                element = videoElement,
                modifier = Modifier.fillMaxSize()
            )
        },
        modifier = modifier
    )
}


@JsFun("() => { if (document.fullscreenElement != null || document.webkitFullscreenElement != null) { if (document.exitFullscreen) document.exitFullscreen(); else if (document.webkitExitFullscreen) document.webkitExitFullscreen(); } else { if (document.documentElement.requestFullscreen) document.documentElement.requestFullscreen(); else if (document.documentElement.webkitRequestFullscreen) document.documentElement.webkitRequestFullscreen(); } }")
private external fun toggleFullscreenJs()

actual fun togglePlatformFullscreen() {
    toggleFullscreenJs()
}

@JsFun("() => document.fullscreenElement != null || document.webkitFullscreenElement != null")
private external fun isFullscreenActiveJs(): Boolean

@Composable
actual fun SystemFullscreenHandler(
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit
) {
    DisposableEffect(Unit) {
        val listener: (org.w3c.dom.events.Event) -> Unit = {
            onFullscreenChange(isFullscreenActiveJs())
        }
        document.addEventListener("fullscreenchange", listener)
        document.addEventListener("webkitfullscreenchange", listener)

        onDispose {
            document.removeEventListener("fullscreenchange", listener)
            document.removeEventListener("webkitfullscreenchange", listener)
            if (isFullscreenActiveJs()) {
                toggleFullscreenJs()
            }
        }
    }
}

@JsFun("() => document.visibilityState === 'visible'")
private external fun isDocumentVisibleWasm(): Boolean



