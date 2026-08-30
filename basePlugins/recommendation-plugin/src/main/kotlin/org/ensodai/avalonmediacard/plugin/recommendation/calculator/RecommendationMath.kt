package org.ensodai.avalonmediacard.plugin.recommendation.calculator

import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.model.ClickstreamContext
import org.ensodai.avalonmediacard.contract.model.ClickstreamEventType
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow

/**
 * Пространства метаданных TMDB, на которые проецируется интерес пользователя.
 */
enum class TargetSpace {
    GENRE,
    KEYWORD,
    DIRECTOR,
    ACTOR,
    COMPANY,
    PACING,
    ERA,
    MOOD
}

/**
 * Фундаментальные константы детерминированной математической модели.
 * Выведены на основе эмпирических исследований (см. Исследование 3).
 */
object RecSysConstants {
    // Параметры смещенной сигмоиды: Pair(midpoint, steepness)
    val SIGMOID_PARAMS = mapOf(
        TargetSpace.GENRE to (15.0 to 0.12),       // Было (50.0 to 0.05) - повышена чувствительность
        TargetSpace.KEYWORD to (5.0 to 0.25),      // Было (15.0 to 0.15)
        TargetSpace.DIRECTOR to (4.0 to 0.35),
        TargetSpace.ACTOR to (6.0 to 0.20),
        TargetSpace.COMPANY to (5.0 to 0.25),
        TargetSpace.PACING to (10.0 to 0.15),
        TargetSpace.ERA to (15.0 to 0.10),
        TargetSpace.MOOD to (4.0 to 0.30)
    )

    // Периоды полураспада (Half-life) в сутках
    val HALF_LIVES_DAYS = mapOf(
        TargetSpace.GENRE to 180.0,
        TargetSpace.KEYWORD to 7.0, // Ультракороткая память для тегов (Mood)
        TargetSpace.DIRECTOR to 90.0,
        TargetSpace.ACTOR to 90.0,
        TargetSpace.COMPANY to 90.0,
        TargetSpace.PACING to 14.0,
        TargetSpace.ERA to 180.0,
        TargetSpace.MOOD to 3.0
    )

    // Базовые веса событий (Wbase)
    val EVENT_WEIGHTS = mapOf(
        ClickstreamEventType.SEARCH to 3.0,
        ClickstreamEventType.CLICK to 1.0,
        ClickstreamEventType.PAGE_VIEW to 0.5,
        ClickstreamEventType.SCROLL to 0.5,
        ClickstreamEventType.IMPRESSION_BATCH to -0.1,
        ClickstreamEventType.PLAYBACK_STOP to 1.0
        // DWELL вычисляется динамически
    )

    // Модификаторы контекста (Wcontext)
    val CONTEXT_WEIGHTS = mapOf(
        ClickstreamContext.SEARCH_PAGE to 2.5,
        ClickstreamContext.CAROUSEL_DETAILS_SIMILAR to 2.0,
        ClickstreamContext.CAROUSEL_DETAILS_RECOMMENDATIONS to 1.8,
        ClickstreamContext.CAROUSEL_PERSON to 1.5,
        ClickstreamContext.CAROUSEL_DISCOVER to 1.2,
        ClickstreamContext.DETAILS_PAGE to 1.0,
        ClickstreamContext.CAROUSEL_CONTINUE to 0.8,
        ClickstreamContext.HERO_BANNER to 0.5,
        ClickstreamContext.HOME_PAGE to 0.5
    )

    const val POSITION_BETA = 0.5
    const val BOUNCE_THRESHOLD_SEC = 10.0
    const val MAX_DWELL_SEC = 120.0
    const val MAX_BOUNCE_PENALTY = 2.0
    const val CAST_DECAY_LAMBDA = 0.4
}

object ExplicitFeedbackConstants {
    const val WEIGHT_COMPLETED = 10.0
    const val WEIGHT_WATCHING = 5.0
    const val WEIGHT_PLANNED = 3.0
    const val WEIGHT_DROPPED = -5.0
    const val WEIGHT_COLLECTION = 5.0

