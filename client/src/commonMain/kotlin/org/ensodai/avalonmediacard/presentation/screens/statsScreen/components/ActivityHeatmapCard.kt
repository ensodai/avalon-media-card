package org.ensodai.avalonmediacard.presentation.screens.statsScreen.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star
import org.ensodai.avalonmediacard.contract.model.ActivityHeatmapDto
import org.ensodai.avalonmediacard.contract.model.HeatmapDayDto
import org.ensodai.avalonmediacard.presentation.navigation.LocalNavigation
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer.LocalTvDrawerState
import org.ensodai.avalonmediacard.presentation.theme.AvalonTheme
import org.jetbrains.compose.resources.stringResource

private enum class HeatmapLensMode {
    VOLUME,
    RATINGS
}

@Composable
fun ActivityHeatmapCard(
    heatmap: ActivityHeatmapDto,
    modifier: Modifier = Modifier
) {
    var lensMode by remember { mutableStateOf(HeatmapLensMode.VOLUME) }
    var hoveredDay by remember { mutableStateOf<HeatmapDayDto?>(null) }
    val dayFocusRequester = remember { FocusRequester() }
    var selectedDayDate by remember { mutableStateOf<String?>(null) }
    val tvDrawerState = LocalTvDrawerState.current
    val navigation = LocalNavigation.current

    val weeks = remember(heatmap.days) {
        heatmap.days.chunked(7)
    }

    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.container,
        color = colors.bgSurface,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // 1. Верхняя панель: Заголовок + компактные KPI слева, переключатель режимов справа
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Иконка + Название
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Calendar,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(Res.string.stats_heatmap_title),
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary
                        )
                    }

                    // Тонкий вертикальный разделитель
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(14.dp)
                            .background(colors.borderSubtle)
                    )

                    // Компактные KPI показатели года
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Активных дней
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${heatmap.totalActiveDays}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "${stringResource(Res.string.stats_heatmap_days_suffix)} ${stringResource(Res.string.stats_heatmap_active_days).lowercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                        }

                        // Текущая серия
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${heatmap.currentStreakDays}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = if (heatmap.currentStreakDays > 0) colors.accentGold else colors.textPrimary
                            )
                            Text(
                                text = "${stringResource(Res.string.stats_heatmap_days_suffix)} ${stringResource(Res.string.stats_heatmap_current_streak).lowercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                            if (heatmap.currentStreakDays > 0) {
                                Icon(
                                    imageVector = Lucide.Flame,
                                    contentDescription = null,
                                    tint = colors.accentGold,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // Пиковый день
                        val peakHours = heatmap.peakDayMinutes / 60.0
                        val peakFormatted = ((peakHours * 10).toInt() / 10.0).toString()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$peakFormatted ${stringResource(Res.string.stats_heatmap_hours_suffix)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = stringResource(Res.string.stats_heatmap_peak_day).lowercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                // Переключатель линз: форма control (10dp), фон bgElevated
                Surface(
                    shape = shapes.control,
                    color = colors.bgElevated
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        HeatmapTabPill(
                            text = stringResource(Res.string.stats_heatmap_mode_volume),
                            isSelected = lensMode == HeatmapLensMode.VOLUME,
                            onClick = { lensMode = HeatmapLensMode.VOLUME }
                        )
                        HeatmapTabPill(
                            text = stringResource(Res.string.stats_heatmap_mode_ratings),
                            isSelected = lensMode == HeatmapLensMode.RATINGS,
                            onClick = { lensMode = HeatmapLensMode.RATINGS }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Адаптивная матрица 52 недели x 7 дней с временной осью месяцев (растянута на всю ширину)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val weekdayColWidth = 24.dp
                val spacingAfterWeekdays = 8.dp
                val cellSpacing = 3.dp
                val availableForGrid = maxWidth - weekdayColWidth - spacingAfterWeekdays
                val weeksCount = weeks.size.coerceAtLeast(1)
                val cellSize = ((availableForGrid - cellSpacing * (weeksCount - 1)) / weeksCount).coerceAtLeast(11.dp)

                val scrollState = rememberScrollState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    verticalAlignment = Alignment.Top
                ) {
                    // Подписи дней недели слева: выровнены ровно по строкам матрицы
                    Column(
                        modifier = Modifier
                            .width(weekdayColWidth)
                            .padding(top = 18.dp), // Отступ под строку месяцев
                        verticalArrangement = Arrangement.spacedBy(cellSpacing)
                    ) {
                        val dayLabels = listOf(
                            stringResource(Res.string.stats_weekday_mon),
                            "",
                            stringResource(Res.string.stats_weekday_wed),
                            "",
                            stringResource(Res.string.stats_weekday_fri),
                            "",
                            ""
                        )
                        dayLabels.forEach { label ->
                            Box(
                                modifier = Modifier
                                    .size(width = weekdayColWidth, height = cellSize),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (label.isNotEmpty()) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textSecondary,
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(spacingAfterWeekdays))

                    // Колонки недель с названиями месяцев сверху
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(cellSpacing),
                        verticalAlignment = Alignment.Top
                    ) {
                        var lastMonthShown = -1
                        weeks.forEachIndexed { weekIndex, week ->
                            val firstDay = week.firstOrNull()
                            val monthNum = firstDay?.date?.substring(5, 7)?.toIntOrNull() ?: 1
                            val isNewMonth = monthNum != lastMonthShown && (weekIndex == 0 || weekIndex - (lastMonthShown * 4) != 0)
                            val showMonthHeader = if (isNewMonth) {
                                lastMonthShown = monthNum
                                true
                            } else {
                                false
                            }

                            Column(horizontalAlignment = Alignment.Start) {
                                // Метка месяца над колонкой
                                Box(
                                    modifier = Modifier
                                        .height(18.dp)
                                        .width(cellSize),
                                    contentAlignment = Alignment.BottomStart
                                ) {
                                    if (showMonthHeader) {
                                        Text(
                                            text = getMonthLabel(monthNum),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textSecondary,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // 7 ячеек дней в колонке (Пн..Вс)
                                Column(verticalArrangement = Arrangement.spacedBy(cellSpacing)) {
                                    week.forEach { day ->
                                        val hasActivity = day.minutes > 0 || day.count > 0
                                        val isHovered = hoveredDay?.date == day.date
                                        val isDrawerOpenForDay = tvDrawerState.isOpen && selectedDayDate == day.date
                                        val isHighlighted = (isHovered || isDrawerOpenForDay) && hasActivity

                                        // В режиме VOLUME активный день подсвечивается теплым золотом (accentGold) для мгновенного контраста.
                                        // В режиме RATINGS базовые дни уже золотые, поэтому активный день загорается чистым фарфоровым белым (chartBarPeak).
                                        val targetColor = when {
                                            isHighlighted -> {
                                                if (lensMode == HeatmapLensMode.VOLUME) colors.accentGold else colors.chartBarPeak
                                            }
                                            else -> getHeatmapCellColor(day, lensMode, colors)
                                        }

                                        val animatedCellBg by animateColorAsState(targetColor)
                                        val animatedScale by animateFloatAsState(if (isHighlighted) 1.28f else 1.0f)

                                        Box(
                                            modifier = Modifier
                                                .size(cellSize)
                                                .zIndex(if (isHighlighted) 2f else 0f)
                                                .graphicsLayer {
                                                    scaleX = animatedScale
                                                    scaleY = animatedScale
                                                }
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(animatedCellBg)
                                                .pointerHoverIcon(if (hasActivity) PointerIcon.Hand else PointerIcon.Default)
                                                .pointerInput(day.date, hasActivity) {
                                                    awaitPointerEventScope {
                                                        while (true) {
                                                            val event = awaitPointerEvent()
                                                            when (event.type) {
                                                                PointerEventType.Enter, PointerEventType.Move -> {
                                                                    hoveredDay = day
                                                                }
                                                                PointerEventType.Exit -> {
                                                                    if (hoveredDay?.date == day.date) hoveredDay = null
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                .then(
                                                    if (hasActivity) {
                                                        val isTarget = selectedDayDate == day.date
                                                        Modifier
                                                            .then(if (isTarget) Modifier.focusRequester(dayFocusRequester) else Modifier)
                                                            .clickable {
                                                                selectedDayDate = day.date
                                                                tvDrawerState.open(
                                                                    DayActivityDrawerScreen(
                                                                        date = day.date,
                                                                        navigation = navigation,
                                                                        callerFocusRequester = dayFocusRequester
                                                                    ),
                                                                    caller = dayFocusRequester
                                                                )
                                                            }
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Стабильный нижний футер: динамическая инфо-строка слева + фиксированная легенда справа
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Динамическая инфо-строка дня при наведении (без рывков интерфейса)
                val curHovered = hoveredDay
                if (curHovered != null && (curHovered.minutes > 0 || curHovered.count > 0)) {
                    val hours = curHovered.minutes / 60
                    val mins = curHovered.minutes % 60
                    val timeStr = if (hours > 0) {
                        if (mins > 0) "$hours ч $mins мин" else "$hours ч"
                    } else {
                        "$mins мин"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = curHovered.date,
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = colors.textPrimary
                        )
                        if (curHovered.count > 0) {
                            Text(
                                text = "(${curHovered.count} ${pluralizeTitles(curHovered.count)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        if (curHovered.avgRating != null) {
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Lucide.Star,
                                    contentDescription = null,
                                    tint = colors.accentGold,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = curHovered.avgRating.toString(),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accentGold
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.stats_heatmap_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary.copy(alpha = 0.7f)
                    )
                }

                // Фиксированная легенда Тафти (Меньше ... Больше)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.stats_heatmap_legend_less),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    val legendColors = if (lensMode == HeatmapLensMode.VOLUME) {
                        listOf(
                            colors.chartTrack,
                            Color(0xFF383840),
                            colors.chartBarDefault,
                            Color(0xFFA1A1AA),
                            colors.chartBarPeak
                        )
                    } else {
                        listOf(
                            colors.chartTrack,
                            Color(0xFF383840),
                            colors.chartBarDefault,
                            Color(0xFFA1A1AA),
                            colors.accentGold
                        )
                    }
                    legendColors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(c)
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = stringResource(Res.string.stats_heatmap_legend_more),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapTabPill(
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
            .padding(horizontal = 11.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = animatedTextColor
        )
    }
}

@Composable
private fun getMonthLabel(month: Int): String = when (month) {
    1 -> stringResource(Res.string.stats_month_1)
    2 -> stringResource(Res.string.stats_month_2)
    3 -> stringResource(Res.string.stats_month_3)
    4 -> stringResource(Res.string.stats_month_4)
    5 -> stringResource(Res.string.stats_month_5)
    6 -> stringResource(Res.string.stats_month_6)
    7 -> stringResource(Res.string.stats_month_7)
    8 -> stringResource(Res.string.stats_month_8)
    9 -> stringResource(Res.string.stats_month_9)
    10 -> stringResource(Res.string.stats_month_10)
    11 -> stringResource(Res.string.stats_month_11)
    else -> stringResource(Res.string.stats_month_12)
}

private fun getHeatmapCellColor(
    day: HeatmapDayDto,
    mode: HeatmapLensMode,
    colors: org.ensodai.avalonmediacard.presentation.theme.AvalonColors
): Color {
    if (day.minutes <= 0 && day.count <= 0) {
        return colors.chartTrack
    }

    return if (mode == HeatmapLensMode.VOLUME) {
        when {
            day.minutes <= 45 -> Color(0xFF383840)
            day.minutes <= 120 -> colors.chartBarDefault
            day.minutes <= 210 -> Color(0xFFA1A1AA)
            else -> colors.chartBarPeak
        }
    } else {
        val rating = day.avgRating
        when {
            rating == null -> Color(0xFF383840)
            rating < 6.0 -> Color(0xFF383840)
            rating < 7.5 -> colors.chartBarDefault
            rating < 8.5 -> Color(0xFFA1A1AA)
            else -> colors.accentGold
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
