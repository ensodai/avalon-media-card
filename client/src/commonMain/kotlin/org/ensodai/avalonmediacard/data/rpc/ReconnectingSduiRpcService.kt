package org.ensodai.avalonmediacard.data.rpc

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.withService
import org.ensodai.avalonmediacard.contract.model.SidebarItem
import org.ensodai.avalonmediacard.contract.model.WidgetSettingsData
import org.ensodai.avalonmediacard.contract.rpc.SduiRpcService
import org.ensodai.avalonmediacard.contract.slot.GlobalManifest
import org.ensodai.avalonmediacard.contract.slot.ScreenStreamEvent
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen

class ReconnectingSduiRpcService(
    private val connectionManager: RpcConnectionManager,
    private val executor: RpcCallExecutor
) : SduiRpcService {

    private suspend fun getService(): SduiRpcService =
        connectionManager.getActiveClient().withService()

    override suspend fun getGlobalManifest(): GlobalManifest =
        executor.execute("getGlobalManifest", getService = { getService() }) { getGlobalManifest() }

    override fun streamSidebar(): Flow<List<SidebarItem>> =
        executor.executeFlow("streamSidebar", getService = { getService() }) {
            streamSidebar()
        }

    override suspend fun getWidgetSettings(): List<WidgetSettingsData> =
        executor.execute("getWidgetSettings", getService = { getService() }) { getWidgetSettings() }

    override suspend fun saveWidgetLayout(settings: List<WidgetSettingsData>): Boolean =
        executor.execute("saveWidgetLayout", getService = { getService() }) { saveWidgetLayout(settings) }

    override fun streamScreen(screen: Screen): Flow<ScreenStreamEvent> =
        executor.executeFlow("streamScreen($screen)", getService = { getService() }) {
            streamScreen(screen)
        }
}
