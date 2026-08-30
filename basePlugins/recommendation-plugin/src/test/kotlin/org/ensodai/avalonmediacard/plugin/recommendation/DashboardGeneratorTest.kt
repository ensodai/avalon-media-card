package org.ensodai.avalonmediacard.plugin.recommendation

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.model.UserEpisodeItem
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.AffinityVector
import org.ensodai.avalonmediacard.contract.model.TelemetryEvent
import org.ensodai.avalonmediacard.contract.model.UserMovieItem
import org.ensodai.avalonmediacard.contract.plugins.*
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.DashboardGenerator
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class DashboardGeneratorTest {

    private fun createMockPluginContext(): PluginContext {
        return PluginContext(
            pluginDir = ".",
            logger = DefaultPluginLogger("DashboardTest"),
            httpClient = HttpClient(MockEngine { respondOk("") }),
            catalog = FileSystemMediaCatalog(Json {
                ignoreUnknownKeys = true
            }, "."),
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
                override fun observeUserLists(userId: Uuid): Flow<List<CustomListInfo>> =
                    emptyFlow()

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
                override fun observeString(key: String, defaultValue: String?): Flow<String?> =
                    emptyFlow()

                override fun observeBoolean(key: String, defaultValue: Boolean): Flow<Boolean> =
                    emptyFlow()
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
                override suspend fun getUserEvents(userId: Uuid, limit: Int): List<TelemetryEvent> = emptyList()
            },
            affinityStore = object : AffinityVectorStore {
                override val vectorUpdates: Flow<Uuid> = emptyFlow()
                override suspend fun getVector(userId: Uuid): AffinityVector? = null
                override suspend fun saveVector(userId: Uuid, vector: AffinityVector, eventCount: Int) {}
                override suspend fun getPendingUsers(limit: Int): List<Uuid> = emptyList()
                override suspend fun getUserEventCount(userId: Uuid): Int = 0
                override suspend fun getCachedEventCount(userId: Uuid): Int? = null
            },
            genreDictionary = object : GenreDictionaryProvider {
                override suspend fun getLocalizedGenres(language: String): Map<String, String> =
                    mapOf("878" to "Фантастика", "28" to "Боевик", "16" to "Мультфильм")
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

    @Test
    fun `test DashboardGenerator transforms AffinityVector to DynamicSections correctly`() = runBlocking {
        val pluginContext = createMockPluginContext()
        val generator = DashboardGenerator(pluginContext)

        val vector = AffinityVector(
            genreWeights = mapOf("878" to 0.9, "28" to 0.8), // Сильная любовь к фантастике и боевикам
            moodWeights = mapOf("adrenaline" to 0.95), // Ищет адреналин
            sessionBingeVector = mapOf("878" to 1.0) // Запойный просмотр фантастики
        )

        val sections = generator.generate(vector)

        // Должно сгенерировать хотя бы несколько секций
        assertTrue(sections.isNotEmpty(), "DashboardGenerator should generate at least one section")

        // Должен быть блюпринт ExploitCurrentObsession (По текущему запою)
        val hasBingeSection = sections.any { it.id.startsWith("exploit_current_obsession", ignoreCase = true) }
        assertTrue(hasBingeSection, "Should include ExploitCurrentObsession blueprint based on sessionBingeVector")

        // Проверяем, что жанры перевелись из ID "878" в "Фантастика"
        val bingeSection = sections.first { it.id.startsWith("exploit_current_obsession", ignoreCase = true) }
        assertTrue(
            bingeSection.title.contains("Фантастика") || bingeSection.title.contains("одержимость"),
            "Title should contain localized genre name 'Фантастика', but was: ${bingeSection.title}"
        )

        // Проверяем лимиты и параметры
        assertTrue(sections.size <= 20, "Should not exceed max sections limit (usually around 10-15)")

        println("Generated sections:")
        sections.forEach { println(" - [${it.type}] ${it.title}: ${it.description} (score: ${it.weight})") }
    }
}
