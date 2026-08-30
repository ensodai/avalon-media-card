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
import org.ensodai.avalonmediacard.core.player.CommonPlaybackController
import org.ensodai.avalonmediacard.presentation.screens.player.action.PlayerActions
import org.ensodai.avalonmediacard.presentation.screens.player.component.UnifiedVideoPlayer
import org.ensodai.avalonmediacard.presentation.screens.player.viewState.PlayerViewState
import org.w3c.dom.HTMLVideoElement
import web.navigator.navigator
import kotlin.js.json
import kotlin.time.Duration.Companion.milliseconds

class JsPlatform : Platform {
    private val userAgent = navigator.userAgent
    private val browserList = listOf("Chrome", "Firefox", "Safari", "Edge")

    override val name: String = userAgent.findAnyOf(browserList, ignoreCase = true)
        ?.let { (startIndex) -> userAgent.substring(startIndex).substringBefore(" ") }
        ?: "Unknown"
}

actual fun getPlatform(): Platform = JsPlatform()

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

fun createHlsConfig(): dynamic = json(
    "maxBufferSize" to 120 * 1000 * 1000,
    "backBufferLength" to 90,
    "maxBufferLength" to 120,
    "maxMaxBufferLength" to 600,
    "manifestLoadingTimeOut" to 120000,
    "manifestLoadingMaxRetryTimeout" to 120000,
    "levelLoadingTimeOut" to 120000,
    "fragLoadingTimeOut" to 120000,
    "fragLoadingMaxRetry" to 3,
    "manifestLoadingMaxRetry" to 3,
    "maxBufferHole" to 0.5,
    "nudgeMaxRetries" to 5,
    "highBufferWatchdogPeriod" to 2,
    "lowBufferWatchdogPeriod" to 0.5
)

