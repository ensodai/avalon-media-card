package org.ensodai.avalonmediacard.presentation.screens.player.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.player.component.pc.formatTime

@Composable
fun PremiumSeekBar(
    currentTime: Double,
    duration: Double,
    bufferTime: Double = 0.0,
    onSeek: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceTarget = LocalDeviceTarget.current
    val isTv = deviceTarget.isTv

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }
    var hoverProgress by remember { mutableStateOf(-1f) }
    var isFocused by remember { mutableStateOf(false) }
    var pendingSeekTime by remember { mutableStateOf<Double?>(null) }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val isDpadSeeking = isFocused && pendingSeekTime != null
    val isActive = isHovered || isDragging || isFocused

    val trackHeight by animateDpAsState(if (isActive) 6.dp else 3.dp)
    val thumbRadius by animateDpAsState(
        if (isDpadSeeking) 10.dp
        else if (isFocused) 9.dp
        else if (isActive) 8.dp
        else 0.dp
    )
    val activeAlpha by animateFloatAsState(if (isActive) 1f else 0.7f)

    val progress = if (duration > 0.0) (currentTime / duration).toFloat().coerceIn(0f, 1f) else 0f
    val bufferProgress = if (duration > 0.0) (bufferTime / duration).toFloat().coerceIn(progress, 1f) else 0f

    val pendingProgress = if (pendingSeekTime != null && duration > 0.0) {
        (pendingSeekTime!! / duration).toFloat().coerceIn(0f, 1f)
    } else null

    val currentDrawProgress = when {
        isDragging -> dragProgress
        pendingProgress != null -> pendingProgress
        else -> progress
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat()

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp) // Touch / Focus target size
                .hoverable(interactionSource)
                .pointerHoverIcon(PointerIcon.Hand)
                .onFocusChanged { focusState ->
                    val newFocused = focusState.isFocused || focusState.hasFocus
                    if (!newFocused && isFocused) {
                        pendingSeekTime = null
                    }
                    isFocused = newFocused
                }
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionLeft -> {
                                if (duration > 0.0) {
                                    if (isTv) {
                                        val current = pendingSeekTime ?: currentTime
                                        val step = maxOf(10.0, duration * 0.005)
                                        pendingSeekTime = (current - step).coerceIn(0.0, duration)
                                    } else {
                                        onSeek((currentTime - 5.0).coerceIn(0.0, duration))
                                    }
                                }
                                true
                            }

                            Key.DirectionRight -> {
                                if (duration > 0.0) {
                                    if (isTv) {
                                        val current = pendingSeekTime ?: currentTime
                                        val step = maxOf(10.0, duration * 0.005)
                                        pendingSeekTime = (current + step).coerceIn(0.0, duration)
                                    } else {
                                        onSeek((currentTime + 5.0).coerceIn(0.0, duration))
                                    }
                                }
                                true
                            }

                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                if (pendingSeekTime != null) {
                                    onSeek(pendingSeekTime!!)
                                    pendingSeekTime = null
                                    true
                                } else {
                                    false
                                }
                            }

                            Key.Back, Key.Escape -> {
                                if (pendingSeekTime != null) {
                                    pendingSeekTime = null
                                    true
                                } else {
                                    false
                                }
                            }

                            else -> false
                        }
                    } else false
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Move, PointerEventType.Enter -> {
                                    val change = event.changes.firstOrNull()
                                    if (change != null) {
                                        val width = size.width.toFloat()
                                        if (width > 0) {
                                            hoverProgress = (change.position.x / width).coerceIn(0f, 1f)
                                        }
                                    }
                                }

                                PointerEventType.Exit -> {
                                    hoverProgress = -1f
                                }
                            }
                        }
                    }
                }
                .pointerInput(duration) {
                    if (duration <= 0.0) return@pointerInput
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown()
                            isDragging = true
                            val width = size.width.toFloat()
                            dragProgress = (down.position.x / width).coerceIn(0f, 1f)

                            var pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                if (change == null || !change.pressed) {
                                    isDragging = false
                                    onSeek(dragProgress.toDouble() * duration)
                                    break
                                } else {
                                    dragProgress = (change.position.x / width).coerceIn(0f, 1f)
                                    change.consume()
                                }
                            }
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerY = canvasHeight / 2f
            val trackH = trackHeight.toPx()
            val cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)

            // 1. Background Track (Inactive)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.2f),
                topLeft = Offset(0f, centerY - trackH / 2f),
                size = Size(canvasWidth, trackH),
                cornerRadius = cornerRadius
            )

            // 2. Buffer Track
            if (bufferProgress > 0f) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.4f),
                    topLeft = Offset(0f, centerY - trackH / 2f),
                    size = Size(canvasWidth * bufferProgress, trackH),
                    cornerRadius = cornerRadius
                )
            }

            // 3. Hover Preview Track
            if (isActive && !isDragging && hoverProgress > currentDrawProgress) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.6f),
                    topLeft = Offset(0f, centerY - trackH / 2f),
                    size = Size(canvasWidth * hoverProgress, trackH),
                    cornerRadius = cornerRadius
                )
            }

            // 4. Active Track
            drawRoundRect(
                color = primaryColor.copy(alpha = activeAlpha),
                topLeft = Offset(0f, centerY - trackH / 2f),
                size = Size(canvasWidth * currentDrawProgress, trackH),
                cornerRadius = cornerRadius
            )

            // 5. Thumb
            val tRadius = thumbRadius.toPx()
            if (tRadius > 0f) {
                drawCircle(
                    color = primaryColor,
                    radius = tRadius,
                    center = Offset(canvasWidth * currentDrawProgress, centerY)
                )
                if (isFocused) {
                    drawCircle(
                        color = Color.White,
                        radius = tRadius * 0.45f,
                        center = Offset(canvasWidth * currentDrawProgress, centerY)
                    )
                }
            }
        }

        // Tooltip
        val tooltipProgress = when {
            isDragging -> dragProgress
            isDpadSeeking && pendingProgress != null -> pendingProgress
            isActive && hoverProgress >= 0f -> hoverProgress
            else -> null
        }
        val tooltipTime = when {
            isDragging -> (dragProgress * duration).coerceIn(0.0, duration)
            isDpadSeeking && pendingSeekTime != null -> pendingSeekTime!!
            isActive && hoverProgress >= 0f -> (hoverProgress * duration).coerceIn(0.0, duration)
            else -> null
        }

        if (tooltipProgress != null && tooltipTime != null && duration > 0.0) {
            Box(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(0, 0) {
                            placeable.place(0, 0)
                        }
                    }
                    .graphicsLayer {
                        val xPos = (tooltipProgress * widthPx) - (size.width / 2f)
                        val maxX = (widthPx - size.width).coerceAtLeast(0f)
                        translationX = xPos.coerceIn(0f, maxX)
                        translationY = -size.height - 6.dp.toPx()
                    }
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = formatTime(tooltipTime),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

