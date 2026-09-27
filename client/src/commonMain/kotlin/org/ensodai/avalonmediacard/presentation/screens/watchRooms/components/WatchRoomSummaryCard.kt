package org.ensodai.avalonmediacard.presentation.screens.watchRooms.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import org.ensodai.avalonmediacard.contract.model.WatchRoomPhase
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonButton
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchRoomSummaryCard(
    room: WatchRoomSummaryDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLive = room.phase == WatchRoomPhase.PLAYING_IN_SYNC || room.phase == WatchRoomPhase.STARTING_SCHEDULED
    val isBuffering = room.phase == WatchRoomPhase.PREPARING || room.phase == WatchRoomPhase.PARTIAL_BUFFERING
    val isPaused = room.phase == WatchRoomPhase.FORCE_PAUSED

    val borderColor = when {
        isLive -> Color(0xFF4CAF50).copy(alpha = 0.35f)
        isBuffering -> Color(0xFFFF9800).copy(alpha = 0.35f)
        isPaused -> Color(0xFF2196F3).copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
    }

    val backgroundColor = when {
        isLive -> Color(0xFF4CAF50).copy(alpha = 0.08f)
        isBuffering -> Color(0xFFFF9800).copy(alpha = 0.06f)
        isPaused -> Color(0xFF2196F3).copy(alpha = 0.06f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .tvAndWebHoverEffect(scaleTarget = 1.005f, shape = RoundedCornerShape(10.dp), onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = room.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (room.isHost) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.watch_party_host_badge),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    when (room.phase) {
                        WatchRoomPhase.PLAYING_IN_SYNC -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                                    .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50))
                                    )
                                    Text(
                                        text = stringResource(Res.string.watch_party_live_badge),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }
                        }
                        WatchRoomPhase.STARTING_SCHEDULED -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(Res.string.watch_party_btn_starting),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF81C784)
                                )
                            }
                        }
                        WatchRoomPhase.PREPARING, WatchRoomPhase.PARTIAL_BUFFERING -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFF9800).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(Res.string.watch_party_status_buffering),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFFB74D)
                                )
                            }
                        }
                        WatchRoomPhase.FORCE_PAUSED -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF2196F3).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (room.lastPositionSeconds > 0L) {
                                        stringResource(Res.string.watch_party_paused_at, formatTimeSeconds(room.lastPositionSeconds))
                                    } else {
                                        stringResource(Res.string.watch_party_status_paused)
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF90CAF9)
                                )
                            }
                        }
                        WatchRoomPhase.LOBBY -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(Res.string.watch_party_in_lobby_badge),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PIN: ${room.joinPin}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val episodeInfo = if (room.currentSeason != null && room.currentEpisode != null) {
                        "S${room.currentSeason} E${room.currentEpisode}"
                    } else null

                    if (episodeInfo != null) {
                        Text(
                            text = episodeInfo,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    if (isLive && room.lastPositionSeconds > 0L) {
                        Text(
                            text = formatTimeSeconds(room.lastPositionSeconds),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4CAF50)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (room.participants.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy((-6).dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val displayed = room.participants.take(4)
                                val remaining = room.participants.size - displayed.size
                                displayed.forEach { participant ->
                                    ParticipantMiniAvatar(participant = participant)
                                }
                                if (remaining > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+$remaining",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        val participantsLabel = if (isLive && room.onlineParticipantsCount > 0) {
                            stringResource(Res.string.watch_party_online_watching, room.onlineParticipantsCount)
                        } else if (room.onlineParticipantsCount > 0) {
                            stringResource(Res.string.watch_party_online_in_lobby, room.onlineParticipantsCount)
                        } else {
                            stringResource(Res.string.watch_party_participants_label, room.participantsCount)
                        }

                        Text(
                            text = participantsLabel,
                            fontSize = 12.sp,
                            color = if (isLive) Color(0xFF81C784) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            AvalonButton(
                text = if (isLive) stringResource(Res.string.watch_party_btn_join_playback)
                else stringResource(Res.string.watch_rooms_enter_room),
                onClick = onClick
            )
        }
    }
}

@Composable
private fun ParticipantMiniAvatar(
    participant: WatchRoomParticipantDto,
    modifier: Modifier = Modifier
) {
    val isHost = participant.role == WatchRoomParticipantRole.HOST
    val isOnline = participant.isOnline

    Box(
        modifier = modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        !isOnline -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                        isHost -> Color(0xFF64B5F6).copy(alpha = 0.35f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    }
                )
                .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = participant.username.take(1).uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    !isOnline -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    isHost -> Color(0xFF90CAF9)
                    else -> MaterialTheme.colorScheme.onBackground
                }
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isOnline) Color(0xFF4CAF50) else Color.Gray.copy(alpha = 0.6f))
                .border(1.dp, MaterialTheme.colorScheme.background, CircleShape)
        )
    }
}

private fun formatTimeSeconds(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        "${hrs}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    } else {
        "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    }
}
