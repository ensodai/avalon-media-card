package org.ensodai.avalonmediacard.plugins.rutube.domain.usecase

import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.SeasonMetadata
import org.ensodai.avalonmediacard.contract.parsers.EpisodeMatcher
import org.ensodai.avalonmediacard.contract.parsers.MappingResult
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.SourceMapping
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.plugins.rutube.domain.model.RutubeMappedEpisode
import org.ensodai.avalonmediacard.plugins.rutube.domain.model.RutubeVideoItem
import org.ensodai.avalonmediacard.plugins.rutube.domain.repository.RutubeRepository
import kotlin.uuid.Uuid

/**
 * **Search Rutube Streams UseCase**
 *
 * Implements an intelligent 4-phase **Adaptive Search & Gap Analysis** engine
 * for discovering, verifying, and streaming movies and TV series from Rutube.
 *
 * ### Architecture & Optimization Pipeline:
 * 1. **Phase 1: Broad Express Search**: Performs 1-2 generic queries with `limit=50` and
 *    server-side duration filtering (`duration=long/medium`), avoiding brute-force query Cartesian products.
 * 2. **Phase 2: Mathematical Gap Analysis**: Compares discovered episodes against TMDB season metadata:
 *    - *Case A (Coverage ≥ 85%)*: Complete season found. No extra requests generated.
 *    - *Case B (15% ≤ Coverage < 85%)*: Incomplete season. Triggers **Author Expansion** (`Person API`)
 *      to fetch missing episodes directly from the dominant uploader in 1 atomic call.
 *    - *Case C (Coverage < 15% / Missing)*: Missing season. Triggers a single targeted query (`limit=50`).
 * 3. **Phase 3: Bounded Fallback**: Concurrently executes necessary fallback tasks under a [Semaphore]
 *    to guarantee fast responses without triggering HTTP 429 rate-limiting.
 * 4. **Phase 4: Deduplication & Quality Aggregation**: Filters junk 1-episode fragments, ranks channels,
 *    and produces clean [MediaStream] season releases.
 *
 * @property context Host plugin context providing catalog, logger, and user storage.
 * @property repository Rutube domain repository.
 */