@JsName("Hls")
external class Hls(config: dynamic = definedExternally) {
    fun loadSource(src: String)
    fun attachMedia(media: HTMLVideoElement)
    fun on(event: String, callback: (dynamic) -> Unit)
    fun destroy()
    fun startLoad(startPosition: Double = definedExternally)
    fun stopLoad()
    fun recoverMediaError()

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
    element: org.w3c.dom.HTMLElement,
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
                        "position: absolute; left: $cssLeft; top: $cssTop; width: $cssWidth; height: $cssHeight; z-index: 0; border-radius: 12px;"
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


actual fun openInExternalPlayer(streamUrl: String, title: String) {
    js(
        """
    var ua = navigator.userAgent.toLowerCase();
    var encodedUrl = encodeURIComponent(streamUrl);
    
    if (ua.indexOf("android") !== -1) {
        var intentUrl = streamUrl.replace(/^https?:\/\//, "");
        var scheme = streamUrl.indexOf("https") === 0 ? "https" : "http";
        var vlcIntent = "intent://" + intentUrl + "#Intent;scheme=" + scheme + ";package=org.videolan.vlc;S.title=" + encodeURIComponent(title) + ";end;";
        window.location.href = vlcIntent;
    } else if (ua.indexOf("iphone") !== -1 || ua.indexOf("ipad") !== -1 || ua.indexOf("ipod") !== -1) {
        window.location.href = "vlc-x-callback://x-callback-url/stream?url=" + encodedUrl;
    } else if (ua.indexOf("macintosh") !== -1 || ua.indexOf("mac os x") !== -1) {
        window.location.href = "iina://weblink?url=" + encodedUrl;
    } else {
        var m3uContent = "#EXTM3U\n#EXTINF:-1," + title + "\n" + streamUrl;
        var blob = new Blob([m3uContent], { type: "text/plain" });
        var blobUrl = window.URL.createObjectURL(blob);
        var a = document.createElement("a");
        a.href = blobUrl;
        a.download = title.replace(/[^a-zA-Z0-9А-Яа-я]/g, "_") + ".m3u";
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(blobUrl);
    }
    """
    )
}

private fun fetchContentType(url: String, callback: (String?) -> Unit): Unit = js(
    """
    fetch(url, { method: 'HEAD' })
        .then(response => {
            const ct = response.headers.get('content-type');
            callback(ct ? ct : null);
        })
        .catch(err => {
            console.error("Fetch Content-Type failed:", err);
            callback(null);
        });
"""
)

fun calculateBufferAheadJs(video: HTMLVideoElement): Double {
    val timeRanges = video.buffered
    val currentTime = video.currentTime
    var bufferAhead = 0.0
    val tolerance = 0.5
    for (i in 0 until timeRanges.length) {
        val start = timeRanges.start(i)
        val end = timeRanges.end(i)
        if (currentTime >= (start - tolerance) && currentTime <= end) {
            bufferAhead = end - currentTime
            break
        }
    }
    return maxOf(0.0, bufferAhead)
}

fun safePlayJs(obj: dynamic) {
    try {
        val p = obj.play()
        if (p != null && p.catch != undefined) {
            p.catch { e: dynamic -> console.warn("safePlayJs ignored error:", e) }
        }
    } catch (e: dynamic) {
        console.warn("safePlayJs synchronous error:", e)
    }
}

fun isTimeInBufferedRangesJs(video: HTMLVideoElement, targetTime: Double): Boolean {
    val timeRanges = video.buffered
    val tolerance = 0.5
    for (i in 0 until timeRanges.length) {
        val start = timeRanges.start(i)
        val end = timeRanges.end(i)
        if (targetTime >= (start - tolerance) && targetTime <= end) {
            return true
        }
    }
    return false
}

class VideoElementPlaybackController(
    val videoElement: HTMLVideoElement
) : CommonPlaybackController() {

    private var intentToPlay = true
    var isSeeking = false
        private set

    override fun setVolume(volume: Double) {
        val clamped = volume.coerceIn(0.0, 1.0)
        videoElement.volume = clamped
        state.volume = clamped
    }

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
                    val isReady = isTimeInBufferedRangesJs(
                        videoElement,
                        videoElement.currentTime
                    ) || (videoElement.asDynamic().readyState as? Int ?: 0) >= 2
                    if (isReady) {
                        state.isBuffering = false
                        if (intentToPlay) {
                            safePlayJs(videoElement)
                            state.isPlaying = true
                        } else {
                            videoElement.pause()
                            state.isPlaying = false
                        }
                    }
                }

                "canplay", "canplaythrough" -> {
                    if (!isSeeking) {
                        val isReady = isTimeInBufferedRangesJs(
                            videoElement,
                            videoElement.currentTime
                        ) || (videoElement.asDynamic().readyState as? Int ?: 0) >= 2
                        if (isReady) {
                            state.isBuffering = false
                            if (intentToPlay) {
                                safePlayJs(videoElement)
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
        val currentBufferAhead = calculateBufferAheadJs(videoElement)
        state.bufferAheadSeconds = currentBufferAhead

        if (intentToPlay) {
            if (state.isBuffering) {
                if (currentBufferAhead >= REBUFFERING_GOAL || currentBufferAhead >= MIN_BUFFER_TO_PLAY) {
                    safePlayJs(videoElement)
                    state.isBuffering = false
                }
            } else {
                if (currentBufferAhead < CRITICAL_BUFFER_LEVEL) {
                    videoElement.pause()
                    state.isBuffering = true
                    state.isPlaying = false
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

    var contentTypeState by remember(url) { mutableStateOf<String?>(null) }
    var isCheckingFormat by remember(url) { mutableStateOf(url != null) }

    DisposableEffect(url) {
        var active = true
        if (url != null) {
            isCheckingFormat = true
            fetchContentType(url) { ct ->
                if (active) {
                    contentTypeState = ct
                    isCheckingFormat = false
                }
            }
        } else {
            isCheckingFormat = false
        }
        onDispose {
            active = false
        }
    }

    val videoElement = remember {
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
                setAttribute("style", "position: absolute;")
                document.body?.appendChild(this)
            }
    }

    val controller = remember(videoElement) { VideoElementPlaybackController(videoElement) }

    var lastKnownTime by remember { mutableStateOf(0.0) }
    var currentUrl by remember { mutableStateOf<String?>(null) }

    if (currentUrl != url) {
        lastKnownTime =
            if (startPositionSeconds != null && startPositionSeconds > 0) startPositionSeconds.toDouble() else 0.0
        currentUrl = url
    }

    LaunchedEffect(audioTracks, subtitleTracks) {
        controller.setTracks(audioTracks, subtitleTracks)
    }

    DisposableEffect(url, isCheckingFormat) {
        if (isCheckingFormat) {
            controller.state.isBuffering = true
            return@DisposableEffect onDispose {}
        }
        controller.state.isBuffering = true
        container.appendChild(videoElement)

        var hlsInstance: Hls? = null
        var playsVideoEngine: PlaysVideoEngine? = null
        var mpegtsPlayer: MpegtsPlayer? = null

        val waitingListener: (org.w3c.dom.events.Event) -> Unit = {
            controller.state.isBuffering = true
        }
        val playingListener: (org.w3c.dom.events.Event) -> Unit = {
            controller.state.isBuffering = false
        }
        val visibilityListener: (org.w3c.dom.events.Event) -> Unit = {
            if (kotlinx.browser.document.asDynamic().visibilityState == "visible") {
                if (!videoElement.paused && videoElement.readyState < 3 && hlsInstance != null) {
                    hlsInstance?.startLoad()
                    safePlayJs(videoElement)
                }
            }
        }

        videoElement.addEventListener("waiting", waitingListener)
        videoElement.addEventListener("stalled", waitingListener)
        videoElement.addEventListener("playing", playingListener)
        videoElement.addEventListener("canplay", playingListener)
        kotlinx.browser.document.addEventListener("visibilitychange", visibilityListener)

        if (url != null) {
            if (url.contains(".avi", ignoreCase = true)) {
                controller.state.playbackError = "Этот видеоформат (например, старый AVI или Xvid) не поддерживается в браузере. Пожалуйста, откройте его во внешнем плеере."
                controller.state.isBuffering = false
            } else if (url.contains(".m3u8", ignoreCase = true) && Hls.isSupported()) {
                val hls = Hls(createHlsConfig())
                hlsInstance = hls
                var pendingRestoreTime: Double? = null
                var networkErrorRetries = 0
                hls.attachMedia(videoElement)
                hls.on("hlsMediaAttached") {
                    hls.loadSource(url)
                }
                hls.on("hlsManifestParsed") {
                    if (pendingRestoreTime != null && pendingRestoreTime!! > 0) {
                        videoElement.currentTime = pendingRestoreTime!!
                        pendingRestoreTime = null
                    }
                    safePlayJs(videoElement)
                }
                hls.on("hlsError") { data ->
                    val fatal = data?.fatal?.unsafeCast<Boolean>() ?: false
                    val errorType = data?.type?.toString() ?: ""
                    if (fatal) {
                        when (errorType) {
                            "networkError" -> {
                                networkErrorRetries++
                                if (networkErrorRetries > 3) {
                                    controller.state.playbackError = "Failed to load video stream from server (502 Bad Gateway). Format or torrent stream is not supported in web version."
                                    controller.state.isBuffering = false
                                    hls.stopLoad()
                                } else {
                                    val currentTime = videoElement.currentTime
                                    pendingRestoreTime = if (currentTime > 0) currentTime else lastKnownTime
                                    hls.loadSource(url)
                                    hls.startLoad()
                                }
                            }
                            "mediaError" -> hls.recoverMediaError()
                            else -> {
                                controller.state.playbackError = "Failed to load video stream. Please choose another source or open in external player."
                                controller.state.isBuffering = false
                                hls.stopLoad()
                            }
                        }
                    }
                }
            } else if ((url.contains(".ts", ignoreCase = true) || url.contains(
                    "format=ts",
                    ignoreCase = true
                )) && mpegts.isSupported()
            ) {
                val dataSource = createMpegtsDataSource("mse", false, url)
                val player = mpegts.createPlayer(dataSource, createMpegtsConfig())
                mpegtsPlayer = player
                player.attachMediaElement(videoElement)
                player.load()
                safePlayJs(player)
            } else {
                val engine = PlaysVideoEngine(videoElement)
                playsVideoEngine = engine
                engine.addEventListener("error") {
                    controller.state.playbackError =
                        "This video format is not supported in the browser. Please open in external player or Android app."
                }
                engine.loadUrl(url)
            }
        }

        onDispose {
            kotlinx.browser.document.removeEventListener("visibilitychange", visibilityListener)
            videoElement.removeEventListener("waiting", waitingListener)
            videoElement.removeEventListener("stalled", waitingListener)
            videoElement.removeEventListener("playing", playingListener)
            videoElement.removeEventListener("canplay", playingListener)
            mpegtsPlayer?.destroy()
            hlsInstance?.destroy()
            playsVideoEngine?.destroy()
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

    LaunchedEffect(controller) {
        while (true) {
            val state = controller.state
            if (!controller.isSeeking) {
                state.currentTime = videoElement.currentTime
                lastKnownTime = videoElement.currentTime
            }
            if (videoElement.duration > 0 && !videoElement.duration.isNaN()) {
                state.duration = videoElement.duration
            }
            state.isMuted = videoElement.muted
            state.volume = videoElement.volume
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

actual fun togglePlatformFullscreen() {
    val doc: dynamic = document
    val root: dynamic = doc.documentElement
    val isActive = doc.fullscreenElement != null || doc.webkitFullscreenElement != null
    if (isActive) {
        if (doc.exitFullscreen != null) doc.exitFullscreen()
        else if (doc.webkitExitFullscreen != null) doc.webkitExitFullscreen()
    } else {
        if (root.requestFullscreen != null) root.requestFullscreen()
        else if (root.webkitRequestFullscreen != null) root.webkitRequestFullscreen()
    }
}

@Composable
actual fun SystemFullscreenHandler(
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit
) {
    DisposableEffect(Unit) {
        val doc: dynamic = document
        val listener: (org.w3c.dom.events.Event) -> Unit = {
            val isActive = doc.fullscreenElement != null || doc.webkitFullscreenElement != null
            onFullscreenChange(isActive)
        }
        document.addEventListener("fullscreenchange", listener)
        document.addEventListener("webkitfullscreenchange", listener)

        onDispose {
            document.removeEventListener("fullscreenchange", listener)
            document.removeEventListener("webkitfullscreenchange", listener)
            val isActive = doc.fullscreenElement != null || doc.webkitFullscreenElement != null
            if (isActive) {
                if (doc.exitFullscreen != null) doc.exitFullscreen()
                else if (doc.webkitExitFullscreen != null) doc.webkitExitFullscreen()
            }
        }
    }
}
