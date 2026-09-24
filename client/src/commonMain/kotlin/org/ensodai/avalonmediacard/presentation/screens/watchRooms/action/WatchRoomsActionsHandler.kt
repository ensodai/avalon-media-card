package org.ensodai.avalonmediacard.presentation.screens.watchRooms.action

import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyStep
import org.ensodai.avalonmediacard.presentation.screens.watchRooms.WatchRoomsViewModel
import kotlin.uuid.Uuid

fun WatchRoomsViewModel.onOpenCreateModal() {
    updateViewState {
        it.copy(
            isModalOpen = true,
            modalInitialStep = WatchPartyStep.SETUP,
            selectedRoomIdToJoin = null
        )
    }
}

fun WatchRoomsViewModel.onOpenConnectModal() {
    updateViewState {
        it.copy(
            isModalOpen = true,
            modalInitialStep = WatchPartyStep.ENTRY,
            selectedRoomIdToJoin = null
        )
    }
}

fun WatchRoomsViewModel.onOpenRoom(roomId: Uuid) {
    updateViewState {
        it.copy(
            isModalOpen = true,
            modalInitialStep = WatchPartyStep.LOBBY,
            selectedRoomIdToJoin = roomId
        )
    }
}

fun WatchRoomsViewModel.onCloseModal() {
    updateViewState {
        it.copy(
            isModalOpen = false,
            selectedRoomIdToJoin = null
        )
    }
    loadRooms()
}
