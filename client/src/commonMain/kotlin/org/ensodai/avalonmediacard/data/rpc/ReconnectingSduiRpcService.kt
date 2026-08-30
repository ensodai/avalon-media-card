package org.ensodai.avalonmediacard.data.rpc

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.rpc.withService
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.model.SidebarItem
import org.ensodai.avalonmediacard.contract.model.WidgetSettingsData
import org.ensodai.avalonmediacard.contract.rpc.SduiRpcService
import org.ensodai.avalonmediacard.contract.slot.GlobalManifest
import org.ensodai.avalonmediacard.contract.slot.ScreenStreamEvent
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

class ReconnectingSduiRpcService(
    private val connectionManager: RpcConnectionManager,
    private val executor: RpcCallExecutor
) : SduiRpcService {

    private val logger = AppLogging.logger("ReconnectingSduiRpcService")

    private suspend fun getService(): SduiRpcService =
        connectionManager.getActiveClient().withService()

    override suspend fun getGlobalManifest(): GlobalManifest =
        executor.execute("getGlobalManifest", getService = { getService() }) { getGlobalManifest() }

    override fun streamSidebar(): Flow<List<SidebarItem>> {
        return flow {
            emitAll(getService().streamSidebar())
        }.retryWhen { cause, attempt ->
            if (cause is CancellationException && !executor.isNetworkCancellation(cause)) {
                return@retryWhen false
            }
            logger.w(cause) { "Sidebar Stream failed (attempt $attempt). Notifying failure & retrying..." }
            connectionManager.notifyStreamFailure(cause)
            delay(min(1000L * (attempt + 1), 5000L).milliseconds)
            true
        }
    }

    override suspend fun getWidgetSettings(): List<WidgetSettingsData> =
        executor.execute("getWidgetSettings", getService = { getService() }) { getWidgetSettings() }

    override suspend fun saveWidgetLayout(settings: List<WidgetSettingsData>): Boolean =
        executor.execute("saveWidgetLayout", getService = { getService() }) { saveWidgetLayout(settings) }

    override fun streamScreen(screen: Screen): Flow<ScreenStreamEvent> {
        return flow {
            emitAll(getService().streamScreen(screen))
        }.retryWhen { cause, attempt ->
            if (cause is CancellationException && !executor.isNetworkCancellation(cause)) {
                return@retryWhen false
            }
            logger.w(cause) { "Screen Stream failed for $screen (attempt $attempt). Notifying failure & retrying..." }
            connectionManager.notifyStreamFailure(cause)
            delay(min(1000L * (attempt + 1), 5000L).milliseconds)
            true
        }
    }
}
