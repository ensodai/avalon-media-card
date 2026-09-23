package org.ensodai.avalonmediacard.presentation.screens.watchParty.action

import androidx.lifecycle.viewModelScope
import avalonmediacard.client.generated.resources.*
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.model.CreateRoomRequest
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.JoinRoomResult
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.contract.model.WatchParticipantIntent
import org.ensodai.avalonmediacard.contract.model.WatchRoomControlMode
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.ensodai.avalonmediacard.presentation.screens.watchParty.WatchPartyViewModel
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyStep
import org.jetbrains.compose.resources.getString
import kotlin.uuid.Uuid

fun WatchPartyViewModel.initialize(mediaKey: MediaKey?, title: String?) {
    val currentUid = tokenStorage.cachedUserId?.let { runCatching { Uuid.parse(it) }.getOrNull() }
    updateViewState {
        it.copy(
            mediaKey = mediaKey,
            mediaTitle = title ?: it.mediaTitle,
            myUserId = currentUid
        )
    }
    if (mediaKey != null) {
        loadSavedRooms(mediaKey.id)
        searchSources(mediaKey)
    }
}

fun WatchPartyViewModel.loadSavedRooms(mediaId: String) {
    viewModelScope.launch {
        updateViewState { it.copy(isLoadingSavedRooms = true) }
        try {
            val rooms = getSavedWatchRoomsUseCase(mediaId)
            updateViewState { it.copy(savedRooms = rooms, isLoadingSavedRooms = false) }
        } catch (_: Exception) {
            updateViewState { it.copy(isLoadingSavedRooms = false) }
        }
    }
}

fun WatchPartyViewModel.searchSources(key: MediaKey) {
    viewModelScope.launch {
        try {
            searchMediaSourcesUseCase(key)
        } catch (_: Exception) {
        }
    }
}

fun WatchPartyViewModel.onSetStep(step: WatchPartyStep) {
    updateViewState { it.copy(step = step, joinError = null, createError = null) }
}

fun WatchPartyViewModel.onPinChanged(pin: String) {
    updateViewState { it.copy(pinInput = pin.take(6).uppercase(), joinError = null) }
}

fun WatchPartyViewModel.onJoinByPin() {
    val pin = viewState.value.pinInput.trim()
    viewModelScope.launch {
        if (pin.length != 6) {
            val errorMsg = getString(Res.string.watch_party_error_invalid_pin)
            updateViewState { it.copy(joinError = errorMsg) }
            return@launch
        }

        updateViewState { it.copy(isJoining = true, joinError = null) }
        when (val result = joinWatchRoomByPinUseCase(pin)) {
            is JoinRoomResult.Success -> {
                val room = result.room
                val myId = viewState.value.myUserId
                val isHost = room.hostUserId == myId
                updateViewState {
                    it.copy(
                        activeRoom = room,
                        participants = room.participants,
                        isHost = isHost,
                        step = WatchPartyStep.LOBBY,
                        isJoining = false
                    )
                }
                subscribeToRoomEvents(room.id)
            }
            is JoinRoomResult.Error -> {
                updateViewState { it.copy(joinError = result.message, isJoining = false) }
            }
        }
    }
}

fun WatchPartyViewModel.onJoinById(roomId: Uuid) {
    viewModelScope.launch {
        updateViewState { it.copy(isJoining = true, joinError = null) }
        when (val result = joinWatchRoomByIdUseCase(roomId)) {
            is JoinRoomResult.Success -> {
                val room = result.room
                val myId = viewState.value.myUserId
                val isHost = room.hostUserId == myId
                updateViewState {
                    it.copy(
                        activeRoom = room,
                        participants = room.participants,
                        isHost = isHost,
                        step = WatchPartyStep.LOBBY,
                        isJoining = false
                    )
                }
                subscribeToRoomEvents(room.id)
            }
            is JoinRoomResult.Error -> {
                updateViewState { it.copy(joinError = result.message, isJoining = false) }
            }
        }
    }
}

fun WatchPartyViewModel.onRoomTitleChanged(title: String) {
    updateViewState { it.copy(roomTitleInput = title) }
}

fun WatchPartyViewModel.onControlModeChanged(mode: WatchRoomControlMode) {
    updateViewState { it.copy(controlMode = mode) }
}

fun WatchPartyViewModel.onSeasonEpisodeChanged(season: Int?, episode: Int?) {
    updateViewState { it.copy(selectedSeason = season, selectedEpisode = episode) }
}

fun WatchPartyViewModel.onSelectSource(
    sourceType: String,
    sourceId: String,
    sourceName: String,
    seasonNumber: Int? = null,
    episodeNumber: Int? = null
) {
    updateViewState {
        it.copy(
            selectedSourceType = sourceType,
            selectedSourceId = sourceId,
            selectedSourceName = sourceName,
            selectedSeason = seasonNumber ?: it.selectedSeason,
            selectedEpisode = episodeNumber ?: it.selectedEpisode,
            isSelectingSource = false,
            isSourceVerified = false
        )
    }
}

fun WatchPartyViewModel.onToggleSelectSource(isSelecting: Boolean) {
    updateViewState { it.copy(isSelectingSource = isSelecting) }
}

