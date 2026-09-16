package org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.yield
import org.ensodai.avalonmediacard.presentation.overlay.FullscreenPopupPositionProvider
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalContentFocusRequester
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvFocusManagerProvider

/**
 * Главный хост выезжающей правой ТВ-шторки. 
 * Располагается на верхнем уровне экрана (например, в корне [MainAppContent]).
 * 
 * Читает навигатор из [LocalTvDrawerNavigator]. При переходах между экранами в стеке
 * автоматически анимирует переход с помощью [AnimatedContent] с учетом направления навигации.
 *
 * Обрабатывает нажатия кнопок Назад/Escape и D-Pad влево, пробрасывая их в `dismissCurrent()`.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AvalonTvRightDrawerHost(
    state: TvDrawerNavigator = LocalTvDrawerNavigator.current,
    drawerWidth: Dp = 380.dp,
    content: @Composable () -> Unit
) {
    val drawerTransition = remember { MutableTransitionState(false) }
    drawerTransition.targetState = state.isOpen

    val showPopup = drawerTransition.currentState || drawerTransition.targetState

    val contentFocusRequester = LocalContentFocusRequester.current
    var activeCallerFocusRequester by remember { mutableStateOf<FocusRequester?>(null) }

    var lastDisplayedScreen by remember { mutableStateOf<TvDrawerScreen?>(null) }
    if (state.currentScreen != null) {
        lastDisplayedScreen = state.currentScreen
    }
    val screenToDisplay = state.currentScreen ?: lastDisplayedScreen

    LaunchedEffect(showPopup) {
        if (!showPopup) {
            lastDisplayedScreen = null
        }
    }

    LaunchedEffect(state.isOpen, state.callerFocusRequester, state.currentScreen) {
        if (state.isOpen) {
            val caller = state.callerFocusRequester
                ?: state.currentScreen?.callerFocusRequester
                ?: state.lastCallerFocusRequester
            if (caller != null) {
                activeCallerFocusRequester = caller
            }
        }
    }

    fun getBestCaller(): FocusRequester {
        return state.callerFocusRequester
            ?: state.lastCallerFocusRequester
            ?: activeCallerFocusRequester
            ?: state.currentScreen?.callerFocusRequester
            ?: contentFocusRequester
    }

    fun safeDismiss() {
        state.dismissCurrent()
    }

    LaunchedEffect(state.isOpen) {
        if (!state.isOpen) {
            val caller = getBestCaller()
            runCatching { caller.requestFocus() }
        }
    }

    val drawerRootRequester = remember { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (showPopup) {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { safeDismiss() },
                properties = PopupProperties(
                    focusable = state.isOpen,
                    dismissOnClickOutside = false,
                    clippingEnabled = false
                )
            ) {
                TvFocusManagerProvider {
                    // Корневой контейнер Popup — изолированный фокусный домен.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(drawerRootRequester)
                            .focusProperties {
                                onExit = {
                                    if (requestedFocusDirection == FocusDirection.Left) {
                                        // Нажатие «Влево» — переход назад в стеке или закрытие шторки
                                        safeDismiss()
                                    }
                                    // Блокируем любой выход фокуса за пределы шторки
                                    cancelFocusChange()
                                }
                            }
                            .focusGroup()
                    ) {
                        // Scrim (затемнение фона) — НЕ фокусируемый.
                        AnimatedVisibility(
                            visible = state.isOpen,
                            enter = fadeIn(animationSpec = tween(durationMillis = 280, easing = LinearEasing)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 250, easing = LinearEasing))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .pointerInput(Unit) { detectTapGestures { safeDismiss() } }
                            )
                        }

                        // Сама правая шторка
                        AnimatedVisibility(
                            visibleState = drawerTransition,
                            enter = slideInHorizontally(
                                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                                initialOffsetX = { it }
                            ),
                            exit = slideOutHorizontally(
                                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                                targetOffsetX = { it }
                            ),
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(drawerWidth)
                                    .clipToBounds()
                                    .background(Color(0xFF131317))
                                    .drawWithContent {
                                        drawContent()
                                        // Четкая левая грань поверх контента шторки
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.08f),
                                            start = Offset(0f, 0f),
                                            end = Offset(0f, size.height),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                                    .onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyUp) {
                                            if (event.key == Key.Back || event.key == Key.Escape) {
                                                safeDismiss()
                                                return@onKeyEvent true
                                            }
                                        }
                                        false
                                    }
                                    .padding(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 0.dp)
                            ) {
                                if (screenToDisplay != null) {
                                    AnimatedContent(
                                        targetState = screenToDisplay,
                                        transitionSpec = {
                                            val animationSpec = tween<IntOffset>(durationMillis = 280, easing = FastOutSlowInEasing)
                                            val fadeSpec = tween<Float>(durationMillis = 220)
                                            if (state.isMovingForward) {
                                                (slideInHorizontally(animationSpec = animationSpec) { width -> width / 3 } + fadeIn(animationSpec = fadeSpec)) togetherWith
                                                        (slideOutHorizontally(animationSpec = animationSpec) { width -> -width / 3 } + fadeOut(animationSpec = fadeSpec))
                                            } else {
                                                (slideInHorizontally(animationSpec = animationSpec) { width -> -width / 3 } + fadeIn(animationSpec = fadeSpec)) togetherWith
                                                        (slideOutHorizontally(animationSpec = animationSpec) { width -> width / 3 } + fadeOut(animationSpec = fadeSpec))
                                            }.using(SizeTransform(clip = false))
                                        },
                                        contentKey = { it.key },
                                        label = "TvDrawerTransition"
                                    ) { screen ->
                                        val screenFocusRequester = remember(screen.key) { FocusRequester() }
                                        val focusManager = LocalFocusManager.current

                                        LaunchedEffect(screen.key) {
                                            yield()
                                            val target = screen.initialFocusRequester ?: screenFocusRequester
                                            val isFocused = runCatching { target.requestFocus() }.isSuccess
                                            if (!isFocused) {
                                                runCatching { focusManager.moveFocus(FocusDirection.Enter) }
                                            }
                                        }

                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .focusRequester(screenFocusRequester)
                                                .then(
                                                    if (screen.initialFocusRequester != null) {
                                                        Modifier.focusRestorer(screen.initialFocusRequester!!)
                                                    } else {
                                                        Modifier.focusRestorer()
                                                    }
                                                )
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

                                            // Содержимое экрана
                                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                                screen.Content(navigator = state)
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
}
