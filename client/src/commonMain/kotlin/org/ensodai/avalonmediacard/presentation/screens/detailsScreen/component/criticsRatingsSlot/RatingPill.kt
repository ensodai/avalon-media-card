package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.criticsRatingsSlot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Star
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder

@Composable
fun RatingPill(
    source: String,
    value: String,
    tint: Color = Color(0xFF4CAF50),
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .background(
                color = if (isLoading) Color.Transparent else Color(0xFF141414),
                shape = RoundedCornerShape(50)
            )
            .border(
                width = 1.dp,
                color = if (isLoading) Color.Transparent else Color(0xFF27272A),
                shape = RoundedCornerShape(50)
            )
            .shimmerPlaceholder(isLoading, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Lucide.Star,
                contentDescription = null,
                tint = if (isLoading) Color.Transparent else tint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = source,
                color = if (isLoading) Color.Transparent else Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = value,
                color = if (isLoading) Color.Transparent else tint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

