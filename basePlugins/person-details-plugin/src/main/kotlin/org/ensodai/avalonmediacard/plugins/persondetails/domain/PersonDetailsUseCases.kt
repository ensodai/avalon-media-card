package org.ensodai.avalonmediacard.plugins.persondetails.domain

import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.PersonMetadata

class GetPersonDetailsUseCase(private val repository: PersonDetailsRepository) {
    suspend operator fun invoke(key: MediaKey): PersonMetadata = repository.getPersonDetails(key)
}
