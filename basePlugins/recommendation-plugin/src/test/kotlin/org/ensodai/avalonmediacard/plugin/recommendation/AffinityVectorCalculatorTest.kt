package org.ensodai.avalonmediacard.plugin.recommendation

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.*
import org.ensodai.avalonmediacard.contract.plugins.*
import org.ensodai.avalonmediacard.plugin.recommendation.calculator.AffinityVectorCalculator
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


@OptIn(ExperimentalUuidApi::class)
class AffinityVectorCalculatorTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val catalog = FileSystemMediaCatalog(json)

    @Test
    fun testVectorCalculationWithDuneAndInterstellar() = runBlocking {
        // Создаем фейковую телеметрию
        val userId = Uuid.random()
        val now = kotlin.time.Clock.System.now()

        val mockEvents = listOf(
            // Юзер искал Дюну
            TelemetryEvent(
                eventType = ClickstreamEventType.SEARCH,
                targetId = "438631",
                targetType = ClickstreamTargetType.MEDIA_MOVIE,
                dwellTimeMs = 120000,
                timestamp = now.minus(kotlin.time.Duration.parse("1h")),
                context = ClickstreamContext.SEARCH_PAGE
            ),
            // Юзер искал Интерстеллар
            TelemetryEvent(
                eventType = ClickstreamEventType.SEARCH,
                targetId = "157336",
                targetType = ClickstreamTargetType.MEDIA_MOVIE,
                dwellTimeMs = 120000,
                timestamp = now,
                context = ClickstreamContext.SEARCH_PAGE
            )
        )

        // Создаем моки провайдеров
        val telemetry = object : TelemetryProvider {
            override suspend fun getUserEvents(userId: Uuid, limit: Int): List<TelemetryEvent> = mockEvents
        }

        var savedVector: AffinityVector? = null
        val affinityStore = object : AffinityVectorStore {
            override suspend fun getVector(userId: Uuid): AffinityVector? = null
            override suspend fun getCachedEventCount(userId: Uuid): Int? = 0
            override suspend fun getUserEventCount(userId: Uuid): Int = mockEvents.size
            override suspend fun saveVector(userId: Uuid, vector: AffinityVector, eventCount: Int) {
                savedVector = vector
            }

            override val vectorUpdates: Flow<Uuid> = kotlinx.coroutines.flow.emptyFlow()
            override suspend fun getPendingUsers(limit: Int): List<Uuid> = emptyList()
        }

        val pluginContext = createMockPluginContext(catalog, telemetry, affinityStore)

        val calculator = AffinityVectorCalculator(pluginContext)

        // Вызываем расчет
        calculator.recalculateVector(userId)

        // Проверяем результат
        assertTrue(savedVector != null, "Vector should be saved")
        val vector = savedVector

        // Ожидаем, что жанр фантастика (878) и приключения (12) будут в топе
        requireNotNull(vector)
        val scifiWeight = vector.genreWeights["878"] ?: 0.0
        val adventureWeight = vector.genreWeights["12"] ?: 0.0

        println("Sci-Fi weight: $scifiWeight")
        println("Adventure weight: $adventureWeight")
        println("All genres: ${vector.genreWeights}")
        println("Top Actors: ${vector.actorWeights.entries.sortedByDescending { it.value }.take(5)}")

        assertTrue(scifiWeight > 0.5, "Sci-Fi weight should be high since both Interstellar and Dune are sci-fi")
    }

    private fun createMockPluginContext(
        mockCatalog: MediaCatalog,
        mockTelemetry: TelemetryProvider,
        mockAffinityStore: AffinityVectorStore
    ): PluginContext {
        return PluginContext(
            pluginDir = ".",
            logger = DefaultPluginLogger("AffinityTest"),
            httpClient = HttpClient(MockEngine) { engine { addHandler { respondOk() } } },
            catalog = mockCatalog,
            userMovies = object : UserMovieProvider {
                override suspend fun getUserMovies(userId: Uuid): List<UserMovieItem> =
                    listOf(
                        UserMovieItem(
                            id = Uuid.random(),
                            userId = userId,
                            catalogId = "tmdb",
                            mediaId = "438631", // Dune
                            mediaType = MediaType.MOVIE,
                            status = MediaStatus.COMPLETED,
                            userRating = 10,
                            lastWatchedAt = Clock.System.now().minus(kotlin.time.Duration.parse("2h"))
                        ),
                        UserMovieItem(
                            id = Uuid.random(),
                            userId = userId,
                            catalogId = "tmdb",
                            mediaId = "157336", // Interstellar
                            mediaType = MediaType.MOVIE,
                            status = MediaStatus.COMPLETED,
                            userRating = 10,
                            lastWatchedAt = Clock.System.now().minus(kotlin.time.Duration.parse("1h"))
                        )
                    )

                override fun observeUserMovies(userId: Uuid): Flow<List<UserMovieItem>> =
                    kotlinx.coroutines.flow.emptyFlow()

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
                override fun observeUserLists(userId: Uuid): Flow<List<CustomListInfo>> =
                    kotlinx.coroutines.flow.emptyFlow()

                override fun observeListItems(listId: Uuid): Flow<List<MediaKey>> = kotlinx.coroutines.flow.emptyFlow()
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
                override suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean = defaultValue
                override suspend fun setBoolean(key: String, value: Boolean) {}
                override fun observeString(key: String, defaultValue: String?): Flow<String?> = flowOf(null)
                override fun observeBoolean(key: String, defaultValue: Boolean): Flow<Boolean> = flowOf(defaultValue)
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
            telemetry = mockTelemetry,
            affinityStore = mockAffinityStore,
            genreDictionary = object : GenreDictionaryProvider {
                override suspend fun getLocalizedGenres(language: String): Map<String, String> = emptyMap()
            },
            integrationManager = object : IntegrationSettingsManager {
                override suspend fun getTmdbToken(userId: Uuid?): ResolvedIntegrationSetting? = null
                override suspend fun getTorrServerHost(userId: Uuid?): ResolvedIntegrationSetting? = null
                override suspend fun getTorrServerAuth(userId: Uuid?): String? = null
            },
            userSettings = object : UserPluginSettings {
                override suspend fun getString(userId: Uuid, key: String): String? = null
                override suspend fun setString(userId: Uuid, key: String, value: String) {}
                override suspend fun getBoolean(userId: Uuid, key: String, defaultValue: Boolean): Boolean = defaultValue
                override suspend fun setBoolean(userId: Uuid, key: String, value: Boolean) {}
                override fun observeString(userId: Uuid, key: String, defaultValue: String?): Flow<String?> = flowOf(defaultValue)
                override fun observeBoolean(userId: Uuid, key: String, defaultValue: Boolean): Flow<Boolean> = flowOf(defaultValue)
            }
        )
    }
}
