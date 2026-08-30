package org.ensodai.avalonmediacard.auth

import org.ensodai.avalonmediacard.contract.model.IntegrationService
import org.ensodai.avalonmediacard.repository.UserExternalAuthRepository
import org.ensodai.avalonmediacard.sync.IntegrationSyncService
import org.koin.core.annotation.Single
import org.koin.core.context.GlobalContext
import kotlin.uuid.Uuid

@Single
class TraktIntegrationProvider(
    private val userExternalAuthRepository: UserExternalAuthRepository,
    private val oauthProviders: List<OAuthProvider>
) : IntegrationProvider {

    override val serviceName: String = "trakt"

    override suspend fun getSettingsDialog(userId: Uuid): org.ensodai.avalonmediacard.contract.slot.Action? {
        return null
    }

    override suspend fun saveSettings(userId: Uuid, settingsJson: String): Boolean {
        return userExternalAuthRepository.updateSettings(userId, IntegrationService.TRAKT, settingsJson)
    }

    override suspend fun triggerSync(userId: Uuid): Boolean {
        val syncService = GlobalContext.get().get<IntegrationSyncService>()
        return syncService.sync(userId, IntegrationService.TRAKT)
    }
}
