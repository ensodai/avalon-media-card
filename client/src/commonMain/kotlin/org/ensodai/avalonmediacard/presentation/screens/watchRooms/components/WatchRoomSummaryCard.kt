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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import org.ensodai.avalonmediacard.contract.model.WatchRoomPhase
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .tvAndWebHoverEffect(
                scaleTarget = 1.02f,
                defaultBorderWidth = 1.dp,
                defaultBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                activeBorderWidth = 2.dp,
                activeBorderColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(14.dp),
                tiltEnabled = false,
                onClick = onClick
            )
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
    ) {
        // 1. Подложка арта (Backdrop)
        if (!room.backdropUrl.isNullOrBlank()) {
            ShimmerImage(
                model = room.backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxSize()
            )

            // Светофильтр для идеальной читаемости текста и бейджей
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.45f),
                                0.30f to Color.Black.copy(alpha = 0.15f),
                                0.55f to Color.Black.copy(alpha = 0.60f),
                                1.0f to Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                            )
                        )
                    )
            )
        }

        // 2. Контент карточки
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Верхняя строка: Бейдж активного статуса воспроизведения (слева) + Хост (справа)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Левая группа: Статус (только активные фазы LIVE / Пауза / Буферизация)
                when {
                    isLive -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.25f))
                                .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.60f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                )
                                val timeStr = if (room.lastPositionSeconds > 0L) {
                                    " • ${formatTimeSeconds(room.lastPositionSeconds)}"
                                } else ""
                                Text(
                                    text = "${stringResource(Res.string.watch_party_live_badge)}$timeStr",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF81C784)
                                )
                            }
                        }
                    }
                    isPaused -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2196F3).copy(alpha = 0.25f))
                                .border(1.dp, Color(0xFF2196F3).copy(alpha = 0.60f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            val pauseLabel = if (room.lastPositionSeconds > 0L) {
                                stringResource(Res.string.watch_party_paused_at, formatTimeSeconds(room.lastPositionSeconds))
                            } else {
                                stringResource(Res.string.watch_party_status_paused)
                            }
                            Text(
                                text = pauseLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF90CAF9)
                            )
                        }
                    }
                    isBuffering -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFF9800).copy(alpha = 0.25f))
                                .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.60f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.watch_party_status_buffering),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFFB74D)
                            )
                        }
                    }
                    else -> {
                        // В лобби — не выводим серую плашку-заплатку, чтобы не засорять артворк
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                }

                // Правая группа: Бейдж создателя (Хост)
                if (room.isHost) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.50f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.watch_party_host_badge),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Нижний блок: Заголовок + Метаданные + Социальная строка
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Название комнаты
                Text(
                    text = room.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Подзаголовок: Тайтл фильма/сериала + сезон/эпизод
                val episodeInfo = if (room.currentSeason != null && room.currentEpisode != null) {
                    "S${room.currentSeason.toString().padStart(2, '0')}E${room.currentEpisode.toString().padStart(2, '0')}"
                } else null

                val subtitleText = buildString {
                    if (!room.mediaTitle.isNullOrBlank() && !room.mediaTitle.equals(room.title, ignoreCase = true)) {
                        append(room.mediaTitle)
                        if (episodeInfo != null) append(" • ")
                    }
                    if (episodeInfo != null) {
                        append(episodeInfo)
                    } else if (room.mediaType == MediaType.TV) {
                        if (isNotEmpty()) append(" • ")
                        append("Сериал")
                    }
                }

                if (subtitleText.isNotBlank()) {
                    Text(
                        text = subtitleText,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Строка социальных активностей (участники, онлайн, вайб)
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                        .background(Color.White.copy(alpha = 0.20f))
                                        .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+$remaining",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Счетчик участников
                    val hasOnline = room.onlineParticipantsCount > 0
                    val (participantsLabel, labelColor) = when {
                        isLive && hasOnline -> {
                            "● " + stringResource(Res.string.watch_party_online_watching, room.onlineParticipantsCount) to Color(0xFF81C784)
                        }
                        hasOnline -> {
                            "● " + stringResource(Res.string.watch_party_participants_count, room.onlineParticipantsCount) to MaterialTheme.colorScheme.primary
                        }
                        else -> {
                            formatParticipantsCount(room.participantsCount) to Color.White.copy(alpha = 0.65f)
                        }
                    }

                    Text(
                        text = participantsLabel,
                        fontSize = 12.sp,
                        fontWeight = if (hasOnline) FontWeight.Medium else FontWeight.Normal,
                        color = labelColor
                    )
                }
            }
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
                        !isOnline -> Color.White.copy(alpha = 0.10f)
                        isHost -> Color(0xFF64B5F6).copy(alpha = 0.35f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
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
                    !isOnline -> Color.White.copy(alpha = 0.4f)
                    isHost -> Color(0xFF90CAF9)
                    else -> Color.White
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

@Composable
private fun formatParticipantsCount(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    val res = when {
        mod100 in 11..19 -> Res.string.watch_party_participants_total_many
        mod10 == 1 -> Res.string.watch_party_participants_total_one
        mod10 in 2..4 -> Res.string.watch_party_participants_total_few
        else -> Res.string.watch_party_participants_total_many
    }
    return stringResource(res, count)
}
