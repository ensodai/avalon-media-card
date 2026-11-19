package org.ensodai.avalonmediacard.presentation.screens.statsScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Clapperboard
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Tv
import org.ensodai.avalonmediacard.contract.model.TimeMilestoneType
import org.ensodai.avalonmediacard.contract.model.TimeStatsDto
import org.jetbrains.compose.resources.stringResource

@Composable
fun HeroTimeCard(
    timeStats: TimeStatsDto,
    modifier: Modifier = Modifier
) {
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

    Surface(
        modifier = modifier.defaultMinSize(minHeight = 220.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Заголовок блока с иконкой часов
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Clock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(Res.string.stats_hero_time_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Индикатор майлстоуна
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Sparkles,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = milestoneTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Крупное отображение времени
            Text(
                text = formattedTotalTime,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = milestoneDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Двухцветный стек-бар соотношения Фильмы vs Сериалы
            val movieWeight = moviesShare.toFloat().coerceIn(5f, 95f)
            val showsWeight = (100f - movieWeight).coerceIn(5f, 95f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(movieWeight)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .weight(showsWeight)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFEC4899), Color(0xFFF43F5E))
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Подписи под стек-баром: Фильмы vs Сериалы
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Lucide.Clapperboard,
                        contentDescription = null,
                        tint = Color(0xFF8B5CF6),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(Res.string.stats_hero_time_movies_fmt, moviesHours, moviesShare),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Lucide.Tv,
                        contentDescription = null,
                        tint = Color(0xFFEC4899),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(Res.string.stats_hero_time_shows_fmt, showsHours, showsShare),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
