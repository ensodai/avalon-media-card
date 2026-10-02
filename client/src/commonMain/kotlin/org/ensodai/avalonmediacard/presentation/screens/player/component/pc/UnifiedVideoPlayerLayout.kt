package org.ensodai.avalonmediacard.presentation.screens.player.component.pc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun UnifiedVideoPlayerLayout(
    showUiOverlay: Boolean,
    videoSurface: @Composable () -> Unit,
    centerOverlays: @Composable () -> Unit,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    rightPanelOverlay: @Composable BoxScope.() -> Unit,
    leftPanelOverlay: @Composable BoxScope.() -> Unit = {},
    reactionOverlay: @Composable BoxScope.() -> Unit = {},
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

        // Оверлей всплывающих реакций (слева снизу)
        reactionOverlay()

        // Левая панель в виде оверлея (внутриплеерный чат)
        leftPanelOverlay()

        // Правые панели в виде оверлея (серии и/или участники)
        rightPanelOverlay()

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
