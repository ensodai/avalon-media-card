package org.ensodai.avalonmediacard.domain.useCases.watchparty

import org.ensodai.avalonmediacard.domain.repository.WatchPartyRepository
import org.koin.core.annotation.Factory
import kotlin.uuid.Uuid

@Factory
class LeaveWatchRoomUseCase(
    private val repository: WatchPartyRepository
) {
    suspend operator fun invoke(roomId: Uuid): Boolean {
        return repository.leaveRoom(roomId)
    }
}
