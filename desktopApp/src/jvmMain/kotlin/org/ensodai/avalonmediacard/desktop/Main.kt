package org.ensodai.avalonmediacard.desktop

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.ensodai.avalonmediacard.desktop.di.DesktopKoinAppConfig
import org.ensodai.avalonmediacard.presentation.App
import org.koin.plugin.module.dsl.startKoin

fun main() = application {
    startKoin<DesktopKoinAppConfig> {}

    val windowState = rememberWindowState(
        width = 1280.dp,
        height = 800.dp
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Avalon Media Card",
        state = windowState,
        icon = painterResource("icons/icon.png")
    ) {
        App()
    }
}
