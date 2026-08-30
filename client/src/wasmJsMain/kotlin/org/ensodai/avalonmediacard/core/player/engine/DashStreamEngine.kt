@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.ensodai.avalonmediacard.core.player.engine

import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.core.VideoElementPlaybackController
import org.ensodai.avalonmediacard.core.safePlayWasm
import org.w3c.dom.HTMLVideoElement
import kotlin.js.JsAny

class DashStreamEngine(
    private val videoElement: HTMLVideoElement,
    private val controller: VideoElementPlaybackController
) : WasmStreamEngine {

    private var dashPlayer: JsAny? = null

    override fun load(url: String, startPosition: Double, audioTrackIndex: Int?) {
        val player = createDashPlayerWasm(videoElement, url, true)
        dashPlayer = player
        if (player == null) return

        var pendingRestoreTime: Double? = if (startPosition > 0) startPosition else null

        onDashEventWasm(player, "error") { event ->
            val code = getDashErrorCodeWasm(event)
            val msg = getDashErrorMessageWasm(event)
            when (code) {
                10, 11 -> {
                    controller.state.playbackError = "Manifest loading failure: $msg (404 or CORS issue)"
                    controller.state.isBuffering = false
                }
                15, 16 -> {
                    // Non-fatal fragment download retry handled by exponential backoff
                }
                200 -> {
                    controller.state.playbackError = "MediaSource unsupported: audio/video codec is not supported by browser."
                    controller.state.isBuffering = false
                }
                in 100..199 -> {
                    controller.state.playbackError = "DRM / EME Error: Media key access was rejected."
                    controller.state.isBuffering = false
                }
                else -> {
                    if (code > 0) {
                        controller.state.playbackError = "DASH playback error ($code): $msg"
                        controller.state.isBuffering = false
                    }
                }
            }
        }

        onDashEventWasm(player, "streamInitialized") { _ ->
            if (pendingRestoreTime != null && pendingRestoreTime!! > 0) {
                dashSeekToPresentationTimeWasm(player, pendingRestoreTime!!)
                pendingRestoreTime = null
            }
            safePlayWasm(videoElement)

            val jsTracks = getDashAudioTracksWasm(player)
            val count = getArrayLengthWasm(jsTracks)
            val ktTracks = mutableListOf<AudioTrack>()
            for (i in 0 until count) {
                val trackObj = getArrayElementWasm(jsTracks, i)
                val lang = getTrackLanguageWasm(trackObj)
                val label = getTrackNameWasm(trackObj) ?: "Audio ${i + 1}"
                val channels = getTrackChannelsWasm(trackObj)
                ktTracks.add(
                    AudioTrack(
                        id = i.toString(),
                        name = if (lang != null) "$label [$lang]" else label,
                        language = lang,
                        channels = channels,
                        isDefault = (audioTrackIndex ?: 0) == i
                    )
                )
            }
            if (ktTracks.isNotEmpty()) {
                controller.setTracks(ktTracks, controller.subtitleTracks)
            }

            val jsSubs = getDashTextTracksWasm(player)
            val subCount = getArrayLengthWasm(jsSubs)
            val ktSubs = mutableListOf<SubtitleTrack>()
            for (i in 0 until subCount) {
                val subObj = getArrayElementWasm(jsSubs, i)
                val lang = getTrackLanguageWasm(subObj)
                val label = getTrackNameWasm(subObj) ?: "Subtitle ${i + 1}"
                ktSubs.add(
                    SubtitleTrack(
                        id = i.toString(),
                        name = if (lang != null) "$label [$lang]" else label,
                        language = lang,
                        isExternal = false,
                        url = null
                    )
                )
            }
            if (ktSubs.isNotEmpty()) {
                controller.setTracks(controller.audioTracks, ktSubs)
            }
        }
    }

    override fun selectAudioTrack(trackId: Int) {
        val player = dashPlayer ?: return
        setDashAudioTrackByIndexWasm(player, trackId)
    }

    override fun selectSubtitleTrack(trackId: Int) {
        val player = dashPlayer ?: return
        setDashTextTrackWasm(player, trackId)
    }

    override fun destroy() {
        if (dashPlayer != null) {
            destroyDashPlayerWasm(dashPlayer!!)
            dashPlayer = null
        }
    }

    companion object {
        fun isSupported(): Boolean = isDashSupportedWasm()
    }
}

