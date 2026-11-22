package org.ensodai.avalonmediacard.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import org.ensodai.avalonmediacard.designsystem.interaction.AvalonInteractiveRole
import org.ensodai.avalonmediacard.designsystem.interaction.AvalonSurfaceColors
import org.ensodai.avalonmediacard.designsystem.interaction.avalonInteractive
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

/**
 * Universal (0, 0) provider for fullscreen modal surfaces.
 */
object AvalonFullscreenPopupPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = IntOffset.Zero
}

/**
 * Canonical Modal Dialog container for «Obsidian Cinema».
 * Features:
 * - Fullscreen Popup layer with deep cinema scrim (#000000 @ 72%)
 * - 16dp rounded corner container with bgOverlay and top 1dp sheen (Linear style)
 * - Safe dismiss on click outside and Back / Escape key handling
 * - Focus trapping and clean return to caller on dismiss
 */
@Composable
fun AvalonDialog(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    callerFocusRequester: FocusRequester? = null,
    initialFocusRequester: FocusRequester? = null,
    dismissOnClickOutside: Boolean = true,
    scrimColor: Color = AvalonTheme.colors.bgPureCinema.copy(alpha = 0.72f),
    content: @Composable BoxScope.() -> Unit
) {
    if (!isOpen) return

    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes
    val modalContainerRequester = remember { FocusRequester() }

    fun safeDismiss() {
        runCatching { callerFocusRequester?.requestFocus() }
        onDismissRequest()
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { callerFocusRequester?.requestFocus() }
        }
    }

    Popup(
        popupPositionProvider = AvalonFullscreenPopupPositionProvider,
        properties = PopupProperties(
            focusable = true,
            dismissOnClickOutside = false,
            clippingEnabled = false
        ),
        onDismissRequest = { safeDismiss() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && (event.key == Key.Back || event.key == Key.Escape)) {
                        safeDismiss()
                        true
                    } else {
                        false
                    }
                }
                .pointerInput(dismissOnClickOutside) {
                    detectTapGestures {
                        if (dismissOnClickOutside) {
                            safeDismiss()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = modifier
                    .pointerInput(Unit) { detectTapGestures { } }
                    .focusRequester(modalContainerRequester)
                    .focusProperties {
                        onExit = {
                            cancelFocusChange()
                        }
                    }
                    .then(
                        if (initialFocusRequester != null) {
                            Modifier.focusRestorer(initialFocusRequester)
                        } else {
                            Modifier.focusRestorer()
                        }
                    )
                    .focusGroup()
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .clip(shapes.container)
                    .background(colors.bgOverlay)
                    .border(1.dp, colors.borderOverlay, shapes.container)
                    .drawWithContent {
                        drawContent()
                        drawLine(
                            color = colors.borderOverlayHighlight,
                            start = Offset(16.dp.toPx(), 1.dp.toPx()),
                            end = Offset(size.width - 16.dp.toPx(), 1.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (title != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                AvalonText(
                                    text = title,
                                    style = AvalonTheme.typography.sectionTitle,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (subtitle != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    AvalonText(
                                        text = subtitle,
                                        style = AvalonTheme.typography.metadata,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            AvalonIconButton(
                                onClick = { safeDismiss() },
                                contentDescription = "Close",
                                modifier = Modifier.size(36.dp)
                            ) {
                                AvalonIcon(
                                    imageVector = Lucide.X,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = colors.textSecondary
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        content()
                    }
                }
            }

            LaunchedEffect(Unit) {
                val target = initialFocusRequester ?: modalContainerRequester
                runCatching { target.requestFocus() }
            }
        }
    }
}

enum class AvalonPopupAnchorSide {
    RIGHT,
    LEFT,
    BOTTOM
}

/**
 * Smart anchor-based position provider that flips when screen boundary is reached.
 */
class AvalonSideAnchorPopupPositionProvider(
    private val preferredSide: AvalonPopupAnchorSide = AvalonPopupAnchorSide.RIGHT,
    private val horizontalMarginPx: Int = 12,
    private val verticalMarginPx: Int = 8,
    private val screenPaddingPx: Int = 16
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val fitsRight = anchorBounds.right + horizontalMarginPx + popupContentSize.width <= windowSize.width - screenPaddingPx
        val fitsLeft = anchorBounds.left - horizontalMarginPx - popupContentSize.width >= screenPaddingPx

        val x: Int
        val y: Int

        when (preferredSide) {
            AvalonPopupAnchorSide.RIGHT -> {
                if (fitsRight) {
                    x = anchorBounds.right + horizontalMarginPx
                    y = anchorBounds.top.coerceIn(
                        screenPaddingPx,
                        (windowSize.height - popupContentSize.height - screenPaddingPx).coerceAtLeast(screenPaddingPx)
                    )
                } else if (fitsLeft) {
                    x = anchorBounds.left - horizontalMarginPx - popupContentSize.width
                    y = anchorBounds.top.coerceIn(
                        screenPaddingPx,
                        (windowSize.height - popupContentSize.height - screenPaddingPx).coerceAtLeast(screenPaddingPx)
                    )
                } else {
                    x = (anchorBounds.right - popupContentSize.width)
                        .coerceIn(screenPaddingPx, (windowSize.width - popupContentSize.width - screenPaddingPx).coerceAtLeast(screenPaddingPx))
                    y = if (anchorBounds.bottom + popupContentSize.height + verticalMarginPx <= windowSize.height - screenPaddingPx) {
                        anchorBounds.bottom + verticalMarginPx
                    } else {
                        (anchorBounds.top - popupContentSize.height - verticalMarginPx).coerceAtLeast(screenPaddingPx)
                    }
                }
            }
            AvalonPopupAnchorSide.LEFT -> {
                if (fitsLeft) {
                    x = anchorBounds.left - horizontalMarginPx - popupContentSize.width
                    y = anchorBounds.top.coerceIn(
                        screenPaddingPx,
                        (windowSize.height - popupContentSize.height - screenPaddingPx).coerceAtLeast(screenPaddingPx)
                    )
                } else if (fitsRight) {
                    x = anchorBounds.right + horizontalMarginPx
                    y = anchorBounds.top.coerceIn(
                        screenPaddingPx,
                        (windowSize.height - popupContentSize.height - screenPaddingPx).coerceAtLeast(screenPaddingPx)
                    )
                } else {
                    x = anchorBounds.left
                        .coerceIn(screenPaddingPx, (windowSize.width - popupContentSize.width - screenPaddingPx).coerceAtLeast(screenPaddingPx))
                    y = if (anchorBounds.bottom + popupContentSize.height + verticalMarginPx <= windowSize.height - screenPaddingPx) {
                        anchorBounds.bottom + verticalMarginPx
                    } else {
                        (anchorBounds.top - popupContentSize.height - verticalMarginPx).coerceAtLeast(screenPaddingPx)
                    }
                }
            }
            AvalonPopupAnchorSide.BOTTOM -> {
                x = (anchorBounds.right - popupContentSize.width)
                    .coerceIn(screenPaddingPx, (windowSize.width - popupContentSize.width - screenPaddingPx).coerceAtLeast(screenPaddingPx))
                y = if (anchorBounds.bottom + popupContentSize.height + verticalMarginPx <= windowSize.height - screenPaddingPx) {
                    anchorBounds.bottom + verticalMarginPx
                } else {
                    (anchorBounds.top - popupContentSize.height - verticalMarginPx).coerceAtLeast(screenPaddingPx)
                }
            }
        }

        return IntOffset(x, y)
    }
}

@Composable
fun rememberAvalonSideAnchorPopupPositionProvider(
    preferredSide: AvalonPopupAnchorSide = AvalonPopupAnchorSide.RIGHT,
    horizontalMargin: Dp = 12.dp,
    verticalMargin: Dp = 8.dp,
    screenPadding: Dp = 16.dp
): PopupPositionProvider {
    val density = LocalDensity.current
    return remember(preferredSide, horizontalMargin, verticalMargin, screenPadding, density) {
        AvalonSideAnchorPopupPositionProvider(
            preferredSide = preferredSide,
            horizontalMarginPx = with(density) { horizontalMargin.roundToPx() },
            verticalMarginPx = with(density) { verticalMargin.roundToPx() },
            screenPaddingPx = with(density) { screenPadding.roundToPx() }
        )
    }
}

/**
 * Canonical Dropdown Menu container for «Obsidian Cinema».
 */
@Composable
fun AvalonDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset.Zero,
    popupPositionProvider: PopupPositionProvider? = null,
    width: Dp = 240.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!expanded) return

    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    val popupContent = @Composable {
        Column(
            modifier = modifier
                .width(width)
                .clip(shapes.control)
                .background(colors.bgOverlay)
                .border(1.dp, colors.borderOverlay, shapes.control)
                .drawWithContent {
                    drawContent()
                    drawLine(
                        color = colors.borderOverlayHighlight,
                        start = Offset(10.dp.toPx(), 1.dp.toPx()),
                        end = Offset(size.width - 10.dp.toPx(), 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .padding(vertical = 6.dp)
        ) {
            content()
        }
    }

    if (popupPositionProvider != null) {
        Popup(
            popupPositionProvider = popupPositionProvider,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true),
            content = popupContent
        )
    } else {
        Popup(
            alignment = alignment,
            offset = offset,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true),
            content = popupContent
        )
    }
}

/**
 * Canonical Dropdown Menu Item.
 */
@Composable
fun AvalonDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingText: String? = null,
    enabled: Boolean = true,
    textColor: Color? = null,
    iconColor: Color? = null
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    val surfaceColors = AvalonSurfaceColors(
        default = Color.Transparent,
        hover = colors.stateLayerHover,
        pressed = colors.stateLayerPressed
    )

    val resolvedTextColor = textColor ?: if (enabled) colors.textPrimary else colors.textMuted
    val resolvedIconColor = iconColor ?: if (enabled) colors.textPrimary else colors.textMuted

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .avalonInteractive(
                role = AvalonInteractiveRole.Control,
                shape = shapes.micro,
                enabled = enabled,
                semanticRole = Role.DropdownList,
                surfaceColors = surfaceColors,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            AvalonIcon(
                imageVector = icon,
                contentDescription = null,
                tint = resolvedIconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        AvalonText(
            text = text,
            style = AvalonTheme.typography.uiLabel,
            color = resolvedTextColor,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (trailingText != null) {
            Spacer(modifier = Modifier.width(8.dp))
            AvalonText(
                text = trailingText,
                style = AvalonTheme.typography.microCaption,
                color = colors.textMuted
            )
        }
    }
}

/**
 * Canonical Right Slide-over Drawer for «Obsidian Cinema».
 * Operates with smooth horizontal slide and full TV/Desktop focus support.
 */
@Composable
fun AvalonRightDrawer(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = if (AvalonTheme.isTv) 380.dp else 420.dp,
    title: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    callerFocusRequester: FocusRequester? = null,
    initialFocusRequester: FocusRequester? = null,
    content: @Composable BoxScope.() -> Unit
) {
    if (!isOpen) return

    val colors = AvalonTheme.colors
    val drawerRootRequester = remember { FocusRequester() }

    fun safeDismiss() {
        runCatching { callerFocusRequester?.requestFocus() }
        onDismissRequest()
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { callerFocusRequester?.requestFocus() }
        }
    }

    Popup(
        popupPositionProvider = AvalonFullscreenPopupPositionProvider,
        properties = PopupProperties(
            focusable = true,
            dismissOnClickOutside = false,
            clippingEnabled = false
        ),
        onDismissRequest = { safeDismiss() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bgPureCinema.copy(alpha = 0.72f))
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && (event.key == Key.Back || event.key == Key.Escape)) {
                        safeDismiss()
                        true
                    } else {
                        false
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { safeDismiss() }
                },
            contentAlignment = Alignment.CenterEnd
        ) {
            AnimatedVisibility(
                visible = isOpen,
                enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(280)) + fadeIn(tween(200)),
                exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(240)) + fadeOut(tween(180))
            ) {
                Box(
                    modifier = modifier
                        .width(width)
                        .fillMaxHeight()
                        .pointerInput(Unit) { detectTapGestures { } }
                        .focusRequester(drawerRootRequester)
                        .focusProperties {
                            onExit = {
                                cancelFocusChange()
                            }
                        }
                        .then(
                            if (initialFocusRequester != null) {
                                Modifier.focusRestorer(initialFocusRequester)
                            } else {
                                Modifier.focusRestorer()
                            }
                        )
                        .focusGroup()
                        .background(colors.bgOverlay)
                        .border(1.dp, colors.borderOverlay)
                        .padding(24.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        if (title != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (icon != null) {
                                    AvalonIcon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = colors.accentPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    AvalonText(
                                        text = title,
                                        style = AvalonTheme.typography.sectionTitle,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (subtitle != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        AvalonText(
                                            text = subtitle,
                                            style = AvalonTheme.typography.metadata,
                                            color = colors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                AvalonIconButton(
                                    onClick = { safeDismiss() },
                                    contentDescription = "Close",
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    AvalonIcon(
                                        imageVector = Lucide.X,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = colors.textSecondary
                                    )
                                }
                            }

                            AvalonDivider(modifier = Modifier.padding(bottom = 16.dp))
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            content()
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                val target = initialFocusRequester ?: drawerRootRequester
                runCatching { target.requestFocus() }
            }
        }
    }
}
