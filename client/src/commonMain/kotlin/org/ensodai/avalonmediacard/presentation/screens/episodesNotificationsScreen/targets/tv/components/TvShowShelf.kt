package org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.targets.tv.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.episodes_notifications_episodes_count_1
import avalonmediacard.client.generated.resources.episodes_notifications_episodes_count_few
import avalonmediacard.client.generated.resources.episodes_notifications_episodes_count_many
import avalonmediacard.client.generated.resources.episodes_notifications_new_count
import avalonmediacard.client.generated.resources.episodes_notifications_season_prefix
import avalonmediacard.client.generated.resources.episodes_notifications_seasons_prefix
import avalonmediacard.client.generated.resources.episodes_notifications_to_show
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.NewEpisodeCardItem
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvHorizontalFocusProvider
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun formatTvEpisodeCount(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    val res = when {
        mod100 in 11..19 -> Res.string.episodes_notifications_episodes_count_many
        mod10 == 1 -> Res.string.episodes_notifications_episodes_count_1
        mod10 in 2..4 -> Res.string.episodes_notifications_episodes_count_few
        else -> Res.string.episodes_notifications_episodes_count_many
    }
    return stringResource(res, count)
}

/**
 * ТВ-полка серий сериала с D-Pad центрированием фокуса.
 * Карточка полки выдержана в едином стиле приложения (surfaceVariant + outlineVariant).
 * Мини-постер не забирает фокус пульта.
 */
@Composable
fun TvShowShelf(
    showTitle: String,
    showPosterUrl: String?,
    episodes: List<NewEpisodeCardItem>,
    onAction: (Action) -> Unit,
    onEpisodeOptionsClick: ((NewEpisodeCardItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val openDetailsAction = episodes.firstOrNull { it.openDetailsAction != null }?.openDetailsAction
    val seasons = episodes.mapNotNull { it.seasonNumber }.distinct().sorted()
    val newCount = episodes.count { it.isNew }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Шапка сериала (декоративный мини-постер без фокуса, название, бейджи и кнопка перехода)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    ShimmerImage(
                        model = showPosterUrl,
                        contentDescription = showTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = showTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val seasonLabel = when {
                            seasons.isEmpty() -> ""
                            seasons.size == 1 -> stringResource(Res.string.episodes_notifications_season_prefix, seasons.first())
                            else -> stringResource(Res.string.episodes_notifications_seasons_prefix, seasons.joinToString(", "))
                        }
                        val countText = "$seasonLabel${formatTvEpisodeCount(episodes.size)}"

                        Text(
                            text = countText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (newCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = stringResource(Res.string.episodes_notifications_new_count, newCount),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

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
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.episodes_notifications_to_show),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Lucide.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Горизонтальный список серий с поддержкой центрирования ТВ-фокуса (Open Rail)
        TvHorizontalFocusProvider(pivotFraction = 0.5f) {
            LazyRow(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = episodes,
                    key = { it.id }
                ) { episode ->
                    TvNotificationEpisodeCard(
                        item = episode,
                        onClick = {
                            val playAct = episode.playAction
                            if (playAct != null) {
                                onAction(playAct)
                            } else {
                                episode.openDetailsAction?.let(onAction)
                            }
                        },
                        onOptionsClick = {
                            onEpisodeOptionsClick?.invoke(episode)
                        }
                    )
                }
            }
        }
    }
}
