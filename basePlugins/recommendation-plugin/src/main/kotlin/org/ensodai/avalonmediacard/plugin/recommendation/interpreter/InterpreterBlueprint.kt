package org.ensodai.avalonmediacard.plugin.recommendation.interpreter

/**
 * Единый интерфейс для всех 36 шаблонов интерпретатора.
 */
interface InterpreterBlueprint {
    val blueprintId: String

    /**
     * Пытается сгенерировать полку на основе текущего вектора и контекста сессии.
     * Возвращает null, если контекст не подходит (например, не то время суток)
     * или у пользователя нет достаточно сильных весов для этого шаблона.
     */
    fun evaluate(context: InterpreterContext): SemanticSegment?

    /**
     * Универсальная формула скоринга шаблона (Из п. 4.1 Исследования 9)
     */
    fun calculateRelevance(
        baseAffinity: Double,
        temporalMultiplier: Double = 1.0,
        serendipityMultiplier: Double = 1.0,
        saturationPenalty: Double = 1.0
    ): Double {
        val raw = baseAffinity * temporalMultiplier * serendipityMultiplier * saturationPenalty
        // Смещенная сигмоида для нормализации в 0.0 .. 1.0
        return (raw / (raw + 0.5)).coerceIn(0.0, 1.0)
    }
}
