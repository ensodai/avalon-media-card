package org.ensodai.avalonmediacard.presentation.components


import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp


fun Modifier.shimmerPlaceholder(
    isLoading: Boolean,
    shape: Shape = RoundedCornerShape(4.dp)
): Modifier = composed {
    if (isLoading) {
        this
            .clip(shape)
            .shimmerEffect()
    } else {
        this
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.outline
    val shimmerColors = listOf(
        baseColor.copy(alpha = 0.4f),
        highlightColor.copy(alpha = 0.6f),
        baseColor.copy(alpha = 0.4f)
    )

    Modifier.drawBehind {
        val translate = translateAnim
        val brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translate - 200f, translate - 200f),
            end = Offset(translate + 200f, translate + 200f)
        )
        drawRect(brush = brush)
    }
}

