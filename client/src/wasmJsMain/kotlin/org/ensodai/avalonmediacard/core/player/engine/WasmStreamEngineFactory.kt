package org.ensodai.avalonmediacard.core.player.engine

import org.ensodai.avalonmediacard.core.Hls
import org.ensodai.avalonmediacard.core.VideoElementPlaybackController
import org.ensodai.avalonmediacard.core.mpegts
import org.w3c.dom.HTMLVideoElement

object WasmStreamEngineFactory {
    fun create(
        url: String,
        videoElement: HTMLVideoElement,
        controller: VideoElementPlaybackController,
        lastKnownTimeProvider: () -> Double
    ): WasmStreamEngine? {
        return when {
            url.contains(".avi", ignoreCase = true) -> null
            url.contains(".m3u8", ignoreCase = true) && Hls.isSupported() -> {
                HlsStreamEngine(videoElement, controller, lastKnownTimeProvider)
            }
            url.contains(".mpd", ignoreCase = true) && DashStreamEngine.isSupported() -> {
                DashStreamEngine(videoElement, controller)
            }
            (url.contains(".ts", ignoreCase = true) || url.contains("format=ts", ignoreCase = true)) && mpegts.isSupported() -> {
                MpegTsStreamEngine(videoElement, controller)
            }
            else -> {
                PlaysVideoStreamEngine(videoElement, controller)
            }
        }
    }
}
