package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.metadataSlot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder

@Composable
fun MetadataPill(
    text: String,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White.copy(alpha = 0.08f),
    contentColor: Color = Color.White.copy(alpha = 0.85f),
    borderColor: Color = Color.White.copy(alpha = 0.12f),
    onClick: (() -> Unit)? = null
) {

    Box(
        modifier = modifier
            .background(
                color = if (isLoading) Color.Transparent else backgroundColor,
                shape = RoundedCornerShape(50)
            )
            .border(
                width = 1.dp,
                color = if (isLoading) Color.Transparent else borderColor,
                shape = RoundedCornerShape(50)
            )
            .shimmerPlaceholder(isLoading, RoundedCornerShape(50))
            .then(
                if (onClick != null && !isLoading) {
                    Modifier.clickable { onClick() }
                } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = if (isLoading) Color.Transparent else contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}