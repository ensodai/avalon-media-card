@file:JsModule("playsvideo")
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.ensodai.avalonmediacard.core

import org.w3c.dom.HTMLVideoElement

external interface EngineLoadOptions : JsAny {
    var audioTrackIndex: kotlin.js.JsNumber?
}

external class PlaysVideoEngine(videoElement: HTMLVideoElement) : JsAny {
    fun loadUrl(url: String, options: EngineLoadOptions? = definedExternally)
    fun destroy()
    fun addEventListener(type: String, listener: (kotlin.js.JsAny) -> Unit)
    fun removeEventListener(type: String, listener: (kotlin.js.JsAny) -> Unit)
}
