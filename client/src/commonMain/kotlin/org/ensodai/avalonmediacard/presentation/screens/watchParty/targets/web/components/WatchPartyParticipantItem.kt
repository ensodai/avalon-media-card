package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun WatchPartyParticipantItem(
    participant: WatchRoomParticipantDto,
    modifier: Modifier = Modifier
) {
    val isHost = participant.role == WatchRoomParticipantRole.HOST
    val intentLabel = stringResource(participant.intent.toLabelRes())

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Аватарка-буква
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isHost) Color(0xFF6C63FF) else Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = participant.username.take(1).uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
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
                        color = Color.White
                    )
                    if (isHost) {
                        Text(
                            text = "👑 " + stringResource(Res.string.watch_party_host_badge),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }
                Text(
                    text = intentLabel,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
        }

        // Индикатор готовности
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (participant.isReady) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (participant.isReady) stringResource(Res.string.watch_party_ready_status_ready) else stringResource(Res.string.watch_party_ready_status_not_ready),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (participant.isReady) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.45f)
            )
        }
    }
}
