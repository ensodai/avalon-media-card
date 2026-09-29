package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.tv.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.HardDrive
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.X
import org.ensodai.avalonmediacard.contract.model.WatchRoomControlMode
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.WatchPartyActions
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.lobby.formatSourceProviderTitle
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyStep
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyViewState
import org.jetbrains.compose.resources.stringResource

private val WatchPartyPurple = Color(0xFF6C63FF)

/**
 * ТВ-экран создания комнаты совместного просмотра.
 * 
 * Оптимизирован для навигации с пульта ДУ (D-Pad):
 * - Все интерактивные элементы обернуты в [tvAndWebHoverEffect] с акцентными рамками.
 * - Фокус автоматически инициализируется при открытии экрана.
 * - Поле названия поддерживает перехват клавиш вверх/вниз для плавного перехода.
 */
@Composable
fun WatchPartySetupTv(
    state: WatchPartyViewState,
    actions: WatchPartyActions,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val initialFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching {
            initialFocusRequester.requestFocus()
        }
    }

    val defaultTitle = if (state.mediaTitle.isNotBlank()) {
        stringResource(Res.string.watch_party_title_format, state.mediaTitle)
    } else {
        stringResource(Res.string.watch_party_default_room_title)
    }

    val canCreate = state.selectedSourceId != null && !state.isCreatingRoom

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Кнопка «Назад» в левом верхнем углу
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 32.dp, top = 28.dp)
                .size(48.dp)
                .tvAndWebHoverEffect(
                    scaleTarget = 1.1f,
                    shape = CircleShape,
                    activeBorderColor = Color.White,
                    onClick = { actions.onSetStep(WatchPartyStep.ENTRY) }
                )
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.ArrowLeft,
                contentDescription = stringResource(Res.string.common_close),
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp)
            )
        }

        // Центральная карточка формы
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .padding(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Шапка формы
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(Res.string.watch_party_step_setup),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    if (state.mediaTitle.isNotBlank()) {
                        Text(
                            text = state.mediaTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 1. Поле названия комнаты
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(Res.string.watch_party_room_title_label),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.75f)
                    )

                    var isFieldFocused by remember { mutableStateOf(false) }
                    val fieldBorderColor by animateColorAsState(
                        targetValue = if (isFieldFocused) WatchPartyPurple else Color.White.copy(alpha = 0.15f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .border(1.dp, fieldBorderColor, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = state.roomTitleInput,
                            onValueChange = actions.onRoomTitleChanged,
                            textStyle = TextStyle(fontSize = 14.sp, color = Color.White),
                            cursorBrush = SolidColor(WatchPartyPurple),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { isFieldFocused = it.isFocused }
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown) {
                                        when (event.key) {
                                            Key.DirectionDown, Key.Enter, Key.NumPadEnter, Key.Tab -> {
                                                focusManager.moveFocus(FocusDirection.Down)
                                                true
                                            }
                                            Key.DirectionUp -> {
                                                focusManager.moveFocus(FocusDirection.Up)
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                },
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (state.roomTitleInput.isEmpty()) {
                                        Text(
                                            text = defaultTitle,
                                            color = Color.White.copy(alpha = 0.4f),
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        if (state.roomTitleInput.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .clickable { actions.onRoomTitleChanged("") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Lucide.X,
                                    contentDescription = "Clear",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Режим управления (Только хост / Демократичный)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(Res.string.watch_party_control_mode_label),
                        fontSize = 13.sp,
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
                                .height(52.dp)
                                .tvAndWebHoverEffect(
                                    scaleTarget = 1.04f,
                                    shape = RoundedCornerShape(14.dp),
                                    activeBorderColor = WatchPartyPurple,
                                    onClick = { actions.onControlModeChanged(WatchRoomControlMode.HOST_ONLY) }
                                )
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isHostOnly) WatchPartyPurple.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f))
                                .border(
                                    1.5.dp,
                                    if (isHostOnly) WatchPartyPurple else Color.White.copy(alpha = 0.12f),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "👑 " + stringResource(Res.string.watch_party_control_mode_host),
                                fontSize = 13.sp,
                                fontWeight = if (isHostOnly) FontWeight.Bold else FontWeight.Medium,
                                color = Color.White
                            )
                        }

                        val isDemocratic = state.controlMode == WatchRoomControlMode.DEMOCRATIC
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .tvAndWebHoverEffect(
                                    scaleTarget = 1.04f,
                                    shape = RoundedCornerShape(14.dp),
                                    activeBorderColor = WatchPartyPurple,
                                    onClick = { actions.onControlModeChanged(WatchRoomControlMode.DEMOCRATIC) }
                                )
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDemocratic) WatchPartyPurple.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f))
                                .border(
                                    1.5.dp,
                                    if (isDemocratic) WatchPartyPurple else Color.White.copy(alpha = 0.12f),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🗳️ " + stringResource(Res.string.watch_party_control_mode_democratic),
                                fontSize = 13.sp,
                                fontWeight = if (isDemocratic) FontWeight.Bold else FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }

                // 3. Блок источника видеопотока
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.watch_party_source_label),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    if (state.selectedSourceId == null) {
                        // Источник еще не выбран -> большая фокусная кнопка
                        Row(
                            modifier = Modifier
                                .focusRequester(initialFocusRequester)
                                .fillMaxWidth()
                                .height(52.dp)
                                .tvAndWebHoverEffect(
                                    scaleTarget = 1.02f,
                                    shape = RoundedCornerShape(14.dp),
                                    activeBorderColor = WatchPartyPurple,
                                    onClick = { actions.onToggleSelectSource(true) }
                                )
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Lucide.HardDrive,
                                contentDescription = null,
                                tint = WatchPartyPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(Res.string.watch_party_source_select_hint),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    } else {
                        // Источник выбран -> информационная карточка + кнопки действий
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = state.selectedSourceName ?: stringResource(Res.string.watch_party_source_selected),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    state.selectedSourceType?.let { sType ->
                                        Text(
                                            text = formatSourceProviderTitle(sType) ?: sType.uppercase(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WatchPartyPurple
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Кнопка проверки
                                if (state.isSourceVerified) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .tvAndWebHoverEffect(
                                                scaleTarget = 1.03f,
                                                shape = RoundedCornerShape(12.dp),
                                                activeBorderColor = Color(0xFF4CAF50),
                                                onClick = { actions.onTestSource() }
                                            )
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                                            .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Lucide.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(Res.string.watch_party_source_verified),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .tvAndWebHoverEffect(
                                                scaleTarget = 1.03f,
                                                shape = RoundedCornerShape(12.dp),
                                                activeBorderColor = Color(0xFFFFB74D),
                                                onClick = { actions.onTestSource() }
                                            )
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFFF9800).copy(alpha = 0.2f))
                                            .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Lucide.Play,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB74D),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(Res.string.watch_party_source_test),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFFFB74D)
                                        )
                                    }
                                }

                                // Кнопка смены источника
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .tvAndWebHoverEffect(
                                            scaleTarget = 1.03f,
                                            shape = RoundedCornerShape(12.dp),
                                            activeBorderColor = WatchPartyPurple,
                                            onClick = { actions.onToggleSelectSource(true) }
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Lucide.RefreshCw,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(Res.string.watch_party_source_change),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Ошибка создания
                if (state.createError != null) {
                    Text(
                        text = state.createError,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF5252)
                    )
                }

                // 4. Кнопка «Создать комнату»
                Row(
                    modifier = Modifier
                        .then(if (state.selectedSourceId != null) Modifier.focusRequester(initialFocusRequester) else Modifier)
                        .fillMaxWidth()
                        .height(54.dp)
                        .tvAndWebHoverEffect(
                            scaleTarget = 1.03f,
                            shape = RoundedCornerShape(16.dp),
                            activeBorderColor = Color.White,
                            clickEnabled = canCreate,
                            onClick = { if (canCreate) actions.onCreateRoom() }
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (canCreate) WatchPartyPurple else Color.White.copy(alpha = 0.12f)
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (state.isCreatingRoom) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.watch_party_btn_submit_create),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canCreate) Color.White else Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}
