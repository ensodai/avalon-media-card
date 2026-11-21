package org.ensodai.avalonmediacard.presentation.screens.statsScreen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.contract.model.ActivityHeatmapDto
import org.ensodai.avalonmediacard.contract.model.HeatmapDayDto
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
    var selectedDay by remember { mutableStateOf<HeatmapDayDto?>(null) }

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
            // Верхняя плашка: Категория слева, Переключатель линз справа
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                // Переключатель линз: форма control (10dp), фон bgElevated, без внутренней рамки
                Surface(
                    shape = shapes.control,
                    color = colors.bgElevated
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
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

            // Крупный KPI-заголовок года в стиле перевернутой пирамиды Bento Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "${heatmap.totalActiveDays} дн",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = FontWeight.Black,
                        color = colors.textPrimary
                    )
                    Text(
                        text = stringResource(Res.string.stats_heatmap_active_days),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }

                Column {
                    Text(
                        text = "${heatmap.currentStreakDays} дн",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = FontWeight.Black,
                        color = colors.textPrimary
                    )
                    Text(
                        text = stringResource(Res.string.stats_heatmap_current_streak),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }

                Column {
                    val peakHours = (heatmap.peakDayMinutes / 60.0)
                    val peakFormatted = ((peakHours * 10).toInt() / 10.0).toString()
                    Text(
                        text = "$peakFormatted ч",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = FontWeight.Black,
                        color = colors.textPrimary
                    )
                    Text(
                        text = stringResource(Res.string.stats_heatmap_peak_day),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Матрица 52 недели x 7 дней с горизонтальным скроллом
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Метки дней недели (Пн, Ср, Пт)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    val dayLabels = listOf("Пн", "", "Ср", "", "Пт", "", "")
                    dayLabels.forEach { label ->
                        Box(
                            modifier = Modifier.size(11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (label.isNotEmpty()) {
                                Text(
                                    text = label,
                                    fontSize = 8.sp,
                                    color = colors.textSecondary.copy(alpha = 0.6f),
                                    lineHeight = 8.sp
                                )
                            }
                        }
                    }
                }

                // Колонки недель
                weeks.forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { day ->
                            val cellColor = getHeatmapCellColor(day, lensMode, colors)
                            val isSelected = selectedDay?.date == day.date

                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(shapes.micro)
                                    .background(cellColor)
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(colors.accentPrimary)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        selectedDay = if (selectedDay?.date == day.date) null else day
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Тултип выбранного дня или легенда Тафти
            val curDay = selectedDay
            if (curDay != null) {
                val avg = curDay.avgRating
                Surface(
                    shape = shapes.control,
                    color = colors.bgElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = curDay.date,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = "${curDay.minutes} мин",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                color = colors.textPrimary
                            )
                            if (avg != null) {
                                Text(
                                    text = "★ ${((avg * 10).toInt() / 10.0)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFeatureSettings = "tnum"
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accentGold
                                )
                            }
                        }
                    }
                }
            } else {
                // Легенда градации
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.stats_heatmap_legend_less),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val legendColors = if (lensMode == HeatmapLensMode.VOLUME) {
                        listOf(
                            colors.chartTrack,
                            Color(0xFF47474F),
                            colors.chartBarDefault,
                            Color(0xFFA1A1AA),
                            colors.chartBarPeak
                        )
                    } else {
                        listOf(
                            colors.chartTrack,
                            Color(0xFF47474F),
                            colors.chartBarDefault,
                            Color(0xFFA1A1AA),
                            colors.accentGold
                        )
                    }
                    legendColors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(shapes.micro)
                                .background(c)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Spacer(modifier = Modifier.width(3.dp))
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

    Box(
        modifier = Modifier
            .clip(shapes.control)
            .background(if (isSelected) colors.bgSurface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) colors.textPrimary else colors.textSecondary
        )
    }
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
            day.minutes <= 45 -> Color(0xFF47474F)
            day.minutes <= 120 -> colors.chartBarDefault
            day.minutes <= 210 -> Color(0xFFA1A1AA)
            else -> colors.chartBarPeak
        }
    } else {
        val rating = day.avgRating
        when {
            rating == null -> Color(0xFF47474F)
            rating < 6.0 -> Color(0xFF47474F)
            rating < 7.5 -> colors.chartBarDefault
            rating < 8.5 -> Color(0xFFA1A1AA)
            else -> colors.accentGold
        }
    }
}
