package org.ensodai.avalonmediacard.rpc

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ensodai.avalonmediacard.contract.rpc.ActionRpcService
import org.ensodai.avalonmediacard.contract.auth.AuthState
import org.ensodai.avalonmediacard.contract.i18n.PluginLocaleElement
import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.plugin.PluginManager
import org.ensodai.avalonmediacard.repository.UserSettingsRepository
import org.ensodai.avalonmediacard.security.RpcSessionContext
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import kotlin.uuid.Uuid

@Factory
class ActionRpcServiceImpl(
    @InjectedParam private val session: RpcSessionContext,
    private val pluginManager: PluginManager,
    private val userSettingsRepository: UserSettingsRepository
) : ActionRpcService {

    private val logger = org.slf4j.LoggerFactory.getLogger(ActionRpcServiceImpl::class.java)

    private val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    private fun currentUserId(): Uuid? {
        val state = session.state.value
        return (state as? AuthState.Authorized)?.userId
    }

    override suspend fun handleAction(action: ServerAction): ActionResult {
        val userId = currentUserId() ?: return ActionResult.Error(401, "Not authorized")
        val userLocale = userSettingsRepository.getUserLocale(userId).takeIf { it != "auto" && it.isNotBlank() } ?: "ru"
        return withContext(PluginLocaleElement(userLocale)) {
            pluginManager.handleAction(action, userId)
        }
    }

}
