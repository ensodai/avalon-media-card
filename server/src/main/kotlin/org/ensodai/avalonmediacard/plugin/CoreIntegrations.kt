package org.ensodai.avalonmediacard.plugin

import io.ktor.client.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.ensodai.avalonmediacard.auth.TraktOAuthProvider
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.IntegrationService
import org.ensodai.avalonmediacard.contract.model.MediaCatalog
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.UserMovieItem
import org.ensodai.avalonmediacard.contract.model.UserSettingsDto
import org.ensodai.avalonmediacard.contract.plugins.*
import org.ensodai.avalonmediacard.contract.slot.*
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.plugin.playback.CorePlaybackSlotFactory
import org.ensodai.avalonmediacard.recommendation.RecommendationEngineRegistry
import org.ensodai.avalonmediacard.repository.SystemSettingsRepository
import org.ensodai.avalonmediacard.repository.UserExternalAuthRepository
import org.ensodai.avalonmediacard.repository.UserFeedCacheRepository
import org.ensodai.avalonmediacard.repository.UserIntegrationSettingsRepository
import org.ensodai.avalonmediacard.repository.UserSettingsRepository
import org.ensodai.avalonmediacard.tmdb.TmdbApi
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Single
class CoreIntegrations(
    private val sharedHttpClient: HttpClient,
    private val catalog: MediaCatalog,
    private val userMovieProvider: UserMovieProvider,
    private val userEpisodeProvider: UserEpisodeProvider,
    private val userCustomLists: UserCustomListProvider,
    private val systemSettingsRepository: SystemSettingsRepository,
    private val tmdbApi: TmdbApi,
    private val userExternalAuthRepository: UserExternalAuthRepository,
    private val traktOAuthProvider: TraktOAuthProvider,
    private val sourceMappingProvider: SourceMappingProvider,
    private val userMediaBindings: UserMediaBindingProvider,
    private val recommendationRegistry: RecommendationEngineRegistry,
    private val telemetryProvider: TelemetryProvider,
    private val affinityStore: AffinityVectorStore,
    private val genreDictionaryProvider: GenreDictionaryProvider,
    private val userFeedCacheRepository: UserFeedCacheRepository,
    private val userSettingsRepository: UserSettingsRepository
) {

    @Serializable
    data class ValidateTmdbToken(
        val tmdb_read_token: String
    ) : ServerAction, UserAwareCommand {
        override var userId: Uuid? = null
    }

    @Serializable
    class DisconnectTrakt : ServerAction, UserAwareCommand {
        override var userId: Uuid? = null
    }

    @Serializable
    data class TraktToggleSetting(
        val key: String,
        val value: Boolean
    ) : ServerAction

    private var tmdbValidationStatus = ValidationStatus.None
    private var tmdbValidationMessage: String? = null
    private var isTmdbSaveEnabled = true
    private var pluginContexts: Map<String, PluginContext> = emptyMap()

    lateinit var settings: PluginSettings
        private set

    fun initialize(
        changeEvents: MutableSharedFlow<String>,
        pluginContexts: MutableMap<String, PluginContext>,
        userIntegrationSettingsRepository: UserIntegrationSettingsRepository,
        streams: StreamRegistry,
        slotUpdater: SlotUpdater
    ) {
        this.pluginContexts = pluginContexts
        val coreSettings = PluginSettingsImpl("core", systemSettingsRepository, changeEvents)
        this.settings = coreSettings

        val coreContext = PluginContext(
            pluginDir = "",
            logger = DefaultPluginLogger("Core"),
            httpClient = sharedHttpClient,
            catalog = catalog,
            i18n = PluginI18nLoader.loadI18n(this::class.java.classLoader),
            userMovies = userMovieProvider,
            userEpisodes = userEpisodeProvider,
            userCustomLists = userCustomLists,
            settings = coreSettings,
            userSettings = UserPluginSettingsImpl("core", userIntegrationSettingsRepository),
            integrationManager = IntegrationSettingsManagerImpl("core", systemSettingsRepository, userIntegrationSettingsRepository, userExternalAuthRepository, userSettingsRepository),
            userMediaBindings = userMediaBindings,
            sourceMappings = sourceMappingProvider,
            updater = slotUpdater,
            streams = streams,
            recommendations = recommendationRegistry,
            telemetry = telemetryProvider,
            affinityStore = affinityStore,
            genreDictionary = genreDictionaryProvider,
            feedCache = userFeedCacheRepository,
            userGlobalSettings = object : UserGlobalSettingsProvider {
                override suspend fun getUserSettings(userId: Uuid): UserSettingsDto? =
                    userSettingsRepository.getUserSettings(userId)
            }
        )
        pluginContexts["core"] = coreContext

        setupCoreSlotsAndCommands(coreContext, changeEvents)
    }

    private fun setupCoreSlotsAndCommands(
        coreContext: PluginContext,
        changeEvents: MutableSharedFlow<String>
    ) {
        coreContext.slots.onScreen<Screen.Integrations> { _, userId ->
            val locale = if (userId != null) userSettingsRepository.getUserLocale(userId) else "ru"
            val isEn = locale.startsWith("en", ignoreCase = true)

            val traktFlow = changeEvents
                .filter { it.startsWith("plugin:core:trakt_") || it == "plugin:core:trakt_auth_changed" }
                .map { Unit }
                .onStart { emit(Unit) }
                .map {
                    val auth = if (userId != null) userExternalAuthRepository.getToken(
                        userId,
                        IntegrationService.TRAKT
                    ) else null

                    if (auth == null) {
                        val authUrl = traktOAuthProvider.getAuthUrl(state = "trakt")
                        SlotUpdate(
                            slotId = SlotId.Integrations,
                            nodeId = "core_trakt",
                            state = org.ensodai.avalonmediacard.contract.slot.SlotState.Content(
                                SlotData.SettingsGroup(
                                    title = "Trakt.tv",
                                    description = if (isEn) "Sync your watch history, ratings, and lists with Trakt." else "Синхронизируйте вашу историю просмотров, оценки и списки с Trakt.",
                                    fields = emptyList(),
                                    saveAction = ActionOpenUrl(authUrl),
                                    saveActionLabel = if (isEn) "Log in with Trakt.tv" else "Войти через Trakt.tv",
                                    connectionStatus = ValidationStatus.None
                                )
                            )
                        )
                    } else {
                        SlotUpdate(
                            slotId = SlotId.Integrations,
                            nodeId = "core_trakt",
                            state = org.ensodai.avalonmediacard.contract.slot.SlotState.Content(
                                SlotData.SettingsGroup(
                                    title = "Trakt.tv",
                                    description = if (isEn) "Manage your profile synchronization." else "Управление синхронизацией вашего профиля.",
                                    fields = emptyList(),
                                    saveAction = DisconnectTrakt(),
                                    saveActionLabel = if (isEn) "Disconnect Trakt" else "Отключить Trakt",
                                    connectionStatus = ValidationStatus.Success
                                )
                            )
                        )
                    }
                }

            ScreenSlots(
                layout = listOf(
                    LayoutNode("core_trakt", SlotId.Integrations)
                ),
                flow = traktFlow.map { ScreenStreamEvent.Update(it) }
            )
        }

        val playbackSlotFactory = CorePlaybackSlotFactory("core", coreContext)
        val playButtonsLayout = listOf(LayoutNode("core", SlotId.PlayButtons))

        coreContext.slots.declare<Screen.MovieDetails>(listOf(SlotId.PlayButtons)) {
            playButtonsLayout
        }
        coreContext.slots.onScreen<Screen.MovieDetails> { screen, userId ->
            val locale = if (userId != null) userSettingsRepository.getUserLocale(userId) else "ru"
            ScreenSlots(
                layout = playButtonsLayout,
                flow = playbackSlotFactory.buildPlayButtonsFlow(screen.key, userId, isTvShow = false, locale = locale)
                    .map { ScreenStreamEvent.Update(it) }
            )
        }

        coreContext.slots.declare<Screen.TvShowDetails>(listOf(SlotId.PlayButtons)) {
            playButtonsLayout
        }
        coreContext.slots.onScreen<Screen.TvShowDetails> { screen, userId ->
            val locale = if (userId != null) userSettingsRepository.getUserLocale(userId) else "ru"
            ScreenSlots(
                layout = playButtonsLayout,
                flow = playbackSlotFactory.buildPlayButtonsFlow(screen.key, userId, isTvShow = true, locale = locale)
                    .map { ScreenStreamEvent.Update(it) }
            )
        }

        coreContext.actions.bind<ValidateTmdbToken> { cmd, userId ->
            val locale = if (userId != null) userSettingsRepository.getUserLocale(userId) else "ru"
            val isEn = locale.startsWith("en", ignoreCase = true)

            tmdbValidationStatus = ValidationStatus.Pending
            tmdbValidationMessage = if (isEn) "Checking token..." else "Проверяем токен..."
            isTmdbSaveEnabled = false
            changeEvents.emit("plugin:core:tmdb_read_token")

            val isValid = tmdbApi.validateToken(cmd.tmdb_read_token)
            if (isValid) {
                systemSettingsRepository.saveSetting("tmdb_read_token", cmd.tmdb_read_token)
                tmdbApi.resetClient()

                tmdbValidationStatus = ValidationStatus.Success
                tmdbValidationMessage = if (isEn) "Token verified and saved!" else "Токен успешно проверен и сохранен!"
                isTmdbSaveEnabled = true
            } else {
                tmdbValidationStatus = ValidationStatus.Error
                tmdbValidationMessage = if (isEn) "Invalid token or network error" else "Неверный токен или нет сети"
                isTmdbSaveEnabled = false
            }
            changeEvents.emit("plugin:core:tmdb_read_token")
            ActionResult.NoOp
        }

        coreContext.actions.bind<DisconnectTrakt> { cmd, userId ->
            if (userId != null) {
                userExternalAuthRepository.deleteToken(userId, IntegrationService.TRAKT)
                changeEvents.emit("plugin:core:trakt_auth_changed")
            }
            ActionResult.NoOp
        }

        coreContext.actions.bind<TraktToggleSetting> { cmd, userId ->
            coreContext.settings.setBoolean(cmd.key, cmd.value)
            ActionResult.NoOp
        }

        coreContext.actions.bind<SaveEpisodeProgressCommand> { cmd, userId ->
            if (userId != null) {
                coreContext.userEpisodes.saveEpisodeProgress(
                    userId = userId,
                    mediaId = cmd.mediaId,
                    season = cmd.season,
                    episode = cmd.episode,
                    progressSeconds = cmd.progressSeconds,
                    durationSeconds = cmd.durationSeconds,
                    isWatched = cmd.isWatched
                )
                coreContext.userMovies.notifyUpdate()
            }
            ActionResult.NoOp
        }

        coreContext.actions.bind<SaveMovieProgressCommand> { cmd, userId ->
            if (userId != null) {
                val currentMovie = coreContext.userMovies.getUserMovies(userId).find { it.mediaId == cmd.mediaId }
                val updated = if (currentMovie != null) {
                    currentMovie.copy(
                        progressSeconds = cmd.progressSeconds,
                        durationSeconds = cmd.durationSeconds,
                        status = if (cmd.isWatched) MediaStatus.COMPLETED else currentMovie.status,
                        lastWatchedAt = kotlin.time.Clock.System.now()
                    )
                } else {
                    UserMovieItem(
                        id = Uuid.random(),
                        userId = userId,
                        catalogId = "tmdb",
                        mediaId = cmd.mediaId,
                        mediaType = MediaType.MOVIE,
                        status = if (cmd.isWatched) MediaStatus.COMPLETED else MediaStatus.WATCHING,
                        progressSeconds = cmd.progressSeconds,
                        durationSeconds = cmd.durationSeconds,
                        lastWatchedAt = Clock.System.now()
                    )
                }
                coreContext.userMovies.updateUserMovie(updated)
            }
            ActionResult.NoOp
        }
    }

    suspend fun searchSources(key: MediaKey, userId: Uuid, forceRefresh: Boolean = false) {
        if (forceRefresh) {
            userMediaBindings.deleteAllBindings(userId, key.id)
        }
        fetchAndEmitSourcesForAllPlugins(key, userId)
    }

    private suspend fun fetchAndEmitSourcesForAllPlugins(
        key: MediaKey,
        userId: Uuid
    ) {
        coroutineScope {
            for ((pluginId, context) in pluginContexts) {
                val streamFlow = context.streams.getStreams(key, season = null, episode = null, userId = userId)
                if (streamFlow != null) {
                    launch {
                        val providerTitle = context.pluginName.ifBlank {
                            pluginId.replaceFirstChar { it.uppercase() }
                        }
                        context.updater.emitSlotUpdate(
                            userId,
                            key,
                            SlotUpdate(SlotId.MediaSources, pluginId, SlotState.Loading())
                        )
                        val collectedStreams = mutableListOf<MediaStream>()
                        try {
                            streamFlow.collect { stream ->
                                collectedStreams.add(stream)
                            }
                            val subFilters = buildSubFilters(collectedStreams, key)
                            context.updater.emitSlotUpdate(
                                userId,
                                key,
                                SlotUpdate(
                                    slotId = SlotId.MediaSources,
                                    nodeId = pluginId,
                                    state = SlotState.Content(
                                        SlotData.MediaSources(
                                            sources = collectedStreams,
                                            mediaKey = key,
                                            providerId = pluginId,
                                            providerTitle = providerTitle,
                                            subFilters = subFilters
                                        )
                                    )
                                )
                            )
                        } catch (e: Exception) {
                            context.logger.error("Error fetching streams for plugin $pluginId", e)
                            context.updater.emitSlotUpdate(
                                userId,
                                key,
                                SlotUpdate(
                                    slotId = SlotId.MediaSources,
                                    nodeId = pluginId,
                                    state = SlotState.Content(
                                        SlotData.MediaSources(
                                            sources = emptyList(),
                                            mediaKey = key,
                                            providerId = pluginId,
                                            providerTitle = providerTitle,
                                            subFilters = emptyList()
                                        )
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun buildSubFilters(streams: List<MediaStream>, key: MediaKey): List<SlotData.SourceSubFilter> {
        if (streams.isEmpty()) return emptyList()

        // 1. Explicit subFilterId and subFilterLabel from streams
        val explicitGroups = streams
            .filter { !it.subFilterId.isNullOrBlank() }
            .groupBy { it.subFilterId!! }

        if (explicitGroups.isNotEmpty()) {
            return explicitGroups.map { (id, list) ->
                val label = list.firstOrNull()?.subFilterLabel ?: id
                SlotData.SourceSubFilter(id = id, label = label, count = list.size)
            }
        }

        // 2. TV show season grouping
        if (key.type == EntityType.TV) {
            val seasonGroups = streams
                .filter { it.seasonNumber != null }
                .groupBy { it.seasonNumber!! }
                .toSortedMap()

            if (seasonGroups.size > 1) {
                return seasonGroups.map { (s, list) ->
                    SlotData.SourceSubFilter(
                        id = "season_$s",
                        label = "Season $s",
                        count = list.size
                    )
                }
            }
        }

        // 3. Torrent / Trackers grouping
        val hasTrackers = streams.any { it.type == StreamType.Torrent || it.type == StreamType.Magnet }
        if (hasTrackers) {
            val trackerGroups = streams
                .filter { it.sourceName.isNotBlank() }
                .groupBy { it.sourceName }

            if (trackerGroups.size > 1) {
                return trackerGroups.map { (tracker, list) ->
                    SlotData.SourceSubFilter(
                        id = "tracker_${tracker.lowercase()}",
                        label = tracker,
                        count = list.size
                    )
                }
            }
        }

        return emptyList()
    }

    fun provideSerializers(): SerializersModule {
        return SerializersModule {
            polymorphic(Action::class) {
                subclass(ActionNavigate::class)
                subclass(ActionPlayVideo::class)
                subclass(ActionPreparePlayer::class)
                subclass(ActionOpenSources::class)
                subclass(ActionOpenUrl::class)

                subclass(SaveEpisodeProgressCommand::class)
                subclass(SaveMovieProgressCommand::class)
                subclass(SearchQueryCommand::class)
                subclass(RefreshIntegrationsCommand::class)
                subclass(UpdateIntegrationSettingCommand::class)
                subclass(ValidateTmdbToken::class)
                subclass(DisconnectTrakt::class)
                subclass(TraktToggleSetting::class)
                subclass(UploadCustomTorrentCommand::class)
            }
            polymorphic(ServerAction::class) {
                subclass(ValidateTmdbToken::class)
                subclass(DisconnectTrakt::class)
                subclass(TraktToggleSetting::class)
                subclass(SaveEpisodeProgressCommand::class)
                subclass(SaveMovieProgressCommand::class)
                subclass(SearchQueryCommand::class)
                subclass(RefreshIntegrationsCommand::class)
                subclass(UpdateIntegrationSettingCommand::class)
                subclass(UploadCustomTorrentCommand::class)
            }
        }
    }
}
