package org.ensodai.avalonmediacard.domain.useCases.watchparty

import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.domain.repository.WatchPartyRepository
import org.koin.core.annotation.Factory

@Factory
class GetSavedWatchRoomsUseCase(
    private val repository: WatchPartyRepository
) {
    suspend operator fun invoke(mediaId: String): List<WatchRoomSummaryDto> {
        return repository.getSavedRoomsForMedia(mediaId)
    }
}
