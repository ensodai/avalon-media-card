package org.ensodai.avalonmediacard.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.X
import org.ensodai.avalonmediacard.designsystem.interaction.AvalonInteractiveRole
import org.ensodai.avalonmediacard.designsystem.interaction.AvalonSurfaceColors
import org.ensodai.avalonmediacard.designsystem.interaction.avalonInteractive
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

/**
 * 1. Foundation BasicTextField with Avalon Obsidian Cinema design tokens.
 */
@Composable
fun AvalonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    shape: Shape = AvalonTheme.shapes.control, // 10.dp
    focusRequester: FocusRequester? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val colors = AvalonTheme.colors
    val internalRequester = remember { FocusRequester() }
    val effectiveRequester = focusRequester ?: internalRequester

    var isFocused by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> colors.statusDanger
            isFocused -> colors.borderStrong
            else -> colors.borderSubtle
        },
        label = "InputBorder"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.bgElevated)
                .border(AvalonTheme.elevation.borderWidth, borderColor, shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = enabled
                ) {
                    effectiveRequester.requestFocus()
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (leadingIcon != null) {
                leadingIcon()
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                textStyle = AvalonTheme.typography.uiLabel.copy(
                    color = if (enabled) colors.textPrimary else colors.textMuted
                ),
                cursorBrush = SolidColor(colors.accentPrimary),
                singleLine = singleLine,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(effectiveRequester)
                    .onFocusChanged { isFocused = it.isFocused },
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            BasicText(
                                text = placeholder,
                                style = AvalonTheme.typography.uiLabel.copy(
                                    color = colors.textSecondary
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (trailingIcon != null) {
                trailingIcon()
            } else if (value.isNotEmpty() && enabled) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(colors.bgSurface)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onValueChange("") }
                        )
                        .pointerHoverIcon(PointerIcon.Hand),
                    contentAlignment = Alignment.Center
                ) {
                    AvalonIcon(
                        imageVector = Lucide.X,
                        contentDescription = "Clear",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (isError && !errorMessage.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            BasicText(
                text = errorMessage,
                style = AvalonTheme.typography.metadata.copy(color = colors.statusDanger)
            )
        }
    }
}

/**
 * 2. Star Rating (read-only or interactive) with accentGold stars.
 */
@Composable
fun AvalonStarRating(
    rating: Int,
    modifier: Modifier = Modifier,
    maxRating: Int = 5,
    starSize: Dp = 20.dp,
    showValueLabel: Boolean = false,
    onRatingSelected: ((Int) -> Unit)? = null
) {
    val colors = AvalonTheme.colors
    val isInteractive = onRatingSelected != null
    var hoveredRating by remember { mutableStateOf<Int?>(null) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val effectiveRating = hoveredRating ?: rating

        for (i in 1..maxRating) {
            val isFilled = i <= effectiveRating
            val starTint = if (isFilled) colors.accentGold else colors.chartTrack

            Box(
                modifier = if (isInteractive) {
                    Modifier
                        .size(starSize + 8.dp)
                        .clip(CircleShape)
                        .avalonInteractive(
                            role = AvalonInteractiveRole.Control,
                            shape = CircleShape,
                            surfaceColors = AvalonSurfaceColors(
                                default = Color.Transparent,
                                hover = colors.bgElevated
                            ),
                            onStateChange = { isHovered ->
                                hoveredRating = if (isHovered) i else null
                            },
                            onClick = { onRatingSelected(i) }
                        )
                } else {
                    Modifier.size(starSize)
                },
                contentAlignment = Alignment.Center
            ) {
                AvalonIcon(
                    imageVector = Lucide.Star,
                    contentDescription = "Star $i",
                    tint = starTint,
                    modifier = Modifier.size(starSize)
                )
            }
        }

        if (showValueLabel && effectiveRating > 0) {
            Spacer(Modifier.width(6.dp))
            BasicText(
                text = "$effectiveRating",
                style = AvalonTheme.typography.uiLabel.copy(
                    color = colors.accentGold,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
