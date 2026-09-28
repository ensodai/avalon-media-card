package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaType
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
    val mediaTitle = state.activeRoom?.mediaTitle?.takeIf { it.isNotBlank() }
        ?: state.mediaTitle.takeIf { it.isNotBlank() && it != state.activeRoom?.title }
        ?: state.activeRoom?.title
        ?: ""
    val isMovie = state.activeRoom?.mediaType == MediaType.MOVIE || state.mediaKey?.type == EntityType.MOVIE
    val seasonNum = if (isMovie) null else (state.activeRoom?.currentSeason ?: state.selectedSeason)
    val episodeNum = if (isMovie) null else (state.activeRoom?.currentEpisode ?: state.selectedEpisode)
    val rawSourceType = state.activeRoom?.sourceType ?: state.selectedSourceType
    val sourceProvider = formatSourceProviderTitle(rawSourceType)
    val sourceDetails = state.selectedSourceName
    val backdropUrl = state.activeRoom?.backdropUrl

    val totalParticipants = state.participants.size
    val readyParticipants = state.participants.count { it.isReady }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWide = maxWidth >= 660.dp

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Левая колонка: Hero-карточка медиа и источника + системное уведомление
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        WatchPartyMediaHeroCard(
                            mediaTitle = mediaTitle,
                            seasonNum = seasonNum,
                            episodeNum = episodeNum,
                            sourceProvider = sourceProvider,
                            sourceDetails = sourceDetails,
                            backdropUrl = backdropUrl,
                            isHost = state.isHost,
                            isSelectingSource = state.isSelectingSource,
                            onToggleSelectSource = { actions.onToggleSelectSource(!state.isSelectingSource) }
                        )

                        if (state.systemNotice != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFFB74D).copy(alpha = 0.12f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = state.systemNotice,
                                    fontSize = 12.sp,
                                    color = Color(0xFFFFB74D),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Правая колонка: Участники и выбор вайба (подтверждение готовности)
                    Column(
                        modifier = Modifier.weight(1.05f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        WatchPartyParticipantsList(participants = state.participants)

                        WatchPartyIntentSelector(
                            selectedIntent = state.myIntent,
                            isReady = state.myIsReady,
                            onSelectIntent = actions.onSetMyIntent
                        )
                    }
                }
            } else {
                // Узкий экран (или сплит-режим выбора источника)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WatchPartyMediaHeroCard(
                        mediaTitle = mediaTitle,
                        seasonNum = seasonNum,
                        episodeNum = episodeNum,
                        sourceProvider = sourceProvider,
                        sourceDetails = sourceDetails,
                        backdropUrl = backdropUrl,
                        isHost = state.isHost,
                        isSelectingSource = state.isSelectingSource,
                        onToggleSelectSource = { actions.onToggleSelectSource(!state.isSelectingSource) }
                    )

                    if (state.systemNotice != null) {
                        Text(
                            text = state.systemNotice,
                            fontSize = 12.sp,
                            color = Color(0xFFFFB74D),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    WatchPartyParticipantsList(participants = state.participants)

                    WatchPartyIntentSelector(
                        selectedIntent = state.myIntent,
                        isReady = state.myIsReady,
                        onSelectIntent = actions.onSetMyIntent
                    )
                }
            }
        }

        // Нижняя панель действий (Запуск / Ожидание + Выход)
        WatchPartyLobbyBottomBar(
            isHost = state.isHost,
            isStartingPlayback = state.isStartingPlayback,
            readyCount = readyParticipants,
            totalCount = totalParticipants,
            onStartPlayback = actions.onStartPlayback,
            onLeaveRoom = actions.onLeaveRoom
        )
    }
}
