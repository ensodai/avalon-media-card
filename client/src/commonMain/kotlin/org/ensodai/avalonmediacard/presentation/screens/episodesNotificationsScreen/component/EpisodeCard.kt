package org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.ensodai.avalonmediacard.designsystem.components.AvalonBadge
import org.ensodai.avalonmediacard.designsystem.components.AvalonBadgeVariant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.CheckCheck
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.Film
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Star
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.NewEpisodeCardItem
import org.ensodai.avalonmediacard.contract.slot.RateEpisodeCommand
import org.ensodai.avalonmediacard.contract.slot.ToggleEpisodeWatchedCommand
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.EpisodeDetailsDialog
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.EpisodeRatingPopup
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private fun parseReleaseDateParts(airDate: String?): Pair<Int, StringResource>? {
    if (airDate.isNullOrBlank()) return null
    val parts = airDate.split("-")
    if (parts.size != 3) return null
    val day = parts[2].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val res = when (month) {
        1 -> Res.string.month_jan_short
        2 -> Res.string.month_feb_short
        3 -> Res.string.month_mar_short
        4 -> Res.string.month_apr_short
        5 -> Res.string.month_may_short
        6 -> Res.string.month_jun_short
        7 -> Res.string.month_jul_short
        8 -> Res.string.month_aug_short
        9 -> Res.string.month_sep_short
        10 -> Res.string.month_oct_short
        11 -> Res.string.month_nov_short
        12 -> Res.string.month_dec_short
        else -> null
    } ?: return null
    return day to res
}

@Composable
fun formatReleaseDate(airDate: String?): String? {
    if (airDate.isNullOrBlank()) return null
    val parsed = parseReleaseDateParts(airDate)
    return if (parsed != null) {
        val monthName = stringResource(parsed.second)
        "${parsed.first} $monthName"
    } else {
        airDate
    }
}