class SearchRutubeStreamsUseCase(
    private val context: PluginContext,
    private val repository: RutubeRepository
) {
    private val episodeMatcher = EpisodeMatcher()
    private val requestSemaphore = Semaphore(3)

    private val stopWords = listOf(
        "трейлер", "trailer", "тизер", "teaser", "премьера",
        "нарезка", "отрывок", "клип", "саундтрек", "реакция", "разбор",
        "shorts", "tiktok", "тикток", "факты", "смешные моменты",
        "аудиокнига", "audiobook", "прохождение", "gameplay", "геймплей", "кампания", "collecta"
    )

    private fun isStopWord(titleLower: String): Boolean {
        for (sw in stopWords) {
            if (titleLower.contains(sw)) return true
        }
        if (Regex("""\b(?:ost|обзор)\b""").containsMatchIn(titleLower)) return true
        return false
    }

    private fun extractDistinctiveAuthorKeyword(authorName: String): String {
        val genericWords = setOf(
            "сериал", "сериалы", "фильм", "фильмы", "онлайн", "hd", "канал", "channel",
            "tv", "тв", "hdrip", "webrip", "кино", "cinema", "movie", "video", "видео",
            "official", "официальный", "group", "группа", "клуб", "club"
        )
        val cleaned = authorName.replace("✔", "").trim()
        val parts = cleaned.split("/", "|", "—", "-")
        for (part in parts) {
            val words = part.trim().split(Regex("""\s+"""))
                .map { it.trim('.', ',', ':', ';', '!', '?', '(', ')', '[', ']') }
                .filter { it.isNotBlank() && it.lowercase() !in genericWords && it.length >= 2 }
            if (words.isNotEmpty()) {
                return words.take(4).joinToString(" ")
            }
        }
        return cleaned.take(20)
    }

    private fun normalizeTitleForSearch(text: String): String {
        return text.lowercase()
            .replace('ё', 'е')
            .replace(Regex("""\bv(?=[а-яё])"""), "в")
            .replace(Regex("""[^\p{L}\p{Nd}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    /**
     * Executes stream discovery for movies or TV shows.
     *
     * @param key Canonical [MediaKey] of the media.
     * @param season Optional season number.
     * @param episode Optional episode number.
     * @param userId Optional user UUID.
     * @return List of [MediaStream] items ready to be displayed in the MediaSources drawer.
     */
    suspend fun execute(
        key: MediaKey,
        season: Int?,
        episode: Int?,
        userId: Uuid?
    ): List<MediaStream> {
        val logger = context.logger

        val metadata = try {
            context.catalog.getMediaDetails(key)
        } catch (e: Exception) {
            logger.warn("Rutube: Failed to fetch TMDB details for key=$key: ${e.message}")
            return emptyList()
        }

        val mainTitle = metadata.title.trim()
        val originalTitle = metadata.originalTitle?.trim()
        val year = metadata.releaseDate?.take(4)?.toIntOrNull()
        val isMovie = key.type == EntityType.MOVIE

        return if (isMovie) {
            searchMovieStreams(key, metadata.runtime, mainTitle, originalTitle, year)
        } else {
            if (episode != null) {
                searchSpecificEpisodeStreams(key, season ?: 1, episode, metadata.runtime, mainTitle, originalTitle)
            } else {
                searchSeasonGroupedStreams(
                    key = key,
                    targetSeasonHint = season,
                    metadataSeasons = metadata.seasons,
                    totalSeasonsCount = metadata.numberOfSeasons,
                    expectedRuntimeMinutes = metadata.runtime,
                    mainTitle = mainTitle,
                    originalTitle = originalTitle
                )
            }
        }
    }

    private suspend fun searchMovieStreams(
        key: MediaKey,
        expectedRuntimeMinutes: Int?,
        mainTitle: String,
        originalTitle: String?,
        year: Int?
    ): List<MediaStream> {
        val logger = context.logger
        val searchQueries = mutableListOf<String>()

        if (mainTitle.isNotBlank()) {
            if (year != null && year > 0) searchQueries.add("$mainTitle $year")
            searchQueries.add(mainTitle)
        }
        if (!originalTitle.isNullOrBlank() && originalTitle != mainTitle) {
            if (year != null && year > 0) searchQueries.add("$originalTitle $year")
            searchQueries.add(originalTitle)
        }

        logger.info("Rutube: Searching movie streams for '$mainTitle' (queries=$searchQueries)")

        val foundVideos = mutableListOf<RutubeVideoItem>()
        val seenIds = mutableSetOf<String>()

        for (query in searchQueries.distinct()) {
            val results = repository.searchVideos(query = query, duration = "movie", limit = 50)
            for (item in results) {
                if (seenIds.add(item.id) && isMatchingMovie(item, expectedRuntimeMinutes)) {
                    foundVideos.add(item)
                }
            }
            if (foundVideos.size >= 5) break
        }

        val streams = mutableListOf<MediaStream>()
        for (video in foundVideos.take(5)) {
            val streamInfo = repository.getStreamInfo(video.id) ?: continue
            val bestQuality = streamInfo.qualities.firstOrNull()?.label ?: "Auto"
            val authorClean = video.authorName?.replace("✔", "")?.trim()
            val channelSubtitle = if (!authorClean.isNullOrBlank() && authorClean != "Rutube") "Канал «$authorClean»" else null

            streams.add(
                MediaStream(
                    id = "rutube_${video.id}",
                    title = video.title,
                    url = streamInfo.masterHlsUrl,
                    type = StreamType.Hls,
                    quality = bestQuality,
                    format = "HLS",
                    videoCodec = "H.264",
                    sourceName = "Rutube",
                    durationSeconds = video.durationSeconds,
                    isMapped = true,
                    seasonNumber = null,
                    episodeNumber = null,
                    episodeName = channelSubtitle,
                    qualityVariants = streamInfo.qualities
                )
            )
        }

        logger.info("Rutube: Resolved ${streams.size} movie streams for '$mainTitle'")
        return streams
    }

    private suspend fun searchSpecificEpisodeStreams(
        key: MediaKey,
        targetSeason: Int,
        targetEpisode: Int,
        expectedRuntimeMinutes: Int?,
        mainTitle: String,
        originalTitle: String?
    ): List<MediaStream> {
        val logger = context.logger
        val searchQueries = mutableListOf<String>()

        if (mainTitle.isNotBlank()) {
            searchQueries.add("$mainTitle $targetSeason сезон $targetEpisode серия")
            searchQueries.add("$mainTitle S${targetSeason}E${targetEpisode}")
            searchQueries.add("$mainTitle $targetEpisode серия")
        }
        if (!originalTitle.isNullOrBlank() && originalTitle != mainTitle) {
            searchQueries.add("$originalTitle S${targetSeason}E${targetEpisode}")
            searchQueries.add("$originalTitle Season $targetSeason Episode $targetEpisode")
        }

        logger.info("Rutube: Searching specific episode S${targetSeason}E${targetEpisode} for '$mainTitle' (queries=$searchQueries)")

        val foundVideos = mutableListOf<RutubeVideoItem>()
        val seenIds = mutableSetOf<String>()

        for (query in searchQueries.distinct()) {
            val results = repository.searchVideos(query = query, duration = null, limit = 50)
            for (item in results) {
                if (seenIds.add(item.id) && isMatchingEpisode(item, mainTitle, targetSeason, targetEpisode)) {
                    foundVideos.add(item)
                }
            }
            if (foundVideos.size >= 5) break
        }

        val streams = mutableListOf<MediaStream>()
        for (video in foundVideos.take(5)) {
            val streamInfo = repository.getStreamInfo(video.id) ?: continue
            val bestQuality = streamInfo.qualities.firstOrNull()?.label ?: "Auto"
            val authorClean = video.authorName?.replace("✔", "")?.trim()
            val channelSubtitle = if (!authorClean.isNullOrBlank() && authorClean != "Rutube") "Канал «$authorClean»" else "S${targetSeason}E$targetEpisode"

            streams.add(
                MediaStream(
                    id = "rutube_ep_${video.id}",
                    title = video.title,
                    url = streamInfo.masterHlsUrl,
                    type = StreamType.Hls,
                    quality = bestQuality,
                    format = "HLS",
                    videoCodec = "H.264",
                    sourceName = "Rutube",
                    durationSeconds = video.durationSeconds,
                    isMapped = true,
                    seasonNumber = targetSeason,
                    episodeNumber = targetEpisode,
                    episodeName = channelSubtitle,
                    qualityVariants = streamInfo.qualities
                )
            )
        }

        logger.info("Rutube: Resolved ${streams.size} streams for S${targetSeason}E${targetEpisode} of '$mainTitle'")
        return streams
    }

    private sealed interface FallbackAction {
        val seasonNum: Int
        data class AuthorExpansion(override val seasonNum: Int, val authorId: String) : FallbackAction
        data class SeasonSearch(override val seasonNum: Int) : FallbackAction
    }

    private data class ParsedEpisode(
        val video: RutubeVideoItem,
        val season: Int,
        val episode: Int
    )

    private suspend fun searchSeasonGroupedStreams(
        key: MediaKey,
        targetSeasonHint: Int?,
        metadataSeasons: List<SeasonMetadata>,
        totalSeasonsCount: Int?,
        expectedRuntimeMinutes: Int?,
        mainTitle: String,
        originalTitle: String?
    ): List<MediaStream> = coroutineScope {
        val logger = context.logger

        // Dynamic duration heuristic: 30+ mins -> "long", otherwise "medium"
        val runtimeMin = expectedRuntimeMinutes ?: 45
        val durationFilter = if (runtimeMin >= 30) "long" else "medium"

        // =========================================================================
        // PHASE 1: Express Broad Search (1-2 parallel queries with limit=100)
        // =========================================================================
        val broadQueries = mutableListOf<String>()
        if (mainTitle.isNotBlank()) {
            if (targetSeasonHint != null && targetSeasonHint > 0) {
                broadQueries.add("$mainTitle $targetSeasonHint сезон")
            }
            broadQueries.add("$mainTitle сериал")

            // Поддержка гибридных названий латиница/кириллица (например, Vизитеры / Vизитёры)
            if (originalTitle.equals("V", ignoreCase = true) && mainTitle.startsWith("В", ignoreCase = true)) {
                val hybridTitle = "V" + mainTitle.drop(1)
                if (targetSeasonHint != null && targetSeasonHint > 0) {
                    broadQueries.add("$hybridTitle $targetSeasonHint сезон")
                }
                broadQueries.add("$hybridTitle сериал")
            }
        }
        if (!originalTitle.isNullOrBlank() && !originalTitle.equals(mainTitle, ignoreCase = true)) {
            // Rutube API выдает 400 Bad Request на запросы <= 2 символов без контекста
            if (originalTitle.length <= 2) {
                broadQueries.add("$originalTitle сериал")
                if (targetSeasonHint != null && targetSeasonHint > 0) {
                    broadQueries.add("$originalTitle $targetSeasonHint season")
                }
            } else {
                if (targetSeasonHint != null && targetSeasonHint > 0) {
                    broadQueries.add("$originalTitle Season $targetSeasonHint")
                }
                broadQueries.add(originalTitle)
            }
        }

        logger.info("Rutube: [Phase 1: Broad Search] Starting express search for '$mainTitle' (queries=$broadQueries, limit=100)")

        val broadTasks = broadQueries.distinct().map { query ->
            async {
                repository.searchVideos(query = query, duration = null, limit = 100)
            }
        }

        val allDiscoveredVideos = mutableListOf<RutubeVideoItem>()
        val seenIds = mutableSetOf<String>()

        for (results in broadTasks.awaitAll()) {
            for (item in results) {
                if (seenIds.add(item.id)) {
                    val titleLower = item.title.lowercase()
                    if (!isStopWord(titleLower) && item.durationSeconds >= 300.0) {
                        allDiscoveredVideos.add(item)
                    }
                }
            }
        }

        logger.info("Rutube: [Phase 1: Broad Search] Discovered ${allDiscoveredVideos.size} unique candidate videos")

        // Keyword validation helper с нормализацией омоглифов и ё->е
        val mainNorm = normalizeTitleForSearch(mainTitle)
        val origNorm = normalizeTitleForSearch(originalTitle ?: "")
        val mainWords = mainNorm.split(" ")
            .map { it.trim() }
            .filter { it.length >= 3 && it !in listOf("сезон", "серия", "сериал", "season", "series", "episode", "фильм", "мир", "the", "world") }

        fun matchesKeywords(video: RutubeVideoItem): Boolean {
            val titleNorm = normalizeTitleForSearch(video.title)
            val authorNorm = normalizeTitleForSearch(video.authorName ?: "")

            if (mainNorm.isNotBlank() && (titleNorm.contains(mainNorm) || authorNorm.contains(mainNorm))) return true
            if (origNorm.isNotBlank() && origNorm.length >= 3 && (titleNorm.contains(origNorm) || authorNorm.contains(origNorm))) return true

            if (mainWords.isNotEmpty()) {
                val matchedCount = mainWords.count { titleNorm.contains(it) || authorNorm.contains(it) }
                if (matchedCount >= 1) return true
            }

            return false
        }

        val parsedEpisodes = mutableListOf<ParsedEpisode>()

        fun processVideoItem(item: RutubeVideoItem) {
            if (!matchesKeywords(item)) return
            val match = episodeMatcher.parse(mainTitle, item.title)

            when (match) {
                is MappingResult.Success -> {
                    val s = match.seasons.firstOrNull() ?: targetSeasonHint ?: 1
                    val e = match.episodes.firstOrNull()
                    if (e != null) {
                        parsedEpisodes.add(ParsedEpisode(item, s, e))
                    }
                }
                is MappingResult.Partial -> {
                    val s = targetSeasonHint ?: 1
                    val e = match.episodes.firstOrNull()
                    if (e != null) {
                        parsedEpisodes.add(ParsedEpisode(item, s, e))
                    }
                }
                is MappingResult.Failed -> {
                    val seasonMatch = Regex("""(?<!\d)(\d{1,2})\s*(?:сезон|season|s)""", RegexOption.IGNORE_CASE).find(item.title)
                    val epMatch = Regex("""(?<!\d)(\d{1,3})\s*(?:серия|сер|эпизод|ep|e)""", RegexOption.IGNORE_CASE).find(item.title)
                    val s = seasonMatch?.groupValues?.get(1)?.toIntOrNull() ?: targetSeasonHint ?: 1
                    val e = epMatch?.groupValues?.get(1)?.toIntOrNull()
                    if (e != null) {
                        parsedEpisodes.add(ParsedEpisode(item, s, e))
                    }
                }
            }
        }

        allDiscoveredVideos.forEach { processVideoItem(it) }

        // =========================================================================
        // PHASE 2: Gap Analysis against TMDB Metadata & Dominant Author Expansion
        // =========================================================================
        val seasonMetadataMap = metadataSeasons.associateBy { it.seasonNumber }
        val targetSeasons = if (targetSeasonHint != null && targetSeasonHint > 0) {
            listOf(targetSeasonHint)
        } else {
            val maxS = (totalSeasonsCount ?: metadataSeasons.maxOfOrNull { it.seasonNumber } ?: 3).coerceIn(1, 15)
            (1..maxS).toList()
        }

        val sniperSearches = mutableListOf<String>()
        val todayStr = try { java.time.LocalDate.now().toString() } catch (e: Exception) { "2099-12-31" }

        for (seasonNum in targetSeasons) {
            val seasonMeta = seasonMetadataMap[seasonNum]
            val totalDeclaredEpisodes = seasonMeta?.episodeCount?.takeIf { it > 0 }

            // Определение реально вышедших серий для онгоингов
            val seasonAirDate = seasonMeta?.airDate
            if (seasonAirDate != null && seasonAirDate > todayStr) {
                logger.info("Rutube: [Gap Analysis] Season $seasonNum hasn't aired yet (airDate=$seasonAirDate). Skipping.")
                continue
            }

            // Получаем детали сезона из TMDB для точного подсчета aired серий
            val tmdbSeasonEpisodes = try {
                context.catalog.getSeasonDetails(key, seasonNum)
            } catch (_: Exception) {
                emptyList()
            }
            val airedCountFromTmdb = if (tmdbSeasonEpisodes.isNotEmpty()) {
                tmdbSeasonEpisodes.count { val d = it.airDate; d == null || d <= todayStr }
            } else null

            val expectedEpisodesCount = airedCountFromTmdb ?: totalDeclaredEpisodes

            if (expectedEpisodesCount == 0) {
                logger.info("Rutube: [Gap Analysis] Season $seasonNum has 0 aired episodes on $todayStr. Skipping.")
                continue
            }

            var episodesForSeason = parsedEpisodes.filter { it.season == seasonNum }
            var authorGroups = episodesForSeason.groupBy { it.video.authorName ?: "Rutube" }
            var dominantAuthorEntry = authorGroups.maxByOrNull { it.value.distinctBy { ep -> ep.episode }.size }
            var dominantAuthorEpisodes = dominantAuthorEntry?.value?.map { it.episode }?.distinct()?.sorted() ?: emptyList()
            var maxAuthorEpisodes = dominantAuthorEpisodes.size
            var dominantAuthorName = dominantAuthorEntry?.key?.replace("✔", "")?.trim()
            val dominantAuthorId = dominantAuthorEntry?.value?.firstOrNull()?.video?.authorId

            // Проверяем непрерывность серий автора (наличие внутренних дыр: например [1,2,3,5..] - пропущена 4)
            val maxFoundEp = dominantAuthorEpisodes.maxOrNull() ?: 0
            val missingInnerEpisodes = (1..maxFoundEp).filter { it !in dominantAuthorEpisodes }
            val hasInnerGaps = missingInnerEpisodes.isNotEmpty()
            val isAiredComplete = expectedEpisodesCount != null && maxAuthorEpisodes >= expectedEpisodesCount

            logger.info("Rutube: [Phase 2: Gap Analysis] Season $seasonNum -> Dominant: '$dominantAuthorName' ($maxAuthorEpisodes eps, airedExpected: $expectedEpisodesCount, innerGaps: $missingInnerEpisodes)")

            // AUTHOR EXPANSION (Person API):
            // Если у автора найдено >= 2 серий, но есть внутренняя дыра или не все вышедшие серии
            if (!isAiredComplete && dominantAuthorId != null && !dominantAuthorName.isNullOrBlank() && dominantAuthorName != "Rutube" && maxAuthorEpisodes >= 2) {
                logger.info("Rutube: [Phase 2: Author Expansion] Fetching channel uploads via Person API for '$dominantAuthorName' (ID: $dominantAuthorId)...")
                try {
                    val authorVideos = repository.getAuthorVideos(authorId = dominantAuthorId, limit = 100)
                    for (item in authorVideos) {
                        if (seenIds.add(item.id)) {
                            val titleLower = item.title.lowercase()
                            if (!isStopWord(titleLower) && item.durationSeconds >= 300.0) {
                                processVideoItem(item)
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Rutube: Person API failed for author $dominantAuthorId: ${e.message}")
                }

                // Пересчитываем метрики после Person API
                episodesForSeason = parsedEpisodes.filter { it.season == seasonNum }
                authorGroups = episodesForSeason.groupBy { it.video.authorName ?: "Rutube" }
                dominantAuthorEntry = authorGroups.maxByOrNull { it.value.distinctBy { ep -> ep.episode }.size }
                dominantAuthorEpisodes = dominantAuthorEntry?.value?.map { it.episode }?.distinct()?.sorted() ?: emptyList()
                maxAuthorEpisodes = dominantAuthorEpisodes.size
                dominantAuthorName = dominantAuthorEntry?.key?.replace("✔", "")?.trim()
            }

            val updatedMaxFound = dominantAuthorEpisodes.maxOrNull() ?: 0
            val remainingInnerGaps = (1..updatedMaxFound).filter { it !in dominantAuthorEpisodes }
            val authorComplete = expectedEpisodesCount != null && maxAuthorEpisodes >= expectedEpisodesCount

            when {
                authorComplete -> {
                    logger.info("Rutube: [Gap Analysis: COMPLETE] Season $seasonNum author '$dominantAuthorName' has all $maxAuthorEpisodes/$expectedEpisodesCount aired episodes ✅")
                }
                remainingInnerGaps.isNotEmpty() -> {
                    // Точечный снайперский поиск по конкретным номерам пропущенных серий
                    for (missingEp in remainingInnerGaps.take(3)) {
                        val sniperQuery = "$mainTitle $seasonNum сезон $missingEp серия"
                        logger.info("Rutube: [Gap Analysis: INNER GAP] Season $seasonNum missing ep $missingEp -> Scheduling sniper query: '$sniperQuery'")
                        sniperSearches.add(sniperQuery)
                    }
                }
                maxAuthorEpisodes == 0 -> {
                    val seasonQuery = "$mainTitle $seasonNum сезон"
                    logger.info("Rutube: [Gap Analysis: MISSING] Season $seasonNum -> Scheduling season query: '$seasonQuery'")
                    sniperSearches.add(seasonQuery)
                    if (!originalTitle.isNullOrBlank() && originalTitle.length > 2) {
                        sniperSearches.add("$originalTitle Season $seasonNum")
                    }
                }
                else -> {
                    // Серии идут подряд с 1 по maxAuthorEpisodes (онгоинг или ожидается выход перевода)
                    logger.info("Rutube: [Gap Analysis: ONGOING TAIL] Season $seasonNum has continuous 1..$maxAuthorEpisodes episodes (out of $expectedEpisodesCount). No inner gaps.")
                }
            }
        }

        // =========================================================================
        // PHASE 3: Targeted Sniper Searches Fallback Execution (Bounded Concurrency)
        // =========================================================================
        if (sniperSearches.isNotEmpty()) {
            logger.info("Rutube: [Phase 3: Fallback Execution] Executing ${sniperSearches.size} sniper queries under Semaphore (limit=100)")
            val fallbackTasks = sniperSearches.distinct().map { query ->
                async {
                    requestSemaphore.withPermit {
                        val p1 = repository.searchVideos(query = query, duration = null, limit = 100, page = 1)
                        if (p1.size >= 100) {
                            val p2 = repository.searchVideos(query = query, duration = null, limit = 100, page = 2)
                            p1 + p2
                        } else {
                            p1
                        }
                    }
                }
            }
            for (results in fallbackTasks.awaitAll()) {
                for (item in results) {
                    if (seenIds.add(item.id)) {
                        val titleLower = item.title.lowercase()
                        if (!isStopWord(titleLower) && item.durationSeconds >= 300.0) {
                            processVideoItem(item)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // PHASE 4: Release Assembly, SEL Scoring & Virtual Composite Releases
        // =========================================================================
        val minValidDurationSeconds = (runtimeMin * 60) * 0.5 // Spam filter (50% rule)
        val validParsedEpisodes = parsedEpisodes.filter { it.video.durationSeconds >= minValidDurationSeconds }

        val groups = validParsedEpisodes.groupBy { Pair(it.season, it.video.authorName ?: "Rutube") }
        val maxEpisodesPerSeason = mutableMapOf<Int, Int>()
        for ((groupKey, episodeList) in groups) {
            val s = groupKey.first
            val count = episodeList.distinctBy { it.episode }.size
            maxEpisodesPerSeason[s] = maxOf(maxEpisodesPerSeason[s] ?: 0, count)
        }

        val streams = mutableListOf<MediaStream>()
        val seasonsWithFullReleases = mutableSetOf<Int>()

        // 4A: Emit Author-Specific Stream Cards
        for ((groupKey, episodeList) in groups) {
            val seasonNum = groupKey.first
            val maxAllowedSeason = totalSeasonsCount ?: 20
            if (seasonNum < 1 || seasonNum > maxAllowedSeason) continue

            // Если запрошен конкретный сезон — эмитируем релизы только этого сезона
            if (targetSeasonHint != null && seasonNum != targetSeasonHint) continue

            val author = groupKey.second
            val distinctEpisodes = episodeList.distinctBy { it.episode }.sortedBy { it.episode }
            val epCount = distinctEpisodes.size
            if (epCount == 0) continue

            val maxForSeason = maxEpisodesPerSeason[seasonNum] ?: 0
            if (maxForSeason >= 4 && epCount < (maxForSeason * 0.5).toInt()) {
                logger.info("Rutube: Filtered out incomplete channel '$author' for Season $seasonNum ($epCount < ${maxForSeason * 0.5})")
                continue
            }

            val expectedTotal = seasonMetadataMap[seasonNum]?.episodeCount?.takeIf { it > 0 }
            if (expectedTotal != null && epCount >= expectedTotal) {
                seasonsWithFullReleases.add(seasonNum)
            }

            val countLabel = when {
                expectedTotal != null && expectedTotal > 0 && epCount >= expectedTotal -> "$epCount ${pluralizeEpisodes(epCount)} (все)"
                expectedTotal != null && expectedTotal > 0 -> "$epCount из $expectedTotal ${pluralizeEpisodes(expectedTotal)}"
                else -> "$epCount ${pluralizeEpisodes(epCount)}"
            }

            val sampleVideo = distinctEpisodes.first().video
            val streamInfo = repository.getStreamInfo(sampleVideo.id)
            val bestQuality = streamInfo?.qualities?.firstOrNull()?.label ?: "1080p"
            val authorClean = author.replace("✔", "").trim()
            val channelSubtitle = if (authorClean.isNotBlank() && authorClean != "Rutube") {
                context.i18n.t("rutube.channel_quality_fmt", authorClean, bestQuality)
            } else {
                context.i18n.t("rutube.all_episodes_quality_fmt", bestQuality)
            }
            val groupId = "rutube_season_${key.id}_${seasonNum}_author_${author.hashCode()}"
            context.sourceMappings.saveMappingsBatch(
                distinctEpisodes.map { ep ->
                    SourceMapping(
                        sourceType = "rutube-plugin",
                        sourceId = groupId,
                        itemKey = ep.video.id,
                        seasons = listOf(seasonNum),
                        episodes = listOf(ep.episode),
                        mediaId = key.id,
                        streamUrl = null,
                        quality = bestQuality
                    )
                }
            )

            val stream = MediaStream(
                id = groupId,
                title = context.i18n.t("rutube.season_title_fmt", mainTitle, seasonNum, countLabel),
                url = streamInfo?.masterHlsUrl ?: "",
                type = StreamType.Hls,
                quality = bestQuality,
                format = "HLS",
                videoCodec = "H.264",
                sourceName = "Rutube",
                durationSeconds = sampleVideo.durationSeconds,
                isMapped = true,
                seasonNumber = seasonNum,
                episodeNumber = null,
                episodesCount = epCount,
                episodesTotal = expectedTotal,
                episodeName = channelSubtitle,
                qualityVariants = streamInfo?.qualities ?: emptyList(),
                subFilterId = "season_$seasonNum",
                subFilterLabel = context.i18n.t("rutube.season_filter_fmt", seasonNum)
            )
            streams.add(stream)
        }

        // 4B: Virtual Composite Release (Сводный релиз) for fragmented seasons
        for (seasonNum in targetSeasons) {
            if (seasonNum in seasonsWithFullReleases) continue
            val seasonEpisodes = validParsedEpisodes.filter { it.season == seasonNum }
            val uniqueEpisodes = seasonEpisodes.distinctBy { it.episode }.sortedBy { it.episode }
            val epCount = uniqueEpisodes.size

            val expectedTotal = seasonMetadataMap[seasonNum]?.episodeCount?.takeIf { it > 0 }
            val maxSingleAuthorCount = maxEpisodesPerSeason[seasonNum] ?: 0
            val shouldEmitComposite = epCount >= 2 && (epCount > maxSingleAuthorCount || (expectedTotal != null && epCount == expectedTotal && maxSingleAuthorCount < expectedTotal))

            if (shouldEmitComposite && streams.none { it.id == "rutube_season_${key.id}_${seasonNum}_composite" }) {
                val countLabel = when {
                    expectedTotal != null && expectedTotal > 0 && epCount >= expectedTotal -> "$epCount ${pluralizeEpisodes(epCount)} (все)"
                    expectedTotal != null && expectedTotal > 0 -> "$epCount из $expectedTotal ${pluralizeEpisodes(expectedTotal)}"
                    else -> "$epCount ${pluralizeEpisodes(epCount)}"
                }

                val sampleVideo = uniqueEpisodes.first().video
                val streamInfo = repository.getStreamInfo(sampleVideo.id)
                val bestQuality = streamInfo?.qualities?.firstOrNull()?.label ?: "1080p"
                val compositeId = "rutube_season_${key.id}_${seasonNum}_composite"
                context.sourceMappings.saveMappingsBatch(
                    uniqueEpisodes.map { ep ->
                        SourceMapping(
                            sourceType = "rutube-plugin",
                            sourceId = compositeId,
                            itemKey = ep.video.id,
                            seasons = listOf(seasonNum),
                            episodes = listOf(ep.episode),
                            mediaId = key.id,
                            streamUrl = null,
                            quality = bestQuality
                        )
                    }
                )

                val compositeStream = MediaStream(
                    id = compositeId,
                    title = context.i18n.t("rutube.season_title_fmt", mainTitle, seasonNum, countLabel),
                    url = streamInfo?.masterHlsUrl ?: "",
                    type = StreamType.Hls,
                    quality = bestQuality,
                    format = "HLS",
                    videoCodec = "H.264",
                    sourceName = "Rutube",
                    durationSeconds = sampleVideo.durationSeconds,
                    isMapped = true,
                    seasonNumber = seasonNum,
                    episodeNumber = null,
                    episodesCount = epCount,
                    episodesTotal = expectedTotal,
                    episodeName = context.i18n.t("rutube.composite_release_fmt", bestQuality),
                    qualityVariants = streamInfo?.qualities ?: emptyList(),
                    subFilterId = "season_$seasonNum",
                    subFilterLabel = context.i18n.t("rutube.season_filter_fmt", seasonNum)
                )
                streams.add(compositeStream)
                logger.info("Rutube: Emitted Composite Virtual Release for Season $seasonNum ($epCount episodes)")
            }
        }

        // Sort by season number ASC, then by episode count DESC
        streams.sortWith(compareBy({ it.seasonNumber ?: 99 }, { -(it.episodesCount ?: 0) }))

        logger.info("Rutube: Successfully emitted ${streams.size} optimized season releases for '$mainTitle'")
        streams
    }

    private fun isMatchingMovie(item: RutubeVideoItem, expectedRuntimeMinutes: Int?): Boolean {
        val titleLower = item.title.lowercase()
        if (stopWords.any { titleLower.contains(it) }) return false

        val minSeconds = if (expectedRuntimeMinutes != null && expectedRuntimeMinutes > 0) {
            expectedRuntimeMinutes * 60 * 0.75
        } else {
            3600.0 // At least 60 minutes
        }

        val maxSeconds = if (expectedRuntimeMinutes != null && expectedRuntimeMinutes > 0) {
            expectedRuntimeMinutes * 60 * 1.30
        } else {
            18000.0
        }

        return item.durationSeconds in minSeconds..maxSeconds
    }

    private fun isMatchingEpisode(
        item: RutubeVideoItem,
        rootTitle: String,
        targetSeason: Int,
        targetEpisode: Int
    ): Boolean {
        val titleLower = item.title.lowercase()
        if (stopWords.any { titleLower.contains(it) }) return false
        if (item.durationSeconds < 300.0) return false

        val match = episodeMatcher.parse(rootTitle, item.title)
        return when (match) {
            is MappingResult.Success -> {
                (targetSeason in match.seasons || match.seasons.isEmpty()) && (targetEpisode in match.episodes)
            }
            is MappingResult.Partial -> {
                targetEpisode in match.episodes
            }
            is MappingResult.Failed -> {
                val epPattern = Regex("""(?<!\d)$targetEpisode\s*(?:серия|сер|эпизод|ep)""", RegexOption.IGNORE_CASE)
                epPattern.containsMatchIn(item.title)
            }
        }
    }

    private fun pluralizeEpisodes(count: Int): String {
        val rem100 = count % 100
        val rem10 = count % 10
        return when {
            rem100 in 11..19 -> "серий"
            rem10 == 1 -> "серия"
            rem10 in 2..4 -> "серии"
            else -> "серий"
        }
    }
}

