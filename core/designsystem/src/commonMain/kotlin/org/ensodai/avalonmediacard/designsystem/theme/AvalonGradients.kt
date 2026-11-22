package org.ensodai.avalonmediacard.designsystem.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Obsidian Cinema - Easing Scrims (Law 6).
 * Multi-stop Larsen curves to avoid Mach bands and create smooth cinematic scrims
 * melting backdrops into bgCanvas without visible seams.
 */
object AvalonGradients {

    // 13-point canonical Larsen curve easing stops: [position, alphaFactor]
    private val LARSEN_STOPS = floatArrayOf(
        0.000f, 0.000f,
        0.100f, 0.015f,
        0.200f, 0.040f,
        0.300f, 0.085f,
        0.400f, 0.155f,
        0.500f, 0.250f,
        0.600f, 0.370f,
        0.700f, 0.515f,
        0.800f, 0.675f,
        0.875f, 0.805f,
        0.935f, 0.910f,
        0.975f, 0.970f,
        1.000f, 1.000f
    )

    fun larsenStops(
        color: Color,
        maxAlpha: Float = 1.0f,
        isReversed: Boolean = false
    ): Array<Pair<Float, Color>> {
        val count = LARSEN_STOPS.size / 2
        return Array(count) { i ->
            val stopIndex = if (isReversed) (count - 1 - i) else i
            val progress = LARSEN_STOPS[i * 2]
            val alphaFactor = LARSEN_STOPS[stopIndex * 2 + 1]
            val alpha = (alphaFactor * maxAlpha).coerceIn(0f, 1f)
            progress to color.copy(alpha = alpha)
        }
    }

    /**
     * Vertical scrim dissolving image into background color from top (transparent) to bottom (scrim).
     */
    fun larsenVerticalScrim(
        color: Color,
        maxAlpha: Float = 1.0f,
        startY: Float = 0.0f,
        endY: Float = Float.POSITIVE_INFINITY
    ): Brush {
        val stops = larsenStops(color = color, maxAlpha = maxAlpha, isReversed = false)
        return Brush.verticalGradient(
            colorStops = stops,
            startY = startY,
            endY = endY
        )
    }

    /**
     * Horizontal scrim dissolving edge.
     */
    fun larsenHorizontalScrim(
        color: Color,
        maxAlpha: Float = 1.0f,
        isReversed: Boolean = false,
        startX: Float = 0.0f,
        endX: Float = Float.POSITIVE_INFINITY
    ): Brush {
        val stops = larsenStops(color = color, maxAlpha = maxAlpha, isReversed = isReversed)
        return Brush.horizontalGradient(
            colorStops = stops,
            startX = startX,
            endX = endX
        )
    }
}
