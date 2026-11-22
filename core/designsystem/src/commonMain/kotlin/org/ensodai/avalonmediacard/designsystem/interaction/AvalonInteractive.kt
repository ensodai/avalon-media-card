package org.ensodai.avalonmediacard.designsystem.interaction

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.ensodai.avalonmediacard.designsystem.theme.LocalAvalonColors
import org.ensodai.avalonmediacard.designsystem.theme.LocalAvalonElevation
import kotlin.time.Duration.Companion.milliseconds

enum class AvalonInputModality {
    Pointer,   // Mouse / Trackpad on Web/Desktop
    Keyboard,  // Tab / Arrows navigation on PC
    TvDpad,    // 10-foot remote control on TV
    Touch      // Mobile touch screen
}

enum class AvalonInteractiveRole(
    val tvScale: Float,
    val isMediaCard: Boolean = false
) {
    Control(tvScale = 1.06f),
    PrimaryCta(tvScale = 1.06f),
    NavItem(tvScale = 1.04f),
    MediaWide(tvScale = 1.06f, isMediaCard = true),
    MediaPoster(tvScale = 1.08f, isMediaCard = true)
}

@Immutable
data class AvalonSurfaceColors(
    val default: Color = Color.Transparent,
    val hover: Color = Color.Unspecified,
    val pressed: Color = Color.Unspecified
)

@Stable
class AvalonInputModalityState(initial: AvalonInputModality) {
    var modality: AvalonInputModality by mutableStateOf(initial)
        internal set
}

val LocalAvalonInputModalityState = staticCompositionLocalOf {
    AvalonInputModalityState(AvalonInputModality.Pointer)
}

@Composable
fun AvalonInputModalityProvider(
    isTv: Boolean,
    content: @Composable () -> Unit
) {
    val state = remember(isTv) {
        AvalonInputModalityState(if (isTv) AvalonInputModality.TvDpad else AvalonInputModality.Pointer)
    }

    CompositionLocalProvider(LocalAvalonInputModalityState provides state) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (!isTv && event.type == KeyEventType.KeyDown && event.key.isAvalonNavigationKey()) {
                        if (state.modality != AvalonInputModality.Keyboard) {
                            state.modality = AvalonInputModality.Keyboard
                        }
                    }
                    false
                }
                .pointerInput(isTv) {
                    if (!isTv) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val pointerType = event.changes.firstOrNull()?.type
                                when {
                                    event.type == PointerEventType.Press && pointerType == PointerType.Touch -> {
                                        state.modality = AvalonInputModality.Touch
                                    }
                                    pointerType == PointerType.Mouse && (
                                        event.type == PointerEventType.Press ||
                                        (event.type == PointerEventType.Move && state.modality == AvalonInputModality.Touch)
                                    ) -> {
                                        state.modality = AvalonInputModality.Pointer
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            content()
        }
    }
}

private fun Key.isAvalonNavigationKey(): Boolean = when (this) {
    Key.Tab, Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight,
    Key.MoveHome, Key.MoveEnd, Key.PageUp, Key.PageDown -> true
    else -> false
}

/**
 * Universal interactive modifier for Core UI.
 * Single Source of Truth for Web Mouse, TV D-pad, Keyboard, and Touch.
 */
