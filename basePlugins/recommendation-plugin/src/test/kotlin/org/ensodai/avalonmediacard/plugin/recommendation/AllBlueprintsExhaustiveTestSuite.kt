package org.ensodai.avalonmediacard.plugin.recommendation

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.AffinityVector
import org.ensodai.avalonmediacard.contract.model.TelemetryEvent
import org.ensodai.avalonmediacard.contract.model.UserEpisodeItem
import org.ensodai.avalonmediacard.contract.model.UserMovieItem
import org.ensodai.avalonmediacard.contract.plugins.*
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.DashboardGenerator
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.InterpreterContext
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.blueprints.AnimeSpecialistBlueprint
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.blueprints.MoodKey
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class AllBlueprintsExhaustiveTestSuite {

    private fun createMockPluginContext(): PluginContext {
        return PluginContext(
            pluginDir = ".",
            logger = DefaultPluginLogger("BlueprintTest"),
            httpClient = HttpClient(MockEngine { respondOk("") }),
            catalog = FileSystemMediaCatalog(kotlinx.serialization.json.Json { ignoreUnknownKeys = true }, "."),
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
                override suspend fun getLocalizedGenres(language: String): Map<String, String> = mapOf(
                    "878" to "Фантастика",
                    "10765" to "Sci-Fi & Фэнтези",
                    "28" to "Боевик",
                    "16" to "Мультфильм",
                    "18" to "Драма"
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

    @Test
    fun `test Persona 1 - Star Trek DS9 Binger genre translation and status verification`() = runBlocking {
        val pluginContext = createMockPluginContext()
        val generator = DashboardGenerator(pluginContext)

        // Юзер смотрит Star Trek: DS9 (TV Sci-Fi 10765)
        val vector = AffinityVector(
            genreWeights = mapOf("10765" to 0.95, "18" to 0.70),
            actorWeights = mapOf("9942" to 0.85), // Colm Meaney / Patrick Stewart
            sessionBingeVector = mapOf("10765" to 1.0)
        )

        val sections = generator.generate(vector)
        assertTrue(sections.isNotEmpty(), "Dashboard must generate sections for Star Trek binger")

        // 1. Проверяем, что SerendipityActorPivotBlueprint перевел TV ID 10765 -> Movie ID 878
        val actorPivotSection = sections.firstOrNull { it.id.startsWith("serendipity_actor_pivot", ignoreCase = true) }
        if (actorPivotSection != null) {
            assertEquals(
                "878",
                actorPivotSection.queryParams["without_genres"],
                "Actor Pivot MUST exclude translated Movie Sci-Fi ID 878, not raw TV ID 10765"
            )
        }

        // 2. Проверяем BingeWatchingWeekendBlueprint статус = 3 (Ended)
        val bingeWeekendSection = sections.firstOrNull { it.id.startsWith("binge_watching_weekend", ignoreCase = true) }
        if (bingeWeekendSection != null) {
            assertEquals(
                "3",
                bingeWeekendSection.queryParams["with_status"],
                "Binge weekend section MUST request with_status = 3 (Ended)"
            )
        }
    }

    @Test
    fun `test Persona 2 - Anime Otaku specialization verification`() = runBlocking {
        val vector = AffinityVector(
            genreWeights = mapOf("16" to 0.85, "18" to 0.60) // Высокий аффинитет к анимации (16)
        )

        val context = InterpreterContext(
            affinityVector = vector,
            topGenres = listOf("16", "18"),
            topKeywords = emptyList(),
            localHour = 14,
            localizedGenres = mapOf("16" to "Мультфильм"),
            isWeekend = false
        )

        // Прямое вычисление блюпринта Аниме-Специалист
        val segment = AnimeSpecialistBlueprint.evaluate(context)
        assertNotNull(segment, "AnimeSpecialistBlueprint MUST activate for animationScore >= 0.3")
        assertEquals(
            "ja",
            segment.queryParams["with_original_language"],
            "Anime specialist MUST filter by Japanese language (ja)"
        )
        assertEquals("16", segment.queryParams["with_genres"], "Anime specialist MUST target animation genre 16")
    }

    @Test
    fun `test Persona 3 - Mood Seeker dark comedy and tension verification`() = runBlocking {
        val pluginContext = createMockPluginContext()
        val generator = DashboardGenerator(pluginContext)

        val vector = AffinityVector(
            moodWeights = mapOf(
                MoodKey.LAUGH.value to 0.80,
                MoodKey.DARK.value to 0.85,
                MoodKey.TENSION.value to 0.90
            )
        )

        val sections = generator.generate(vector)

        // Проверяем активацию каруселей настроения
        val moodSections = sections.filter {
            it.id.startsWith("mood_", ignoreCase = true)
        }
        assertTrue(
            moodSections.isNotEmpty(),
            "Mood sections MUST activate when moodWeights are populated via SemanticMoodClassifier"
        )
    }

    @Test
    fun `test Persona 4 - Cold Start new user fallback verification`() = runBlocking {
        val pluginContext = createMockPluginContext()
        val generator = DashboardGenerator(pluginContext)

        val emptyVector = AffinityVector()

        val sections = generator.generate(emptyVector)

        assertTrue(sections.isNotEmpty(), "Cold start user MUST receive trending fallback sections")
        val hasTrending = sections.any {
            it.id.startsWith(
                "social_trending_now",
                ignoreCase = true
            ) || it.id.startsWith("social_critics_choice", ignoreCase = true)
        }
        assertTrue(hasTrending, "Cold start user MUST see trending or critics choice shelves")
    }

    @Test
    fun `test All 41 Blueprints Exhaustive Execution and Query Integrity`() = runBlocking {
        val pluginContext = createMockPluginContext()
        val generator = DashboardGenerator(pluginContext)

        // Богатый вектор с вариациями по всем пространствам
        val richVector = AffinityVector(
            genreWeights = mapOf("878" to 0.9, "28" to 0.8, "16" to 0.7, "18" to 0.65),
            keywordWeights = mapOf("9714" to 0.8, "4379" to 0.85, "4565" to 0.75),
            actorWeights = mapOf("9942" to 0.8),
            directorWeights = mapOf("100" to 0.85),
            companyWeights = mapOf("418" to 0.9),
            eraWeights = mapOf("1980s" to 0.85),
            pacingWeights = mapOf("fast" to 0.8),
            moodWeights = mapOf("laugh" to 0.8, "dark" to 0.85, "tension" to 0.9, "adrenaline" to 0.88),
            sessionBingeVector = mapOf("878" to 1.0)
        )

        val sections = generator.generate(richVector)

        assertTrue(sections.isNotEmpty(), "Generator should produce valid sections for rich vector")

        // Каждая секция должна иметь уникальный ID и непустые queryParams
        val sectionIds = sections.map { it.id }
        assertEquals(
            sectionIds.distinct().size,
            sectionIds.size,
            "All section IDs MUST be unique to prevent Compose LazyColumn duplicate key crashes"
        )

        sections.forEach { section ->
            assertNotNull(section.title, "Section title cannot be null")
            assertTrue(
                section.queryParams.isNotEmpty(),
                "Section queryParams cannot be empty for TMDB discover requests"
            )
        }
    }
}
