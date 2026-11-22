package org.ensodai.avalonmediacard.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.designsystem.interaction.AvalonInteractiveRole
import org.ensodai.avalonmediacard.designsystem.interaction.AvalonSurfaceColors
import org.ensodai.avalonmediacard.designsystem.interaction.avalonInteractive
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

/**
 * 1. Concentric Segmented Control according to DESIGN.md v2.1.
 * Outer container radius 12.dp, padding 3.dp -> Inner segment thumb radius 9.dp.
 */
@Composable
fun <T> AvalonSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    itemLabel: @Composable (T) -> Unit = { item ->
        BasicText(
            text = item.toString(),
            style = AvalonTheme.typography.uiLabel.copy(
                color = if (item == selectedItem) AvalonTheme.colors.textPrimary else AvalonTheme.colors.textSecondary,
                fontWeight = if (item == selectedItem) FontWeight.SemiBold else FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    },
    leadingIcon: (@Composable (T) -> Unit)? = null,
    trailingBadge: (@Composable (T) -> Unit)? = null
) {
    val colors = AvalonTheme.colors
    val outerShape = AvalonTheme.shapes.media // 12.dp
    val innerShape = AvalonTheme.shapes.segmentThumb // 8.dp (concentric)

    Row(
        modifier = modifier
            .clip(outerShape)
            .background(colors.bgSurface)
            .border(AvalonTheme.elevation.borderWidth, colors.borderSubtle, outerShape)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEach { item ->
            val isSelected = item == selectedItem

            val segmentBg by animateColorAsState(
                targetValue = if (isSelected) colors.bgElevated else Color.Transparent,
                label = "SegmentBg"
            )
            val segmentBorder by animateColorAsState(
                targetValue = if (isSelected) colors.borderStrong.copy(alpha = 0.6f) else Color.Transparent,
                label = "SegmentBorder"
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(innerShape)
                    .background(segmentBg)
                    .border(AvalonTheme.elevation.borderWidth, segmentBorder, innerShape)
                    .avalonInteractive(
                        role = AvalonInteractiveRole.Tab,
                        shape = innerShape,
                        enabled = enabled,
                        semanticRole = Role.Tab,
                        surfaceColors = AvalonSurfaceColors(
                            default = segmentBg,
                            hover = if (isSelected) colors.bgElevated else colors.bgSurface.copy(alpha = 0.5f)
                        ),
                        onClick = { if (enabled) onItemSelected(item) }
                    )
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    leadingIcon(item)
                    Spacer(Modifier.width(6.dp))
                }
                itemLabel(item)
                if (trailingBadge != null) {
                    Spacer(Modifier.width(6.dp))
                    trailingBadge(item)
                }
            }
        }
    }
}

/**
 * 2. Scrollable Tab Row for variable-width or many items (e.g. TV seasons).
 */
@Composable
fun <T> AvalonScrollableTabRow(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    itemLabel: @Composable (T) -> Unit = { item ->
        BasicText(
            text = item.toString(),
            style = AvalonTheme.typography.uiLabel.copy(
                color = if (item == selectedItem) AvalonTheme.colors.textPrimary else AvalonTheme.colors.textSecondary,
                fontWeight = if (item == selectedItem) FontWeight.SemiBold else FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    },
    leadingIcon: (@Composable (T) -> Unit)? = null,
    trailingBadge: (@Composable (T) -> Unit)? = null
) {
    val colors = AvalonTheme.colors
    val outerShape = AvalonTheme.shapes.media // 12.dp
    val innerShape = AvalonTheme.shapes.segmentThumb // 8.dp (concentric)
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .clip(outerShape)
            .background(colors.bgSurface)
            .border(AvalonTheme.elevation.borderWidth, colors.borderSubtle, outerShape)
            .padding(3.dp)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { item ->
            val isSelected = item == selectedItem

            val segmentBg by animateColorAsState(
                targetValue = if (isSelected) colors.bgElevated else Color.Transparent,
                label = "ScrollableTabBg"
            )
            val segmentBorder by animateColorAsState(
                targetValue = if (isSelected) colors.borderStrong.copy(alpha = 0.6f) else Color.Transparent,
                label = "ScrollableTabBorder"
            )

            Row(
                modifier = Modifier
                    .height(34.dp)
                    .clip(innerShape)
                    .background(segmentBg)
                    .border(AvalonTheme.elevation.borderWidth, segmentBorder, innerShape)
                    .avalonInteractive(
                        role = AvalonInteractiveRole.Tab,
                        shape = innerShape,
                        enabled = enabled,
                        semanticRole = Role.Tab,
                        surfaceColors = AvalonSurfaceColors(
                            default = segmentBg,
                            hover = if (isSelected) colors.bgElevated else colors.bgSurface.copy(alpha = 0.5f)
                        ),
                        onClick = { if (enabled) onItemSelected(item) }
                    )
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    leadingIcon(item)
                    Spacer(Modifier.width(6.dp))
                }
                itemLabel(item)
                if (trailingBadge != null) {
                    Spacer(Modifier.width(6.dp))
                    trailingBadge(item)
                }
            }
        }
    }
}

/**
 * 3. Avalon Badge (Micro chip) for 4K, HDR, NEW, timecodes and status indicators.
 */
