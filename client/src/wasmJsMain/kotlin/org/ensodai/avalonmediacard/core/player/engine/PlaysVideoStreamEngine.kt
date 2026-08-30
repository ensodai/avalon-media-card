@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.ensodai.avalonmediacard.core.player.engine

import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.core.EngineLoadOptions
import org.ensodai.avalonmediacard.core.PlaysVideoEngine
import org.ensodai.avalonmediacard.core.VideoElementPlaybackController
import org.ensodai.avalonmediacard.core.safePlayWasm
import org.w3c.dom.HTMLVideoElement
import kotlin.js.JsAny
import kotlin.js.toJsNumber

class PlaysVideoStreamEngine(
    private val videoElement: HTMLVideoElement,
    private val controller: VideoElementPlaybackController
) : WasmStreamEngine {

    private var playsVideoEngine: PlaysVideoEngine? = null
    private var currentUrl: String? = null
    private var currentAudioTrackIndex: Int? = null
    private var pendingRestoreTime: Double? = null

    override fun load(url: String, startPosition: Double, audioTrackIndex: Int?) {
        currentUrl = url
        currentAudioTrackIndex = audioTrackIndex
        pendingRestoreTime = if (startPosition > 0) startPosition else null

        val engine = PlaysVideoEngine(videoElement)
        playsVideoEngine = engine

        engine.addEventListener("error") {
            controller.state.playbackError =
                "This video format is not supported in the browser. Please open in external player or Android app."
        }

        engine.addEventListener("ready") { event ->
            val jsTracks = extractAudioTracksFromEventWasm(event)
            val count = getArrayLengthWasm(jsTracks)
            val ktTracks = mutableListOf<AudioTrack>()
            for (i in 0 until count) {
                val trackObj = getArrayElementWasm(jsTracks, i)
                val codec = getTrackCodecWasm(trackObj)
                val language = getTrackLanguageWasm(trackObj)
                val trackName = getTrackNameWasm(trackObj) ?: "Track ${i + 1}"
                val finalName = if (codec != null) "$trackName [$codec]" else trackName
                val channels = getTrackChannelsWasm(trackObj)

                ktTracks.add(
                    AudioTrack(
                        id = i.toString(),
                        name = finalName,
                        language = language,
                        channels = channels,
                        isDefault = (currentAudioTrackIndex ?: 0) == i
                    )
                )
            }
            if (ktTracks.isNotEmpty()) {
                controller.setTracks(ktTracks, controller.subtitleTracks)
                val activeTrack = ktTracks.find { it.id == (currentAudioTrackIndex ?: 0).toString() } ?: ktTracks.firstOrNull()
                if (activeTrack != null) {
                    controller.selectAudioTrack(activeTrack)
                }
            }
            val isAudioUnsupported = getAudioUnsupportedWasm(event)
            controller.state.audioUnsupported = isAudioUnsupported

            if (pendingRestoreTime != null && pendingRestoreTime!! > 0) {
                videoElement.currentTime = pendingRestoreTime!!
                pendingRestoreTime = null
            }
            safePlayWasm(videoElement)
        }

        val options = createEmptyLoadOptionsWasm() as EngineLoadOptions
        if (audioTrackIndex != null) {
            options.audioTrackIndex = audioTrackIndex.toJsNumber()
        }
        engine.loadUrl(url, options)
    }

    override fun selectAudioTrack(trackId: Int) {
        val engine = playsVideoEngine ?: return
        val url = currentUrl ?: return
        if (currentAudioTrackIndex == trackId) return

        val currentTime = videoElement.currentTime
        pendingRestoreTime = if (currentTime > 0) currentTime else null
        currentAudioTrackIndex = trackId

        val options = createEmptyLoadOptionsWasm() as EngineLoadOptions
        options.audioTrackIndex = trackId.toJsNumber()
        engine.loadUrl(url, options)

        val track = controller.audioTracks.find { it.id == trackId.toString() }
        if (track != null) {
            controller.selectAudioTrack(track)
        }
    }

    override fun selectSubtitleTrack(trackId: Int) {
        // Subtitle handling
    }

    override fun destroy() {
        playsVideoEngine?.destroy()
        playsVideoEngine = null
    }
}

@JsFun("(arr) => (arr && arr.length !== undefined) ? arr.length : 0")
private external fun getArrayLengthWasm(arr: JsAny): Int

@JsFun("(arr, index) => arr[index]")
private external fun getArrayElementWasm(arr: JsAny, index: Int): JsAny

@JsFun("(event) => { return (event && event.detail && event.detail.audioTracks) ? event.detail.audioTracks : []; }")
private external fun extractAudioTracksFromEventWasm(event: JsAny): JsAny

@JsFun("(obj) => obj.codec || null")
private external fun getTrackCodecWasm(obj: JsAny): String?

@JsFun("(obj) => obj.language || null")
private external fun getTrackLanguageWasm(obj: JsAny): String?

@JsFun("(obj) => obj.name || null")
private external fun getTrackNameWasm(obj: JsAny): String?

@JsFun("(obj) => { if (obj.channels) { const c = parseInt(obj.channels); return isNaN(c) ? null : c; } return null; }")
private external fun getTrackChannelsWasm(obj: JsAny): Int?

@JsFun("(event) => { return event && event.detail && event.detail.audioUnsupported === true; }")
private external fun getAudioUnsupportedWasm(event: JsAny): Boolean

@JsFun("() => { return {}; }")
private external fun createEmptyLoadOptionsWasm(): JsAny
