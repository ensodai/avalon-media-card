package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Главный хост выезжающей правой ТВ-шторки. 
 * Должен располагаться на верхнем уровне экрана (например, в корне Box плеера).
 * 
 * Читает стейт из [LocalTvDrawerState]. При добавлении экранов (вложенных меню)
 * автоматически анимирует переход с помощью [AnimatedContent].
 *
 * Обрабатывает нажатия кнопок Назад/Escape, пробрасывая их в `dismissCurrent()`.
 */
@Composable
fun AvalonTvRightDrawerHost(
    state: TvDrawerState = LocalTvDrawerState.current,
    drawerWidth: Dp = 380.dp,
    content: @Composable () -> Unit
) {
    var showPopup by remember { mutableStateOf(false) }

    // Автофокус при открытии шторки и задержка для анимации закрытия
    LaunchedEffect(state.isOpen) {
        if (state.isOpen) {
            showPopup = true
        } else {
            delay(350.milliseconds)
            showPopup = false
        }
    }

    val currentScreen = state.current
    val screenFocusRequester = remember(currentScreen?.id) { FocusRequester() }

    LaunchedEffect(currentScreen?.id, showPopup) {
        if (showPopup && currentScreen != null) {
            delay(50.milliseconds)
            runCatching { screenFocusRequester.requestFocus() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (showPopup) {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { state.dismissCurrent() },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnClickOutside = false,
                    clippingEnabled = false
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Scrim (затемнение фона на весь экран)
                    AnimatedVisibility(
                        visible = state.isOpen,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.65f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    state.dismissCurrent()
                                }
                        )
                    }

                    // Сама правая шторка
                    AnimatedVisibility(
                        visible = state.isOpen,
                        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(drawerWidth)
                                .background(Color(0xFF141418))
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp && (event.key == Key.Back || event.key == Key.Escape)) {
                                        state.dismissCurrent()
                                        true
                                    } else false
                                }
                                .padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 0.dp)
                        ) {
                            if (currentScreen != null) {
                                AnimatedContent(
                                    targetState = currentScreen,
                                    transitionSpec = {
                                        (slideInHorizontally { width -> width / 2 } + fadeIn()) togetherWith
                                                (slideOutHorizontally { width -> -width / 2 } + fadeOut())
                                    },
                                    label = "TvDrawerTransition"
                                ) { screen ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .focusRequester(screenFocusRequester)
                                            .focusRestorer()
                                            .focusGroup()
                                    ) {
                                        // Header шторки
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (screen.icon != null) {
                                                Icon(
                                                    imageVector = screen.icon!!,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = screen.title,
                                                    color = Color.White,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (screen.subtitle != null) {
                                                    Text(
                                                        text = screen.subtitle!!,
                                                        color = Color.White.copy(alpha = 0.6f),
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        // Разделитель
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(Color.White.copy(alpha = 0.1f))
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Содержимое шторки
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                            screen.content()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
