package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import org.ensodai.avalonmediacard.designsystem.components.AvalonDropdownMenu as CoreAvalonDropdownMenu
import org.ensodai.avalonmediacard.designsystem.components.AvalonDropdownMenuItem as CoreAvalonDropdownMenuItem
import org.ensodai.avalonmediacard.designsystem.components.AvalonPopupAnchorSide
import org.ensodai.avalonmediacard.designsystem.components.rememberAvalonSideAnchorPopupPositionProvider

@Composable
fun rememberSideAnchorPopupPositionProvider(
    preferredSide: AvalonPopupAnchorSide = AvalonPopupAnchorSide.RIGHT,
    horizontalMargin: Dp = 12.dp,
    verticalMargin: Dp = 8.dp,
    screenPadding: Dp = 16.dp
): PopupPositionProvider = rememberAvalonSideAnchorPopupPositionProvider(
    preferredSide = preferredSide,
    horizontalMargin = horizontalMargin,
    verticalMargin = verticalMargin,
    screenPadding = screenPadding
)

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
    CoreAvalonDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        alignment = alignment,
        offset = offset,
        popupPositionProvider = popupPositionProvider,
        width = width,
        content = content
    )
}

@Composable
fun AvalonDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    textColor: Color? = null,
    iconColor: Color? = null,
    trailingText: String? = null,
    enabled: Boolean = true
) {
    CoreAvalonDropdownMenuItem(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        trailingText = trailingText,
        enabled = enabled,
        textColor = textColor,
        iconColor = iconColor
    )
}
