package org.ensodai.avalonmediacard.presentation.screens.watchParty.action

import androidx.lifecycle.viewModelScope
import avalonmediacard.client.generated.resources.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.model.CreateRoomRequest
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.JoinRoomResult
import org.ensodai.avalonmediacard.contract.model.LobbyEvent
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.SetLobbyStatusRequest
import org.ensodai.avalonmediacard.contract.model.UpdateRoomSourceRequest
import org.ensodai.avalonmediacard.contract.model.WatchParticipantIntent
import org.ensodai.avalonmediacard.contract.model.WatchRoomControlMode
import org.ensodai.avalonmediacard.contract.slot.ScreenStreamEvent
import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.contract.slot.SlotId
import org.ensodai.avalonmediacard.contract.slot.SlotUpdate
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.presentation.core.extractSlot
import org.ensodai.avalonmediacard.presentation.core.extractSlots
import org.ensodai.avalonmediacard.presentation.screens.watchParty.WatchPartyViewModel
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyStep
import org.jetbrains.compose.resources.getString
import kotlin.uuid.Uuid

private val logger = AppLogging.logger("WatchParty")

fun WatchPartyViewModel.initialize(
    mediaKey: MediaKey?,
    title: String?,
    initialStep: WatchPartyStep? = null
) {
    val isMovie = mediaKey?.type == EntityType.MOVIE
    val currentKey = viewState.value.mediaKey
    val isDifferentMedia = mediaKey != null && mediaKey != currentKey

    if (initialStep == WatchPartyStep.ENTRY) {
        eventStreamJob?.cancel()
        eventStreamJob = null
        val currentUid = tokenStorage.cachedUserId?.let { runCatching { Uuid.parse(it) }.getOrNull() }
        updateViewState {
            it.copy(
                activeRoom = null,
                participantsMap = emptyMap(),
                isHost = false,
                step = WatchPartyStep.ENTRY,
                mediaKey = mediaKey,
                mediaTitle = title ?: "",
                pinInput = "",
                selectedSeason = null,
                selectedEpisode = null,
                selectedSourceType = null,
                selectedSourceId = null,
                selectedSourceName = null,
                isSelectingSource = false,
                isSourceVerified = false,
                isTestingSource = false,
                roomTitleInput = "",
                joinError = null,
                createError = null,
                myIsReady = false,
                myIntent = null,
                isActionPending = false,
                isStartingPlayback = false,
                myUserId = currentUid,
                savedRooms = emptyList()
            )
        }
        if (mediaKey != null) {
            loadSavedRooms(mediaKey.id)
            searchSources(mediaKey)
            subscribeToMediaSources(mediaKey)
        }
        return
    }

    if (!isDifferentMedia && viewState.value.activeRoom != null && viewState.value.step == WatchPartyStep.LOBBY) {
        if (initialStep == null || initialStep == WatchPartyStep.LOBBY) {
            if (title != null && viewState.value.mediaTitle.isBlank()) {
                updateViewState { it.copy(mediaTitle = title) }
            }
            return
        }
    }

    if (isDifferentMedia || initialStep == WatchPartyStep.SETUP) {
        eventStreamJob?.cancel()
        eventStreamJob = null
    }
    val currentUid = tokenStorage.cachedUserId?.let { runCatching { Uuid.parse(it) }.getOrNull() }
    val step = initialStep ?: if (mediaKey != null) WatchPartyStep.SETUP else WatchPartyStep.ENTRY
    updateViewState {
        it.copy(
            step = step,
            mediaKey = mediaKey,
            mediaTitle = title ?: (if (mediaKey == null) "" else it.mediaTitle),
            myUserId = currentUid,
            activeRoom = if (isDifferentMedia || initialStep == WatchPartyStep.SETUP) null else it.activeRoom,
            participantsMap = if (isDifferentMedia || initialStep == WatchPartyStep.SETUP) emptyMap() else it.participantsMap,
            isHost = if (isDifferentMedia || initialStep == WatchPartyStep.SETUP) false else it.isHost,
            selectedSeason = if (isMovie) null else (if (isDifferentMedia) null else it.selectedSeason),
            selectedEpisode = if (isMovie) null else (if (isDifferentMedia) null else it.selectedEpisode),
            selectedSourceType = if (isDifferentMedia) null else it.selectedSourceType,
            selectedSourceId = if (isDifferentMedia) null else it.selectedSourceId,
            selectedSourceName = if (isDifferentMedia) null else it.selectedSourceName,
            isSourceVerified = if (isDifferentMedia) false else it.isSourceVerified,
            isTestingSource = false,
            isSelectingSource = false,
            roomTitleInput = if (isDifferentMedia) "" else it.roomTitleInput,
            createError = null,
            joinError = null
        )
    }
    if (mediaKey != null) {
        loadSavedRooms(mediaKey.id)
        searchSources(mediaKey)
        subscribeToMediaSources(mediaKey)
    }
}

