package org.ensodai.avalonmediacard.domain.useCases.watchparty

import org.ensodai.avalonmediacard.contract.model.UpdateRoomSourceRequest
import org.ensodai.avalonmediacard.domain.repository.WatchPartyRepository
import org.koin.core.annotation.Factory

@Factory
class UpdateWatchRoomSourceUseCase(
    private val repository: WatchPartyRepository
) {
    suspend operator fun invoke(request: UpdateRoomSourceRequest): Boolean {
        return repository.updateRoomSource(request)
    }
}
