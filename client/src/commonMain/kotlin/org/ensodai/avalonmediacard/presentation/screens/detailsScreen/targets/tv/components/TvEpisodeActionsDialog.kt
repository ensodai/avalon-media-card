package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.tv.components

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon as Icon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText as Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.*
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.EpisodeItem
import org.ensodai.avalonmediacard.presentation.overlay.TvModalSurface
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TvEpisodeActionsDialog(
    targetEpisode: EpisodeItem?,
    callerFocusRequester: FocusRequester? = null,
    onDismiss: () -> Unit,
    onAction: (Action) -> Unit
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes
    val typography = AvalonTheme.typography

    TvModalSurface(
        isOpen = targetEpisode != null,
        onDismissRequest = onDismiss,
        callerFocusRequester = callerFocusRequester,
        scrimColor = colors.bgPureCinema.copy(alpha = 0.72f)
    ) {
        if (targetEpisode == null) return@TvModalSurface

        Column(
            modifier = Modifier
                .width(380.dp)
                .clip(shapes.container)
                .background(colors.bgOverlay)
                .border(1.dp, colors.borderOverlay, shapes.container)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${targetEpisode.episodeNumber}. ${targetEpisode.name}",
                style = typography.cardTitle,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 1. Play Action Button
            TvActionItem(
                icon = Lucide.Play,
                label = stringResource(Res.string.details_episodes_play),
                iconTint = colors.accentPrimary,
                onClick = {
                    targetEpisode.playAction?.let(onAction)
                    onDismiss()
                }
            )

            // 2. Toggle Watch Action Button
            TvActionItem(
                icon = if (targetEpisode.isWatched) Lucide.EyeOff else Lucide.CheckCheck,
                label = if (targetEpisode.isWatched) stringResource(Res.string.details_episodes_unmark_watched) else stringResource(Res.string.details_episodes_mark_watched),
                iconTint = if (targetEpisode.isWatched) colors.textSecondary else colors.statusLive,
                onClick = {
                    targetEpisode.toggleWatchedAction?.let(onAction)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
internal fun TvActionItem(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes
    val typography = AvalonTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .tvAndWebHoverEffect(
                scaleTarget = 1.02f,
                activeBorderWidth = 1.5.dp,
                activeBorderColor = colors.accentPrimary,
                defaultBorderWidth = 1.dp,
                defaultBorderColor = colors.borderSubtle,
                shape = shapes.control,
                tiltEnabled = false,
                onClick = onClick
            )
            .background(colors.bgElevated, shapes.control)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = typography.uiLabel,
            color = colors.textPrimary
        )
    }
}
