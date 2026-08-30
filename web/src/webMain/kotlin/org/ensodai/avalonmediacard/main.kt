package org.ensodai.avalonmediacard

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import org.ensodai.avalonmediacard.di.WebKoinAppConfig
import org.ensodai.avalonmediacard.presentation.App
import org.koin.plugin.module.dsl.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin<WebKoinAppConfig> {}
    ComposeViewport {
        App()
    }
}