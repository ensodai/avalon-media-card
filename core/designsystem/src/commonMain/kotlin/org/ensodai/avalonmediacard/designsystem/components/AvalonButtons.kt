package org.ensodai.avalonmediacard.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.designsystem.interaction.AvalonInteractiveRole
import org.ensodai.avalonmediacard.designsystem.interaction.AvalonSurfaceColors
import org.ensodai.avalonmediacard.designsystem.interaction.avalonInteractive
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.ensodai.avalonmediacard.designsystem.theme.LocalAvalonContentColor

enum class AvalonButtonVariant { Primary, Secondary, Ghost }
enum class AvalonButtonSize { Hero, Standard, Compact }

@Immutable
data class AvalonButtonMetrics(
    val height: Dp,
    val horizontalPadding: Dp,
    val iconSideReduction: Dp,
    val iconSize: Dp,
    val iconGap: Dp
)

@Immutable
data class AvalonButtonColors(
    val surface: AvalonSurfaceColors,
    val contentColor: Color,
    val iconColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color
)

object AvalonButtonDefaults {
    @Composable
    fun metrics(size: AvalonButtonSize): AvalonButtonMetrics {
        val isTv = AvalonTheme.isTv
        return when (size) {
            AvalonButtonSize.Hero -> AvalonButtonMetrics(
                height = if (isTv) 52.dp else 44.dp,
                horizontalPadding = if (isTv) 28.dp else 24.dp,
                iconSideReduction = 4.dp,
                iconSize = if (isTv) 22.dp else 18.dp,
                iconGap = 8.dp
            )
            AvalonButtonSize.Standard -> AvalonButtonMetrics(
                height = if (isTv) 48.dp else 40.dp,
                horizontalPadding = if (isTv) 24.dp else 20.dp,
                iconSideReduction = 4.dp,
                iconSize = if (isTv) 20.dp else 18.dp,
                iconGap = 8.dp
            )
            AvalonButtonSize.Compact -> AvalonButtonMetrics(
                height = if (isTv) 40.dp else 32.dp,
                horizontalPadding = if (isTv) 16.dp else 12.dp,
                iconSideReduction = 2.dp,
                iconSize = if (isTv) 18.dp else 16.dp,
                iconGap = 6.dp
            )
        }
    }

    @Composable
    fun colors(variant: AvalonButtonVariant): AvalonButtonColors {
        val c = AvalonTheme.colors
        return when (variant) {
            AvalonButtonVariant.Primary -> AvalonButtonColors(
                surface = AvalonSurfaceColors(
                    default = c.accentPrimary,
                    hover = c.accentPrimaryHover,
                    pressed = c.accentPrimaryPressed
                ),
                contentColor = c.onAccentPrimary,
                iconColor = c.onAccentPrimary,
                disabledContainerColor = c.accentPrimary.copy(alpha = 0.35f),
                disabledContentColor = c.onAccentPrimary.copy(alpha = 0.6f)
            )
            AvalonButtonVariant.Secondary -> AvalonButtonColors(
                surface = AvalonSurfaceColors(
                    default = c.bgElevated,
                    hover = c.bgElevatedHover,
                    pressed = c.bgElevatedPressed
                ),
                contentColor = c.textPrimary,
                iconColor = c.textSecondary,
                disabledContainerColor = c.bgElevated.copy(alpha = 0.5f),
                disabledContentColor = c.textMuted
            )
            AvalonButtonVariant.Ghost -> AvalonButtonColors(
                surface = AvalonSurfaceColors(
                    default = Color.Transparent,
                    hover = c.stateLayerHover,
                    pressed = c.stateLayerPressed
                ),
                contentColor = c.textSecondary,
                iconColor = c.textSecondary,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = c.textMuted.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun AvalonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AvalonButtonVariant = AvalonButtonVariant.Secondary,
    size: AvalonButtonSize = AvalonButtonSize.Standard,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    colors: AvalonButtonColors = AvalonButtonDefaults.colors(variant)
) {
    AvalonButtonCore(
        text = text,
        onClick = onClick,
        shape = AvalonTheme.shapes.control, // 10.dp
        modifier = modifier,
        variant = variant,
        size = size,
        enabled = enabled,
        isLoading = isLoading,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        colors = colors
    )
}

@Composable
private fun AvalonButtonCore(
    text: String,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    variant: AvalonButtonVariant = AvalonButtonVariant.Secondary,
    size: AvalonButtonSize = AvalonButtonSize.Standard,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    colors: AvalonButtonColors = AvalonButtonDefaults.colors(variant)
) {
    val metrics = AvalonButtonDefaults.metrics(size)
    val interactiveRole = if (variant == AvalonButtonVariant.Primary) {
        AvalonInteractiveRole.PrimaryCta
    } else {
        AvalonInteractiveRole.Control
    }

    val startPad = if (leadingIcon != null) {
        metrics.horizontalPadding - metrics.iconSideReduction
    } else {
        metrics.horizontalPadding
    }

    val endPad = if (trailingIcon != null) {
        metrics.horizontalPadding - metrics.iconSideReduction
    } else {
        metrics.horizontalPadding
    }

    val activeSurface = if (enabled) {
        colors.surface
    } else {
        AvalonSurfaceColors(default = colors.disabledContainerColor)
    }

    val contentCol = if (enabled) colors.contentColor else colors.disabledContentColor
    val iconCol = if (enabled) colors.iconColor else colors.disabledContentColor

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(metrics.height)
            .avalonInteractive(
                role = interactiveRole,
                shape = shape,
                enabled = enabled && !isLoading,
                semanticRole = Role.Button,
                surfaceColors = activeSurface,
                onClick = onClick
            )
            .padding(PaddingValues(start = startPad, end = endPad))
    ) {
        // Zero layout shift when isLoading = true (content stays in composition)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer { alpha = if (isLoading) 0f else 1f }
        ) {
            if (leadingIcon != null) {
                CompositionLocalProvider(LocalAvalonContentColor provides iconCol) {
                    Box(Modifier.size(metrics.iconSize), contentAlignment = Alignment.Center) {
                        leadingIcon()
                    }
                }
                Spacer(Modifier.width(metrics.iconGap))
            }
            BasicText(
                text = text,
                style = AvalonTheme.typography.uiLabel.copy(color = contentCol),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null) {
                Spacer(Modifier.width(metrics.iconGap))
                CompositionLocalProvider(LocalAvalonContentColor provides iconCol) {
                    Box(Modifier.size(metrics.iconSize), contentAlignment = Alignment.Center) {
                        trailingIcon()
                    }
                }
            }
        }

        if (isLoading) {
            AvalonMicroSpinner(
                color = contentCol,
                size = metrics.iconSize
            )
        }
    }
}

@Composable
fun AvalonIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    variant: AvalonButtonVariant = AvalonButtonVariant.Secondary,
    size: AvalonButtonSize = AvalonButtonSize.Standard,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    colors: AvalonButtonColors = AvalonButtonDefaults.colors(variant),
    icon: @Composable () -> Unit
) {
    val metrics = AvalonButtonDefaults.metrics(size)
    val shape = AvalonTheme.shapes.control
    val activeSurface = if (enabled) {
        colors.surface
    } else {
        AvalonSurfaceColors(default = colors.disabledContainerColor)
    }
    val iconCol = if (enabled) colors.iconColor else colors.disabledContentColor

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(metrics.height) // Exact square: 44 / 40 / 32 dp
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else Modifier
            )
            .avalonInteractive(
                role = AvalonInteractiveRole.Control,
                shape = shape,
                enabled = enabled && !isLoading,
                semanticRole = Role.Button,
                surfaceColors = activeSurface,
                onClick = onClick
            )
    ) {
        if (isLoading) {
            AvalonMicroSpinner(color = iconCol, size = metrics.iconSize)
        } else {
            CompositionLocalProvider(LocalAvalonContentColor provides iconCol) {
                Box(Modifier.size(metrics.iconSize), contentAlignment = Alignment.Center) {
                    icon()
                }
            }
        }
    }
}

