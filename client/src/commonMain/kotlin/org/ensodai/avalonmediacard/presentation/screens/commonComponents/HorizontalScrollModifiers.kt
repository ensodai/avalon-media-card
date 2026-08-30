package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

/**
 * Enables smooth horizontal dragging via mouse/touch and calibrated mouse wheel scrolling
 * for Compose Multiplatform LazyRow (JVM, Android, Web Wasm/JS).
 */
fun Modifier.horizontalScrollWithMouseAndTouch(
    state: LazyListState,
    wheelSpeedMultiplier: Float = 0.35f
): Modifier = composed {
    val coroutineScope = rememberCoroutineScope()
    this
        .pointerInput(state) {
            detectHorizontalDragGestures { change, dragAmount ->
                change.consume()
                state.dispatchRawDelta(-dragAmount)
            }
        }
        .pointerInput(state, wheelSpeedMultiplier) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val change = event.changes.firstOrNull()
                        if (change != null) {
                            val deltaY = change.scrollDelta.y
                            val deltaX = change.scrollDelta.x
                            val delta = if (deltaX != 0f) deltaX else deltaY
                            if (delta != 0f) {
                                val pixels = delta * 64f * wheelSpeedMultiplier
                                coroutineScope.launch {
                                    state.dispatchRawDelta(pixels)
                                }
                                change.consume()
                            }
                        }
                    }
                }
            }
        }
}
