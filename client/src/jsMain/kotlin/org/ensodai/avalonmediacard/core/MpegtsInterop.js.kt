package org.ensodai.avalonmediacard.core

import org.w3c.dom.HTMLVideoElement
import kotlin.js.json

@JsModule("mpegts.js")
@JsNonModule
external object mpegts {
    fun getFeatureList(): dynamic
    fun isSupported(): Boolean
    fun createPlayer(mediaDataSource: dynamic, config: dynamic = definedExternally): MpegtsPlayer
}

external interface MpegtsPlayer {
    fun attachMediaElement(mediaElement: HTMLVideoElement)
    fun load()
    fun play()
    fun destroy()
    fun pause()
    fun unload()
    fun detachMediaElement()
}

fun createMpegtsDataSource(type: String, isLive: Boolean, url: String): dynamic =
    kotlin.js.json("type" to type, "isLive" to isLive, "url" to url)

fun createMpegtsConfig(): dynamic = json(
    "enableWorker" to true,
    "seekType" to "range",
    "accurateSeek" to true,
    "lazyLoad" to true,
    "lazyLoadMaxDuration" to 180,
    "lazyLoadRecoverDuration" to 30,
    "autoCleanupSourceBuffer" to true,
    "autoCleanupMaxBackwardDuration" to 180
)
