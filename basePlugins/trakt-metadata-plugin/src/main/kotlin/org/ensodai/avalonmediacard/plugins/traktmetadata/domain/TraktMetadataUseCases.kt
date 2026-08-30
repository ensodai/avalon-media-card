package org.ensodai.avalonmediacard.plugins.traktmetadata.domain

import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.slot.CommentItem

class GetMediaCommentsUseCase(
    private val repository: TraktMetadataRepository
) {
    suspend operator fun invoke(key: MediaKey, page: Int = 1, limit: Int = 10): List<CommentItem> {
        return repository.getComments(key, page, limit)
    }
}
