package org.ensodai.avalonmediacard.plugin.recommendation.interpreter

import org.ensodai.avalonmediacard.contract.model.KeywordMetadata
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.blueprints.MoodKey

/**
 * Семантический классификатор настроений.
 * Сопоставляет числовые ID тегов/ключевых слов TMDB с понятийными строковыми ключами MoodKey.
 */
object SemanticMoodClassifier {
    private val KEYWORD_MOOD_MAP = mapOf(
        "33810" to MoodKey.DARK, "10077" to MoodKey.DARK, "14606" to MoodKey.DARK,
        "9714" to MoodKey.LAUGH, "13130" to MoodKey.JOY, "9799" to MoodKey.JOY,
        "4379" to MoodKey.TENSION, "10292" to MoodKey.TENSION, "12988" to MoodKey.TENSION,
        "4842" to MoodKey.INTELLECTUAL, "9715" to MoodKey.ADRENALINE, "9717" to MoodKey.ADRENALINE
    )

    fun processMoodScores(
        keywords: List<KeywordMetadata>,
        eventScore: Double,
        moodDecay: Double,
        targetMap: MutableMap<String, Double>
    ) {
        for (kw in keywords) {
            val moodKey = KEYWORD_MOOD_MAP[kw.id.toString()] ?: continue
            val currentScore = targetMap[moodKey.value] ?: 0.0
            targetMap[moodKey.value] = currentScore + (eventScore * moodDecay)
        }
    }
}
