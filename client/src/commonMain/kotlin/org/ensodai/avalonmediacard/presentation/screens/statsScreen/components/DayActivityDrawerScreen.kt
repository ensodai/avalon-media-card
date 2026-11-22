package org.ensodai.avalonmediacard.presentation.screens.statsScreen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import org.ensodai.avalonmediacard.designsystem.components.AvalonCircularProgressIndicator
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonSurface
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star
import org.ensodai.avalonmediacard.contract.model.DayActivityDto
import org.ensodai.avalonmediacard.contract.model.DayActivityItemDto
import org.ensodai.avalonmediacard.contract.ui.navigation.Navigation
import org.ensodai.avalonmediacard.domain.repository.StatsRepository
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.navigation.LocalNavigation
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.TvDrawerNavigator
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.TvDrawerScreen
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Выезжающая правая шторка хроники дня.
 * Отображает список фильмов и серий, просмотренных или отмеченных в выбранную дату.
 */
class DayActivityDrawerScreen(
    val date: String,
    val navigation: Navigation,
    override val callerFocusRequester: FocusRequester? = null
) : TvDrawerScreen {

    override val key: String = "day_activity_$date"
    override val title: String = date
    override val icon: ImageVector = Lucide.Calendar

    @Composable
    override fun Content(navigator: TvDrawerNavigator) {
        CompositionLocalProvider(LocalNavigation provides navigation) {
            val statsRepository: StatsRepository = koinInject()
            val colors = AvalonTheme.colors
            val shapes = AvalonTheme.shapes

            var dayActivity by remember { mutableStateOf<DayActivityDto?>(null) }
            var isLoading by remember { mutableStateOf(true) }
            var error by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(date) {
            isLoading = true
            error = null
            try {
                dayActivity = statsRepository.getDayActivity(date)
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AvalonCircularProgressIndicator(
                            color = colors.accentGold,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AvalonText(
                            text = error ?: stringResource(Res.string.stats_loading_error),
                            style = AvalonTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                }

                dayActivity != null -> {
                    val activity = dayActivity!!
                    if (activity.items.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AvalonText(
                                text = stringResource(Res.string.stats_day_activity_empty),
                                style = AvalonTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Сводный блок дня
                            item {
                                DaySummaryCard(activity = activity)
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // Список просмотренных тайтлов
                            items(activity.items, key = { "${it.mediaKey.id}_${it.seasonNumber}_${it.episodeNumber}_${it.isCompleted}" }) { item ->
                                DayActivityItemCard(
                                    item = item,
                                    onClick = {
                                        navigator.clear()
                                        navigation.navigateToDetails(item.mediaKey)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySummaryCard(
    activity: DayActivityDto
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    val hours = activity.totalMinutes / 60
    val minutes = activity.totalMinutes % 60
    val timeFormatted = if (hours > 0) {
        if (minutes > 0) "$hours ч $minutes мин" else "$hours ч"
    } else {
        "$minutes мин"
    }

    AvalonSurface(
        shape = shapes.control,
        color = colors.bgElevated,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                AvalonText(
                    text = timeFormatted,
                    style = AvalonTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                AvalonText(
                    text = "${activity.items.size} ${pluralizeTitles(activity.items.size)}",
                    style = AvalonTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            if (activity.averageRating != null) {
                AvalonSurface(
                    shape = shapes.control,
                    color = colors.bgSurface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AvalonIcon(
                            imageVector = Lucide.Star,
                            contentDescription = null,
                            tint = colors.accentGold,
                            modifier = Modifier.size(12.dp)
                        )
                        AvalonText(
                            text = activity.averageRating.toString(),
                            style = AvalonTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = colors.accentGold
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun DayActivityItemCard(
    item: DayActivityItemDto,
    onClick: () -> Unit
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    AvalonSurface(
        onClick = onClick,
        shape = shapes.control,
        color = colors.bgElevated,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Постер
            Box(
                modifier = Modifier
                    .size(width = 50.dp, height = 75.dp)
            ) {
                ShimmerImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    shape = shapes.media,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Текстовая информация
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AvalonText(
                    text = item.title,
                    style = AvalonTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.isEpisode && item.seasonNumber != null && item.episodeNumber != null) {
                    val epText = buildString {
                        append("S${item.seasonNumber} · E${item.episodeNumber}")
                        if (!item.episodeTitle.isNullOrBlank()) {
                            append(" «${item.episodeTitle}»")
                        }
                    }
                    AvalonText(
                        text = epText,
                        style = AvalonTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Статус и время просмотра
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (item.isCompleted) {
                        AvalonIcon(
                            imageVector = Lucide.Check,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        AvalonText(
                            text = stringResource(Res.string.stats_day_activity_completed),
                            style = AvalonTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    } else {
                        AvalonIcon(
                            imageVector = Lucide.Clock,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        val watchedStr = if (item.durationMinutes > 0) {
                            stringResource(
                                Res.string.stats_day_activity_progress_fmt,
                                item.watchedMinutes,
                                item.durationMinutes
                            )
                        } else {
                            "${item.watchedMinutes} мин"
                        }
                        AvalonText(
                            text = watchedStr,
                            style = AvalonTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = colors.textSecondary
                        )
                    }

                    if (item.userRating != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            AvalonIcon(
                                imageVector = Lucide.Star,
                                contentDescription = null,
                                tint = colors.accentGold,
                                modifier = Modifier.size(11.dp)
                            )
                            AvalonText(
                                text = item.userRating.toString(),
                                style = AvalonTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = colors.accentGold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun pluralizeTitles(count: Int): String {
    val rem10 = count % 10
    val rem100 = count % 100
    return when {
        rem100 in 11..19 -> "тайтлов"
        rem10 == 1 -> "тайтл"
        rem10 in 2..4 -> "тайтла"
        else -> "тайтлов"
    }
}
