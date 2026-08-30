package org.ensodai.avalonmediacard.sync.processors

import org.ensodai.avalonmediacard.auth.ExternalDataSyncProvider
import org.ensodai.avalonmediacard.auth.TraktSettings
import org.ensodai.avalonmediacard.contract.model.IntegrationService
import kotlin.uuid.Uuid

data class SyncContext(
    val userId: Uuid,
    val service: IntegrationService,
    val isFirstSync: Boolean,
    val accessToken: String,
    val syncProvider: ExternalDataSyncProvider,
    val settings: TraktSettings
)
