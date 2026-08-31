package org.ensodai.avalonmediacard.tmdb

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaCatalog
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaMetadata
import org.ensodai.avalonmediacard.contract.model.PersonMetadata
import org.ensodai.avalonmediacard.contract.model.TmdbMovieDto
import org.ensodai.avalonmediacard.contract.model.TmdbMultiSearchDto
import org.ensodai.avalonmediacard.repository.MediaDiscoverCacheRepository
import org.ensodai.avalonmediacard.repository.MediaRepository
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

@Single
class TmdbMediaCatalog(
    private val repository: TmdbRepository,
    private val mapper: TmdbMetadataMapper,
    private val cacheRepository: MediaRepository,
    private val discoverCache: MediaDiscoverCacheRepository
) : MediaCatalog {
    private val catalogScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeRequests = ConcurrentHashMap<MediaKey, Mutex>()
    private val logger = LoggerFactory.getLogger(TmdbMediaCatalog::class.java)

    override suspend fun getTrending(page: Int, language: String): List<TmdbMovieDto> {
        val params = mapOf("category" to "trending_movies")
        val cached = discoverCache.get(params, EntityType.MOVIE, page, language)
        if (cached != null) return cached
        val fresh = repository.getTrendingMovies(page, language)
        if (fresh.isNotEmpty()) discoverCache.put(params, EntityType.MOVIE, page, language, results = fresh)
        return fresh
    }

    override suspend fun getTopRated(page: Int, language: String): List<TmdbMovieDto> {
        val params = mapOf("category" to "top_rated_movies")
        val cached = discoverCache.get(params, EntityType.MOVIE, page, language)
        if (cached != null) return cached
        val fresh = repository.getTopRatedMovies(page, language)
        if (fresh.isNotEmpty()) discoverCache.put(params, EntityType.MOVIE, page, language, results = fresh)
        return fresh
    }

    override suspend fun getUpcoming(page: Int, language: String): List<TmdbMovieDto> {
        val params = mapOf("category" to "upcoming_movies")
        val cached = discoverCache.get(params, EntityType.MOVIE, page, language)
        if (cached != null) return cached
        val fresh = repository.getUpcomingMovies(page, language)
        if (fresh.isNotEmpty()) discoverCache.put(params, EntityType.MOVIE, page, language, results = fresh)
        return fresh
    }

    override suspend fun getTrendingShows(page: Int, language: String): List<TmdbMovieDto> {
        val params = mapOf("category" to "trending_shows")
        val cached = discoverCache.get(params, EntityType.TV, page, language)
        if (cached != null) return cached
        val fresh = repository.getTrendingShows(page, language)
        if (fresh.isNotEmpty()) discoverCache.put(params, EntityType.TV, page, language, results = fresh)
        return fresh
    }

    override suspend fun getPopularShows(page: Int, language: String): List<TmdbMovieDto> {
        val params = mapOf("category" to "popular_shows")
        val cached = discoverCache.get(params, EntityType.TV, page, language)
        if (cached != null) return cached
        val fresh = repository.getPopularShows(page, language)
        if (fresh.isNotEmpty()) discoverCache.put(params, EntityType.TV, page, language, results = fresh)
        return fresh
    }

    override suspend fun getTopRatedShows(page: Int, language: String): List<TmdbMovieDto> {
        val params = mapOf("category" to "top_rated_shows")
        val cached = discoverCache.get(params, EntityType.TV, page, language)
        if (cached != null) return cached
        val fresh = repository.getTopRatedShows(page, language)
        if (fresh.isNotEmpty()) discoverCache.put(params, EntityType.TV, page, language, results = fresh)
        return fresh
    }

    override suspend fun getRecommendations(key: MediaKey, page: Int, language: String): List<TmdbMovieDto> {
        val params = mapOf("category" to "recommendations", "media_id" to key.id)
        val cached = discoverCache.get(params, key.type, page, language)
        if (cached != null) return cached
        val tmdbId = if (key.type == EntityType.TV) "tv:${key.id}" else key.id
        val fresh = repository.getRecommendations(tmdbId, page, language)
        if (fresh.isNotEmpty()) discoverCache.put(params, key.type, page, language, results = fresh)
        return fresh
    }

    override suspend fun getSimilar(key: MediaKey, page: Int, language: String): List<TmdbMovieDto> {
        val params = mapOf("category" to "similar", "media_id" to key.id)
        val cached = discoverCache.get(params, key.type, page, language)
        if (cached != null) return cached
        val tmdbId = if (key.type == EntityType.TV) "tv:${key.id}" else key.id
        val fresh = repository.getSimilarMovies(tmdbId, page, language)
        if (fresh.isNotEmpty()) discoverCache.put(params, key.type, page, language, results = fresh)
        return fresh
    }

    override suspend fun searchMedia(query: String, page: Int, language: String): List<TmdbMultiSearchDto> {
        return repository.searchMulti(query, page, language)
    }

    override suspend fun discoverMedia(
        genres: List<Int>,
        keywords: List<Int>,
        page: Int,
        isTv: Boolean,
        language: String
    ): List<TmdbMovieDto> {
        val params = mutableMapOf<String, String>()
        if (genres.isNotEmpty()) params["with_genres"] = genres.joinToString(",")
        if (keywords.isNotEmpty()) params["with_keywords"] = keywords.joinToString(",")
        val targetType = if (isTv) EntityType.TV else EntityType.MOVIE
        val cached = discoverCache.get(params, targetType, page, language)
        if (cached != null) return cached
        val fresh = repository.discoverMedia(genres, keywords, page, isTv, language)
        if (fresh.isNotEmpty()) discoverCache.put(params, targetType, page, language, results = fresh)
        return fresh
    }

    override suspend fun discoverMediaByParams(
        params: Map<String, String>,
        targetType: EntityType,
        page: Int,
        language: String
    ): List<TmdbMovieDto> {
        val cached = discoverCache.get(params, targetType, page, language)
        if (cached != null) {
            logger.info("discoverMediaByParams БД-КЭШ-ХИТ для targetType=$targetType, page=$page, lang=$language")
            return cached
        }
        val fresh = repository.discoverMediaByParams(params, targetType, page, language)
        if (fresh.isNotEmpty()) {
            discoverCache.put(params, targetType, page, language, results = fresh)
        }
        return fresh
    }

    override suspend fun getMediaDetails(
        key: MediaKey,
        requireSeasons: Boolean,
        requireVideos: Boolean,
        language: String
    ): MediaMetadata {
        logger.debug("getMediaDetails called for key={}, lang={}", key, language)

        try {
            val dbCached = cacheRepository.getMetadata("tmdb", key.id, language)
            if (dbCached != null && dbCached.rating != null) {
                logger.info("getMediaDetails БД-КЭШ-ХИТ для key=$key, lang=$language")
                return dbCached
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.error("Ошибка при чтении кэша БД для key=$key", e)
        }

        val mutex = activeRequests.getOrPut(key) { Mutex() }
        return mutex.withLock {
            val dbCached = cacheRepository.getMetadata("tmdb", key.id, language)
            if (dbCached != null && dbCached.rating != null) {
                return@withLock dbCached
            }

            val tmdbId = if (key.type == EntityType.TV && !key.id.startsWith("tv:")) "tv:${key.id}" else key.id
            val details =
                repository.getMovieDetails(tmdbId, requireVideos, language) ?: throw Exception("Медиа не найдено в TMDB")
            val metadata = mapper.mapMediaDetails(key.id, details, language = language)

            catalogScope.launch {
                try {
                    cacheRepository.upsertMetadata(
                        catalogId = "tmdb",
                        externalId = key.id,
                        mediaType = key.type.name,
                        metadata = metadata,
                        language = language
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            metadata
        }.also {
            activeRequests.remove(key)
        }
    }

    override suspend fun getMediaDetailsBatch(
        keys: List<MediaKey>,
        requireSeasons: Boolean,
        requireVideos: Boolean,
        language: String
    ): Map<MediaKey, MediaMetadata> {
        if (keys.isEmpty()) return emptyMap()
        
        val result = mutableMapOf<MediaKey, MediaMetadata>()
        
        // 1. Пытаемся пакетно забрать из БД (это 9 запросов для любого количества ID)
        val externalIds = keys.map { it.id }.distinct()
        val dbCachedBatch = try {
            cacheRepository.getMetadataBatch("tmdb", externalIds, language)
        } catch (e: Exception) {
            logger.error("Ошибка при пакетном чтении кэша БД", e)
            emptyMap()
        }
        
        // Мапим обратно по ключам, чтобы быстро найти
        for (key in keys) {
            val cached = dbCachedBatch[key.id]
            if (cached != null && cached.rating != null) {
                result[key] = cached
            }
        }
        
        // 2. Все ключи, которых не оказалось в БД — запрашиваем по одному (или асинхронно)
        // Для избежания спама в TMDB, мы вызовем обычный getMediaDetails, который берет локи
        val missingKeys = keys.filter { !result.containsKey(it) }
        if (missingKeys.isNotEmpty()) {
            logger.info("getMediaDetailsBatch: ${result.size} найдено в БД, ${missingKeys.size} отсутствует. Запрашиваем TMDB...")
            for (key in missingKeys) {
                try {
                    result[key] = getMediaDetails(key, requireSeasons, requireVideos, language)
                } catch (e: Exception) {
                    logger.error("Ошибка при получении отсутствующих медиа key=$key", e)
                }
            }
        }
        
        return result
    }

    override suspend fun getPersonDetails(key: MediaKey, language: String): PersonMetadata {
        val person = repository.getPersonDetails(key.id, language) ?: throw Exception("Актер не найден в TMDB")
        return mapper.mapPersonDetails(person)
    }

    override suspend fun getSeasonDetails(
        key: MediaKey,
        seasonNumber: Int,
        language: String
    ): List<org.ensodai.avalonmediacard.contract.slot.EpisodeItem> {
        val cached = cacheRepository.getSeasonDetails("tmdb", key.id, language, seasonNumber)
        if (cached != null && cached.isNotEmpty()) {
            logger.info("getSeasonDetails БД-КЭШ-ХИТ для key=$key, season=$seasonNumber, lang=$language")
            return cached
        }

        val tmdbId = if (key.type == EntityType.TV && !key.id.startsWith("tv:")) "tv:${key.id}" else key.id
        val seasonDetail = repository.getSeasonDetails(tmdbId, seasonNumber, language) ?: return emptyList()
        val mapped = mapper.mapSeasonDetails(seasonDetail)

        catalogScope.launch {
            try {
                cacheRepository.upsertSeasonDetails("tmdb", key.id, language, seasonNumber, mapped)
            } catch (e: Exception) {
                logger.error("Ошибка сохранения кэша сезона key=$key, season=$seasonNumber", e)
            }
        }
        return mapped
    }
}
