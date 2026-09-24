package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.WatchPartyActions
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.lobby.*
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyViewState
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchPartyLobbyStep(
    state: WatchPartyViewState,
    actions: WatchPartyActions,
    modifier: Modifier = Modifier
) {
    val pin = state.activeRoom?.joinPin ?: "------"
    val roomTitle = state.activeRoom?.title?.takeIf { it.isNotBlank() } ?: state.mediaTitle
    val seasonNum = state.selectedSeason ?: state.activeRoom?.currentSeason
    val episodeNum = state.selectedEpisode ?: state.activeRoom?.currentEpisode
    val sourceLabel = state.selectedSourceName
        ?: state.activeRoom?.sourceType?.uppercase()
        ?: stringResource(Res.string.watch_party_source_selected)

    val totalParticipants = state.participants.size
    val readyParticipants = state.participants.count { it.isReady }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Карточка медиа
        WatchPartyMediaHeaderCard(
            roomTitle = roomTitle,
            seasonNum = seasonNum,
            episodeNum = episodeNum,
            sourceLabel = sourceLabel
        )

        // 2. PIN-код лобби
        WatchPartyPinCard(pin = pin)

        // 3. Выбор намерения/интента
        WatchPartyIntentSelector(
            selectedIntent = state.myIntent,
            onSelectIntent = actions.onSetMyIntent
        )

        // 4. Переключатель готовности
        WatchPartyReadyToggle(
            isReady = state.myIsReady,
            onToggleReady = actions.onToggleReady
        )

        // 5. Список участников
        WatchPartyParticipantsList(participants = state.participants)

        // 6. Системное уведомление
        if (state.systemNotice != null) {
            Text(
                text = state.systemNotice,
                fontSize = 12.sp,
                color = Color(0xFFFFB74D),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 7. Баннер счетчика готовности
        WatchPartyReadinessBanner(
            readyCount = readyParticipants,
            totalCount = totalParticipants
        )

        // 8. Нижняя панель действий
        WatchPartyLobbyBottomBar(
            isHost = state.isHost,
            isStartingPlayback = state.isStartingPlayback,
            onStartPlayback = actions.onStartPlayback,
            onLeaveRoom = actions.onLeaveRoom
        )
    }
}
