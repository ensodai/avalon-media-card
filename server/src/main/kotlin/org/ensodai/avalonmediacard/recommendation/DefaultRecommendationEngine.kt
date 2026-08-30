package org.ensodai.avalonmediacard.recommendation

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.AffinityVector
import org.ensodai.avalonmediacard.contract.model.ClickstreamTargetType
import org.ensodai.avalonmediacard.contract.model.DynamicSection
import org.ensodai.avalonmediacard.contract.model.SectionType
import org.ensodai.avalonmediacard.contract.plugins.AffinityVectorStore
import org.ensodai.avalonmediacard.contract.plugins.GenreDictionaryProvider
import org.ensodai.avalonmediacard.contract.plugins.RecommendationEngine
import org.ensodai.avalonmediacard.database.UserClickstreamTable
import org.ensodai.avalonmediacard.database.dbQuery
import org.ensodai.avalonmediacard.repository.UserSettingsRepository
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single(binds = [DefaultRecommendationEngine::class])
class DefaultRecommendationEngine(
    private val affinityStore: AffinityVectorStore,
    private val genreDictionaryProvider: GenreDictionaryProvider,
    private val userSettingsRepository: UserSettingsRepository
) : RecommendationEngine {

    override suspend fun getAffinityVector(userId: Uuid): AffinityVector {
        val cached = affinityStore.getVector(userId)
        if (cached != null) return cached

        return dbQuery {
            val recentGenres = UserClickstreamTable
                .selectAll()
                .where { (UserClickstreamTable.userId eq userId) and (UserClickstreamTable.targetType eq ClickstreamTargetType.GENRE) }
                .orderBy(UserClickstreamTable.createdAt to SortOrder.DESC)
                .limit(10)
                .mapNotNull { it[UserClickstreamTable.targetId] }
                .distinct()

            val genreMap = recentGenres.associateWith { 0.5 }
            AffinityVector(genreWeights = genreMap)
        }
    }

    override suspend fun generateDashboard(userId: Uuid, language: String): List<DynamicSection> {
        val locale = language.ifBlank { userSettingsRepository.getUserLocale(userId) }
        val isEn = locale.startsWith("en", ignoreCase = true)
        val vector = getAffinityVector(userId)
        val genres = genreDictionaryProvider.getLocalizedGenres(locale)

        val sections = mutableListOf<DynamicSection>()

        sections.add(
            DynamicSection(
                id = "DEFAULT_HERO",
                title = if (isEn) "Popular Now" else "Популярное сейчас",
                type = SectionType.HERO,
                targetType = EntityType.MOVIE,
                weight = 100.0,
                queryParams = mapOf("sort_by" to "popularity.desc")
            )
        )

        var weightCounter = 90.0
        val topGenres = vector.genreWeights.entries.sortedByDescending { it.value }.take(3)
        for ((genreId, _) in topGenres) {
            val fallbackGenre = if (isEn) "Genre $genreId" else "Жанр $genreId"
            val genreName = genres[genreId] ?: fallbackGenre
            val title = if (isEn) "Because you like $genreName" else "Потому что вы любите $genreName"
            sections.add(
                DynamicSection(
                    id = "GENRE_$genreId",
                    title = title,
                    type = SectionType.CAROUSEL_POSTERS,
                    targetType = EntityType.MOVIE,
                    weight = weightCounter,
                    queryParams = mapOf(
                        "with_genres" to genreId,
                        "sort_by" to "popularity.desc"
                    )
                )
            )
            weightCounter -= 10.0
        }

        sections.add(
            DynamicSection(
                id = "EXPLORE_NEW",
                title = if (isEn) "Time for Discovery" else "Время открытий",
                type = SectionType.EXPLORATION,
                targetType = EntityType.MOVIE,
                weight = 50.0,
                queryParams = mapOf(
                    "sort_by" to "vote_average.desc",
                    "vote_count.gte" to "1000"
                )
            )
        )

        return sections.sortedByDescending { it.weight }
    }

    override suspend fun generateTab(userId: Uuid, scope: String, language: String): List<DynamicSection> {
        val locale = language.ifBlank { userSettingsRepository.getUserLocale(userId) }
        val isEn = locale.startsWith("en", ignoreCase = true)

        val targetType = when (scope.lowercase()) {
            "tv_shows", "tvshows", "tv" -> EntityType.TV
            else -> EntityType.MOVIE
        }

        val titlePrefix = when (scope.lowercase()) {
            "movies", "movie" -> if (isEn) "Popular Movies" else "Популярные фильмы"
            "tv_shows", "tvshows", "tv" -> if (isEn) "Popular TV Shows" else "Популярные сериалы"
            "trends", "trend" -> if (isEn) "Trending This Week" else "Тренды недели"
            else -> if (isEn) "Popular" else "Популярное"
        }

        return listOf(
            DynamicSection(
                id = "DEFAULT_${scope.uppercase()}_HERO",
                title = titlePrefix,
                type = SectionType.HERO,
                targetType = targetType,
                weight = 100.0,
                queryParams = mapOf("sort_by" to "popularity.desc")
            ),
            DynamicSection(
                id = "DEFAULT_${scope.uppercase()}_CAROUSEL",
                title = if (isEn) "Audience Choice" else "Выбор зрителей",
                type = SectionType.CAROUSEL_POSTERS,
                targetType = targetType,
                weight = 90.0,
                queryParams = mapOf("sort_by" to "vote_average.desc", "vote_count.gte" to "500")
            ),
            DynamicSection(
                id = "DEFAULT_${scope.uppercase()}_EXPLORE",
                title = if (isEn) "Explore Section" else "Исследовать раздел",
                type = SectionType.EXPLORATION,
                targetType = targetType,
                weight = 80.0,
                queryParams = mapOf("sort_by" to "popularity.desc")
            )
        )
    }
}
