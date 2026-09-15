package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.CheckCheck
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.X
import org.ensodai.avalonmediacard.contract.slot.EpisodeItem
import org.ensodai.avalonmediacard.contract.slot.NewEpisodeCardItem
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.overlay.TvModalSurface
import org.jetbrains.compose.resources.stringResource

@Composable
fun EpisodeDetailsDialog(
    modifier: Modifier = Modifier,
    title: String,
    seasonNumber: Int?,
    episodeNumber: Int?,
    stillUrl: String?,
    airDate: String?,
    durationMinutes: Int?,
    voteAverage: Double?,
    overview: String?,
    isWatched: Boolean,
    onPlay: (() -> Unit)?,
    onToggleWatch: (() -> Unit)?,
    onDismiss: () -> Unit,
    callerFocusRequester: FocusRequester? = null,

) {
    TvModalSurface(
        isOpen = true,
        onDismissRequest = onDismiss,
        callerFocusRequester = callerFocusRequester,
        scrimColor = Color.Black.copy(alpha = 0.8f)
    ) {
        Box(
            modifier = modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF16161A))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header with Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val headerText = when {
                            seasonNumber != null && episodeNumber != null ->
                                stringResource(Res.string.episodes_dialog_season_episode, seasonNumber, episodeNumber)
                            episodeNumber != null ->
                                stringResource(Res.string.episodes_dialog_episode_prefix, episodeNumber)
                            else -> ""
                        }

                        if (headerText.isNotBlank()) {
                            Text(
                                text = headerText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .tvAndWebHoverEffect(
                                    scaleTarget = 1.15f,
                                    shape = CircleShape,
                                    activeBorderWidth = 1.5.dp,
                                    activeBorderColor = Color.White.copy(alpha = 0.4f),
                                    defaultBorderWidth = 1.dp,
                                    defaultBorderColor = Color.White.copy(alpha = 0.1f),
                                    tiltEnabled = false,
                                    onClick = onDismiss
                                )
                                .background(Color.White.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = stringResource(Res.string.episodes_dialog_close),
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Large Still Image 16:9
                    if (!stillUrl.isNullOrEmpty() && stillUrl != "placeholder") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F0F12))
                        ) {
                            ShimmerImage(
                                model = stillUrl,
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Title
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Meta: Date, Runtime, Rating
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!airDate.isNullOrEmpty()) {
                            Text(
                                text = airDate,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }

                        if (durationMinutes != null && durationMinutes > 0) {
                            if (!airDate.isNullOrEmpty()) {
                                Text(
                                    text = "•",
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = stringResource(Res.string.player_duration_mins_single_fmt, durationMinutes),
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }

                        if (voteAverage != null && voteAverage > 0.0) {
                            val formattedRating = ((voteAverage * 10).toInt() / 10.0).toString()
                            Text(
                                text = "•",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 13.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = formattedRating,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFC107)
                                )
                            }
                        }
                    }

                    // Full Synopsis
                    val cleanOverview = overview?.trim()
                    if (!cleanOverview.isNullOrEmpty()) {
                        Text(
                            text = cleanOverview,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    // Action Buttons (Play & Watch Toggle)
                    if (onPlay != null || onToggleWatch != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (onPlay != null) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .tvAndWebHoverEffect(
                                            scaleTarget = 1.03f,
                                            shape = RoundedCornerShape(10.dp),
                                            activeBorderWidth = 1.5.dp,
                                            activeBorderColor = Color.White,
                                            tiltEnabled = false,
                                            onClick = onPlay
                                        )
                                        .background(Color.White, RoundedCornerShape(10.dp))
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Lucide.Play,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = stringResource(Res.string.episodes_dialog_watch_episode),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }

                            if (onToggleWatch != null) {
                                val watchBg = if (isWatched) Color(0xFF4CAF50).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f)
                                val watchBorder = if (isWatched) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f)
                                val watchColor = if (isWatched) Color(0xFF4CAF50) else Color.White

                                Box(
                                    modifier = Modifier
                                        .tvAndWebHoverEffect(
                                            scaleTarget = 1.03f,
                                            shape = RoundedCornerShape(10.dp),
                                            activeBorderWidth = 1.5.dp,
                                            activeBorderColor = if (isWatched) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.4f),
                                            defaultBorderWidth = 1.dp,
                                            defaultBorderColor = watchBorder,
                                            tiltEnabled = false,
                                            onClick = onToggleWatch
                                        )
                                        .background(watchBg, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isWatched) Lucide.CheckCheck else Lucide.Eye,
                                            contentDescription = null,
                                            tint = watchColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (isWatched) {
                                                stringResource(Res.string.episodes_dialog_watched)
                                            } else {
                                                stringResource(Res.string.episodes_dialog_mark_watched)
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = watchColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
fun EpisodeDetailsDialog(
    episode: EpisodeItem,
    seasonNumber: Int,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleWatch: () -> Unit,
    callerFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) = EpisodeDetailsDialog(
    title = episode.name,
    seasonNumber = seasonNumber,
    episodeNumber = episode.episodeNumber,
    stillUrl = episode.stillUrl,
    airDate = episode.airDate,
    durationMinutes = episode.runtime,
    voteAverage = episode.voteAverage,
    overview = episode.overview,
    isWatched = episode.isWatched,
    onPlay = onPlay,
    onToggleWatch = onToggleWatch,
    onDismiss = onDismiss,
    callerFocusRequester = callerFocusRequester,
    modifier = modifier
)

@Composable
fun EpisodeDetailsDialog(
    item: NewEpisodeCardItem,
    onDismiss: () -> Unit,
    onPlay: (() -> Unit)?,
    onToggleWatch: (() -> Unit)?,
    callerFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) = EpisodeDetailsDialog(
    title = item.episodeTitle ?: item.showTitle,
    seasonNumber = item.seasonNumber,
    episodeNumber = item.episodeNumber,
    stillUrl = item.stillUrl ?: item.showPosterUrl,
    airDate = item.airDate,
    durationMinutes = item.durationMinutes,
    voteAverage = item.voteAverage,
    overview = item.overview,
    isWatched = item.isWatched,
    onPlay = onPlay,
    onToggleWatch = onToggleWatch,
    onDismiss = onDismiss,
    callerFocusRequester = callerFocusRequester,
    modifier = modifier
)