fun WatchPartyViewModel.loadSavedRooms(mediaId: String) {
    savedRoomsStreamJob?.cancel()
    savedRoomsStreamJob = viewModelScope.launch {
        updateViewState { it.copy(isLoadingSavedRooms = true) }
        try {
            getSavedWatchRoomsUseCase.stream(mediaId).collect { rooms ->
                updateViewState { it.copy(savedRooms = rooms, isLoadingSavedRooms = false) }
            }
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
                val mediaType = if (room.mediaType == MediaType.TV) EntityType.TV else EntityType.MOVIE
                val isRoomMovie = room.mediaType == MediaType.MOVIE || mediaType == EntityType.MOVIE
                val resolvedKey = if (viewState.value.mediaKey?.id == room.mediaId && viewState.value.mediaKey?.type == mediaType) {
                    viewState.value.mediaKey!!
                } else {
                    MediaKey(provider = MediaProvider.Tmdb, type = mediaType, id = room.mediaId)
                }
                updateViewState { state ->
                    val resolvedMediaTitle = room.mediaTitle?.takeIf { it.isNotBlank() }
                        ?: (if (state.mediaKey?.id == room.mediaId) state.mediaTitle.takeIf { t -> t.isNotBlank() && t != room.title } else null)
                        ?: room.title
                    state.copy(
                        activeRoom = room,
                        mediaKey = resolvedKey,
                        mediaTitle = resolvedMediaTitle,
                        selectedSeason = if (isRoomMovie) null else room.currentSeason,
                        selectedEpisode = if (isRoomMovie) null else room.currentEpisode,
                        selectedSourceType = room.sourceType,
                        selectedSourceId = room.sourceId,
                        selectedSourceName = null,
                        participantsMap = room.participants.associateBy { p -> p.userId },
                        isHost = isHost,
                        step = WatchPartyStep.LOBBY,
                        isJoining = false
                    )
                }
                subscribeToLobbyState(room.id)
                searchSources(resolvedKey)
                subscribeToMediaSources(resolvedKey)
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
                val mediaType = if (room.mediaType == MediaType.TV) EntityType.TV else EntityType.MOVIE
                val isRoomMovie = room.mediaType == MediaType.MOVIE || mediaType == EntityType.MOVIE
                val resolvedKey = if (viewState.value.mediaKey?.id == room.mediaId && viewState.value.mediaKey?.type == mediaType) {
                    viewState.value.mediaKey!!
                } else {
                    MediaKey(provider = MediaProvider.Tmdb, type = mediaType, id = room.mediaId)
                }
                updateViewState { state ->
                    val resolvedMediaTitle = room.mediaTitle?.takeIf { it.isNotBlank() }
                        ?: (if (state.mediaKey?.id == room.mediaId) state.mediaTitle.takeIf { t -> t.isNotBlank() && t != room.title } else null)
                        ?: room.title
                    state.copy(
                        activeRoom = room,
                        mediaKey = resolvedKey,
                        mediaTitle = resolvedMediaTitle,
                        selectedSeason = if (isRoomMovie) null else room.currentSeason,
                        selectedEpisode = if (isRoomMovie) null else room.currentEpisode,
                        selectedSourceType = room.sourceType,
                        selectedSourceId = room.sourceId,
                        selectedSourceName = null,
                        participantsMap = room.participants.associateBy { p -> p.userId },
                        isHost = isHost,
                        step = WatchPartyStep.LOBBY,
                        isJoining = false
                    )
                }
                subscribeToLobbyState(room.id)
                searchSources(resolvedKey)
                subscribeToMediaSources(resolvedKey)
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
    val currentState = viewState.value
    val currentRoom = currentState.activeRoom
    val isLobby = currentState.step == WatchPartyStep.LOBBY
    val isMovie = currentState.mediaKey?.type == EntityType.MOVIE || currentRoom?.mediaType == MediaType.MOVIE

    val targetSeason = if (isMovie) null else (seasonNumber ?: currentState.selectedSeason)
    val targetEpisode = if (isMovie) null else (episodeNumber ?: currentState.selectedEpisode)

    updateViewState {
        it.copy(
            selectedSourceType = sourceType,
            selectedSourceId = sourceId,
            selectedSourceName = sourceName,
            selectedSeason = targetSeason,
            selectedEpisode = targetEpisode,
            activeRoom = it.activeRoom?.copy(
                sourceType = sourceType,
                sourceId = sourceId,
                currentSeason = if (isMovie) null else (seasonNumber ?: it.activeRoom.currentSeason),
                currentEpisode = if (isMovie) null else (episodeNumber ?: it.activeRoom.currentEpisode)
            ),
            isSelectingSource = false,
            isSourceVerified = false
        )
    }

    if (isLobby && currentRoom != null && currentState.isHost) {
        viewModelScope.launch {
            try {
                updateWatchRoomSourceUseCase(
                    UpdateRoomSourceRequest(
                        roomId = currentRoom.id,
                        sourceType = sourceType,
                        sourceId = sourceId,
                        sourceName = sourceName,
                        season = targetSeason,
                        episode = targetEpisode
                    )
                )
            } catch (_: Exception) {
            }
        }
    }
}

fun WatchPartyViewModel.onToggleSelectSource(isSelecting: Boolean) {
    updateViewState { it.copy(isSelectingSource = isSelecting) }
    if (isSelecting && viewState.value.mediaSourcesList.isEmpty()) {
        onRefreshSources()
    }
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
            val isMovie = key.type == EntityType.MOVIE
            val defaultTitle = if (state.mediaTitle.isNotBlank()) {
                getString(Res.string.watch_party_title_format, state.mediaTitle)
            } else {
                getString(Res.string.watch_party_default_room_title)
            }
            val request = CreateRoomRequest(
                mediaId = key.id,
                mediaType = mediaType,
                title = state.roomTitleInput.ifBlank { defaultTitle },
                mediaTitle = state.mediaTitle.takeIf { it.isNotBlank() },
                season = if (isMovie) null else state.selectedSeason,
                episode = if (isMovie) null else state.selectedEpisode,
                startPositionSeconds = 0L,
                sourceType = state.selectedSourceType,
                sourceId = state.selectedSourceId,
                controlMode = state.controlMode,
                isPrivate = state.isPrivate
            )
            val room = createWatchRoomUseCase(request)
            val resolvedMediaTitle = room.mediaTitle?.takeIf { it.isNotBlank() }
                ?: state.mediaTitle.takeIf { t -> t.isNotBlank() && t != room.title }
                ?: ""
            updateViewState {
                it.copy(
                    activeRoom = room,
                    mediaTitle = resolvedMediaTitle,
                    participantsMap = room.participants.associateBy { p -> p.userId },
                    isHost = true,
                    step = WatchPartyStep.LOBBY,
                    isCreatingRoom = false
                )
            }
            subscribeToLobbyState(room.id)
        } catch (e: Exception) {
            val errorFallback = getString(Res.string.watch_party_error_create_failed)
            updateViewState { it.copy(createError = e.message ?: errorFallback, isCreatingRoom = false) }
        }
    }
}

