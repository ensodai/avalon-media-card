package org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.episodes_notifications_filter_all
import avalonmediacard.client.generated.resources.episodes_notifications_filter_recent
import avalonmediacard.client.generated.resources.episodes_notifications_filter_unwatched
import avalonmediacard.client.generated.resources.episodes_notifications_mark_all_read
import avalonmediacard.client.generated.resources.episodes_notifications_subtitle
import avalonmediacard.client.generated.resources.episodes_notifications_title
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.CheckCheck
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.viewState.EpisodeFilter
import org.jetbrains.compose.resources.stringResource


@Composable
fun EpisodesNotificationsHeader(
    totalUnreadCount: Int,
    selectedFilter: EpisodeFilter,
    countAll: Int = 0,
    countRecent: Int = 0,
    countUnwatched: Int = 0,
    onFilterSelected: (EpisodeFilter) -> Unit,
    onMarkAllReadClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AvalonTheme.colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    AvalonIcon(
                        imageVector = Lucide.Bell,
                        contentDescription = null,
                        tint = AvalonTheme.colors.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    AvalonText(
                        text = stringResource(Res.string.episodes_notifications_title),
                        style = AvalonTheme.typography.pageTitle.copy(
                            color = AvalonTheme.colors.textPrimary
                        )
                    )
                    AvalonText(
                        text = stringResource(Res.string.episodes_notifications_subtitle),
                        style = AvalonTheme.typography.bodySynopsis.copy(
                            color = AvalonTheme.colors.textSecondary
                        )
                    )
                }
            }

            if (totalUnreadCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .tvAndWebHoverEffect(
                            scaleTarget = 1.04f,
                            activeBorderWidth = 1.5.dp,
                            activeBorderColor = AvalonTheme.colors.primary,
                            defaultBorderWidth = 1.dp,
                            defaultBorderColor = AvalonTheme.colors.primary.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp),
                            onClick = onMarkAllReadClicked
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(AvalonTheme.colors.primary.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    AvalonIcon(
                        imageVector = Lucide.CheckCheck,
                        contentDescription = null,
                        tint = AvalonTheme.colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    AvalonText(
                        text = stringResource(Res.string.episodes_notifications_mark_all_read),
                        color = AvalonTheme.colors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter tabs with count badges
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NotificationFilterTab(
                label = stringResource(Res.string.episodes_notifications_filter_all),
                count = countAll,
                isSelected = selectedFilter == EpisodeFilter.ALL,
                onClick = { onFilterSelected(EpisodeFilter.ALL) }
            )
            NotificationFilterTab(
                label = stringResource(Res.string.episodes_notifications_filter_recent),
                count = countRecent,
                isSelected = selectedFilter == EpisodeFilter.RECENT_7_DAYS,
                onClick = { onFilterSelected(EpisodeFilter.RECENT_7_DAYS) }
            )
            NotificationFilterTab(
                label = stringResource(Res.string.episodes_notifications_filter_unwatched),
                count = countUnwatched,
                isSelected = selectedFilter == EpisodeFilter.UNWATCHED,
                onClick = { onFilterSelected(EpisodeFilter.UNWATCHED) }
            )
        }
    }
}

@Composable
private fun NotificationFilterTab(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val animatedBgColor by animateColorAsState(
        if (isSelected) AvalonTheme.colors.primary else AvalonTheme.colors.surfaceVariant.copy(alpha = 0.35f)
    )
    val animatedTextColor by animateColorAsState(
        if (isSelected) AvalonTheme.colors.onPrimary else AvalonTheme.colors.onSurface
    )
    val badgeBgColor = if (isSelected) {
        AvalonTheme.colors.onPrimary.copy(alpha = 0.22f)
    } else {
        AvalonTheme.colors.onSurface.copy(alpha = 0.08f)
    }
    val badgeTextColor = if (isSelected) {
        AvalonTheme.colors.onPrimary
    } else {
        AvalonTheme.colors.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .tvAndWebHoverEffect(
                scaleTarget = 1.04f,
                activeBorderWidth = 1.5.dp,
                activeBorderColor = if (isSelected) AvalonTheme.colors.onPrimary else AvalonTheme.colors.primary,
                defaultBorderWidth = if (isSelected) 0.dp else 1.dp,
                defaultBorderColor = if (isSelected) Color.Transparent else AvalonTheme.colors.outlineVariant.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp),
                onClick = onClick
            )
            .clip(RoundedCornerShape(20.dp))
            .background(animatedBgColor)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        AvalonText(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = animatedTextColor
        )
        if (count > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeBgColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                AvalonText(
                    text = count.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor
                )
            }
        }
    }
}
