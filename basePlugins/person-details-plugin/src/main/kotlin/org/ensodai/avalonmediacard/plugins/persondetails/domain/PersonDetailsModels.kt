package org.ensodai.avalonmediacard.plugins.persondetails.domain

import org.ensodai.avalonmediacard.contract.model.PersonMetadata

data class PersonDetailsState(
    val metadata: PersonMetadata? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
