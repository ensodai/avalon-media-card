package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.tv.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.common_close
import avalonmediacard.client.generated.resources.watch_party_btn_join
import avalonmediacard.client.generated.resources.watch_party_enter_pin_hint
import avalonmediacard.client.generated.resources.watch_rooms_btn_connect
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Delete
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.WatchPartyActions
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyViewState
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchPartyPinEntryTv(
    state: WatchPartyViewState,
    actions: WatchPartyActions,
    modifier: Modifier = Modifier
) {
    val firstButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching {
            firstButtonFocusRequester.requestFocus()
        }
    }

    val onAppendDigit: (String) -> Unit = { digit ->
        if (state.pinInput.length < 6) {
            actions.onPinChanged(state.pinInput + digit)
        }
    }

    val onDeleteDigit: () -> Unit = {
        if (state.pinInput.isNotEmpty()) {
            actions.onPinChanged(state.pinInput.dropLast(1))
        }
    }

    val onClearPin: () -> Unit = {
        actions.onPinChanged("")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val digit = when (event.key) {
                        Key.Zero, Key.NumPad0 -> "0"
                        Key.One, Key.NumPad1 -> "1"
                        Key.Two, Key.NumPad2 -> "2"
                        Key.Three, Key.NumPad3 -> "3"
                        Key.Four, Key.NumPad4 -> "4"
                        Key.Five, Key.NumPad5 -> "5"
                        Key.Six, Key.NumPad6 -> "6"
                        Key.Seven, Key.NumPad7 -> "7"
                        Key.Eight, Key.NumPad8 -> "8"
                        Key.Nine, Key.NumPad9 -> "9"
                        else -> null
                    }
                    when {
                        digit != null -> {
                            onAppendDigit(digit)
                            true
                        }
                        event.key == Key.Backspace || event.key == Key.Delete -> {
                            onDeleteDigit()
                            true
                        }
                        event.key == Key.Enter || event.key == Key.NumPadEnter -> {
                            if (state.pinInput.length == 6 && !state.isJoining) {
                                actions.onJoinByPin()
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        // Кнопка закрытия / назад в левом верхнем углу
        Box(
            modifier = Modifier
                .padding(start = 32.dp, top = 28.dp)
                .size(46.dp)
                .tvAndWebHoverEffect(scaleTarget = 1.1f, shape = CircleShape, onClick = actions.onClose)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Lucide.X,
                contentDescription = stringResource(Res.string.common_close),
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp)
            )
        }

        // Центрированная ТВ-карточка ввода PIN
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(480.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
                .padding(horizontal = 32.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Заголовок
            Text(
                text = stringResource(Res.string.watch_rooms_btn_connect),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(Res.string.watch_party_enter_pin_hint),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )

            // 6 сегментов PIN-кода
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary

                for (i in 0 until 6) {
                    val digitChar = state.pinInput.getOrNull(i)
                    val isActiveCell = (i == state.pinInput.length) && state.pinInput.length < 6
                    val isFilled = digitChar != null

                    val cellShape = RoundedCornerShape(12.dp)
                    val borderColor = when {
                        isActiveCell -> primaryColor
                        isFilled -> Color.White.copy(alpha = 0.4f)
                        else -> Color.White.copy(alpha = 0.12f)
                    }
                    val borderWidth = if (isActiveCell) 2.dp else 1.dp

                    Box(
                        modifier = Modifier
                            .size(54.dp, 62.dp)
                            .clip(cellShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(borderWidth, borderColor, cellShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFilled) {
                            Text(
                                text = digitChar.toString(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "•",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }

            // Ошибка валидации или подключения
            if (state.joinError != null) {
                Text(
                    text = state.joinError,
                    fontSize = 13.sp,
                    color = Color(0xFFFF5252),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Экранный цифровой блок 3x4 под D-pad
            val numpadShape = RoundedCornerShape(14.dp)
            val buttons = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "DEL")
            )

            Column(
                modifier = Modifier.width(320.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                buttons.forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEachIndexed { colIndex, keySymbol ->
                            val isFirstButton = rowIndex == 0 && colIndex == 0
                            val isActionKey = keySymbol == "C" || keySymbol == "DEL"

                            val onClick = {
                                when (keySymbol) {
                                    "C" -> onClearPin()
                                    "DEL" -> onDeleteDigit()
                                    else -> onAppendDigit(keySymbol)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .then(if (isFirstButton) Modifier.focusRequester(firstButtonFocusRequester) else Modifier)
                                    .tvAndWebHoverEffect(
                                        scaleTarget = 1.08f,
                                        shape = numpadShape,
                                        activeBorderWidth = 2.dp,
                                        activeBorderColor = MaterialTheme.colorScheme.primary,
                                        defaultBorderWidth = 1.dp,
                                        defaultBorderColor = Color.White.copy(alpha = 0.08f),
                                        onClick = onClick
                                    )
                                    .background(
                                        if (isActionKey) Color.White.copy(alpha = 0.05f)
                                        else Color.White.copy(alpha = 0.09f),
                                        numpadShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (keySymbol == "DEL") {
                                    Icon(
                                        imageVector = Lucide.Delete,
                                        contentDescription = "Удалить",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = keySymbol,
                                        fontSize = if (isActionKey) 15.sp else 20.sp,
                                        fontWeight = if (isActionKey) FontWeight.SemiBold else FontWeight.Bold,
                                        color = if (isActionKey) Color.White.copy(alpha = 0.7f) else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Главная кнопка "Присоединиться"
            val isJoinEnabled = state.pinInput.length == 6 && !state.isJoining
            val joinShape = RoundedCornerShape(14.dp)

            Box(
                modifier = Modifier
                    .width(320.dp)
                    .height(52.dp)
                    .tvAndWebHoverEffect(
                        scaleTarget = 1.04f,
                        shape = joinShape,
                        activeBorderWidth = 2.dp,
                        activeBorderColor = MaterialTheme.colorScheme.primary,
                        clickEnabled = isJoinEnabled,
                        onClick = actions.onJoinByPin
                    )
                    .background(
                        if (isJoinEnabled) Color.White else Color.White.copy(alpha = 0.12f),
                        joinShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (state.isJoining) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = Color.Black
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.watch_party_btn_join),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isJoinEnabled) Color.Black else Color.White.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }
}