    const val RATING_MAX_WEIGHT = 15.0

    const val PROGRESS_MAX_WEIGHT = 10.0
    const val PROGRESS_BOUNCE_PENALTY = 5.0
    const val PROGRESS_BOUNCE_THRESHOLD = 0.1
    const val PROGRESS_PLATEAU_THRESHOLD = 0.8
}

/**
 * 1. Компенсация позиционного смещения (Position Bias Multiplier)
 * Логарифмически увеличивает вес клика, совершенного глубоко в карусели.
 */
fun calculatePositionMultiplier(positionIndex: Int?): Double {
    if (positionIndex == null || positionIndex < 0) return 1.0
    return 1.0 + RecSysConstants.POSITION_BETA * log2(positionIndex.toDouble() + 1.0)
}

/**
 * 2. Функция Dwell Time и Bounce Penalty
 * Пенализирует быстрые отказы (Bounce) и поощряет долгое изучение страницы.
 */
fun calculateDwellTimeMultiplier(dwellTimeMs: Long?): Double {
    if (dwellTimeMs == null || dwellTimeMs == 0L) return 1.0 // Для событий без удержания

    val t = dwellTimeMs.toDouble() / 1000.0
    val tBounce = RecSysConstants.BOUNCE_THRESHOLD_SEC
    val tMax = RecSysConstants.MAX_DWELL_SEC

    return if (t < tBounce) {
        // Нейтральный вклад вместо отрицательной эрозии профиля
        0.0
    } else {
        // Логарифмический рост от 0.1 до 1.0
        val progress = log10(t - tBounce + 1.0) / log10(tMax - tBounce + 1.0)
        (0.1 + 0.9 * progress).coerceAtMost(1.0)
    }
}

/**
 * 3. Функция экспоненциального затухания (Time Decay)
 * Плавно дисконтирует старые интересы.
 */
fun calculateTimeDecay(daysAgo: Double, space: TargetSpace): Double {
    val halfLife = RecSysConstants.HALF_LIVES_DAYS[space] ?: 30.0
    return 2.0.pow(-daysAgo / halfLife)
}

/**
 * 4. Смещенная Сигмоида (Shifted Sigmoid Normalization)
 * Нормализует агрегированные сырые баллы в диапазон [0.0, 1.0].
 */
fun normalizeScore(rawScore: Double, space: TargetSpace): Double {
    if (rawScore <= 0.0) return 0.0 // Отрицательные баллы обнуляются

    val params = RecSysConstants.SIGMOID_PARAMS[space] ?: return 0.0
    val x0 = params.first
    val k = params.second

    val sigRaw = 1.0 / (1.0 + exp(-k * (rawScore - x0)))
    val sigZero = 1.0 / (1.0 + exp(-k * (0.0 - x0)))

    return ((sigRaw - sigZero) / (1.0 - sigZero)).coerceIn(0.0, 1.0)
}

/**
 * 5. Температурное масштабирование (Разрушение эхо-комнаты)
 * Подтягивает слабые (забытые) интересы для стимуляции Exploration.
 */
fun applyTemperatureScaling(affinity: Double, temperature: Double): Double {
    if (affinity <= 0.0 || temperature <= 0.0) return 0.0
    return affinity.pow(1.0 / temperature).coerceIn(0.0, 1.0)
}

/**
 * Байесовская инвертированная частота (Bayesian BM25 IDF) с жестким отсечением (Trash Tag Cutoff).
 * Отсекает мусорные теги (DF > 5%), защищая графы от инфляции весов.
 */
fun calculateBayesianIdf(documentFrequency: Int, totalDocuments: Int): Double {
    val df = documentFrequency.toDouble()
    val n = totalDocuments.toDouble()

    // Trash Cutoff: если тег в 5%+ фильмов, он считается мусором
    if (df > 0.05 * n) return 0.0

    val idf = log10((n - df + 0.5) / (df + 0.5))
    return max(0.0, idf)
}

