package org.ensodai.avalonmediacard.presentation.screens.watchRooms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.WatchRoomPhase
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AdaptiveLayout
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalRootOverlay
import org.ensodai.avalonmediacard.presentation.screens.player.launchPlayerOverlay
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerInitParams
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerMode
import org.ensodai.avalonmediacard.presentation.screens.watchParty.WatchPartyScreen
import org.ensodai.avalonmediacard.presentation.screens.watchParty.WatchPartyViewModel
import org.ensodai.avalonmediacard.presentation.screens.watchRooms.targets.tv.WatchRoomsLayoutTv
import org.ensodai.avalonmediacard.presentation.screens.watchRooms.targets.web.WatchRoomsLayoutWeb
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

@Composable
fun WatchRoomsScreen(
    onStartPlayback: ((roomId: Uuid, season: Int?, episode: Int?, startPositionSeconds: Long) -> Unit)? = null,
    viewModel: WatchRoomsViewModel = koinInject(),
    watchPartyViewModel: WatchPartyViewModel = koinInject()
) {
    val state by viewModel.viewState.collectAsState()
    val actions = viewModel.actions
    val rootOverlay = LocalRootOverlay.current

    val onRoomClick: (WatchRoomSummaryDto) -> Unit = { room ->
        if (room.phase != WatchRoomPhase.LOBBY) {
            if (onStartPlayback != null) {
                onStartPlayback.invoke(room.id, room.currentSeason, room.currentEpisode, room.lastPositionSeconds)
            } else {
                val entityType = if (room.mediaType == MediaType.TV) EntityType.TV else EntityType.MOVIE
                val mediaKey = MediaKey(provider = MediaProvider.Tmdb, type = entityType, id = room.mediaId)
                launchPlayerOverlay(
                    rootOverlay = rootOverlay,
                    params = PlayerInitParams(
                        title = room.title,
                        seriesTitle = room.title,
                        mediaKey = mediaKey,
                        targetSeason = room.currentSeason,
                        targetEpisode = room.currentEpisode,
                        startPositionSeconds = room.lastPositionSeconds,
                        mode = PlayerMode.WATCH_PARTY,
                        sourceType = room.sourceType,
                        sourceId = room.sourceId,
                        watchRoomId = room.id,
                        isHost = room.isHost
                    ),
                    onReturnToLobby = { roomId ->
                        actions.onOpenRoom(roomId)
                    }
                )
            }
        } else {
            actions.onOpenRoom(room.id)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AdaptiveLayout(
            tv = {
                WatchRoomsLayoutTv(
                    state = state,
                    actions = actions,
                    onRoomClick = onRoomClick,
                    modifier = Modifier.fillMaxSize()
                )
            },
            web = {
                WatchRoomsLayoutWeb(
                    state = state,
                    actions = actions,
                    onRoomClick = onRoomClick,
                    modifier = Modifier.fillMaxSize()
                )
            },
            default = {
                WatchRoomsLayoutWeb(
                    state = state,
                    actions = actions,
                    onRoomClick = onRoomClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        )

        // 5. Watch Party Modal (if open)
        if (state.isModalOpen) {
            LaunchedEffect(state.selectedRoomIdToJoin) {
                state.selectedRoomIdToJoin?.let { roomId ->
                    watchPartyViewModel.actions.onJoinById(roomId)
                }
            }

            WatchPartyScreen(
                isVisible = true,
                initialStep = state.modalInitialStep,
                viewModel = watchPartyViewModel,
                onClose = { actions.onCloseModal() },
                onStartPlayback = { roomId, season, episode, startPositionSeconds ->
                    actions.onCloseModal()
                    if (onStartPlayback != null) {
                        onStartPlayback.invoke(roomId, season, episode, startPositionSeconds)
                    } else {
                        val activeRoom = watchPartyViewModel.viewState.value.activeRoom
                        val mediaKey = watchPartyViewModel.viewState.value.mediaKey
                        val isHost = watchPartyViewModel.viewState.value.isHost
                        if (mediaKey != null) {
                            launchPlayerOverlay(
                                rootOverlay = rootOverlay,
                                params = PlayerInitParams(
                                    title = activeRoom?.title ?: watchPartyViewModel.viewState.value.mediaTitle,
                                    seriesTitle = watchPartyViewModel.viewState.value.mediaTitle,
                                    mediaKey = mediaKey,
                                    targetSeason = season ?: activeRoom?.currentSeason,
                                    targetEpisode = episode ?: activeRoom?.currentEpisode,
                                    startPositionSeconds = if (startPositionSeconds > 0L) startPositionSeconds else activeRoom?.lastPositionSeconds,
                                    mode = PlayerMode.WATCH_PARTY,
                                    sourceType = activeRoom?.sourceType,
                                    sourceId = activeRoom?.sourceId,
                                    watchRoomId = roomId,
                                    isHost = isHost
                                ),
                                onReturnToLobby = { retRoomId ->
                                    actions.onOpenRoom(retRoomId)
                                }
                            )
                        }
                    }
                }
            )
        }
    }
}
