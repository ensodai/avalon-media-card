package org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import org.ensodai.avalonmediacard.designsystem.components.AvalonCard as Card
import org.ensodai.avalonmediacard.designsystem.components.CardDefaults
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon as Icon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.NewEpisodeCardItem
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun MovieNotificationCard(
    item: NewEpisodeCardItem,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    val playAction = item.playAction
    val markWatchedAction = item.markWatchedAction
    val openDetailsAction = item.openDetailsAction
    val formattedDate = formatReleaseDate(item.airDate)
    val durationMinutes = item.durationMinutes
    val isTv = LocalDeviceTarget.current.isTv

    var isPosterHovered by remember { mutableStateOf(false) }

    val playOverlayAlpha by animateFloatAsState(
        targetValue = if (isPosterHovered && playAction != null) 1f else 0f,
        label = "MoviePlayOverlayAlpha"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AvalonTheme.colors.surfaceVariant.copy(alpha = 0.2f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = AvalonTheme.colors.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Превью постера 16:9
            val previewWidth = if (isTv) 280.dp else 240.dp
            Box(
                modifier = Modifier
                    .width(previewWidth)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0D0D10))
                    .tvAndWebHoverEffect(
                        scaleTarget = 1.02f,
                        activeBorderWidth = 1.5.dp,
                        activeBorderColor = Color.White.copy(alpha = 0.4f),
                        defaultBorderWidth = 0.dp,
                        shape = RoundedCornerShape(10.dp),
                        tiltEnabled = false,
                        onStateChange = { isPosterHovered = it },
                        onClick = {
                            if (playAction != null) onAction(playAction)
                            else openDetailsAction?.let(onAction)
                        }
                    )
            ) {
                ShimmerImage(
                    model = item.stillUrl ?: item.showPosterUrl,
                    contentDescription = item.showTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Бейдж "ПРЕМЬЕРА"
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(AvalonTheme.colors.primary)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (item.isNew) stringResource(Res.string.episodes_notifications_badge_movie_new) else stringResource(Res.string.episodes_notifications_badge_movie),
                        color = AvalonTheme.colors.onPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Дата выхода
                if (formattedDate != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formattedDate,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Длительность
                if (durationMinutes != null && durationMinutes > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.player_duration_mins_single_fmt, durationMinutes),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Оверлей Play при ховере/фокусе
                if (playOverlayAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = playOverlayAlpha }
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(AvalonTheme.colors.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Play,
                                contentDescription = null,
                                tint = AvalonTheme.colors.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 2. Описание фильма и кнопки действий
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.showTitle,
                            style = AvalonTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AvalonTheme.colors.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable(enabled = openDetailsAction != null) {
                                openDetailsAction?.let(onAction)
                            }
                        )

                        if (formattedDate != null) {
                            Text(
                                text = formattedDate,
                                style = AvalonTheme.typography.labelSmall.copy(
                                    color = AvalonTheme.colors.onSurfaceVariant
                                )
                            )
                        }
                    }

                    val premiereFallback = stringResource(Res.string.episodes_notifications_movie_premiere)
                    val tagText = item.episodeTitle?.ifBlank { null } ?: premiereFallback
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tagText,
                        style = AvalonTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = AvalonTheme.colors.primary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val overview = item.overview
                    if (!overview.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = overview,
                            style = AvalonTheme.typography.bodySmall.copy(
                                color = AvalonTheme.colors.onSurfaceVariant.copy(alpha = 0.85f),
                                lineHeight = 17.sp
                            ),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Кнопки действий
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (playAction != null) {
                        Row(
                            modifier = Modifier
                                .tvAndWebHoverEffect(
                                    scaleTarget = 1.03f,
                                    shape = RoundedCornerShape(8.dp),
                                    tiltEnabled = false,
                                    onClick = { onAction(playAction) }
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .background(AvalonTheme.colors.primary)
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Play,
                                contentDescription = null,
                                tint = AvalonTheme.colors.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(Res.string.episodes_notifications_watch_episode),
                                color = AvalonTheme.colors.onPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (markWatchedAction != null) {
                        Row(
                            modifier = Modifier
                                .tvAndWebHoverEffect(
                                    scaleTarget = 1.03f,
                                    shape = RoundedCornerShape(8.dp),
                                    tiltEnabled = false,
                                    onClick = { onAction(markWatchedAction) }
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .background(AvalonTheme.colors.surfaceVariant.copy(alpha = 0.35f))
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = null,
                                tint = AvalonTheme.colors.onSurface,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(Res.string.episodes_notifications_mark_watched),
                                color = AvalonTheme.colors.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (openDetailsAction != null) {
                        Row(
                            modifier = Modifier
                                .tvAndWebHoverEffect(
                                    scaleTarget = 1.03f,
                                    shape = RoundedCornerShape(8.dp),
                                    tiltEnabled = false,
                                    onClick = { onAction(openDetailsAction) }
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .background(AvalonTheme.colors.surface.copy(alpha = 0.6f))
                                .border(
                                    1.dp,
                                    AvalonTheme.colors.outlineVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.episodes_notifications_about_movie),
                                color = AvalonTheme.colors.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Lucide.ChevronRight,
                                contentDescription = null,
                                tint = AvalonTheme.colors.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
