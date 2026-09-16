package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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

enum class PopupAnchorSide {
    RIGHT,
    LEFT,
    BOTTOM
}

/**
 * Умный провайдер позиционирования попапа относительно вызывающего элемента.
 * По умолчанию пытается разместиться справа от элемента (preferredSide = RIGHT),
 * если места справа до границы окна недостаточно — автоматически переключается налево.
 * Если экран слишком узкий для бокового размещения — открывается снизу (или сверху) от элемента.
 */
class SideAnchorPopupPositionProvider(
    private val preferredSide: PopupAnchorSide = PopupAnchorSide.RIGHT,
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
            PopupAnchorSide.RIGHT -> {
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
            PopupAnchorSide.LEFT -> {
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
            PopupAnchorSide.BOTTOM -> {
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
fun rememberSideAnchorPopupPositionProvider(
    preferredSide: PopupAnchorSide = PopupAnchorSide.RIGHT,
    horizontalMargin: Dp = 12.dp,
    verticalMargin: Dp = 8.dp,
    screenPadding: Dp = 16.dp
): PopupPositionProvider {
    val density = LocalDensity.current
    return remember(preferredSide, horizontalMargin, verticalMargin, screenPadding, density) {
        SideAnchorPopupPositionProvider(
            preferredSide = preferredSide,
            horizontalMarginPx = with(density) { horizontalMargin.roundToPx() },
            verticalMarginPx = with(density) { verticalMargin.roundToPx() },
            screenPaddingPx = with(density) { screenPadding.roundToPx() }
        )
    }
}

@Composable
fun AvalonDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 0),
    popupPositionProvider: PopupPositionProvider? = null,
    width: Dp = 240.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    if (expanded) {
        val popupContent = @Composable {
            Column(
                modifier = modifier
                    .width(width)
                    .background(Color(0xEE141414), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(vertical = 8.dp)
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
}

@Composable
fun AvalonDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    textColor: Color = Color.White,
    iconColor: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .tvAndWebHoverEffect(
                scaleTarget = 1.02f,
                shape = RoundedCornerShape(8.dp),
                activeBorderColor = Color.Transparent
            ,
    onClick = { onClick() })
            .background(if (isHovered) Color.White.copy(alpha = 0.1f) else Color.Transparent)
            
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
