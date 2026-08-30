package org.ensodai.avalonmediacard.plugin.recommendation.interpreter

import org.ensodai.avalonmediacard.contract.i18n.currentPluginLocale
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.AffinityVector
import org.ensodai.avalonmediacard.contract.model.DynamicSection
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.plugin.recommendation.RecommendationI18nHelper
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.blueprints.MovieTabBlueprints
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.blueprints.TrendTabBlueprints
import org.ensodai.avalonmediacard.plugin.recommendation.interpreter.blueprints.TvShowTabBlueprints
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class SidebarTabScope {
    MOVIES,
    TV_SHOWS,
    TRENDS
}

/**
 * Специализированный генератор макетов для выделенных вкладок сайдбара.
 * Производит вычисление, трансляцию жанров, ранжирование атрибутов и дедупликацию.
 */
@OptIn(ExperimentalUuidApi::class)
class SideTabGenerator(
    private val pluginContext: PluginContext,
    private val movieBlueprints: List<InterpreterBlueprint> = MovieTabBlueprints,
    private val tvBlueprints: List<InterpreterBlueprint> = TvShowTabBlueprints,
    private val trendBlueprints: List<InterpreterBlueprint> = TrendTabBlueprints
) {

    suspend fun generateTab(userId: Uuid, scope: SidebarTabScope, language: String = "ru"): List<DynamicSection> {
        val vector = try {
            pluginContext.affinityStore.getVector(userId) ?: AffinityVector()
        } catch (e: Exception) {
            pluginContext.logger.error("Failed to read affinity vector for tab $scope, using empty", e)
            AffinityVector()
        }

        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val secondsSinceEpoch = nowMillis / 1000
        val daysSinceEpoch = secondsSinceEpoch / 86400
        val dayOfWeekIndex = ((daysSinceEpoch + 3) % 7).toInt()
        val isWeekend = dayOfWeekIndex == 5 || dayOfWeekIndex == 6
        val hour = (((secondsSinceEpoch % 86400) / 3600).toInt() + 3) % 24

        val genreMap = try {
            pluginContext.genreDictionary.getLocalizedGenres(language)
        } catch (e: Exception) {
            emptyMap()
        }

        val userMovies = try {
            pluginContext.userMovies.getUserMovies(userId)
        } catch (e: Exception) {
            emptyMap<String, Any>()
            emptyList()
        }

        val targetEntityType = if (scope == SidebarTabScope.TV_SHOWS) EntityType.TV else EntityType.MOVIE
        val continueWatchingItems = userMovies.filter { item ->
            val matchesType =
                if (targetEntityType == EntityType.TV) item.mediaType == MediaType.TV else item.mediaType == MediaType.MOVIE
            matchesType && item.progressSeconds > 0 && item.status != MediaStatus.COMPLETED
        }.sortedByDescending { it.lastWatchedAt }

        val continueWatchingKeys = continueWatchingItems.map { item ->
            MediaKey(
                if (item.catalogId == "tmdb") MediaProvider.Tmdb else MediaProvider.Custom(
                    item.catalogId
                ),
                targetEntityType,
                item.mediaId
            )
        }

        val lastWatchedId = vector.recentWatchedIds.firstOrNull()
        val lastWatchedTitle = if (lastWatchedId != null) {
            try {
                val key = MediaKey(
                    MediaProvider.Tmdb,
                    targetEntityType,
                    lastWatchedId
                )
                pluginContext.catalog.getMediaDetails(key, requireSeasons = false, requireVideos = false, language = language).title
            } catch (e: Exception) {
                null
            }
        } else null

        val topActorId = vector.actorWeights.maxByOrNull { it.value }?.key
        val topActorName = if (topActorId != null) {
            try {
                val key = MediaKey(
                    MediaProvider.Tmdb,
                    EntityType.MOVIE,
                    topActorId
                )
                pluginContext.catalog.getPersonDetails(key, language = language).name
            } catch (e: Exception) {
                null
            }
        } else null

        val topDirectorId = vector.directorWeights.maxByOrNull { it.value }?.key
        val topDirectorName = if (topDirectorId != null) {
            try {
                val key = MediaKey(
                    MediaProvider.Tmdb,
                    EntityType.MOVIE,
                    topDirectorId
                )
                pluginContext.catalog.getPersonDetails(key, language = language).name
            } catch (e: Exception) {
                null
            }
        } else null

        val effectiveI18n = if (pluginContext.i18n is org.ensodai.avalonmediacard.contract.i18n.EmptyPluginI18n) {
            RecommendationI18nHelper.load()
        } else {
            pluginContext.i18n
        }

        val context = InterpreterContext(
            affinityVector = vector,
            topGenres = vector.genreWeights.entries.sortedByDescending { it.value }.map { it.key },
            topKeywords = vector.keywordWeights.entries.sortedByDescending { it.value }.map { it.key },
            localHour = hour,
            localizedGenres = genreMap,
            isWeekend = isWeekend,
            lastWatchedTitle = lastWatchedTitle,
            topActorName = topActorName,
            topDirectorName = topDirectorName,
            continueWatchingKeys = continueWatchingKeys,
            i18n = effectiveI18n,
            locale = language
        )

        val activeBlueprints = when (scope) {
            SidebarTabScope.MOVIES -> movieBlueprints
            SidebarTabScope.TV_SHOWS -> tvBlueprints
            SidebarTabScope.TRENDS -> trendBlueprints
        }

        val evaluatedSegments = mutableListOf<SemanticSegment>()
        for (blueprint in activeBlueprints) {
            val segment = blueprint.evaluate(context)
            if (segment != null) {
                evaluatedSegments.add(segment)
            }
        }

        // Ранжирование атрибутов на основе вычисленной релевантности
        evaluatedSegments.sortByDescending { it.relevanceScore }

        val orchestratedSections = mutableListOf<DynamicSection>()
        val seenGenres = mutableSetOf<String>()
        val seenCompanies = mutableSetOf<String>()

        for ((index, segment) in evaluatedSegments.withIndex()) {
            val genreParam = segment.queryParams["with_genres"]
            val companyParam = segment.queryParams["with_companies"]

            var isDuplicate = false
            if (genreParam != null && seenGenres.contains(genreParam)) isDuplicate = true
            if (companyParam != null && seenCompanies.contains(companyParam)) isDuplicate = true

            // Исключение дедупликации для сверхважных персональных блоков
            if (segment.relevanceScore >= 1.0) isDuplicate = false

            if (!isDuplicate) {
                orchestratedSections.add(
                    DynamicSection(
                        id = "${segment.blueprintId.lowercase()}_${scope.name.lowercase()}_$index",
                        title = segment.displayTitle,
                        description = segment.displaySubtitle,
                        type = segment.visualLayout,
                        targetType = segment.targetType,
                        weight = segment.relevanceScore,
                        queryParams = segment.queryParams,
                        mediaIds = segment.mediaIds
                    )
                )

                if (genreParam != null) seenGenres.add(genreParam)
                if (companyParam != null) seenCompanies.add(companyParam)
            }

            // Жесткое когнитивное ограничение в 15 строк для предотвращения перегрузки
            if (orchestratedSections.size >= 15) break
        }

        return orchestratedSections
    }
}
