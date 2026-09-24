package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchPartyReadyToggle(
    isReady: Boolean,
    onToggleReady: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isReady) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f))
            .border(
                1.dp,
                if (isReady) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onToggleReady)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isReady) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.3f))
            )
            Text(
                text = if (isReady) stringResource(Res.string.watch_party_ready_check) else stringResource(Res.string.watch_party_ready_check_hint),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Text(
            text = if (isReady) stringResource(Res.string.watch_party_ready_status_ready) else stringResource(Res.string.watch_party_ready_status_not_ready),
            fontSize = 12.sp,
            color = if (isReady) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.5f)
        )
    }
}
