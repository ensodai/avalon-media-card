package org.ensodai.avalonmediacard.plugin.recommendation

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.*
import org.ensodai.avalonmediacard.contract.plugins.*
import org.ensodai.avalonmediacard.plugin.recommendation.calculator.AffinityVectorCalculator
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.DashboardGenerator
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class RealDataVisualSimulationTest {

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    private val catalog = FileSystemMediaCatalog(json)

    @Test
    fun `test Visual Dashboard Generation for Sci-Fi Enthusiast (Interstellar + Dune)`() = runBlocking {
        val userId = Uuid.random()
        val now = Clock.System.now()

        // Юзер активно искал и смотрел Дюну (438631) и Интерстеллар (157336)
        val telemetryEvents = listOf(
            TelemetryEvent(
                eventType = ClickstreamEventType.PLAYBACK_STOP,
                targetId = "438631",
                targetType = ClickstreamTargetType.MEDIA_MOVIE,
                dwellTimeMs = 7200000L,
                timestamp = now.minus(2.hours),
                context = ClickstreamContext.DETAILS_PAGE,
                payload = ClickstreamPayload.PlaybackStop(completionPercentage = 0.95)
            ),
            TelemetryEvent(
                eventType = ClickstreamEventType.PLAYBACK_STOP,
                targetId = "157336",
                targetType = ClickstreamTargetType.MEDIA_MOVIE,
                dwellTimeMs = 7200000L,
                timestamp = now,
                context = ClickstreamContext.DETAILS_PAGE,
                payload = ClickstreamPayload.PlaybackStop(completionPercentage = 0.98)
            )
        )

        val context = createRealDataContext(telemetryEvents)
        val calculator = AffinityVectorCalculator(context)

        // 1. Считаем аффинити-вектор
        calculator.recalculateVector(userId)
        val vector = context.affinityStore.getVector(userId) ?: AffinityVector()

        // 2. Генерируем дашборд
        val generator = DashboardGenerator(context)
        val sections = generator.generate(vector)

        println("\n=======================================================================")
        println(" 🎬 РЕЗУЛЬТАТЫ СИМУЛЯЦИИ ДАШБОРДА ДЛЯ ЗРИТЕЛЯ (Sci-Fi / Дюна + Интерстеллар)")
        println("=======================================================================")
        println(" 📊 Веса жанров: ${vector.genreWeights}")
        println(" 🎭 Веса актеров (Топ-3): ${vector.actorWeights.entries.sortedByDescending { it.value }.take(3)}")
        println(" 📌 Количество сгенерированных каруселей: ${sections.size}")
        println("-----------------------------------------------------------------------")
        sections.forEachIndexed { i, section ->
            println("[Полка #${i + 1}] [${section.type}] \"${section.title}\"")
            println("   📝 Описание: ${section.description}")
            println("   🎯 TMDB Params: ${section.queryParams}")
            println("   ⭐ Relevance Score: ${section.weight}")
            println()
        }
        println("=======================================================================\n")
    }

    @Test
    fun `test Visual Dashboard Generation for Anime Enthusiast (Spirited Away)`() = runBlocking {
        val userId = Uuid.random()
        val now = Clock.System.now()

        // Юзер смотрел Унесенные Призраками (movie_129.json)
        val telemetryEvents = listOf(
            TelemetryEvent(
                eventType = ClickstreamEventType.PLAYBACK_STOP,
                targetId = "129",
                targetType = ClickstreamTargetType.MEDIA_MOVIE,
                dwellTimeMs = 7200000L,
                timestamp = now,
                context = ClickstreamContext.DETAILS_PAGE,
                payload = ClickstreamPayload.PlaybackStop(completionPercentage = 1.0)
            )
        )

        val context = createRealDataContext(telemetryEvents)
        val calculator = AffinityVectorCalculator(context)

        calculator.recalculateVector(userId)
        val vector = context.affinityStore.getVector(userId) ?: AffinityVector()

        val generator = DashboardGenerator(context)
        val sections = generator.generate(vector)

        println("\n=======================================================================")
        println(" ⛩️ РЕЗУЛЬТАТЫ СИМУЛЯЦИИ ДАШБОРДА ДЛЯ ЗРИТЕЛЯ АНИМЕ (Унесенные Призраками)")
        println("=======================================================================")
        println(" 📊 Веса жанров: ${vector.genreWeights}")
        println(" 📌 Количество сгенерированных каруселей: ${sections.size}")
        println("-----------------------------------------------------------------------")
        sections.forEachIndexed { i, section ->
            println("[Полка #${i + 1}] [${section.type}] \"${section.title}\"")
            println("   📝 Описание: ${section.description}")
            println("   🎯 TMDB Params: ${section.queryParams}")
            println("   ⭐ Relevance Score: ${section.weight}")
            println()
        }
        println("=======================================================================\n")
    }

    private fun createRealDataContext(events: List<TelemetryEvent>): PluginContext {
        var storedVector: AffinityVector? = null
        val affinityStore = object : AffinityVectorStore {
            override suspend fun getVector(userId: Uuid): AffinityVector? = storedVector
            override suspend fun getCachedEventCount(userId: Uuid): Int? = 0
            override suspend fun getUserEventCount(userId: Uuid): Int = events.size
            override suspend fun saveVector(userId: Uuid, vector: AffinityVector, eventCount: Int) {
                storedVector = vector
            }

            override val vectorUpdates: Flow<Uuid> = emptyFlow()
            override suspend fun getPendingUsers(limit: Int): List<Uuid> = emptyList()
        }

        return PluginContext(
            pluginDir = ".",
            logger = DefaultPluginLogger("RealDataSimTest"),
            httpClient = HttpClient(MockEngine { respondOk("") }),
            catalog = catalog,
            userMovies = object : UserMovieProvider {
                override suspend fun getUserMovies(userId: Uuid): List<UserMovieItem> =
                    emptyList()

                override fun observeUserMovies(userId: Uuid): Flow<List<UserMovieItem>> =
                    emptyFlow()

                override suspend fun updateUserMovie(item: UserMovieItem): Boolean =
                    false

                override suspend fun deleteUserMovie(userId: Uuid, mediaId: String): Boolean = false
                override suspend fun getUserEpisodes(
                    userId: Uuid,
                    mediaId: String
                ): List<UserEpisodeItem> = emptyList()

                override suspend fun updateUserEpisode(item: UserEpisodeItem): Boolean =
                    false

                override suspend fun notifyUpdate() {}
            },
            userCustomLists = object : UserCustomListProvider {
                override fun observeUserLists(userId: Uuid): Flow<List<CustomListInfo>> = emptyFlow()
                override fun observeListItems(listId: Uuid): Flow<List<MediaKey>> = emptyFlow()
                override suspend fun getCustomListsWithStatus(
                    userId: Uuid,
                    mediaKey: MediaKey
                ): List<CustomListStatus> = emptyList()

                override suspend fun toggleList(userId: Uuid, listId: String, mediaKey: MediaKey) {}
                override suspend fun createList(userId: Uuid, listName: String, mediaKey: MediaKey) {}
            },
            userEpisodes = object : UserEpisodeProvider {
                override suspend fun getEpisodesProgress(
                    userId: Uuid,
                    mediaId: String,
                    catalogId: String
                ): List<UserEpisodeProgress> = emptyList()

                override suspend fun saveEpisodeProgress(
                    userId: Uuid,
                    catalogId: String,
                    mediaId: String,
                    season: Int,
                    episode: Int,
                    progressSeconds: Long,
                    durationSeconds: Long,
                    isWatched: Boolean
                ) {
                }
            },
            torrentMappings = object : TorrentMappingProvider {
                override suspend fun getMappingsByHash(torrentHash: String): List<TorrentMapping> = emptyList()
                override suspend fun getMappingsByMediaId(mediaId: String): List<TorrentMapping> = emptyList()
                override suspend fun saveMapping(
                    torrentHash: String,
                    filePath: String,
                    seasons: List<Int>?,
                    episodes: List<Int>?,
                    isAbsolute: Boolean,
                    isManual: Boolean,
                    mediaId: String?,
                    fileIndex: Int?,
                    fileSize: Long?
                ): TorrentMapping = TODO()

                override suspend fun clearMappingsByMediaId(mediaId: String) {}
            },
            settings = object : PluginSettings {
                override suspend fun getString(key: String): String? = null
                override suspend fun setString(key: String, value: String) {}
                override suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean = false
                override suspend fun setBoolean(key: String, value: Boolean) {}
                override fun observeString(key: String, defaultValue: String?): Flow<String?> = emptyFlow()
                override fun observeBoolean(key: String, defaultValue: Boolean): Flow<Boolean> = emptyFlow()
            },
            userMediaBindings = object : UserMediaBindingProvider {
                override suspend fun getBinding(userId: Uuid, mediaId: String, sourceType: String): String? = null
                override suspend fun saveBinding(userId: Uuid, mediaId: String, sourceType: String, sourceId: String) {}
                override suspend fun deleteBinding(userId: Uuid, mediaId: String, sourceType: String) {}
            },
            recommendations = object : RecommendationEngineRegistrar {
                override fun registerEngine(engine: RecommendationEngine) {}
                override fun unregisterEngine() {}
            },
            telemetry = object : TelemetryProvider {
                override suspend fun getUserEvents(userId: Uuid, limit: Int): List<TelemetryEvent> = events
            },
            affinityStore = affinityStore,
            genreDictionary = object : GenreDictionaryProvider {
                override suspend fun getLocalizedGenres(language: String): Map<String, String> = mapOf(
                    "878" to "Фантастика",
                    "12" to "Приключения",
                    "16" to "Мультфильм",
                    "18" to "Драма",
                    "28" to "Боевик"
                )
            },
            integrationManager = object : org.ensodai.avalonmediacard.contract.plugins.IntegrationSettingsManager {
                override suspend fun getTmdbToken(userId: kotlin.uuid.Uuid?): org.ensodai.avalonmediacard.contract.plugins.ResolvedIntegrationSetting? = null
                override suspend fun getTorrServerHost(userId: kotlin.uuid.Uuid?): org.ensodai.avalonmediacard.contract.plugins.ResolvedIntegrationSetting? = null
                override suspend fun getTorrServerAuth(userId: kotlin.uuid.Uuid?): String? = null
            },
            userSettings = object : org.ensodai.avalonmediacard.contract.plugins.UserPluginSettings {
                override suspend fun getString(userId: kotlin.uuid.Uuid, key: String): String? = null
                override suspend fun setString(userId: kotlin.uuid.Uuid, key: String, value: String) {}
                override suspend fun getBoolean(userId: kotlin.uuid.Uuid, key: String, defaultValue: Boolean): Boolean = defaultValue
                override suspend fun setBoolean(userId: kotlin.uuid.Uuid, key: String, value: Boolean) {}
                override fun observeString(userId: kotlin.uuid.Uuid, key: String, defaultValue: String?): kotlinx.coroutines.flow.Flow<String?> = kotlinx.coroutines.flow.flowOf(defaultValue)
                override fun observeBoolean(userId: kotlin.uuid.Uuid, key: String, defaultValue: Boolean): kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.flowOf(defaultValue)
            }
        )
    }
}
