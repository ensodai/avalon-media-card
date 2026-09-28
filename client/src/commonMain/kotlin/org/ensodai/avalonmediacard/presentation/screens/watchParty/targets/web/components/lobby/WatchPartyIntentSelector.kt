package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.contract.model.WatchParticipantIntent
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.toLabelRes
import org.jetbrains.compose.resources.stringResource

private fun intentToEmoji(intent: WatchParticipantIntent): String = when (intent) {
    WatchParticipantIntent.WATCHING_ATTENTIVELY -> "🍿"
    WatchParticipantIntent.CHILLING -> "🛋️"
    WatchParticipantIntent.AWAY_FOR_SNACKS -> "☕"
    WatchParticipantIntent.SILENT_NO_PAUSES -> "🤫"
    WatchParticipantIntent.ACTIVE_DISCUSSION -> "💬"
    WatchParticipantIntent.BACKGROUND_LISTENING -> "🎧"
}

@Composable
fun WatchPartyIntentSelector(
    selectedIntent: WatchParticipantIntent?,
    isReady: Boolean,
    onSelectIntent: (WatchParticipantIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isReady && selectedIntent != null) {
                    stringResource(Res.string.watch_party_my_intent_label)
                } else {
                    stringResource(Res.string.watch_party_choose_intent_hint)
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp
            )
            if (isReady && selectedIntent != null) {
                Text(
                    text = stringResource(selectedIntent.toLabelRes()),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF81C784)
                )
            } else {
                Text(
                    text = stringResource(Res.string.watch_party_ready_status_not_ready),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.45f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val intents = listOf(
                WatchParticipantIntent.WATCHING_ATTENTIVELY,
                WatchParticipantIntent.CHILLING,
                WatchParticipantIntent.AWAY_FOR_SNACKS,
                WatchParticipantIntent.SILENT_NO_PAUSES,
                WatchParticipantIntent.ACTIVE_DISCUSSION,
                WatchParticipantIntent.BACKGROUND_LISTENING
            )

            val intentShape = RoundedCornerShape(12.dp)
            intents.forEach { intent ->
                val isSelected = isReady && selectedIntent == intent

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .tvAndWebHoverEffect(
                            scaleTarget = 1.10f,
                            shape = intentShape,
                            activeBorderColor = Color(0xFF8C82FF),
                            defaultBorderWidth = if (isSelected) 1.5.dp else 1.dp,
                            defaultBorderColor = if (isSelected) Color(0xFF8C82FF) else Color.White.copy(alpha = 0.10f),
                            onClick = { onSelectIntent(intent) }
                        )
                        .background(
                            if (isSelected) Color(0xFF6C63FF).copy(alpha = 0.35f)
                            else Color.White.copy(alpha = 0.06f),
                            intentShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = intentToEmoji(intent),
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
