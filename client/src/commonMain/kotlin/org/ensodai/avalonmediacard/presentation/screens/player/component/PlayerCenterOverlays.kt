package org.ensodai.avalonmediacard.presentation.screens.player.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import avalonmediacard.client.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RotateCcw
import com.composables.icons.lucide.RotateCw
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import org.ensodai.avalonmediacard.core.PlaybackController
import org.ensodai.avalonmediacard.core.openInExternalPlayer
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget

private enum class SeekDirection {
    BACKWARD, FORWARD
}

@Composable
fun PlayerCenterOverlays(
    modifier: Modifier = Modifier,
    controller: PlaybackController,
    url: String?,
    title: String,
    errorOverride: String?,
    onTap: (() -> Unit)? = null,
) {
    val deviceTarget = LocalDeviceTarget.current
    val isTouch = deviceTarget.isTouch

    val isPlaying = controller.state.isPlaying
    val playbackError = errorOverride ?: controller.state.playbackError
    val isBuffering = controller.state.isBuffering || url.isNullOrBlank()


    var seekDirection by remember { mutableStateOf<SeekDirection?>(null) }
    var accumulatedSeconds by remember { mutableIntStateOf(0) }
    var seekTrigger by remember { mutableLongStateOf(0L) }

    fun triggerSeek(direction: SeekDirection) {
        if (seekDirection == direction) {
            accumulatedSeconds += 10
        } else {
            seekDirection = direction
            accumulatedSeconds = 10
        }
        seekTrigger = Clock.System.now().toEpochMilliseconds()
    }

    // Таймер накопления перемотки (YouTube Style для сенсорных экранов):
    // При серии тапов (10s -> 20s -> 30s) таймер сбрасывается и ждет окончания тапов,
    // после чего выполняет однократный seek и плавно прячет плашку.
    LaunchedEffect(seekTrigger) {
        if (isTouch && seekTrigger > 0L && seekDirection != null && accumulatedSeconds > 0) {
            delay(650.milliseconds)
            val current = controller.state.currentTime
            val duration = controller.state.duration
            val target = when (seekDirection) {
                SeekDirection.BACKWARD -> (current - accumulatedSeconds).coerceAtLeast(0.0)
                SeekDirection.FORWARD -> if (duration > 0.0) (current + accumulatedSeconds).coerceAtMost(duration) else current + accumulatedSeconds
                null -> current
            }
            controller.seek(target)
            delay(200.milliseconds)
            seekDirection = null
            accumulatedSeconds = 0
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Мягкая подсветка стороны перемотки (полупрозрачный градиент в стиле YouTube) - только для сенсорных устройств
        if (isTouch && seekDirection == SeekDirection.BACKWARD) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.42f)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
                        )
                    )
            )
        } else if (isTouch && seekDirection == SeekDirection.FORWARD) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.42f)
                    .align(Alignment.CenterEnd)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.12f))
                        )
                    )
            )
        }

        // Кликабельная зона: на мобильных устройствах поддержка двойного тапа и мультитапа, на ПК/Веб — мгновенный клик
        if (isTouch) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                val width = size.width
                                if (seekDirection != null) {
                                    if (seekDirection == SeekDirection.BACKWARD && offset.x < width * 0.45f) {
                                        triggerSeek(SeekDirection.BACKWARD)
                                    } else if (seekDirection == SeekDirection.FORWARD && offset.x > width * 0.55f) {
                                        triggerSeek(SeekDirection.FORWARD)
                                    } else {
                                        if (onTap != null) {
                                            onTap()
                                        } else {
                                            if (isPlaying) controller.pause() else controller.play()
                                        }
                                    }
                                } else {
                                    if (onTap != null) {
                                        onTap()
                                    } else {
                                        if (isPlaying) controller.pause() else controller.play()
                                    }
                                }
                            },
                            onDoubleTap = { offset ->
                                val width = size.width
                                if (offset.x < width * 0.4f) {
                                    triggerSeek(SeekDirection.BACKWARD)
                                } else if (offset.x > width * 0.6f) {
                                    triggerSeek(SeekDirection.FORWARD)
                                } else {
                                    if (onTap != null) onTap() else {
                                        if (isPlaying) controller.pause() else controller.play()
                                    }
                                }
                            }
                        )
                    }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (onTap != null) {
                            onTap()
                        } else {
                            if (isPlaying) controller.pause() else controller.play()
                        }
                    }
            )
        }

        // Индикатор перемотки назад (-10, -20, -30 сек) - только для сенсорных устройств
        AnimatedVisibility(
            visible = isTouch && seekDirection == SeekDirection.BACKWARD,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) + scaleIn(initialScale = 0.82f),
            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.82f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp))
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Lucide.RotateCcw,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Lucide.ChevronLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(Res.string.player_seconds_short_fmt, "-$accumulatedSeconds"),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Индикатор перемотки вперед (+10, +20, +30 сек) - только для сенсорных устройств
        AnimatedVisibility(
            visible = isTouch && seekDirection == SeekDirection.FORWARD,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) + scaleIn(initialScale = 0.82f),
            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.82f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp))
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Lucide.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Lucide.RotateCw,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(Res.string.player_seconds_short_fmt, "+$accumulatedSeconds"),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (playbackError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = playbackError,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { if (url != null) openInExternalPlayer(url, title) }) {
                        Icon(imageVector = Lucide.ExternalLink, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.player_open_external))
                    }
                }
            }
        } else if (isBuffering) {
            val bgAlpha = if (url.isNullOrBlank()) 0.85f else 0.45f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = bgAlpha)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
        }


        // Мультиплатформенный оверлей субтитров в commonMain
        val currentSubtitleText = controller.state.currentSubtitleText
        if (!currentSubtitleText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = currentSubtitleText,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black,
                            offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                )
            }
        }
    }
}
