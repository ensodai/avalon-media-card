package org.ensodai.avalonmediacard.core.player

import com.sun.jna.Memory
import com.sun.jna.Platform
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.PointerByReference
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import java.io.File
import java.net.JarURLConnection
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class DesktopMpvPlaybackController : CommonPlaybackController() {

    private val mpv: MpvNative = MpvNative.INSTANCE
    private var mpvHandle: Pointer? = null
    private var renderContext: Pointer? = null

    private val renderLock = ReentrantLock()
    var bufferPool: TripleBufferPool? = null
        private set

    private val sizeMem = Memory(8)
    private val formatMem = Memory(8).apply { setString(0, "bgr0") }
    private val strideMem = Memory(8)
    private var renderParams: Array<MpvNative.MpvRenderParam>? = null

    private val _frameCount = MutableStateFlow(0L)
    val frameCount: StateFlow<Long> = _frameCount.asStateFlow()

    private var videoWidth = 1920
    private var videoHeight = 1080

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var eventJob: Job? = null
    private var renderJob: Job? = null
    private var isDisposed = false

    private val renderSignal = Channel<Unit>(Channel.CONFLATED)
    private val renderCallback = object : MpvNative.MpvRenderUpdateCallback {
        override fun invoke(cb_ctx: Pointer?) {
            renderSignal.trySend(Unit)
        }
    }

    init {
        initializeMpv()
    }

    private fun initializeMpv() {
        MpvNative.Companion.LibC.initLocale()
        val handle = mpv.mpv_create() ?: throw IllegalStateException("mpv_create returned null")
        mpvHandle = handle

        // --- Кэш и демультиплексор в RAM (512MB Forward, 256MB Back-Buffer) ---
        mpv.mpv_set_option_string(handle, "cache", "yes")
        mpv.mpv_set_option_string(handle, "cache-secs", "300.0")
        mpv.mpv_set_option_string(handle, "demuxer-max-bytes", "536870912") // 512 MiB
        mpv.mpv_set_option_string(handle, "demuxer-max-back-bytes", "268435456") // 256 MiB
        mpv.mpv_set_option_string(handle, "demuxer-seekable-cache", "yes")
        mpv.mpv_set_option_string(handle, "demuxer-readahead-secs", "30.0")
        mpv.mpv_set_option_string(handle, "stream-buffer-size", "8388608") // 8 MiB

        // --- Защита от сетевого отката субтитров ---
        mpv.mpv_set_option_string(handle, "demuxer-mkv-subtitle-preroll", "no")
        mpv.mpv_set_option_string(handle, "demuxer-mkv-subtitle-preroll-secs", "0.0")

        // --- Сетевой стек FFmpeg и протокол HTTP ---
        mpv.mpv_set_option_string(handle, "demuxer-lavf-buffersize", "4194304") // 4 MiB
        mpv.mpv_set_option_string(handle, "demuxer-lavf-probesize", "32768000") // 32 MiB
        mpv.mpv_set_option_string(handle, "demuxer-lavf-analyzeduration", "5.0")
        mpv.mpv_set_option_string(handle, "demuxer-lavf-linearize-timestamps", "no")
        mpv.mpv_set_option_string(handle, "demuxer-mkv-probe-start-time", "no")
        mpv.mpv_set_option_string(
            handle,
            "demuxer-lavf-o",
            "reconnect=1,reconnect_streamed=1,reconnect_delay_max=5,reconnect_on_network_error=1,reconnect_on_http_error=4xx,5xx,seekable=1,tcp_nodelay=1"
        )

        // --- Устойчивость декодеров к повреждениям сетевого потока ---
        mpv.mpv_set_option_string(handle, "vd-lavc-o", "err_detect=ignore_err")
        mpv.mpv_set_option_string(handle, "ad-lavc-o", "err_detect=ignore_err")

        // --- Перемотка в торрент-стримах (быстрый seek без сброса сети) ---
        mpv.mpv_set_option_string(handle, "hr-seek", "no")
        mpv.mpv_set_option_string(handle, "hr-seek-framedrop", "yes")

        // --- Декодирование (Copy-Back для SW Render с CPU фоллбэком) ---
        mpv.mpv_set_option_string(handle, "hwdec", "auto-copy-safe")
        mpv.mpv_set_option_string(handle, "hwdec-software-fallback", "1")
        mpv.mpv_set_option_string(handle, "hwdec-extra-frames", "8")
        mpv.mpv_set_option_string(handle, "vd-lavc-threads", "0")
        mpv.mpv_set_option_string(handle, "vd-lavc-dr", "yes")

        // --- Оптимизация программного скейлера (Критично для 4K в SW Render) ---
        mpv.mpv_set_option_string(handle, "sws-scaler", "fast-bilinear")
        mpv.mpv_set_option_string(handle, "sws-fast", "yes")
        mpv.mpv_set_option_string(handle, "sws-allow-zimg", "yes")

        // --- Вывод и синхронизация (Критично для Compose Canvas) ---
        mpv.mpv_set_option_string(handle, "vo", "libmpv")
        mpv.mpv_set_option_string(handle, "video-sync", "audio")
        mpv.mpv_set_option_string(handle, "framedrop", "vo")
        mpv.mpv_set_option_string(handle, "autosync", "30")

        // --- Аудио-тракт и защита от артефактов/щелчков ---
        mpv.mpv_set_option_string(handle, "audio-client-name", "AvalonMediaCard")
        mpv.mpv_set_option_string(handle, "ao", "pulse,pipewire,alsa,wasapi,auto")
        mpv.mpv_set_option_string(handle, "audio-stream-silence", "yes")
        mpv.mpv_set_option_string(handle, "audio-buffer", "0.5")
        mpv.mpv_set_option_string(handle, "audio-resample-async", "1")
        mpv.mpv_set_option_string(handle, "audio-pitch-correction", "yes")

        // --- Рендеринг субтитров (Оптимизация для CPU) ---
        mpv.mpv_set_option_string(handle, "sub-font-provider", "auto")
        mpv.mpv_set_option_string(handle, "embeddedfonts", "yes")
        mpv.mpv_set_option_string(handle, "sub-ass-shaper", "simple")
        mpv.mpv_set_option_string(handle, "sub-ass-scale-with-window", "yes")

        // --- Общее поведение ---
        mpv.mpv_set_option_string(handle, "user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        mpv.mpv_set_option_string(handle, "network-timeout", "30")
        mpv.mpv_set_option_string(handle, "keep-open", "yes")
        mpv.mpv_set_option_string(handle, "idle", "yes")
        mpv.mpv_set_option_string(handle, "ytdl", "no")
        mpv.mpv_set_option_string(handle, "terminal", "no")

        // --- Пользовательская конфигурация и скрипты (Resource Bundling) ---
        val mpvConfigDir = extractMpvResourcesIfNeeded()
        if (mpvConfigDir.exists()) {
            mpv.mpv_set_option_string(handle, "config-dir", mpvConfigDir.absolutePath)
            val inputConfFile = File(mpvConfigDir, "input.conf")
            if (inputConfFile.exists()) {
                println("[MPV] Using input-conf: ${inputConfFile.absolutePath}")
                mpv.mpv_set_option_string(handle, "input-conf", inputConfFile.absolutePath)
            }
            mpv.mpv_set_option_string(handle, "load-scripts", "yes")
        }

        val initRes = mpv.mpv_initialize(handle)
        if (initRes < 0) {
            mpv.mpv_destroy(handle)
            mpvHandle = null
            throw IllegalStateException("mpv_initialize failed with error code: $initRes")
        }

        val scriptsDir = File(mpvConfigDir, "scripts")
        if (scriptsDir.exists()) {
            scriptsDir.listFiles { _, name -> name.endsWith(".lua") || name.endsWith(".js") }?.forEach { scriptFile ->
                println("[MPV] Loading user script: ${scriptFile.absolutePath}")
                mpv.mpv_command(handle, arrayOf("load-script", scriptFile.absolutePath))
            }
        }

        mpv.mpv_request_log_messages(handle, "v")
        setupSoftwareRenderContext(handle)
        startObservingProperties(handle)
        startRenderLoop()
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Structure> T.toArrayTyped(size: Int): Array<T> =
        toArray(size) as Array<T>

    private fun setupSoftwareRenderContext(handle: Pointer) {
        val apiTypeMem = Memory(8).apply { setString(0, MpvNative.MPV_RENDER_API_TYPE_SW) }

        val params = MpvNative.MpvRenderParam().toArrayTyped(2).apply {
            this[0].type = MpvNative.MPV_RENDER_PARAM_API_TYPE
            this[0].data = apiTypeMem
            this[1].type = MpvNative.MPV_RENDER_PARAM_INVALID
            this[1].data = null
        }
        params[0].write()
        params[1].write()

        val renderCtxRef = PointerByReference()
        val createRes = mpv.mpv_render_context_create(renderCtxRef, handle, params[0].pointer)
        if (createRes < 0) {
            println("[MPV] mpv_render_context_create failed: $createRes")
            return
        }

        val ctx = renderCtxRef.value
        renderContext = ctx

        val pool = TripleBufferPool(videoWidth, videoHeight)
        bufferPool = pool
        sizeMem.setInt(0, pool.width)
        sizeMem.setInt(4, pool.height)
        strideMem.setLong(0, pool.stride.toLong())

        val rParams = MpvNative.MpvRenderParam().toArrayTyped(5).apply {
            this[0].type = MpvNative.MPV_RENDER_PARAM_SW_SIZE
            this[0].data = sizeMem
            this[1].type = MpvNative.MPV_RENDER_PARAM_SW_FORMAT
            this[1].data = formatMem
            this[2].type = MpvNative.MPV_RENDER_PARAM_SW_STRIDE
            this[2].data = strideMem
            this[3].type = MpvNative.MPV_RENDER_PARAM_SW_POINTER
            this[3].data = null
            this[4].type = MpvNative.MPV_RENDER_PARAM_INVALID
            this[4].data = null
        }
        for (p in rParams) p.write()
        renderParams = rParams

        mpv.mpv_render_context_set_update_callback(ctx, renderCallback, null)
    }

    private fun startRenderLoop() {
        renderJob = scope.launch {
            try {
                for (signal in renderSignal) {
                    if (!isActive || isDisposed) break
                    val ctx = renderContext ?: continue
                    val flags = mpv.mpv_render_context_update(ctx)
                    if (flags != 0L) {
                        renderFrame()
                        mpv.mpv_render_context_report_swap(ctx)
                    }
                }
            } catch (_: Throwable) {
                // Ignore cancellation and channel close during disposal
            }
        }
    }

    private fun startObservingProperties(handle: Pointer) {
        mpv.mpv_observe_property(handle, 1, "time-pos", MpvNative.MPV_FORMAT_DOUBLE)
        mpv.mpv_observe_property(handle, 2, "duration", MpvNative.MPV_FORMAT_DOUBLE)
        mpv.mpv_observe_property(handle, 3, "pause", MpvNative.MPV_FORMAT_FLAG)
        mpv.mpv_observe_property(handle, 4, "paused-for-cache", MpvNative.MPV_FORMAT_FLAG)
        mpv.mpv_observe_property(handle, 5, "dwidth", MpvNative.MPV_FORMAT_INT64)
        mpv.mpv_observe_property(handle, 6, "dheight", MpvNative.MPV_FORMAT_INT64)
        mpv.mpv_observe_property(handle, 7, "track-list/count", MpvNative.MPV_FORMAT_INT64)
        mpv.mpv_observe_property(handle, 8, "eof-reached", MpvNative.MPV_FORMAT_FLAG)
        mpv.mpv_observe_property(handle, 9, "demuxer-cache-duration", MpvNative.MPV_FORMAT_DOUBLE)
        mpv.mpv_observe_property(handle, 10, "container-fps", MpvNative.MPV_FORMAT_DOUBLE)
        mpv.mpv_observe_property(handle, 11, "estimated-vf-fps", MpvNative.MPV_FORMAT_DOUBLE)
        mpv.mpv_observe_property(handle, 12, "vf", MpvNative.MPV_FORMAT_STRING)
        mpv.mpv_observe_property(handle, 13, "glsl-shaders", MpvNative.MPV_FORMAT_STRING)
        mpv.mpv_observe_property(handle, 14, "volume", MpvNative.MPV_FORMAT_DOUBLE)
        mpv.mpv_observe_property(handle, 15, "mute", MpvNative.MPV_FORMAT_FLAG)

        eventJob = scope.launch {
            while (isActive && !isDisposed) {
                val event = mpv.mpv_wait_event(handle, 0.05)
                if (event != null && event.event_id != MpvNative.MPV_EVENT_NONE) {
                    handleMpvEvent(event)
                }
            }
        }
    }

    private fun handleMpvEvent(event: MpvNative.MpvEvent) {
        when (event.event_id) {
            MpvNative.MPV_EVENT_PROPERTY_CHANGE -> {
                val dataPtr = event.data ?: return
                val prop = MpvNative.MpvEventProperty(dataPtr)

                when (prop.name) {
                    "time-pos" -> {
                        if (prop.format == MpvNative.MPV_FORMAT_DOUBLE && prop.data != null) {
                            val timeSeconds = prop.data!!.getDouble(0)
                            updateTime(timeSeconds)
                        }
                    }
                    "duration" -> {
                        if (prop.format == MpvNative.MPV_FORMAT_DOUBLE && prop.data != null) {
                            val durationSeconds = prop.data!!.getDouble(0)
                            state.duration = durationSeconds
                        }
                    }
                    "pause" -> {
                        if (prop.format == MpvNative.MPV_FORMAT_FLAG && prop.data != null) {
                            val isPaused = prop.data!!.getInt(0) != 0
                            state.isPlaying = !isPaused
                        }
                    }
                    "paused-for-cache" -> {
                        if (prop.format == MpvNative.MPV_FORMAT_FLAG && prop.data != null) {
                            val isBuffering = prop.data!!.getInt(0) != 0
                            state.isBuffering = isBuffering
                        }
                    }
                    "demuxer-cache-duration" -> {
                        if (prop.format == MpvNative.MPV_FORMAT_DOUBLE && prop.data != null) {
                            val cacheSeconds = prop.data!!.getDouble(0)
                            state.bufferAheadSeconds = cacheSeconds.coerceAtLeast(0.0)
                        }
                    }
                    "container-fps" -> {
                        if (prop.format == MpvNative.MPV_FORMAT_DOUBLE && prop.data != null) {
                            val fpsVal = prop.data!!.getDouble(0)
                            if (state.fps == null || state.fps == 0.0) {
                                state.fps = fpsVal
                            }
                        }
                    }
                    "estimated-vf-fps" -> {
                        if (prop.format == MpvNative.MPV_FORMAT_DOUBLE && prop.data != null) {
                            val fpsVal = prop.data!!.getDouble(0)
                            if (fpsVal > 0.0) {
                                state.fps = fpsVal
                            }
                        }
                    }
                    "vf" -> {
                        val vfStr = getPropertyString("vf") ?: ""
                        println("[MPV Filter Change] Active Video Filters (vf): '$vfStr'")
                    }
                    "glsl-shaders" -> {
                        val shadersStr = getPropertyString("glsl-shaders") ?: ""
                        println("[MPV Shaders Change] Active GLSL Shaders: '$shadersStr'")
                    }
                    "dwidth" -> {
                        if (prop.format == MpvNative.MPV_FORMAT_INT64 && prop.data != null) {
                            val w = prop.data!!.getLong(0).toInt()
                            if (w > 0 && w != videoWidth) {
                                videoWidth = w
                                updateBufferPoolDimensions()
                            }
                        }
                    }
                    "dheight" -> {
                        if (prop.format == MpvNative.MPV_FORMAT_INT64 && prop.data != null) {
                            val h = prop.data!!.getLong(0).toInt()
                            if (h > 0 && h != videoHeight) {
                                videoHeight = h
                                updateBufferPoolDimensions()
                            }
                        }
                    }
                    "track-list/count" -> {
                        extractTracks()
                    }
                    "eof-reached" -> {
                        if (prop.format == MpvNative.MPV_FORMAT_FLAG && prop.data != null) {
                            val eof = prop.data!!.getInt(0) != 0
                            if (eof) {
                                state.isPlaying = false
                            }
                        }
                    }
                    "volume" -> {
                        if (prop.format == MpvNative.MPV_FORMAT_DOUBLE && prop.data != null) {
                            val vol = prop.data!!.getDouble(0) / 100.0
                            state.volume = vol.coerceIn(0.0, 1.0)
                        }
                    }
                    "mute" -> {
                        if (prop.format == MpvNative.MPV_FORMAT_FLAG && prop.data != null) {
                            val isMuted = prop.data!!.getInt(0) != 0
                            state.isMuted = isMuted
                        }
                    }
                }
            }
            MpvNative.MPV_EVENT_LOG_MESSAGE -> {
                val dataPtr = event.data ?: return
                val logMsg = MpvNative.MpvEventLogMessage(dataPtr)
                val prefix = logMsg.prefix ?: "mpv"
                val level = logMsg.level ?: "info"
                val text = (logMsg.text ?: "").trim()
                if (text.isNotBlank() && !prefix.startsWith("cache") && !text.contains("Linearizing discontinuity")) {
                    println("[MPV Native][$prefix][$level] $text")
                }
            }
            MpvNative.MPV_EVENT_FILE_LOADED -> {
                println("[MPV Event] FILE_LOADED")
                state.isBuffering = false
                state.isPlaying = true
                extractTracks()
                renderSignal.trySend(Unit)
            }
            MpvNative.MPV_EVENT_PLAYBACK_RESTART -> {
                println("[MPV Event] PLAYBACK_RESTART")
                state.isBuffering = false
                state.isPlaying = true
                renderSignal.trySend(Unit)
            }
        }
    }

    private fun updateBufferPoolDimensions() {
        if (videoWidth <= 0 || videoHeight <= 0) return
        renderLock.withLock {
            val currentPool = bufferPool
            if (currentPool != null && currentPool.width == videoWidth && currentPool.height == videoHeight) {
                return
            }
            println("[MPV] Updating buffer pool dimensions to ${videoWidth}x${videoHeight}")
            val newPool = TripleBufferPool(videoWidth, videoHeight)
            bufferPool = newPool
            sizeMem.setInt(0, newPool.width)
            sizeMem.setInt(4, newPool.height)
            strideMem.setLong(0, newPool.stride.toLong())
            renderParams?.let { params ->
                params[0].write()
                params[2].write()
            }
            currentPool?.release()
        }
    }

    private fun renderFrame() {
        val ctx = renderContext ?: return
        renderLock.withLock {
            val pool = bufferPool ?: return
            val params = renderParams ?: return
            val backPtr = pool.getBackNativePointer()

            params[3].data = backPtr
            params[3].write()

            val res = mpv.mpv_render_context_render(ctx, params[0].pointer)
            val current = _frameCount.value
            if (res < 0) {
                val errStr = mpv.mpv_error_string(res) ?: res.toString()
                println("[MPV Render ERROR] mpv_render_context_render code=$res ($errStr)")
            } else {
                pool.swapAfterWrite()
                _frameCount.value = current + 1
            }
        }
    }

    private data class RawAudioTrack(
        val id: String,
        val title: String?,
        val lang: String?,
        val codec: String?,
        val channels: Int?,
        val bitrate: Long?,
        val hlsBitrate: Long?,
        val isSelected: Boolean,
        val isDefault: Boolean
    )

    private fun extractTracks() {
        val handle = mpvHandle ?: return
        val countStr = getPropertyString("track-list/count")?.toIntOrNull() ?: 0
        val rawAudioList = mutableListOf<RawAudioTrack>()
        val subList = mutableListOf<SubtitleTrack>()
        var hasVideoRenditions = false

        for (i in 0 until countStr) {
            val type = getPropertyString("track-list/$i/type") ?: continue
            val id = getPropertyString("track-list/$i/id") ?: (i + 1).toString()
            val title = getPropertyString("track-list/$i/title")
            val lang = getPropertyString("track-list/$i/lang")
            val selected = getPropertyString("track-list/$i/selected") == "yes"
            val isDefault = getPropertyString("track-list/$i/default") == "yes"

            if (type == "video") {
                hasVideoRenditions = true
            } else if (type == "audio") {
                val codec = getPropertyString("track-list/$i/codec")
                val channels = getPropertyString("track-list/$i/audio-channels")?.toIntOrNull()
                val bitrate = getPropertyString("track-list/$i/demux-bitrate")?.toLongOrNull()
                val hlsBitrate = getPropertyString("track-list/$i/hls-bitrate")?.toLongOrNull()
                rawAudioList.add(
                    RawAudioTrack(
                        id = id,
                        title = title,
                        lang = lang,
                        codec = codec,
                        channels = channels,
                        bitrate = bitrate,
                        hlsBitrate = hlsBitrate,
                        isSelected = selected,
                        isDefault = isDefault
                    )
                )
            } else if (type == "sub") {
                val displayName = title?.takeIf { it.isNotBlank() }
                    ?: lang?.takeIf { it.isNotBlank() }
                    ?: "Subtitles ${subList.size + 1}"
                subList.add(
                    SubtitleTrack(
                        id = id,
                        name = displayName,
                        language = lang,
                        isExternal = false,
                        url = null
                    )
                )
            }
        }

        // Detect if audio tracks are duplicate HLS variant bitrates
        val isHlsDuplicateRenditions = rawAudioList.size > 1 &&
                rawAudioList.all { it.title.isNullOrBlank() } &&
                rawAudioList.map { it.lang }.distinct().size == 1 &&
                rawAudioList.map { it.codec }.distinct().size == 1 &&
                rawAudioList.map { it.channels }.distinct().size == 1

        val finalAudioTracks = if (isHlsDuplicateRenditions) {
            // Keep only active / highest bitrate track for HLS variant audio
            val bestTrack = rawAudioList.find { it.isSelected }
                ?: rawAudioList.maxByOrNull { it.bitrate ?: it.hlsBitrate ?: 0L }
                ?: rawAudioList.first()
            val displayName = bestTrack.lang?.takeIf { it.isNotBlank() } ?: "Default Audio"
            listOf(
                AudioTrack(
                    id = bestTrack.id,
                    name = displayName,
                    language = bestTrack.lang,
                    channels = bestTrack.channels,
                    isDefault = true
                )
            )
        } else {
            // Real multi-track stream (MKV, torrents, multi-lang HLS): disambiguate duplicate language labels
            val langCounts = rawAudioList.groupingBy { it.title?.ifBlank { null } ?: it.lang ?: "Audio" }.eachCount()
            rawAudioList.mapIndexed { index, track ->
                val baseName = track.title?.takeIf { it.isNotBlank() }
                    ?: track.lang?.takeIf { it.isNotBlank() }
                    ?: "Audio ${index + 1}"

                val displayName = if ((langCounts[baseName] ?: 0) > 1) {
                    val codecLabel = track.codec?.uppercase()?.takeIf { it.isNotBlank() }
                    val chLabel = when (track.channels) {
                        6 -> "5.1"
                        8 -> "7.1"
                        2 -> "2.0"
                        1 -> "1.0"
                        else -> null
                    }
                    val techSpecs = listOfNotNull(codecLabel, chLabel).joinToString(" ")
                    if (techSpecs.isNotBlank()) "$baseName ($techSpecs)" else "$baseName #${index + 1}"
                } else {
                    baseName
                }

                AudioTrack(
                    id = track.id,
                    name = displayName,
                    language = track.lang,
                    channels = track.channels,
                    isDefault = track.isSelected || track.isDefault
                )
            }
        }

        setTracks(finalAudioTracks, subList)
    }

    private fun getPropertyString(name: String): String? {
        val handle = mpvHandle ?: return null
        val ptr = mpv.mpv_get_property_string(handle, name) ?: return null
        return try {
            ptr.getString(0)
        } finally {
            mpv.mpv_free(ptr)
        }
    }

    fun loadMedia(url: String, startPositionSeconds: Double = 0.0) {
        val handle = mpvHandle ?: return
        state.currentTime = startPositionSeconds
        state.duration = 0.0
        state.isBuffering = true
        state.bufferAheadSeconds = 0.0
        state.playbackError = null
        _audioTracks = emptyList()
        _subtitleTracks = emptyList()
        _selectedAudioTrack = null
        _selectedSubtitleTrack = null

        println("[MPV] loadMedia: url=$url, startPositionSeconds=$startPositionSeconds")

        // Для AVI без индекса в хвосте включаем ignidx+genpts, для HTTP/HTTPS включаем discardcorrupt+genpts, для остальных держим чистым
        if (url.contains(".avi", ignoreCase = true) || url.contains("format=avi", ignoreCase = true)) {
            mpv.mpv_set_option_string(handle, "demuxer-lavf-o", "fflags=+ignidx+genpts")
        } else if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
            mpv.mpv_set_option_string(handle, "demuxer-lavf-o", "fflags=+discardcorrupt+genpts")
        } else {
            mpv.mpv_set_option_string(handle, "demuxer-lavf-o", "")
        }

        if (startPositionSeconds > 0.0) {
            val startSecondsInt = startPositionSeconds.toLong()
            mpv.mpv_command(handle, arrayOf("loadfile", url, "replace", "-1", "start=$startSecondsInt"))
        } else {
            mpv.mpv_command(handle, arrayOf("loadfile", url, "replace"))
        }
        mpv.mpv_set_property_string(handle, "pause", "no")
        state.isPlaying = true
    }

    override fun play() {
        val handle = mpvHandle ?: return
        mpv.mpv_set_property_string(handle, "pause", "no")
        state.isPlaying = true
    }

    override fun pause() {
        val handle = mpvHandle ?: return
        mpv.mpv_set_property_string(handle, "pause", "yes")
        state.isPlaying = false
    }

    override fun stop() {
        val handle = mpvHandle ?: return
        mpv.mpv_command(handle, arrayOf("stop"))
        state.isPlaying = false
        state.isBuffering = true
        state.currentTime = 0.0
    }


    override fun togglePlayPause() {
        if (state.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    override fun seek(time: Double) {
        updateTime(time)
        val handle = mpvHandle ?: return
        mpv.mpv_command(handle, arrayOf("seek", time.toString(), "absolute+exact"))
    }

    override fun stepForward() {
        val handle = mpvHandle ?: return
        pause()
        mpv.mpv_command(handle, arrayOf("frame-step"))
    }

    override fun stepBackward() {
        val handle = mpvHandle ?: return
        pause()
        mpv.mpv_command(handle, arrayOf("frame-back-step"))
    }

    override fun setVolume(volume: Double) {
        val clamped = volume.coerceIn(0.0, 1.0)
        state.volume = clamped
        val handle = mpvHandle ?: return
        val volInt = (clamped * 100).toInt().toString()
        mpv.mpv_set_property_string(handle, "volume", volInt)
    }

    override fun setMuted(muted: Boolean) {
        state.isMuted = muted
        val handle = mpvHandle ?: return
        mpv.mpv_set_property_string(handle, "mute", if (muted) "yes" else "no")
    }

    override fun selectAudioTrack(track: AudioTrack) {
        super.selectAudioTrack(track)
        val handle = mpvHandle ?: return
        val trackId = track.id.toIntOrNull() ?: return
        mpv.mpv_set_property_string(handle, "aid", trackId.toString())
    }

    override fun selectSubtitleTrack(track: SubtitleTrack?) {
        super.selectSubtitleTrack(track)
        val handle = mpvHandle ?: return
        if (track == null) {
            mpv.mpv_set_property_string(handle, "sid", "no")
        } else {
            val trackId = track.id.toIntOrNull() ?: return
            mpv.mpv_set_property_string(handle, "sid", trackId.toString())
        }
    }

    override fun sendKeyPress(key: String) {
        val handle = mpvHandle ?: return
        println("[MPV] Sending keypress: $key")
        mpv.mpv_command(handle, arrayOf("keypress", key))
    }

    private fun extractMpvResourcesIfNeeded(): File {
        val userHome = System.getProperty("user.home")
        val baseDir = if (Platform.isWindows()) {
            val appData = System.getenv("APPDATA") ?: "$userHome/AppData/Roaming"
            File(appData, "AvalonMediaCard/mpv")
        } else {
            File(userHome, ".config/avalon-media-card/mpv")
        }

        try {
            if (!baseDir.exists()) {
                baseDir.mkdirs()
            }
            copyResourceTree("mpv", baseDir)

            val inputConfFile = File(baseDir, "input.conf")
            val mvtoolsFile = File(baseDir, "vapoursynth/mvtools.vpy")
            if (inputConfFile.exists() && mvtoolsFile.exists()) {
                val absoluteMvtools = mvtoolsFile.absolutePath.replace('\\', '/')
                val text = inputConfFile.readText()
                if (text.contains("~~/vapoursynth/mvtools.vpy") || text.contains("~~config/vapoursynth/mvtools.vpy")) {
                    val patched = text
                        .replace("~~/vapoursynth/mvtools.vpy", absoluteMvtools)
                        .replace("~~config/vapoursynth/mvtools.vpy", absoluteMvtools)
                    inputConfFile.writeText(patched)
                    println("[MPV] Updated input.conf with absolute VapourSynth path: $absoluteMvtools")
                }
            }
        } catch (e: Throwable) {
            println("[MPV] Error extracting mpv resources: ${e.message}")
        }
        return baseDir
    }

    private fun copyResourceTree(resourcePath: String, targetDir: File) {
        val classLoader = DesktopMpvPlaybackController::class.java.classLoader
        val url = classLoader.getResource(resourcePath) ?: return

        if (url.protocol == "file") {
            try {
                val srcDir = File(url.toURI())
                if (srcDir.exists()) {
                    srcDir.copyRecursively(targetDir, overwrite = false)
                }
            } catch (e: Throwable) {
                println("[MPV] File copy failed: ${e.message}")
            }
        } else if (url.protocol == "jar") {
            try {
                val jarConnection = url.openConnection() as? JarURLConnection ?: return
                val jarFile = jarConnection.jarFile
                val entries = jarFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.startsWith(resourcePath) && !entry.isDirectory) {
                        val relativePath = entry.name.removePrefix(resourcePath).removePrefix("/")
                        val destFile = File(targetDir, relativePath)
                        if (!destFile.exists()) {
                            destFile.parentFile?.mkdirs()
                            jarFile.getInputStream(entry).use { input ->
                                destFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                println("[MPV] Jar copy failed: ${e.message}")
            }
        }
    }

    fun release() {
        if (isDisposed) return
        isDisposed = true
        renderSignal.close()
        eventJob?.cancel()
        renderJob?.cancel()
        scope.cancel()

        val ctx = renderContext
        if (ctx != null) {
            mpv.mpv_render_context_set_update_callback(ctx, null, null)
            mpv.mpv_render_context_free(ctx)
            renderContext = null
        }

        renderLock.withLock {
            bufferPool?.release()
            bufferPool = null
            renderParams = null
        }

        val handle = mpvHandle
        if (handle != null) {
            mpv.mpv_terminate_destroy(handle)
            mpvHandle = null
        }
    }
}
