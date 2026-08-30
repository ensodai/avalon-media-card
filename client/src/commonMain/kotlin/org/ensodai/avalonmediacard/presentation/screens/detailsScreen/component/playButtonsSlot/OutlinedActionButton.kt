package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.playButtonsSlot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect

@Composable
fun OutlinedActionButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .shimmerPlaceholder(isLoading, RoundedCornerShape(8.dp))
            .tvAndWebHoverEffect(
                scaleTarget = 1.04f,
                activeBorderWidth = 1.dp,
                activeBorderColor = Color.White,
                defaultBorderWidth = 1.dp,
                defaultBorderColor = Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                clickEnabled = !isLoading,
    onClick = { onClick() })
            .background(if (isLoading) Color.Transparent else Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (isLoading) Color.Transparent else Color.White
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isLoading) Color.Transparent else Color.White
        )
    }
}

