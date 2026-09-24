package org.ensodai.avalonmediacard.presentation.screens.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerInitParams

/**
 * Единая точка запуска полноэкранного плеера через корневой оверлей.
 */
fun launchPlayerOverlay(
    rootOverlay: MutableState<(@Composable () -> Unit)?>,
    params: PlayerInitParams,
    onClose: () -> Unit = {}
) {
    rootOverlay.value = {
        PlayerScreen(
            params = params,
            onClose = {
                rootOverlay.value = null
                onClose()
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
