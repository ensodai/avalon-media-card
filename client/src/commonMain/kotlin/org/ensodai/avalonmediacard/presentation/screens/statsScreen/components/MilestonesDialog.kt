package org.ensodai.avalonmediacard.presentation.screens.statsScreen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import org.ensodai.avalonmediacard.contract.model.TimeMilestoneType
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonIconButton
import org.ensodai.avalonmediacard.designsystem.components.AvalonSurface
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.ensodai.avalonmediacard.presentation.overlay.TvModalSurface
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun MilestonesDialog(
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
            AvalonSurface(
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
                            AvalonIcon(
                                imageVector = Lucide.Flame,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            AvalonText(
                                text = "Шкала экранных вех",
                                style = AvalonTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }

                        AvalonIconButton(onClick = onDismiss) {
                            AvalonIcon(
                                imageVector = Lucide.X,
                                contentDescription = "Закрыть",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AvalonText(
                        text = "Каждая веха отмечает масштаб просмотренного контента и сопоставляет его с историческими экспедициями и рекордами человечества.",
                        style = AvalonTheme.typography.bodySmall,
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

                            AvalonSurface(
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
                                            AvalonIcon(
                                                imageVector = Lucide.Check,
                                                contentDescription = null,
                                                tint = colors.bgSurface,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        } else if (isCurrent) {
                                            AvalonIcon(
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
                                            AvalonText(
                                                text = step.title,
                                                style = AvalonTheme.typography.bodyMedium,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCurrent) colors.textPrimary else if (isCompleted) colors.textSecondary else colors.textSecondary.copy(alpha = 0.5f)
                                            )
                                            if (isCurrent) {
                                                AvalonSurface(
                                                    shape = shapes.micro,
                                                    color = colors.accentPrimary
                                                ) {
                                                    AvalonText(
                                                        text = "Текущий",
                                                        style = AvalonTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.bgSurface,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        AvalonText(
                                            text = step.subtitle,
                                            style = AvalonTheme.typography.bodySmall.copy(fontSize = 11.sp),
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
                    AvalonSurface(
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
                            AvalonText(
                                text = "Понятно",
                                style = AvalonTheme.typography.labelMedium,
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
