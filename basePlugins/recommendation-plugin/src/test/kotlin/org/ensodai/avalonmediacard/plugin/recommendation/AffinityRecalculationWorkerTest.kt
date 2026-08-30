package org.ensodai.avalonmediacard.plugin.recommendation

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.model.AffinityVector
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.UserEpisodeItem
import org.ensodai.avalonmediacard.contract.model.UserMovieItem
import org.ensodai.avalonmediacard.contract.plugins.*
import org.ensodai.avalonmediacard.plugin.recommendation.calculator.AffinityRecalculationWorker
import org.ensodai.avalonmediacard.plugin.recommendation.calculator.AffinityVectorCalculator
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class AffinityRecalculationWorkerTest {

    @Test
    fun testWorkerProcessesEachUserOnceAndDoesNotLoopInfinitely() = runBlocking {
        val user1 = Uuid.random()
        val user2 = Uuid.random()

        var getPendingUsersCallCount = 0

        val fakeAffinityStore = object : AffinityVectorStore {
            override val vectorUpdates = flowOf<Uuid>()
            override suspend fun getVector(userId: Uuid): AffinityVector? = null
            override suspend fun saveVector(userId: Uuid, vector: AffinityVector, eventCount: Int) {}
            override suspend fun getPendingUsers(limit: Int): List<Uuid> {
                getPendingUsersCallCount++
                return listOf(user1, user2)
            }
            override suspend fun getUserEventCount(userId: Uuid): Int = 10
            override suspend fun getCachedEventCount(userId: Uuid): Int = 10 // Same count -> skip
        }

        val testContext = PluginContext(
            pluginDir = ".",
            logger = DefaultPluginLogger("WorkerTest"),
            httpClient = HttpClient(MockEngine) { engine { addHandler { respondOk() } } },
            catalog = FileSystemMediaCatalog(Json { ignoreUnknownKeys = true }, "."),
            userMovies = object : UserMovieProvider {
                override suspend fun getUserMovies(userId: Uuid): List<UserMovieItem> = emptyList()
                override fun observeUserMovies(userId: Uuid): Flow<List<UserMovieItem>> = flowOf(emptyList())
                override suspend fun updateUserMovie(item: UserMovieItem): Boolean = false
                override suspend fun deleteUserMovie(userId: Uuid, mediaId: String): Boolean = false
                override suspend fun getUserEpisodes(userId: Uuid, mediaId: String): List<UserEpisodeItem> = emptyList()
                override suspend fun updateUserEpisode(item: UserEpisodeItem): Boolean = false
                override suspend fun notifyUpdate() {}
            },
            userCustomLists = object : UserCustomListProvider {
                override fun observeUserLists(userId: Uuid): Flow<List<CustomListInfo>> = flowOf(emptyList())
                override fun observeListItems(listId: Uuid): Flow<List<MediaKey>> = flowOf(emptyList())
                override suspend fun getCustomListsWithStatus(userId: Uuid, mediaKey: MediaKey): List<CustomListStatus> = emptyList()
                override suspend fun toggleList(userId: Uuid, listId: String, mediaKey: MediaKey) {}
                override suspend fun createList(userId: Uuid, listName: String, mediaKey: MediaKey) {}
            },
            userEpisodes = object : UserEpisodeProvider {
                override suspend fun getEpisodesProgress(userId: Uuid, mediaId: String, catalogId: String): List<UserEpisodeProgress> = emptyList()
                override suspend fun saveEpisodeProgress(
                    userId: Uuid,
                    catalogId: String,
                    mediaId: String,
                    season: Int,
                    episode: Int,
                    progressSeconds: Long,
                    durationSeconds: Long,
                    isWatched: Boolean
                ) {}
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
            telemetry = object : TelemetryProvider {
                override suspend fun getUserEvents(userId: Uuid, limit: Int) = emptyList<org.ensodai.avalonmediacard.contract.model.TelemetryEvent>()
            },
            affinityStore = fakeAffinityStore,
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

        val calculator = AffinityVectorCalculator(testContext)
        val worker = AffinityRecalculationWorker(testContext, calculator)
        worker.start()

        // Ждем 500 мс
        delay(500.milliseconds)

        // getPendingUsers должен вызваться максимум 2 раза (1 раз для получения и 1 раз убедиться, что отфильтрованный список пуст)
        // В забагованном коде здесь было бы бесконечно много вызовов!
        assertTrue(getPendingUsersCallCount in 1..2, "getPendingUsers must not loop infinitely! Call count: $getPendingUsersCallCount")

        worker.stop()
    }
}
