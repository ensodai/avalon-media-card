package org.ensodai.avalonmediacard.presentation.navigation

import kotlinx.browser.window
import org.w3c.dom.events.Event

private var popstateHandler: ((Event) -> Unit)? = null

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
fun pushHistorySentinel() {
    js("window.history.pushState({ avalonSentinel: true }, '')")
}

fun setPopstateHandler(callback: () -> Unit) {
    clearPopstateHandler()
    val handler: (Event) -> Unit = { callback() }
    popstateHandler = handler
    window.addEventListener("popstate", handler)
}

fun clearPopstateHandler() {
    popstateHandler?.let {
        window.removeEventListener("popstate", it)
        popstateHandler = null
    }
}
