package org.ensodai.avalonmediacard.presentation.screens.player.component.pc

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Volume1
import com.composables.icons.lucide.Volume2
import com.composables.icons.lucide.VolumeX
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VolumeControl(
    volume: Double,
    isMuted: Boolean,
    onVolumeChange: (Double) -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    val isSliderVisible = isHovered || isDragging

    // Animate the width of the slider area based on hover or dragging state
    val sliderWidth by animateDpAsState(
        targetValue = if (isSliderVisible) 80.dp else 0.dp,
        animationSpec = tween(durationMillis = 200)
    )

    val currentVolume = if (isMuted) 0.0 else volume.coerceIn(0.0, 1.0)

    val currentOnVolumeChange by rememberUpdatedState(onVolumeChange)
    val currentOnToggleMute by rememberUpdatedState(onToggleMute)
    val currentIsMuted by rememberUpdatedState(isMuted)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> isHovered = true
                            PointerEventType.Exit -> isHovered = false
                        }
                    }
                }
            }
    ) {
        // Volume Icon Button
        Box(
            modifier = Modifier
                .size(36.dp)
                .tvAndWebHoverEffect(
                    scaleTarget = 1.15f,
                    shape = CircleShape,
                    activeBorderColor = Color.Transparent,
                    onClick = { currentOnToggleMute() }
                )
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    isMuted || currentVolume == 0.0 -> Lucide.VolumeX
                    currentVolume < 0.5 -> Lucide.Volume1
                    else -> Lucide.Volume2
                },
                contentDescription = stringResource(Res.string.player_volume),
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        // Animated Slider
        if (sliderWidth > 0.dp) {
            Box(
                modifier = Modifier
                    .width(sliderWidth)
                    .height(36.dp)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerHoverIcon(PointerIcon.Hand)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    isDragging = true
                                    val canvasWidth = size.width.toFloat()
                                    if (canvasWidth > 0f) {
                                        val newVol = (down.position.x / canvasWidth).toDouble().coerceIn(0.0, 1.0)
                                        if (currentIsMuted && newVol > 0.0) {
                                            currentOnToggleMute()
                                        }
                                        currentOnVolumeChange(newVol)
                                    }

                                    val pointerId = down.id
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == pointerId }
                                        if (change == null || !change.pressed) {
                                            isDragging = false
                                            break
                                        } else {
                                            val currentWidth = size.width.toFloat()
                                            if (currentWidth > 0f) {
                                                val draggedVol = (change.position.x / currentWidth).toDouble().coerceIn(0.0, 1.0)
                                                if (currentIsMuted && draggedVol > 0.0) {
                                                    currentOnToggleMute()
                                                }
                                                currentOnVolumeChange(draggedVol)
                                            }
                                            change.consume()
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    val trackH = 4.dp.toPx()
                    val cornerRadius = CornerRadius(trackH / 2f, trackH / 2f)
                    val centerY = size.height / 2f
                    val fillWidth = size.width * currentVolume.toFloat()

                    // Background track
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.3f),
                        topLeft = Offset(0f, centerY - trackH / 2f),
                        size = Size(size.width, trackH),
                        cornerRadius = cornerRadius
                    )

                    // Fill track
                    if (fillWidth > 0f) {
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(0f, centerY - trackH / 2f),
                            size = Size(fillWidth, trackH),
                            cornerRadius = cornerRadius
                        )
                    }

                    // Thumb
                    val thumbRadius = 6.dp.toPx()
                    drawCircle(
                        color = Color.White,
                        radius = thumbRadius,
                        center = Offset(fillWidth, centerY)
                    )
                }
            }
        }
    }
}
