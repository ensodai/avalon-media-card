package org.ensodai.avalonmediacard.presentation.screens.player.component.pc.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.player.viewState.PlayerChatUiMessage
import org.jetbrains.compose.resources.stringResource

/**
 * Элемент сообщения внутриплеерного чата.
 */
@Composable
fun WatchPartyChatMessageItem(
    message: PlayerChatUiMessage,
    onTimecodeClicked: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val initial = message.senderUsername.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val avatarBg = if (message.isFromMe) {
        Color.White.copy(alpha = 0.20f)
    } else if (message.isHost) {
        Color(0xFFD97706)
    } else {
        Color(0xFF374151)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (message.isFromMe) Color.White.copy(alpha = 0.04f) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Аватар / Инициалы
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Контентная часть (Автор, бейдж, таймкод и текст сообщения)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    val authorName = if (message.isFromMe) {
                        if (message.senderUsername.isNotBlank()) {
                            stringResource(Res.string.watch_party_chat_you_fmt, message.senderUsername)
                        } else {
                            stringResource(Res.string.watch_party_you)
                        }
                    } else {
                        message.senderUsername
                    }
                    Text(
                        text = authorName,
                        color = if (message.isHost) Color(0xFFFBBF24) else Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (message.isFromMe || message.isHost) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (message.isHost) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFD97706).copy(alpha = 0.35f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.watch_party_host_badge),
                                color = Color(0xFFFDE68A),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Кликабельный бейдж таймкода с TV/Web hover эффектом
                Box(
                    modifier = Modifier
                        .tvAndWebHoverEffect(
                            scaleTarget = 1.08f,
                            shape = RoundedCornerShape(6.dp),
                            activeBorderColor = Color.White.copy(alpha = 0.30f),
                            activeBorderWidth = 1.dp,
                            onClick = { onTimecodeClicked(message.rawPositionMs) }
                        )
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message.formattedPosition,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Текст сообщения
            Text(
                text = message.text,
                color = Color.White.copy(alpha = if (message.isSending) 0.55f else 0.95f),
                fontSize = 13.sp,
                lineHeight = 17.sp
            )

            if (message.isSending) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(Res.string.watch_party_chat_sending),
                    color = Color.White.copy(alpha = 0.40f),
                    fontSize = 10.sp
                )
            }
        }
    }
}
