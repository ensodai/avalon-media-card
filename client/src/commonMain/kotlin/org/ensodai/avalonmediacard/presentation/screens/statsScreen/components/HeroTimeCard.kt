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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import org.ensodai.avalonmediacard.contract.model.TimeMilestoneType
import org.ensodai.avalonmediacard.contract.model.TimeStatsDto
import org.ensodai.avalonmediacard.presentation.overlay.TvModalSurface
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.theme.AvalonTheme
import org.jetbrains.compose.resources.stringResource

private enum class SplitSection {
    MOVIES,
    SHOWS
}

private fun Modifier.splitSectionHover(
    section: SplitSection,
    currentHovered: () -> SplitSection?,
    onHoverChange: (SplitSection?) -> Unit
): Modifier = this
    .pointerHoverIcon(PointerIcon.Default)
    .pointerInput(section) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                when (event.type) {
                    PointerEventType.Enter, PointerEventType.Move -> {
                        onHoverChange(section)
                    }
                    PointerEventType.Exit -> {
                        if (currentHovered() == section) {
                            onHoverChange(null)
                        }
                    }
                }
            }
        }
    }

@Composable
fun HeroTimeCard(
    timeStats: TimeStatsDto,
    onMoviesClicked: (() -> Unit)? = null,
    onShowsClicked: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var hoveredSection by remember { mutableStateOf<SplitSection?>(null) }
    var isMilestonesDialogOpen by remember { mutableStateOf(false) }

    val milestone = timeStats.milestone

    val milestoneTitle = when (milestone.type) {
        TimeMilestoneType.FIRST_STEPS -> stringResource(Res.string.stats_milestone_first_steps_title)
        TimeMilestoneType.LOTR_MARATHON -> stringResource(Res.string.stats_milestone_lotr_title)
        TimeMilestoneType.APOLLO_11 -> stringResource(Res.string.stats_milestone_apollo_title)
        TimeMilestoneType.ORBITAL_MISSION -> stringResource(Res.string.stats_milestone_orbital_title)
        TimeMilestoneType.ARCHIVIST -> stringResource(Res.string.stats_milestone_archivist_title)
    }

    val milestoneDesc = when (milestone.type) {
        TimeMilestoneType.FIRST_STEPS -> stringResource(Res.string.stats_milestone_first_steps_desc)
        TimeMilestoneType.LOTR_MARATHON -> stringResource(Res.string.stats_milestone_lotr_desc, milestone.param)
        TimeMilestoneType.APOLLO_11 -> stringResource(Res.string.stats_milestone_apollo_desc)
        TimeMilestoneType.ORBITAL_MISSION -> stringResource(Res.string.stats_milestone_orbital_desc)
        TimeMilestoneType.ARCHIVIST -> stringResource(Res.string.stats_milestone_archivist_desc, milestone.param)
    }

    val hours = timeStats.totalMinutes / 60
    val days = hours / 24
    val remainingHours = hours % 24

    val formattedTotalTime = when {
        days > 0 -> stringResource(Res.string.stats_hero_time_days_hours_fmt, days, remainingHours)
        hours > 0 -> stringResource(Res.string.stats_hero_time_hours_mins_fmt, hours, timeStats.totalMinutes % 60)
        else -> stringResource(Res.string.stats_hero_time_mins_fmt, timeStats.totalMinutes)
    }

    val moviesHours = timeStats.moviesMinutes / 60
    val showsHours = timeStats.showsMinutes / 60
    val totalSafe = (timeStats.moviesMinutes + timeStats.showsMinutes).coerceAtLeast(1)
    val moviesShare = ((timeStats.moviesMinutes.toFloat() / totalSafe) * 100f).toInt()
    val showsShare = ((timeStats.showsMinutes.toFloat() / totalSafe) * 100f).toInt()

    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    // Анимации интерактивности сплит-бара: утолщается ТОЛЬКО активный сегмент
    val movieHeight by animateDpAsState(
        targetValue = if (hoveredSection == SplitSection.MOVIES) 8.5.dp else 6.dp,
        label = "movie_height"
    )
    val showsHeight by animateDpAsState(
        targetValue = if (hoveredSection == SplitSection.SHOWS) 8.5.dp else 6.dp,
        label = "shows_height"
    )
    val movieAlpha by animateFloatAsState(
        targetValue = if (hoveredSection == SplitSection.SHOWS) 0.35f else 1f,
        label = "movie_alpha"
    )
    val showsAlpha by animateFloatAsState(
        targetValue = if (hoveredSection == SplitSection.MOVIES) 0.35f else 1f,
        label = "shows_alpha"
    )

    var isMilestoneHovered by remember { mutableStateOf(false) }
    val milestoneFocusRequester = remember { FocusRequester() }

    val milestoneTextColor by animateColorAsState(
        targetValue = if (isMilestoneHovered) colors.textPrimary else colors.textSecondary,
        label = "milestone_text_color"
    )
    val milestoneIconColor by animateColorAsState(
        targetValue = if (isMilestoneHovered) colors.accentGold else colors.textSecondary,
        label = "milestone_icon_color"
    )

    val isMoviesHovered = hoveredSection == SplitSection.MOVIES
    val isShowsHovered = hoveredSection == SplitSection.SHOWS

    val moviesTextColor by animateColorAsState(
        targetValue = if (isMoviesHovered) colors.textPrimary else colors.textSecondary,
        label = "movies_text_color"
    )
    val showsTextColor by animateColorAsState(
        targetValue = if (isShowsHovered) colors.textPrimary else colors.textSecondary,
        label = "shows_text_color"
    )
    val moviesDotSize by animateDpAsState(
        targetValue = if (isMoviesHovered) 8.dp else 6.dp,
        label = "movies_dot_size"
    )
    val showsDotSize by animateDpAsState(
        targetValue = if (isShowsHovered) 8.dp else 6.dp,
        label = "shows_dot_size"
    )

    Surface(
        modifier = modifier.defaultMinSize(minHeight = 220.dp),
        shape = shapes.container,
        color = colors.bgSurface,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Верхний ряд: категория + интерактивный бейдж майлстоуна
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Lucide.Clock,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.stats_hero_time_title),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary
                    )
                }

                // Интерактивный бейдж майлстоуна: открывает диалог шкалы достижений (без рамок и плашек)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .focusRequester(milestoneFocusRequester)
                        .tvAndWebHoverEffect(
                            scaleTarget = 1.05f,
                            shape = shapes.control,
                            activeBorderWidth = 0.dp,
                            activeBorderColor = Color.Transparent,
                            defaultBorderWidth = 0.dp,
                            defaultBorderColor = Color.Transparent,
                            onClick = { isMilestonesDialogOpen = true },
                            onStateChange = { isMilestoneHovered = it }
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Flame,
                        contentDescription = null,
                        tint = milestoneIconColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = milestoneTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isMilestoneHovered) FontWeight.Bold else FontWeight.SemiBold,
                        color = milestoneTextColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Lucide.ChevronRight,
                        contentDescription = null,
                        tint = milestoneTextColor.copy(alpha = if (isMilestoneHovered) 0.9f else 0.5f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Крупное отображение времени
            Text(
                text = formattedTotalTime,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFeatureSettings = "tnum"
                ),
                fontWeight = FontWeight.Black,
                color = colors.textPrimary
            )

            Text(
                text = milestoneDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Spacer(modifier = Modifier.weight(1f, fill = false))

            // Интерактивный двухцветный сплит-бар: Фильмы vs Сериалы
            // Контейнер высотой 26dp дает комфортную увеличенную зону наведения (hit-box) без Layout Shift
            // При этом визуальная полоска центрируется, а утолщается только выбранный сегмент
            val movieWeight = moviesShare.toFloat().coerceIn(5f, 95f)
            val showsWeight = (100f - movieWeight).coerceIn(5f, 95f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                contentAlignment = Alignment.Center
            ) {
                // Серый фоновый трек всей полосы
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(shapes.micro)
                        .background(colors.chartTrack)
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Сегмент Фильмов (хит-бокс на всю высоту 26dp, обычный курсор мыши, без клика)
                    Box(
                        modifier = Modifier
                            .weight(movieWeight)
                            .fillMaxHeight()
                            .splitSectionHover(
                                section = SplitSection.MOVIES,
                                currentHovered = { hoveredSection },
                                onHoverChange = { hoveredSection = it }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Визуальная полоска фильмов (утолщается ТОЛЬКО она при ее ховере)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 1.dp)
                                .height(movieHeight)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 4.dp,
                                        bottomStart = 4.dp,
                                        topEnd = 1.5.dp,
                                        bottomEnd = 1.5.dp
                                    )
                                )
                                .background(colors.accentPrimary.copy(alpha = movieAlpha))
                        )
                    }

                    // Сегмент Сериалов (хит-бокс на всю высоту 26dp, обычный курсор мыши, без клика)
                    Box(
                        modifier = Modifier
                            .weight(showsWeight)
                            .fillMaxHeight()
                            .splitSectionHover(
                                section = SplitSection.SHOWS,
                                currentHovered = { hoveredSection },
                                onHoverChange = { hoveredSection = it }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Визуальная полоска сериалов (утолщается ТОЛЬКО она при ее ховере)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 1.dp)
                                .height(showsHeight)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 1.5.dp,
                                        bottomStart = 1.5.dp,
                                        topEnd = 4.dp,
                                        bottomEnd = 4.dp
                                    )
                                )
                                .background(colors.chartBarDefault.copy(alpha = showsAlpha))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Чистая интерактивная легенда (только визуальная подсветка без клика и без смены курсора)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Легенда фильмов
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .splitSectionHover(
                            section = SplitSection.MOVIES,
                            currentHovered = { hoveredSection },
                            onHoverChange = { hoveredSection = it }
                        )
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(moviesDotSize)
                            .clip(CircleShape)
                            .background(colors.accentPrimary)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = stringResource(Res.string.stats_hero_time_movies_fmt, moviesHours, moviesShare),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = if (isMoviesHovered) 13.5.sp else 13.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = if (isMoviesHovered) FontWeight.SemiBold else FontWeight.Normal,
                        color = moviesTextColor
                    )
                }

                // Легенда сериалов
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .splitSectionHover(
                            section = SplitSection.SHOWS,
                            currentHovered = { hoveredSection },
                            onHoverChange = { hoveredSection = it }
                        )
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(showsDotSize)
                            .clip(CircleShape)
                            .background(colors.chartBarDefault)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = stringResource(Res.string.stats_hero_time_shows_fmt, showsHours, showsShare),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = if (isShowsHovered) 13.5.sp else 13.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = if (isShowsHovered) FontWeight.SemiBold else FontWeight.Normal,
                        color = showsTextColor
                    )
                }
            }
        }
    }

    // Модальный диалог всех экранных майлстоунов
    if (isMilestonesDialogOpen) {
        MilestonesDialog(
            currentMilestone = milestone.type,
            totalHours = hours,
            callerFocusRequester = milestoneFocusRequester,
            onDismiss = { isMilestonesDialogOpen = false }
        )
    }
}

