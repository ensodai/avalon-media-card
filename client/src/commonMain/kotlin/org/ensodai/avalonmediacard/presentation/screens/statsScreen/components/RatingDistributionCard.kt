package org.ensodai.avalonmediacard.presentation.screens.statsScreen.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import org.ensodai.avalonmediacard.designsystem.components.AvalonBentoCard
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonSurface
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star
import org.ensodai.avalonmediacard.contract.model.CriticProfileType
import org.ensodai.avalonmediacard.contract.model.RatingDistributionDto
import org.ensodai.avalonmediacard.contract.model.UserMediaFilter
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.presentation.navigation.LocalNavigation
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun RatingDistributionCard(
    ratingDistribution: RatingDistributionDto,
    modifier: Modifier = Modifier
) {
    val maxCount = ratingDistribution.distribution.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val avg = ratingDistribution.averageRating
    val navigation = LocalNavigation.current

    var hoveredRating by remember { mutableStateOf<Int?>(null) }

    val verdict = when (ratingDistribution.criticProfile) {
        CriticProfileType.NO_RATINGS -> stringResource(Res.string.stats_critic_no_ratings)
        CriticProfileType.ENTHUSIAST -> stringResource(Res.string.stats_critic_enthusiast)
        CriticProfileType.GENEROUS -> stringResource(Res.string.stats_critic_generous)
        CriticProfileType.BALANCED -> stringResource(Res.string.stats_critic_balanced)
        CriticProfileType.STRICT -> stringResource(Res.string.stats_critic_strict)
    }

    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    AvalonBentoCard(
        title = stringResource(Res.string.stats_ratings_title),
        icon = {
            AvalonIcon(
                imageVector = Lucide.Star,
                contentDescription = null,
                tint = colors.accentGold,
                modifier = Modifier.size(16.dp)
            )
        },
        trailingHeader = if (avg != null) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvalonIcon(
                        imageVector = Lucide.Star,
                        contentDescription = null,
                        tint = colors.accentGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    AvalonText(
                        text = ((avg * 10).toInt() / 10.0).toString(),
                        style = AvalonTheme.typography.uiLabel.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            }
        } else null,
        modifier = modifier.defaultMinSize(minHeight = 260.dp),
        contentPadding = 24.dp
    ) {
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
                    val hasItems = count > 0
                    val isHovered = hoveredRating == rating && hasItems
                    val isAnyHovered = hoveredRating != null

                    val animatedBg by animateColorAsState(
                        if (isHovered) colors.bgElevated else Color.Transparent
                    )
                    val animatedBarColor by animateColorAsState(
                        if (isHovered) {
                            if (isPeak) colors.accentGold else colors.chartBarPeak
                        } else if (isAnyHovered) {
                            if (isPeak) colors.accentGold.copy(alpha = 0.55f) else colors.chartBarDefault.copy(alpha = 0.35f)
                        } else {
                            if (isPeak) colors.accentGold else colors.chartBarDefault
                        }
                    )
                    val animatedTopCountColor by animateColorAsState(
                        if (isHovered) {
                            if (isPeak) colors.accentGold else colors.textPrimary
                        } else if (isAnyHovered) {
                            if (isPeak) colors.accentGold.copy(alpha = 0.55f) else colors.textSecondary.copy(alpha = 0.4f)
                        } else if (isPeak) {
                            colors.accentGold
                        } else {
                            colors.textSecondary
                        }
                    )
                    val animatedBottomTextColor by animateColorAsState(
                        if (isHovered) colors.textPrimary
                        else if (isAnyHovered) {
                            if (isPeak) colors.textPrimary.copy(alpha = 0.6f) else colors.textSecondary.copy(alpha = 0.4f)
                        } else if (isPeak) {
                            colors.textPrimary
                        } else {
                            colors.textSecondary
                        }
                    )
                    val animatedBarHeight by animateDpAsState(
                        (80 * fraction).dp + if (isHovered) 4.dp else 0.dp
                    )

                    val screenTitle = stringResource(Res.string.stats_ratings_list_title_fmt, rating)

                    AvalonSurface(
                        onClick = {
                            if (hasItems) {
                                navigation.navigateTo(
                                    Screen.UserMediaList(
                                        filter = UserMediaFilter.ByRating(rating),
                                        title = screenTitle
                                    )
                                )
                            }
                        },
                        enabled = hasItems,
                        shape = shapes.control,
                        color = animatedBg,
                        modifier = Modifier
                            .weight(1f)
                            .pointerHoverIcon(if (hasItems) PointerIcon.Hand else PointerIcon.Default)
                            .pointerInput(rating, hasItems) {
                                if (hasItems) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            when (event.type) {
                                                PointerEventType.Enter, PointerEventType.Move -> hoveredRating = rating
                                                PointerEventType.Exit -> if (hoveredRating == rating) hoveredRating = null
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            AvalonText(
                                text = if (count > 0) count.toString() else "",
                                style = AvalonTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                fontWeight = if (isHovered || isPeak) FontWeight.Bold else FontWeight.Normal,
                                color = animatedTopCountColor
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(if (isHovered) 0.68f else 0.6f)
                                    .height(animatedBarHeight)
                                    .clip(shapes.micro)
                                    .background(animatedBarColor)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            AvalonText(
                                text = rating.toString(),
                                style = AvalonTheme.typography.labelSmall.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                fontWeight = if (isHovered || isPeak) FontWeight.Bold else FontWeight.Medium,
                                color = animatedBottomTextColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Вердикт критика
            AvalonText(
                text = verdict,
                style = AvalonTheme.typography.bodySmall,
                color = colors.textSecondary
            )
    }
}
