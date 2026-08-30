@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.ensodai.avalonmediacard.core.player.engine

import org.ensodai.avalonmediacard.core.MpegtsPlayer
import org.ensodai.avalonmediacard.core.VideoElementPlaybackController
import org.ensodai.avalonmediacard.core.createMpegtsConfig
import org.ensodai.avalonmediacard.core.createMpegtsDataSource
import org.ensodai.avalonmediacard.core.mpegts
import org.ensodai.avalonmediacard.core.safePlayWasm
import org.w3c.dom.HTMLVideoElement

class MpegTsStreamEngine(
    private val videoElement: HTMLVideoElement,
    private val controller: VideoElementPlaybackController
) : WasmStreamEngine {

    private var mpegtsPlayer: MpegtsPlayer? = null

    override fun load(url: String, startPosition: Double, audioTrackIndex: Int?) {
        val dataSource = createMpegtsDataSource("mse", false, url)
        val player = mpegts.createPlayer(dataSource, createMpegtsConfig())
        mpegtsPlayer = player
        player.attachMediaElement(videoElement)
        player.load()
        safePlayWasm(player)
    }

    override fun selectAudioTrack(trackId: Int) {
        // MPEG-TS track switching via TS demuxer if supported
    }

    override fun selectSubtitleTrack(trackId: Int) {
        // Subtitle track selection
    }

    override fun destroy() {
        mpegtsPlayer?.destroy()
        mpegtsPlayer = null
    }
}
