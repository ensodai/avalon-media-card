package org.ensodai.avalonmediacard.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.designsystem.interaction.AvalonInteractiveRole
import org.ensodai.avalonmediacard.designsystem.interaction.AvalonSurfaceColors
import org.ensodai.avalonmediacard.designsystem.interaction.avalonInteractive
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

/**
 * Person card (Actors, Directors):
 * Strictly round 1:1 avatar (48dp), initials monogram fallback on chartTrack,
 * and clear centered labels below.
 */
@Composable
fun AvalonPersonCard(
    name: String,
    role: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 48.dp,
    avatarContent: (@Composable BoxScope.() -> Unit)? = null
) {
    val colors = AvalonTheme.colors
    val cardShape = AvalonTheme.shapes.media
    val initials = remember(name) {
        name.trim()
            .split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "?" }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(avatarSize + 48.dp)
            .avalonInteractive(
                role = AvalonInteractiveRole.Control,
                shape = cardShape,
                surfaceColors = AvalonSurfaceColors(
                    default = Color.Transparent,
                    hover = colors.stateLayerHover,
                    pressed = colors.stateLayerPressed
                ),
                onClick = onClick
            )
            .padding(AvalonTheme.spacing.space8)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(avatarSize)
                .clip(AvalonTheme.shapes.circle)
                .background(colors.chartTrack)
                .border(1.dp, colors.mediaHairline, AvalonTheme.shapes.circle)
        ) {
            if (avatarContent != null) {
                avatarContent()
            } else {
                BasicText(
                    text = initials,
                    style = AvalonTheme.typography.uiLabel.copy(color = colors.textSecondary),
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(AvalonTheme.spacing.space8))

        BasicText(
            text = name,
            style = AvalonTheme.typography.cardTitle.copy(
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (role.isNotBlank()) {
            BasicText(
                text = role,
                style = AvalonTheme.typography.metadata.copy(
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
