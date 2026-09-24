package org.ensodai.avalonmediacard.presentation.screens.watchRooms

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.domain.useCases.watchparty.GetUserWatchRoomsUseCase
import org.ensodai.avalonmediacard.presentation.core.mvi.BaseViewModel
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyStep
import org.ensodai.avalonmediacard.presentation.screens.watchRooms.action.*
import org.ensodai.avalonmediacard.presentation.screens.watchRooms.viewState.WatchRoomsViewState
import org.koin.core.annotation.Factory
import kotlin.uuid.Uuid

@Factory
class WatchRoomsViewModel(
    val getUserWatchRoomsUseCase: GetUserWatchRoomsUseCase
) : BaseViewModel<WatchRoomsViewState, WatchRoomsActions>(
    initialState = WatchRoomsViewState()
) {
    override val actions = WatchRoomsActions(
        onRefresh = ::loadRooms,
        onOpenCreateModal = ::onOpenCreateModal,
        onOpenConnectModal = ::onOpenConnectModal,
        onOpenRoom = ::onOpenRoom,
        onCloseModal = ::onCloseModal
    )

    init {
        viewModelScope.launch {
            getUserWatchRoomsUseCase.userRoomsFlow.collect { rooms ->
                updateViewState { it.copy(rooms = rooms) }
            }
        }
        loadRooms()
    }

    fun loadRooms() {
        viewModelScope.launch {
            updateViewState { it.copy(isLoading = true) }
            try {
                val rooms = getUserWatchRoomsUseCase()
                updateViewState { it.copy(rooms = rooms, isLoading = false) }
            } catch (_: Exception) {
                updateViewState { it.copy(isLoading = false) }
            }
        }
    }
}