@Composable
private fun MilestonesDialog(
    currentMilestone: TimeMilestoneType,
    totalHours: Long,
    callerFocusRequester: FocusRequester? = null,
    onDismiss: () -> Unit
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    data class MilestoneStep(
        val type: TimeMilestoneType,
        val title: String,
        val subtitle: String,
        val minHours: Int
    )

    val steps = listOf(
        MilestoneStep(
            TimeMilestoneType.FIRST_STEPS,
            stringResource(Res.string.stats_milestone_first_steps_title),
            stringResource(Res.string.stats_milestone_first_steps_desc),
            0
        ),
        MilestoneStep(
            TimeMilestoneType.LOTR_MARATHON,
            stringResource(Res.string.stats_milestone_lotr_title),
            "Марафон кинотрилогии «Властелин колец» (12 часов)",
            12
        ),
        MilestoneStep(
            TimeMilestoneType.APOLLO_11,
            stringResource(Res.string.stats_milestone_apollo_title),
            "Экспедиция Apollo-11 на Луну (72 часа)",
            72
        ),
        MilestoneStep(
            TimeMilestoneType.ORBITAL_MISSION,
            stringResource(Res.string.stats_milestone_orbital_title),
            "Орбитальная миссия на Международной космической станции (150 часов)",
            150
        ),
        MilestoneStep(
            TimeMilestoneType.ARCHIVIST,
            stringResource(Res.string.stats_milestone_archivist_title),
            "Архивариус Госфильмофонда (300+ часов киномании)",
            300
        )
    )

    TvModalSurface(
        isOpen = true,
        onDismissRequest = onDismiss,
        callerFocusRequester = callerFocusRequester,
        scrimColor = Color.Black.copy(alpha = 0.75f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = shapes.container,
                color = colors.bgSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Заголовок диалога и кнопка закрытия
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Lucide.Flame,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Шкала экранных вех",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Lucide.X,
                                contentDescription = "Закрыть",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Каждая веха отмечает масштаб просмотренного контента и сопоставляет его с историческими экспедициями и рекордами человечества.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Список ступеней
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val currentIdx = steps.indexOfFirst { it.type == currentMilestone }.coerceAtLeast(0)

                        steps.forEachIndexed { index, step ->
                            val isCompleted = index < currentIdx
                            val isCurrent = index == currentIdx

                            Surface(
                                shape = shapes.control,
                                color = if (isCurrent) colors.bgElevated else colors.bgSurface,
                                border = if (isCurrent) BorderStroke(1.dp, colors.borderSubtle) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Индикатор статуса шага
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isCurrent -> colors.accentPrimary
                                                    isCompleted -> colors.chartBarDefault
                                                    else -> colors.chartTrack
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCompleted) {
                                            Icon(
                                                imageVector = Lucide.Check,
                                                contentDescription = null,
                                                tint = colors.bgSurface,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        } else if (isCurrent) {
                                            Icon(
                                                imageVector = Lucide.Flame,
                                                contentDescription = null,
                                                tint = colors.bgSurface,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = step.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCurrent) colors.textPrimary else if (isCompleted) colors.textSecondary else colors.textSecondary.copy(alpha = 0.5f)
                                            )
                                            if (isCurrent) {
                                                Surface(
                                                    shape = shapes.micro,
                                                    color = colors.accentPrimary
                                                ) {
                                                    Text(
                                                        text = "Текущий",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.bgSurface,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = step.subtitle,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = colors.textSecondary.copy(alpha = if (isCurrent) 0.9f else 0.6f),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Кнопка закрытия
                    Surface(
                        onClick = onDismiss,
                        shape = shapes.control,
                        color = colors.bgElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvAndWebHoverEffect(
                                scaleTarget = 1.02f,
                                shape = shapes.control,
                                activeBorderWidth = 0.dp,
                                onClick = onDismiss
                            )
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Понятно",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
