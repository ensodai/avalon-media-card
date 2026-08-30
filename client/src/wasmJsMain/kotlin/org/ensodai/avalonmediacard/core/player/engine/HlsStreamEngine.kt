@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.ensodai.avalonmediacard.core.player.engine

import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.core.Hls
import org.ensodai.avalonmediacard.core.VideoElementPlaybackController
import org.ensodai.avalonmediacard.core.createHlsConfig
import org.ensodai.avalonmediacard.core.safePlayWasm
import org.w3c.dom.HTMLVideoElement
import kotlin.js.JsAny

class HlsStreamEngine(
    private val videoElement: HTMLVideoElement,
    private val controller: VideoElementPlaybackController,
    private val lastKnownTimeProvider: () -> Double
) : WasmStreamEngine {

    private var hlsInstance: Hls? = null
    private var pendingRestoreTime: Double? = null
    private var networkErrorRetries = 0
    private var isInitialLoad = true

    override fun load(url: String, startPosition: Double, audioTrackIndex: Int?) {
        val hls = Hls(createHlsConfig())
        hlsInstance = hls
        pendingRestoreTime = if (startPosition > 0) startPosition else null
        networkErrorRetries = 0
        isInitialLoad = true

        hls.attachMedia(videoElement)

        hls.on("hlsMediaAttached") {
            if (isInitialLoad) {
                isInitialLoad = false
                hls.loadSource(url)
            }
        }

        hls.on("hlsManifestParsed") {
            networkErrorRetries = 0
            if (pendingRestoreTime != null && pendingRestoreTime!! > 0) {
                videoElement.currentTime = pendingRestoreTime!!
                pendingRestoreTime = null
            }
            safePlayWasm(videoElement)
        }

        onHlsEventWasm(hls, "hlsError") { data ->
            val fatal = isHlsErrorFatalWasm(data)
            val errorType = getHlsErrorTypeWasm(data)
            if (fatal) {
                when (errorType) {
                    "networkError" -> {
                        networkErrorRetries++
                        if (networkErrorRetries > 3) {
                            controller.state.playbackError =
                                "Failed to load video stream from server (502 Bad Gateway). Format or torrent stream is not supported in web version."
                            controller.state.isBuffering = false
                            hlsStopLoadWasm(hls)
                        } else {
                            val currentTime = videoElement.currentTime
                            val lastKnown = lastKnownTimeProvider()
                            pendingRestoreTime = if (currentTime > 0) currentTime else lastKnown
                            hlsLoadSourceWasm(hls, url)
                            hlsStartLoadWasm(hls)
                        }
                    }
                    "mediaError" -> hlsRecoverMediaErrorWasm(hls)
                    else -> {
                        controller.state.playbackError =
                            "Failed to load video stream. Please choose another source or open in external player."
                        controller.state.isBuffering = false
                        hlsStopLoadWasm(hls)
                    }
                }
            }
        }

        onHlsEventWasm(hls, "hlsAudioTracksUpdated") { data ->
            val jsTracks = getHlsAudioTracksWasm(data)
            val count = getArrayLengthWasm(jsTracks)
            val ktTracks = mutableListOf<AudioTrack>()
            for (i in 0 until count) {
                val trackObj = getArrayElementWasm(jsTracks, i)
                val id = getHlsTrackIdWasm(trackObj)
                val language = getHlsTrackLanguageWasm(trackObj)
                val trackName = getHlsTrackNameWasm(trackObj) ?: "Track ${i + 1}"
                val channels = getHlsTrackChannelsWasm(trackObj)
                val isDefault = getHlsTrackDefaultWasm(trackObj)
                ktTracks.add(
                    AudioTrack(
                        id = id.toString(),
                        name = trackName,
                        language = language,
                        channels = if (channels > 0) channels else null,
                        isDefault = isDefault || (audioTrackIndex != null && id == audioTrackIndex)
                    )
                )
            }
            if (ktTracks.isNotEmpty()) {
                controller.setTracks(ktTracks, controller.subtitleTracks)
            }
        }

        onHlsEventWasm(hls, "hlsSubtitleTracksUpdated") { data ->
            val jsTracks = getHlsSubtitleTracksWasm(data)
            val count = getArrayLengthWasm(jsTracks)
            val ktTracks = mutableListOf<SubtitleTrack>()
            for (i in 0 until count) {
                val trackObj = getArrayElementWasm(jsTracks, i)
                val id = getHlsTrackIdWasm(trackObj)
                val language = getHlsTrackLanguageWasm(trackObj)
                val trackName = getHlsTrackNameWasm(trackObj) ?: "Subtitle ${i + 1}"
                ktTracks.add(
                    SubtitleTrack(
                        id = id.toString(),
                        name = trackName,
                        language = language,
                        isExternal = false,
                        url = null
                    )
                )
            }
            if (ktTracks.isNotEmpty()) {
                controller.setTracks(controller.audioTracks, ktTracks)
            }
        }
    }

    override fun selectAudioTrack(trackId: Int) {
        val hls = hlsInstance ?: return
        setHlsAudioTrackWasm(hls, trackId)
    }

    override fun selectSubtitleTrack(trackId: Int) {
        val hls = hlsInstance ?: return
        setHlsSubtitleTrackWasm(hls, trackId)
    }

    override fun onVisibilityChanged(isVisible: Boolean) {
        val hls = hlsInstance ?: return
        if (isVisible && !videoElement.paused && videoElement.readyState < 3) {
            hlsStartLoadWasm(hls)
            safePlayWasm(videoElement)
        }
    }

    override fun destroy() {
        hlsInstance?.destroy()
        hlsInstance = null
    }
}