@JsFun("() => typeof dashjs !== 'undefined'")
private external fun isDashSupportedWasm(): Boolean

@JsFun("""(video, url, autoPlay) => { 
    try { 
        if (typeof dashjs === 'undefined') return null; 
        const player = dashjs.MediaPlayer().create(); 
        player.updateSettings({ 
            debug: { logLevel: 0 },
            streaming: { 
                buffer: { 
                    bufferTimeAtTopQuality: 60, 
                    stableBufferTime: 30, 
                    bufferToKeep: 60 
                },
                retryAttempts: { 
                    MPD: 3, 
                    MediaSegment: 3, 
                    InitializationSegment: 3 
                },
                retryIntervals: { 
                    MPD: 1000, 
                    MediaSegment: 1000, 
                    InitializationSegment: 1000 
                },
                cmcd: { 
                    enabled: true, 
                    applyToRequests: ['video', 'audio', 'manifest'] 
                }
            } 
        }); 
        player.initialize(video, url, autoPlay); 
        return player; 
    } catch(e) { 
        console.error('dashjs init error', e); 
        return null; 
    } 
}""")
private external fun createDashPlayerWasm(video: HTMLVideoElement, url: String, autoPlay: Boolean): JsAny?

@JsFun("(player) => { try { if (player && player.destroy) { player.destroy(); } } catch(e) { console.warn('dashjs destroy error', e); } }")
private external fun destroyDashPlayerWasm(player: JsAny)

@JsFun("(player, eventName, callback) => { try { if (player && player.on) { player.on(eventName, (e) => callback(e)); } } catch(e) {} }")
private external fun onDashEventWasm(player: JsAny, eventName: String, callback: (JsAny) -> Unit)

@JsFun("(event) => (event && event.error && event.error.code) ? event.error.code : -1")
private external fun getDashErrorCodeWasm(event: JsAny): Int

@JsFun("(event) => (event && event.error && event.error.message) ? String(event.error.message) : 'Unknown DASH error'")
private external fun getDashErrorMessageWasm(event: JsAny): String

@JsFun("(player, time) => { try { if (player && player.seekToPresentationTime) { player.seekToPresentationTime(time); } } catch(e) {} }")
private external fun dashSeekToPresentationTimeWasm(player: JsAny, time: Double)

@JsFun("(player) => { try { return player ? player.getTracksFor('audio') : []; } catch(e) { return []; } }")
private external fun getDashAudioTracksWasm(player: JsAny): JsAny

@JsFun("(player) => { try { return player ? player.getTracksFor('text') : []; } catch(e) { return []; } }")
private external fun getDashTextTracksWasm(player: JsAny): JsAny

@JsFun("(player, trackIndex) => { try { if (player) { const tracks = player.getTracksFor('audio'); if (tracks && tracks[trackIndex]) { player.setCurrentTrack(tracks[trackIndex]); } } } catch(e) {} }")
private external fun setDashAudioTrackByIndexWasm(player: JsAny, trackIndex: Int)

@JsFun("(player, trackIndex) => { try { if (player) { player.setTextTrack(trackIndex); } } catch(e) {} }")
private external fun setDashTextTrackWasm(player: JsAny, trackIndex: Int)

@JsFun("(arr) => (arr && arr.length !== undefined) ? arr.length : 0")
private external fun getArrayLengthWasm(arr: JsAny): Int

@JsFun("(arr, index) => arr[index]")
private external fun getArrayElementWasm(arr: JsAny, index: Int): JsAny

@JsFun("(obj) => obj.name || obj.label || null")
private external fun getTrackNameWasm(obj: JsAny): String?

@JsFun("(obj) => obj.lang || obj.language || null")
private external fun getTrackLanguageWasm(obj: JsAny): String?

@JsFun("(obj) => { if (obj.channels) { const c = parseInt(obj.channels); return isNaN(c) ? 0 : c; } return 0; }")
private external fun getTrackChannelsWasm(obj: JsAny): Int
