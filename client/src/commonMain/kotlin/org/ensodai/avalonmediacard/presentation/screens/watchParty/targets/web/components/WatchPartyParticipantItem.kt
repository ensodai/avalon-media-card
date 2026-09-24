package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val successColor = Color(0xFF4CAF50)

    return remember(isOnline, isHost, isReady, primaryColor) {
        val rowAlpha = if (isOnline) 1f else 0.5f
        val avatarBg = when {
            !isOnline -> Color.White.copy(alpha = 0.12f)
            isHost -> primaryColor
            else -> Color.White.copy(alpha = 0.2f)
        }
        val readyBg = if (isReady) successColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f)
        val readyText = if (isReady) successColor else Color.White.copy(alpha = 0.45f)

        ParticipantVisualState(
            rowAlpha = rowAlpha,
            rowBackground = Color.White.copy(alpha = if (isOnline) 0.04f else 0.02f),
            avatarBackground = avatarBg,
            avatarTextColor = Color.White.copy(alpha = rowAlpha),
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
    val isHost = participant.role == WatchRoomParticipantRole.HOST
    val isOnline = participant.isOnline
    val isReady = participant.isReady
    val visualState = rememberParticipantVisualState(isOnline = isOnline, isHost = isHost, isReady = isReady)
    val intentLabel = stringResource(participant.intent.toLabelRes())

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
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
                    Text(
                        text = participant.username.take(1).uppercase(),
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
                            .background(Color(0xFF4CAF50))
                    )
                }
            }

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = participant.username,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = visualState.rowAlpha)
                    )
                    if (isHost) {
                        Text(
                            text = "👑 " + stringResource(Res.string.watch_party_host_badge),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                    if (!isOnline) {
                        Text(
                            text = "• " + stringResource(Res.string.watch_party_participant_offline),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
                Text(
                    text = intentLabel,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = if (isOnline) 0.55f else 0.35f)
                )
            }
        }

        // Индикатор готовности
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(visualState.readyBadgeBackground)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isReady) stringResource(Res.string.watch_party_ready_status_ready) else stringResource(Res.string.watch_party_ready_status_not_ready),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = visualState.readyBadgeTextColor
            )
        }
    }
}
