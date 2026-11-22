package org.ensodai.avalonmediacard.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.designsystem.interaction.AvalonInteractiveRole
import org.ensodai.avalonmediacard.designsystem.interaction.AvalonSurfaceColors
import org.ensodai.avalonmediacard.designsystem.interaction.avalonInteractive
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

/**
 * 1. Catalog Poster Card (2:3 ratio).
 */
@Composable
fun AvalonPosterCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badgeOverlay: (@Composable BoxScope.() -> Unit)? = null,
    artwork: @Composable BoxScope.() -> Unit
) {
    val colors = AvalonTheme.colors
    val shape = AvalonTheme.shapes.media // 12.dp

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .avalonInteractive(
                    role = AvalonInteractiveRole.MediaPoster,
                    shape = shape,
                    surfaceColors = AvalonSurfaceColors(default = colors.bgSurface),
                    onClick = onClick
                )
        ) {
            artwork()
            badgeOverlay?.invoke(this)
        }
        Spacer(Modifier.height(AvalonTheme.spacing.space8))
        BasicText(
            text = title,
            style = AvalonTheme.typography.cardTitle.copy(color = colors.textPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            BasicText(
                text = subtitle,
                style = AvalonTheme.typography.metadata.copy(color = colors.textSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 2. Episode Card (16:9 ratio) with progress bar and monospace timecode.
 */
@Composable
fun AvalonEpisodeCard(
    title: String,
    episodeCode: String,
    durationText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    subtitle: String? = null,
    artwork: @Composable BoxScope.() -> Unit
) {
    val colors = AvalonTheme.colors
    val shape = AvalonTheme.shapes.media // 12.dp
    val badgeShape = AvalonTheme.shapes.micro // 4.dp

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .avalonInteractive(
                    role = AvalonInteractiveRole.MediaWide,
                    shape = shape,
                    surfaceColors = AvalonSurfaceColors(default = colors.bgSurface),
                    onClick = onClick
                )
        ) {
            artwork()
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = if (progress != null) 12.dp else 8.dp)
                    .clip(badgeShape)
                    .background(colors.bgCanvas.copy(alpha = 0.84f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                BasicText(
                    text = durationText,
                    style = AvalonTheme.typography.monoTech.copy(color = colors.textPrimary),
                    maxLines = 1
                )
            }
            if (progress != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .height(3.dp)
                        .clip(AvalonTheme.shapes.micro)
                        .background(colors.chartTrack.copy(alpha = 0.85f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(3.dp)
                            .background(colors.accentPrimary)
                    )
                }
            }
        }
        Spacer(Modifier.height(AvalonTheme.spacing.space8))
        BasicText(
            text = "$episodeCode · $title",
            style = AvalonTheme.typography.cardTitle.copy(color = colors.textPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            BasicText(
                text = subtitle,
                style = AvalonTheme.typography.metadata.copy(color = colors.textSecondary),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 3. Bento Card container (16dp).
 */
@Composable
fun AvalonBentoCard(
    title: String,
    modifier: Modifier = Modifier,
    trailingHeader: (@Composable () -> Unit)? = null,
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AvalonTheme.colors
    val shape = AvalonTheme.shapes.container // 16.dp

    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.bgSurface)
            .border(AvalonTheme.elevation.borderWidth, colors.borderSubtle, shape)
            .padding(contentPadding)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicText(
                text = title,
                style = AvalonTheme.typography.sectionTitle.copy(color = colors.textPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            trailingHeader?.invoke()
        }
        Spacer(Modifier.height(AvalonTheme.spacing.space16))
        content()
    }
}
