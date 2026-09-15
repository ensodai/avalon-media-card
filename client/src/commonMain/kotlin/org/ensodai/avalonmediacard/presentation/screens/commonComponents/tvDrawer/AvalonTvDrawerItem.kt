package org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvDrawer

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            isFocused -> Color.White.copy(alpha = 0.14f)
            else -> Color.White.copy(alpha = 0.05f)
        }
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .tvAndWebHoverEffect(
                scaleTarget = 1.02f,
                activeBorderColor = MaterialTheme.colorScheme.primary,
                activeBorderWidth = 1.5.dp,
                shape = RoundedCornerShape(12.dp),
                onClick = onClick
            )
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = if (isFocused) 0.95f else 0.7f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                fontSize = 17.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = if (isFocused) 0.8f else 0.5f),
                    fontSize = 12.sp
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Lucide.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
