package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X

@Composable
fun AvalonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    initialFocus: Boolean = false,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    showClearButton: Boolean = true,
    fontSize: TextUnit = 24.sp,
    height: Dp = 64.dp,
    shape: Shape = RoundedCornerShape(24.dp)
) {
    val internalRequester = remember { FocusRequester() }
    val effectiveRequester = focusRequester ?: internalRequester
    val focusManager = LocalFocusManager.current

    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isCompact = height < 56.dp

    Row(
        modifier = modifier
            .height(height)
            .tvAndWebHoverEffect(
                shape = shape,
                clickEnabled = false,
                focusEnabled = false, // Row сам не является focusable-узлом, фокус принадлежит BasicTextField
                activeBorderWidth = 0.dp,
                defaultBorderWidth = 0.dp
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                effectiveRequester.requestFocus()
            }
            .padding(horizontal = if (isCompact) 14.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 16.dp)
    ) {
        if (leadingIcon != null) {
            leadingIcon()
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .weight(1f)
                .focusRequester(effectiveRequester)
                .initialFocus(effectiveRequester, enabled = initialFocus)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionDown, Key.Enter, Key.NumPadEnter, Key.Tab -> {
                                focusManager.moveFocus(FocusDirection.Down)
                            }
                            Key.DirectionUp -> {
                                focusManager.moveFocus(FocusDirection.Up)
                            }
                            Key.DirectionLeft -> {
                                if (value.isEmpty()) {
                                    focusManager.moveFocus(FocusDirection.Left)
                                } else {
                                    false
                                }
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                },
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color.Gray.copy(alpha = 0.6f),
                            fontSize = fontSize,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    innerTextField()
                }
            }
        )

        if (showClearButton && value.isNotEmpty()) {
            val clearBtnSize = if (isCompact) 28.dp else 36.dp
            val clearIconSize = if (isCompact) 16.dp else 20.dp
            Box(
                modifier = Modifier
                    .size(clearBtnSize)
                    .tvAndWebHoverEffect(
                        shape = CircleShape,
                        scaleTarget = 1.15f,
                        clickEnabled = true,
                        onClick = {
                            onValueChange("")
                            effectiveRequester.requestFocus()
                        },
                        activeBorderColor = Color.White.copy(alpha = 0.8f),
                        activeBorderWidth = 1.5.dp
                    )
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Lucide.X,
                    contentDescription = "Clear",
                    tint = Color.White,
                    modifier = Modifier.size(clearIconSize)
                )
            }
        }
    }
}
