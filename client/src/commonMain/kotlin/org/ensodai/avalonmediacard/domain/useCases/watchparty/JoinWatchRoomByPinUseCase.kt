package org.ensodai.avalonmediacard.domain.useCases.watchparty

import org.ensodai.avalonmediacard.contract.model.JoinRoomResult
import org.ensodai.avalonmediacard.domain.repository.WatchPartyRepository
import org.koin.core.annotation.Factory

@Factory
class JoinWatchRoomByPinUseCase(
    private val repository: WatchPartyRepository
) {
    suspend operator fun invoke(pin: String): JoinRoomResult {
        return repository.joinRoomByPin(pin)
    }
}
