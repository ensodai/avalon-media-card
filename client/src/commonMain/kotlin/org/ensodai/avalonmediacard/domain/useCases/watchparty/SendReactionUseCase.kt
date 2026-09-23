package org.ensodai.avalonmediacard.domain.useCases.watchparty

import org.ensodai.avalonmediacard.domain.repository.WatchPartyRepository
import org.koin.core.annotation.Factory
import kotlin.uuid.Uuid

@Factory
class SendReactionUseCase(
    private val repository: WatchPartyRepository
) {
    suspend operator fun invoke(roomId: Uuid, emoji: String): Boolean {
        return repository.sendReaction(roomId, emoji)
    }
}