@JsFun("(hls, event, callback) => hls.on(event, (e, data) => callback(data))")
private external fun onHlsEventWasm(hls: JsAny, event: String, callback: (JsAny) -> Unit)

@JsFun("(data) => data.audioTracks || []")
private external fun getHlsAudioTracksWasm(data: JsAny): JsAny

@JsFun("(data) => data.subtitleTracks || []")
private external fun getHlsSubtitleTracksWasm(data: JsAny): JsAny

@JsFun("(obj) => obj.id !== undefined ? obj.id : -1")
private external fun getHlsTrackIdWasm(obj: JsAny): Int

@JsFun("(obj) => obj.name || null")
private external fun getHlsTrackNameWasm(obj: JsAny): String?

@JsFun("(obj) => obj.language || null")
private external fun getHlsTrackLanguageWasm(obj: JsAny): String?

@JsFun("(obj) => { if (obj.channels) { const c = parseInt(obj.channels); return isNaN(c) ? 0 : c; } return 0; }")
private external fun getHlsTrackChannelsWasm(obj: JsAny): Int

@JsFun("(obj) => !!obj.default")
private external fun getHlsTrackDefaultWasm(obj: JsAny): Boolean

@JsFun("(hls, trackId) => { hls.audioTrack = trackId; }")
private external fun setHlsAudioTrackWasm(hls: JsAny, trackId: Int)

@JsFun("(hls, trackId) => { hls.subtitleTrack = trackId; }")
private external fun setHlsSubtitleTrackWasm(hls: JsAny, trackId: Int)

@JsFun("(hls) => { if (hls && hls.startLoad) hls.startLoad(); }")
private external fun hlsStartLoadWasm(hls: JsAny)

@JsFun("(hls) => { if (hls && hls.recoverMediaError) hls.recoverMediaError(); }")
private external fun hlsRecoverMediaErrorWasm(hls: JsAny)

@JsFun("(hls, url) => { if (hls && hls.loadSource) hls.loadSource(url); }")
private external fun hlsLoadSourceWasm(hls: JsAny, url: String)

@JsFun("(data) => !!(data && data.fatal)")
private external fun isHlsErrorFatalWasm(data: JsAny): Boolean

@JsFun("(data) => (data && data.type) ? String(data.type) : ''")
private external fun getHlsErrorTypeWasm(data: JsAny): String

@JsFun("(hls) => { if (hls && hls.stopLoad) hls.stopLoad(); }")
private external fun hlsStopLoadWasm(hls: JsAny)

@JsFun("(arr) => (arr && arr.length !== undefined) ? arr.length : 0")
private external fun getArrayLengthWasm(arr: JsAny): Int

@JsFun("(arr, index) => arr[index]")
private external fun getArrayElementWasm(arr: JsAny, index: Int): JsAny
