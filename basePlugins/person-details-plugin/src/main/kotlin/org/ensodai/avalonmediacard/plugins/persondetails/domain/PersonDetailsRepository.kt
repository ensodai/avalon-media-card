package org.ensodai.avalonmediacard.plugins.persondetails.domain

import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.PersonMetadata
import org.ensodai.avalonmediacard.contract.plugins.PluginContext

interface PersonDetailsRepository {
    suspend fun getPersonDetails(key: MediaKey): PersonMetadata
}

class PersonDetailsRepositoryImpl(
    private val context: PluginContext
) : PersonDetailsRepository {
    override suspend fun getPersonDetails(key: MediaKey): PersonMetadata = context.catalog.getPersonDetails(key)
}
