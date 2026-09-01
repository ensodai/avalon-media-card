package org.ensodai.avalonmediacard.plugin

import io.ktor.client.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import org.ensodai.avalonmediacard.auth.TraktOAuthProvider
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.model.SidebarItem
import org.ensodai.avalonmediacard.contract.model.MediaCatalog
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.UserSettingsDto
import org.ensodai.avalonmediacard.contract.plugins.*
import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.contract.slot.GlobalManifest
import org.ensodai.avalonmediacard.contract.slot.ScreenManifest
import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.recommendation.RecommendationEngineRegistry
import org.ensodai.avalonmediacard.repository.SystemSettingsRepository
import org.ensodai.avalonmediacard.repository.UserExternalAuthRepository
import org.ensodai.avalonmediacard.repository.UserFeedCacheRepository
import org.ensodai.avalonmediacard.repository.UserIntegrationSettingsRepository
import org.ensodai.avalonmediacard.repository.UserSettingsRepository
import org.ensodai.avalonmediacard.tmdb.TmdbApi
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URLClassLoader
import java.util.*
import kotlin.uuid.Uuid

@Single
class PluginManager(
    private val catalog: MediaCatalog,
    private val sharedHttpClient: HttpClient,
    private val userMovieProvider: UserMovieProvider,
    private val userEpisodeProvider: UserEpisodeProvider,
    private val userCustomLists: UserCustomListProvider,
    private val systemSettingsRepository: SystemSettingsRepository,
    private val tmdbApi: TmdbApi,
    private val userExternalAuthRepository: UserExternalAuthRepository,
    private val traktOAuthProvider: TraktOAuthProvider,
    private val coreIntegrations: CoreIntegrations,
    private val sourceMappingProvider: SourceMappingProvider,
    private val userMediaBindings: UserMediaBindingProvider,
    private val userIntegrationSettingsRepository: UserIntegrationSettingsRepository,
    private val recommendationRegistry: RecommendationEngineRegistry,
    private val telemetryProvider: TelemetryProvider,
    private val affinityVectorStore: AffinityVectorStore,
    private val genreDictionaryProvider: GenreDictionaryProvider,
    private val userFeedCacheRepository: UserFeedCacheRepository,
    private val userSettingsRepository: UserSettingsRepository
) {
    private val logger = LoggerFactory.getLogger(PluginManager::class.java)
    private val loadedPlugins = mutableListOf<AvalonPlugin>()
    private val pluginContexts = mutableMapOf<String, PluginContext>()
    private val pluginClassLoaders = mutableMapOf<String, URLClassLoader>()
    val serviceRegistry = ServiceRegistry()
    var serializersModule = SerializersModule {}
        private set

    // Лента событий изменения настроек
    private val changeEvents = MutableSharedFlow<String>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    data class SlotUpdateEvent(
        val userId: Uuid,
        val key: MediaKey,
        val update: org.ensodai.avalonmediacard.contract.slot.SlotUpdate
    )

    // Поток обновлений слотов от плагинов
    private val _slotUpdates = MutableSharedFlow<SlotUpdateEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val slotUpdates = _slotUpdates.asSharedFlow()

    // Поток обновлений при загрузке плагинов
    private val _pluginUpdates = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).apply { tryEmit(Unit) }
    val pluginUpdates = _pluginUpdates.asSharedFlow()

    suspend fun emitChangeEvent(event: String) {
        changeEvents.emit(event)
    }

    private fun getPluginsDir(): File {
        return if (File("server/plugins").exists() && (File("server/plugins").listFiles { file -> file.extension == "jar" }?.isNotEmpty() == true)) {
            File("server/plugins")
        } else {
            File("plugins").apply { if (!exists()) mkdirs() }
        }
    }

    init {
        val pluginsDir = getPluginsDir()

        // Инициализируем системные интеграции ядра (Core)
        coreIntegrations.initialize(
            changeEvents = changeEvents,
            pluginContexts = pluginContexts,
            userIntegrationSettingsRepository = userIntegrationSettingsRepository,
            streams = StreamRegistry(
                fallbackProvider = { k, s, u, p -> getPlaylistForMedia(k, s, u, p) },
                fallbackPreparer = { s, u -> prepareStream(s, u) }
            ),
            slotUpdater = SlotUpdaterImpl(_slotUpdates)
        )

        val jarFiles = pluginsDir.listFiles { file -> file.extension == "jar" } ?: emptyArray()

        jarFiles.forEach { jarFile ->
            loadPlugin(jarFile)
        }

        updateSerializersModule()
    }

    fun loadPlugin(jarFile: File): List<AvalonPlugin> {
        val classLoader = URLClassLoader(
            arrayOf(jarFile.toURI().toURL()),
            this::class.java.classLoader
        )

        val serviceLoader = ServiceLoader.load(AvalonPlugin::class.java, classLoader)
        val newPlugins = serviceLoader.toList()

        newPlugins.forEach { plugin ->
            // Очищаем старые регистрации при перезагрузке плагина с тем же id
            loadedPlugins.filter { it.id == plugin.id }.forEach { oldPlugin ->
                runCatching { oldPlugin.onDestroy() }
            }
            loadedPlugins.removeIf { it.id == plugin.id }
            pluginClassLoaders.remove(plugin.id)?.let { runCatching { it.close() } }
            pluginClassLoaders[plugin.id] = classLoader
            pluginContexts.remove(plugin.id)?.let { oldContext ->
                oldContext.scope.cancel()
            }

            val i18n = PluginI18nLoader.loadI18n(classLoader, jarFile)
            val context = PluginContext(
                pluginDir = jarFile.parentFile.absolutePath,
                pluginName = plugin.name,
                logger = DefaultPluginLogger(plugin.name),
                httpClient = sharedHttpClient,
                catalog = catalog,
                i18n = i18n,
                userMovies = userMovieProvider,
                userEpisodes = userEpisodeProvider,
                userCustomLists = userCustomLists,
                settings = PluginSettingsImpl(plugin.id, systemSettingsRepository, changeEvents),
                userSettings = UserPluginSettingsImpl(plugin.id, userIntegrationSettingsRepository),
                integrationManager = IntegrationSettingsManagerImpl(plugin.id, systemSettingsRepository, userIntegrationSettingsRepository, userExternalAuthRepository, userSettingsRepository),
                userMediaBindings = userMediaBindings,
                sourceMappings = sourceMappingProvider,
                updater = SlotUpdaterImpl(_slotUpdates),
                streams = StreamRegistry(
                    fallbackProvider = { k, s, u, p -> getPlaylistForMedia(k, s, u, p) },
                    fallbackPreparer = { s, u -> prepareStream(s, u) }
                ),
                recommendations = recommendationRegistry,
                telemetry = telemetryProvider,
                affinityStore = affinityVectorStore,
                genreDictionary = genreDictionaryProvider,
                feedCache = userFeedCacheRepository,
                userGlobalSettings = object : UserGlobalSettingsProvider {
                    override suspend fun getUserSettings(userId: Uuid): UserSettingsDto? =
                        userSettingsRepository.getUserSettings(userId)
                }
            )

            pluginContexts[plugin.id] = context

            try {
                plugin.onInitialize(context)
                plugin.onBind(serviceRegistry)
            } catch (e: Exception) {
                System.err.println("Failed to initialize plugin ${plugin.name}")
                e.printStackTrace()
            }
        }

        // Добавляем только те плагины, которых еще нет в списке
        newPlugins.forEach { plugin ->
            if (loadedPlugins.none { it.id == plugin.id }) {
                loadedPlugins.add(plugin)
            }
        }

        println("Loaded ${newPlugins.size} plugins from ${jarFile.name}")
        newPlugins.forEach {
            println(" - Plugin: ${it.name} (Version: ${it.version})")
        }

        _pluginUpdates.tryEmit(Unit)
        updateSerializersModule()

        return newPlugins
    }

    private fun updateSerializersModule() {
        var combined = coreIntegrations.provideSerializers()
        for (plugin in loadedPlugins) {
            val module = plugin.provideSerializers()
            if (module != null) {
                combined += module
            }
        }
        serializersModule = combined
    }

    fun getPlugins(): List<AvalonPlugin> = loadedPlugins

    suspend fun getScreenSlots(
        screen: Screen,
        userId: Uuid? = null
    ): List<org.ensodai.avalonmediacard.contract.plugins.ScreenSlots> {
        return pluginContexts.values.mapNotNull { context ->
            context.slots.getScreenSlots(screen, userId)
        }
    }

    suspend fun buildGlobalManifest(userId: Uuid?): GlobalManifest {
        val screens = mutableMapOf<String, ScreenManifest>()

        for (context in pluginContexts.values) {
            for ((screenClass, slotIds) in context.slots.declarations) {
                val screenName = screenClass.simpleName ?: continue

                val layoutBuilder = context.slots.getManifestLayoutBuilder(screenClass)
                val layout = layoutBuilder?.invoke(userId) ?: emptyList()

                val existing = screens.getOrPut(screenName) { ScreenManifest(emptyList(), emptyList()) }
                screens[screenName] = ScreenManifest(
                    slots = (existing.slots + slotIds).distinct(),
                    layout = (existing.layout + layout).distinct()
                )
            }
        }

        return GlobalManifest(screens)
    }

    fun getSidebarFlows(userId: Uuid? = null): List<Flow<List<SidebarItem>>> {
        return pluginContexts.values.mapNotNull { context ->
            context.sidebars.getFlow(userId)
        }
    }

    fun getStreamsForMedia(
        key: MediaKey,
        season: Int?,
        episode: Int?,
        userId: kotlin.uuid.Uuid?
    ): List<Flow<MediaStream>> {
        return pluginContexts.values.mapNotNull { context ->
            context.streams.getStreams(key, season, episode, userId)
        }
    }

    suspend fun prepareStream(stream: MediaStream, userId: kotlin.uuid.Uuid?): MediaStream {
        if (stream.sourceName.isNotBlank()) {
            val matchedContext = pluginContexts.entries.firstOrNull { (id, _) ->
                id.contains(stream.sourceName, ignoreCase = true) || stream.sourceName.contains(id, ignoreCase = true)
            }?.value
            val prepared = matchedContext?.streams?.prepareDirectStream(stream, userId)
            if (prepared != null) return prepared
        }

        for (context in pluginContexts.values) {
            val preparedStream = context.streams.prepareDirectStream(stream, userId)
            if (preparedStream != null) {
                return preparedStream
            }
        }
        return stream
    }

    suspend fun getPlaylistForMedia(
        key: MediaKey,
        sourceId: String,
        userId: Uuid?,
        providerId: String? = null
    ): List<MediaStream>? {
        if (providerId != null) {
            val targetContext = pluginContexts[providerId]
                ?: pluginContexts.entries.firstOrNull { (id, _) ->
                    id.contains(providerId, ignoreCase = true) || providerId.contains(id, ignoreCase = true)
                }?.value
            val list = targetContext?.streams?.getDirectPlaylist(key, sourceId, userId)
            if (!list.isNullOrEmpty()) {
                return list
            }
        }
        for (context in pluginContexts.values) {
            val list = context.streams.getDirectPlaylist(key, sourceId, userId)
            if (!list.isNullOrEmpty()) {
                return list
            }
        }
        return null
    }

    suspend fun searchSources(key: MediaKey, userId: Uuid, forceRefresh: Boolean = false) {
        coreIntegrations.searchSources(key, userId, forceRefresh)
    }

    fun getPluginContext(pluginId: String): PluginContext? = pluginContexts[pluginId]

    suspend fun handleAction(action: ServerAction, userId: Uuid?): ActionResult {
        val actionClass = action::class
        for (context in pluginContexts.values) {
            val handler = context.actions.handlers[actionClass]
            if (handler != null) {
                return handler.invoke(action, userId)
            }
        }
        return ActionResult.Error(404, "No handler found for action ${actionClass.simpleName}")
    }

    fun getLoadedPluginsCount(): Int = loadedPlugins.size
    fun getLoadedPlugins(): List<AvalonPlugin> = loadedPlugins.toList()

    fun unloadPlugin(pluginId: String) {
        val plugin = loadedPlugins.find { it.id == pluginId } ?: return
        try {
            plugin.onDestroy()
        } catch (e: Exception) {
            logger.error("Failed to destroy plugin {}", plugin.name, e)
        }
        loadedPlugins.remove(plugin)
        pluginClassLoaders.remove(pluginId)?.let { runCatching { it.close() } }
        pluginContexts.remove(pluginId)?.let { context ->
            context.scope.cancel()
        }
        _pluginUpdates.tryEmit(Unit)
        updateSerializersModule()
    }

    fun destroyAll() {
        loadedPlugins.forEach { plugin ->
            runCatching { plugin.onDestroy() }
        }
        loadedPlugins.clear()
        pluginContexts.values.forEach { it.scope.cancel() }
        pluginContexts.clear()
        pluginClassLoaders.values.forEach { runCatching { it.close() } }
        pluginClassLoaders.clear()
        _pluginUpdates.tryEmit(Unit)
        updateSerializersModule()
    }

    fun reloadAllPlugins(): Int {
        logger.info("Starting hot reload of all plugins...")

        // 1. Очищаем все загруженные плагины
        loadedPlugins.forEach { plugin ->
            runCatching { plugin.onDestroy() }
        }
        loadedPlugins.clear()

        pluginContexts.values.forEach { it.scope.cancel() }
        pluginContexts.clear()

        pluginClassLoaders.values.forEach { runCatching { it.close() } }
        pluginClassLoaders.clear()

        // 2. Переинициализируем системные интеграции ядра (Core)
        coreIntegrations.initialize(
            changeEvents = changeEvents,
            pluginContexts = pluginContexts,
            userIntegrationSettingsRepository = userIntegrationSettingsRepository,
            streams = StreamRegistry(
                fallbackProvider = { k, s, u, p -> getPlaylistForMedia(k, s, u, p) },
                fallbackPreparer = { s, u -> prepareStream(s, u) }
            ),
            slotUpdater = SlotUpdaterImpl(_slotUpdates)
        )

        // 3. Сканируем папку плагинов и загружаем JAR файлы
        val pluginsDir = getPluginsDir()
        val jarFiles = pluginsDir.listFiles { file -> file.extension == "jar" } ?: emptyArray()

        jarFiles.forEach { jarFile ->
            try {
                loadPlugin(jarFile)
            } catch (e: Exception) {
                logger.error("Failed to reload plugin from {}", jarFile.name, e)
            }
        }

        updateSerializersModule()
        _pluginUpdates.tryEmit(Unit)

        logger.info("Plugin reload complete. Active plugins count: {}", loadedPlugins.size)
        return loadedPlugins.size
    }
}

class SlotUpdaterImpl(
    private val events: MutableSharedFlow<PluginManager.SlotUpdateEvent>
) : SlotUpdater {
    override suspend fun emitSlotUpdate(
        userId: Uuid,
        key: MediaKey,
        update: org.ensodai.avalonmediacard.contract.slot.SlotUpdate
    ) {
        events.emit(PluginManager.SlotUpdateEvent(userId, key, update))
    }
}
