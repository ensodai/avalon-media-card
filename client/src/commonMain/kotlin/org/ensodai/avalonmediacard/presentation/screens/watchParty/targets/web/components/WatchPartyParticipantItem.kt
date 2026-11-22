package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.contract.model.WatchParticipantIntent
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

fun WatchParticipantIntent.toLabelRes(): StringResource = when (this) {
    WatchParticipantIntent.WATCHING_ATTENTIVELY -> Res.string.watch_party_intent_attentively
    WatchParticipantIntent.BACKGROUND_LISTENING -> Res.string.watch_party_intent_background
    WatchParticipantIntent.AWAY_FOR_SNACKS -> Res.string.watch_party_intent_snacks
    WatchParticipantIntent.SILENT_NO_PAUSES -> Res.string.watch_party_intent_silent
    WatchParticipantIntent.ACTIVE_DISCUSSION -> Res.string.watch_party_intent_discussion
    WatchParticipantIntent.CHILLING -> Res.string.watch_party_intent_chilling
}

fun intentToEmoji(intent: WatchParticipantIntent): String = when (intent) {
    WatchParticipantIntent.WATCHING_ATTENTIVELY -> "🍿"
    WatchParticipantIntent.CHILLING -> "🛋️"
    WatchParticipantIntent.AWAY_FOR_SNACKS -> "☕"
    WatchParticipantIntent.SILENT_NO_PAUSES -> "🤫"
    WatchParticipantIntent.ACTIVE_DISCUSSION -> "💬"
    WatchParticipantIntent.BACKGROUND_LISTENING -> "🎧"
}

private class ParticipantVisualState(
    val rowAlpha: Float,
    val rowBackground: Color,
    val avatarBackground: Color,
    val avatarTextColor: Color,
    val isOnlineIndicatorVisible: Boolean,
    val readyBadgeBackground: Color,
    val readyBadgeTextColor: Color
)

@Composable
private fun rememberParticipantVisualState(
    isOnline: Boolean,
    isHost: Boolean,
    isReady: Boolean
): ParticipantVisualState {
    val colors = AvalonTheme.colors

    return remember(isOnline, isHost, isReady, colors) {
        val rowAlpha = if (isOnline) 1f else 0.5f
        val avatarBg = when {
            !isOnline -> colors.bgElevated
            isHost -> colors.accentPrimary
            else -> colors.bgSurface
        }
        val readyBg = if (isReady) colors.statusLive.copy(alpha = 0.15f) else colors.bgElevated
        val readyText = if (isReady) colors.statusLive else colors.textSecondary

        ParticipantVisualState(
            rowAlpha = rowAlpha,
            rowBackground = colors.bgElevated.copy(alpha = if (isOnline) 0.6f else 0.3f),
            avatarBackground = avatarBg,
            avatarTextColor = colors.textPrimary.copy(alpha = rowAlpha),
            isOnlineIndicatorVisible = isOnline,
            readyBadgeBackground = readyBg,
            readyBadgeTextColor = readyText
        )
    }
}

@Composable
fun WatchPartyParticipantItem(
    participant: WatchRoomParticipantDto,
    modifier: Modifier = Modifier
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes
    val typography = AvalonTheme.typography

    val isHost = participant.role == WatchRoomParticipantRole.HOST
    val isOnline = participant.isOnline
    val isReady = participant.isReady
    val visualState = rememberParticipantVisualState(isOnline = isOnline, isHost = isHost, isReady = isReady)
    val intentLabel = stringResource(participant.intent.toLabelRes())

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.control)
            .background(visualState.rowBackground)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Аватарка с индикатором онлайн
            Box(
                modifier = Modifier.size(30.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(visualState.avatarBackground),
                    contentAlignment = Alignment.Center
                ) {
                    AvalonText(
                        text = participant.username.take(1).uppercase(),
                        style = typography.uiLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = visualState.avatarTextColor
                    )
                }

                // Индикатор "В сети"
                if (visualState.isOnlineIndicatorVisible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.statusLive)
                    )
                }
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AvalonText(
                        text = participant.username,
                        style = typography.uiLabel,
                        color = colors.textPrimary.copy(alpha = visualState.rowAlpha)
                    )
                    if (isHost) {
                        AvalonText(
                            text = "👑 " + stringResource(Res.string.watch_party_host_badge),
                            style = typography.microCaption,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentGold
                        )
                    }
                    if (!isOnline) {
                        AvalonText(
                            text = "• " + stringResource(Res.string.watch_party_participant_offline),
                            style = typography.microCaption,
                            color = colors.textMuted
                        )
                    }
                }
                AvalonText(
                    text = if (isReady) {
                        "${intentToEmoji(participant.intent)} $intentLabel"
                    } else {
                        stringResource(Res.string.watch_party_participant_waiting)
                    },
                    style = typography.metadata,
                    fontSize = 11.sp,
                    color = if (isOnline) colors.textSecondary else colors.textMuted
                )
            }
        }

        // Индикатор готовности
        Box(
            modifier = Modifier
                .clip(shapes.micro)
                .background(visualState.readyBadgeBackground)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            AvalonText(
                text = if (isReady) stringResource(Res.string.watch_party_ready_status_ready) else stringResource(Res.string.watch_party_ready_status_not_ready),
                style = typography.microCaption,
                color = visualState.readyBadgeTextColor
            )
        }
    }
}