@Composable
fun AvalonSplitButton(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    onDropdownClick: () -> Unit,
    dropdownContentDescription: String,
    modifier: Modifier = Modifier,
    variant: AvalonButtonVariant = AvalonButtonVariant.Primary,
    size: AvalonButtonSize = AvalonButtonSize.Hero,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    chevronIcon: @Composable () -> Unit
) {
    val metrics = AvalonButtonDefaults.metrics(size)
    val colors = AvalonButtonDefaults.colors(variant)
    val startHalfShape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
    val endHalfShape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)
    val activeSurface = if (enabled) {
        colors.surface
    } else {
        AvalonSurfaceColors(default = colors.disabledContainerColor)
    }
    val dividerColor = if (variant == AvalonButtonVariant.Primary) {
        colors.contentColor.copy(alpha = 0.16f)
    } else {
        AvalonTheme.colors.borderStrong
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(metrics.height)
    ) {
        AvalonButtonCore(
            text = primaryText,
            onClick = onPrimaryClick,
            shape = startHalfShape,
            variant = variant,
            size = size,
            enabled = enabled,
            isLoading = isLoading,
            leadingIcon = leadingIcon,
            colors = colors
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(metrics.height)
                .background(activeSurface.default)
                .padding(vertical = metrics.height * 0.25f)
                .background(dividerColor)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .defaultMinSize(minWidth = metrics.height * 0.85f, minHeight = metrics.height)
                .semantics { contentDescription = dropdownContentDescription }
                .avalonInteractive(
                    role = AvalonInteractiveRole.Control,
                    shape = endHalfShape,
                    enabled = enabled && !isLoading,
                    semanticRole = Role.DropdownList,
                    surfaceColors = activeSurface,
                    onClick = onDropdownClick
                )
        ) {
            CompositionLocalProvider(LocalAvalonContentColor provides colors.iconColor) {
                Box(Modifier.size(metrics.iconSize), contentAlignment = Alignment.Center) {
                    chevronIcon()
                }
            }
        }
    }
}

@Composable
internal fun AvalonMicroSpinner(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "AvalonSpinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinnerAngle"
    )

    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        drawArc(
            color = color.copy(alpha = 0.22f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = stroke
        )
        drawArc(
            color = color,
            startAngle = angle,
            sweepAngle = 100f,
            useCenter = false,
            style = stroke
        )
    }
}
