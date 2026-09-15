package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager

@OptIn(ExperimentalFoundationApi::class)
class TvPivotBringIntoViewSpec(
    private val pivotFraction: Float = 0.35f
) : BringIntoViewSpec {
    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float
    ): Float {
        val targetPivot = containerSize * pivotFraction
        val elementCenter = offset + (size / 2f)
        return elementCenter - targetPivot
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvFocusManagerProvider(
    isTv: Boolean = LocalDeviceTarget.current.isTv,
    pivotFraction: Float = 0.35f,
    content: @Composable () -> Unit
) {
    val deviceTarget = LocalDeviceTarget.current
    val focusManager = LocalFocusManager.current

    // Включаем трансляцию клавиш только для Web и Desktop (где нет нативного Android FocusFinder)
    val shouldDispatchWebKeys = deviceTarget != DeviceTarget.ANDROID_TV && !deviceTarget.isTouch

    val keyModifier = if (shouldDispatchWebKeys) {
        Modifier.onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                val direction = when (event.key) {
                    Key.DirectionDown -> FocusDirection.Down
                    Key.DirectionUp -> FocusDirection.Up
                    Key.DirectionRight -> FocusDirection.Right
                    Key.DirectionLeft -> FocusDirection.Left
                    else -> null
                }
                if (direction != null) {
                    focusManager.moveFocus(direction)
                } else false
            } else false
        }
    } else Modifier

    val wrappedContent = @Composable {
        Box(modifier = Modifier.fillMaxSize().then(keyModifier)) {
            content()
        }
    }

    if (isTv) {
        CompositionLocalProvider(
            LocalBringIntoViewSpec provides TvPivotBringIntoViewSpec(pivotFraction = pivotFraction),
            content = wrappedContent
        )
    } else {
        wrappedContent()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvHorizontalFocusProvider(
    pivotFraction: Float = 0.5f,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalBringIntoViewSpec provides TvPivotBringIntoViewSpec(pivotFraction = pivotFraction),
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
class TvEdgeGatedBringIntoViewSpec(
    private val pivotFraction: Float = 0.35f,
    private val safeViewportFraction: Float = 0.75f
) : BringIntoViewSpec {
    override fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float
    ): Float {
        val safeThreshold = containerSize * safeViewportFraction
        if (offset >= 0f && (offset + size) <= safeThreshold) {
            return 0f
        }
        val targetPivot = containerSize * pivotFraction
        val elementCenter = offset + (size / 2f)
        return elementCenter - targetPivot
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvEdgeGatedFocusProvider(
    pivotFraction: Float = 0.35f,
    safeViewportFraction: Float = 0.75f,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalBringIntoViewSpec provides TvEdgeGatedBringIntoViewSpec(
            pivotFraction = pivotFraction,
            safeViewportFraction = safeViewportFraction
        ),
        content = content
    )
}
