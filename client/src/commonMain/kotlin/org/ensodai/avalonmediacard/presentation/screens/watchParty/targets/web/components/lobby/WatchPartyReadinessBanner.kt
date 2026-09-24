package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Users
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchPartyReadinessBanner(
    readyCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val isAllReady = totalCount > 0 && readyCount == totalCount

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isAllReady) Color(0xFF4CAF50).copy(alpha = 0.12f) else Color(0xFFFFB74D).copy(alpha = 0.1f))
            .border(
                1.dp,
                if (isAllReady) Color(0xFF4CAF50).copy(alpha = 0.3f) else Color(0xFFFFB74D).copy(alpha = 0.25f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isAllReady) Lucide.Check else Lucide.Users,
            contentDescription = null,
            tint = if (isAllReady) Color(0xFF4CAF50) else Color(0xFFFFB74D),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = if (isAllReady) {
                stringResource(Res.string.watch_party_ready_counter_all, readyCount, totalCount)
            } else {
                stringResource(Res.string.watch_party_ready_counter_waiting, readyCount, totalCount)
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isAllReady) Color(0xFF81C784) else Color(0xFFFFCC80)
        )
    }
}
