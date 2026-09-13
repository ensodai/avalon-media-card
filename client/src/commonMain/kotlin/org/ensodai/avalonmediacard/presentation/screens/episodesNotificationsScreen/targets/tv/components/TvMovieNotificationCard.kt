package org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.targets.tv.components

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.episodes_notifications_about_movie
import avalonmediacard.client.generated.resources.episodes_notifications_badge_movie
import avalonmediacard.client.generated.resources.episodes_notifications_badge_movie_new
import avalonmediacard.client.generated.resources.episodes_notifications_mark_watched
import avalonmediacard.client.generated.resources.episodes_notifications_movie_premiere
import avalonmediacard.client.generated.resources.episodes_notifications_watch_episode
import avalonmediacard.client.generated.resources.player_duration_mins_single_fmt
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Film
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.NewEpisodeCardItem
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component.formatReleaseDate
import org.jetbrains.compose.resources.stringResource

/**
 * ТВ-карточка премьеры фильма: широкий 16:9 баннер с D-Pad фокусом и кнопками действий.
 * Фон и стиль согласованы с остальными карточками системы (surfaceVariant).
 */
@Composable
fun TvMovieNotificationCard(
    item: NewEpisodeCardItem,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    val playAction = item.playAction
    val markWatchedAction = item.markWatchedAction
    val openDetailsAction = item.openDetailsAction
    val formattedDate = formatReleaseDate(item.airDate)
    val durationMinutes = item.durationMinutes

    var isPosterFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
            .focusGroup(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Превью постера 16:9 с оверлеем фокуса
        Box(
            modifier = Modifier
                .width(270.dp)
                .aspectRatio(16f / 9f)
                .tvAndWebHoverEffect(
                    scaleTarget = 1.04f,
                    activeBorderWidth = 2.dp,
                    activeBorderColor = Color.White,
                    defaultBorderWidth = 1.dp,
                    defaultBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp),
                    tiltEnabled = false,
                    onStateChange = { isPosterFocused = it },
                    onClick = {
                        if (playAction != null) onAction(playAction)
                        else openDetailsAction?.let(onAction)
                    }
                )
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0D0D10))
        ) {
            val still = item.stillUrl ?: item.showPosterUrl
            if (!still.isNullOrEmpty() && still != "placeholder") {
                ShimmerImage(
                    model = still,
                    contentDescription = item.showTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Lucide.Film,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(36.dp).align(Alignment.Center)
                )
            }

            // Бейдж ПРЕМЬЕРА (top-start)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (item.isNew) stringResource(Res.string.episodes_notifications_badge_movie_new)
                    else stringResource(Res.string.episodes_notifications_badge_movie),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Длительность (bottom-start)
            if (durationMinutes != null && durationMinutes > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.player_duration_mins_single_fmt, durationMinutes),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Просмотрено (top-end)
            if (item.isWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.95f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Оверлей Play при фокусе
            androidx.compose.animation.AnimatedVisibility(
                visible = isPosterFocused,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(8.dp, CircleShape)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Play,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp).offset(x = 1.5.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(18.dp))

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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(enabled = openDetailsAction != null) {
                            openDetailsAction?.let(onAction)
                        }
                    )

                    if (formattedDate != null) {
                        Text(
                            text = formattedDate,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                val premiereFallback = stringResource(Res.string.episodes_notifications_movie_premiere)
                val tagText = item.episodeTitle?.ifBlank { null } ?: premiereFallback
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tagText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val overview = item.overview
                if (!overview.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = overview,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Кнопки действий под ТВ-пульт
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (playAction != null) {
                    Row(
                        modifier = Modifier
                            .tvAndWebHoverEffect(
                                scaleTarget = 1.04f,
                                activeBorderWidth = 2.dp,
                                activeBorderColor = Color.White,
                                defaultBorderWidth = 0.dp,
                                shape = RoundedCornerShape(8.dp),
                                tiltEnabled = false,
                                onClick = { onAction(playAction) }
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Play,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(Res.string.episodes_notifications_watch_episode),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (markWatchedAction != null) {
                    Row(
                        modifier = Modifier
                            .tvAndWebHoverEffect(
                                scaleTarget = 1.04f,
                                activeBorderWidth = 1.5.dp,
                                activeBorderColor = MaterialTheme.colorScheme.primary,
                                defaultBorderWidth = 1.dp,
                                defaultBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp),
                                tiltEnabled = false,
                                onClick = { onAction(markWatchedAction) }
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(Res.string.episodes_notifications_mark_watched),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (openDetailsAction != null) {
                    Row(
                        modifier = Modifier
                            .tvAndWebHoverEffect(
                                scaleTarget = 1.04f,
                                activeBorderWidth = 1.5.dp,
                                activeBorderColor = MaterialTheme.colorScheme.primary,
                                defaultBorderWidth = 1.dp,
                                defaultBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp),
                                tiltEnabled = false,
                                onClick = { onAction(openDetailsAction) }
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.episodes_notifications_about_movie),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = Lucide.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
