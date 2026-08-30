package org.ensodai.avalonmediacard.presentation.screens.player.component.pc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun UnifiedVideoPlayerLayout(
    isFullscreen: Boolean,
    showUiOverlay: Boolean,
    showRightPanel: Boolean,
    hasEpisodesContext: Boolean,
    videoSurface: @Composable () -> Unit,
    centerOverlays: @Composable () -> Unit,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    rightPanelOverlay: @Composable BoxScope.() -> Unit,

    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Видео и оверлеи поверх него
        Box(modifier = Modifier.fillMaxSize()) {
            videoSurface()

            centerOverlays()

            // Верхний бар (Player Header)
            AnimatedVisibility(
                visible = showUiOverlay,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                topBar()
            }
        }

        // Правая панель в виде оверлея
        if (hasEpisodesContext) {
            rightPanelOverlay()
        }

        // Нижняя панель управления вынесена наверх
        AnimatedVisibility(
            visible = showUiOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
        ) {
            bottomBar()
        }
    }
}
