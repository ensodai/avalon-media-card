package org.ensodai.avalonmediacard.presentation.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalContentFocusRequester
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvFocusManagerProvider

/**
 * Универсальный провайдер нулевого смещения для полноэкранных попапов.
 * Гарантирует абсолютную привязку к (0,0) экрана вне зависимости от координат вызывающего элемента.
 */
val FullscreenPopupPositionProvider = object : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = IntOffset.Zero
}

/**
 * Локальный провайдер функции безопасного закрытия текущего модального окна.
 * Автоматически восстанавливает фокус на вызывающий элемент перед вызовом onDismissRequest.
 */
val LocalModalDismiss = compositionLocalOf<() -> Unit> { { } }

/**
 * Единый архитектурный хост модальных поверхностей и диалогов на TV и Web.
 * Инкапсулирует:
 * 1. Полноэкранный слой [Popup] с нулевым смещением;
 * 2. 2D-навигацию стрелками пульта/клавиатуры через [TvFocusManagerProvider];
 * 3. Перехват аппаратных клавиш Back/Escape для закрытия;
 * 4. Захват фокуса через [LaunchedEffect] после монтирования Popup FocusOwner в Wasm;
 * 5. Изоляцию D-Pad навигации внутри диалога через [focusProperties];
 * 6. Автоматическое восстановление фокуса на вызывающий элемент ([callerFocusRequester]) при закрытии
 *    (как при вызове [safeDismiss], так и страховочно через [DisposableEffect.onDispose]).
 */
@Composable
fun TvModalSurface(
    modifier: Modifier = Modifier,
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    callerFocusRequester: FocusRequester? = null,
    initialFocusRequester: FocusRequester? = null,
    scrimColor: Color = Color.Black.copy(alpha = 0.85f),
    contentAlignment: Alignment = Alignment.Center,
    dismissOnClickOutside: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    if (!isOpen) return

    val contentFocusRequester = LocalContentFocusRequester.current

    fun safeDismiss() {
        val targetRequester = callerFocusRequester ?: contentFocusRequester
        runCatching { targetRequester.requestFocus() }
        onDismissRequest()
    }

    // Страховочный возврат фокуса при любом закрытии модального слоя (включая смену isOpen из ViewModel)
    DisposableEffect(Unit) {
        onDispose {
            val targetRequester = callerFocusRequester ?: contentFocusRequester
            runCatching { targetRequester.requestFocus() }
        }
    }

    Popup(
        popupPositionProvider = FullscreenPopupPositionProvider,
        properties = PopupProperties(
            focusable = true,
            dismissOnClickOutside = false,
            clippingEnabled = false
        ),
        onDismissRequest = { safeDismiss() }
    ) {
        TvFocusManagerProvider {
            CompositionLocalProvider(LocalModalDismiss provides ::safeDismiss) {
                val modalContainerRequester = remember { FocusRequester() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scrimColor)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && (event.key == Key.Back || event.key == Key.Escape)) {
                                safeDismiss()
                                true
                            } else {
                                false
                            }
                        }
                        .pointerInput(dismissOnClickOutside) {
                            detectTapGestures {
                                if (dismissOnClickOutside) {
                                    safeDismiss()
                                }
                            }
                        },
                    contentAlignment = contentAlignment
                ) {
                    Box(
                        modifier = modifier
                            .pointerInput(Unit) { detectTapGestures { } } // Защита от прокликивания на scrim из тела контента без захвата фокуса
                            .focusRequester(modalContainerRequester)
                            .focusProperties {
                                onExit = {
                                    cancelFocusChange()
                                }
                            }
                            .then(
                                if (initialFocusRequester != null) {
                                    Modifier.focusRestorer(initialFocusRequester)
                                } else {
                                    Modifier.focusRestorer()
                                }
                            )
                            .focusGroup(),
                        content = content
                    )
                }

                LaunchedEffect(Unit) {
                    val target = initialFocusRequester ?: modalContainerRequester
                    runCatching { target.requestFocus() }
                }
            }
        }
    }
}
