package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.tv.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.*
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.EpisodeItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TvEpisodeActionsDialog(
    targetEpisode: EpisodeItem?,
    onDismiss: () -> Unit,
    onAction: (Action) -> Unit
) {
    if (targetEpisode == null) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(380.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF141418))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .clickable(enabled = false) {}
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${targetEpisode.episodeNumber}. ${targetEpisode.name}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 1. Play Action Button
                TvActionItem(
                    icon = Lucide.Play,
                    label = stringResource(Res.string.details_episodes_play),
                    iconTint = Color.White,
                    onClick = {
                        targetEpisode.playAction?.let(onAction)
                        onDismiss()
                    }
                )

                // 2. Toggle Watch Action Button
                TvActionItem(
                    icon = if (targetEpisode.isWatched) Lucide.EyeOff else Lucide.CheckCheck,
                    label = if (targetEpisode.isWatched) stringResource(Res.string.details_episodes_unmark_watched) else stringResource(Res.string.details_episodes_mark_watched),
                    iconTint = if (targetEpisode.isWatched) Color.White.copy(alpha = 0.7f) else Color(0xFF4CAF50),
                    onClick = {
                        targetEpisode.toggleWatchedAction?.let(onAction)
                        onDismiss()
                    }
                )
            }
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .tvAndWebHoverEffect(
                scaleTarget = 1.02f,
                activeBorderWidth = 1.5.dp,
                activeBorderColor = Color.White,
                defaultBorderWidth = 1.dp,
                defaultBorderColor = Color.Transparent,
                shape = RoundedCornerShape(10.dp),
                tiltEnabled = false,
                onClick = onClick
            )
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
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
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
