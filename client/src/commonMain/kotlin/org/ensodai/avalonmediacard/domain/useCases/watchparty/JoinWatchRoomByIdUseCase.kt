package org.ensodai.avalonmediacard.domain.useCases.watchparty

import org.ensodai.avalonmediacard.contract.model.JoinRoomResult
import org.ensodai.avalonmediacard.domain.repository.WatchPartyRepository
import org.koin.core.annotation.Factory
import kotlin.uuid.Uuid

@Factory
class JoinWatchRoomByIdUseCase(
    private val repository: WatchPartyRepository
) {
    suspend operator fun invoke(roomId: Uuid): JoinRoomResult {
        return repository.joinRoomById(roomId)
    }
}
