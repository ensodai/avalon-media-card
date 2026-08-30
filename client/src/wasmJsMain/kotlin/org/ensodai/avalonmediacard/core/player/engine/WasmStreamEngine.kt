package org.ensodai.avalonmediacard.core.player.engine

interface WasmStreamEngine {
    fun load(url: String, startPosition: Double, audioTrackIndex: Int?)
    fun selectAudioTrack(trackId: Int)
    fun selectSubtitleTrack(trackId: Int)
    fun onVisibilityChanged(isVisible: Boolean) {}
    fun destroy()
}
