package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.syncStatusSlot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder

@Composable
fun SyncStatusSlotContent(
    data: SlotData.SyncStatus,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(if (isLoading) Color.Gray else Color(0xFF4CAF50), CircleShape)
                .shimmerPlaceholder(isLoading, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = data.statusText,
            color = if (isLoading) Color.Transparent else Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier.shimmerPlaceholder(isLoading, RoundedCornerShape(4.dp))
        )
    }
}