package org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.targets.tv.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.initialFocus
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.episodes_notifications_filter_all
import avalonmediacard.client.generated.resources.episodes_notifications_filter_recent
import avalonmediacard.client.generated.resources.episodes_notifications_filter_unwatched
import avalonmediacard.client.generated.resources.episodes_notifications_mark_all_read
import avalonmediacard.client.generated.resources.episodes_notifications_title
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.CheckCheck
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.viewState.EpisodeFilter
import org.jetbrains.compose.resources.stringResource

/**
 * Компактная ТВ-шапка в одну строку: Заголовок + Фильтры + Действие "Прочитать все".
 * Экономит более 120dp вертикального пространства для контента.
 */
@Composable
fun TvNotificationsHeader(
    modifier: Modifier = Modifier,
    totalUnreadCount: Int,
    selectedFilter: EpisodeFilter,
    countAll: Int = 0,
    countRecent: Int = 0,
    countUnwatched: Int = 0,
    onFilterSelected: (EpisodeFilter) -> Unit,
    onMarkAllReadClicked: () -> Unit,
    firstTabFocusRequester: FocusRequester? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Левая часть: Иконка + Заголовок + Фильтры
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.focusGroup()
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Bell,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = stringResource(Res.string.episodes_notifications_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(6.dp))

            TvFilterTab(
                label = stringResource(Res.string.episodes_notifications_filter_all),
                count = countAll,
                isSelected = selectedFilter == EpisodeFilter.ALL,
                onClick = { onFilterSelected(EpisodeFilter.ALL) },
                modifier = Modifier.initialFocus(firstTabFocusRequester)
            )
            TvFilterTab(
                label = stringResource(Res.string.episodes_notifications_filter_recent),
                count = countRecent,
                isSelected = selectedFilter == EpisodeFilter.RECENT_7_DAYS,
                onClick = { onFilterSelected(EpisodeFilter.RECENT_7_DAYS) }
            )
            TvFilterTab(
                label = stringResource(Res.string.episodes_notifications_filter_unwatched),
                count = countUnwatched,
                isSelected = selectedFilter == EpisodeFilter.UNWATCHED,
                onClick = { onFilterSelected(EpisodeFilter.UNWATCHED) }
            )
        }

        // Правая часть: кнопка "Прочитать все"
        if (totalUnreadCount > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .tvAndWebHoverEffect(
                        scaleTarget = 1.04f,
                        activeBorderWidth = 1.5.dp,
                        activeBorderColor = MaterialTheme.colorScheme.primary,
                        defaultBorderWidth = 1.dp,
                        defaultBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        tiltEnabled = false,
                        onClick = onMarkAllReadClicked
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Icon(
                    imageVector = Lucide.CheckCheck,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(Res.string.episodes_notifications_mark_all_read),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TvFilterTab(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBgColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    )
    val animatedTextColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    )
    val badgeBgColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    }
    val badgeTextColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .tvAndWebHoverEffect(
                scaleTarget = 1.05f,
                activeBorderWidth = 2.dp,
                activeBorderColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                defaultBorderWidth = 1.dp,
                defaultBorderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp),
                tiltEnabled = false,
                onClick = onClick
            )
            .clip(RoundedCornerShape(20.dp))
            .background(animatedBgColor)
            .padding(horizontal = 13.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
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
                    .padding(horizontal = 7.dp, vertical = 1.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor
                )
            }
        }
    }
}
