package org.ensodai.avalonmediacard.presentation.screens.watchRooms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.KeyRound
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalRootOverlay
import org.ensodai.avalonmediacard.presentation.screens.player.launchPlayerOverlay
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerInitParams
import org.ensodai.avalonmediacard.presentation.screens.player.model.PlayerMode
import org.ensodai.avalonmediacard.presentation.screens.watchParty.WatchPartyScreen
import org.ensodai.avalonmediacard.presentation.screens.watchParty.WatchPartyViewModel
import org.ensodai.avalonmediacard.presentation.screens.watchRooms.components.WatchRoomSummaryCard
import org.ensodai.avalonmediacard.presentation.screens.watchRooms.components.WatchRoomsActionCard
import org.ensodai.avalonmediacard.presentation.screens.watchRooms.components.WatchRoomsHeader
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

@Composable
fun WatchRoomsScreen(
    onStartPlayback: ((roomId: Uuid, season: Int?, episode: Int?) -> Unit)? = null,
    viewModel: WatchRoomsViewModel = koinInject(),
    watchPartyViewModel: WatchPartyViewModel = koinInject()
) {
    val state by viewModel.viewState.collectAsState()
    val actions = viewModel.actions
    val rootOverlay = LocalRootOverlay.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Header
            item {
                WatchRoomsHeader()
            }

            // 2. Action Cards (Create / Join by PIN)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WatchRoomsActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Lucide.Plus,
                        title = stringResource(Res.string.watch_rooms_btn_create),
                        subtitle = stringResource(Res.string.watch_rooms_create_desc),
                        buttonText = stringResource(Res.string.watch_rooms_btn_create),
                        onClick = actions.onOpenCreateModal
                    )

                    WatchRoomsActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Lucide.KeyRound,
                        title = stringResource(Res.string.watch_rooms_btn_connect),
                        subtitle = stringResource(Res.string.watch_rooms_connect_desc),
                        buttonText = stringResource(Res.string.watch_rooms_btn_connect),
                        onClick = actions.onOpenConnectModal
                    )
                }
            }

            // 3. User Rooms Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.watch_rooms_history_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 4. User Rooms List or Empty State
            if (state.rooms.isEmpty() && !state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.watch_rooms_history_empty),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                items(state.rooms, key = { it.id.toString() }) { room ->
                    WatchRoomSummaryCard(
                        room = room,
                        onClick = { actions.onOpenRoom(room.id) }
                    )
                }
            }
        }

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
                onStartPlayback = { roomId, season, episode ->
                    actions.onCloseModal()
                    if (onStartPlayback != null) {
                        onStartPlayback.invoke(roomId, season, episode)
                    } else {
                        val activeRoom = watchPartyViewModel.viewState.value.activeRoom
                        val mediaKey = watchPartyViewModel.viewState.value.mediaKey
                        if (mediaKey != null) {
                            launchPlayerOverlay(
                                rootOverlay = rootOverlay,
                                params = PlayerInitParams(
                                    title = activeRoom?.title ?: watchPartyViewModel.viewState.value.mediaTitle,
                                    seriesTitle = watchPartyViewModel.viewState.value.mediaTitle,
                                    mediaKey = mediaKey,
                                    targetSeason = season ?: activeRoom?.currentSeason,
                                    targetEpisode = episode ?: activeRoom?.currentEpisode,
                                    mode = PlayerMode.WATCH_PARTY,
                                    sourceType = activeRoom?.sourceType,
                                    sourceId = activeRoom?.sourceId,
                                    watchRoomId = roomId
                                )
                            )
                        }
                    }
                }
            )
        }
    }
}
