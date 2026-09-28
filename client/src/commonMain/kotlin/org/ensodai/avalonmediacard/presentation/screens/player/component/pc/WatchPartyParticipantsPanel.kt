package org.ensodai.avalonmediacard.presentation.screens.player.component.pc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.*
import org.ensodai.avalonmediacard.contract.model.WatchParticipantIntent
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import org.ensodai.avalonmediacard.contract.model.WatchRoomPlaybackState
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.toLabelRes
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchPartyParticipantsPanel(
    participants: List<WatchRoomParticipantDto>,
    currentUserId: String?,
    onCloseClick: () -> Unit = {},
    isLocalBuffering: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    val onlineCount = remember(participants) { participants.count { it.isOnline } }

    Box(
        modifier = modifier
            .width(280.dp)
            .animateContentSize()
            .background(Color.Black.copy(alpha = 0.70f), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.width(280.dp)) {
            val headerShape = if (isExpanded) {
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            } else {
                RoundedCornerShape(20.dp)
            }

            // Кликабельная шапка с TV/Web эффектом: по клику на нее список участников сворачивается или разворачивается
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(headerShape)
                    .tvAndWebHoverEffect(
                        scaleTarget = 1.03f,
                        shape = headerShape,
                        activeBorderColor = MaterialTheme.colorScheme.primary,
                        activeBorderWidth = 1.5.dp,
                        defaultBorderColor = Color.Transparent,
                        defaultBorderWidth = 0.dp,
                        onClick = { isExpanded = !isExpanded }
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF64B5F6).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Users,
                            contentDescription = null,
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = stringResource(Res.string.watch_party_participants_header),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    // Бейдж количества онлайн
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF4CAF50).copy(alpha = 0.20f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = onlineCount.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784)
                        )
                    }
                }

                // Иконка сворачивания / разворачивания
                Icon(
                    imageVector = if (isExpanded) Lucide.ChevronUp else Lucide.ChevronDown,
                    contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        thickness = 1.dp
                    )

                    if (participants.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.watch_rooms_history_empty),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.45f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(
                                items = participants,
                                key = { it.userId.toString() }
                            ) { participant ->
                                val isCurrentUser = participant.userId.toString() == currentUserId
                                ParticipantPlayerItem(
                                    participant = participant,
                                    isCurrentUser = isCurrentUser,
                                    isLocalBuffering = isLocalBuffering
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantPlayerItem(
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
private fun ParticipantPlaybackStatusRow(
    state: WatchRoomPlaybackState,
    isOnline: Boolean
) {
    if (!isOnline || state == WatchRoomPlaybackState.OFFLINE) {
        Text(
            text = stringResource(Res.string.watch_party_participant_offline),
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.35f)
        )
        return
    }

    when (state) {
        WatchRoomPlaybackState.BUFFERING -> {
            Row(
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
                color = Color.White.copy(alpha = 0.35f)
            )
        }
    }
}
