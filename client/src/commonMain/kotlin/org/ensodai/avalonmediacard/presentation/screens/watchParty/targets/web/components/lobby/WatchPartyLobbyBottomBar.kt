package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import org.ensodai.avalonmediacard.designsystem.components.AvalonCircularProgressIndicator as CircularProgressIndicator
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon as Icon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText as Text
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchPartyLobbyBottomBar(
    isHost: Boolean,
    isStartingPlayback: Boolean,
    readyCount: Int,
    totalCount: Int,
    onStartPlayback: () -> Unit,
    onLeaveRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes
    val elevation = AvalonTheme.elevation
    val typography = AvalonTheme.typography

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Кнопка "Покинуть"
        val leaveShape = shapes.control
        Box(
            modifier = Modifier
                .height(48.dp)
                .tvAndWebHoverEffect(
                    scaleTarget = 1.02f,
                    shape = leaveShape,
                    activeBorderColor = colors.borderFocus,
                    onClick = onLeaveRoom
                )
                .background(colors.bgElevated, leaveShape)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(Res.string.watch_party_btn_leave),
                style = typography.uiLabel,
                color = colors.textSecondary
            )
        }

        // Главная кнопка старта для хоста
        if (isHost) {
            val startShape = shapes.control
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .tvAndWebHoverEffect(
                        scaleTarget = 1.02f,
                        shape = startShape,
                        activeBorderColor = colors.borderFocus,
                        onClick = onStartPlayback
                    )
                    .background(colors.textPrimary, startShape),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isStartingPlayback) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = colors.onAccentPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Lucide.Play,
                        contentDescription = null,
                        tint = colors.onAccentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (totalCount > 0) {
                            "${stringResource(Res.string.watch_party_btn_start_playback)} ($readyCount/$totalCount)"
                        } else {
                            stringResource(Res.string.watch_party_btn_start_playback)
                        },
                        style = typography.uiLabel,
                        color = colors.onAccentPrimary
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(shapes.control)
                    .background(colors.bgElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (totalCount > 0) {
                        "${stringResource(Res.string.watch_party_waiting_host)} ($readyCount/$totalCount)"
                    } else {
                        stringResource(Res.string.watch_party_waiting_host)
                    },
                    style = typography.uiLabel,
                    color = colors.textSecondary
                )
            }
        }
    }
}
