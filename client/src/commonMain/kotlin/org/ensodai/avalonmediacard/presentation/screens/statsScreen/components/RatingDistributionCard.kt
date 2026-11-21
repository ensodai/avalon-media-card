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
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star
import org.ensodai.avalonmediacard.contract.model.CriticProfileType
import org.ensodai.avalonmediacard.contract.model.RatingDistributionDto
import org.ensodai.avalonmediacard.presentation.theme.AvalonTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun RatingDistributionCard(
    ratingDistribution: RatingDistributionDto,
    modifier: Modifier = Modifier
) {
    val maxCount = ratingDistribution.distribution.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val avg = ratingDistribution.averageRating

    val verdict = when (ratingDistribution.criticProfile) {
        CriticProfileType.NO_RATINGS -> stringResource(Res.string.stats_critic_no_ratings)
        CriticProfileType.ENTHUSIAST -> stringResource(Res.string.stats_critic_enthusiast)
        CriticProfileType.GENEROUS -> stringResource(Res.string.stats_critic_generous)
        CriticProfileType.BALANCED -> stringResource(Res.string.stats_critic_balanced)
        CriticProfileType.STRICT -> stringResource(Res.string.stats_critic_strict)
    }

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
            // Заголовок категории и средний балл
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Lucide.Star,
                        contentDescription = null,
                        tint = colors.accentGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.stats_ratings_title),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary
                    )
                }

                if (avg != null) {
                    // Бейдж среднего балла: фон bgElevated, радиус control (10dp), без внутренней рамки
                    Surface(
                        shape = shapes.control,
                        color = colors.bgElevated
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Star,
                                contentDescription = null,
                                tint = colors.accentGold,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = ((avg * 10).toInt() / 10.0).toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                fontWeight = FontWeight.Bold,
                                color = colors.accentGold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Гистограмма 1..10 по Тафти: серые столбцы + один золотой пик (accentGold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                for (rating in 1..10) {
                    val count = ratingDistribution.distribution[rating] ?: 0
                    val fraction = (count.toFloat() / maxCount).coerceIn(0.04f, 1f)
                    val isPeak = count == maxCount && count > 0

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (count > 0) count.toString() else "",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal,
                            color = if (isPeak) colors.accentGold else colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height((80 * fraction).dp)
                                .clip(shapes.micro)
                                .background(if (isPeak) colors.accentGold else colors.chartBarDefault)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = rating.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Medium,
                            color = if (isPeak) colors.textPrimary else colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Вердикт критика
            Text(
                text = verdict,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}