fun Modifier.avalonInteractive(
    role: AvalonInteractiveRole = AvalonInteractiveRole.Control,
    shape: Shape,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    semanticRole: Role? = null,
    surfaceColors: AvalonSurfaceColors = AvalonSurfaceColors(),
    onStateChange: ((isActive: Boolean) -> Unit)? = null
): Modifier = composed {
    val density = LocalDensity.current
    val isTv = AvalonTheme.isTv
    val modalityState = LocalAvalonInputModalityState.current
    val colors = LocalAvalonColors.current
    val elevation = LocalAvalonElevation.current

    var isFocused by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }
    var tvPressed by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isItemPressed by interactionSource.collectIsPressedAsState()
    val isItemHovered by interactionSource.collectIsHoveredAsState()

    val modality = if (isTv) AvalonInputModality.TvDpad else modalityState.modality

    val isMouseHovered = (isHovered || isItemHovered) && (modality == AvalonInputModality.Pointer)
    val isKeyboardFocused = isFocused && (modality == AvalonInputModality.Keyboard)
    val isTvActive = (isFocused || isHovered || isItemHovered) && (modality == AvalonInputModality.TvDpad)

    val isPressed = isItemPressed || tvPressed
    val isActive = isMouseHovered || isKeyboardFocused || isTvActive

    val tvPressEvents = remember { MutableSharedFlow<Boolean>(extraBufferCapacity = 10) }

    LaunchedEffect(Unit) {
        tvPressEvents.collectLatest { pressed ->
            if (pressed) {
                tvPressed = true
            } else {
                delay(100.milliseconds)
                tvPressed = false
            }
        }
    }

    LaunchedEffect(isActive) {
        onStateChange?.invoke(isActive)
    }

    val targetScale = when {
        isPressed && modality == AvalonInputModality.TvDpad -> 0.96f
        isPressed -> 0.98f
        isTvActive -> role.tvScale
        isMouseHovered && !role.isMediaCard -> 1.01f
        isMouseHovered && role.isMediaCard -> 1.02f
        else -> 1.0f
    }

    // Tonal Surface Elevation: on Web/Mouse we don't draw border, we elevate background!
    val targetBorderWidth: Dp = when {
        isTvActive -> elevation.focusBorderWidth // 2.dp White on TV
        isKeyboardFocused -> elevation.focusBorderWidth // 2.dp White on Keyboard
        isMouseHovered && role.isMediaCard -> elevation.borderWidth // Hairline border on media
        else -> 0.dp
    }

    val targetBorderColor: Color = when {
        isTvActive -> colors.borderFocus
        isKeyboardFocused -> colors.borderFocus
        isMouseHovered && role.isMediaCard -> colors.borderStrong
        else -> Color.Transparent
    }

    val targetBgColor: Color = when {
        isPressed -> {
            if (surfaceColors.pressed != Color.Unspecified) surfaceColors.pressed
            else colors.stateLayerPressed
        }
        isMouseHovered -> {
            if (surfaceColors.hover != Color.Unspecified) surfaceColors.hover
            else colors.stateLayerHover
        }
        else -> surfaceColors.default
    }

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (isPressed) tween(50) else spring(),
        label = "avalon_scale"
    )

    val animatedBg by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(120),
        label = "avalon_bg"
    )

    val animatedBorderWidth by animateDpAsState(
        targetValue = targetBorderWidth,
        animationSpec = tween(100),
        label = "avalon_border_w"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(100),
        label = "avalon_border_c"
    )

    this
        .zIndex(if (isActive) 1f else 0f)
        .onFocusChanged { isFocused = it.isFocused || it.hasFocus }
        .onPreviewKeyEvent { event ->
            if (enabled && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                when (event.type) {
                    KeyEventType.KeyDown -> {
                        if (onClick != null) {
                            tvPressEvents.tryEmit(true)
                            return@onPreviewKeyEvent true
                        }
                    }
                    KeyEventType.KeyUp -> {
                        if (onClick != null) {
                            tvPressEvents.tryEmit(false)
                            onClick.invoke()
                            return@onPreviewKeyEvent true
                        }
                    }
                }
            }
            false
        }
        .focusable(enabled = enabled)
        .pointerHoverIcon(
            if (onClick != null && enabled) PointerIcon.Hand else PointerIcon.Default,
            overrideDescendants = true
        )
        .then(
            if (enabled && onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = true,
                    role = semanticRole,
                    onClick = onClick
                )
            } else Modifier
        )
        .hoverable(
            interactionSource = interactionSource,
            enabled = enabled
        )
        .focusProperties { canFocus = enabled }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            cameraDistance = 12f * density.density
        }
        .then(
            if (animatedBorderWidth > 0.dp && animatedBorderColor != Color.Transparent) {
                Modifier.border(animatedBorderWidth, animatedBorderColor, shape)
            } else Modifier
        )
        .background(animatedBg, shape)
        .clip(shape)
}
