package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration.Companion.milliseconds

fun Modifier.tvAndWebHoverEffect(
    scaleTarget: Float = 1.08f,
    webHoverScale: Float = if (scaleTarget > 1.0f) 1.02f else 1.0f,
    activeBorderWidth: Dp = 2.dp,
    activeBorderColor: Color = Color.White,
    webHoverBorderWidth: Dp? = null,
    webHoverBorderColor: Color? = null,
    defaultBorderWidth: Dp = 0.dp,
    defaultBorderColor: Color = Color.Transparent,
    defaultBgColor: Color = Color.Transparent,
    hoverBgColor: Color? = null,
    shape: Shape,
    clickEnabled: Boolean = true,
    focusEnabled: Boolean = true,
    tiltEnabled: Boolean = false,
    onClick: (() -> Unit)? = null,
    onStateChange: ((isActive: Boolean) -> Unit)? = null
): Modifier = composed {
    val density = LocalDensity.current
    val deviceTarget = LocalDeviceTarget.current
    val isTv = deviceTarget.isTv
    var maxDimensionPx by remember { mutableStateOf(0f) }

    var isHovered by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var tvPressed by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isItemPressed by interactionSource.collectIsPressedAsState()
    val isItemHovered by interactionSource.collectIsHoveredAsState()

    val isMouseHovered = (isHovered || isItemHovered) && !isTv
    val isKeyboardFocused = isFocused && !isTv && !isMouseHovered
    val isTvActive = (isFocused || isHovered || isItemHovered) && isTv

    val isPressed = isItemPressed || tvPressed
    val isActive = isMouseHovered || isKeyboardFocused || isTvActive

    val tvPressEvents = remember { MutableSharedFlow<Boolean>(extraBufferCapacity = 10) }

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

    val pressScale = remember(maxDimensionPx, isTv) {
        if (maxDimensionPx <= 0f) {
            if (isTv) 0.95f else 0.98f
        } else {
            val shrinkDp = if (isTv) 4.dp else 2.dp
            val shrinkPx = with(density) { shrinkDp.toPx() }
            val calculated = 1f - (shrinkPx / maxDimensionPx)
            calculated.coerceIn(if (isTv) 0.85f else 0.95f, 0.995f)
        }
    }

    val targetScale = when {
        isPressed -> pressScale
        isTvActive -> hoverScale
        isMouseHovered -> webHoverScale
        isKeyboardFocused -> 1.0f
        else -> 1.0f
    }

    val resolvedWebBorderWidth = webHoverBorderWidth ?: if (hoverBgColor != null) 0.dp else 1.dp

    val targetBorderWidth = when {
        isTvActive -> activeBorderWidth
        isKeyboardFocused -> activeBorderWidth
        isMouseHovered -> resolvedWebBorderWidth
        else -> defaultBorderWidth
    }

    val targetBorderColor = when {
        isTvActive -> activeBorderColor
        isKeyboardFocused -> activeBorderColor
        isMouseHovered -> {
            when {
                webHoverBorderColor != null -> webHoverBorderColor
                resolvedWebBorderWidth == 0.dp -> Color.Transparent
                activeBorderColor == Color.Transparent -> Color.Transparent
                activeBorderColor == Color.White -> Color.White.copy(alpha = 0.35f)
                else -> activeBorderColor
            }
        }
        else -> defaultBorderColor
    }

    val targetBgColor = when {
        isPressed && hoverBgColor != null -> hoverBgColor
        isMouseHovered && hoverBgColor != null -> hoverBgColor
        else -> defaultBgColor
    }

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (isPressed) tween(50) else spring(),
        label = "hover_scale"
    )
    val currentBorderWidth by animateDpAsState(
        targetValue = targetBorderWidth,
        label = "hover_border_width"
    )
    val currentBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        label = "hover_border_color"
    )
    val currentBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(120),
        label = "hover_bg_color"
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
            if (onClick != null && clickEnabled) PointerIcon.Hand else PointerIcon.Default,
            overrideDescendants = true
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
        .hoverable(
            interactionSource = interactionSource,
            enabled = clickEnabled || focusEnabled
        )
        .focusProperties { canFocus = focusEnabled }
        .pointerInput(tiltEnabled) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    when (event.type) {
                        PointerEventType.Enter -> isHovered = true
                        PointerEventType.Move -> {
                            isHovered = true
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
        .then(
            if (currentBorderWidth > 0.dp && currentBorderColor != Color.Transparent) {
                Modifier.border(currentBorderWidth, currentBorderColor, shape)
            } else Modifier
        )
        .then(
            if (currentBgColor != Color.Transparent) {
                Modifier.background(currentBgColor, shape)
            } else Modifier
        )
        .clip(shape)
}