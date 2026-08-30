package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.continueWatchingSlot

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.jetbrains.compose.resources.stringResource

@Composable
fun ContinueWatchingSlotContent(
    data: SlotData.ContinueWatching,
    isLoading: Boolean,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 40.dp)) {
        Text(
            text = stringResource(Res.string.details_continue_watching),
            color = if (isLoading) Color.Transparent else Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.shimmerPlaceholder(isLoading, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(enabled = !isLoading) { data.playAction?.let { onAction(it) } }
        ) {
            Text(
                text = data.title,
                color = if (isLoading) Color.Transparent else Color.White,
                modifier = Modifier.shimmerPlaceholder(isLoading, RoundedCornerShape(4.dp))
            )
            val prog = data.progressPercent
            if (prog != null || isLoading) {
                Text(
                    text = " - ${(prog ?: 0.5f * 100).toInt()}%",
                    color = if (isLoading) Color.Transparent else Color.Gray,
                    modifier = Modifier.shimmerPlaceholder(isLoading, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}