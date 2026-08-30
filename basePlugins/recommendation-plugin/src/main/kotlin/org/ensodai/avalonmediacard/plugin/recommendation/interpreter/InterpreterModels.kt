package org.ensodai.avalonmediacard.plugin.recommendation.interpreter

import org.ensodai.avalonmediacard.contract.i18n.EmptyPluginI18n
import org.ensodai.avalonmediacard.contract.i18n.PluginI18n
import org.ensodai.avalonmediacard.contract.model.AffinityVector
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.SectionType
import org.ensodai.avalonmediacard.plugin.recommendation.RecommendationI18nHelper

/**
 * Контекст интерпретации. Содержит все необходимые данные для шаблонов.
 */
data class InterpreterContext(
    val affinityVector: AffinityVector, // Настоящий вектор из контракта
    val topGenres: List<String>,            // Для быстрого доступа к топам
    val topKeywords: List<String>,
    val localHour: Int,                     // Локальный час юзера (от 0 до 23)
    val localizedGenres: Map<String, String>, // Локализованные жанры из ядра
    val isWeekend: Boolean,
    val lastWatchedTitle: String? = null,
    val topActorName: String? = null,
    val topDirectorName: String? = null,
    val topCompanyName: String? = null,
    val continueWatchingKeys: List<MediaKey> = emptyList(),
    val i18n: PluginI18n = RecommendationI18nHelper.load(),
    val locale: String = "ru"
)

data class SemanticSegment(
    val blueprintId: String,
    val displayTitle: String,
    val displaySubtitle: String,
    val targetType: EntityType,
    val queryParams: Map<String, String>,
    val relevanceScore: Double,
    val visualLayout: SectionType,
    val mediaIds: List<MediaKey> = emptyList()
)
