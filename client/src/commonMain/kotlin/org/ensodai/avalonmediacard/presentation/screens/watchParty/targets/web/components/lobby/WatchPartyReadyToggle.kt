package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.designsystem.components.AvalonText as Text
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchPartyReadyToggle(
    isReady: Boolean,
    onToggleReady: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes
    val elevation = AvalonTheme.elevation
    val typography = AvalonTheme.typography

    val shape = shapes.control
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .tvAndWebHoverEffect(
                scaleTarget = 1.02f,
                shape = shape,
                activeBorderColor = if (isReady) colors.statusLive else colors.borderFocus,
                defaultBorderWidth = elevation.borderWidth,
                defaultBorderColor = if (isReady) colors.statusLive.copy(alpha = 0.6f) else colors.borderSubtle,
                onClick = onToggleReady
            )
            .background(if (isReady) colors.statusLive.copy(alpha = 0.2f) else colors.bgElevated, shape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isReady) colors.statusLive else colors.textMuted)
            )
            Text(
                text = if (isReady) stringResource(Res.string.watch_party_ready_check) else stringResource(Res.string.watch_party_ready_check_hint),
                style = typography.uiLabel,
                color = colors.textPrimary
            )
        }

        Text(
            text = if (isReady) stringResource(Res.string.watch_party_ready_status_ready) else stringResource(Res.string.watch_party_ready_status_not_ready),
            style = typography.metadata,
            color = if (isReady) colors.statusLive else colors.textSecondary
        )
    }
}
