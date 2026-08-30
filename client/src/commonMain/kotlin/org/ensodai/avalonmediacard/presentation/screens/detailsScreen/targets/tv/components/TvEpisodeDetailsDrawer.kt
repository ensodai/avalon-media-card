package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.tv.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.*
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ActionPreparePlayer
import org.ensodai.avalonmediacard.contract.slot.EpisodeItem
import org.ensodai.avalonmediacard.contract.slot.RateEpisodeCommand
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonTvDrawerItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvDrawerEffect
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvEpisodeRatingPopup
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TvEpisodeDetailsDrawer(
    isOpen: Boolean,
    episode: EpisodeItem?,
    selectedSeasonNumber: Int,
    onDismiss: () -> Unit,
    onAction: (Action) -> Unit
) {
    if (!isOpen || episode == null) return

    var isRatingPopupOpen by remember { mutableStateOf(false) }

    TvDrawerEffect(
        title = "${episode.episodeNumber}. ${episode.name}",
        subtitle = episode.airDate,
        icon = Lucide.Info,
        onDismiss = onDismiss
    ) {
        val overviewScrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        var isTextFocused by remember { mutableStateOf(false) }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Metadata Badges (Duration, Rating, Watched)
            item(key = "episode_metadata_badges") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val runtime = episode.runtime
                    if (runtime != null && runtime > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.player_duration_mins_single_fmt, runtime),
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

            // 2. Action: Rate Episode
            item(key = "episode_action_rate") {
                val userRating = episode.userRating
                val hasRating = userRating != null && userRating > 0
                AvalonTvDrawerItem(
                    title = if (hasRating && userRating != null) stringResource(Res.string.details_rating_my, userRating) else stringResource(Res.string.details_rating_rate),
                    icon = Lucide.Star,
                    isSelected = hasRating,
                    onClick = { isRatingPopupOpen = true }
                )
            }

            // 3. Action: Mark as Watched / Unmark
            item(key = "episode_action_toggle_watched") {
                AvalonTvDrawerItem(
                    title = if (episode.isWatched) stringResource(Res.string.details_episodes_unmark_watched) else stringResource(Res.string.details_episodes_mark_watched),
                    icon = if (episode.isWatched) Lucide.EyeOff else Lucide.CheckCheck,
                    isSelected = episode.isWatched,
                    onClick = { episode.toggleWatchedAction?.let(onAction) }
                )
            }

            // 4. Focusable Scrollable Overview Text Block
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

    // Episode Rating Popup rendered at the root level (outside TvDrawerEffect)
    if (isRatingPopupOpen) {
        TvEpisodeRatingPopup(
            currentRating = episode.userRating,
            onDismiss = { isRatingPopupOpen = false },
            onRate = { rating ->
                isRatingPopupOpen = false
                val playAct = episode.playAction as? ActionPreparePlayer
                if (playAct != null) {
                    onAction(RateEpisodeCommand(playAct.key, selectedSeasonNumber, episode.episodeNumber, rating))
                }
            }
        )
    }
}
