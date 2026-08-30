@file:JsModule("playsvideo")
@file:JsNonModule

package org.ensodai.avalonmediacard.core

import org.w3c.dom.HTMLVideoElement

external class PlaysVideoEngine(videoElement: HTMLVideoElement) {
    fun loadUrl(url: String)
    fun destroy()
    fun addEventListener(type: String, listener: (dynamic) -> Unit)
    fun removeEventListener(type: String, listener: (dynamic) -> Unit)
}
