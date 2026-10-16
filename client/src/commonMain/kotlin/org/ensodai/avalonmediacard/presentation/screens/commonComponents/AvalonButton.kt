package org.ensodai.avalonmediacard.presentation.screens.commonComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AvalonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    height: Dp = 56.dp,
    fontSize: TextUnit = 18.sp,
    shape: Shape = RoundedCornerShape(24.dp)
) {
    Box(
        modifier = modifier
            .height(height)
            .tvAndWebHoverEffect(
                shape = shape,
                clickEnabled = enabled && !isLoading,
                onClick = onClick,
                activeBorderColor = Color.White,
                activeBorderWidth = 2.dp
            )
            .clip(shape)
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.5f))
            .padding(horizontal = if (height < 50.dp) 16.dp else 24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = contentColor,
                modifier = Modifier.size(if (height < 50.dp) 18.dp else 24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
