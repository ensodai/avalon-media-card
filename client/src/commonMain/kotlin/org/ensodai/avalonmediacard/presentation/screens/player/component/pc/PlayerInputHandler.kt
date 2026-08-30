package org.ensodai.avalonmediacard.presentation.screens.player.component.pc

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import org.ensodai.avalonmediacard.core.PlaybackController

@Composable
fun PlayerInputHandler(
    controller: PlaybackController,
    isFullscreen: Boolean,
    showUiOverlay: Boolean = true,
    onFullscreenToggle: () -> Unit,
    onMouseMoved: (x: Float, y: Float) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    LaunchedEffect(showUiOverlay) {
        runCatching { focusRequester.requestFocus() }
    }

    Box(
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    val duration = if (controller.state.duration > 0.0) controller.state.duration else Double.MAX_VALUE
                    val code = event.utf16CodePoint

                    val isStepBackward = event.key == Key.Comma ||
                            code == '<'.code || code == ','.code ||
                            code == 'б'.code || code == 'Б'.code

                    val isStepForward = event.key == Key.Period ||
                            code == '>'.code || code == '.'.code ||
                            code == 'ю'.code || code == 'Ю'.code

                    if (event.isAltPressed || event.isCtrlPressed) {
                        val digit = when (event.key) {
                            Key.One, Key.NumPad1 -> "1"
                            Key.Two, Key.NumPad2 -> "2"
                            Key.Three, Key.NumPad3 -> "3"
                            Key.Four, Key.NumPad4 -> "4"
                            Key.Five, Key.NumPad5 -> "5"
                            Key.Six, Key.NumPad6 -> "6"
                            Key.Seven, Key.NumPad7 -> "7"
                            Key.Eight, Key.NumPad8 -> "8"
                            Key.Nine, Key.NumPad9 -> "9"
                            Key.Zero, Key.NumPad0 -> "0"
                            else -> null
                        }
                        if (digit != null) {
                            val prefix = if (event.isAltPressed) "Alt+" else "Ctrl+"
                            controller.sendKeyPress("$prefix$digit")
                            return@onKeyEvent true
                        }
                    }

                    when {
                        event.key == Key.Spacebar -> {
                            if (controller.state.isPlaying) controller.pause() else controller.play()
                            true
                        }

                        event.key == Key.DirectionLeft -> {
                            val newTime = (controller.state.currentTime - 5.0).coerceAtLeast(0.0)
                            controller.seek(newTime)
                            true
                        }

                        event.key == Key.DirectionRight -> {
                            val newTime = (controller.state.currentTime + 5.0).coerceAtMost(duration)
                            controller.seek(newTime)
                            true
                        }

                        isStepBackward -> {
                            controller.stepBackward()
                            true
                        }

                        isStepForward -> {
                            controller.stepForward()
                            true
                        }

                        event.key == Key.DirectionUp -> {
                            val newVol = (controller.state.volume + 0.05).coerceIn(0.0, 1.0)
                            if (controller.state.isMuted) {
                                controller.setMuted(false)
                            }
                            controller.setVolume(newVol)
                            true
                        }

                        event.key == Key.DirectionDown -> {
                            val newVol = (controller.state.volume - 0.05).coerceIn(0.0, 1.0)
                            controller.setVolume(newVol)
                            true
                        }

                        event.key == Key.M -> {
                            if (controller.state.isMuted || controller.state.volume == 0.0) {
                                if (controller.state.volume == 0.0) {
                                    controller.setVolume(0.5)
                                }
                                controller.setMuted(false)
                            } else {
                                controller.setMuted(true)
                            }
                            true
                        }

                        event.key == Key.F || event.key == Key.F11 -> {
                            onFullscreenToggle()
                            true
                        }

                        event.key == Key.Escape -> {
                            if (isFullscreen) {
                                onFullscreenToggle()
                                true
                            } else false
                        }

                        else -> false
                    }
                } else false
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Move) {
                            val position = event.changes.first().position
                            onMouseMoved(position.x, position.y)
                        } else if (event.type == PointerEventType.Press) {
                            runCatching { focusRequester.requestFocus() }
                        }
                    }
                }
            }
    ) {
        content()
    }
}