/**
 * Асимметричная атрибуция негативного фидбека.
 * Негативные баллы возводятся в степень 1.5 по IDF, перекладывая всю вину на редкие теги.
 */
fun projectScoreWithAsymmetricPenalty(score: Double, normalizedIdf: Double): Double {
    if (score >= 0.0) return score * normalizedIdf
    return score * normalizedIdf.pow(1.5)
}

/**
 * Индекс пресыщения (Saturation Penalty).
 * Глушит теги, если юзер "переел" однотипного контента за выходные.
 */
fun calculateSaturationMultiplier(recentOccurrences: Int): Double {
    return max(0.4, 1.0 - (recentOccurrences * 0.12))
}

/**
 * Множитель кросс-опыления (Serendipity Boost).
 * Форсирует скор фильма, если у него есть мощный тег, способный пробить барьер нелюбимого жанра.
 */
fun calculateSerendipityMultiplier(maxGenreAffinity: Double, maxKeywordAffinity: Double): Double {
    // Константа агрессивности = 4.0, порог нейтральности = 0.5
    return 1.0 + max(0.0, 0.5 - maxGenreAffinity) * maxKeywordAffinity * 4.0
}

/**
 * 6. Оценка явного статуса (MediaStatus)
 */
fun getStatusWeight(status: MediaStatus?): Double = when (status) {
    MediaStatus.COMPLETED -> ExplicitFeedbackConstants.WEIGHT_COMPLETED
    MediaStatus.WATCHING -> ExplicitFeedbackConstants.WEIGHT_WATCHING
    MediaStatus.PLANNED -> ExplicitFeedbackConstants.WEIGHT_PLANNED
    MediaStatus.DROPPED -> ExplicitFeedbackConstants.WEIGHT_DROPPED
    MediaStatus.NONE, null -> 0.0
}

/**
 * 7. Преобразование оценки пользователя (1-10)
 */
fun calculateRatingWeight(userRating: Int?): Double {
    if (userRating == null || userRating !in 1..10) return 0.0
    return ((userRating - 5.5) / 4.5) * ExplicitFeedbackConstants.RATING_MAX_WEIGHT
}

/**
 * 8. Расчет прогресса просмотра (Piecewise-функция)
 */
fun calculateWatchProgressWeightFromRatio(p: Double): Double {
    val clamped = p.coerceIn(0.0, 1.0)
    return when {
        clamped < ExplicitFeedbackConstants.PROGRESS_BOUNCE_THRESHOLD -> {
            -ExplicitFeedbackConstants.PROGRESS_BOUNCE_PENALTY *
                    (1.0 - (clamped / ExplicitFeedbackConstants.PROGRESS_BOUNCE_THRESHOLD))
        }

        clamped < ExplicitFeedbackConstants.PROGRESS_PLATEAU_THRESHOLD -> {
            val range =
                ExplicitFeedbackConstants.PROGRESS_PLATEAU_THRESHOLD - ExplicitFeedbackConstants.PROGRESS_BOUNCE_THRESHOLD
            ExplicitFeedbackConstants.PROGRESS_MAX_WEIGHT *
                    ((clamped - ExplicitFeedbackConstants.PROGRESS_BOUNCE_THRESHOLD) / range)
        }

        else -> {
            ExplicitFeedbackConstants.PROGRESS_MAX_WEIGHT
        }
    }
}

fun calculateWatchProgressWeight(progressSeconds: Long, durationSeconds: Long): Double {
    if (durationSeconds <= 0) return 0.0
    val p = (progressSeconds.toDouble() / durationSeconds.toDouble())
    return calculateWatchProgressWeightFromRatio(p)
}

/**
 * 9. Оценка наличия в коллекции
 */
fun getCollectionWeight(inCollection: Boolean): Double {
    return if (inCollection) ExplicitFeedbackConstants.WEIGHT_COLLECTION else 0.0
}
