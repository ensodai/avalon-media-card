package org.ensodai.avalonmediacard.plugins.torrserver.presentation.settings

import kotlinx.coroutines.flow.*
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.ScreenSlots
import org.ensodai.avalonmediacard.contract.slot.*
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.*
import org.ensodai.avalonmediacard.plugins.torrserver.presentation.ValidationStateTracker
import org.ensodai.avalonmediacard.plugins.torrserver.presentation.settings.model.TorrServerState
import org.ensodai.avalonmediacard.plugins.torrserver.presentation.settings.model.SearchEngineState
import kotlin.uuid.Uuid

class IntegrationsScreenPresenter(
    private val pluginId: String,
    private val context: PluginContext
) {
    val layoutNodes = listOf(
        LayoutNode("${pluginId}_torrserver", SlotId.Integrations),
        LayoutNode("${pluginId}_prowlarr", SlotId.Integrations),
        LayoutNode("${pluginId}_jackett", SlotId.Integrations)
    )

    fun getIntegrationsSlots(userId: Uuid?): ScreenSlots {
        if (userId == null) {
            return ScreenSlots(layout = emptyList(), flow = emptyFlow())
        }

        val hostFlow = context.userSettings.observeString(userId, "torrserver_host", "http://127.0.0.1:8090")
        val loginFlow = context.userSettings.observeString(userId, "torrserver_login", "")
        val passFlow = context.userSettings.observeString(userId, "torrserver_password", "")
        val useTorrServerFlow = context.userSettings.observeBoolean(userId, "use_torrserver", true)
        val useTorrServerGstFlow = context.userSettings.observeBoolean(userId, "use_torrserver_gst", false)

        val useProwlarrFlow = context.userSettings.observeBoolean(userId, "use_prowlarr", false)
        val prowlarrUrlFlow = context.userSettings.observeString(userId, "prowlarr_url", "http://localhost:9696")
        val prowlarrKeyFlow = context.userSettings.observeString(userId, "prowlarr_api_key", "")
        
        val useJackettFlow = context.userSettings.observeBoolean(userId, "use_jackett", false)
        val jackettUrlFlow = context.userSettings.observeString(userId, "jackett_url", "http://localhost:9117")
        val jackettKeyFlow = context.userSettings.observeString(userId, "jackett_api_key", "")

        val legacyEngineFlow = context.userSettings.observeString(userId, "torrent_search_engine", "")

        val torrServerFlow = combine(useTorrServerFlow, hostFlow, loginFlow, passFlow, useTorrServerGstFlow) { useTorr, host, login, pass, useGst ->
            TorrServerState(useTorr, host, login, pass, useGst)
        }
        val prowlarrFlow = combine(useProwlarrFlow, prowlarrUrlFlow, prowlarrKeyFlow) { useProwl, url, key ->
            SearchEngineState(useProwl, url, key)
        }
        val jackettFlow = combine(useJackettFlow, jackettUrlFlow, jackettKeyFlow) { useJack, url, key ->
            SearchEngineState(useJack, url, key)
        }

        val settingsFlow = combine(
            torrServerFlow,
            prowlarrFlow,
            jackettFlow,
            legacyEngineFlow,
            ValidationStateTracker.validationStates
        ) { torr, prowl, jack, legacyEngine, validationStatesMap ->
            data class StateSnapshot(
                val torr: TorrServerState,
                val prowl: SearchEngineState,
                val jack: SearchEngineState,
                val validationStatesMap: Map<String, Map<String, Pair<ValidationStatus, String?>>>
            )
            StateSnapshot(torr, prowl, jack, validationStatesMap)
        }

        val finalSettingsFlow = settingsFlow.map { snapshot ->
            val userStates = snapshot.validationStatesMap[userId.toString()] ?: emptyMap()

            val torrServerStatus = if (snapshot.torr.use) userStates["torrserver_host"]?.first ?: ValidationStatus.None else ValidationStatus.None
            val prowlarrStatus = if (snapshot.prowl.use) userStates["prowlarr_url"]?.first ?: ValidationStatus.None else ValidationStatus.None
            val jackettStatus = if (snapshot.jack.use) userStates["jackett_url"]?.first ?: ValidationStatus.None else ValidationStatus.None

            val hasSharedTorr = context.integrationManager.getTorrServerHost(null) != null
            val hasSharedProwl = context.integrationManager.getProwlarrSettings(null) != null
            val hasSharedJack = context.integrationManager.getJackettSettings(null) != null

            listOf(
                buildTorrServerSlot(snapshot.torr, torrServerStatus, userStates["torrserver_host"]?.second, hasSharedTorr),
                buildProwlarrSlot(snapshot.prowl, prowlarrStatus, userStates["prowlarr_url"]?.second, hasSharedProwl),
                buildJackettSlot(snapshot.jack, jackettStatus, userStates["jackett_url"]?.second, hasSharedJack)
            )
        }.flatMapConcat { updates ->
            flow { updates.forEach { emit(it) } }
        }

        return ScreenSlots(
            layout = layoutNodes,
            flow = finalSettingsFlow.map { ScreenStreamEvent.Update(it) }
        )
    }

    private suspend fun buildTorrServerSlot(torr: TorrServerState, status: ValidationStatus, msg: String?, hasShared: Boolean): SlotUpdate {
        val desc = if (!torr.use && hasShared) {
            context.i18n.t("settings.torrserver.shared_desc")
        } else {
            context.i18n.t("settings.torrserver.personal_desc")
        }
        return SlotUpdate(
            slotId = SlotId.Integrations,
            nodeId = "${pluginId}_torrserver",
            state = SlotState.Content(
                SlotData.SettingsGroup(
                    title = context.i18n.t("settings.torrserver.title"),
                    description = desc,
                    fields = listOf(
                        SettingField.Toggle(
                            key = "use_torrserver",
                            label = context.i18n.t("settings.torrserver.use_personal"),
                            value = torr.use,
                            onChangeAction = SaveTorrServerSettingsCommand()
                        ),
                        SettingField.TextField(
                            key = "torrserver_host",
                            label = context.i18n.t("settings.torrserver.address"),
                            value = torr.host ?: "http://127.0.0.1:8090",
                            placeholder = "http://127.0.0.1:8090",
                            isSensitive = false,
                            isEnabled = torr.use,
                            validateAction = TestTorrServerConnectionCommand(),
                            validationStatus = status,
                            validationMessage = msg
                        ),
                        SettingField.Toggle(
                            key = "use_torrserver_gst",
                            label = context.i18n.t("settings.torrserver.gst_transcode"),
                            value = torr.useGst,
                            onChangeAction = SaveTorrServerSettingsCommand()
                        ),
                        SettingField.TextField(
                            key = "torrserver_login",
                            label = context.i18n.t("settings.torrserver.login"),
                            value = torr.login ?: "",
                            placeholder = context.i18n.t("settings.torrserver.login_placeholder"),
                            isSensitive = false,
                            isEnabled = torr.use
                        ),
                        SettingField.TextField(
                            key = "torrserver_password",
                            label = context.i18n.t("settings.torrserver.password"),
                            value = torr.pass ?: "",
                            placeholder = context.i18n.t("settings.torrserver.password_placeholder"),
                            isSensitive = true,
                            isEnabled = torr.use
                        )
                    ),
                    saveAction = SaveTorrServerSettingsCommand(),
                    saveActionLabel = context.i18n.t("settings.save"),
                    isSaveEnabled = true,
                    connectionStatus = status
                )
            )
        )
    }

    private suspend fun buildProwlarrSlot(prowl: SearchEngineState, status: ValidationStatus, msg: String?, hasShared: Boolean): SlotUpdate {
        val desc = if (!prowl.use && hasShared) {
            context.i18n.t("settings.prowlarr.shared_desc")
        } else {
            context.i18n.t("settings.prowlarr.personal_desc")
        }
        return SlotUpdate(
            slotId = SlotId.Integrations,
            nodeId = "${pluginId}_prowlarr",
            state = SlotState.Content(
                SlotData.SettingsGroup(
                    title = context.i18n.t("settings.prowlarr.title"),
                    description = desc,
                    fields = listOf(
                        SettingField.Toggle(
                            key = "use_prowlarr",
                            label = context.i18n.t("settings.prowlarr.use_personal"),
                            value = prowl.use,
                            onChangeAction = SaveProwlarrSettingsCommand()
                        ),
                        SettingField.TextField(
                            key = "prowlarr_url",
                            label = "URL Prowlarr",
                            value = prowl.url ?: "http://localhost:9696",
                            placeholder = "http://localhost:9696",
                            isSensitive = false,
                            isEnabled = prowl.use,
                            validateAction = TestProwlarrConnectionCommand(),
                            validationStatus = status,
                            validationMessage = msg
                        ),
                        SettingField.TextField(
                            key = "prowlarr_api_key",
                            label = context.i18n.t("settings.prowlarr.apikey"),
                            value = prowl.key ?: "",
                            placeholder = context.i18n.t("settings.prowlarr.apikey_placeholder"),
                            isSensitive = true,
                            isEnabled = prowl.use
                        )
                    ),
                    saveAction = SaveProwlarrSettingsCommand(),
                    saveActionLabel = context.i18n.t("settings.save"),
                    isSaveEnabled = true,
                    connectionStatus = status
                )
            )
        )
    }

    private suspend fun buildJackettSlot(jack: SearchEngineState, status: ValidationStatus, msg: String?, hasShared: Boolean): SlotUpdate {
        val desc = if (!jack.use && hasShared) {
            context.i18n.t("settings.jackett.shared_desc")
        } else {
            context.i18n.t("settings.jackett.personal_desc")
        }
        return SlotUpdate(
            slotId = SlotId.Integrations,
            nodeId = "${pluginId}_jackett",
            state = SlotState.Content(
                SlotData.SettingsGroup(
                    title = context.i18n.t("settings.jackett.title"),
                    description = desc,
                    fields = listOf(
                        SettingField.Toggle(
                            key = "use_jackett",
                            label = context.i18n.t("settings.jackett.use_personal"),
                            value = jack.use,
                            onChangeAction = SaveJackettSettingsCommand()
                        ),
                        SettingField.TextField(
                            key = "jackett_url",
                            label = "URL Jackett",
                            value = jack.url ?: "http://localhost:9117",
                            placeholder = "http://localhost:9117",
                            isSensitive = false,
                            isEnabled = jack.use,
                            validateAction = TestJackettConnectionCommand(),
                            validationStatus = status,
                            validationMessage = msg
                        ),
                        SettingField.TextField(
                            key = "jackett_api_key",
                            label = context.i18n.t("settings.jackett.apikey"),
                            value = jack.key ?: "",
                            placeholder = context.i18n.t("settings.jackett.apikey_placeholder"),
                            isSensitive = true,
                            isEnabled = jack.use
                        )
                    ),
                    saveAction = SaveJackettSettingsCommand(),
                    saveActionLabel = context.i18n.t("settings.save"),
                    isSaveEnabled = true,
                    connectionStatus = status
                )
            )
        )
    }
}
