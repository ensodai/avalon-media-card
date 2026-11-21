package org.ensodai.avalonmediacard.presentation.screens.statsScreen.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Film
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Tv
import kotlin.math.roundToInt
import org.ensodai.avalonmediacard.contract.model.LibraryStatsDto
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.UserMediaFilter
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.presentation.navigation.LocalNavigation
import org.ensodai.avalonmediacard.presentation.theme.AvalonTheme
import org.jetbrains.compose.resources.stringResource

private enum class LibraryStatsMode {
    SHOWS,
    MOVIES
}

private enum class ShowStatusCategory {
    WATCHING,
    COMPLETED,
    ON_HOLD,
    DNF
}

@Composable
fun LibraryCountersCard(
    libraryStats: LibraryStatsDto,
    modifier: Modifier = Modifier
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes
    val navigation = LocalNavigation.current

    val showsTabName = stringResource(Res.string.stats_library_tab_shows)
    val moviesTabName = stringResource(Res.string.stats_library_tab_movies)

    var selectedMode by remember { mutableStateOf(LibraryStatsMode.SHOWS) }
    var hoveredCategory by remember { mutableStateOf<ShowStatusCategory?>(null) }

    val currentTypeName = if (selectedMode == LibraryStatsMode.SHOWS) showsTabName else moviesTabName
    val currentMediaType = if (selectedMode == LibraryStatsMode.SHOWS) MediaType.TV else MediaType.MOVIE

    val watchingLabel = stringResource(Res.string.stats_library_status_watching)
    val completedLabel = stringResource(Res.string.stats_library_status_completed)
    val plannedLabel = stringResource(Res.string.stats_library_status_planned)
    val droppedLabel = stringResource(Res.string.stats_library_status_dropped)

    val watchingListTitle = stringResource(Res.string.stats_library_list_title_fmt, currentTypeName, watchingLabel)
    val completedListTitle = stringResource(Res.string.stats_library_list_title_fmt, currentTypeName, completedLabel)
    val plannedListTitle = stringResource(Res.string.stats_library_list_title_fmt, currentTypeName, plannedLabel)
    val droppedListTitle = stringResource(Res.string.stats_library_list_title_fmt, currentTypeName, droppedLabel)

    val watchingCount = if (selectedMode == LibraryStatsMode.SHOWS) {
        libraryStats.watchingShowsCount
    } else {
        libraryStats.watchingMoviesCount
    }

    val completedCount = if (selectedMode == LibraryStatsMode.SHOWS) {
        libraryStats.completedShowsCount
    } else {
        libraryStats.completedMoviesCount
    }

    val onHoldCount = if (selectedMode == LibraryStatsMode.SHOWS) {
        libraryStats.onHoldShowsCount
    } else {
        libraryStats.onHoldMoviesCount
    }

    val dnfCount = if (selectedMode == LibraryStatsMode.SHOWS) {
        libraryStats.dnfShowsCount
    } else {
        libraryStats.dnfMoviesCount
    }

    val totalCategorized = watchingCount + completedCount + onHoldCount + dnfCount

    fun formatPercent(count: Int): String? {
        if (totalCategorized <= 0 || count <= 0) return null
        val pct = maxOf(1, (count * 100f / totalCategorized).roundToInt())
        return "$pct%"
    }

    Surface(
        modifier = modifier.defaultMinSize(minHeight = 160.dp),
        shape = shapes.container,
        color = colors.bgSurface,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Заголовок категории
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Lucide.Folder,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.stats_library_title),
                    style = AvalonTheme.typography.sectionTitle,
                    color = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Основные KPI: Фильмы, Серии, Сериалы (Фильмы и Сериалы работают как переключатели)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiPill(
                    icon = Lucide.Film,
                    count = libraryStats.totalMoviesCount,
                    label = stringResource(Res.string.stats_library_movies),
                    isSelected = selectedMode == LibraryStatsMode.MOVIES,
                    onClick = {
                        selectedMode = LibraryStatsMode.MOVIES
                        hoveredCategory = null
                        navigation.navigateTo(
                            Screen.UserMediaList(
                                filter = UserMediaFilter.ByType(MediaType.MOVIE),
                                title = moviesTabName
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                KpiPill(
                    icon = Lucide.Tv,
                    count = libraryStats.totalEpisodesCount,
                    label = stringResource(Res.string.stats_library_episodes),
                    isSelected = false,
                    onClick = null,
                    modifier = Modifier.weight(1f)
                )
                KpiPill(
                    icon = Lucide.Folder,
                    count = libraryStats.totalShowsCount,
                    label = stringResource(Res.string.stats_library_shows),
                    isSelected = selectedMode == LibraryStatsMode.SHOWS,
                    onClick = {
                        selectedMode = LibraryStatsMode.SHOWS
                        hoveredCategory = null
                        navigation.navigateTo(
                            Screen.UserMediaList(
                                filter = UserMediaFilter.ByType(MediaType.TV),
                                title = showsTabName
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Равномерный эластичный отступ, сбалансированный с соседней карточкой ДНК жанров
            Spacer(modifier = Modifier.height(20.dp))
            Spacer(modifier = Modifier.weight(1f))

            // Заголовок секции статусов с микро-переключателями
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedMode == LibraryStatsMode.SHOWS) {
                        stringResource(Res.string.stats_library_backlog_status)
                    } else {
                        stringResource(Res.string.stats_library_movies_status)
                    },
                    style = AvalonTheme.typography.uiLabel,
                    color = colors.textSecondary
                )

                // Сегментированный переключатель (Segmented Control) в едином дизайн-коде
                Surface(
                    shape = shapes.control,
                    color = colors.bgElevated
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        LibrarySegmentTab(
                            text = stringResource(Res.string.stats_library_tab_movies),
                            isSelected = selectedMode == LibraryStatsMode.MOVIES,
                            onClick = {
                                selectedMode = LibraryStatsMode.MOVIES
                                hoveredCategory = null
                            }
                        )
                        LibrarySegmentTab(
                            text = stringResource(Res.string.stats_library_tab_shows),
                            isSelected = selectedMode == LibraryStatsMode.SHOWS,
                            onClick = {
                                selectedMode = LibraryStatsMode.SHOWS
                                hoveredCategory = null
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Мини-трек распределения бэклога
            BacklogBar(
                watchingCount = watchingCount,
                completedCount = completedCount,
                onHoldCount = onHoldCount,
                dnfCount = dnfCount,
                totalCategorized = totalCategorized,
                hoveredCategory = hoveredCategory,
                onHoverChange = { hoveredCategory = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Сетка статусов (2x2) с выравниванием SpaceBetween
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusTag(
                        category = ShowStatusCategory.WATCHING,
                        dotColor = colors.statusLive,
                        count = watchingCount,
                        label = watchingLabel,
                        percent = formatPercent(watchingCount),
                        hoveredCategory = hoveredCategory,
                        onHoverChange = { hoveredCategory = it },
                        onClick = {
                            navigation.navigateTo(
                                Screen.UserMediaList(
                                    filter = UserMediaFilter.ByStatus(currentMediaType, MediaStatus.WATCHING),
                                    title = watchingListTitle
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    StatusTag(
                        category = ShowStatusCategory.COMPLETED,
                        dotColor = colors.textPrimary.copy(alpha = 0.85f),
                        count = completedCount,
                        label = completedLabel,
                        percent = formatPercent(completedCount),
                        hoveredCategory = hoveredCategory,
                        onHoverChange = { hoveredCategory = it },
                        onClick = {
                            navigation.navigateTo(
                                Screen.UserMediaList(
                                    filter = UserMediaFilter.ByStatus(currentMediaType, MediaStatus.COMPLETED),
                                    title = completedListTitle
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusTag(
                        category = ShowStatusCategory.ON_HOLD,
                        dotColor = colors.statusWarn,
                        count = onHoldCount,
                        label = plannedLabel,
                        percent = formatPercent(onHoldCount),
                        hoveredCategory = hoveredCategory,
                        onHoverChange = { hoveredCategory = it },
                        onClick = {
                            navigation.navigateTo(
                                Screen.UserMediaList(
                                    filter = UserMediaFilter.ByStatus(currentMediaType, MediaStatus.PLANNED),
                                    title = plannedListTitle
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    StatusTag(
                        category = ShowStatusCategory.DNF,
                        dotColor = colors.statusDanger,
                        count = dnfCount,
                        label = droppedLabel,
                        percent = formatPercent(dnfCount),
                        hoveredCategory = hoveredCategory,
                        onHoverChange = { hoveredCategory = it },
                        onClick = {
                            navigation.navigateTo(
                                Screen.UserMediaList(
                                    filter = UserMediaFilter.ByStatus(currentMediaType, MediaStatus.DROPPED),
                                    title = droppedListTitle
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LibrarySegmentTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes
    var isHovered by remember { mutableStateOf(false) }

    val animatedBg by animateColorAsState(
        if (isSelected) {
            colors.bgSurface
        } else if (isHovered) {
            colors.borderSubtle.copy(alpha = 0.4f)
        } else {
            Color.Transparent
        }
    )
    val animatedTextColor by animateColorAsState(
        if (isSelected || isHovered) colors.textPrimary else colors.textSecondary
    )

    Box(
        modifier = Modifier
            .clip(shapes.control)
            .background(animatedBg)
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter, PointerEventType.Move -> isHovered = true
                            PointerEventType.Exit -> isHovered = false
                        }
                    }
                }
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = animatedTextColor
        )
    }
}

@Composable
private fun KpiPill(
    icon: ImageVector,
    count: Int,
    label: String,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    var isHovered by remember { mutableStateOf(false) }

    val animatedBg by animateColorAsState(
        if (isSelected) {
            colors.bgOverlay
        } else if (isHovered) {
            colors.borderSubtle.copy(alpha = 0.4f)
        } else {
            colors.bgElevated
        }
    )
    val animatedIconColor by animateColorAsState(
        if (isSelected || isHovered) colors.textPrimary else colors.textSecondary
    )

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier
            .pointerHoverIcon(if (onClick != null) PointerIcon.Hand else PointerIcon.Default)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter, PointerEventType.Move -> isHovered = true
                            PointerEventType.Exit -> isHovered = false
                        }
                    }
                }
            },
        shape = shapes.control,
        color = animatedBg,
        border = null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = animatedIconColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = count.toString(),
                style = AvalonTheme.typography.kpiTile,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = AvalonTheme.typography.metadata,
                color = if (isSelected) colors.textPrimary else colors.textSecondary
            )
        }
    }
}

@Composable
private fun BacklogBar(
    watchingCount: Int,
    completedCount: Int,
    onHoldCount: Int,
    dnfCount: Int,
    totalCategorized: Int,
    hoveredCategory: ShowStatusCategory?,
    onHoverChange: (ShowStatusCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Фоновый серый трек
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(shapes.micro)
                .background(colors.chartTrack)
        )

        if (totalCategorized > 0) {
            val watchingRatio = watchingCount.toFloat() / totalCategorized
            val completedRatio = completedCount.toFloat() / totalCategorized
            val onHoldRatio = onHoldCount.toFloat() / totalCategorized
            val dnfRatio = dnfCount.toFloat() / totalCategorized

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(shapes.micro),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (watchingCount > 0) {
                    BacklogSegment(
                        category = ShowStatusCategory.WATCHING,
                        color = colors.statusLive,
                        hoveredCategory = hoveredCategory,
                        onHoverChange = onHoverChange,
                        modifier = Modifier.weight(watchingRatio)
                    )
                }
                if (completedCount > 0) {
                    BacklogSegment(
                        category = ShowStatusCategory.COMPLETED,
                        color = colors.textPrimary.copy(alpha = 0.85f),
                        hoveredCategory = hoveredCategory,
                        onHoverChange = onHoverChange,
                        modifier = Modifier.weight(completedRatio)
                    )
                }
                if (onHoldCount > 0) {
                    BacklogSegment(
                        category = ShowStatusCategory.ON_HOLD,
                        color = colors.statusWarn,
                        hoveredCategory = hoveredCategory,
                        onHoverChange = onHoverChange,
                        modifier = Modifier.weight(onHoldRatio)
                    )
                }
                if (dnfCount > 0) {
                    BacklogSegment(
                        category = ShowStatusCategory.DNF,
                        color = colors.statusDanger,
                        hoveredCategory = hoveredCategory,
                        onHoverChange = onHoverChange,
                        modifier = Modifier.weight(dnfRatio)
                    )
                }
            }
        }
    }
}

@Composable
private fun BacklogSegment(
    category: ShowStatusCategory,
    color: Color,
    hoveredCategory: ShowStatusCategory?,
    onHoverChange: (ShowStatusCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    val shapes = AvalonTheme.shapes
    val currentHovered by rememberUpdatedState(hoveredCategory)
    val isHovered = hoveredCategory == category
    val isAnyHovered = hoveredCategory != null

    val animatedHeight by animateDpAsState(
        targetValue = if (isHovered) 6.5.dp else 4.dp
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isHovered) 1.0f else if (isAnyHovered) 0.35f else 1.0f
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .pointerHoverIcon(PointerIcon.Default)
            .pointerInput(category) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter, PointerEventType.Move -> onHoverChange(category)
                            PointerEventType.Exit -> {
                                if (currentHovered == category) onHoverChange(null)
                            }
                        }
                    }
                }
            }
            .padding(horizontal = 0.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .clip(shapes.micro)
                .background(color.copy(alpha = animatedAlpha))
        )
    }
}

@Composable
private fun StatusTag(
    category: ShowStatusCategory,
    dotColor: Color,
    count: Int,
    label: String,
    percent: String?,
    hoveredCategory: ShowStatusCategory?,
    onHoverChange: (ShowStatusCategory?) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    val currentHovered by rememberUpdatedState(hoveredCategory)
    val isHovered = hoveredCategory == category
    val isAnyHovered = hoveredCategory != null

    val animatedBg by animateColorAsState(
        if (isHovered) colors.borderSubtle.copy(alpha = 0.5f) else colors.bgElevated
    )
    val animatedDotSize by animateDpAsState(
        if (isHovered) 8.dp else 6.dp
    )
    val animatedLabelColor by animateColorAsState(
        if (isHovered) colors.textPrimary else if (isAnyHovered) colors.textSecondary.copy(alpha = 0.5f) else colors.textSecondary
    )
    val animatedCountColor by animateColorAsState(
        if (isHovered) colors.textPrimary else if (isAnyHovered) colors.textPrimary.copy(alpha = 0.5f) else colors.textPrimary
    )
    val animatedDotAlpha by animateFloatAsState(
        if (isHovered) 1.0f else if (isAnyHovered) 0.4f else 1.0f
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .pointerInput(category) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter, PointerEventType.Move -> onHoverChange(category)
                            PointerEventType.Exit -> {
                                if (currentHovered == category) onHoverChange(null)
                            }
                        }
                    }
                }
            },
        shape = shapes.control,
        color = animatedBg,
        border = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Левая часть: точка + название статуса
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(animatedDotSize)
                        .clip(CircleShape)
                        .background(dotColor.copy(alpha = animatedDotAlpha))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = AvalonTheme.typography.uiLabel,
                    fontWeight = if (isHovered) FontWeight.SemiBold else FontWeight.Medium,
                    color = animatedLabelColor
                )
            }

            // Правая часть: количество + процент
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = count.toString(),
                    style = AvalonTheme.typography.uiLabel.copy(
                        fontFeatureSettings = "tnum"
                    ),
                    fontWeight = FontWeight.Bold,
                    color = animatedCountColor
                )
                if (percent != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = percent,
                        style = AvalonTheme.typography.microCaption,
                        color = colors.textSecondary.copy(alpha = if (isHovered) 0.85f else 0.5f)
                    )
                }
            }
        }
    }
}
