package org.ensodai.avalonmediacard

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.coroutines.delay
import org.ensodai.avalonmediacard.di.WebKoinAppConfig
import org.ensodai.avalonmediacard.presentation.App
import org.koin.plugin.module.dsl.startKoin
import org.w3c.dom.HTMLElement

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin<WebKoinAppConfig> {}

    val composeRoot = document.getElementById("compose-root") as? HTMLElement
        ?: document.body
        ?: error("Missing #compose-root or body container")

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
        }
        App()
    }
}