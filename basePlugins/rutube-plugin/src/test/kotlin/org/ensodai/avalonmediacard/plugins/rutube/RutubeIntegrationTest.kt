package org.ensodai.avalonmediacard.plugins.rutube

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import org.ensodai.avalonmediacard.contract.plugins.DefaultPluginLogger
import org.ensodai.avalonmediacard.contract.plugins.PluginLogger
import org.ensodai.avalonmediacard.plugins.rutube.data.network.RutubeApiClient
import org.ensodai.avalonmediacard.plugins.rutube.data.network.dto.RutubeVideoDto
import org.ensodai.avalonmediacard.plugins.rutube.data.repository.RutubeRepositoryImpl
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RutubeIntegrationTest {

    private val testLogger = DefaultPluginLogger("Rutube Test")

    @Ignore
    @Test
    fun testDiagnosticSilo() = runBlocking {
        val client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
        }
        val apiClient = RutubeApiClient(client, testLogger)
        val matcher = org.ensodai.avalonmediacard.contract.parsers.EpisodeMatcher()

        println("=== ДИАГНОСТИКА 'Укрытие' (Silo) В RUTUBE ===")
        val queries = listOf("Укрытие 1 сезон", "Укрытие сериал", "Silo season 1")
        for (q in queries) {
            val results = apiClient.searchVideos(query = q, duration = "long", limit = 50)
            println("\n--- Query '$q' -> найдено ${results.size} видео ---")
            val authorGroups = results.groupBy { it.author?.name ?: "Unknown" }
            println("Всего разных авторов в выдаче: ${authorGroups.size}")
            for ((author, vids) in authorGroups.entries.sortedByDescending { it.value.size }.take(5)) {
                val authorId = vids.first().author?.id
                println("  Канал '$author' (ID: $authorId): ${vids.size} видео в выдаче")
                for (v in vids.take(3)) {
                    val m = matcher.parse("Укрытие", v.title)
                    println("     -> '${v.title}' (parsed: $m)")
                }
            }

            // Посмотрим, какие серии 1 сезона собрались суммарно со всех авторов
            val allEpisodesFound = mutableSetOf<Int>()
            for (v in results) {
                val m = matcher.parse("Укрытие", v.title)
                if (m is org.ensodai.avalonmediacard.contract.parsers.MappingResult.Success) {
                    if (1 in m.seasons || m.seasons.isEmpty()) {
                        allEpisodesFound.addAll(m.episodes)
                    }
                }
            }
            println("Суммарно уникальных серий 1 сезона найдено среди ВСЕХ авторов: ${allEpisodesFound.sorted()}")
        }

        println("\n--- Проверяем канал 'Сериал Укрытие | Silo' (ID: 34835052) через Person API ---")
        val channelVids = apiClient.getAuthorVideos(authorId = "34835052", limit = 100, maxPages = 1)
        println("Всего видео на канале 34835052: ${channelVids.size}")
        val s1Eps = mutableSetOf<Int>()
        for (v in channelVids) {
            val m = matcher.parse("Укрытие", v.title)
            if (m is org.ensodai.avalonmediacard.contract.parsers.MappingResult.Success && (1 in m.seasons || m.seasons.isEmpty())) {
                s1Eps.addAll(m.episodes)
            }
            println("  -> '${v.title}'")
        }
        println("Серии 1 сезона на канале 34835052: ${s1Eps.sorted()}")

        client.close()
    }

    @Ignore
    @Test
    fun testSearchSeasonPlusChannelName() = runBlocking {
        val client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
        }
        val apiClient = RutubeApiClient(client, testLogger)
        val matcher = org.ensodai.avalonmediacard.contract.parsers.EpisodeMatcher()

        println("=== ТЕСТ ПОИСКА: СЕЗОН + НАЗВАНИЕ КАНАЛА ===")
        val testCases = listOf(
            "Укрытие 1 сезон Фильмач" to "Укрытие",
            "Укрытие 1 сезон Сериал Укрытие" to "Укрытие",
            "Укрытие 1 сезон Apple TV+" to "Укрытие",
            "Звездный путь глубокий космос 9 1 сезон Дальний космос 9" to "Звездный путь глубокий космос 9",
            "Звездный путь глубокий космос 9 2 сезон Дальний космос 9" to "Звездный путь глубокий космос 9"
        )

        for ((query, titleKey) in testCases) {
            println("\n-----------------------------------------------------------------------")
            println("📡 ЗАПРОС: '$query'")
            val results = apiClient.searchVideos(query = query, duration = "long", limit = 50)
            println("📊 Всего найдено: ${results.size}")

            val authorGroups = results.groupBy { it.author?.name ?: "Unknown" }
            for ((author, vids) in authorGroups) {
                val epNumbers = mutableSetOf<Int>()
                for (v in vids) {
                    val m = matcher.parse(titleKey, v.title)
                    if (m is org.ensodai.avalonmediacard.contract.parsers.MappingResult.Success) {
                        epNumbers.addAll(m.episodes)
                    }
                }
                println("  Канал '$author': ${vids.size} видео в выдаче -> Серии: ${epNumbers.sorted()}")
            }
        }
        client.close()
    }

    @Ignore
    @Test
    fun testSearchTheLostWorld() = runBlocking {
        val client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
        }
        val apiClient = RutubeApiClient(client, testLogger)

        println("=== ПОИСК 'Затерянный мир' В RUTUBE ===")
        val queries = listOf(
            "Затерянный мир",
            "Затерянный мир сериал",
            "Затерянный мир 1 сезон",
            "Затерянный мир 2 сезон",
            "Затерянный мир 3 сезон"
        )
        for (q in queries) {
            val results = apiClient.searchVideos(q)
            println("--- Query: '$q' -> найдено ${results.size} видео ---")
            for ((idx, r) in results.take(15).withIndex()) {
                println("  [$idx] id=${r.id}, duration=${r.duration}s, author='${r.author?.name}' (id=${r.author?.id}), title='${r.title}'")
            }
        }
        client.close()
    }

    @Test
    fun testEpisodeMatcherOnRutubeTitles() {
        val matcher = org.ensodai.avalonmediacard.contract.parsers.EpisodeMatcher()
        val titles = listOf(
            "Сериал Затерянный мир / The Lost World Сезон 1 серия 2. Сели на мель",
            "Сериал Затерянный мир / The Lost World Сезон 2 серия 8. Узник",
            "Сериал Затерянный мир / The Lost World Сезон 3 серия 8. Бесплодная победа"
        )
        for (t in titles) {
            val res = matcher.parse("Затерянный мир", t)
            println("Title: '$t' -> Matcher Result: $res")
        }
    }

    @Ignore
    @Test
    fun testSeasonDiscoveryForLostWorld() = runBlocking {
        val client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
        }
        val apiClient = RutubeApiClient(client, testLogger)
        val repo = RutubeRepositoryImpl(apiClient)
        val matcher = org.ensodai.avalonmediacard.contract.parsers.EpisodeMatcher()

        println("=== ПОИСК И МЭППИНГ СЕЗОНОВ RUTUBE: Затерянный мир ===")
        val mainTitle = "Затерянный мир"
        val totalSeasons = 3
        val queries = (1..totalSeasons).map { "$mainTitle $it сезон" } + listOf("$mainTitle сериал", mainTitle)

        val discoveredVideos = mutableListOf<org.ensodai.avalonmediacard.plugins.rutube.domain.model.RutubeVideoItem>()
        val seenIds = mutableSetOf<String>()

        for (q in queries) {
            val results = repo.searchVideos(q)
            for (v in results) {
                if (seenIds.add(v.id) && v.durationSeconds >= 300.0) {
                    discoveredVideos.add(v)
                }
            }
        }

        println("Всего уникальных видео найдено: ${discoveredVideos.size}")

        data class ParsedEp(val season: Int, val episode: Int, val title: String, val author: String)
        val parsed = mutableListOf<ParsedEp>()

        for (v in discoveredVideos) {
            val match = matcher.parse(mainTitle, v.title)
            if (match is org.ensodai.avalonmediacard.contract.parsers.MappingResult.Success) {
                val s = match.seasons.firstOrNull() ?: continue
                val e = match.episodes.firstOrNull() ?: continue
                parsed.add(ParsedEp(s, e, v.title, v.authorName ?: "Unknown"))
            }
        }

        val grouped = parsed.groupBy { it.season }
        for ((s, eps) in grouped.toSortedMap()) {
            println("=== Сезон $s: найдено ${eps.size} серий (уникальных серий: ${eps.distinctBy { it.episode }.size}) ===")
            for (ep in eps.distinctBy { it.episode }.sortedBy { it.episode }.take(10)) {
                println("  S${ep.season}E${ep.episode} -> '${ep.title}' [${ep.author}]")
            }
        }
        client.close()
    }

    @Test
    fun testEpisodeMatcherEdgeCases() {
        val matcher = org.ensodai.avalonmediacard.contract.parsers.EpisodeMatcher()
        val testCases = listOf(
            "Сериал Затерянный мир / The Lost World Сезон 1 серия 1 Приключения начинаются" to (1 to 1),
            "Затерянный мир Сезон 1серия 3. Больше, чем люди" to (1 to 3),
            "Затерянный мир 1 сезон, 7 серия" to (1 to 7),
            "Затерянный мир 1 сезон 7.серия" to (1 to 7),
            "Затерянный мир 01х05 (Cyrillic x)" to (1 to 5),
            "Затерянный мир 1x05 (Latin x)" to (1 to 5),
            "Затерянный мир (1 сезон) [01-22 из 22]" to (1 to 1), // or 1..22
            "Затерянный мир 2 сезон 1-2 серии" to (2 to 1), // or 1..2
            "Затерянный мир s02e05" to (2 to 5),
            "Затерянный мир S2 E5" to (2 to 5),
            "Затерянный мир - Сезон 1 (Серия 4)" to (1 to 4),
            "Затерянный мир Серия 5 (2 сезон)" to (2 to 5),
            "Сериал Затерянный мир / The Lost World Сезон 1 сериал 18. Неотъемлемое право" to (1 to 18),
            "Сериал Частилище 1 сезон 5 серия" to (1 to 5),
            "Сериал Человек 2 сезон 3 серия" to (2 to 3),
            "Затерянный мир 3 сезон [05/22]" to (3 to 5)
        )

        for ((title, expected) in testCases) {
            val result = matcher.parse("Затерянный мир", title)
            println("CASE: '$title'")
            println("   -> RESULT: $result (Expected S${expected.first}E${expected.second})")
        }
    }

    @Ignore
    @Test
    fun testEndToEndGroupingForLostWorld() = runBlocking {
        val client = HttpClient(CIO)
        val apiClient = RutubeApiClient(client, testLogger)
        val repo = RutubeRepositoryImpl(apiClient)
        val matcher = org.ensodai.avalonmediacard.contract.parsers.EpisodeMatcher()

        val mainTitle = "Затерянный мир"
        val originalTitle = "The Lost World"
        val totalSeasonsCount = 3

        val searchQueries = mutableListOf<String>()
        for (s in 1..totalSeasonsCount) {
            searchQueries.add("Сериал $mainTitle $s сезон")
            searchQueries.add("$mainTitle $s сезон")
        }
        searchQueries.add("Сериал $mainTitle")
        searchQueries.add("$mainTitle сериал")
        searchQueries.add(mainTitle)
        for (s in 1..totalSeasonsCount) {
            searchQueries.add("$originalTitle Season $s")
        }
        searchQueries.add("$originalTitle series")
        searchQueries.add(originalTitle)

        val stopWords = listOf(
            "трейлер", "trailer", "тизер", "teaser", "премьера",
            "нарезка", "отрывок", "клип", "саундтрек", "реакция", "разбор",
            "shorts", "tiktok", "тикток", "факты", "смешные моменты",
            "аудиокнига", "audiobook", "прохождение", "gameplay", "геймплей", "кампания", "collecta"
        )

        val allDiscoveredVideos = mutableListOf<org.ensodai.avalonmediacard.plugins.rutube.domain.model.RutubeVideoItem>()
        val seenIds = mutableSetOf<String>()

        for (query in searchQueries.distinct()) {
            val results = repo.searchVideos(query, duration = null)
            val channelResults = results.filter { it.authorName?.contains("Затерянный мир", ignoreCase = true) == true }
            println("Query '$query' -> Total: ${results.size}, Channel: ${channelResults.size}")
            for (item in results) {
                if (seenIds.add(item.id)) {
                    val titleLower = item.title.lowercase()
                    if (!stopWords.any { titleLower.contains(it) } && item.durationSeconds >= 300.0) {
                        allDiscoveredVideos.add(item)
                    }
                }
            }
        }

        println("Discovered unique videos: ${allDiscoveredVideos.size}")

        val mainClean = mainTitle.lowercase().trim()
        val origClean = (originalTitle ?: "").lowercase().trim()
        val mainWords = mainClean.split(" ")
            .map { it.trim('.', ',', ':', ';', '!', '?', '-', '/') }
            .filter { it.length >= 3 && it !in listOf("сезон", "серия", "сериал", "season", "series", "episode", "фильм", "мир", "the", "world") }

        fun matchesKeywords(video: org.ensodai.avalonmediacard.plugins.rutube.domain.model.RutubeVideoItem): Boolean {
            val titleLower = video.title.lowercase()
            val authorLower = (video.authorName ?: "").lowercase()

            if (mainClean.isNotBlank() && (titleLower.contains(mainClean) || authorLower.contains(mainClean))) return true
            if (origClean.isNotBlank() && (titleLower.contains(origClean) || authorLower.contains(origClean))) return true
            if (mainWords.isNotEmpty()) {
                val matchedCount = mainWords.count { titleLower.contains(it) || authorLower.contains(it) }
                if (matchedCount >= 1) return true
            }
            return false
        }

        data class ParsedEpisode(
            val video: org.ensodai.avalonmediacard.plugins.rutube.domain.model.RutubeVideoItem,
            val season: Int,
            val episode: Int
        )

        val parsedEpisodes = mutableListOf<ParsedEpisode>()
        for (item in allDiscoveredVideos) {
            val kwMatch = matchesKeywords(item)
            val match = matcher.parse(mainTitle, item.title)

            if (item.authorName?.contains("Затерянный мир", ignoreCase = true) == true) {
                println("CHANNEL VID: Title='${item.title}' | kwMatch=$kwMatch | Matcher=$match")
            }

            if (!kwMatch) continue

            when (match) {
                is org.ensodai.avalonmediacard.contract.parsers.MappingResult.Success -> {
                    val s = match.seasons.firstOrNull() ?: 1
                    val e = match.episodes.firstOrNull() ?: continue
                    parsedEpisodes.add(ParsedEpisode(item, s, e))
                }
                is org.ensodai.avalonmediacard.contract.parsers.MappingResult.Partial -> {
                    val e = match.episodes.firstOrNull() ?: continue
                    parsedEpisodes.add(ParsedEpisode(item, 1, e))
                }
                else -> {}
            }
        }

        val groups = parsedEpisodes.groupBy { Pair(it.season, it.video.authorName ?: "Rutube") }
        val maxEpisodesPerSeason = mutableMapOf<Int, Int>()
        for ((groupKey, episodeList) in groups) {
            val s = groupKey.first
            val count = episodeList.distinctBy { it.episode }.size
            maxEpisodesPerSeason[s] = maxOf(maxEpisodesPerSeason[s] ?: 0, count)
        }

        println("=== ИТОГОВЫЕ КАРТОЧКИ СЕЗОНОВ RUTUBE ===")
        for ((groupKey, episodeList) in groups) {
            val seasonNum = groupKey.first
            if (seasonNum > totalSeasonsCount || seasonNum < 1) continue

            val author = groupKey.second
            val distinctEpisodes = episodeList.distinctBy { it.episode }.sortedBy { it.episode }
            val epCount = distinctEpisodes.size

            val maxForSeason = maxEpisodesPerSeason[seasonNum] ?: 0
            if (maxForSeason >= 4 && epCount < (maxForSeason * 0.5).toInt()) {
                continue
            }

            println("  -> $mainTitle • Сезон $seasonNum ($epCount серий) | Канал «$author» [серии: ${distinctEpisodes.map { it.episode }.take(5)}...${distinctEpisodes.last().episode}]")
        }

        client.close()
    }

    private fun createTestContext(catalog: org.ensodai.avalonmediacard.contract.model.MediaCatalog): org.ensodai.avalonmediacard.contract.plugins.PluginContext {
        return org.ensodai.avalonmediacard.contract.plugins.PluginContext(
            pluginDir = ".",
            logger = testLogger,
            httpClient = HttpClient(io.ktor.client.engine.cio.CIO),
            catalog = catalog,
            userMovies = object : org.ensodai.avalonmediacard.contract.plugins.UserMovieProvider {
                override suspend fun getUserMovies(userId: kotlin.uuid.Uuid): List<org.ensodai.avalonmediacard.contract.model.UserMovieItem> = emptyList()
                override fun observeUserMovies(userId: kotlin.uuid.Uuid): kotlinx.coroutines.flow.Flow<List<org.ensodai.avalonmediacard.contract.model.UserMovieItem>> = kotlinx.coroutines.flow.emptyFlow()
                override suspend fun updateUserMovie(item: org.ensodai.avalonmediacard.contract.model.UserMovieItem): Boolean = false
                override suspend fun deleteUserMovie(userId: kotlin.uuid.Uuid, mediaId: String): Boolean = false
                override suspend fun getUserEpisodes(userId: kotlin.uuid.Uuid, mediaId: String): List<org.ensodai.avalonmediacard.contract.model.UserEpisodeItem> = emptyList()
                override suspend fun updateUserEpisode(item: org.ensodai.avalonmediacard.contract.model.UserEpisodeItem): Boolean = false
                override suspend fun notifyUpdate() {}
            },
            userCustomLists = object : org.ensodai.avalonmediacard.contract.plugins.UserCustomListProvider {
                override fun observeUserLists(userId: kotlin.uuid.Uuid): kotlinx.coroutines.flow.Flow<List<org.ensodai.avalonmediacard.contract.plugins.CustomListInfo>> = kotlinx.coroutines.flow.emptyFlow()
                override fun observeListItems(listId: kotlin.uuid.Uuid): kotlinx.coroutines.flow.Flow<List<org.ensodai.avalonmediacard.contract.model.MediaKey>> = kotlinx.coroutines.flow.emptyFlow()
                override suspend fun getCustomListsWithStatus(userId: kotlin.uuid.Uuid, mediaKey: org.ensodai.avalonmediacard.contract.model.MediaKey): List<org.ensodai.avalonmediacard.contract.plugins.CustomListStatus> = emptyList()
                override suspend fun toggleList(userId: kotlin.uuid.Uuid, listId: String, mediaKey: org.ensodai.avalonmediacard.contract.model.MediaKey) {}
                override suspend fun createList(userId: kotlin.uuid.Uuid, listName: String, mediaKey: org.ensodai.avalonmediacard.contract.model.MediaKey) {}
            },
            userEpisodes = object : org.ensodai.avalonmediacard.contract.plugins.UserEpisodeProvider {
                override suspend fun getEpisodesProgress(userId: kotlin.uuid.Uuid, mediaId: String, catalogId: String): List<org.ensodai.avalonmediacard.contract.plugins.UserEpisodeProgress> = emptyList()
                override suspend fun saveEpisodeProgress(userId: kotlin.uuid.Uuid, catalogId: String, mediaId: String, season: Int, episode: Int, progressSeconds: Long, durationSeconds: Long, isWatched: Boolean) {}
            },
            sourceMappings = object : org.ensodai.avalonmediacard.contract.plugins.SourceMappingProvider {
                val list = mutableListOf<org.ensodai.avalonmediacard.contract.plugins.SourceMapping>()
                override suspend fun getMappingsBySourceId(sourceId: String): List<org.ensodai.avalonmediacard.contract.plugins.SourceMapping> =
                    list.filter { it.sourceId == sourceId }
                override suspend fun getMappingsByMediaId(mediaId: String): List<org.ensodai.avalonmediacard.contract.plugins.SourceMapping> =
                    list.filter { it.mediaId == mediaId }
                override suspend fun getMappings(mediaId: String, sourceId: String): List<org.ensodai.avalonmediacard.contract.plugins.SourceMapping> =
                    list.filter { it.mediaId == mediaId && it.sourceId == sourceId }
                override suspend fun saveMapping(mapping: org.ensodai.avalonmediacard.contract.plugins.SourceMapping): org.ensodai.avalonmediacard.contract.plugins.SourceMapping {
                    list.removeIf { it.sourceId == mapping.sourceId && it.itemKey == mapping.itemKey }
                    list.add(mapping)
                    return mapping
                }
                override suspend fun saveMappingsBatch(mappings: List<org.ensodai.avalonmediacard.contract.plugins.SourceMapping>) {
                    mappings.forEach { saveMapping(it) }
                }
                override suspend fun clearMappingsByMediaId(mediaId: String) {
                    list.removeIf { it.mediaId == mediaId }
                }
                override suspend fun clearMappingsBySourceId(sourceId: String) {
                    list.removeIf { it.sourceId == sourceId }
                }
            },
            torrentMappings = object : org.ensodai.avalonmediacard.contract.plugins.TorrentMappingProvider {
                override suspend fun getMappingsByHash(torrentHash: String): List<org.ensodai.avalonmediacard.contract.plugins.TorrentMapping> = emptyList()
                override suspend fun getMappingsByMediaId(mediaId: String): List<org.ensodai.avalonmediacard.contract.plugins.TorrentMapping> = emptyList()
                override suspend fun saveMapping(torrentHash: String, filePath: String, seasons: List<Int>?, episodes: List<Int>?, isAbsolute: Boolean, isManual: Boolean, mediaId: String?, fileIndex: Int?, fileSize: Long?): org.ensodai.avalonmediacard.contract.plugins.TorrentMapping = error("stub")
                override suspend fun clearMappingsByMediaId(mediaId: String) {}
            },
            settings = object : org.ensodai.avalonmediacard.contract.plugins.PluginSettings {
                override suspend fun getString(key: String): String? = null
                override suspend fun setString(key: String, value: String) {}
                override suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean = false
                override suspend fun setBoolean(key: String, value: Boolean) {}
                override fun observeString(key: String, defaultValue: String?): kotlinx.coroutines.flow.Flow<String?> = kotlinx.coroutines.flow.emptyFlow()
                override fun observeBoolean(key: String, defaultValue: Boolean): kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.emptyFlow()
            },
            userMediaBindings = object : org.ensodai.avalonmediacard.contract.plugins.UserMediaBindingProvider {
                override suspend fun getBinding(userId: kotlin.uuid.Uuid, mediaId: String, sourceType: String): String? = null
                override suspend fun saveBinding(userId: kotlin.uuid.Uuid, mediaId: String, sourceType: String, sourceId: String) {}
                override suspend fun deleteBinding(userId: kotlin.uuid.Uuid, mediaId: String, sourceType: String) {}
            },
            recommendations = object : org.ensodai.avalonmediacard.contract.plugins.RecommendationEngineRegistrar {
                override fun registerEngine(engine: org.ensodai.avalonmediacard.contract.plugins.RecommendationEngine) {}
                override fun unregisterEngine() {}
            },
            telemetry = object : org.ensodai.avalonmediacard.contract.plugins.TelemetryProvider {
                override suspend fun getUserEvents(userId: kotlin.uuid.Uuid, limit: Int): List<org.ensodai.avalonmediacard.contract.model.TelemetryEvent> = emptyList()
            },
            affinityStore = object : org.ensodai.avalonmediacard.contract.plugins.AffinityVectorStore {
                override val vectorUpdates: kotlinx.coroutines.flow.Flow<kotlin.uuid.Uuid> = kotlinx.coroutines.flow.emptyFlow()
                override suspend fun getVector(userId: kotlin.uuid.Uuid): org.ensodai.avalonmediacard.contract.model.AffinityVector? = null
                override suspend fun saveVector(userId: kotlin.uuid.Uuid, vector: org.ensodai.avalonmediacard.contract.model.AffinityVector, eventCount: Int) {}
                override suspend fun getPendingUsers(limit: Int): List<kotlin.uuid.Uuid> = emptyList()
                override suspend fun getUserEventCount(userId: kotlin.uuid.Uuid): Int = 0
                override suspend fun getCachedEventCount(userId: kotlin.uuid.Uuid): Int? = null
            },
            genreDictionary = object : org.ensodai.avalonmediacard.contract.plugins.GenreDictionaryProvider {
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

    @Ignore
    @Test
    fun testAdaptiveSearchUseCaseWithGapAnalysis() = runBlocking {
        val client = HttpClient(CIO)
        val apiClient = RutubeApiClient(client, testLogger)
        val repo = RutubeRepositoryImpl(apiClient)

        val mockCatalog = object : org.ensodai.avalonmediacard.contract.model.MediaCatalog {
            override suspend fun getTrending(page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
            override suspend fun getTopRated(page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
            override suspend fun getUpcoming(page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
            override suspend fun getTrendingShows(page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
            override suspend fun getPopularShows(page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
            override suspend fun getTopRatedShows(page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
            override suspend fun getRecommendations(key: org.ensodai.avalonmediacard.contract.model.MediaKey, page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
            override suspend fun getSimilar(key: org.ensodai.avalonmediacard.contract.model.MediaKey, page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
            override suspend fun searchMedia(query: String, page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMultiSearchDto>()
            override suspend fun getMediaDetails(key: org.ensodai.avalonmediacard.contract.model.MediaKey, requireSeasons: Boolean, requireVideos: Boolean, language: String): org.ensodai.avalonmediacard.contract.model.MediaMetadata {
                return org.ensodai.avalonmediacard.contract.model.MediaMetadata(
                    title = "Затерянный мир",
                    originalTitle = "The Lost World",
                    numberOfSeasons = 3,
                    runtime = 45,
                    seasons = listOf(
                        org.ensodai.avalonmediacard.contract.model.SeasonMetadata(id = "s1", seasonNumber = 1, name = "Season 1", episodeCount = 22),
                        org.ensodai.avalonmediacard.contract.model.SeasonMetadata(id = "s2", seasonNumber = 2, name = "Season 2", episodeCount = 22),
                        org.ensodai.avalonmediacard.contract.model.SeasonMetadata(id = "s3", seasonNumber = 3, name = "Season 3", episodeCount = 22)
                    )
                )
            }
            override suspend fun getMediaDetailsBatch(keys: List<org.ensodai.avalonmediacard.contract.model.MediaKey>, requireSeasons: Boolean, requireVideos: Boolean, language: String) = emptyMap<org.ensodai.avalonmediacard.contract.model.MediaKey, org.ensodai.avalonmediacard.contract.model.MediaMetadata>()
            override suspend fun getPersonDetails(key: org.ensodai.avalonmediacard.contract.model.MediaKey, language: String) = error("Not needed")
            override suspend fun getSeasonDetails(key: org.ensodai.avalonmediacard.contract.model.MediaKey, seasonNumber: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.slot.EpisodeItem>()
            override suspend fun discoverMedia(genres: List<Int>, keywords: List<Int>, page: Int, isTv: Boolean, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
            override suspend fun discoverMediaByParams(params: Map<String, String>, targetType: org.ensodai.avalonmediacard.contract.model.EntityType, page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
        }

        val mockContext = createTestContext(mockCatalog)
        val useCase = org.ensodai.avalonmediacard.plugins.rutube.domain.usecase.SearchRutubeStreamsUseCase(mockContext, repo)
        val streams = useCase.execute(
            key = org.ensodai.avalonmediacard.contract.model.MediaKey(
                provider = org.ensodai.avalonmediacard.contract.model.MediaProvider.Tmdb,
                type = org.ensodai.avalonmediacard.contract.model.EntityType.TV,
                id = "100"
            ),
            season = null,
            episode = null,
            userId = null
        )

        println("=== RELEASES EMITTED BY ADAPTIVE USECASE (${streams.size} streams) ===")
        for (st in streams) {
            println("  -> [${st.subFilterLabel}] Title='${st.title}' | Subtitle='${st.episodeName}' | Count=${st.episodesCount}")
        }
        assertTrue(streams.isNotEmpty(), "Adaptive search should emit streams for The Lost World")
        client.close()
    }

    @Ignore
    @Test
    fun testDiagnosticSearchStarTrekDeepSpaceNine() = runBlocking {
        val client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
        }
        val apiClient = RutubeApiClient(client, testLogger)
        val repo = RutubeRepositoryImpl(apiClient)
        val matcher = org.ensodai.avalonmediacard.contract.parsers.EpisodeMatcher()

        val mainTitle = "Звёздный путь: Глубокий космос 9"
        val originalTitle = "Star Trek: Deep Space Nine"

        println("=======================================================================")
        println("🔍 ДИАГНОСТИКА ПОИСКА RUTUBE: '$mainTitle' / '$originalTitle'")
        println("=======================================================================")

        // 1. Тестируем сырые запросы поиска
        val testQueries = listOf(
            "$mainTitle сериал",
            mainTitle,
            originalTitle,
            "Звездный путь глубокий космос 9",
            "Звездный путь глубокий космос 9 1 сезон",
            "Звездный путь глубокий космос 9 2 сезон",
            "Star Trek Deep Space Nine season 1"
        )

        // 2. Тестируем полный цикл SearchRutubeStreamsUseCase
        println("\n=======================================================================")
        println("🚀 ЗАПУСК ПОЛНОГО USECASE С GAP ANALYSIS ДЛЯ STAR TREK DS9")
        println("=======================================================================")

        val mockCatalog = object : org.ensodai.avalonmediacard.contract.model.MediaCatalog {
            override suspend fun getTrending(page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
            override suspend fun getTopRated(page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
            override suspend fun getUpcoming(page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
            override suspend fun getTrendingShows(page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
            override suspend fun getPopularShows(page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
            override suspend fun getTopRatedShows(page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
            override suspend fun getRecommendations(key: org.ensodai.avalonmediacard.contract.model.MediaKey, page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
            override suspend fun getSimilar(key: org.ensodai.avalonmediacard.contract.model.MediaKey, page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
            override suspend fun searchMedia(query: String, page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMultiSearchDto>()
            override suspend fun getMediaDetails(key: org.ensodai.avalonmediacard.contract.model.MediaKey, requireSeasons: Boolean, requireVideos: Boolean, language: String): org.ensodai.avalonmediacard.contract.model.MediaMetadata {
                return org.ensodai.avalonmediacard.contract.model.MediaMetadata(
                    title = mainTitle,
                    originalTitle = originalTitle,
                    numberOfSeasons = 7,
                    runtime = 45,
                    seasons = listOf(
                        org.ensodai.avalonmediacard.contract.model.SeasonMetadata(id = "s1", seasonNumber = 1, name = "Season 1", episodeCount = 20),
                        org.ensodai.avalonmediacard.contract.model.SeasonMetadata(id = "s2", seasonNumber = 2, name = "Season 2", episodeCount = 26),
                        org.ensodai.avalonmediacard.contract.model.SeasonMetadata(id = "s3", seasonNumber = 3, name = "Season 3", episodeCount = 26),
                        org.ensodai.avalonmediacard.contract.model.SeasonMetadata(id = "s4", seasonNumber = 4, name = "Season 4", episodeCount = 26),
                        org.ensodai.avalonmediacard.contract.model.SeasonMetadata(id = "s5", seasonNumber = 5, name = "Season 5", episodeCount = 26),
                        org.ensodai.avalonmediacard.contract.model.SeasonMetadata(id = "s6", seasonNumber = 6, name = "Season 6", episodeCount = 26),
                        org.ensodai.avalonmediacard.contract.model.SeasonMetadata(id = "s7", seasonNumber = 7, name = "Season 7", episodeCount = 26)
                    )
                )
            }
            override suspend fun getMediaDetailsBatch(keys: List<org.ensodai.avalonmediacard.contract.model.MediaKey>, requireSeasons: Boolean, requireVideos: Boolean, language: String) = emptyMap<org.ensodai.avalonmediacard.contract.model.MediaKey, org.ensodai.avalonmediacard.contract.model.MediaMetadata>()
            override suspend fun getPersonDetails(key: org.ensodai.avalonmediacard.contract.model.MediaKey, language: String) = error("Not needed")
            override suspend fun getSeasonDetails(key: org.ensodai.avalonmediacard.contract.model.MediaKey, seasonNumber: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.slot.EpisodeItem>()
            override suspend fun discoverMedia(genres: List<Int>, keywords: List<Int>, page: Int, isTv: Boolean, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
            override suspend fun discoverMediaByParams(params: Map<String, String>, targetType: org.ensodai.avalonmediacard.contract.model.EntityType, page: Int, language: String) = emptyList<org.ensodai.avalonmediacard.contract.model.TmdbMovieDto>()
        }

        val mockContext = createTestContext(mockCatalog)
        val useCase = org.ensodai.avalonmediacard.plugins.rutube.domain.usecase.SearchRutubeStreamsUseCase(mockContext, repo)
        val streams = useCase.execute(
            key = org.ensodai.avalonmediacard.contract.model.MediaKey(
                provider = org.ensodai.avalonmediacard.contract.model.MediaProvider.Tmdb,
                type = org.ensodai.avalonmediacard.contract.model.EntityType.TV,
                id = "580"
            ),
            season = null,
            episode = null,
            userId = null
        )

        println("\n=======================================================================")
        println("✨ ИТОГОВЫЕ КАРТОЧКИ ПОТОКОВ ДЛЯ STAR TREK DS9 (${streams.size} streams):")

        client.close()
    }
}
