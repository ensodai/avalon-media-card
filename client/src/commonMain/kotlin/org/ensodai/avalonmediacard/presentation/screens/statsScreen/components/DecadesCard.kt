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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.contract.model.DecadeStatDto
import org.ensodai.avalonmediacard.contract.model.UserMediaFilter
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.presentation.navigation.LocalNavigation
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
    val navigation = LocalNavigation.current

    var hoveredDecadeYear by remember { mutableStateOf<Int?>(null) }

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
                    style = AvalonTheme.typography.sectionTitle,
                    color = colors.textPrimary
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
                        val hasItems = decade.watchedCount > 0
                        val isHovered = hoveredDecadeYear == decade.startYear && hasItems
                        val isAnyHovered = hoveredDecadeYear != null

                        val animatedBg by animateColorAsState(
                            if (isHovered) colors.bgElevated else Color.Transparent
                        )
                        val animatedBarColor by animateColorAsState(
                            if (isHovered) colors.chartBarPeak
                            else if (isAnyHovered) {
                                if (isPeak) colors.chartBarPeak.copy(alpha = 0.4f) else colors.chartBarDefault.copy(alpha = 0.35f)
                            } else {
                                if (isPeak) colors.chartBarPeak else colors.chartBarDefault
                            }
                        )
                        val animatedTopCountColor by animateColorAsState(
                            if (isHovered) colors.textPrimary
                            else if (isAnyHovered) colors.textSecondary.copy(alpha = 0.4f)
                            else if (isPeak) colors.chartBarPeak
                            else colors.textSecondary
                        )
                        val animatedBottomTextColor by animateColorAsState(
                            if (isHovered) colors.textPrimary
                            else if (isAnyHovered) colors.textSecondary.copy(alpha = 0.4f)
                            else if (isPeak) colors.textPrimary
                            else colors.textSecondary
                        )
                        val animatedBarHeight by animateDpAsState(
                            (80 * fraction).dp + if (isHovered) 4.dp else 0.dp
                        )

                        val label = when (decade.startYear) {
                            0 -> stringResource(Res.string.stats_decade_before_70)
                            1970 -> stringResource(Res.string.stats_decade_70s)
                            1980 -> stringResource(Res.string.stats_decade_80s)
                            1990 -> stringResource(Res.string.stats_decade_90s)
                            2000 -> stringResource(Res.string.stats_decade_00s)
                            2010 -> stringResource(Res.string.stats_decade_10s)
                            else -> stringResource(Res.string.stats_decade_20s)
                        }
                        val decadeKey = decade.startYear.toString()

                        val screenTitle = stringResource(Res.string.stats_decades_list_title_fmt, label)

                        Surface(
                            onClick = {
                                if (hasItems) {
                                    navigation.navigateTo(
                                        Screen.UserMediaList(
                                            filter = UserMediaFilter.ByDecade(decadeKey),
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
                                .pointerInput(decade.startYear, hasItems) {
                                    if (hasItems) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                when (event.type) {
                                                    PointerEventType.Enter, PointerEventType.Move -> hoveredDecadeYear = decade.startYear
                                                    PointerEventType.Exit -> if (hoveredDecadeYear == decade.startYear) hoveredDecadeYear = null
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
                                Text(
                                    text = if (decade.watchedCount > 0) decade.watchedCount.toString() else "",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    fontWeight = if (isHovered || isPeak) FontWeight.Bold else FontWeight.Normal,
                                    color = animatedTopCountColor
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(if (isHovered) 0.65f else 0.55f)
                                        .height(animatedBarHeight)
                                        .clip(shapes.micro)
                                        .background(animatedBarColor)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFeatureSettings = "tnum"
                                    ),
                                    fontWeight = if (isHovered || isPeak) FontWeight.Bold else FontWeight.Medium,
                                    color = animatedBottomTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
