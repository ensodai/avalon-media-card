package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.tv.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.watch_party_choose_intent_hint
import avalonmediacard.client.generated.resources.watch_party_my_intent_label
import avalonmediacard.client.generated.resources.watch_party_ready_status_not_ready
import org.ensodai.avalonmediacard.contract.model.WatchParticipantIntent
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.intentToEmoji
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.toLabelRes
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchPartyIntentSelectorTv(
    selectedIntent: WatchParticipantIntent?,
    isReady: Boolean,
    onSelectIntent: (WatchParticipantIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val intents = listOf(
        WatchParticipantIntent.WATCHING_ATTENTIVELY,
        WatchParticipantIntent.CHILLING,
        WatchParticipantIntent.AWAY_FOR_SNACKS,
        WatchParticipantIntent.SILENT_NO_PAUSES,
        WatchParticipantIntent.ACTIVE_DISCUSSION,
        WatchParticipantIntent.BACKGROUND_LISTENING
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 0.5.sp
            )
            if (isReady && selectedIntent != null) {
                Text(
                    text = stringResource(selectedIntent.toLabelRes()),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF81C784)
                )
            } else {
                Text(
                    text = stringResource(Res.string.watch_party_ready_status_not_ready),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.45f)
                )
            }
        }

        // 2 ряда по 3 плашки настроения под ТВ-пульт с крупными целями фокуса
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chunked = intents.chunked(3)
            val intentShape = RoundedCornerShape(12.dp)

            chunked.forEach { rowIntents ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowIntents.forEach { intent ->
                        val isSelected = isReady && selectedIntent == intent
                        val activeBorderColor = MaterialTheme.colorScheme.primary

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .tvAndWebHoverEffect(
                                    scaleTarget = 1.05f,
                                    shape = intentShape,
                                    activeBorderWidth = 2.dp,
                                    activeBorderColor = activeBorderColor,
                                    defaultBorderWidth = if (isSelected) 1.5.dp else 1.dp,
                                    defaultBorderColor = if (isSelected) activeBorderColor else Color.White.copy(alpha = 0.12f),
                                    onClick = { onSelectIntent(intent) }
                                )
                                .background(
                                    if (isSelected) activeBorderColor.copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.05f),
                                    intentShape
                                )
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = intentToEmoji(intent),
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(intent.toLabelRes()),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
