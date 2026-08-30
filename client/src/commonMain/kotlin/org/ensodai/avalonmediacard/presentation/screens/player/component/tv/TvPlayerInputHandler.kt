package org.ensodai.avalonmediacard.presentation.screens.player.component.tv

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import org.ensodai.avalonmediacard.core.PlaybackController
import org.ensodai.avalonmediacard.presentation.navigation.AvalonBackHandler

/**
 * Обработчик клавиш пульта (D-Pad) и удержания фокуса на ТВ.
 */
@Composable
fun TvPlayerInputHandler(
    controller: PlaybackController,
    isUiVisible: Boolean,
    isShelfVisible: Boolean,
    onWakeUpUi: () -> Unit,
    onHideUi: () -> Unit,
    onToggleShelf: () -> Unit,
    onCloseShelf: () -> Unit,
    onClosePlayer: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AvalonBackHandler(enabled = true) {
        if (isShelfVisible) {
            onCloseShelf()
        } else if (isUiVisible) {
            // Первое нажатие Back при открытом UI скрывает оверлей
            onHideUi()
        } else {
            // Нажатие Back при скрытом UI закрывает плеер
            onClosePlayer()
        }
    }

    Box(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    onWakeUpUi() // Любое нажатие пульта сбрасывает таймер сна и будит UI

                    if (!isUiVisible) {
                        // Интерфейс скрыт: пробуждаем по любой кнопке
                        when (event.key) {
                            Key.DirectionCenter, Key.Enter -> {
                                true // Поглощаем первый клик Center для пробуждения
                            }

                            Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight -> {
                                true // Поглощаем D-pad для безопасного пробуждения и перевода фокуса
                            }

                            Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                                if (controller.state.isPlaying) controller.pause() else controller.play()
                                true
                            }

                            Key.Back, Key.Escape -> {
                                onClosePlayer()
                                true
                            }

                            else -> false
                        }
                    } else {
                        // Интерфейс виден
                        when (event.key) {
                            Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
                                if (controller.state.isPlaying) controller.pause() else controller.play()
                                true
                            }

                            Key.Back, Key.Escape -> {
                                if (isShelfVisible) {
                                    onCloseShelf()
                                    true
                                } else {
                                    onHideUi()
                                    true
                                }
                            }

                            else -> false // Остальные клавиши (D-Pad) идут в сфокусированные кнопки интерфейса
                        }
                    }
                } else false
            }
    ) {
        content()
    }
}
