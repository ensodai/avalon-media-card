package org.ensodai.avalonmediacard.rpc

import org.ensodai.avalonmediacard.contract.auth.AuthState
import org.ensodai.avalonmediacard.contract.model.UserSettingsDto
import org.ensodai.avalonmediacard.contract.rpc.UserSettingsRpcService
import org.ensodai.avalonmediacard.repository.UserFeedCacheRepository
import org.ensodai.avalonmediacard.repository.UserSettingsRepository
import org.ensodai.avalonmediacard.security.RpcSessionContext
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

@Factory
class UserSettingsRpcServiceImpl(
    @InjectedParam private val session: RpcSessionContext,
    private val userSettingsRepository: UserSettingsRepository,
    private val userFeedCacheRepository: UserFeedCacheRepository
) : UserSettingsRpcService {

    override suspend fun getUserSettings(): UserSettingsDto {
        val state = session.state.value
        val userId = (state as? AuthState.Authorized)?.userId ?: return UserSettingsDto()
        return userSettingsRepository.getUserSettings(userId)
    }

    override suspend fun updateUserSettings(settings: UserSettingsDto): Boolean {
        val state = session.state.value
        val userId = (state as? AuthState.Authorized)?.userId ?: return false
        userSettingsRepository.saveUserSettings(userId, settings)
        runCatching { userFeedCacheRepository.invalidateUser(userId) }
        return true
    }
}
