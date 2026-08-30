package org.ensodai.avalonmediacard.data.rpc

import kotlinx.rpc.withService
import org.ensodai.avalonmediacard.contract.auth.AuthResponse
import org.ensodai.avalonmediacard.contract.auth.LoginRequest
import org.ensodai.avalonmediacard.contract.auth.RegisterRequest
import org.ensodai.avalonmediacard.contract.model.IntegrationStatus
import org.ensodai.avalonmediacard.contract.rpc.AuthRpcService

class ReconnectingAuthRpcService(
    private val connectionManager: RpcConnectionManager,
    private val executor: RpcCallExecutor
) : AuthRpcService {

    private suspend fun getService(): AuthRpcService =
        connectionManager.getActiveClient().withService()

    override suspend fun login(request: LoginRequest): AuthResponse =
        executor.execute("login", getService = { getService() }) { login(request) }

    override suspend fun register(request: RegisterRequest): AuthResponse =
        executor.execute("register", getService = { getService() }) { register(request) }

    override suspend fun authenticate(token: String): AuthResponse? =
        executor.execute("authenticate", getService = { getService() }) { authenticate(token) }

    override suspend fun getOAuthUrl(service: String): String =
        executor.execute("getOAuthUrl", getService = { getService() }) { getOAuthUrl(service) }

    override suspend fun exchangeOAuthCode(service: String, code: String): Boolean =
        executor.execute("exchangeOAuthCode", getService = { getService() }) { exchangeOAuthCode(service, code) }

    override suspend fun getIntegrationsStatus(): List<IntegrationStatus> =
        executor.execute("getIntegrationsStatus", getService = { getService() }) { getIntegrationsStatus() }

    override suspend fun updateLocale(locale: String): Boolean =
        executor.execute("updateLocale", getService = { getService() }) { updateLocale(locale) }
}
