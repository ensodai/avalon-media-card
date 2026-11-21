package org.ensodai.avalonmediacard.presentation.screens.statsScreen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.contract.model.DecadeStatDto
import org.ensodai.avalonmediacard.presentation.theme.AvalonTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun DecadesCard(
    decades: List<DecadeStatDto>,
    modifier: Modifier = Modifier
) {
    val maxCount = decades.maxOfOrNull { it.watchedCount }?.coerceAtLeast(1) ?: 1

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
                    imageVector = Lucide.Calendar,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.stats_decades_title),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (decades.isEmpty() || decades.all { it.watchedCount == 0 }) {
                Text(
                    text = stringResource(Res.string.stats_decades_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    for (decade in decades) {
                        val fraction = (decade.watchedCount.toFloat() / maxCount).coerceIn(0.04f, 1f)
                        val isPeak = decade.watchedCount == maxCount && decade.watchedCount > 0

                        val label = when (decade.startYear) {
                            0 -> "<70"
                            1970 -> "70-е"
                            1980 -> "80-е"
                            1990 -> "90-е"
                            2000 -> "00-е"
                            2010 -> "10-е"
                            else -> "20-е"
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (decade.watchedCount > 0) decade.watchedCount.toString() else "",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal,
                                color = if (isPeak) colors.chartBarPeak else colors.textSecondary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .height((80 * fraction).dp)
                                    .clip(shapes.micro)
                                    .background(if (isPeak) colors.chartBarPeak else colors.chartBarDefault)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPeak) colors.textPrimary else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
