package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.tv.components

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.ensodai.avalonmediacard.designsystem.components.AvalonBadge
import org.ensodai.avalonmediacard.designsystem.components.AvalonBadgeVariant
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.*
import org.ensodai.avalonmediacard.contract.slot.EpisodeItem
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TvEpisodeCard(
    episode: EpisodeItem,
    isCurrentFocused: Boolean,
    onFocus: () -> Unit,
    onPlay: () -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    Column(
        modifier = modifier
            .width(260.dp)
            .padding(vertical = 4.dp)
            .tvAndWebHoverEffect(
                scaleTarget = 1.06f,
                activeBorderWidth = AvalonTheme.elevation.focusBorderWidth,
                activeBorderColor = colors.borderFocus,
                defaultBorderWidth = 1.dp,
                defaultBorderColor = if (isCurrentFocused) colors.borderStrong else colors.borderSubtle,
                shape = shapes.media,
                tiltEnabled = false,
                onClick = onPlay,
                onStateChange = { active ->
                    isFocused = active
                    if (active) onFocus()
                }
            )
            .background(colors.bgSurface, shapes.media)
    ) {
        // 16:9 Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(colors.bgCanvas),
            contentAlignment = Alignment.Center
        ) {
            val imageAlpha = if (episode.isWatched && !isFocused) 0.65f else 1.0f

            if (!episode.stillUrl.isNullOrEmpty() && episode.stillUrl != "placeholder") {
                ShimmerImage(
                    model = episode.stillUrl,
                    contentDescription = episode.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = imageAlpha }
                )
            } else {
                AvalonIcon(
                    imageVector = Lucide.Film,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Scrim Overlay when focused with center Play button
            androidx.compose.animation.AnimatedVisibility(
                visible = isFocused,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(8.dp, CircleShape)
                            .background(colors.accentPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AvalonIcon(
                            imageVector = Lucide.Play,
                            contentDescription = "Смотреть",
                            tint = colors.onAccentPrimary,
                            modifier = Modifier.size(22.dp).offset(x = 1.5.dp)
                        )
                    }
                }
            }

            // Duration badge (bottom-left)
            val runtime = episode.runtime
            if (runtime != null && runtime > 0) {
                AvalonBadge(
                    text = stringResource(Res.string.player_duration_mins_single_fmt, runtime),
                    variant = AvalonBadgeVariant.Glass,
                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                )
            }

            // Watched checkmark badge (top-right)
            if (episode.isWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(colors.statusLive),
                    contentAlignment = Alignment.Center
                ) {
                    AvalonIcon(
                        imageVector = Lucide.Check,
                        contentDescription = "Просмотрено",
                        tint = colors.onAccentPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Title text under thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            AvalonText(
                text = "${episode.episodeNumber}. ${episode.name}",
                style = AvalonTheme.typography.cardTitle,
                color = if (isFocused) colors.textPrimary else colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun TvEpisodeCardSkeleton(
    modifier: Modifier = Modifier
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    Column(
        modifier = modifier
            .width(260.dp)
            .padding(vertical = 4.dp)
            .background(colors.bgSurface, shapes.media)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .shimmerPlaceholder(true, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .fillMaxWidth(0.7f)
                .height(14.dp)
                .shimmerPlaceholder(true, shapes.micro)
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}
