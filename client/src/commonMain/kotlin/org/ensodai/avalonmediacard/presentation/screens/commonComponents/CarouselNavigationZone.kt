package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import org.ensodai.avalonmediacard.designsystem.components.AvalonNavigationZone

@Composable
fun BoxScope.CarouselNavigationZone(
    isRight: Boolean,
    isVisible: Boolean = true,
    onClick: () -> Unit
) {
    AvalonNavigationZone(
        isRight = isRight,
        isVisible = isVisible,
        onClick = onClick
    )
}

