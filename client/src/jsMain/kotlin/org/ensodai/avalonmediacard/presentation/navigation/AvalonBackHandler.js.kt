package org.ensodai.avalonmediacard.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.browser.window
import org.w3c.dom.events.Event

private var popstateHandler: ((Event) -> Unit)? = null

private fun pushHistorySentinel() {
    // В JS мы можем использовать window.history напрямую
    window.history.pushState(js("({ avalonSentinel: true })"), "")
}

private fun setPopstateHandler(callback: () -> Unit) {
    clearPopstateHandler()
    val handler: (Event) -> Unit = {
        callback()
    }
    popstateHandler = handler
    window.addEventListener("popstate", handler)
}

private fun clearPopstateHandler() {
    popstateHandler?.let {
        window.removeEventListener("popstate", it)
        popstateHandler = null
    }
}

@Composable
actual fun AvalonBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val currentOnBack by rememberUpdatedState(onBack)

    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}

        pushHistorySentinel()
        setPopstateHandler {
            currentOnBack()
            pushHistorySentinel()
        }

        onDispose {
            clearPopstateHandler()
        }
    }
}
