package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import org.ensodai.avalonmediacard.contract.model.WatchRoomControlMode
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.WatchPartyActions
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyViewState
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchPartySetupStep(
    state: WatchPartyViewState,
    actions: WatchPartyActions,
    modifier: Modifier = Modifier
) {
    val defaultTitle = if (state.mediaTitle.isNotBlank()) {
        stringResource(Res.string.watch_party_title_format, state.mediaTitle)
    } else {
        stringResource(Res.string.watch_party_default_room_title)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Поле названия
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(Res.string.watch_party_room_title_label),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.75f)
            )
            BasicTextField(
                value = state.roomTitleInput,
                onValueChange = actions.onRoomTitleChanged,
                textStyle = TextStyle(fontSize = 14.sp, color = Color.White),
                cursorBrush = SolidColor(Color(0xFF6C63FF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                decorationBox = { innerTextField ->
                    if (state.roomTitleInput.isEmpty()) {
                        Text(
                            text = defaultTitle,
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            )
        }

        // Режим управления
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(Res.string.watch_party_control_mode_label),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.75f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val isHostOnly = state.controlMode == WatchRoomControlMode.HOST_ONLY
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isHostOnly) Color(0xFF6C63FF).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                        .border(
                            1.dp,
                            if (isHostOnly) Color(0xFF6C63FF) else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { actions.onControlModeChanged(WatchRoomControlMode.HOST_ONLY) }
                        .padding(vertical = 12.dp, horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "👑 " + stringResource(Res.string.watch_party_control_mode_host),
                        fontSize = 13.sp,
                        fontWeight = if (isHostOnly) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White
                    )
                }

                val isDemocratic = state.controlMode == WatchRoomControlMode.DEMOCRATIC
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDemocratic) Color(0xFF6C63FF).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                        .border(
                            1.dp,
                            if (isDemocratic) Color(0xFF6C63FF) else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { actions.onControlModeChanged(WatchRoomControlMode.DEMOCRATIC) }
                        .padding(vertical = 12.dp, horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🗳️ " + stringResource(Res.string.watch_party_control_mode_democratic),
                        fontSize = 13.sp,
                        fontWeight = if (isDemocratic) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White
                    )
                }
            }
        }

        // Блок выбора и проверки источника
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(Res.string.watch_party_source_label),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            if (state.selectedSourceId == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(Res.string.watch_party_source_select_hint),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Row(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (state.isSelectingSource) Color(0xFF6C63FF) else Color.White.copy(alpha = 0.12f))
                            .clickable {
                                actions.onToggleSelectSource(!state.isSelectingSource)
                            }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.isSelectingSource) {
                                stringResource(Res.string.watch_party_source_close_selector)
                            } else {
                                stringResource(Res.string.watch_party_source_select_hint)
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.selectedSourceName ?: stringResource(Res.string.watch_party_source_selected),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        state.selectedSourceType?.let { sType ->
                            Text(
                                text = sType.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6C63FF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Кнопка или бейдж проверки источника
                        if (state.isSourceVerified) {
                            Row(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                                    .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .clickable { actions.onTestSource() }
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(Res.string.watch_party_source_verified),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFF9800).copy(alpha = 0.2f))
                                    .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .clickable { actions.onTestSource() }
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Play,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB74D),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = stringResource(Res.string.watch_party_source_test),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFFB74D)
                                )
                            }
                        }

                        // Кнопка изменить / закрыть селектор
                        Row(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (state.isSelectingSource) Color(0xFF6C63FF).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
                                .clickable {
                                    actions.onToggleSelectSource(!state.isSelectingSource)
                                }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (state.isSelectingSource) {
                                    stringResource(Res.string.watch_party_source_close_selector)
                                } else {
                                    stringResource(Res.string.watch_party_source_change)
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        if (state.createError != null) {
            Text(
                text = state.createError,
                fontSize = 12.sp,
                color = Color(0xFFFF5252)
            )
        }

        // Кнопка создания
        val canCreate = state.selectedSourceId != null && !state.isCreatingRoom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .tvAndWebHoverEffect(
                    scaleTarget = 1.02f,
                    shape = RoundedCornerShape(14.dp),
                    onClick = { if (canCreate) actions.onCreateRoom() }
                )
                .background(
                    if (canCreate) Color(0xFF6C63FF) else Color.White.copy(alpha = 0.15f),
                    RoundedCornerShape(14.dp)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (state.isCreatingRoom) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(Res.string.watch_party_btn_submit_create),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canCreate) Color.White else Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}
