package org.ensodai.avalonmediacard.presentation.navigation


import androidx.compose.animation.core.CubicBezierEasing

object TelegramEasings {
    val Default = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
    val EaseOutQuint = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
}

object AvalonTransitions {
    const val PARALLAX_FACTOR = 0.1f
    const val BASE_DURATION_MS = 300
}