fun WatchPartyViewModel.onSetMyIntent(intent: WatchParticipantIntent) {
    val room = viewState.value.activeRoom ?: return
    if (viewState.value.isActionPending) return
    val currentReady = viewState.value.myIsReady
    val currentIntent = viewState.value.myIntent

    val nextReady = !(currentReady && currentIntent == intent)
    val nextIntent = if (nextReady) intent else null

    updateViewState { it.copy(myIntent = nextIntent, myIsReady = nextReady, isActionPending = true) }
    viewModelScope.launch {
        try {
            setLobbyStatusUseCase(
                roomId = room.id,
                request = SetLobbyStatusRequest(
                    isReady = nextReady,
                    intent = intent
                )
            )
        } catch (_: Exception) {
            updateViewState { it.copy(isActionPending = false) }
        }
    }
}

fun WatchPartyViewModel.onToggleReady() {
    val room = viewState.value.activeRoom ?: return
    if (viewState.value.isActionPending) return
    val nextReady = !viewState.value.myIsReady
    val currentIntent = viewState.value.myIntent ?: WatchParticipantIntent.WATCHING_ATTENTIVELY
    updateViewState { it.copy(myIsReady = nextReady, isActionPending = true) }
    viewModelScope.launch {
        try {
            setLobbyStatusUseCase(
                roomId = room.id,
                request = SetLobbyStatusRequest(
                    isReady = nextReady,
                    intent = currentIntent
                )
            )
        } catch (_: Exception) {
            updateViewState { it.copy(myIsReady = !nextReady, isActionPending = false) }
        }
    }
}

