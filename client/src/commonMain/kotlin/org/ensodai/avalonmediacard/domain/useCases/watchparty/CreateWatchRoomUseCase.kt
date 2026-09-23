package org.ensodai.avalonmediacard.domain.useCases.watchparty

import org.ensodai.avalonmediacard.contract.model.CreateRoomRequest
import org.ensodai.avalonmediacard.contract.model.WatchRoomDto
import org.ensodai.avalonmediacard.domain.repository.WatchPartyRepository
import org.koin.core.annotation.Factory

@Factory
class CreateWatchRoomUseCase(
    private val repository: WatchPartyRepository
) {
    suspend operator fun invoke(request: CreateRoomRequest): WatchRoomDto {
        return repository.createRoom(request)
    }
}