@Composable
fun EpisodeCard(
    item: NewEpisodeCardItem,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    val playAction = item.playAction
    val markWatchedAction = item.markWatchedAction
    val openDetailsAction = item.openDetailsAction
    val formattedDate = formatReleaseDate(item.airDate)
    val durationMinutes = item.durationMinutes

    val isWatched = item.isWatched
    val userRating = item.userRating

    val seasonNum = item.seasonNumber ?: 1
    val epNum = item.episodeNumber ?: 1
    val onToggleWatched: () -> Unit = {
        val action = markWatchedAction ?: ToggleEpisodeWatchedCommand(
            key = item.mediaKey,
            seasonNumber = seasonNum,
            episodeNumber = epNum,
            isWatched = !item.isWatched
        )
        onAction(action)
    }

    val isTouch = LocalDeviceTarget.current.isTouch
    var isHovered by remember { mutableStateOf(false) }
    var isPlayHovered by remember { mutableStateOf(false) }
    var isSelectedOnTouch by remember { mutableStateOf(false) }
    var isRatingPopupOpen by remember { mutableStateOf(false) }
    var isDetailsDialogOpen by remember { mutableStateOf(false) }

    val isControlsVisible = if (isTouch) isSelectedOnTouch || isRatingPopupOpen else isHovered || isRatingPopupOpen

    val cardBgColor by animateColorAsState(
        targetValue = if (isControlsVisible) Color(0xFF1E1E24) else Color(0xFF141418),
        label = "EpisodeCardBg"
    )

    Box(
        modifier = modifier
            .tvAndWebHoverEffect(
                scaleTarget = if (isTouch) 1f else 1.02f,
                activeBorderWidth = 1.5.dp,
                activeBorderColor = Color.White.copy(alpha = 0.35f),
                defaultBorderWidth = 1.dp,
                defaultBorderColor = if (isSelectedOnTouch) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                tiltEnabled = false,
                clickEnabled = isTouch,
                onClick = if (isTouch) { { isSelectedOnTouch = !isSelectedOnTouch } } else null,
                onStateChange = { if (!isTouch) isHovered = it }
            )
            .clip(RoundedCornerShape(12.dp))
            .background(cardBgColor)
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Превью 16:9 с оверлеями, Action Pill и центральной кнопкой Play
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0D0D10))
                    .clickable(enabled = playAction != null || openDetailsAction != null) {
                        if (playAction != null) onAction(playAction)
                        else openDetailsAction?.let(onAction)
                    },
                contentAlignment = Alignment.Center
            ) {
                val imageAlpha = if (item.isWatched && !isControlsVisible) 0.65f else 1.0f

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
                    AvalonIcon(
                        imageVector = Lucide.Film,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Бейдж "NEW"
                if (item.isNew && !item.isWatched) {
                    AvalonBadge(
                        text = stringResource(Res.string.episodes_notifications_badge_new),
                        variant = AvalonBadgeVariant.Accent,
                        modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                    )
                }

                // Дата выхода (когда капсула действий скрыта)
                if (!isControlsVisible && formattedDate != null) {
                    AvalonBadge(
                        text = formattedDate,
                        variant = AvalonBadgeVariant.Glass,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                    )
                }

                // Длительность (bottom-start)
                if (durationMinutes != null && durationMinutes > 0) {
                    AvalonBadge(
                        text = stringResource(Res.string.player_duration_mins_single_fmt, durationMinutes),
                        variant = AvalonBadgeVariant.Glass,
                        modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                    )
                }

                // Центральная кнопка Play при ховере/фокусе
                androidx.compose.animation.AnimatedVisibility(
                    visible = isControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val playBg by animateColorAsState(
                            targetValue = if (isPlayHovered) Color.White else Color.Black.copy(alpha = 0.75f),
                            label = "PlayBg"
                        )
                        val playIconTint by animateColorAsState(
                            targetValue = if (isPlayHovered) Color.Black else Color.White,
                            label = "PlayIconTint"
                        )

                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .shadow(8.dp, CircleShape)
                                .tvAndWebHoverEffect(
                                    scaleTarget = 1.12f,
                                    shape = CircleShape,
                                    activeBorderWidth = 2.dp,
                                    activeBorderColor = Color.White,
                                    defaultBorderWidth = 1.5.dp,
                                    defaultBorderColor = Color.White.copy(alpha = 0.8f),
                                    tiltEnabled = false,
                                    onStateChange = { isPlayHovered = it },
                                    onClick = {
                                        if (playAction != null) onAction(playAction)
                                        else openDetailsAction?.let(onAction)
                                    }
                                )
                                .background(playBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AvalonIcon(
                                imageVector = Lucide.Play,
                                contentDescription = stringResource(Res.string.episodes_notifications_watch_episode),
                                tint = playIconTint,
                                modifier = Modifier.size(22.dp).offset(x = 1.5.dp)
                            )
                        }
                    }
                }

                // Top-Right Action Pill (Статус просмотра + Рейтинг ⭐)
                androidx.compose.animation.AnimatedVisibility(
                    visible = isControlsVisible,
                    enter = fadeIn() + slideInVertically { -it / 2 },
                    exit = fadeOut() + slideOutVertically { -it / 2 },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    var isWatchHovered by remember { mutableStateOf(false) }
                    var isRatingHovered by remember { mutableStateOf(false) }

                    val watchColor = if (isWatched) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.9f)
                    val currentWatchBg by animateColorAsState(
                        targetValue = if (isWatched) {
                            if (isWatchHovered) Color(0xFF4CAF50).copy(alpha = 0.35f) else Color(0xFF4CAF50).copy(alpha = 0.2f)
                        } else {
                            if (isWatchHovered) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f)
                        },
                        label = "WatchBg"
                    )
                    val currentWatchBorder by animateColorAsState(
                        targetValue = if (isWatched) {
                            if (isWatchHovered) Color(0xFF4CAF50).copy(alpha = 0.7f) else Color(0xFF4CAF50).copy(alpha = 0.4f)
                        } else {
                            if (isWatchHovered) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f)
                        },
                        label = "WatchBorder"
                    )

                    val hasRating = userRating != null && userRating > 0
                    val ratingColor = if (hasRating) Color(0xFFFFC107) else Color.White.copy(alpha = 0.9f)
                    val currentRatingBg by animateColorAsState(
                        targetValue = if (hasRating) {
                            if (isRatingHovered) Color(0xFFFFC107).copy(alpha = 0.35f) else Color(0xFFFFC107).copy(alpha = 0.2f)
                        } else {
                            if (isRatingHovered) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f)
                        },
                        label = "RatingBg"
                    )
                    val currentRatingBorder by animateColorAsState(
                        targetValue = if (hasRating) {
                            if (isRatingHovered) Color(0xFFFFC107).copy(alpha = 0.7f) else Color(0xFFFFC107).copy(alpha = 0.4f)
                        } else {
                            if (isRatingHovered) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f)
                        },
                        label = "RatingBorder"
                    )

                    Row(
                        modifier = Modifier
                            .shadow(10.dp, RoundedCornerShape(20.dp))
                            .background(Color(0xFF141418).copy(alpha = 0.96f), RoundedCornerShape(20.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 5.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Watch Status Button
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .tvAndWebHoverEffect(
                                    scaleTarget = 1.08f,
                                    shape = CircleShape,
                                    activeBorderWidth = 1.5.dp,
                                    activeBorderColor = currentWatchBorder,
                                    defaultBorderWidth = 1.dp,
                                    defaultBorderColor = currentWatchBorder,
                                    tiltEnabled = false,
                                    onStateChange = { isWatchHovered = it },
                                    onClick = onToggleWatched
                                )
                                .background(currentWatchBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AvalonIcon(
                                imageVector = if (isWatched) Lucide.CheckCheck else Lucide.Eye,
                                contentDescription = if (isWatched) {
                                    stringResource(Res.string.episodes_notifications_mark_watched)
                                } else {
                                    stringResource(Res.string.episodes_dialog_mark_watched)
                                },
                                tint = watchColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(16.dp)
                                .background(Color.White.copy(alpha = 0.18f))
                        )

                        // Rating Button
                        Box(contentAlignment = Alignment.Center) {
                            Row(
                                modifier = Modifier
                                    .height(30.dp)
                                    .then(
                                        if (hasRating) Modifier.padding(horizontal = 6.dp)
                                        else Modifier.size(30.dp)
                                    )
                                    .tvAndWebHoverEffect(
                                        scaleTarget = 1.06f,
                                        shape = if (hasRating) RoundedCornerShape(15.dp) else CircleShape,
                                        activeBorderWidth = 1.5.dp,
                                        activeBorderColor = currentRatingBorder,
                                        defaultBorderWidth = 1.dp,
                                        defaultBorderColor = currentRatingBorder,
                                        tiltEnabled = false,
                                        onStateChange = { isRatingHovered = it },
                                        onClick = { isRatingPopupOpen = true }
                                    )
                                    .background(currentRatingBg, if (hasRating) RoundedCornerShape(15.dp) else CircleShape),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                AvalonIcon(
                                    imageVector = Lucide.Star,
                                    contentDescription = if (hasRating) "$userRating" else null,
                                    tint = ratingColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                if (hasRating) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    AvalonText(
                                        text = "$userRating",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ratingColor
                                    )
                                }
                            }

                            if (isRatingPopupOpen) {
                                EpisodeRatingPopup(
                                    currentRating = userRating,
                                    alignment = Alignment.TopEnd,
                                    offset = IntOffset(0, -60),
                                    onDismiss = { isRatingPopupOpen = false },
                                    onRate = { rating ->
                                        isRatingPopupOpen = false
                                        onAction(RateEpisodeCommand(item.mediaKey, seasonNum, epNum, rating))
                                    }
                                )
                            }
                        }
                    }
                }

                // Зеленая полоска прогресса снизу превью (если серия просмотрена)
                if (isWatched) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color(0xFF4CAF50))
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Номер и название серии
            val seasonPart = item.seasonNumber?.let { "S${it.toString().padStart(2, '0')}" }
            val epPart = item.episodeNumber?.let { "E${it.toString().padStart(2, '0')}" }
            val epCode = listOfNotNull(seasonPart, epPart).joinToString(" · ")
            val fallbackTitle = item.episodeNumber?.let { stringResource(Res.string.episodes_notifications_episode_fallback, it) } ?: ""
            val epTitle = item.episodeTitle?.ifBlank { null } ?: fallbackTitle

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (item.isWatched) {
                    AvalonIcon(
                        imageVector = Lucide.CheckCheck,
                        contentDescription = stringResource(Res.string.episodes_notifications_mark_watched),
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(15.dp)
                    )
                }

                AvalonText(
                    text = if (epCode.isNotEmpty()) "$epCode • $epTitle" else epTitle,
                    style = AvalonTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = AvalonTheme.colors.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // Мета-строка: дата релиза + TMDB рейтинг (если есть)
            val voteAverage = item.voteAverage
            val hasVote = voteAverage != null && voteAverage > 0.0
            if (formattedDate != null || hasVote) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (formattedDate != null) {
                        AvalonText(
                            text = formattedDate,
                            fontSize = 11.sp,
                            color = AvalonTheme.colors.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    if (hasVote) {
                        if (formattedDate != null) {
                            AvalonText(
                                text = "•",
                                fontSize = 11.sp,
                                color = AvalonTheme.colors.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            AvalonIcon(
                                imageVector = Lucide.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(11.dp)
                            )
                            val formattedVote = ((voteAverage * 10).toInt() / 10.0).toString()
                            AvalonText(
                                text = formattedVote,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFFC107)
                            )
                        }
                    }
                }
            }

            // Синопсис (2 строки) — кликабельный, открывающий модальное окно EpisodeDetailsDialog
            val overview = item.overview?.trim()
            if (!overview.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvAndWebHoverEffect(
                            scaleTarget = 1.0f,
                            shape = RoundedCornerShape(4.dp),
                            tiltEnabled = false,
                            onClick = { isDetailsDialogOpen = true }
                        ),
                    verticalAlignment = Alignment.Bottom
                ) {
                    AvalonText(
                        text = overview,
                        style = AvalonTheme.typography.bodySmall.copy(
                            color = AvalonTheme.colors.onSurfaceVariant.copy(alpha = if (isHovered) 0.9f else 0.75f),
                            lineHeight = 15.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Модальный диалог с подробным описанием серии
    if (isDetailsDialogOpen) {
        EpisodeDetailsDialog(
            title = item.episodeTitle ?: item.showTitle,
            seasonNumber = item.seasonNumber,
            episodeNumber = item.episodeNumber,
            stillUrl = item.stillUrl ?: item.showPosterUrl,
            airDate = item.airDate,
            durationMinutes = item.durationMinutes,
            voteAverage = item.voteAverage,
            overview = item.overview,
            isWatched = isWatched,
            onPlay = playAction?.let { act ->
                {
                    isDetailsDialogOpen = false
                    onAction(act)
                }
            },
            onToggleWatch = onToggleWatched,
            onDismiss = { isDetailsDialogOpen = false }
        )
    }
}

