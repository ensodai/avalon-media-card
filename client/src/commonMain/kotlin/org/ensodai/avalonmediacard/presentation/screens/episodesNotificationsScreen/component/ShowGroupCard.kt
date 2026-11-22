package org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import org.ensodai.avalonmediacard.designsystem.components.AvalonCard as Card
import org.ensodai.avalonmediacard.designsystem.components.CardDefaults
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon as Icon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText as Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.NewEpisodeCardItem
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource

@Composable
private fun formatEpisodeCount(count: Int): String {
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

@Composable
fun ShowGroupCard(
    showTitle: String,
    showPosterUrl: String?,
    episodes: List<NewEpisodeCardItem>,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    val isTv = LocalDeviceTarget.current.isTv
    val cardWidth = if (isTv) 320.dp else 290.dp

    val openDetailsAction = episodes.firstOrNull { it.openDetailsAction != null }?.openDetailsAction
    val seasons = episodes.mapNotNull { it.seasonNumber }.distinct().sorted()
    val newCount = episodes.count { it.isNew }

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            // 1. Шапка сериала
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Левая часть: мини-постер + название + сезон и кол-во серий
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AvalonTheme.colors.surfaceVariant)
                            .clickable(enabled = openDetailsAction != null) {
                                openDetailsAction?.let(onAction)
                            }
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
                            val countText = "$seasonLabel${formatEpisodeCount(episodes.size)}"

                            Text(
                                text = countText,
                                style = AvalonTheme.typography.bodySmall.copy(
                                    color = AvalonTheme.colors.onSurfaceVariant
                                )
                            )

                            if (newCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AvalonTheme.colors.primary.copy(alpha = 0.2f))
                                        .border(
                                            1.dp,
                                            AvalonTheme.colors.primary.copy(alpha = 0.5f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = stringResource(Res.string.episodes_notifications_new_count, newCount),
                                        color = AvalonTheme.colors.primary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Правая часть: кнопка перехода к сериалу с поддержкой пульта ТВ и мыши
                if (openDetailsAction != null) {
                    Row(
                        modifier = Modifier
                            .tvAndWebHoverEffect(
                                scaleTarget = 1.04f,
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
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.episodes_notifications_to_show),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = AvalonTheme.colors.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Lucide.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = AvalonTheme.colors.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Горизонтальный список серий
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = episodes,
                    key = { it.id }
                ) { episode ->
                    EpisodeCard(
                        item = episode,
                        onAction = onAction,
                        modifier = Modifier.width(cardWidth)
                    )
                }
            }
        }
    }
}
