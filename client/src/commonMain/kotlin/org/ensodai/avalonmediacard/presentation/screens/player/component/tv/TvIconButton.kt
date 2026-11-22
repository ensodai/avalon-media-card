package org.ensodai.avalonmediacard.presentation.screens.player.component.tv

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect

/**
 * Вспомогательная ТВ-кнопка плеера с круглым ховером под D-Pad.
 */
@Composable
fun TvIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = AvalonTheme.colors.textPrimary,
    onClick: () -> Unit
) {
    val colors = AvalonTheme.colors

    Box(
        modifier = Modifier
            .size(44.dp)
            .tvAndWebHoverEffect(
                scaleTarget = 1.15f,
                shape = CircleShape,
                activeBorderColor = colors.accentPrimary,
                activeBorderWidth = AvalonTheme.elevation.focusBorderWidth,
                defaultBorderColor = colors.borderOverlay,
                defaultBorderWidth = 1.dp,
                onClick = onClick
            )
            .clip(CircleShape)
            .background(colors.bgOverlay)
            .border(1.dp, colors.borderOverlay, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AvalonIcon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}
