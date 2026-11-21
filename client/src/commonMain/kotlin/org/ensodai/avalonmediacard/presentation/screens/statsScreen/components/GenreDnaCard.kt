package org.ensodai.avalonmediacard.presentation.screens.statsScreen.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.contract.model.GenreStatDto
import org.ensodai.avalonmediacard.presentation.theme.AvalonTheme
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun GenreDnaCard(
    genres: List<GenreStatDto>,
    modifier: Modifier = Modifier
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    Surface(
        modifier = modifier.defaultMinSize(minHeight = 260.dp),
        shape = shapes.container,
        color = colors.bgSurface,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Заголовок категории
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Lucide.Flame,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.stats_genres_title),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (genres.isEmpty()) {
                Text(
                    text = stringResource(Res.string.stats_genres_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            } else {
                val topGenres = genres.take(7)
                val maxPercentage = topGenres.firstOrNull()?.percentage?.coerceAtLeast(1f) ?: 1f

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    topGenres.forEachIndexed { index, genre ->
                        val isLeader = index == 0
                        GenreRow(
                            name = genre.genreName,
                            count = genre.watchedCount,
                            percentage = genre.percentage,
                            fraction = (genre.percentage / maxPercentage).coerceIn(0.05f, 1f),
                            isLeader = isLeader
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreRow(
    name: String,
    count: Int,
    percentage: Float,
    fraction: Float,
    isLeader: Boolean
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isLeader) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isLeader) colors.textPrimary else colors.textSecondary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.stats_genres_count_fmt, count),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFeatureSettings = "tnum"
                    ),
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${percentage.roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFeatureSettings = "tnum"
                    ),
                    fontWeight = FontWeight.Bold,
                    color = if (isLeader) colors.chartBarPeak else colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Полоса прогресса по Тафти: трек chartTrack, заливка: лидер - chartBarPeak, остальные - chartBarDefault
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(shapes.micro)
                .background(colors.chartTrack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(shapes.micro)
                    .background(if (isLeader) colors.chartBarPeak else colors.chartBarDefault)
            )
        }
    }
}
