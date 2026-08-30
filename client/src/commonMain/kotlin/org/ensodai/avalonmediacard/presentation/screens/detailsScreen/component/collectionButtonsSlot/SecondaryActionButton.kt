package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.collectionButtonsSlot

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect

@Composable
fun SecondaryActionButton(
    icon: ImageVector,
    tint: Color = Color.White,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(46.dp)
            .shimmerPlaceholder(isLoading, CircleShape)
            .tvAndWebHoverEffect(
                scaleTarget = 1.08f,
                defaultBorderWidth = 1.dp,
                defaultBorderColor = Color.White.copy(alpha = 0.15f),
                activeBorderWidth = 1.dp,
                activeBorderColor = Color.White,
                shape = CircleShape,
                clickEnabled = !isLoading,
                onClick = { onClick() }
            )
            .background(if (isLoading) Color.Transparent else Color.White.copy(alpha = 0.08f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = icon to tint,
            transitionSpec = {
                scaleIn(
                    initialScale = 0.5f,
                    animationSpec = spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(100))
            },
            label = "icon_anim"
        ) { (targetIcon, targetTint) ->
            Icon(
                imageVector = targetIcon,
                contentDescription = null,
                tint = if (isLoading) Color.Transparent else targetTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
