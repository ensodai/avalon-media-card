package org.ensodai.avalonmediacard.core.player

import android.app.ActivityManager
import android.content.Context
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.cert.X509Certificate
import `is`.xyz.mpv.MPV
import `is`.xyz.mpv.MPVNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack

@Serializable
private data class MpvTrackItem(
    val id: Int,
    val type: String,
    val title: String? = null,
    val lang: String? = null,
    val default: Boolean? = null,
    val selected: Boolean? = null,
    val external: Boolean? = null,
    val codec: String? = null,
    @SerialName("demux-bitrate") val demuxBitrate: Long? = null,
    @SerialName("external-filename") val externalFilename: String? = null,
    @SerialName("audio-channels") val audioChannels: Long? = null
)

class MpvPlaybackController(
    val context: Context
) : CommonPlaybackController() {

    val mpv = MPV()

    private val logger = AppLogging.logger("MpvPlaybackController")
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }

    private var currentUrl: String? = null
    private var isInitialized = false
    private var pendingSeekSeconds: Double = 0.0

    private val logObserver = object : MPV.LogObserver {
        override fun logMessage(prefix: String, level: Int, text: String) {
            val trimmed = text.trim()
            if (trimmed.isNotBlank() && !prefix.startsWith("cache") && !trimmed.contains("Linearizing discontinuity")) {
                Log.d("POMODORO_MPV_NATIVE", "[$prefix][$level] $trimmed")
            }
        }
    }

    private val eventObserver = object : MPV.EventObserver {
        override fun eventProperty(property: String) {}

        override fun eventProperty(property: String, value: Long) {
            when (property) {
                "time-pos" -> {
                    updateTime(value.toDouble())
                }
                "duration" -> {
                    if (value > 0) {
                        state.duration = value.toDouble()
                    }
                }
            }
        }

        override fun eventProperty(property: String, value: Boolean) {
            when (property) {
                "pause" -> {
                    state.isPlaying = !value
                }
                "paused-for-cache" -> {
                    state.isBuffering = value
                }
            }
        }

        override fun eventProperty(property: String, value: Double) {
            when (property) {
                "time-pos" -> {
                    updateTime(value)
                }
                "duration" -> {
                    if (value > 0.0) {
                        state.duration = value
                    }
                }
                "demuxer-cache-time" -> {
                    state.bufferAheadSeconds = value.coerceAtLeast(0.0)
                }
            }
        }

        override fun eventProperty(property: String, value: String) {
            when (property) {
                "track-list" -> {
                    parseTrackList(value)
                }
            }
        }

        override fun eventProperty(property: String, value: MPVNode) {}

        override fun event(eventId: Int, data: MPVNode) {
            when (eventId) {
                MPV.mpvEvent.MPV_EVENT_START_FILE -> {
                    state.isBuffering = true
                }
                MPV.mpvEvent.MPV_EVENT_FILE_LOADED -> {
                    state.isBuffering = false
                    state.isPlaying = true

                    if (pendingSeekSeconds > 0.0) {
                        val sec = pendingSeekSeconds.toInt()
                        pendingSeekSeconds = 0.0
                        scope.launch(Dispatchers.IO) {
                            mpv.setPropertyInt("time-pos", sec)
                        }
                    }

                    // Считываем список дорожек при загрузке файла
                    scope.launch(Dispatchers.IO) {
                        val trackListStr = mpv.getPropertyString("track-list")
                        if (!trackListStr.isNullOrBlank()) {
                            scope.launch(Dispatchers.Main) {
                                parseTrackList(trackListStr)
                            }
                        }
                    }
                }
                MPV.mpvEvent.MPV_EVENT_PLAYBACK_RESTART -> {
                    state.isBuffering = false
                    state.isPlaying = true
                }
                MPV.mpvEvent.MPV_EVENT_END_FILE -> {
                    state.isPlaying = false
                }
            }
        }
    }

    companion object {
        @Volatile
        private var isNativeInitialized = false
        private val initLock = Any()
    }

    init {
        initializeMpv()
    }

    private fun readTtfFamilyName(file: File): String? {
        return try {
            val bytes = file.readBytes()
            if (bytes.size < 12) return null
            val numTables = ((bytes[4].toInt() and 0xFF) shl 8) or (bytes[5].toInt() and 0xFF)
            var nameOffset = 0
            for (i in 0 until numTables) {
                val tableHeaderOffset = 12 + i * 16
                if (tableHeaderOffset + 16 > bytes.size) break
                val tag = String(bytes, tableHeaderOffset, 4, Charsets.US_ASCII)
                if (tag == "name") {
                    nameOffset = ((bytes[tableHeaderOffset + 8].toInt() and 0xFF) shl 24) or
                            ((bytes[tableHeaderOffset + 9].toInt() and 0xFF) shl 16) or
                            ((bytes[tableHeaderOffset + 10].toInt() and 0xFF) shl 8) or
                            (bytes[tableHeaderOffset + 11].toInt() and 0xFF)
                    break
                }
            }
            if (nameOffset == 0 || nameOffset + 6 > bytes.size) return null
            val count = ((bytes[nameOffset + 2].toInt() and 0xFF) shl 8) or (bytes[nameOffset + 3].toInt() and 0xFF)
            val strOffset = ((bytes[nameOffset + 4].toInt() and 0xFF) shl 8) or (bytes[nameOffset + 5].toInt() and 0xFF)

            var fallbackName: String? = null
            for (i in 0 until count) {
                val recordOffset = nameOffset + 6 + i * 12
                if (recordOffset + 12 > bytes.size) break
                val platformId = ((bytes[recordOffset].toInt() and 0xFF) shl 8) or (bytes[recordOffset + 1].toInt() and 0xFF)
                val languageId = ((bytes[recordOffset + 4].toInt() and 0xFF) shl 8) or (bytes[recordOffset + 5].toInt() and 0xFF)
                val nameId = ((bytes[recordOffset + 6].toInt() and 0xFF) shl 8) or (bytes[recordOffset + 7].toInt() and 0xFF)
                val length = ((bytes[recordOffset + 8].toInt() and 0xFF) shl 8) or (bytes[recordOffset + 9].toInt() and 0xFF)
                val offset = ((bytes[recordOffset + 10].toInt() and 0xFF) shl 8) or (bytes[recordOffset + 11].toInt() and 0xFF)

                if (nameId == 1) { // 1 = Font Family
                    val strStart = nameOffset + strOffset + offset
                    if (strStart + length <= bytes.size) {
                        val nameStr = if (platformId == 3 || platformId == 0) {
                            String(bytes, strStart, length, Charsets.UTF_16BE)
                        } else {
                            String(bytes, strStart, length, Charsets.UTF_8)
                        }
                        if (nameStr.isNotBlank()) {
                            fallbackName = nameStr
                            if (platformId == 3 && languageId == 0x0409) {
                                return nameStr
                            }
                        }
                    }
                }
            }
            fallbackName
        } catch (e: Exception) {
            Log.w("MpvPlaybackController", "Failed to parse TTF font family name", e)
            null
        }
    }

    private fun exportCombinedCertificates(context: Context, destFile: File) {
        try {
            val pemBuilder = StringBuilder()

            // 1. Базовые сертификаты Mozilla из assets
            try {
                context.assets.open("cacert.pem").bufferedReader().use {
                    pemBuilder.append(it.readText()).append("\n")
                }
            } catch (e: Exception) {
                Log.w("MpvPlaybackController", "cacert.pem not found in assets, proceeding with system CAs only")
            }

            // 2. Системные доверенные сертификаты Android OS (AndroidCAStore - системные и пользовательские CA)
            val keyStore = KeyStore.getInstance("AndroidCAStore")
            keyStore.load(null, null)
            val aliases = keyStore.aliases()
            var count = 0
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement()
                val cert = keyStore.getCertificate(alias) as? X509Certificate ?: continue
                val base64Encoded = Base64.encodeToString(cert.encoded, Base64.DEFAULT)
                pemBuilder.append("-----BEGIN CERTIFICATE-----\n")
                pemBuilder.append(base64Encoded)
                pemBuilder.append("-----END CERTIFICATE-----\n")
                count++
            }

            destFile.writeText(pemBuilder.toString())
            logger.d { "Exported $count Android system CAs combined with Mozilla bundle to ${destFile.path}" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to export combined TLS certificates" }
        }
    }

    private fun setupFontsAndCerts(context: Context): String {
        var detectedFamilyName: String? = null
        try {
            // 1. Копируем встроенные TTF шрифты (Inter из Avalon) в изолированную папку fonts для libass
            val fontsDir = File(context.filesDir, "fonts")
            if (!fontsDir.exists()) fontsDir.mkdirs()

            val fontFiles = listOf("subfont.ttf", "subfont_bold.ttf")
            for (fontName in fontFiles) {
                val destFile = File(fontsDir, fontName)
                if (!destFile.exists() || destFile.length() == 0L) {
                    context.assets.open(fontName).use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    logger.d { "Extracted $fontName to fonts directory" }
                }
                if (fontName == "subfont.ttf" && destFile.exists()) {
                    detectedFamilyName = readTtfFamilyName(destFile)
                    logger.d { "Auto-detected TTF family name: '$detectedFamilyName'" }
                }
            }

            // 2. Экспорт системных сертификатов Android OS + Mozilla CA для полноценной валидации TLS в MPV
            val certFile = File(context.filesDir, "cacert.pem")
            if (!certFile.exists() || certFile.length() == 0L) {
                exportCombinedCertificates(context, certFile)
            }
        } catch (e: Exception) {
            Log.e("MpvPlaybackController", "Failed to setup fonts or certs", e)
        }
        return detectedFamilyName ?: "sans-serif"
    }

    private fun initializeMpv() {
        try {
            val resolvedFontFamily = setupFontsAndCerts(context.applicationContext)

            mpv.create(context.applicationContext)

            // Графика и аппаратное декодирование (с софтверным фоллбэком как на ПК)
            mpv.setOptionString("vo", "gpu")
            mpv.setOptionString("hwdec", "mediacodec,mediacodec-copy,auto-safe")
            mpv.setOptionString("hwdec-codecs", "all")
            mpv.setOptionString("hwdec-software-fallback", "1")
            mpv.setOptionString("hwdec-extra-frames", "8")
            mpv.setOptionString("vd-lavc-threads", "0")
            mpv.setOptionString("vd-lavc-dr", "yes")

            // Оптимизация программного скейлера (при CPU фоллбэке на ARM)
            mpv.setOptionString("sws-scaler", "fast-bilinear")
            mpv.setOptionString("sws-fast", "yes")
            mpv.setOptionString("sws-allow-zimg", "yes")

            // Вывод и синхронизация
            mpv.setOptionString("video-sync", "audio")

            // Настройки буферизации для TorrServer / HTTP стриминга адаптивно под объем памяти устройства
            val actManager = context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memoryClass = actManager?.memoryClass ?: 192
            val demuxerBufferBytes = when {
                memoryClass <= 192 -> 64L * 1024 * 1024   // 64MB для low-end TV (1GB RAM)
                memoryClass <= 384 -> 128L * 1024 * 1024  // 128MB для medium TV / Mobile (2-3GB RAM)
                else -> 256L * 1024 * 1024                // 256MB для high-end устройств (4GB+ RAM)
            }
            val demuxerBackBufferBytes = demuxerBufferBytes / 2

            mpv.setOptionString("cache", "yes")
            mpv.setOptionString("cache-secs", "120.0")
            mpv.setOptionString("demuxer-max-bytes", demuxerBufferBytes.toString())
            mpv.setOptionString("demuxer-max-back-bytes", demuxerBackBufferBytes.toString())
            mpv.setOptionString("demuxer-seekable-cache", "yes")
            mpv.setOptionString("demuxer-readahead-secs", "30.0")
            mpv.setOptionString("stream-buffer-size", "4194304") // 4 MiB

            // Защита от сетевого отката субтитров
            mpv.setOptionString("demuxer-mkv-subtitle-preroll", "no")
            mpv.setOptionString("demuxer-mkv-subtitle-preroll-secs", "0.0")

            // Сетевой стек FFmpeg и протокол HTTP
            mpv.setOptionString("demuxer-lavf-buffersize", "4194304") // 4 MiB
            mpv.setOptionString("demuxer-lavf-probesize", "32768000") // 32 MiB
            mpv.setOptionString("demuxer-lavf-analyzeduration", "5.0")
            mpv.setOptionString("demuxer-lavf-linearize-timestamps", "no")
            mpv.setOptionString("demuxer-mkv-probe-start-time", "no")
            mpv.setOptionString(
                "demuxer-lavf-o",
                "reconnect=1,reconnect_streamed=1,reconnect_delay_max=5,reconnect_on_network_error=1,reconnect_on_http_error=4xx,5xx,seekable=1,tcp_nodelay=1"
            )

            // Устойчивость декодеров к повреждениям сетевого потока
            mpv.setOptionString("vd-lavc-o", "err_detect=ignore_err")
            mpv.setOptionString("ad-lavc-o", "err_detect=ignore_err")

            // Поведение перемотки в торрент-стримах (быстрый seek без сброса сети)
            mpv.setOptionString("hr-seek", "no")
            mpv.setOptionString("hr-seek-framedrop", "yes")

            // Аудио-тракт и защита от щелчков
            mpv.setOptionString("ao", "audiotrack,opensles,auto")
            mpv.setOptionString("audio-stream-silence", "yes")
            mpv.setOptionString("audio-buffer", "0.5")
            mpv.setOptionString("audio-resample-async", "1")
            mpv.setOptionString("audio-pitch-correction", "yes")

            // Настройки шрифтов и субтитров (libass) через локальный каталог шрифтов приложения
            val fontsDirPath = "${context.filesDir.path}/fonts/"
            mpv.setOptionString("sub-fonts-dir", fontsDirPath)
            mpv.setOptionString("sub-font-provider", "none")
            mpv.setOptionString("sub-font", resolvedFontFamily)
            mpv.setOptionString("sub-font-size", "65")
            mpv.setOptionString("sub-color", "#FFFFFFFF")
            mpv.setOptionString("sub-border-color", "#FF000000")
            mpv.setOptionString("sub-border-size", "3")
            mpv.setOptionString("sub-pos", "100")
            mpv.setOptionString("sub-ass-shaper", "simple")
            mpv.setOptionString("sub-ass-override", "force")
            mpv.setOptionString("sub-ass-justify", "yes")
            mpv.setOptionString("sub-scale-with-window", "yes")

            // TLS сертификаты
            val certFile = File(context.filesDir, "cacert.pem")
            if (certFile.exists()) {
                mpv.setOptionString("tls-verify", "yes")
                mpv.setOptionString("tls-ca-file", certFile.path)
            }

            // Логирование нативного ядра MPV в Android Logcat
            mpv.setOptionString("msg-level", "all=v")
            mpv.setOptionString("terminal", "no")

            // Поведение окна и аудио
            mpv.setOptionString("keep-open", "yes")
            mpv.setOptionString("idle", "yes")
            mpv.setOptionString("force-window", "no")
            mpv.setOptionString("audio-client-name", "AvalonMediaCard")
            mpv.setOptionString("vd-lavc-film-grain", "cpu")

            // Сеть, системный User-Agent (точь-в-точь как в Media3 / HttpURLConnection) и отключение ytdl
            val systemUserAgent = System.getProperty("http.agent")
                ?: "Dalvik/2.1.0 (Linux; U; Android ${android.os.Build.VERSION.RELEASE}; ${android.os.Build.MODEL})"
            mpv.setOptionString("user-agent", systemUserAgent)
            mpv.setOptionString("network-timeout", "30")
            mpv.setOptionString("ytdl", "no")

            mpv.init()
            logger.d { "MPV core initialized successfully with subtitles and fontconfig" }

            // Регистрация слушателей для текущего контроллера
            mpv.addLogObserver(logObserver)
            mpv.addObserver(eventObserver)
            observeProperties()

            isInitialized = true
        } catch (e: Exception) {
            Log.e("MpvPlaybackController", "Failed to initialize MPV", e)
            state.playbackError = "MPV initialization error: ${e.message}"
        }
    }

    private fun observeProperties() {
        mpv.observeProperty("time-pos", MPV.mpvFormat.MPV_FORMAT_DOUBLE)
        mpv.observeProperty("duration", MPV.mpvFormat.MPV_FORMAT_DOUBLE)
        mpv.observeProperty("pause", MPV.mpvFormat.MPV_FORMAT_FLAG)
        mpv.observeProperty("paused-for-cache", MPV.mpvFormat.MPV_FORMAT_FLAG)
        mpv.observeProperty("demuxer-cache-time", MPV.mpvFormat.MPV_FORMAT_DOUBLE)
        mpv.observeProperty("track-list", MPV.mpvFormat.MPV_FORMAT_STRING)
    }

    private fun parseTrackList(jsonString: String) {
        try {
            val items = json.decodeFromString<List<MpvTrackItem>>(jsonString)
            val rawAudioList = mutableListOf<MpvTrackItem>()
            val subtitleTracksList = mutableListOf<SubtitleTrack>()

            var selectedSubtitle: SubtitleTrack? = null

            for (item in items) {
                when (item.type) {
                    "audio" -> {
                        rawAudioList.add(item)
                    }
                    "sub" -> {
                        val name = item.title?.takeIf { it.isNotBlank() }
                            ?: item.lang?.takeIf { it.isNotBlank() }
                            ?: "Subtitles ${subtitleTracksList.size + 1}"
                        val track = SubtitleTrack(
                            id = item.id.toString(),
                            name = name,
                            language = item.lang,
                            isExternal = item.external == true,
                            url = item.externalFilename
                        )
                        subtitleTracksList.add(track)
                        if (item.selected == true) {
                            selectedSubtitle = track
                        }
                    }
                }
            }

            val isHlsDuplicateRenditions = rawAudioList.size > 1 &&
                    rawAudioList.all { it.title.isNullOrBlank() } &&
                    rawAudioList.map { it.lang }.distinct().size == 1 &&
                    rawAudioList.map { it.codec }.distinct().size == 1 &&
                    rawAudioList.map { it.audioChannels }.distinct().size == 1

            val finalAudioTracks = if (isHlsDuplicateRenditions) {
                val bestTrack = rawAudioList.find { it.selected == true }
                    ?: rawAudioList.maxByOrNull { it.demuxBitrate ?: 0L }
                    ?: rawAudioList.first()
                val displayName = bestTrack.lang?.takeIf { it.isNotBlank() } ?: "Default Audio"
                listOf(
                    AudioTrack(
                        id = bestTrack.id.toString(),
                        name = displayName,
                        language = bestTrack.lang,
                        channels = bestTrack.audioChannels?.toInt(),
                        isDefault = true
                    )
                )
            } else {
                val langCounts = rawAudioList.groupingBy { it.title?.ifBlank { null } ?: it.lang ?: "Audio" }.eachCount()
                rawAudioList.mapIndexed { index, track ->
                    val baseName = track.title?.takeIf { it.isNotBlank() }
                        ?: track.lang?.takeIf { it.isNotBlank() }
                        ?: "Audio ${index + 1}"

                    val displayName = if ((langCounts[baseName] ?: 0) > 1) {
                        val codecLabel = track.codec?.uppercase()?.takeIf { it.isNotBlank() }
                        val chLabel = when (track.audioChannels?.toInt()) {
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
                        id = track.id.toString(),
                        name = displayName,
                        language = track.lang,
                        channels = track.audioChannels?.toInt(),
                        isDefault = track.selected == true || track.default == true
                    )
                }
            }

            val selectedAudio = finalAudioTracks.find { it.isDefault } ?: finalAudioTracks.firstOrNull()

            setTracks(finalAudioTracks, subtitleTracksList)
            if (selectedAudio != null) {
                _selectedAudioTrack = selectedAudio
            }
            if (selectedSubtitle != null) {
                _selectedSubtitleTrack = selectedSubtitle
            }
        } catch (e: Exception) {
            Log.e("MpvPlaybackController", "Failed to parse track-list JSON", e)
        }
    }

    fun loadAndPlay(url: String, startPositionMs: Long) {
        currentUrl = url
        val startSec = (startPositionMs / 1000.0).coerceAtLeast(0.0)
        pendingSeekSeconds = startSec

        // Мгновенный сброс состояния под новую серию (как на Desktop)
        state.currentTime = startSec
        state.duration = 0.0
        state.isBuffering = true
        state.bufferAheadSeconds = 0.0
        state.playbackError = null
        _audioTracks = emptyList()
        _subtitleTracks = emptyList()
        _selectedAudioTrack = null
        _selectedSubtitleTrack = null

        scope.launch(Dispatchers.IO) {
            try {
                if (url.contains(".avi", ignoreCase = true) || url.contains("format=avi", ignoreCase = true)) {
                    mpv.setOptionString("demuxer-lavf-o", "fflags=+ignidx+genpts")
                } else if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
                    mpv.setOptionString("demuxer-lavf-o", "fflags=+discardcorrupt+genpts")
                } else {
                    mpv.setOptionString("demuxer-lavf-o", "")
                }
                mpv.command("loadfile", url)
                mpv.setPropertyBoolean("pause", false)
                mpv.setPropertyString("vo", "gpu")
            } catch (e: Exception) {
                Log.e("MpvPlaybackController", "Failed to loadfile in MPV", e)
            }
        }
    }

    override fun play() {
        scope.launch(Dispatchers.IO) {
            mpv.setPropertyBoolean("pause", false)
        }
    }

    override fun pause() {
        scope.launch(Dispatchers.IO) {
            mpv.setPropertyBoolean("pause", true)
        }
    }

    override fun togglePlayPause() {
        scope.launch(Dispatchers.IO) {
            mpv.command("cycle", "pause")
        }
    }

    override fun seek(time: Double) {
        updateTime(time)
        scope.launch(Dispatchers.IO) {
            mpv.command("seek", time.toInt().toString(), "absolute", "keyframes")
        }
    }

    override fun setMuted(muted: Boolean) {
        state.isMuted = muted
        scope.launch(Dispatchers.IO) {
            mpv.setPropertyBoolean("mute", muted)
        }
    }

    override fun setVolume(volume: Double) {
        state.volume = volume
        if (!state.isMuted) {
            scope.launch(Dispatchers.IO) {
                mpv.setPropertyInt("volume", (volume * 100).toInt())
            }
        }
    }

    override fun selectAudioTrack(track: AudioTrack) {
        super.selectAudioTrack(track)
        val trackId = track.id.toIntOrNull() ?: return
        scope.launch(Dispatchers.IO) {
            mpv.setPropertyInt("aid", trackId)
        }
    }

    override fun selectSubtitleTrack(track: SubtitleTrack?) {
        super.selectSubtitleTrack(track)
        scope.launch(Dispatchers.IO) {
            if (track == null) {
                mpv.setPropertyString("sid", "no")
            } else {
                val trackId = track.id.toIntOrNull() ?: return@launch
                mpv.setPropertyInt("sid", trackId)
            }
        }
    }

    fun release() {
        scope.cancel()
        try {
            mpv.removeLogObserver(logObserver)
            mpv.removeObserver(eventObserver)
            mpv.command("stop")
            mpv.destroy()
        } catch (e: Exception) {
            Log.e("MpvPlaybackController", "Error during MPV stop/destroy", e)
        }
    }
}
