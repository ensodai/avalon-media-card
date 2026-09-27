package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.ArrowRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.WatchPartyActions
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyStep
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyViewState
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchPartyEntryStep(
    state: WatchPartyViewState,
    actions: WatchPartyActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Кнопка: Создать новую комнату
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .tvAndWebHoverEffect(
                    scaleTarget = 1.02f,
                    defaultBorderWidth = 1.dp,
                    defaultBorderColor = Color(0xFF6C63FF).copy(alpha = 0.4f),
                    activeBorderWidth = 1.dp,
                    activeBorderColor = Color(0xFF6C63FF),
                    shape = RoundedCornerShape(16.dp),
                    onClick = { actions.onSetStep(WatchPartyStep.SETUP) }
                )
                .background(Color(0xFF6C63FF).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Lucide.Plus,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(Res.string.watch_party_btn_create_room),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Icon(
                imageVector = Lucide.ArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }

        // Карточка: Войти по PIN-коду
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.watch_party_enter_pin_hint),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.75f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BasicTextField(
                    value = state.pinInput,
                    onValueChange = actions.onPinChanged,
                    textStyle = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 4.sp
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(Color(0xFF6C63FF)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    decorationBox = { innerTextField ->
                        if (state.pinInput.isEmpty()) {
                            Text(
                                text = "------",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 20.sp,
                                letterSpacing = 4.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        innerTextField()
                    }
                )

                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .tvAndWebHoverEffect(
                            scaleTarget = 1.03f,
                            shape = RoundedCornerShape(12.dp),
                            onClick = actions.onJoinByPin
                        )
                        .background(
                            if (state.pinInput.length == 6) Color.White else Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.isJoining) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.watch_party_btn_join),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.pinInput.length == 6) Color.Black else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            if (state.joinError != null) {
                Text(
                    text = state.joinError,
                    fontSize = 12.sp,
                    color = Color(0xFFFF5252)
                )
            }
        }

        // Секция: Активные комнаты тайтла
        if (state.savedRooms.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(Res.string.watch_party_active_rooms_title),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.75f)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.savedRooms) { room ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (room.isPlaying) Color(0xFF4CAF50).copy(alpha = 0.08f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    1.dp,
                                    if (room.isPlaying) Color(0xFF4CAF50).copy(alpha = 0.35f)
                                    else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { actions.onJoinById(room.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = room.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    if (room.isPlaying) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                                                .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = stringResource(Res.string.watch_party_live_badge),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4CAF50)
                                            )
                                        }
                                    }
                                }

                                val participantsLabel = if (room.isPlaying && room.onlineParticipantsCount > 0) {
                                    stringResource(Res.string.watch_party_online_watching, room.onlineParticipantsCount)
                                } else if (room.onlineParticipantsCount > 0) {
                                    stringResource(Res.string.watch_party_online_in_lobby, room.onlineParticipantsCount)
                                } else {
                                    stringResource(Res.string.watch_party_participants_label, room.participantsCount)
                                }

                                Text(
                                    text = participantsLabel,
                                    fontSize = 12.sp,
                                    color = if (room.isPlaying) Color(0xFF81C784) else Color.White.copy(alpha = 0.6f)
                                )
                            }
                            Text(
                                text = if (room.isPlaying) stringResource(Res.string.watch_party_btn_join_playback)
                                else stringResource(Res.string.watch_party_btn_join),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (room.isPlaying) Color(0xFF4CAF50) else Color(0xFF6C63FF)
                            )
                        }
                    }
                }
            }
        }
    }
}
