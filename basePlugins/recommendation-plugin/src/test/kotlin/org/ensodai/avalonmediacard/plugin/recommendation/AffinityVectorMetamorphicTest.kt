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
import org.ensodai.avalonmediacard.plugin.recommendation.calculator.TargetSpace
import org.ensodai.avalonmediacard.plugin.recommendation.calculator.calculateDwellTimeMultiplier
import org.ensodai.avalonmediacard.plugin.recommendation.calculator.calculateTimeDecay
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class AffinityVectorMetamorphicTest {

    @Test
    fun `test MR1 - Monotonic increase of genre weight on click event`() = runBlocking {
        val userId = Uuid.random()
        val now = Clock.System.now()

        // Сценарий 1: Холодный вектор
        val initialVector = AffinityVector()
        val initialSciFi = initialVector.genreWeights["878"] ?: 0.0
        assertEquals(0.0, initialSciFi, "Initial Sci-Fi weight should be 0.0")

        // Сценарий 2: Клик на Sci-Fi фильм
        val clickEvents = listOf(
            TelemetryEvent(
                eventType = ClickstreamEventType.CLICK,
                targetId = "438631",
                targetType = ClickstreamTargetType.MEDIA_MOVIE,
                dwellTimeMs = 30000L,
                timestamp = now,
                context = ClickstreamContext.DETAILS_PAGE
            )
        )

        var savedVector: AffinityVector? = null
        val affinityStore = object : AffinityVectorStore {
            override suspend fun getVector(userId: Uuid): AffinityVector? = savedVector
            override suspend fun getCachedEventCount(userId: Uuid): Int? = 0
            override suspend fun getUserEventCount(userId: Uuid): Int = clickEvents.size
            override suspend fun saveVector(userId: Uuid, vector: AffinityVector, eventCount: Int) {
                savedVector = vector
            }

            override val vectorUpdates: Flow<Uuid> = emptyFlow()
            override suspend fun getPendingUsers(limit: Int): List<Uuid> = emptyList()
        }

        val mockCatalog = FileSystemMediaCatalog(
            kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        )
        val pluginContext = createMockContext(mockCatalog, clickEvents, affinityStore)
        val calculator = AffinityVectorCalculator(pluginContext)

        calculator.recalculateVector(userId)

        assertNotNull(savedVector, "Vector should be saved")
        val postClickSciFi = savedVector.genreWeights["878"] ?: 0.0

        // Метаморфическое утверждение MR1: Клик монотонно увеличивает вес
        assertTrue(postClickSciFi >= initialSciFi, "MR1 Failure: Click MUST monotonically increase genre weight")
    }

    @Test
    fun `test MR2 - Monotonic decay of genre weight over time`() = runBlocking {
        val freshTimeDecay = calculateTimeDecay(0.0, TargetSpace.GENRE)
        val oldTimeDecay = calculateTimeDecay(180.0, TargetSpace.GENRE)

        // Метаморфическое утверждение MR2: Сдвиг времени монотонно уменьшает коэффициент затухания
        assertTrue(
            oldTimeDecay < freshTimeDecay,
            "MR2 Failure: Time decay MUST monotonically decrease weight for older events (old=$oldTimeDecay, fresh=$freshTimeDecay)"
        )
    }

    @Test
    fun `test MR3 - Bounce click produces zero multiplier without corrupting vector`() {
        val bounceMultiplier = calculateDwellTimeMultiplier(5000L)

        // Метаморфическое утверждение MR3: Быстрый отскок (< 10 сек) возвращает нейтральный 0.0
        assertEquals(
            0.0,
            bounceMultiplier,
            "MR3 Failure: Bounce click (< 10s) MUST yield 0.0 multiplier to avoid negative erosion"
        )
    }

    private fun createMockContext(
        mockCatalog: MediaCatalog,
        mockEvents: List<TelemetryEvent>,
        mockAffinityStore: AffinityVectorStore
    ): PluginContext {
        return PluginContext(
            pluginDir = ".",
            logger = DefaultPluginLogger("AffinityMetaTest"),
            httpClient = HttpClient(MockEngine { respondOk("") }),
            catalog = mockCatalog,
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
                override suspend fun getUserEvents(userId: Uuid, limit: Int): List<TelemetryEvent> = mockEvents
            },
            affinityStore = mockAffinityStore,
            genreDictionary = object : GenreDictionaryProvider {
                override suspend fun getLocalizedGenres(language: String): Map<String, String> = emptyMap()
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