enum class AvalonBadgeVariant {
    Default,
    Accent,
    Success,
    Warning,
    Glass
}

@Composable
fun AvalonBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: AvalonBadgeVariant = AvalonBadgeVariant.Default,
    shape: Shape = AvalonTheme.shapes.micro, // 4.dp
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val colors = AvalonTheme.colors

    val (bg, textColor, borderColor) = when (variant) {
        AvalonBadgeVariant.Default -> Triple(
            colors.bgElevated,
            colors.textSecondary,
            colors.borderSubtle
        )
        AvalonBadgeVariant.Accent -> Triple(
            colors.accentPrimary.copy(alpha = 0.15f),
            colors.accentPrimary,
            colors.accentPrimary.copy(alpha = 0.35f)
        )
        AvalonBadgeVariant.Success -> Triple(
            colors.statusLive.copy(alpha = 0.15f),
            colors.statusLive,
            colors.statusLive.copy(alpha = 0.35f)
        )
        AvalonBadgeVariant.Warning -> Triple(
            colors.statusWarn.copy(alpha = 0.15f),
            colors.statusWarn,
            colors.statusWarn.copy(alpha = 0.35f)
        )
        AvalonBadgeVariant.Glass -> Triple(
            colors.bgCanvas.copy(alpha = 0.72f),
            colors.textPrimary,
            Color.White.copy(alpha = 0.10f)
        )
    }

    Row(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .border(AvalonTheme.elevation.borderWidth, borderColor, shape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (leadingIcon != null) {
            leadingIcon()
        }
        BasicText(
            text = text,
            style = AvalonTheme.typography.monoTech.copy(color = textColor),
            maxLines = 1
        )
    }
}

/**
 * 4. Avalon Navigation Zone for Carousels (Fitts's Law: 72.dp full-height gradient edge).
 */
@Composable
fun BoxScope.AvalonNavigationZone(
    isRight: Boolean,
    isVisible: Boolean = true,
    onClick: () -> Unit
) {
    if (AvalonTheme.isTv) return

    val canvasColor = AvalonTheme.colors.bgCanvas

    val gradientColors = if (isRight) {
        listOf(Color.Transparent, canvasColor.copy(alpha = 0.50f), canvasColor.copy(alpha = 0.85f))
    } else {
        listOf(canvasColor.copy(alpha = 0.85f), canvasColor.copy(alpha = 0.50f), Color.Transparent)
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(150)),
        modifier = Modifier.matchParentSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(if (isRight) Alignment.CenterEnd else Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(72.dp)
                    .background(Brush.horizontalGradient(colors = gradientColors))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                    .pointerHoverIcon(PointerIcon.Hand),
                contentAlignment = Alignment.Center
            ) {
                AvalonIcon(
                    imageVector = if (isRight) Lucide.ChevronRight else Lucide.ChevronLeft,
                    contentDescription = if (isRight) "Forward" else "Back",
                    tint = AvalonTheme.colors.textPrimary,
                    modifier = Modifier
                        .size(44.dp)
                        .padding(
                            start = if (isRight) 14.dp else 0.dp,
                            end = if (isRight) 0.dp else 14.dp
                        )
                )
            }
        }
    }
}

/**
 * 5. Universal Avalon Carousel with Smart TV D-pad focus restoration and Web/Desktop Fitts zones.
 */
@Composable
fun AvalonCarousel(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    header: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = AvalonTheme.spacing.space32),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(AvalonTheme.spacing.space16),
    isLoading: Boolean = false,
    content: LazyListScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val canScrollForward by remember { derivedStateOf { state.canScrollForward } }
    val canScrollBackward by remember { derivedStateOf { state.canScrollBackward } }

    val carouselInteractionSource = remember { MutableInteractionSource() }
    val isCarouselHovered by carouselInteractionSource.collectIsHoveredAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .focusRestorer()
            .focusGroup()
            .padding(vertical = AvalonTheme.spacing.space24)
    ) {
        if (header != null) {
            header()
            Spacer(modifier = Modifier.height(AvalonTheme.spacing.space16))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .hoverable(carouselInteractionSource),
            contentAlignment = Alignment.Center
        ) {
            LazyRow(
                state = state,
                horizontalArrangement = horizontalArrangement,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxWidth(),
                content = content
            )

            // Left scroll zone
            if (canScrollBackward && !isLoading) {
                AvalonNavigationZone(isRight = false, isVisible = isCarouselHovered) {
                    coroutineScope.launch {
                        val visibleCount = state.layoutInfo.visibleItemsInfo.size
                        val step = (visibleCount - 1).coerceAtLeast(1)
                        val target = (state.firstVisibleItemIndex - step).coerceAtLeast(0)
                        state.animateScrollToItem(target)
                    }
                }
            }

            // Right scroll zone
            if (canScrollForward && !isLoading) {
                AvalonNavigationZone(isRight = true, isVisible = isCarouselHovered) {
                    coroutineScope.launch {
                        val visibleCount = state.layoutInfo.visibleItemsInfo.size
                        val total = state.layoutInfo.totalItemsCount
                        val step = (visibleCount - 1).coerceAtLeast(1)
                        val target = (state.firstVisibleItemIndex + step).coerceAtMost(total - 1)
                        state.animateScrollToItem(target)
                    }
                }
            }
        }
    }
}
