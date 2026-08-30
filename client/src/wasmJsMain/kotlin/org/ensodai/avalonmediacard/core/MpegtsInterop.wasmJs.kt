@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.ensodai.avalonmediacard.core

import org.w3c.dom.HTMLVideoElement

@JsModule("mpegts.js")
external object mpegts : JsAny {
    fun getFeatureList(): JsAny
    fun isSupported(): Boolean
    fun createPlayer(mediaDataSource: JsAny, config: JsAny? = definedExternally): MpegtsPlayer
}

external interface MpegtsPlayer : JsAny {
    fun attachMediaElement(mediaElement: HTMLVideoElement)
    fun load()
    fun play()
    fun destroy()
    fun pause()
    fun unload()
    fun detachMediaElement()
}

@JsFun("(type, isLive, url) => ({ type: type, isLive: isLive, url: url })")
external fun createMpegtsDataSource(type: String, isLive: Boolean, url: String): JsAny

@JsFun("() => ({ enableWorker: true, seekType: 'range', accurateSeek: true, lazyLoad: true, lazyLoadMaxDuration: 180, lazyLoadRecoverDuration: 30, autoCleanupSourceBuffer: true, autoCleanupMaxBackwardDuration: 180 })")
external fun createMpegtsConfig(): JsAny

