package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.lobby

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantDto
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.WatchPartyParticipantItem
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchPartyParticipantsList(
    participants: List<WatchRoomParticipantDto>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.watch_party_participants_label, participants.size),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.5f),
            letterSpacing = 0.5.sp
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 210.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(participants, key = { it.userId.toString() }) { p ->
                WatchPartyParticipantItem(participant = p)
            }
        }
    }
}
