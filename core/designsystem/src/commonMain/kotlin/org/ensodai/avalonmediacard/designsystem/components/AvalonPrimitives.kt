package org.ensodai.avalonmediacard.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.ensodai.avalonmediacard.designsystem.interaction.AvalonInteractiveRole
import org.ensodai.avalonmediacard.designsystem.interaction.AvalonSurfaceColors
import org.ensodai.avalonmediacard.designsystem.interaction.avalonInteractive
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.ensodai.avalonmediacard.designsystem.theme.LocalAvalonContentColor
import org.ensodai.avalonmediacard.designsystem.theme.LocalAvalonTextStyle

/**
 * Avalon Foundation Text:
 * Pure Compose Foundation replacement for Material 3 Text.
 * Inherits typography styles and resolves colors from LocalAvalonContentColor without M3 dependencies.
 */
@Composable
fun AvalonText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalAvalonTextStyle.current
) {
    val textColor = if (color.isSpecified) color else LocalAvalonContentColor.current
    val mergedStyle = style.merge(
        TextStyle(
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            fontStyle = fontStyle,
            letterSpacing = letterSpacing
        )
    )

    BasicText(
        text = text,
        modifier = modifier,
        style = mergedStyle,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines
    )
}

@Composable
fun AvalonText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalAvalonTextStyle.current
) {
    val textColor = if (color.isSpecified) color else LocalAvalonContentColor.current
    val mergedStyle = style.merge(
        TextStyle(
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            fontStyle = fontStyle,
            letterSpacing = letterSpacing
        )
    )

    BasicText(
        text = text,
        modifier = modifier,
        style = mergedStyle,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines
    )
}

/**
 * Avalon Foundation Icon:
 * Pure Compose Foundation replacement for Material 3 Icon.
 * Renders ImageVector or Painter without MaterialTheme or ripple overhead.
 */
@Composable
fun AvalonIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalAvalonContentColor.current
) {
    val colorFilter = if (tint.isSpecified) ColorFilter.tint(tint) else null
    val semantics = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    } else {
        Modifier.clearAndSetSemantics {}
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .then(semantics)
            .paint(
                painter = rememberVectorPainter(imageVector),
                colorFilter = colorFilter,
                contentScale = ContentScale.Fit
            )
    )
}

@Composable
fun AvalonIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalAvalonContentColor.current
) {
    val colorFilter = if (tint.isSpecified) ColorFilter.tint(tint) else null
    val semantics = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    } else {
        Modifier.clearAndSetSemantics {}
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .then(semantics)
            .paint(
                painter = painter,
                colorFilter = colorFilter,
                contentScale = ContentScale.Fit
            )
    )
}

/**
 * Avalon Circular Progress Indicator:
 * Pure Compose Foundation spinning ring indicator without Material 3 dependencies.
 */
@Composable
fun AvalonCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = AvalonTheme.colors.accentPrimary,
    strokeWidth: Dp = 3.dp
) {
    val transition = rememberInfiniteTransition()
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(
        modifier = modifier.size(36.dp)
    ) {
        val sweepAngle = 270f
        drawArc(
            color = color,
            startAngle = rotation,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Avalon Foundation Surface:
 * Pure Compose Foundation replacement for Material 3 Surface.
 * Draws background, optional border, clips to shape, provides LocalAvalonContentColor and handles optional click.
 */
@Composable
fun AvalonSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = AvalonTheme.colors.bgSurface,
    contentColor: Color = AvalonTheme.colors.textPrimary,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    val clickModifier = if (onClick != null) {
        val src = interactionSource ?: remember { MutableInteractionSource() }
        Modifier.clickable(
            interactionSource = src,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
    } else {
        Modifier
    }

    CompositionLocalProvider(
        LocalAvalonContentColor provides contentColor
    ) {
        Box(
            modifier = modifier
                .then(if (border != null) Modifier.border(border, shape) else Modifier)
                .background(color = color, shape = shape)
                .clip(shape)
                .then(clickModifier),
            propagateMinConstraints = false
        ) {
            content()
        }
    }
}

@Immutable
data class AvalonCardColors(val containerColor: Color, val contentColor: Color)

typealias CardColors = AvalonCardColors
typealias ButtonColors = AvalonButtonColors

object CardDefaults {
    @Composable
    fun cardColors(
        containerColor: Color = AvalonTheme.colors.bgSurface,
        contentColor: Color = AvalonTheme.colors.textPrimary
    ): AvalonCardColors = AvalonCardColors(containerColor, contentColor)

    @Composable
    fun cardElevation(defaultElevation: Dp = 0.dp): Dp = defaultElevation
}

/**
 * Avalon Foundation Card:
 * Pure Compose Foundation replacement for Material 3 Card.
 */
@Composable
fun AvalonCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    colors: AvalonCardColors = CardDefaults.cardColors(),
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    AvalonSurface(
        modifier = modifier,
        shape = shape,
        color = colors.containerColor,
        contentColor = colors.contentColor,
        border = border,
        onClick = onClick,
        enabled = enabled,
        content = content
    )
}

object ButtonDefaults {
    @Composable
    fun buttonColors(
        containerColor: Color = AvalonTheme.colors.accentPrimary,
        contentColor: Color = AvalonTheme.colors.onAccentPrimary,
        disabledContainerColor: Color = containerColor.copy(alpha = 0.35f),
        disabledContentColor: Color = contentColor.copy(alpha = 0.5f)
    ): AvalonButtonColors {
        return AvalonButtonColors(
            surface = AvalonSurfaceColors(
                default = containerColor,
                hover = containerColor.copy(alpha = 0.88f),
                pressed = containerColor.copy(alpha = 0.75f)
            ),
            contentColor = contentColor,
            iconColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )
    }
}

/**
 * Avalon Foundation Button (RowScope slot overload):
 * Replaces Material 3 Button(onClick = ...) { ... } with Foundation implementation.
 */
@Composable
fun AvalonButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(10.dp),
    colors: AvalonButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit
) {
    val activeSurface = if (enabled) {
        colors.surface
    } else {
        AvalonSurfaceColors(default = colors.disabledContainerColor)
    }

    Box(
        modifier = modifier
            .avalonInteractive(
                role = AvalonInteractiveRole.Control,
                shape = shape,
                enabled = enabled,
                semanticRole = Role.Button,
                surfaceColors = activeSurface,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalAvalonContentColor provides if (enabled) colors.contentColor else colors.disabledContentColor
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                content()
            }
        }
    }
}

