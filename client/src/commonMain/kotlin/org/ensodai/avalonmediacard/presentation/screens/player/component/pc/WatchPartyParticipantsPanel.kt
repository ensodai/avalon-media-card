package org.ensodai.avalonmediacard.presentation.screens.player.component.pc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Users
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantDto
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.player.component.pc.participants.ParticipantPlayerItem
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchPartyParticipantsPanel(
    participants: List<WatchRoomParticipantDto>,
    currentUserId: String?,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    isLocalBuffering: Boolean = false,
    modifier: Modifier = Modifier
) {
    val onlineCount = remember(participants) { participants.count { it.isOnline } }
    val panelShape = if (isExpanded) RoundedCornerShape(20.dp) else RoundedCornerShape(22.dp)
    val panelBg = if (isExpanded) Color.Black.copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.65f)
    val panelBorderColor = if (isExpanded) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .widthIn(max = 280.dp)
            .animateContentSize(
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            )
            .clip(panelShape)
            .background(panelBg)
            .border(1.dp, panelBorderColor, panelShape)
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            val headerShape = if (isExpanded) {
                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            } else {
                RoundedCornerShape(22.dp)
            }

            // Кликабельная шапка: в свернутом виде — компактная таблетка [👥 2], в развернутом — полная шапка списка
            Row(
                modifier = Modifier
                    .then(
                        if (isExpanded) Modifier.width(280.dp) else Modifier.wrapContentWidth()
                    )
                    .tvAndWebHoverEffect(
                        scaleTarget = if (isExpanded) 1.0f else 1.08f,
                        shape = headerShape,
                        activeBorderColor = MaterialTheme.colorScheme.primary,
                        activeBorderWidth = if (isExpanded) 1.5.dp else 2.dp,
                        defaultBorderColor = Color.Transparent,
                        defaultBorderWidth = 0.dp,
                        onClick = onToggleExpanded
                    )
                    .pointerHoverIcon(PointerIcon.Hand, overrideDescendants = true)
                    .padding(
                        horizontal = 14.dp,
                        vertical = if (isExpanded) 10.dp else 9.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isExpanded) Arrangement.SpaceBetween else Arrangement.spacedBy(6.dp)
            ) {
                if (isExpanded) {
                    // Развернутый вид: иконка в кружочке + заголовок + бейдж
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF64B5F6).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Users,
                                contentDescription = null,
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(13.dp)
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

                    // Шеврон сворачивания
                    Icon(
                        imageVector = Lucide.ChevronUp,
                        contentDescription = stringResource(Res.string.common_collapse),
                        tint = Color.White.copy(alpha = 0.60f),
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    // Свернутый вид: таблетка в точности как стиль островка PlayerIslandDynamicButton
                    Icon(
                        imageVector = Lucide.Users,
                        contentDescription = stringResource(Res.string.watch_party_participants_header),
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(17.dp)
                    )
                    if (onlineCount > 0) {
                        Text(
                            text = onlineCount.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64B5F6)
                        )
                    }
                }
            }

            // Выпадающий список участников при развернутом состоянии
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.width(280.dp)) {
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
