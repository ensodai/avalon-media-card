package org.ensodai.avalonmediacard.domain.useCases.watchparty

import kotlinx.coroutines.flow.Flow
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.domain.repository.WatchPartyRepository
import org.koin.core.annotation.Factory

@Factory
class GetSavedWatchRoomsUseCase(
    private val repository: WatchPartyRepository
) {

    fun stream(mediaId: String): Flow<List<WatchRoomSummaryDto>> {
        return repository.streamSavedRoomsForMedia(mediaId)
    }
}
