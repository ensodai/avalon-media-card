package org.ensodai.avalonmediacard.domain.useCases.watchparty

import kotlinx.coroutines.flow.Flow
import org.ensodai.avalonmediacard.contract.model.WatchRoomEvent
import org.ensodai.avalonmediacard.domain.repository.WatchPartyRepository
import org.koin.core.annotation.Factory
import kotlin.uuid.Uuid

@Factory
class StreamWatchRoomEventsUseCase(
    private val repository: WatchPartyRepository
) {
    operator fun invoke(roomId: Uuid): Flow<WatchRoomEvent> {
        return repository.streamRoomEvents(roomId)
    }
}
