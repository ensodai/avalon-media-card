package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration.Companion.milliseconds

fun Modifier.tvAndWebHoverEffect(
    scaleTarget: Float = 1.08f,
    activeBorderWidth: Dp = 2.dp,
    activeBorderColor: Color = Color.White,
    defaultBorderWidth: Dp = 0.dp,
    defaultBorderColor: Color = Color.Transparent,
    shape: Shape,
    clickEnabled: Boolean = true,
    focusEnabled: Boolean = true,
    tiltEnabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onStateChange: ((isActive: Boolean) -> Unit)? = null
): Modifier = composed {
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val deviceTarget = LocalDeviceTarget.current
    var maxDimensionPx by remember { mutableStateOf(0f) }

    var isHovered by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var tvPressed by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isItemPressed by interactionSource.collectIsPressedAsState()

    val isPressed = isItemPressed || tvPressed
    val isActive = isHovered || isFocused

    val tvPressEvents = remember { MutableSharedFlow<Boolean>(extraBufferCapacity = 10) }

    LaunchedEffect(interactionSource, deviceTarget) {
        if (!deviceTarget.isTv) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Cancel -> {
                        if (!isHovered && isFocused) {
                            focusManager.clearFocus()
                        }
                    }
                    is PressInteraction.Release -> {
                        if (!isHovered && isFocused) {
                            focusManager.clearFocus()
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        tvPressEvents.collectLatest { pressed ->
            if (pressed) {
                tvPressed = true
            } else {
                delay(100.milliseconds)
                tvPressed = false
            }
        }
    }

    LaunchedEffect(isActive) {
        onStateChange?.invoke(isActive)
    }

    var componentSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    var targetRotationX by remember { mutableStateOf(0f) }
    var targetRotationY by remember { mutableStateOf(0f) }

    val currentRotationX by animateFloatAsState(targetValue = targetRotationX, label = "rotX")
    val currentRotationY by animateFloatAsState(targetValue = targetRotationY, label = "rotY")

    val hoverScale = remember(maxDimensionPx, scaleTarget) {
        if (maxDimensionPx <= 0f) scaleTarget else {
            val growPx = with(density) { 8.dp.toPx() }
            val calculated = 1f + (growPx / maxDimensionPx)
            minOf(calculated, scaleTarget)
        }
    }

    val pressScale = remember(maxDimensionPx) {
        if (maxDimensionPx <= 0f) 0.95f else {
            val shrinkPx = with(density) { 4.dp.toPx() }
            val calculated = 1f - (shrinkPx / maxDimensionPx)
            calculated.coerceIn(0.85f, 0.995f)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressScale else if (isActive) hoverScale else 1.0f,
        animationSpec = if (isPressed) tween(50) else spring(),
        label = "hover_scale"
    )
    val currentBorderWidth by animateDpAsState(
        targetValue = if (isActive) activeBorderWidth else defaultBorderWidth,
        label = "hover_border_width"
    )
    val currentBorderColor by animateColorAsState(
        targetValue = if (isActive) activeBorderColor else defaultBorderColor,
        label = "hover_border_color"
    )

    this
        .onSizeChanged { size ->
            componentSize = size
            maxDimensionPx = maxOf(size.width, size.height).toFloat()
        }
        .zIndex(if (isActive) 1f else 0f)
        .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
        .onPreviewKeyEvent { event ->
            if (focusEnabled && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                when (event.type) {
                    KeyEventType.KeyDown -> {
                        if (clickEnabled) {
                            tvPressEvents.tryEmit(true)
                            return@onPreviewKeyEvent true
                        }
                    }
                    KeyEventType.KeyUp -> {
                        if (clickEnabled) {
                            tvPressEvents.tryEmit(false)
                            onClick?.invoke()
                            return@onPreviewKeyEvent true
                        }
                    }
                }
            }
            false
        }
        .focusable(enabled = focusEnabled)
        .pointerHoverIcon(
            if (onClick != null && clickEnabled) PointerIcon.Hand else PointerIcon.Default
        )
        .then(
            if (clickEnabled && onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = true,
                    onClick = onClick
                )
            } else Modifier
        )
        .focusProperties { canFocus = focusEnabled }
        .pointerInput(tiltEnabled) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    when (event.type) {
                        PointerEventType.Enter -> isHovered = true
                        PointerEventType.Move -> {
                            if (tiltEnabled && componentSize.width > 0 && componentSize.height > 0) {
                                val position = event.changes.firstOrNull()?.position ?: continue
                                val nx = (position.x / componentSize.width) * 2f - 1f
                                val ny = (position.y / componentSize.height) * 2f - 1f

                                val maxRotation = if (maxDimensionPx <= 0f) 10f else {
                                    (300f / maxDimensionPx * 10f).coerceIn(1.5f, 10f)
                                }

                                targetRotationY = nx * maxRotation
                                targetRotationX = -ny * maxRotation
                            }
                        }
                        PointerEventType.Exit -> {
                            isHovered = false
                            targetRotationX = 0f
                            targetRotationY = 0f
                            if (!deviceTarget.isTv && !isItemPressed && isFocused && !tvPressed) {
                                focusManager.clearFocus()
                            }
                        }
                    }
                }
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            rotationX = currentRotationX
            rotationY = currentRotationY
            cameraDistance = 12f * density.density
        }
        .border(currentBorderWidth, currentBorderColor, shape)
        .clip(shape)
}