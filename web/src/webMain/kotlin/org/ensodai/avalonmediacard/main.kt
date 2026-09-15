package org.ensodai.avalonmediacard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.ensodai.avalonmediacard.di.WebKoinAppConfig
import org.ensodai.avalonmediacard.presentation.App
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.DeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.koin.plugin.module.dsl.startKoin
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.FocusEvent
import org.w3c.dom.events.KeyboardEvent

private external interface ShadowRootExt {
    val activeElement: Element?
}

internal fun findComposeCanvas(root: HTMLElement): HTMLElement? {
    val direct = root.querySelector("canvas") as? HTMLElement
    if (direct != null) return direct

    fun search(el: Element): HTMLElement? {
        val shadow = el.shadowRoot
        if (shadow != null) {
            val canvasInShadow = shadow.querySelector("canvas") as? HTMLElement
            if (canvasInShadow != null) return canvasInShadow
            val shadowChildren = shadow.children
            for (i in 0 until shadowChildren.length) {
                val child = shadowChildren.item(i) ?: continue
                val found = search(child)
                if (found != null) return found
            }
        }
        val children = el.children
        for (i in 0 until children.length) {
            val child = children.item(i) ?: continue
            val found = search(child)
            if (found != null) return found
        }
        return null
    }

    return search(root)
}

internal fun getDeepActiveElement(): Element? {
    var el = document.activeElement
    while (el != null) {
        val shadow = el.shadowRoot ?: break
        val inner = (shadow as? ShadowRootExt)?.activeElement ?: break
        el = inner
    }
    return el
}

@OptIn(ExperimentalComposeUiApi::class, kotlin.js.ExperimentalWasmJsInterop::class)
fun main() {
    startKoin<WebKoinAppConfig> {}

    val composeRoot = document.getElementById("compose-root") as? HTMLElement
        ?: document.body
        ?: error("Missing #compose-root or body container")

    val deviceTarget = detectWebDeviceTarget()

    fun focusCanvas(): Boolean {
        val canvas = findComposeCanvas(composeRoot)
        if (canvas != null) {
            canvas.setAttribute("tabindex", "0")
            canvas.focus()
            return true
        }
        return false
    }

    window.addEventListener("click", {
        focusCanvas()
    })

    window.addEventListener("focusout", { event ->
        val fe = event as? FocusEvent
        val nextTarget = fe?.relatedTarget as? Element
        if (nextTarget == null || nextTarget == document.body) {
            window.setTimeout({
                val deepActive = getDeepActiveElement()
                val activeTag = deepActive?.tagName?.lowercase()
                val isInput = activeTag == "input" || activeTag == "textarea"
                if (!isInput) {
                    focusCanvas()
                }
                null
            }, 15)
        }
    })

    window.addEventListener("keydown", { event ->
        val ke = event as? KeyboardEvent ?: return@addEventListener
        val deepActive = getDeepActiveElement()
        val activeTag = deepActive?.tagName?.lowercase()
        val isInputFocused = activeTag == "input" || activeTag == "textarea"

        if (!isInputFocused) {
            val canvas = findComposeCanvas(composeRoot)
            if (canvas != null && deepActive != canvas) {
                canvas.setAttribute("tabindex", "0")
                canvas.focus()
            }
        }

        when (ke.key) {
            "ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight" -> {
                if (!isInputFocused) {
                    ke.preventDefault()
                }
            }
        }
    })

    ComposeViewport(
        viewportContainer = composeRoot
    ) {
        LaunchedEffect(Unit) {
            val loader = document.getElementById("avalon-html-loader") as? HTMLElement
            if (loader != null) {
                loader.style.opacity = "0"
                delay(300) // Ждем завершения transition: opacity 0.3s
                loader.remove()
            }
            focusCanvas()
        }
        CompositionLocalProvider(LocalDeviceTarget provides deviceTarget) {
            App()
        }
    }
}

internal fun detectWebDeviceTarget(): DeviceTarget {
    val ua = window.navigator.userAgent.lowercase()
    val search = window.location.search.lowercase()
    val isTv = search.contains("ui=tv") ||
        ua.contains("tizen") ||
        ua.contains("smart-tv") ||
        ua.contains("smarttv") ||
        ua.contains("webos") ||
        ua.contains("vidaa") ||
        ua.contains("hbbtv") ||
        (ua.contains("samsung") && ua.contains("tv"))
    return if (isTv) DeviceTarget.TV_WEB else DeviceTarget.DESKTOP_WEB
}