fun WatchPartyViewModel.onStartPlayback() {
    val room = viewState.value.activeRoom ?: return
    viewModelScope.launch {
        updateViewState { it.copy(isStartingPlayback = true) }
        try {
            triggerStartPlaybackUseCase(room.id)
        } catch (_: Exception) {
            updateViewState { it.copy(isStartingPlayback = false) }
        }
    }
}

fun WatchPartyViewModel.onLeaveRoom() {
    val room = viewState.value.activeRoom ?: return
    viewModelScope.launch {
        try {
            leaveWatchRoomUseCase(room.id)
        } catch (_: Exception) {
        }
        eventStreamJob?.cancel()
        eventStreamJob = null
        savedRoomsStreamJob?.cancel()
        savedRoomsStreamJob = null
        sourcesStreamJob?.cancel()
        sourcesStreamJob = null
        updateViewState {
            it.copy(
                activeRoom = null,
                participantsMap = emptyMap(),
                isHost = false,
                step = WatchPartyStep.ENTRY,
                mediaKey = null,
                mediaTitle = "",
                pinInput = "",
                selectedSeason = null,
                selectedEpisode = null,
                selectedSourceType = null,
                selectedSourceId = null,
                selectedSourceName = null,
                isSelectingSource = false,
                isSourceVerified = false,
                isTestingSource = false,
                roomTitleInput = "",
                joinError = null,
                createError = null,
                myIsReady = false,
                myIntent = null,
                isActionPending = false,
                isStartingPlayback = false,
                savedRooms = emptyList()
            )
        }
        onCloseRequested?.invoke()
    }
}

