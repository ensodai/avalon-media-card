package org.ensodai.avalonmediacard.rpc

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.ensodai.avalonmediacard.contract.auth.AuthState
import org.ensodai.avalonmediacard.contract.i18n.PluginLocaleElement
import org.ensodai.avalonmediacard.contract.i18n.PluginUserElement
import org.ensodai.avalonmediacard.contract.model.SidebarItem
import org.ensodai.avalonmediacard.contract.model.SidebarItemType
import org.ensodai.avalonmediacard.contract.model.WidgetSettingsData
import org.ensodai.avalonmediacard.contract.rpc.SduiRpcService
import org.ensodai.avalonmediacard.contract.slot.GlobalManifest
import org.ensodai.avalonmediacard.contract.slot.ScreenStreamEvent
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.database.WidgetSettings
import org.ensodai.avalonmediacard.plugin.PluginManager
import org.ensodai.avalonmediacard.repository.UserClickstreamRepository
import org.ensodai.avalonmediacard.repository.UserSettingsRepository
import org.ensodai.avalonmediacard.repository.WidgetSettingsRepository
import org.ensodai.avalonmediacard.security.RpcSessionContext
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
@Factory
class SduiRpcServiceImpl(
    @InjectedParam private val session: RpcSessionContext,
    private val pluginManager: PluginManager,
    private val widgetSettingsRepository: WidgetSettingsRepository,
    private val userClickstreamRepository: UserClickstreamRepository,
    private val userSettingsRepository: UserSettingsRepository
) : SduiRpcService {

    private fun currentUserId(): Uuid? {
        val state = session.state.value
        return (state as? AuthState.Authorized)?.userId
    }

    private suspend fun getUserLocale(userId: Uuid?): String {
        if (userId == null) return "ru"
        return userSettingsRepository.getUserLocale(userId).takeIf { it != "auto" && it.isNotBlank() } ?: "ru"
    }

    override fun streamSidebar(): Flow<List<SidebarItem>> = channelFlow {
        val userId = session.awaitUserId()
        val userLocale = getUserLocale(userId)
        val userContext = PluginLocaleElement(userLocale) + (userId?.let { PluginUserElement(it) } ?: kotlin.coroutines.EmptyCoroutineContext)
        withContext(userContext) {
            pluginManager.pluginUpdates.flatMapLatest {
                val flows = pluginManager.getSidebarFlows(userId)
                if (flows.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(flows) { itemLists ->
                        val allItems = itemLists.flatMap { it.toList() }
                            .sortedWith(compareBy<SidebarItem> { it.group }.thenBy { it.order })

                        val result = mutableListOf<SidebarItem>()
                        var currentGroup: Int? = null

                        for (item in allItems) {
                            if (currentGroup != null && currentGroup != item.group) {
                                // Add divider
                                result.add(
                                    SidebarItem(
                                        itemId = "divider_${currentGroup}_to_${item.group}",
                                        type = SidebarItemType.DIVIDER
                                    )
                                )
                            }
                            result.add(item)
                            currentGroup = item.group
                        }
                        result
                    }
                }
            }.collect { items ->
                send(items)
            }
        }
    }

    override suspend fun getGlobalManifest(): GlobalManifest {
        val userId = session.awaitUserId()
        val userLocale = getUserLocale(userId)
        val userContext = PluginLocaleElement(userLocale) + (userId?.let { PluginUserElement(it) } ?: kotlin.coroutines.EmptyCoroutineContext)
        return withContext(userContext) {
            pluginManager.buildGlobalManifest(userId)
        }
    }

    override fun streamScreen(screen: Screen): Flow<ScreenStreamEvent> = channelFlow {
        val userId = session.awaitUserId()
        val userLocale = getUserLocale(userId)
        val userContext = PluginLocaleElement(userLocale) + (userId?.let { PluginUserElement(it) } ?: kotlin.coroutines.EmptyCoroutineContext)
        withContext(userContext) {
            val allScreenSlots = pluginManager.getScreenSlots(screen, userId)
            println("SduiRpcService streamScreen: screen=$screen (class=${screen::class}), allScreenSlots=${allScreenSlots.size}")

            val mergedLayout = allScreenSlots.flatMap { it.layout }
            if (mergedLayout.isNotEmpty()) {
                send(ScreenStreamEvent.Layout(mergedLayout))
            }

            val screenKey = when (screen) {
                is Screen.MovieDetails -> screen.key
                is Screen.TvShowDetails -> screen.key
                is Screen.Person -> screen.key
                else -> null
            }

            val dynamicUpdatesFlow: Flow<ScreenStreamEvent> = if (screenKey != null) {
                pluginManager.slotUpdates
                    .filter { it.userId == userId && it.key == screenKey }
                    .map { ScreenStreamEvent.Update(it.update) }
            } else {
                emptyFlow()
            }

            val allFlows = allScreenSlots.map { it.flow.catch { e ->
                System.err.println("Screen slot flow failed: ${e.message}")
                e.printStackTrace()
            } } + listOf(dynamicUpdatesFlow)

            if (allFlows.isNotEmpty()) {
                kotlinx.coroutines.flow.merge(*allFlows.toTypedArray()).collect { event ->
                    send(event)
                }
            }
        }
    }

    override suspend fun getWidgetSettings(): List<WidgetSettingsData> {
        return widgetSettingsRepository.getAllSettings().map {
            WidgetSettingsData(
                widgetId = it.pluginId,
                isVisible = it.isVisible,
                orderIndex = it.orderIndex,
                widthSpan = it.widthSpan
            )
        }
    }

    override suspend fun saveWidgetLayout(settings: List<WidgetSettingsData>): Boolean {
        return try {
            val dbSettings = settings.map {
                WidgetSettings(
                    id = Uuid.random(),
                    pluginId = it.widgetId,
                    isVisible = it.isVisible,
                    orderIndex = it.orderIndex,
                    widthSpan = it.widthSpan
                )
            }
            widgetSettingsRepository.saveAllSettings(dbSettings)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
