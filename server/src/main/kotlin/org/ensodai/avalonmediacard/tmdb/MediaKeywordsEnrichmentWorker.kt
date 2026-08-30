package org.ensodai.avalonmediacard.tmdb

import kotlinx.coroutines.*
import org.ensodai.avalonmediacard.repository.MediaKeywordRepository
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@Single
class MediaKeywordsEnrichmentWorker(
    private val tmdbApi: TmdbApi,
    private val mediaKeywordRepository: MediaKeywordRepository
) {
    private val logger = LoggerFactory.getLogger(MediaKeywordsEnrichmentWorker::class.java)
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            logger.info("MediaKeywordsEnrichmentWorker started. Will enrich missing keywords in background.")
            while (isActive) {
                try {
                    enrichBatch()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    logger.error("Error in MediaKeywordsEnrichmentWorker", e)
                }
                delay(5.minutes)
            }
        }
    }

    private suspend fun enrichBatch() {
        // Берем пачку фильмов без тегов
        val missing = mediaKeywordRepository.getMediaIdsWithoutKeywords(20)
        if (missing.isEmpty()) return

        logger.info("Enriching keywords for ${missing.size} media items...")

        for ((mediaId, type, tmdbId) in missing) {
            val prefix = if (type == "tv") "tv:" else "movie:"
            val fullId = "$prefix$tmdbId"

            try {
                val keywords = tmdbApi.getEnglishKeywords(fullId)
                mediaKeywordRepository.saveKeywords(mediaId, keywords)
                logger.debug("Saved ${keywords.size} keywords for $fullId")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                logger.error("Failed to enrich keywords for $fullId", e)
            }

            // Маленькая пауза, чтобы не спамить TMDB
            delay(1000.milliseconds)
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        logger.info("MediaKeywordsEnrichmentWorker stopped.")
    }
}
