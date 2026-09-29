package org.ensodai.avalonmediacard.presentation.screens.player.component.pc.participants

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.Play
import org.ensodai.avalonmediacard.contract.model.WatchParticipantIntent
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import org.ensodai.avalonmediacard.contract.model.WatchRoomPlaybackState
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.toLabelRes
import org.jetbrains.compose.resources.stringResource

/**
 * Элемент списка участников совместного просмотра с аватаром, статусом онлайн и состоянием воспроизведения.
 */
@Composable
fun ParticipantPlayerItem(
    participant: WatchRoomParticipantDto,
    isCurrentUser: Boolean,
    isLocalBuffering: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isHost = participant.role == WatchRoomParticipantRole.HOST
    val isOnline = participant.isOnline
    val alpha = if (isOnline) 1f else 0.4f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = if (isOnline) 0.05f else 0.02f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Аватарка
        Box(
            modifier = Modifier.size(34.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !isOnline -> Color.White.copy(alpha = 0.08f)
                            isHost -> Color(0xFF64B5F6).copy(alpha = 0.25f)
                            else -> Color.White.copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = participant.username.take(1).uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = alpha)
                )
            }

            // Индикатор онлайн
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(
                        if (isOnline) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.3f)
                    )
                    .border(1.5.dp, Color.Black, CircleShape)
            )
        }

        // Инфо об участнике
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = participant.username,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (isCurrentUser) {
                    Text(
                        text = "(${stringResource(Res.string.watch_party_you)})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64B5F6)
                    )
                }

                if (isHost) {
                    Text(
                        text = "👑",
                        fontSize = 11.sp
                    )
                }
            }

            // Статус воспроизведения (мгновенный отклик для текущего пользователя)
            val effectiveState = if (isCurrentUser && isLocalBuffering && isOnline) {
                WatchRoomPlaybackState.BUFFERING
            } else {
                participant.playbackState
            }
            ParticipantPlaybackStatusRow(
                state = effectiveState,
                isOnline = isOnline
            )

            // Настроение / Интент (если указан отличный от дефолтного)
            if (participant.intent != WatchParticipantIntent.WATCHING_ATTENTIVELY) {
                val intentLabel = stringResource(participant.intent.toLabelRes())
                Text(
                    text = intentLabel,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = if (isOnline) 0.5f else 0.25f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ParticipantPlaybackStatusRow(
    state: WatchRoomPlaybackState,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isOnline || state == WatchRoomPlaybackState.OFFLINE) {
        Text(
            text = stringResource(Res.string.watch_party_participant_offline),
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.35f),
            modifier = modifier
        )
        return
    }

    when (state) {
        WatchRoomPlaybackState.BUFFERING -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(11.dp),
                    strokeWidth = 1.5.dp,
                    color = Color(0xFFFFB74D)
                )
                Text(
                    text = stringResource(Res.string.watch_party_status_buffering),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFFB74D)
                )
            }
        }

        WatchRoomPlaybackState.PLAYING -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Lucide.Play,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = stringResource(Res.string.watch_party_status_playing),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4CAF50)
                )
            }
        }

        WatchRoomPlaybackState.PAUSED -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Lucide.Pause,
                    contentDescription = null,
                    tint = Color(0xFF90CAF9),
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = stringResource(Res.string.watch_party_status_paused),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF90CAF9)
                )
            }
        }

        WatchRoomPlaybackState.READY -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Lucide.Check,
                    contentDescription = null,
                    tint = Color(0xFF81C784),
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = stringResource(Res.string.watch_party_status_ready),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF81C784)
                )
            }
        }

        WatchRoomPlaybackState.OFFLINE -> {
            Text(
                text = stringResource(Res.string.watch_party_participant_offline),
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.35f),
                modifier = modifier
            )
        }
    }
}
