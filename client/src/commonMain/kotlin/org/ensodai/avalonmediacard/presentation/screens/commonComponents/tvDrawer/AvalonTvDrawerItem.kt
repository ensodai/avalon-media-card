package org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect

/**
 * Типовая плашка (кнопка) списка внутри ТВ-шторки.
 * 
 * Обернута в [tvAndWebHoverEffect] для корректной отработки фокуса D-Pad'а 
 * с эффектом увеличения при наведении.
 */
@Composable
fun AvalonTvDrawerItem(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean = false,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes
    val typography = AvalonTheme.typography

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> colors.accentPrimary.copy(alpha = 0.22f)
            isFocused -> colors.bgElevated
            else -> colors.bgSurface
        }
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .tvAndWebHoverEffect(
                scaleTarget = 1.02f,
                activeBorderColor = colors.accentPrimary,
                activeBorderWidth = 1.5.dp,
                shape = shapes.control,
                onClick = onClick
            )
            .clip(shapes.control)
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            AvalonIcon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) colors.accentPrimary else if (isFocused) colors.textPrimary else colors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            AvalonText(
                text = title,
                color = if (isSelected) colors.accentPrimary else colors.textPrimary,
                style = typography.uiLabel
            )
            if (subtitle != null) {
                AvalonText(
                    text = subtitle,
                    color = colors.textSecondary,
                    style = typography.microCaption
                )
            }
        }

        if (isSelected) {
            AvalonIcon(
                imageVector = Lucide.Check,
                contentDescription = "Selected",
                tint = colors.accentPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
