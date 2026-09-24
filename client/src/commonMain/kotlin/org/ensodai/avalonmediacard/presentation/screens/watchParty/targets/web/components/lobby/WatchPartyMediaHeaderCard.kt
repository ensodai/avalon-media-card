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
import com.composables.icons.lucide.Film
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Tv
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchPartyMediaHeaderCard(
    roomTitle: String,
    seasonNum: Int?,
    episodeNum: Int?,
    sourceLabel: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF6C63FF).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (seasonNum != null) Lucide.Tv else Lucide.Film,
                contentDescription = null,
                tint = Color(0xFF8C82FF),
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.watch_party_media_card_label).uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.45f),
                letterSpacing = 1.sp
            )
            Text(
                text = roomTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (seasonNum != null && episodeNum != null) {
                    Text(
                        text = stringResource(Res.string.watch_party_media_info_season_ep, seasonNum, episodeNum),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Text(text = "•", fontSize = 12.sp, color = Color.White.copy(alpha = 0.3f))
                }
                Text(
                    text = sourceLabel,
                    fontSize = 12.sp,
                    color = Color(0xFF6C63FF),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
