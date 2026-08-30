package org.ensodai.avalonmediacard.plugin.recommendation.calculator

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaMetadata
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.AffinityVector
import org.ensodai.avalonmediacard.contract.model.ClickstreamEventType
import org.ensodai.avalonmediacard.contract.model.ClickstreamPayload
import org.ensodai.avalonmediacard.contract.model.ClickstreamTargetType
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.SemanticMoodClassifier
import kotlin.math.exp
import kotlin.time.Clock
import kotlin.uuid.Uuid

/**
 * Тяжелый вычислительный класс. Запрашивает телеметрию, ходит в БД за деталями,
 * выполняет дедупликацию в памяти, применяет формулы из RecommendationMath
 * и сохраняет итоговый вектор в кэш БД.
 */
class AffinityVectorCalculator(
    private val context: PluginContext
) {

    suspend fun recalculateVector(userId: Uuid) {
        // 1. Проверяем нужно ли вообще пересчитывать
        val currentEventCount = context.affinityStore.getUserEventCount(userId)
        val cachedEventCount = context.affinityStore.getCachedEventCount(userId)

        if (cachedEventCount != null && currentEventCount == cachedEventCount) {
            context.logger.info("Skip recalculating vector for user $userId: no new events (events=$currentEventCount)")
            return
        }

        context.logger.info("Recalculating affinity vector for user $userId (events: $cachedEventCount -> $currentEventCount)")

        // 2. Извлекаем телеметрию и явные данные
        val events = try {
            context.telemetry.getUserEvents(userId, limit = 2000)
        } catch (e: Exception) {
            context.logger.error("Failed to fetch telemetry for user $userId", e)
            emptyList()
        }

        val userMovies = try {
            context.userMovies.getUserMovies(userId)
        } catch (e: Exception) {
            context.logger.error("Failed to fetch user movies for user $userId", e)
            emptyList()
        }

        if (events.isEmpty() && userMovies.isEmpty()) {
            context.affinityStore.saveVector(userId, AffinityVector(), 0)
            return
        }

        // Кэш медиа-деталей в рамках одного расчета (In-memory дедупликация)
        val mediaCache = mutableMapOf<String, MediaMetadata?>()

        // Сырые баллы
        val rawScores = mapOf(
            TargetSpace.GENRE to mutableMapOf<String, Double>(),
            TargetSpace.KEYWORD to mutableMapOf<String, Double>(),
            TargetSpace.DIRECTOR to mutableMapOf<String, Double>(),
            TargetSpace.ACTOR to mutableMapOf<String, Double>(),
            TargetSpace.COMPANY to mutableMapOf<String, Double>(),
            TargetSpace.PACING to mutableMapOf<String, Double>(),
            TargetSpace.ERA to mutableMapOf<String, Double>(),
            TargetSpace.MOOD to mutableMapOf<String, Double>()
        )

        // Для расчета Сатиации (Saturation Penalty)
        val recentKeywordCounts = mutableMapOf<String, Int>()

        val now = Clock.System.now()

        // --- Пакетная предзагрузка медиаданных ---
        val keysToFetch = mutableSetOf<MediaKey>()
        for (event in events) {
            if (event.eventType == ClickstreamEventType.IMPRESSION_BATCH) {
                val payload = event.payload as? ClickstreamPayload.ImpressionBatch
                payload?.items?.forEach { 
                    val t = when(it.targetType) {
                        ClickstreamTargetType.MEDIA_MOVIE -> MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, it.id)
                        ClickstreamTargetType.MEDIA_TV -> MediaKey(MediaProvider.Tmdb, EntityType.TV, it.id)
                        else -> null
                    }
                    if (t != null) keysToFetch.add(t)
                }
            } else {
                val tId = event.targetId
                if (tId != null) {
                    val tType = event.targetType ?: ClickstreamTargetType.MEDIA_MOVIE
                    val t = when(tType) {
                        ClickstreamTargetType.MEDIA_MOVIE -> MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, tId)
                        ClickstreamTargetType.MEDIA_TV -> MediaKey(MediaProvider.Tmdb, EntityType.TV, tId)
                        else -> null
                    }
                    if (t != null) keysToFetch.add(t)
                }
            }
        }
        for (userMovie in userMovies) {
            val entityType = when (userMovie.mediaType) {
                org.ensodai.avalonmediacard.contract.model.MediaType.MOVIE -> EntityType.MOVIE
                org.ensodai.avalonmediacard.contract.model.MediaType.TV -> EntityType.TV
                else -> EntityType.MOVIE
            }
            keysToFetch.add(MediaKey(MediaProvider.Tmdb, entityType, userMovie.mediaId))
        }

        try {
            val batch = context.catalog.getMediaDetailsBatch(keysToFetch.toList(), requireSeasons = false, requireVideos = false)
            for ((key, meta) in batch) {
                mediaCache["${key.type}:${key.id}"] = meta
            }
        } catch (e: Exception) {
            context.logger.error("Failed to fetch batch metadata", e)
        }

        // 3. Итерация по событиям
        for (event in events) {
            val targets = mutableListOf<Pair<String, ClickstreamTargetType>>()

            if (event.eventType == ClickstreamEventType.IMPRESSION_BATCH) {
                val payload = event.payload as? ClickstreamPayload.ImpressionBatch
                if (payload != null) {
                    payload.items.forEach { targets.add(it.id to it.targetType) }
                }
            } else {
                val targetId = event.targetId
                if (targetId != null) {
                    val tType = event.targetType ?: ClickstreamTargetType.MEDIA_MOVIE
                    targets.add(targetId to tType)
                }
            }

            if (targets.isEmpty()) continue

            for ((targetId, targetType) in targets) {
                if (targetType == ClickstreamTargetType.PERSON) {
                    val wBase = RecSysConstants.EVENT_WEIGHTS[event.eventType] ?: 1.0
                    val wContext = RecSysConstants.CONTEXT_WEIGHTS[event.context] ?: 1.0
                    val eventScore = wBase * wContext * calculateDwellTimeMultiplier(event.dwellTimeMs)

                    val ts = event.timestamp
                    val daysAgo = if (ts != null) {
                        (now - ts).inWholeHours / 24.0
                    } else {
                        0.0
                    }

                    val actorDecay = calculateTimeDecay(daysAgo, TargetSpace.ACTOR)
                    val currentActor = rawScores[TargetSpace.ACTOR]!![targetId] ?: 0.0
                    rawScores[TargetSpace.ACTOR]!![targetId] = currentActor + (eventScore * actorDecay)

                    val dirDecay = calculateTimeDecay(daysAgo, TargetSpace.DIRECTOR)
                    val currentDir = rawScores[TargetSpace.DIRECTOR]!![targetId] ?: 0.0
                    rawScores[TargetSpace.DIRECTOR]!![targetId] = currentDir + (eventScore * dirDecay)

                    continue
                }

                val mediaKey = when (targetType) {
                    ClickstreamTargetType.MEDIA_MOVIE -> MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, targetId)
                    ClickstreamTargetType.MEDIA_TV -> MediaKey(MediaProvider.Tmdb, EntityType.TV, targetId)
                    else -> continue // Пока считаем только фильмы и сериалы
                }

                // In-memory дедупликация
                val cacheKey = "${mediaKey.type}:${mediaKey.id}"
                val mediaInfo = if (mediaCache.containsKey(cacheKey)) {
                    mediaCache[cacheKey]
                } else {
                    try {
                        val info =
                            context.catalog.getMediaDetails(mediaKey, requireSeasons = false, requireVideos = false)
                        mediaCache[cacheKey] = info
                        info
                    } catch (e: Exception) {
                        mediaCache[cacheKey] = null
                        null
                    }
                } ?: continue

                // Вычисляем базовый вес и контекст
                val wBase = RecSysConstants.EVENT_WEIGHTS[event.eventType] ?: 1.0
                val wContext = RecSysConstants.CONTEXT_WEIGHTS[event.context] ?: 1.0

                var posMultiplier = 1.0
                var dwellMultiplier = 1.0
                var eventScore = wBase * wContext

                when (event.eventType) {
                    ClickstreamEventType.PLAYBACK_STOP -> {
                        val payload = event.payload as? ClickstreamPayload.PlaybackStop
                        if (payload != null) {
                            eventScore = calculateWatchProgressWeightFromRatio(payload.completionPercentage) * wContext
                        }
                    }

                    ClickstreamEventType.SCROLL -> {
                        val payload = event.payload as? ClickstreamPayload.ScrollDepth
                        if (payload != null && payload.maxScrollDepthPercentage >= 0.8) {
                            posMultiplier = calculatePositionMultiplier(20) // Имитация глубокого просмотра карусели
                            dwellMultiplier = calculateDwellTimeMultiplier(event.dwellTimeMs)
                            eventScore = wBase * wContext * posMultiplier * dwellMultiplier
                        } else {
                            dwellMultiplier = calculateDwellTimeMultiplier(event.dwellTimeMs)
                            eventScore = wBase * wContext * dwellMultiplier
                        }
                    }

                    else -> {
                        val payload = event.payload as? ClickstreamPayload.CarouselInteraction
                        posMultiplier = calculatePositionMultiplier(payload?.positionIndex)
                        dwellMultiplier = calculateDwellTimeMultiplier(event.dwellTimeMs)
                        eventScore = wBase * wContext * posMultiplier * dwellMultiplier
                    }
                }

                // Давность события
                val ts = event.timestamp
                val daysAgo = if (ts != null) {
                    (now - ts).inWholeHours / 24.0
                } else {
                    0.0
                }

                // Проецируем баллы (Жанры)
                val genreDecay = calculateTimeDecay(daysAgo, TargetSpace.GENRE)
                for (genre in mediaInfo.genres) {
                    val genreKey = genre.id.toString()
                    val current = rawScores[TargetSpace.GENRE]!![genreKey] ?: 0.0
                    rawScores[TargetSpace.GENRE]!![genreKey] = current + (eventScore * genreDecay)
                }

                // Проецируем баллы (Ключевые слова)
                val isRecent = daysAgo <= 2.0
                val keywordDecay = calculateTimeDecay(daysAgo, TargetSpace.KEYWORD)
                for (kw in mediaInfo.keywords) {
                    val kwKey = kw.id.toString()
                    if (isRecent) {
                        recentKeywordCounts[kwKey] = (recentKeywordCounts[kwKey] ?: 0) + 1
                    }

                    // Нормализуем IDF (макс IDF ~ 6.0 для 1 млн базы)
                    val df = if (kw.documentFrequency > 0) kw.documentFrequency else 10
                    val idf = calculateBayesianIdf(df, 1_000_000)
                    if (idf == 0.0) continue // Мусорный тег (Trash Cutoff)

                    val projectedScore = projectScoreWithAsymmetricPenalty(eventScore, idf / 6.0)
                    val current = rawScores[TargetSpace.KEYWORD]!![kwKey] ?: 0.0
                    rawScores[TargetSpace.KEYWORD]!![kwKey] = current + (projectedScore * keywordDecay)
                }

                // Режиссер
                val director = mediaInfo.director
                if (director != null) {
                    val dirDecay = calculateTimeDecay(daysAgo, TargetSpace.DIRECTOR)
                    val directorKey = mediaInfo.directorId ?: director
                    val current = rawScores[TargetSpace.DIRECTOR]!![directorKey] ?: 0.0
                    rawScores[TargetSpace.DIRECTOR]!![directorKey] = current + (eventScore * dirDecay)
                }

                // Актеры
                val actorDecay = calculateTimeDecay(daysAgo, TargetSpace.ACTOR)
                mediaInfo.cast.forEachIndexed { index, actor ->
                    val castWeight = exp(-RecSysConstants.CAST_DECAY_LAMBDA * index)
                    val actorKey = actor.id ?: actor.name

                    val current = rawScores[TargetSpace.ACTOR]!![actorKey] ?: 0.0
                    rawScores[TargetSpace.ACTOR]!![actorKey] = current + (eventScore * actorDecay * castWeight)
                }

                // --- Новые вектора (Исследование 7) ---

                // 1. Вектор Студий (Company)
                val companyDecay = calculateTimeDecay(daysAgo, TargetSpace.COMPANY)
                mediaInfo.productionCompanies.forEach { company ->
                    val compKey = company.id.toString()
                    val currentComp = rawScores[TargetSpace.COMPANY]!![compKey] ?: 0.0
                    rawScores[TargetSpace.COMPANY]!![compKey] = currentComp + (eventScore * companyDecay)
                }

                // 2. Вектор Эпохи (Era)
                val releaseYear = mediaInfo.releaseDate?.substringBefore("-")?.toIntOrNull()
                if (releaseYear != null) {
                    val eraKey = if (releaseYear < 1970) "Golden Age (<1970)" else "${(releaseYear / 10) * 10}s"
                    val eraDecay = calculateTimeDecay(daysAgo, TargetSpace.ERA)
                    val currentEra = rawScores[TargetSpace.ERA]!![eraKey] ?: 0.0
                    rawScores[TargetSpace.ERA]!![eraKey] = currentEra + (eventScore * eraDecay)
                }

                // 3. Вектор Темпа (Pacing)
                val runtime = mediaInfo.runtime ?: 0
                if (runtime > 0) {
                    val pacingKey = when {
                        mediaKey.type == EntityType.TV -> when {
                            runtime < 30 -> "fast"
                            runtime <= 50 -> "medium"
                            else -> "slow_epic"
                        }

                        else -> when {
                            runtime < 90 -> "fast"
                            runtime <= 120 -> "medium"
                            else -> "slow_epic"
                        }
                    }
                    val pacingDecay = calculateTimeDecay(daysAgo, TargetSpace.PACING)
                    val currentPacing = rawScores[TargetSpace.PACING]!![pacingKey] ?: 0.0
                    rawScores[TargetSpace.PACING]!![pacingKey] = currentPacing + (eventScore * pacingDecay)
                }

                // 4. Вектор Настроения (Mood)
                val moodDecay = calculateTimeDecay(daysAgo, TargetSpace.MOOD)
                SemanticMoodClassifier.processMoodScores(
                    keywords = mediaInfo.keywords,
                    eventScore = eventScore,
                    moodDecay = moodDecay,
                    targetMap = rawScores[TargetSpace.MOOD]!!
                )


            } // end targets loop
        }

        // 4. Итерация по Явным данным (User Movies)
        for (userMovie in userMovies) {
            val entityType = when (userMovie.mediaType) {
                MediaType.MOVIE -> EntityType.MOVIE
                MediaType.TV -> EntityType.TV
                else -> EntityType.MOVIE
            }
            val mediaKey = MediaKey(MediaProvider.Tmdb, entityType, userMovie.mediaId)

            val cacheKey = "${mediaKey.type}:${mediaKey.id}"
            val mediaInfo = if (mediaCache.containsKey(cacheKey)) {
                mediaCache[cacheKey]
            } else {
                try {
                    val info = context.catalog.getMediaDetails(mediaKey, requireSeasons = false, requireVideos = false)
                    mediaCache[cacheKey] = info
                    info
                } catch (e: Exception) {
                    mediaCache[cacheKey] = null
                    null
                }
            } ?: continue

            val explicitScore = getStatusWeight(userMovie.status) +
                    calculateRatingWeight(userMovie.userRating) +
                    calculateWatchProgressWeight(userMovie.progressSeconds, userMovie.durationSeconds) +
                    getCollectionWeight(userMovie.inCollection)

            if (explicitScore == 0.0) continue

            val ts = userMovie.lastWatchedAt
            val daysAgo = (now - ts).inWholeHours / 24.0

            // Проецируем баллы (Жанры)
            val genreDecay = calculateTimeDecay(daysAgo, TargetSpace.GENRE)
            for (genre in mediaInfo.genres) {
                val genreKey = genre.id.toString()
                val current = rawScores[TargetSpace.GENRE]!![genreKey] ?: 0.0
                rawScores[TargetSpace.GENRE]!![genreKey] = current + (explicitScore * genreDecay)
            }

            // Проецируем баллы (Ключевые слова)
            val isRecent = daysAgo <= 2.0
            val keywordDecay = calculateTimeDecay(daysAgo, TargetSpace.KEYWORD)
            for (kw in mediaInfo.keywords) {
                val kwKey = kw.id.toString()
                if (isRecent) {
                    recentKeywordCounts[kwKey] = (recentKeywordCounts[kwKey] ?: 0) + 1
                }

                val df = if (kw.documentFrequency > 0) kw.documentFrequency else 10
                val idf = calculateBayesianIdf(df, 1_000_000)
                if (idf == 0.0) continue

                val projectedScore = projectScoreWithAsymmetricPenalty(explicitScore, idf / 6.0)
                val current = rawScores[TargetSpace.KEYWORD]!![kwKey] ?: 0.0
                rawScores[TargetSpace.KEYWORD]!![kwKey] = current + (projectedScore * keywordDecay)
            }

            // Режиссер
            val director = mediaInfo.director
            if (director != null) {
                val dirDecay = calculateTimeDecay(daysAgo, TargetSpace.DIRECTOR)
                val directorKey = mediaInfo.directorId ?: director
                val current = rawScores[TargetSpace.DIRECTOR]!![directorKey] ?: 0.0
                rawScores[TargetSpace.DIRECTOR]!![directorKey] = current + (explicitScore * dirDecay)
            }

            // Актеры
            val actorDecay = calculateTimeDecay(daysAgo, TargetSpace.ACTOR)
            mediaInfo.cast.forEachIndexed { index, actor ->
                val castWeight = exp(-RecSysConstants.CAST_DECAY_LAMBDA * index)
                val actorKey = actor.id ?: actor.name

                val current = rawScores[TargetSpace.ACTOR]!![actorKey] ?: 0.0
                rawScores[TargetSpace.ACTOR]!![actorKey] = current + (explicitScore * actorDecay * castWeight)
            }

            // --- Новые вектора (Исследование 7) ---

            // 1. Вектор Студий (Company)
            val companyDecay = calculateTimeDecay(daysAgo, TargetSpace.COMPANY)
            mediaInfo.productionCompanies.forEach { company ->
                val compKey = company.id.toString()
                val currentComp = rawScores[TargetSpace.COMPANY]!![compKey] ?: 0.0
                rawScores[TargetSpace.COMPANY]!![compKey] = currentComp + (explicitScore * companyDecay)
            }

            // 2. Вектор Эпохи (Era)
            val releaseYear = mediaInfo.releaseDate?.substringBefore("-")?.toIntOrNull()
            if (releaseYear != null) {
                val eraKey = if (releaseYear < 1970) "Golden Age (<1970)" else "${(releaseYear / 10) * 10}s"
                val eraDecay = calculateTimeDecay(daysAgo, TargetSpace.ERA)
                val currentEra = rawScores[TargetSpace.ERA]!![eraKey] ?: 0.0
                rawScores[TargetSpace.ERA]!![eraKey] = currentEra + (explicitScore * eraDecay)
            }

            // 3. Вектор Темпа (Pacing)
            val runtime = mediaInfo.runtime ?: 0
            if (runtime > 0) {
                val pacingKey = when {
                    mediaKey.type == EntityType.TV -> when {
                        runtime < 30 -> "fast"
                        runtime <= 50 -> "medium"
                        else -> "slow_epic"
                    }

                    else -> when {
                        runtime < 90 -> "fast"
                        runtime <= 120 -> "medium"
                        else -> "slow_epic"
                    }
                }
                val pacingDecay = calculateTimeDecay(daysAgo, TargetSpace.PACING)
                val currentPacing = rawScores[TargetSpace.PACING]!![pacingKey] ?: 0.0
                rawScores[TargetSpace.PACING]!![pacingKey] = currentPacing + (explicitScore * pacingDecay)
            }

            // 4. Вектор Настроения (Mood)
            val moodDecay = calculateTimeDecay(daysAgo, TargetSpace.MOOD)
            SemanticMoodClassifier.processMoodScores(
                keywords = mediaInfo.keywords,
                eventScore = explicitScore,
                moodDecay = moodDecay,
                targetMap = rawScores[TargetSpace.MOOD]!!
            )
        }

        val sessionBingeVector = mutableMapOf<String, Double>()
        for (event in events) {
            val ts = event.timestamp
            val daysAgo = if (ts != null) (now - ts).inWholeHours / 24.0 else 0.0
            if (daysAgo <= 2.0 && event.targetType == ClickstreamTargetType.MEDIA_MOVIE) {
                val mediaKey = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, event.targetId ?: continue)
                val cacheKey = "${mediaKey.type}:${mediaKey.id}"
                val mediaInfo = mediaCache[cacheKey] ?: continue
                for (genre in mediaInfo.genres) {
                    val gKey = genre.id.toString()
                    sessionBingeVector[gKey] = (sessionBingeVector[gKey] ?: 0.0) + 1.0
                }
            }
        }
        val normalizedBingeVector = sessionBingeVector.mapValues {
            it.value / (sessionBingeVector.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0)
        }

        val recentWatchedIds = userMovies
            .filter { it.status == MediaStatus.COMPLETED || it.status == MediaStatus.WATCHING }
            .sortedByDescending { it.lastWatchedAt }
            .take(50)
            .map { it.mediaId }

        // 5. Нормализация (Смещенная Сигмоида) + Сатиация
        val vector = AffinityVector(
            genreWeights = rawScores[TargetSpace.GENRE]!!.mapValues { normalizeScore(it.value, TargetSpace.GENRE) }
                .filterValues { it > 0.01 },
            keywordWeights = rawScores[TargetSpace.KEYWORD]!!.mapValues { (kw, raw) ->
                val baseNorm = normalizeScore(raw, TargetSpace.KEYWORD)
                baseNorm * calculateSaturationMultiplier(recentKeywordCounts[kw] ?: 0)
            }.filterValues { it > 0.01 },
            directorWeights = rawScores[TargetSpace.DIRECTOR]!!.mapValues {
                normalizeScore(
                    it.value,
                    TargetSpace.DIRECTOR
                )
            }.filterValues { it > 0.01 },
            actorWeights = rawScores[TargetSpace.ACTOR]!!.mapValues { normalizeScore(it.value, TargetSpace.ACTOR) }
                .filterValues { it > 0.01 },
            companyWeights = rawScores[TargetSpace.COMPANY]!!.mapValues {
                normalizeScore(
                    it.value,
                    TargetSpace.COMPANY
                )
            }.filterValues { it > 0.01 },
            pacingWeights = rawScores[TargetSpace.PACING]!!.mapValues { normalizeScore(it.value, TargetSpace.PACING) }
                .filterValues { it > 0.01 },
            eraWeights = rawScores[TargetSpace.ERA]!!.mapValues { normalizeScore(it.value, TargetSpace.ERA) }
                .filterValues { it > 0.01 },
            moodWeights = rawScores[TargetSpace.MOOD]!!.mapValues { normalizeScore(it.value, TargetSpace.MOOD) }
                .filterValues { it > 0.01 },
            recentWatchedIds = recentWatchedIds,
            sessionBingeVector = normalizedBingeVector
        )

        // 6. Сохранение в БД и инвалидация кэша ленты пользователя
        context.affinityStore.saveVector(userId, vector, currentEventCount)
        context.feedCache.invalidateUser(userId)
        context.logger.info("Successfully recalculated vector for user $userId. RESULT: $vector")
    }
}
