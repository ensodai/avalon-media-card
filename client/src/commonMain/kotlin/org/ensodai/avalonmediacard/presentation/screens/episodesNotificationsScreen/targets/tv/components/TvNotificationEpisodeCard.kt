package org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.targets.tv.components

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.episodes_notifications_badge_new
import avalonmediacard.client.generated.resources.episodes_notifications_hold_for_options
import avalonmediacard.client.generated.resources.episodes_notifications_watch_episode
import avalonmediacard.client.generated.resources.player_duration_mins_single_fmt
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Film
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.slot.NewEpisodeCardItem
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component.formatReleaseDate
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.milliseconds

/**
 * ТВ-карточка серии: 16:9 превью, адаптивная ширина (250dp),
 * запуск по клику OK, вызов шторки по долгому нажатию OK или кнопке Menu/Info.
 * Фон и обводка согласованы с темой приложения.
 */
@Composable
fun TvNotificationEpisodeCard(
    item: NewEpisodeCardItem,
    isCurrentFocused: Boolean = false,
    onFocus: (() -> Unit)? = null,
    onClick: () -> Unit,
    onOptionsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val formattedDate = formatReleaseDate(item.airDate)
    val duration = item.durationMinutes

    var isKeyDown by remember { mutableStateOf(false) }
    var isLongPressTriggered by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .width(250.dp)
            .padding(vertical = 4.dp)
            .onPreviewKeyEvent { event ->
                if (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter) {
                    when (event.type) {
                        KeyEventType.KeyDown -> {
                            if (!isKeyDown) {
                                isKeyDown = true
                                isLongPressTriggered = false
                                coroutineScope.launch {
                                    delay(500.milliseconds)
                                    if (isKeyDown) {
                                        isLongPressTriggered = true
                                        onOptionsClick?.invoke()
                                    }
                                }
                            }
                            true
                        }
                        KeyEventType.KeyUp -> {
                            isKeyDown = false
                            if (!isLongPressTriggered) {
                                onClick()
                            }
                            true
                        }
                        else -> false
                    }
                } else if (event.type == KeyEventType.KeyDown && (event.key == Key.Menu || event.key == Key.Info)) {
                    onOptionsClick?.invoke()
                    true
                } else {
                    false
                }
            }
            .tvAndWebHoverEffect(
                scaleTarget = 1.05f,
                activeBorderWidth = 2.dp,
                activeBorderColor = Color.White,
                defaultBorderWidth = 1.dp,
                defaultBorderColor = if (isCurrentFocused) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp),
                tiltEnabled = false,
                clickEnabled = false,
                onStateChange = { active ->
                    isFocused = active
                    if (active) onFocus?.invoke()
                }
            )
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // 1. Превью 16:9
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(Color(0xFF0D0D10)),
            contentAlignment = Alignment.Center
        ) {
            val imageAlpha = if (item.isWatched && !isFocused) 0.60f else 1.0f
            val still = item.stillUrl ?: item.showPosterUrl

            if (!still.isNullOrEmpty() && still != "placeholder") {
                ShimmerImage(
                    model = still,
                    contentDescription = item.episodeTitle ?: item.showTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = imageAlpha }
                )
            } else {
                Icon(
                    imageVector = Lucide.Film,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Оверлей фокуса с кнопкой Play и подсказкой для пульта
            androidx.compose.animation.AnimatedVisibility(
                visible = isFocused,
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
                            .size(44.dp)
                            .shadow(8.dp, CircleShape)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Play,
                            contentDescription = stringResource(Res.string.episodes_notifications_watch_episode),
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp).offset(x = 1.5.dp)
                        )
                    }

                    if (onOptionsClick != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.episodes_notifications_hold_for_options),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Бейдж "NEW" (top-start)
            if (item.isNew && !item.isWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.episodes_notifications_badge_new),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Дата выхода (top-end, если не в фокусе)
            if (!isFocused && formattedDate != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formattedDate,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Длительность (bottom-start)
            if (duration != null && duration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.player_duration_mins_single_fmt, duration),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Бейдж "Просмотрено" (top-end)
            if (item.isWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.95f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // 2. Описание и название серии под превью (чистый заголовок без микро-кнопок)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            val episodePrefix = when {
                item.seasonNumber != null && item.episodeNumber != null -> "S${item.seasonNumber} · E${item.episodeNumber}"
                item.episodeNumber != null -> "E${item.episodeNumber}"
                else -> ""
            }
            val title = item.episodeTitle ?: item.showTitle

            if (episodePrefix.isNotBlank()) {
                Text(
                    text = episodePrefix,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (isFocused) 1f else 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isFocused) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TvNotificationEpisodeCardSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(250.dp)
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .shimmerPlaceholder(true, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .fillMaxWidth(0.5f)
                .height(11.dp)
                .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .fillMaxWidth(0.8f)
                .height(13.dp)
                .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