fun WatchPartyViewModel.onTestSource() {
    updateViewState { it.copy(isTestingSource = true) }
}

fun WatchPartyViewModel.onTestSourceVerified(verified: Boolean) {
    updateViewState { it.copy(isSourceVerified = verified, isTestingSource = false) }
}

fun WatchPartyViewModel.onTestSourceCancel() {
    updateViewState { it.copy(isTestingSource = false) }
}

fun WatchPartyViewModel.onCreateRoom() {
    val state = viewState.value
    val key = state.mediaKey ?: return

    viewModelScope.launch {
        if (state.selectedSourceType == null || state.selectedSourceId == null) {
            val errorMsg = getString(Res.string.watch_party_error_no_source)
            updateViewState { it.copy(createError = errorMsg) }
            return@launch
        }

        updateViewState { it.copy(isCreatingRoom = true, createError = null) }
        try {
            val mediaType = if (key.type == EntityType.TV) MediaType.TV else MediaType.MOVIE
            val defaultTitle = if (state.mediaTitle.isNotBlank()) {
                getString(Res.string.watch_party_title_format, state.mediaTitle)
            } else {
                getString(Res.string.watch_party_default_room_title)
            }
            val request = CreateRoomRequest(
                mediaId = key.id,
                mediaType = mediaType,
                title = state.roomTitleInput.ifBlank { defaultTitle },
                season = state.selectedSeason,
                episode = state.selectedEpisode,
                startPositionSeconds = 0L,
                sourceType = state.selectedSourceType,
                sourceId = state.selectedSourceId,
                controlMode = state.controlMode,
                isPrivate = state.isPrivate
            )
            val room = createWatchRoomUseCase(request)
            updateViewState {
                it.copy(
                    activeRoom = room,
                    participants = room.participants,
                    isHost = true,
                    step = WatchPartyStep.LOBBY,
                    isCreatingRoom = false
                )
            }
            subscribeToRoomEvents(room.id)
        } catch (e: Exception) {
            val errorFallback = getString(Res.string.watch_party_error_create_failed)
            updateViewState { it.copy(createError = e.message ?: errorFallback, isCreatingRoom = false) }
        }
    }
}

fun WatchPartyViewModel.onSetMyIntent(intent: WatchParticipantIntent) {
    val room = viewState.value.activeRoom ?: return
    updateViewState { it.copy(myIntent = intent) }
    viewModelScope.launch {
        sendPlaybackCommandUseCase(
            roomId = room.id,
            command = RoomPlaybackCommand.SetLobbyStatus(
                intent = intent,
                isReady = viewState.value.myIsReady
            )
        )
    }
}

fun WatchPartyViewModel.onToggleReady() {
    val room = viewState.value.activeRoom ?: return
    val nextReady = !viewState.value.myIsReady
    updateViewState { it.copy(myIsReady = nextReady) }
    viewModelScope.launch {
        sendPlaybackCommandUseCase(
            roomId = room.id,
            command = RoomPlaybackCommand.SetLobbyStatus(
                intent = viewState.value.myIntent,
                isReady = nextReady
            )
        )
    }
}

fun WatchPartyViewModel.onStartPlayback() {
    val room = viewState.value.activeRoom ?: return
    viewModelScope.launch {
        updateViewState { it.copy(isStartingPlayback = true) }
        sendPlaybackCommandUseCase(
            roomId = room.id,
            command = RoomPlaybackCommand.Play(positionMs = 0L)
        )
    }
}

fun WatchPartyViewModel.onLeaveRoom() {
    val room = viewState.value.activeRoom ?: return
    viewModelScope.launch {
        leaveWatchRoomUseCase(room.id)
        eventStreamJob?.cancel()
        eventStreamJob = null
        updateViewState {
            it.copy(
                activeRoom = null,
                participants = emptyList(),
                isHost = false,
                step = WatchPartyStep.ENTRY,
                myIsReady = false
            )
        }
        viewState.value.mediaKey?.let { loadSavedRooms(it.id) }
    }
}

fun WatchPartyViewModel.subscribeToRoomEvents(roomId: Uuid) {
    eventStreamJob?.cancel()
    eventStreamJob = viewModelScope.launch {
        streamWatchRoomEventsUseCase(roomId).collect { event ->
            when (event) {
                is WatchRoomEvent.SyncState -> {
                    if (event.isPlaying) {
                        onLaunchPlayerRequested?.invoke(roomId, event.season, event.episode)
                    }
                }
                is WatchRoomEvent.ParticipantsUpdated -> {
                    val myId = viewState.value.myUserId
                    val myParticipant = event.participants.find { it.userId == myId }
                    updateViewState {
                        it.copy(
                            participants = event.participants,
                            myIsReady = myParticipant?.isReady ?: it.myIsReady,
                            myIntent = myParticipant?.intent ?: it.myIntent
                        )
                    }
                }
                is WatchRoomEvent.SystemNotice -> {
                    updateViewState { it.copy(systemNotice = event.message) }
                }
                is WatchRoomEvent.ReactionTriggered -> {
                }
            }
        }
    }
}
