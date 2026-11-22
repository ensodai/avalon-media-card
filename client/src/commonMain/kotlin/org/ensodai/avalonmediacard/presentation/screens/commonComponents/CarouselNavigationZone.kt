package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon as Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

@Composable
fun BoxScope.CarouselNavigationZone(
    isRight: Boolean,
    isVisible: Boolean = true,
    onClick: () -> Unit
) {
    if (LocalDeviceTarget.current.isTv) return

    val canvasColor = AvalonTheme.colors.bgCanvas

    // Воздушный мягкий градиент в цвет холста (без тёмных серых коробок)
    val gradientColors = if (isRight) {
        listOf(Color.Transparent, canvasColor.copy(alpha = 0.50f), canvasColor.copy(alpha = 0.85f))
    } else {
        listOf(canvasColor.copy(alpha = 0.85f), canvasColor.copy(alpha = 0.50f), Color.Transparent)
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(150)),
        modifier = Modifier.matchParentSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .align(if (isRight) Alignment.CenterEnd else Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(72.dp)
                    .background(Brush.horizontalGradient(colors = gradientColors))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                    .pointerHoverIcon(PointerIcon.Hand),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRight) Lucide.ChevronRight else Lucide.ChevronLeft,
                    contentDescription = if (isRight) "Forward" else "Back",
                    tint = AvalonTheme.colors.textPrimary,
                    modifier = Modifier
                        .size(44.dp)
                        .padding(
                            start = if (isRight) 14.dp else 0.dp,
                            end = if (isRight) 0.dp else 14.dp
                        )
                )
            }
        }
    }
}
