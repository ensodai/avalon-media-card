package org.ensodai.avalonmediacard.plugins.lampac

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.classification.AnimeSubType
import org.ensodai.avalonmediacard.contract.model.AffinityVector
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaCatalog
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaMetadata
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.model.PersonMetadata
import org.ensodai.avalonmediacard.contract.model.SeasonMetadata
import org.ensodai.avalonmediacard.contract.model.TelemetryEvent
import org.ensodai.avalonmediacard.contract.model.TmdbMovieDto
import org.ensodai.avalonmediacard.contract.model.TmdbMultiSearchDto
import org.ensodai.avalonmediacard.contract.model.UserEpisodeItem
import org.ensodai.avalonmediacard.contract.model.UserMovieItem
import org.ensodai.avalonmediacard.contract.plugins.AffinityVectorStore
import org.ensodai.avalonmediacard.contract.plugins.CustomListInfo
import org.ensodai.avalonmediacard.contract.plugins.CustomListStatus
import org.ensodai.avalonmediacard.contract.plugins.DefaultPluginLogger
import org.ensodai.avalonmediacard.contract.plugins.GenreDictionaryProvider
import org.ensodai.avalonmediacard.contract.plugins.IntegrationSettingsManager
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.PluginSettings
import org.ensodai.avalonmediacard.contract.plugins.RecommendationEngine
import org.ensodai.avalonmediacard.contract.plugins.RecommendationEngineRegistrar
import org.ensodai.avalonmediacard.contract.plugins.ResolvedIntegrationSetting
import org.ensodai.avalonmediacard.contract.plugins.SourceMapping
import org.ensodai.avalonmediacard.contract.plugins.SourceMappingProvider
import org.ensodai.avalonmediacard.contract.plugins.TelemetryProvider
import org.ensodai.avalonmediacard.contract.plugins.TorrentMapping
import org.ensodai.avalonmediacard.contract.plugins.TorrentMappingProvider
import org.ensodai.avalonmediacard.contract.plugins.UserCustomListProvider
import org.ensodai.avalonmediacard.contract.plugins.UserEpisodeProgress
import org.ensodai.avalonmediacard.contract.plugins.UserEpisodeProvider
import org.ensodai.avalonmediacard.contract.plugins.UserMediaBindingProvider
import org.ensodai.avalonmediacard.contract.plugins.UserMovieProvider
import org.ensodai.avalonmediacard.contract.plugins.UserPluginSettings
import org.ensodai.avalonmediacard.contract.slot.EpisodeItem
import org.ensodai.avalonmediacard.plugins.lampac.data.network.dto.JacRedTorrentDto
import org.ensodai.avalonmediacard.plugins.lampac.data.network.dto.LampacBalancerDto
import org.ensodai.avalonmediacard.plugins.lampac.data.network.dto.LampacResponseDto
import org.ensodai.avalonmediacard.plugins.lampac.domain.model.LampacBalancer
import org.ensodai.avalonmediacard.plugins.lampac.domain.model.LampacEpisode
import org.ensodai.avalonmediacard.plugins.lampac.domain.model.LampacSeason
import org.ensodai.avalonmediacard.plugins.lampac.domain.model.LampacSourceDescriptor
import org.ensodai.avalonmediacard.plugins.lampac.domain.model.LampacStreamInfo
import org.ensodai.avalonmediacard.plugins.lampac.domain.repository.LampacRepository
import org.ensodai.avalonmediacard.plugins.lampac.domain.usecase.GetLampacPlaylistUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LampacIntegrationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testParseEventsResponse() {
        val sampleJson = """
            [
                {"name": "HDRezka", "url": "http://localhost:9118/lite/rezka", "balanser": "rezka"},
                {"name": "Filmix", "url": "http://localhost:9118/lite/filmix", "balanser": "filmix"},
                {"name": "VK Видео", "url": "http://localhost:9118/lite/vkmovie", "balanser": "vkmovie"}
            ]
        """.trimIndent()

        val parsed = json.decodeFromString<List<LampacBalancerDto>>(sampleJson)
        assertEquals(3, parsed.size)
        assertEquals("rezka", parsed[0].balanser)
        assertEquals("Filmix", parsed[1].name)
    }

    @Test
    fun testParsePlayResponse() {
        val sampleJson = """
            {
                "method": "play",
                "title": "Inception (Дубляж BD)",
                "translate": "Дубляж BD",
                "maxquality": "1080p",
                "quality": {
                    "1080p": "http://localhost:9118/proxy/video1080.m3u8",
                    "720p": "http://localhost:9118/proxy/video720.m3u8"
                },
                "subtitles": [
                    {"label": "Full (BD)", "url": "http://localhost:9118/proxy/sub_full.vtt"}
                ],
                "voice": [
                    {"id": "1", "name": "Дубляж BD", "active": true},
                    {"id": "56", "name": "LostFilm", "active": false}
                ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<LampacResponseDto>(sampleJson)
        assertEquals("play", parsed.method)
        assertEquals("1080p", parsed.maxquality)
        assertNotNull(parsed.quality)
        assertEquals(2, parsed.quality.size)
        assertEquals(1, parsed.subtitles?.size)
        assertEquals(2, parsed.voice?.size)
    }

    @Test
    fun testParseSeasonAndEpisodesResponse() {
        val seasonJson = """
            {
                "type": "season",
                "maxquality": "1080p",
                "voice": [
                    {"id": "56", "name": "LostFilm", "active": true},
                    {"id": "89", "name": "Red Head Sound", "active": false}
                ],
                "data": [
                    {"id": "1", "name": "1 сезон", "url": "http://localhost:9118/lite/rezka?id=123&s=1"},
                    {"id": "2", "name": "2 сезон", "url": "http://localhost:9118/lite/rezka?id=123&s=2"}
                ]
            }
        """.trimIndent()

        val seasonParsed = json.decodeFromString<LampacResponseDto>(seasonJson)
        assertEquals("season", seasonParsed.type)
        assertEquals(2, seasonParsed.data?.size)
        assertEquals(1, seasonParsed.data?.first()?.seasonNumber)

        val episodeJson = """
            {
                "type": "episode",
                "voice": [
                    {"id": "56", "name": "LostFilm"}
                ],
                "data": [
                    {
                        "s": 1,
                        "e": 1,
                        "title": "1 серия (LostFilm)",
                        "url": "http://localhost:9118/lite/rezka?id=123&s=1&e=1&t=56"
                    },
                    {
                        "s": 1,
                        "e": 2,
                        "title": "2 серия (LostFilm)",
                        "url": "http://localhost:9118/lite/rezka?id=123&s=1&e=2&t=56"
                    }
                ]
            }
        """.trimIndent()

        val epParsed = json.decodeFromString<LampacResponseDto>(episodeJson)
        assertEquals("episode", epParsed.type)
        assertEquals(2, epParsed.data?.size)
        assertEquals(1, epParsed.data?.first()?.seasonNumber)
        assertEquals(1, epParsed.data?.first()?.episodeNumber)
        assertEquals(2, epParsed.data?.get(1)?.episodeNumber)
    }

    @Test
    fun testNarutoAbsoluteEpisodeMapping() = runTest {
        val tvKey = MediaKey(MediaProvider.Tmdb, EntityType.TV, "31910")
        val fakeMetadata = MediaMetadata(
            title = "Наруто: Ураганные хроники",
            originalTitle = "Naruto Shippuden",
            posterUrl = "http://tmdb.org/poster.jpg",
            animeSubType = AnimeSubType.JAPANESE_ANIME,
            seasons = (1..16).map { sNum ->
                SeasonMetadata(
                    id = "s$sNum",
                    seasonNumber = sNum,
                    name = "Сезон $sNum",
                    episodeCount = if (sNum == 1) 32 else 21 // 32 + 15*21 = 347
                )
            } + listOf(
                SeasonMetadata(id = "s17", seasonNumber = 17, name = "Сезон 17", episodeCount = 28)
            )
        )

        val s17Episodes = listOf(
            EpisodeItem(id = "370", episodeNumber = 23, name = "Скрывающийся во тьме", overview = null, stillUrl = "http://tmdb.org/still370.jpg", airDate = null, voteAverage = null, runtime = 24),
            EpisodeItem(id = "371", episodeNumber = 24, name = "Истинное сердце", overview = null, stillUrl = "http://tmdb.org/still371.jpg", airDate = null, voteAverage = null, runtime = 24)
        )

        val testContext = createTestContext(
            mediaMetadata = fakeMetadata,
            seasonEpisodes = mapOf(17 to s17Episodes)
        )

        val mockRepo = createMockRepo(
            episodes = listOf(
                LampacEpisode(seasonNumber = 1, episodeNumber = 370, name = "370 серия", title = "Наруто: Ураганные хроники (370 серия)", url = "http://localhost:9118/stream370.m3u8"),
                LampacEpisode(seasonNumber = 1, episodeNumber = 371, name = "371 серия", title = "Наруто: Ураганные хроники (371 серия)", url = "http://localhost:9118/stream371.m3u8")
            )
        )

        val useCase = GetLampacPlaylistUseCase(testContext, mockRepo)
        val playlist = useCase.execute(tvKey, "lampac_season_31910_1_aniliberty", null)

        assertEquals(2, playlist.size)
        val first = playlist[0]
        assertEquals(17, first.seasonNumber)
        assertEquals(23, first.episodeNumber)
        assertEquals("Скрывающийся во тьме", first.episodeName)
        assertEquals("http://tmdb.org/still370.jpg", first.episodePosterUrl)
        assertEquals("Наруто: Ураганные хроники • S17E23 «Скрывающийся во тьме»", first.title)

        val second = playlist[1]
        assertEquals(17, second.seasonNumber)
        assertEquals(24, second.episodeNumber)
        assertEquals("Истинное сердце", second.episodeName)
        assertEquals("Наруто: Ураганные хроники • S17E24 «Истинное сердце»", second.title)
    }

    @Test
    fun testWesternShowRegularSeasonMapping() = runTest {
        val tvKey = MediaKey(MediaProvider.Tmdb, EntityType.TV, "1396")
        val fakeMetadata = MediaMetadata(
            title = "Во все тяжкие",
            originalTitle = "Breaking Bad",
            posterUrl = "http://tmdb.org/bb_poster.jpg",
            animeSubType = AnimeSubType.NOT_ANIME,
            seasons = listOf(
                SeasonMetadata(id = "s1", seasonNumber = 1, name = "Сезон 1", episodeCount = 7),
                SeasonMetadata(id = "s2", seasonNumber = 2, name = "Сезон 2", episodeCount = 13)
            )
        )

        val s2Episodes = (1..13).map { epNum ->
            EpisodeItem(id = "bb_s2e$epNum", episodeNumber = epNum, name = "Эпизод $epNum", overview = null, stillUrl = "http://tmdb.org/bb_still_$epNum.jpg", airDate = null, voteAverage = null, runtime = 47)
        }

        val testContext = createTestContext(
            mediaMetadata = fakeMetadata,
            seasonEpisodes = mapOf(2 to s2Episodes)
        )

        val mockRepo = createMockRepo(
            episodes = listOf(
                LampacEpisode(seasonNumber = 2, episodeNumber = 5, name = "5 серия", title = "Во все тяжкие (5 серия)", url = "http://localhost:9118/bb_s2e5.m3u8")
            )
        )

        val useCase = GetLampacPlaylistUseCase(testContext, mockRepo)
        val playlist = useCase.execute(tvKey, "lampac_season_1396_2_rezka", null)

        assertEquals(1, playlist.size)
        val ep = playlist.first()
        assertEquals(2, ep.seasonNumber, "Regular Western show Season 2 should remain Season 2")
        assertEquals(5, ep.episodeNumber, "Episode 5 should remain Episode 5")
        assertEquals("Эпизод 5", ep.episodeName)
        assertEquals("Во все тяжкие • S2E5 «Эпизод 5»", ep.title)
    }

    @Test
    fun testSingleSeasonAnimeDirectMapping() = runTest {
        val tvKey = MediaKey(MediaProvider.Tmdb, EntityType.TV, "99999")
        val fakeMetadata = MediaMetadata(
            title = "Одиночный сезон Аниме",
            originalTitle = "Single Season Anime",
            posterUrl = "http://tmdb.org/single_poster.jpg",
            animeSubType = AnimeSubType.JAPANESE_ANIME,
            seasons = listOf(
                SeasonMetadata(id = "s1", seasonNumber = 1, name = "Сезон 1", episodeCount = 12)
            )
        )

        val s1Episodes = (1..12).map { epNum ->
            EpisodeItem(id = "ep_$epNum", episodeNumber = epNum, name = "Серия $epNum", overview = null, stillUrl = "http://tmdb.org/single_$epNum.jpg", airDate = null, voteAverage = null, runtime = 24)
        }

        val testContext = createTestContext(
            mediaMetadata = fakeMetadata,
            seasonEpisodes = mapOf(1 to s1Episodes)
        )

        val mockRepo = createMockRepo(
            episodes = listOf(
                LampacEpisode(seasonNumber = 1, episodeNumber = 8, name = "8 серия", title = "8 серия", url = "http://localhost:9118/single_8.m3u8")
            )
        )

        val useCase = GetLampacPlaylistUseCase(testContext, mockRepo)
        val playlist = useCase.execute(tvKey, "lampac_season_99999_1_anilibria", null)

        assertEquals(1, playlist.size)
        val ep = playlist.first()
        assertEquals(1, ep.seasonNumber)
        assertEquals(8, ep.episodeNumber)
        assertEquals("Серия 8", ep.episodeName)
    }

    @Test
    fun testBleachMultiSeasonContinuousMapping() = runTest {
        val tvKey = MediaKey(MediaProvider.Tmdb, EntityType.TV, "30984")
        val fakeMetadata = MediaMetadata(
            title = "Блич",
            originalTitle = "Bleach",
            posterUrl = "http://tmdb.org/bleach_poster.jpg",
            animeSubType = AnimeSubType.JAPANESE_ANIME,
            seasons = listOf(
                SeasonMetadata(id = "s1", seasonNumber = 1, name = "Сезон 1", episodeCount = 20),
                SeasonMetadata(id = "s2", seasonNumber = 2, name = "Сезон 2", episodeCount = 21), // 21..41
                SeasonMetadata(id = "s3", seasonNumber = 3, name = "Сезон 3", episodeCount = 22), // 42..63
                SeasonMetadata(id = "s4", seasonNumber = 4, name = "Сезон 4", episodeCount = 28)  // 64..91
            )
        )

        val s4Episodes = listOf(
            EpisodeItem(id = "bleach_64", episodeNumber = 1, name = "Возвращение", overview = null, stillUrl = "http://tmdb.org/bleach_64.jpg", airDate = null, voteAverage = null, runtime = 24)
        )

        val testContext = createTestContext(
            mediaMetadata = fakeMetadata,
            seasonEpisodes = mapOf(4 to s4Episodes)
        )

        val mockRepo = createMockRepo(
            episodes = listOf(
                LampacEpisode(seasonNumber = 1, episodeNumber = 64, name = "64 серия", title = "Блич (64 серия)", url = "http://localhost:9118/bleach_64.m3u8")
            )
        )

        val useCase = GetLampacPlaylistUseCase(testContext, mockRepo)
        val playlist = useCase.execute(tvKey, "lampac_season_30984_1_aniliberty", null)

        assertEquals(1, playlist.size)
        val ep = playlist.first()
        assertEquals(4, ep.seasonNumber, "Episode 64 should map to Season 4")
        assertEquals(1, ep.episodeNumber, "Episode 64 should be Episode 1 of Season 4 (64 - 63 = 1)")
        assertEquals("Возвращение", ep.episodeName)
    }

    private fun createTestContext(
        mediaMetadata: MediaMetadata,
        seasonEpisodes: Map<Int, List<EpisodeItem>> = emptyMap()
    ): PluginContext {
        val mockCatalog = object : MediaCatalog {
            override suspend fun getTrending(page: Int, language: String): List<TmdbMovieDto> = emptyList()
            override suspend fun getTopRated(page: Int, language: String): List<TmdbMovieDto> = emptyList()
            override suspend fun getUpcoming(page: Int, language: String): List<TmdbMovieDto> = emptyList()
            override suspend fun getTrendingShows(page: Int, language: String): List<TmdbMovieDto> = emptyList()
            override suspend fun getPopularShows(page: Int, language: String): List<TmdbMovieDto> = emptyList()
            override suspend fun getTopRatedShows(page: Int, language: String): List<TmdbMovieDto> = emptyList()
            override suspend fun getRecommendations(key: MediaKey, page: Int, language: String): List<TmdbMovieDto> = emptyList()
            override suspend fun getSimilar(key: MediaKey, page: Int, language: String): List<TmdbMovieDto> = emptyList()
            override suspend fun searchMedia(query: String, page: Int, language: String): List<TmdbMultiSearchDto> = emptyList()
            override suspend fun getMediaDetails(key: MediaKey, requireSeasons: Boolean, requireVideos: Boolean, language: String): MediaMetadata = mediaMetadata
            override suspend fun getMediaDetailsBatch(keys: List<MediaKey>, requireSeasons: Boolean, requireVideos: Boolean, language: String): Map<MediaKey, MediaMetadata> = mapOf(MediaKey(MediaProvider.Tmdb, EntityType.TV, "1") to mediaMetadata)
            override suspend fun getPersonDetails(key: MediaKey, language: String): PersonMetadata = error("stub")
            override suspend fun getSeasonDetails(key: MediaKey, seasonNumber: Int, language: String): List<EpisodeItem> = seasonEpisodes[seasonNumber] ?: emptyList()
            override suspend fun discoverMedia(genres: List<Int>, keywords: List<Int>, page: Int, isTv: Boolean, language: String): List<TmdbMovieDto> = emptyList()
            override suspend fun discoverMediaByParams(params: Map<String, String>, targetType: EntityType, page: Int, language: String): List<TmdbMovieDto> = emptyList()
        }

        return PluginContext(
            pluginDir = ".",
            logger = DefaultPluginLogger("LampacTest"),
            httpClient = HttpClient(io.ktor.client.engine.cio.CIO),
            catalog = mockCatalog,
            userMovies = object : UserMovieProvider {
                override suspend fun getUserMovies(userId: kotlin.uuid.Uuid): List<UserMovieItem> = emptyList()
                override fun observeUserMovies(userId: kotlin.uuid.Uuid) = emptyFlow<List<UserMovieItem>>()
                override suspend fun updateUserMovie(item: UserMovieItem): Boolean = false
                override suspend fun deleteUserMovie(userId: kotlin.uuid.Uuid, mediaId: String): Boolean = false
                override suspend fun getUserEpisodes(userId: kotlin.uuid.Uuid, mediaId: String): List<UserEpisodeItem> = emptyList()
                override suspend fun updateUserEpisode(item: UserEpisodeItem): Boolean = false
                override suspend fun notifyUpdate() {}
            },
            userCustomLists = object : UserCustomListProvider {
                override fun observeUserLists(userId: kotlin.uuid.Uuid) = emptyFlow<List<CustomListInfo>>()
                override fun observeListItems(listId: kotlin.uuid.Uuid) = emptyFlow<List<MediaKey>>()
                override suspend fun getCustomListsWithStatus(userId: kotlin.uuid.Uuid, mediaKey: MediaKey) = emptyList<CustomListStatus>()
                override suspend fun toggleList(userId: kotlin.uuid.Uuid, listId: String, mediaKey: MediaKey) {}
                override suspend fun createList(userId: kotlin.uuid.Uuid, listName: String, mediaKey: MediaKey) {}
            },
            userEpisodes = object : UserEpisodeProvider {
                override suspend fun getEpisodesProgress(userId: kotlin.uuid.Uuid, mediaId: String, catalogId: String) = emptyList<UserEpisodeProgress>()
                override suspend fun saveEpisodeProgress(userId: kotlin.uuid.Uuid, catalogId: String, mediaId: String, season: Int, episode: Int, progressSeconds: Long, durationSeconds: Long, isWatched: Boolean) {}
            },
            sourceMappings = object : SourceMappingProvider {
                val list = mutableListOf<SourceMapping>()
                override suspend fun getMappingsBySourceId(sourceId: String) = list.filter { it.sourceId == sourceId }
                override suspend fun getMappingsByMediaId(mediaId: String) = list.filter { it.mediaId == mediaId }
                override suspend fun getMappings(mediaId: String, sourceId: String) = list.filter { it.mediaId == mediaId && it.sourceId == sourceId }
                override suspend fun saveMapping(mapping: SourceMapping): SourceMapping {
                    list.removeIf { it.sourceId == mapping.sourceId && it.itemKey == mapping.itemKey }
                    list.add(mapping)
                    return mapping
                }
                override suspend fun saveMappingsBatch(mappings: List<SourceMapping>) { mappings.forEach { saveMapping(it) } }
                override suspend fun clearMappingsByMediaId(mediaId: String) { list.removeIf { it.mediaId == mediaId } }
                override suspend fun clearMappingsBySourceId(sourceId: String) { list.removeIf { it.sourceId == sourceId } }
            },
            torrentMappings = object : TorrentMappingProvider {
                override suspend fun getMappingsByHash(torrentHash: String) = emptyList<TorrentMapping>()
                override suspend fun getMappingsByMediaId(mediaId: String) = emptyList<TorrentMapping>()
                override suspend fun saveMapping(torrentHash: String, filePath: String, seasons: List<Int>?, episodes: List<Int>?, isAbsolute: Boolean, isManual: Boolean, mediaId: String?, fileIndex: Int?, fileSize: Long?): TorrentMapping = error("stub")
                override suspend fun clearMappingsByMediaId(mediaId: String) {}
            },
            settings = object : PluginSettings {
                override suspend fun getString(key: String): String? = null
                override suspend fun setString(key: String, value: String) {}
                override suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean = false
                override suspend fun setBoolean(key: String, value: Boolean) {}
                override fun observeString(key: String, defaultValue: String?) = emptyFlow<String?>()
                override fun observeBoolean(key: String, defaultValue: Boolean) = emptyFlow<Boolean>()
            },
            userSettings = object : UserPluginSettings {
                override suspend fun getString(userId: kotlin.uuid.Uuid, key: String): String? = null
                override suspend fun setString(userId: kotlin.uuid.Uuid, key: String, value: String) {}
                override suspend fun getBoolean(userId: kotlin.uuid.Uuid, key: String, defaultValue: Boolean): Boolean = false
                override suspend fun setBoolean(userId: kotlin.uuid.Uuid, key: String, value: Boolean) {}
                override fun observeString(userId: kotlin.uuid.Uuid, key: String, defaultValue: String?) = emptyFlow<String?>()
                override fun observeBoolean(userId: kotlin.uuid.Uuid, key: String, defaultValue: Boolean) = emptyFlow<Boolean>()
            },
            integrationManager = object : IntegrationSettingsManager {
                override suspend fun getTmdbToken(userId: kotlin.uuid.Uuid?): ResolvedIntegrationSetting? = null
                override suspend fun getTorrServerHost(userId: kotlin.uuid.Uuid?): ResolvedIntegrationSetting? = null
                override suspend fun getTorrServerAuth(userId: kotlin.uuid.Uuid?): String? = null
            },
            userMediaBindings = object : UserMediaBindingProvider {
                override suspend fun getBinding(userId: kotlin.uuid.Uuid, mediaId: String, sourceType: String): String? = null
                override suspend fun saveBinding(userId: kotlin.uuid.Uuid, mediaId: String, sourceType: String, sourceId: String) {}
                override suspend fun deleteBinding(userId: kotlin.uuid.Uuid, mediaId: String, sourceType: String) {}
            },
            recommendations = object : RecommendationEngineRegistrar {
                override fun registerEngine(engine: RecommendationEngine) {}
                override fun unregisterEngine() {}
            },
            telemetry = object : TelemetryProvider {
                override suspend fun getUserEvents(userId: kotlin.uuid.Uuid, limit: Int): List<TelemetryEvent> = emptyList()
            },
            affinityStore = object : AffinityVectorStore {
                override val vectorUpdates = emptyFlow<kotlin.uuid.Uuid>()
                override suspend fun getVector(userId: kotlin.uuid.Uuid): AffinityVector? = null
                override suspend fun saveVector(userId: kotlin.uuid.Uuid, vector: AffinityVector, eventCount: Int) {}
                override suspend fun getPendingUsers(limit: Int) = emptyList<kotlin.uuid.Uuid>()
                override suspend fun getUserEventCount(userId: kotlin.uuid.Uuid): Int = 0
                override suspend fun getCachedEventCount(userId: kotlin.uuid.Uuid): Int? = null
            },
            genreDictionary = object : GenreDictionaryProvider {
                override suspend fun getLocalizedGenres(language: String): Map<String, String> = emptyMap()
            }
        )
    }

    private fun createMockRepo(
        episodes: List<LampacEpisode> = emptyList(),
        movieStreams: List<LampacStreamInfo> = emptyList()
    ): LampacRepository {
        return object : LampacRepository {
            override suspend fun isGatewayAvailable(): Boolean = true
            override suspend fun getAvailableBalancers(title: String, originalTitle: String?, year: Int?, tmdbId: Long?, imdbId: String?, kinopoiskId: Long?, isSerial: Boolean, isAnime: Boolean, originalLanguage: String?): List<LampacBalancer> = emptyList()
            override suspend fun getMovieStreams(balancer: String, title: String, originalTitle: String?, year: Int?, tmdbId: Long?, imdbId: String?, kinopoiskId: Long?): List<LampacStreamInfo> = movieStreams
            override suspend fun getSeasons(balancer: String, title: String, originalTitle: String?, year: Int?, tmdbId: Long?, imdbId: String?, kinopoiskId: Long?): List<LampacSeason> = emptyList()
            override suspend fun getEpisodes(balancer: String, title: String, season: Int, originalTitle: String?, year: Int?, tmdbId: Long?, imdbId: String?, kinopoiskId: Long?, translationId: String?): List<LampacEpisode> = episodes
            override suspend fun resolveStream(descriptor: LampacSourceDescriptor): LampacStreamInfo? = movieStreams.firstOrNull()
            override suspend fun searchTorrents(title: String, year: Int?): List<JacRedTorrentDto> = emptyList()
        }
    }
}
