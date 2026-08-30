package org.ensodai.avalonmediacard.rpc

import org.ensodai.avalonmediacard.contract.auth.AuthState
import org.ensodai.avalonmediacard.contract.rpc.TelemetryRpcService
import org.ensodai.avalonmediacard.contract.model.TelemetryEvent
import org.ensodai.avalonmediacard.repository.UserClickstreamRepository
import org.ensodai.avalonmediacard.security.RpcSessionContext
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

@Factory
class TelemetryRpcServiceImpl(
    @InjectedParam private val session: RpcSessionContext,
    private val repository: UserClickstreamRepository
) : TelemetryRpcService {

    override suspend fun logEvent(event: TelemetryEvent) {
        val currentState = session.state.value
        if (currentState is AuthState.Authorized) {
            repository.logEvent(currentState.userId, event)
        }
    }
}
