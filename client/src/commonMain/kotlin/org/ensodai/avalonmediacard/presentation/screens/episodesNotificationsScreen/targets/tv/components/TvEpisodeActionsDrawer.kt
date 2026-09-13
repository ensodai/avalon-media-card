package org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.targets.tv.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.details_episodes_unmark_watched
import avalonmediacard.client.generated.resources.details_rating_my
import avalonmediacard.client.generated.resources.details_rating_rate
import avalonmediacard.client.generated.resources.details_seasons_completed
import avalonmediacard.client.generated.resources.details_seasons_no_desc
import avalonmediacard.client.generated.resources.episodes_notifications_about_show
import avalonmediacard.client.generated.resources.episodes_notifications_mark_watched
import avalonmediacard.client.generated.resources.episodes_notifications_watch_episode
import avalonmediacard.client.generated.resources.player_duration_mins_single_fmt
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.CheckCheck
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Star
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.NewEpisodeCardItem
import org.ensodai.avalonmediacard.contract.slot.RateEpisodeCommand
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonTvDrawerItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvDrawerEffect
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvEpisodeRatingPopup
import org.jetbrains.compose.resources.stringResource

/**
 * Выдвижная правая ТВ-шторка с действиями над серией из уведомлений.
 */
@Composable
fun TvEpisodeActionsDrawer(
    isOpen: Boolean,
    episode: NewEpisodeCardItem?,
    onDismiss: () -> Unit,
    onAction: (Action) -> Unit
) {
    if (!isOpen || episode == null) return

    var isRatingPopupOpen by remember { mutableStateOf(false) }

    val episodePrefix = when {
        episode.seasonNumber != null && episode.episodeNumber != null -> "S${episode.seasonNumber} · E${episode.episodeNumber}"
        episode.episodeNumber != null -> "E${episode.episodeNumber}"
        else -> ""
    }
    val title = episode.episodeTitle ?: episode.showTitle
    val subtitle = if (episodePrefix.isNotBlank()) "$episodePrefix · ${episode.showTitle}" else episode.showTitle

    TvDrawerEffect(
        title = title,
        subtitle = subtitle,
        icon = Lucide.Info,
        onDismiss = onDismiss
    ) {
        val overviewScrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        var isTextFocused by remember { mutableStateOf(false) }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Метаданные (длительность, рейтинг, статус просмотра)
            item(key = "tv_episode_badges") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val duration = episode.durationMinutes
                    if (duration != null && duration > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.player_duration_mins_single_fmt, duration),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    val voteAvg = episode.voteAverage
                    if (voteAvg != null && voteAvg > 0.0) {
                        val formattedRating = ((voteAvg * 10).toInt() / 10.0).toString()
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = formattedRating,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFC107)
                            )
                        }
                    }

                    if (episode.isWatched) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = stringResource(Res.string.details_seasons_completed),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }

            // 2. Действие: Смотреть серию
            val playAction = episode.playAction
            if (playAction != null) {
                item(key = "action_play_episode") {
                    AvalonTvDrawerItem(
                        title = stringResource(Res.string.episodes_notifications_watch_episode),
                        icon = Lucide.Play,
                        onClick = {
                            onDismiss()
                            onAction(playAction)
                        }
                    )
                }
            }

            // 3. Действие: Отметить просмотренной / снять отметку
            val markWatchedAction = episode.markWatchedAction
            if (markWatchedAction != null) {
                item(key = "action_toggle_watched") {
                    AvalonTvDrawerItem(
                        title = if (episode.isWatched) stringResource(Res.string.details_episodes_unmark_watched)
                        else stringResource(Res.string.episodes_notifications_mark_watched),
                        icon = if (episode.isWatched) Lucide.EyeOff else Lucide.CheckCheck,
                        isSelected = episode.isWatched,
                        onClick = {
                            onAction(markWatchedAction)
                        }
                    )
                }
            }

            // 4. Действие: Оценить серию
            item(key = "action_rate_episode") {
                val userRating = episode.userRating
                val hasRating = userRating != null && userRating > 0
                AvalonTvDrawerItem(
                    title = if (hasRating) stringResource(Res.string.details_rating_my, userRating)
                    else stringResource(Res.string.details_rating_rate),
                    icon = Lucide.Star,
                    isSelected = hasRating,
                    onClick = {
                        isRatingPopupOpen = true
                    }
                )
            }

            // 5. Действие: Перейти к сериалу
            val openDetailsAction = episode.openDetailsAction
            if (openDetailsAction != null) {
                item(key = "action_open_details") {
                    AvalonTvDrawerItem(
                        title = stringResource(Res.string.episodes_notifications_about_show),
                        icon = Lucide.ChevronRight,
                        onClick = {
                            onDismiss()
                            onAction(openDetailsAction)
                        }
                    )
                }
            }

            // 6. Описание серии с поддержкой D-Pad скролла
            item(key = "episode_overview_block") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isTextFocused) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f))
                        .border(
                            width = 1.dp,
                            color = if (isTextFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .onFocusChanged { isTextFocused = it.isFocused }
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.DirectionDown -> {
                                        if (overviewScrollState.canScrollForward) {
                                            coroutineScope.launch { overviewScrollState.animateScrollBy(120f) }
                                            true
                                        } else false
                                    }
                                    Key.DirectionUp -> {
                                        if (overviewScrollState.canScrollBackward) {
                                            coroutineScope.launch { overviewScrollState.animateScrollBy(-120f) }
                                            true
                                        } else false
                                    }
                                    else -> false
                                }
                            } else false
                        }
                        .focusable()
                        .verticalScroll(overviewScrollState)
                        .padding(16.dp)
                ) {
                    Text(
                        text = episode.overview?.takeIf { it.isNotBlank() }
                            ?: stringResource(Res.string.details_seasons_no_desc),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }

    if (isRatingPopupOpen) {
        TvEpisodeRatingPopup(
            currentRating = episode.userRating,
            onDismiss = { isRatingPopupOpen = false },
            onRate = { rating ->
                isRatingPopupOpen = false
                val seasonNum = episode.seasonNumber ?: 1
                val epNum = episode.episodeNumber ?: 1
                onAction(RateEpisodeCommand(episode.mediaKey, seasonNum, epNum, rating))
            }
        )
    }
}
