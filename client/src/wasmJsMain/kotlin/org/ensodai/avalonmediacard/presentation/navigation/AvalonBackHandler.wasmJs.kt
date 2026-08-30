package org.ensodai.avalonmediacard.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

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
