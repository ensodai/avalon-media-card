package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.collectionButtonsSlot

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.designsystem.components.AvalonButtonSize
import org.ensodai.avalonmediacard.designsystem.components.AvalonButtonVariant
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonIconButton
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder

@Composable
fun SecondaryActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    tint: Color = AvalonTheme.colors.textPrimary,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    if (isLoading) {
        Box(
            modifier = modifier
                .size(44.dp)
                .shimmerPlaceholder(true, AvalonTheme.shapes.control)
        )
    } else {
        AvalonIconButton(
            onClick = onClick,
            modifier = modifier,
            variant = AvalonButtonVariant.Secondary,
            size = AvalonButtonSize.Standard
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
                AvalonIcon(
                    imageVector = targetIcon,
                    contentDescription = null,
                    tint = targetTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
