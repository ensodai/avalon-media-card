package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
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
import org.ensodai.avalonmediacard.designsystem.components.AvalonButton
import org.ensodai.avalonmediacard.designsystem.components.AvalonButtonSize
import org.ensodai.avalonmediacard.designsystem.components.AvalonButtonVariant
import org.ensodai.avalonmediacard.designsystem.components.AvalonDialog
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon as Icon
import org.ensodai.avalonmediacard.designsystem.components.AvalonIconButton
import org.ensodai.avalonmediacard.designsystem.components.AvalonText as Text
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
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
    callerFocusRequester: FocusRequester? = null
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    val headerText = when {
        seasonNumber != null && episodeNumber != null ->
            stringResource(Res.string.episodes_dialog_season_episode, seasonNumber, episodeNumber)
        episodeNumber != null ->
            stringResource(Res.string.episodes_dialog_episode_prefix, episodeNumber)
        else -> null
    }

    AvalonDialog(
        isOpen = true,
        onDismissRequest = onDismiss,
        callerFocusRequester = callerFocusRequester,
        modifier = modifier
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
                if (headerText != null) {
                    Text(
                        text = headerText,
                        style = AvalonTheme.typography.uiLabel,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentPrimary
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                AvalonIconButton(
                    onClick = onDismiss,
                    contentDescription = stringResource(Res.string.episodes_dialog_close),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = null,
                        tint = colors.textSecondary,
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
                        .clip(shapes.media)
                        .background(colors.bgSurface)
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
                style = AvalonTheme.typography.sectionTitle,
                color = colors.textPrimary
            )

            // Meta: Date, Runtime, Rating
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!airDate.isNullOrEmpty()) {
                    Text(
                        text = airDate,
                        style = AvalonTheme.typography.metadata,
                        color = colors.textSecondary
                    )
                }

                if (durationMinutes != null && durationMinutes > 0) {
                    if (!airDate.isNullOrEmpty()) {
                        Text(
                            text = "•",
                            style = AvalonTheme.typography.metadata,
                            color = colors.textMuted
                        )
                    }
                    Text(
                        text = stringResource(Res.string.player_duration_mins_single_fmt, durationMinutes),
                        style = AvalonTheme.typography.metadata,
                        color = colors.textSecondary
                    )
                }

                if (voteAverage != null && voteAverage > 0.0) {
                    val formattedRating = ((voteAverage * 10).toInt() / 10.0).toString()
                    Text(
                        text = "•",
                        style = AvalonTheme.typography.metadata,
                        color = colors.textMuted
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Star,
                            contentDescription = null,
                            tint = colors.accentGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formattedRating,
                            style = AvalonTheme.typography.metadata,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentGold
                        )
                    }
                }
            }

            // Full Synopsis
            val cleanOverview = overview?.trim()
            if (!cleanOverview.isNullOrEmpty()) {
                Text(
                    text = cleanOverview,
                    style = AvalonTheme.typography.bodySynopsis,
                    color = colors.textBody
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
                        AvalonButton(
                            text = stringResource(Res.string.episodes_dialog_watch_episode),
                            onClick = onPlay,
                            variant = AvalonButtonVariant.Primary,
                            size = AvalonButtonSize.Standard,
                            leadingIcon = {
                                Icon(
                                    imageVector = Lucide.Play,
                                    contentDescription = null,
                                    tint = colors.onAccentPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (onToggleWatch != null) {
                        val watchLabel = if (isWatched) {
                            stringResource(Res.string.episodes_dialog_watched)
                        } else {
                            stringResource(Res.string.episodes_dialog_mark_watched)
                        }

                        AvalonButton(
                            text = watchLabel,
                            onClick = onToggleWatch,
                            variant = AvalonButtonVariant.Secondary,
                            size = AvalonButtonSize.Standard,
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isWatched) Lucide.CheckCheck else Lucide.Eye,
                                    contentDescription = null,
                                    tint = if (isWatched) colors.statusLive else colors.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
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
    onPlay: () -> Unit,
    onToggleWatch: () -> Unit,
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
