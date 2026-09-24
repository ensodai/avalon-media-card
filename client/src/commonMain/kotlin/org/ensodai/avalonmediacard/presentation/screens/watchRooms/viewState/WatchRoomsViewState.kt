package org.ensodai.avalonmediacard.presentation.screens.watchRooms.viewState

import androidx.compose.runtime.Immutable
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.presentation.core.mvi.BaseViewState
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyStep
import kotlin.uuid.Uuid

@Immutable
data class WatchRoomsViewState(
    val rooms: List<WatchRoomSummaryDto> = emptyList(),
    val isLoading: Boolean = false,
    val isModalOpen: Boolean = false,
    val modalInitialStep: WatchPartyStep = WatchPartyStep.ENTRY,
    val selectedRoomIdToJoin: Uuid? = null
) : BaseViewState()
