package org.ensodai.avalonmediacard.core.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.video.VideoRendererEventListener
import androidx.media3.common.util.Log
import androidx.media3.exoplayer.Renderer
import android.os.Handler
import com.homesoft.exo.extractor.AviExtractorsFactory
import org.ensodai.avalonmediacard.contract.logging.AppLogging

@androidx.annotation.OptIn(UnstableApi::class)
object TorExoPlayerFactory {

    private val logger = AppLogging.logger("TorExoPlayerFactory")

    /**
     * Создает оптимизированный ExoPlayer для потокового видео и p2p сетей (TorServer)
     * с учетом протестированных таймингов из веб-клиента.
     */
    fun create(context: Context): ExoPlayer {
        val isFfmpegAvailable = androidx.media3.decoder.ffmpeg.FfmpegLibrary.isAvailable()
        val ffmpegVersion = if (isFfmpegAvailable) androidx.media3.decoder.ffmpeg.FfmpegLibrary.getVersion() else "N/A"
        logger.d { "FFmpeg Native Check: isAvailable=$isFfmpegAvailable, version=$ffmpegVersion" }

        // 1. Настройка буферизации (портировано из HLS/Video Web Player)
        // В вебе: MIN_BUFFER_TO_PLAY = 2.0, REBUFFERING_GOAL = 5.0, maxBufferLength = 120
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                60_000,  // minBufferMs (стараемся держать в памяти минимум 60 сек)
                120_000, // maxBufferMs (максимальный размер буфера, эквивалент 120 сек в вебе)
                2_000,   // bufferForPlaybackMs (быстрый старт, как MIN_BUFFER_TO_PLAY = 2.0)
                5_000    // bufferForPlaybackAfterRebufferMs (как REBUFFERING_GOAL = 5.0)
            )
            .setTargetBufferBytes(120 * 1024 * 1024) // 120 MB как в maxBufferSize (Web)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // 2. Рендереры и аппаратное ускорение
        // Включаем наши FFmpeg декодеры в приоритетном режиме (для AC3/DTS/TrueHD)
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildVideoRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: MediaCodecSelector,
                enableDecoderFallback: Boolean,
                eventHandler: Handler,
                eventListener: VideoRendererEventListener,
                allowedVideoJoiningTimeMs: Long,
                out: ArrayList<Renderer>
            ) {
                val customCodecSelector = object : MediaCodecSelector {
                    override fun getDecoderInfos(
                        mimeType: String,
                        requiresSecureDecoder: Boolean,
                        requiresTunnelingDecoder: Boolean
                    ): MutableList<MediaCodecInfo> {
                        var targetMimeType = mimeType
                        // Dolby Vision Profile 7 downgrade до HDR10
                        if (mimeType == MimeTypes.VIDEO_DOLBY_VISION) {
                            targetMimeType = MimeTypes.VIDEO_H265
                            Log.i("TorExoPlayerFactory", "Downgrading Dolby Vision to HEVC (HDR10)")
                        }
                        
                        val decoders = MediaCodecUtil.getDecoderInfos(
                            targetMimeType,
                            requiresSecureDecoder,
                            requiresTunnelingDecoder
                        )
                        return decoders
                    }
                }
                
                super.buildVideoRenderers(
                    context,
                    extensionRendererMode,
                    customCodecSelector, // Подменяем селектор кодеков
                    enableDecoderFallback,
                    eventHandler,
                    eventListener,
                    allowedVideoJoiningTimeMs,
                    out
                )
            }
            // Удален кастомный buildAudioSink с FadeInAudioProcessor
        }.apply {
            setExtensionRendererMode(2 /* EXTENSION_RENDERER_MODE_PREFERRED */)
            setEnableDecoderFallback(true) // Спасает, если железо не потянуло кодек
        }

        // 3. Сетевой слой с увеличенными таймаутами (TorServer может долго искать пиров)
        // В вебе мы ставили огромные таймауты до 120 сек. Здесь поставим по 30 секунд.
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(30_000)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        
        // Интеграция dburckh/Media3Avi (кастомные экстракторы для лучшей поддержки AVI)
        val extractorsFactory = AviExtractorsFactory()
        
        val loadErrorHandlingPolicy = androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy(5)
        
        val mediaSourceFactory = DefaultMediaSourceFactory(context, extractorsFactory)
            .setDataSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)

        // 4. Сборка плеера
        val player = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()

        // 5. Оптимизация перемотки (снижает нагрузку на p2p сеть при сканировании)
        // Плеер прыгает только по ключевым I-frames
        player.setSeekParameters(SeekParameters.CLOSEST_SYNC)

        // 6. Прямой проброс аудио (Passthrough) на ресивер / саундбар
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE) // Критично для TV, избегает ресэмплинга
            .build()
        player.setAudioAttributes(audioAttributes, true)

        return player
    }
}
