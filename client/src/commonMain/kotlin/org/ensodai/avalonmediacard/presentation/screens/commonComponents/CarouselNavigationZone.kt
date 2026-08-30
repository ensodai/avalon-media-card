package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide

@Composable
fun BoxScope.CarouselNavigationZone(
    isRight: Boolean,
    onClick: () -> Unit
) {
    if (LocalDeviceTarget.current.isTv) return

    val backgroundColor = MaterialTheme.colorScheme.background

    // Формируем градиент для плавного скрытия карточек
    val gradientColors = if (isRight) {
        listOf(Color.Transparent, backgroundColor.copy(alpha = 0.8f), backgroundColor)
    } else {
        listOf(backgroundColor, backgroundColor.copy(alpha = 0.8f), Color.Transparent)
    }

    // 1. Внешний Box берет размеры LazyRow (matchParentSize определяет точные границы всей карусели по высоте)
    Box(
        modifier = Modifier
            .matchParentSize()
    ) {

        // 2. Внутренний Box прижимается к краю, забирает всю высоту родителя и нужную ширину
        Box(
            modifier = Modifier
                .align(if (isRight) Alignment.CenterEnd else Alignment.CenterStart)
                .fillMaxHeight() // Идеально растянет по высоте карточек
                .width(80.dp)    // Широкая удобная зона для клика
                .background(Brush.horizontalGradient(colors = gradientColors))
                .clickable(onClick = onClick)
                .pointerHoverIcon(PointerIcon.Hand),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRight) Lucide.ChevronRight else Lucide.ChevronLeft,
                contentDescription = if (isRight) "Forward" else "Back",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(48.dp)
                    .padding(
                        start = if (isRight) 16.dp else 0.dp,
                        end = if (isRight) 0.dp else 16.dp
                    )
            )
        }
    }
}