fun WatchPartyViewModel.subscribeToLobbyState(roomId: Uuid) {
    eventStreamJob?.cancel()
    eventStreamJob = viewModelScope.launch {
        try {
            streamLobbyStateUseCase(roomId).collect { event ->
                when (event) {
                    is LobbyEvent.InitialSnapshot -> {
                        val map = event.participants.associateBy { it.userId }
                        val myId = viewState.value.myUserId
                        val me = myId?.let { map[it] }
                        val isMovie = viewState.value.mediaKey?.type == EntityType.MOVIE || viewState.value.activeRoom?.mediaType == MediaType.MOVIE
                        updateViewState {
                            it.copy(
                                participantsMap = map,
                                selectedSourceType = event.sourceType,
                                selectedSourceId = event.sourceId,
                                selectedSourceName = event.sourceName,
                                selectedSeason = if (isMovie) null else event.season,
                                selectedEpisode = if (isMovie) null else event.episode,
                                activeRoom = it.activeRoom?.copy(
                                    sourceType = event.sourceType,
                                    sourceId = event.sourceId,
                                    currentSeason = if (isMovie) null else event.season,
                                    currentEpisode = if (isMovie) null else event.episode
                                ),
                                myIsReady = me?.isReady ?: it.myIsReady,
                                myIntent = if (me?.isReady == true) me.intent else null,
                                isActionPending = false
                            )
                        }
                    }
                    is LobbyEvent.ParticipantUpdated -> {
                        val myId = viewState.value.myUserId
                        val isMe = event.participant.userId == myId
                        updateViewState {
                            it.copy(
                                participantsMap = it.participantsMap + (event.participant.userId to event.participant),
                                myIsReady = if (isMe) event.participant.isReady else it.myIsReady,
                                myIntent = if (isMe) (if (event.participant.isReady) event.participant.intent else null) else it.myIntent,
                                isActionPending = if (isMe) false else it.isActionPending
                            )
                        }
                    }
                    is LobbyEvent.ParticipantRemoved -> {
                        updateViewState {
                            it.copy(participantsMap = it.participantsMap - event.userId)
                        }
                    }
                    is LobbyEvent.SourceUpdated -> {
                        val isMovie = viewState.value.mediaKey?.type == EntityType.MOVIE || viewState.value.activeRoom?.mediaType == MediaType.MOVIE
                        updateViewState {
                            it.copy(
                                selectedSourceType = event.sourceType,
                                selectedSourceId = event.sourceId,
                                selectedSourceName = event.sourceName,
                                selectedSeason = if (isMovie) null else event.season,
                                selectedEpisode = if (isMovie) null else event.episode,
                                activeRoom = it.activeRoom?.copy(
                                    sourceType = event.sourceType,
                                    sourceId = event.sourceId,
                                    currentSeason = if (isMovie) null else event.season,
                                    currentEpisode = if (isMovie) null else event.episode
                                )
                            )
                        }
                    }
                    is LobbyEvent.TransitionToPlayer -> {
                        eventStreamJob?.cancel()
                        eventStreamJob = null
                        onLaunchPlayerRequested?.invoke(roomId, event.season, event.episode, event.startPositionSeconds)
                    }
                    is LobbyEvent.SystemNotice -> {
                        updateViewState { it.copy(systemNotice = event.message) }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(e) { "Lobby state stream failed for room $roomId" }
        }
    }
}

fun WatchPartyViewModel.subscribeToMediaSources(key: MediaKey) {
    sourcesStreamJob?.cancel()
    sourcesStreamJob = viewModelScope.launch {
        val currentSlotsMap = mutableMapOf<SlotId, MutableMap<String, SlotUpdate>>()
        try {
            streamScreenSlotsUseCase(Screen.Details(key)).collect { event ->
                if (event is ScreenStreamEvent.Update) {
                    val update = event.update
                    if (update.slotId == SlotId.MediaSources || update.slotId == SlotId.TorrentInspector) {
                        val slotMap = currentSlotsMap.getOrPut(update.slotId) { mutableMapOf() }
                        slotMap[update.nodeId] = update
                        val sources = currentSlotsMap.extractSlots<SlotData.MediaSources>(
                            SlotId.MediaSources,
                            oldSlots = viewState.value.mediaSourcesList
                        )
                        val inspector = currentSlotsMap.extractSlot<SlotData.TorrentInspector>(
                            SlotId.TorrentInspector,
                            oldSlot = viewState.value.torrentInspectorSlot
                        )
                        updateViewState {
                            it.copy(
                                mediaSourcesList = sources,
                                torrentInspectorSlot = inspector
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
}

fun WatchPartyViewModel.onRefreshSources() {
    val key = viewState.value.mediaKey ?: return
    viewModelScope.launch {
        try {
            searchMediaSourcesUseCase(key, forceRefresh = true)
        } catch (_: Exception) {
        }
    }
}

fun WatchPartyViewModel.onExecuteServerAction(action: ServerAction) {
    viewModelScope.launch {
        try {
            executeServerActionUseCase(action)
        } catch (_: Exception) {
        }
    }
}
