package org.ensodai.avalonmediacard.domain.useCases.watchparty

import org.ensodai.avalonmediacard.contract.model.RoomPlaybackCommand
import org.ensodai.avalonmediacard.domain.repository.WatchPartyRepository
import org.koin.core.annotation.Factory
import kotlin.uuid.Uuid

@Factory
class SendPlaybackCommandUseCase(
    private val repository: WatchPartyRepository
) {
    suspend operator fun invoke(roomId: Uuid, command: RoomPlaybackCommand): Boolean {
        return repository.sendPlaybackCommand(roomId, command)
    }
}