/**
 * Avalon Foundation IconButton (slot overload):
 * Replaces Material 3 IconButton(onClick = ...) { ... } with Foundation implementation.
 */
@Composable
fun AvalonIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val colors = AvalonButtonDefaults.colors(AvalonButtonVariant.Ghost)
    val activeSurface = if (enabled) {
        colors.surface
    } else {
        AvalonSurfaceColors(default = colors.disabledContainerColor)
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else Modifier
            )
            .avalonInteractive(
                role = AvalonInteractiveRole.Control,
                shape = shape,
                enabled = enabled,
                semanticRole = Role.Button,
                surfaceColors = activeSurface,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalAvalonContentColor provides if (enabled) colors.contentColor else colors.disabledContentColor
        ) {
            content()
        }
    }
}

/**
 * Avalon Foundation Switch:
 * Replaces Material 3 Switch with animated Pure Foundation toggle.
 */
@Composable
fun AvalonSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackColor by animateColorAsState(
        if (checked) AvalonTheme.colors.accentPrimary else AvalonTheme.colors.bgElevated
    )
    val thumbColor = if (checked) AvalonTheme.colors.onAccentPrimary else AvalonTheme.colors.textSecondary
    val thumbOffset by animateDpAsState(
        if (checked) 22.dp else 2.dp
    )

    Box(
        modifier = modifier
            .size(width = 46.dp, height = 26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(trackColor)
            .border(1.dp, AvalonTheme.colors.borderSubtle, RoundedCornerShape(13.dp))
            .then(
                if (onCheckedChange != null) {
                    Modifier.clickable(enabled = enabled, role = Role.Switch) {
                        onCheckedChange(!checked)
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .size(20.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

/**
 * Avalon Foundation Snackbar State & Host
 */
@Stable
class AvalonSnackbarHostState {
    var currentMessage by mutableStateOf<String?>(null)
        private set

    suspend fun showSnackbar(message: String) {
        currentMessage = message
        delay(3500)
        if (currentMessage == message) {
            currentMessage = null
        }
    }
}

@Composable
fun AvalonSnackbarHost(
    hostState: AvalonSnackbarHostState,
    modifier: Modifier = Modifier
) {
    val message = hostState.currentMessage
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier.padding(16.dp)
    ) {
        if (message != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(AvalonTheme.colors.bgElevated)
                    .border(1.dp, AvalonTheme.colors.borderSubtle, RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                AvalonText(
                    text = message,
                    style = AvalonTheme.typography.uiLabel,
                    color = AvalonTheme.colors.textPrimary
                )
            }
        }
    }
}

/**
 * Avalon Foundation Divider:
 * 1dp subtle hairline divider.
 */
@Composable
fun AvalonDivider(
    modifier: Modifier = Modifier,
    color: Color = AvalonTheme.colors.borderSubtle,
    thickness: Dp = 1.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}
