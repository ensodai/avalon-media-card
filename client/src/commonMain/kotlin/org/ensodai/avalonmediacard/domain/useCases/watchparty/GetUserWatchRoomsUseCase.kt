package org.ensodai.avalonmediacard.domain.useCases.watchparty

import kotlinx.coroutines.flow.StateFlow
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.domain.repository.WatchPartyRepository
import org.koin.core.annotation.Factory

@Factory
class GetUserWatchRoomsUseCase(
    private val repository: WatchPartyRepository
) {
    val userRoomsFlow: StateFlow<List<WatchRoomSummaryDto>> get() = repository.userRoomsFlow

    suspend operator fun invoke(): List<WatchRoomSummaryDto> {
        return repository.refreshUserRooms()
    }
}
