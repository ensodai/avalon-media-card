package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Users
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon as Icon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText as Text
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchPartyReadinessBanner(
    readyCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes
    val elevation = AvalonTheme.elevation
    val typography = AvalonTheme.typography

    val isAllReady = totalCount > 0 && readyCount == totalCount
    val statusColor = if (isAllReady) colors.statusLive else colors.statusWarn

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.container)
            .background(statusColor.copy(alpha = 0.12f))
            .border(
                elevation.borderWidth,
                statusColor.copy(alpha = 0.28f),
                shapes.container
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isAllReady) Lucide.Check else Lucide.Users,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = if (isAllReady) {
                stringResource(Res.string.watch_party_ready_counter_all, readyCount, totalCount)
            } else {
                stringResource(Res.string.watch_party_ready_counter_waiting, readyCount, totalCount)
            },
            style = typography.uiLabel,
            fontSize = 12.sp,
            color = statusColor
        )
    }
}
