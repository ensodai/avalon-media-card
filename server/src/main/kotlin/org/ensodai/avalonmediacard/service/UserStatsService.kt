package org.ensodai.avalonmediacard.service

import org.ensodai.avalonmediacard.contract.model.DecadeStatDto
import org.ensodai.avalonmediacard.contract.model.GenreStatDto
import org.ensodai.avalonmediacard.contract.model.TimeMilestoneDto
import org.ensodai.avalonmediacard.contract.model.TimeMilestoneType
import org.ensodai.avalonmediacard.contract.model.TimeStatsDto
import org.ensodai.avalonmediacard.contract.model.UserStatsDto
import org.ensodai.avalonmediacard.repository.UserSettingsRepository
import org.ensodai.avalonmediacard.repository.UserStatsRepository
import org.koin.core.annotation.Single
import kotlin.uuid.Uuid

@Single
class UserStatsService(
    private val userStatsRepository: UserStatsRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val archetypeClassifier: CinematicArchetypeClassifier
) {

    suspend fun getUserStats(userId: Uuid): UserStatsDto {
        val settings = userSettingsRepository.getUserSettings(userId)
        val locale = settings.uiLocale.takeIf { it.isNotBlank() } ?: "ru"

        // 1. Время (Time Overview)
        val (moviesMinutes, showsMinutes) = userStatsRepository.getTimeStats(userId)
        val totalMinutes = moviesMinutes + showsMinutes
        val milestone = calculateTimeMilestone(totalMinutes)
        val timeOverview = TimeStatsDto(
            totalMinutes = totalMinutes,
            moviesMinutes = moviesMinutes,
            showsMinutes = showsMinutes,
            milestone = milestone
        )

        // 2. Библиотека (Library Overview)
        val libraryOverview = userStatsRepository.getLibraryStats(userId)

        // 3. Список просмотренных медиа
        val watchedMediaIds = userStatsRepository.getWatchedMediaIds(userId)

        // 4. Оценки (Rating Distribution)
        val ratingDistribution = userStatsRepository.getRatingDistribution(userId)

        // 5. Жанры (Genre Breakdown)
        val rawGenres = userStatsRepository.getMediaGenres(watchedMediaIds, locale)
        val genreCounts = rawGenres.groupingBy { it }.eachCount()
        val totalGenreHits = genreCounts.values.sum().coerceAtLeast(1)

        val genreBreakdown = genreCounts.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { (pair, count) ->
                val (genreId, genreName) = pair
                GenreStatDto(
                    genreId = genreId,
                    genreName = genreName,
                    watchedCount = count,
                    percentage = (count.toFloat() / totalGenreHits) * 100f
                )
            }

        // 6. Десятилетия (Decades Breakdown)
        val releaseYears = userStatsRepository.getMediaReleaseYears(watchedMediaIds)
        val decadesBreakdown = calculateDecades(releaseYears)

        // 7. Архетип личности (Cinematic Archetype)
        val archetype = archetypeClassifier.classify(genreBreakdown, decadesBreakdown)

        // 8. Топ персон (Top People: Режиссеры и актеры)
        val ratingsMap = userStatsRepository.getUserRatingsMap(userId)
        val topPeople = userStatsRepository.getTopPeople(watchedMediaIds, locale, ratingsMap)

        return UserStatsDto(
            timeOverview = timeOverview,
            libraryOverview = libraryOverview,
            genreBreakdown = genreBreakdown,
            ratingDistribution = ratingDistribution,
            topPeople = topPeople,
            archetype = archetype,
            decadesBreakdown = decadesBreakdown
        )
    }

    private fun calculateTimeMilestone(totalMinutes: Long): TimeMilestoneDto {
        val hours = totalMinutes / 60
        return when {
            hours < 5 -> TimeMilestoneDto(TimeMilestoneType.FIRST_STEPS, 0)
            hours < 30 -> {
                val lotrCount = (hours / 11).toInt().coerceAtLeast(1)
                TimeMilestoneDto(TimeMilestoneType.LOTR_MARATHON, lotrCount)
            }
            hours < 100 -> TimeMilestoneDto(TimeMilestoneType.APOLLO_11, 72)
            hours < 300 -> TimeMilestoneDto(TimeMilestoneType.ORBITAL_MISSION, 0)
            else -> {
                val days = (hours / 24).toInt()
                TimeMilestoneDto(TimeMilestoneType.ARCHIVIST, days)
            }
        }
    }

    private fun calculateDecades(years: List<Int>): List<DecadeStatDto> {
        val decadeRanges = listOf(
            0 to { y: Int -> y < 1970 },
            1970 to { y: Int -> y in 1970..1979 },
            1980 to { y: Int -> y in 1980..1989 },
            1990 to { y: Int -> y in 1990..1999 },
            2000 to { y: Int -> y in 2000..2009 },
            2010 to { y: Int -> y in 2010..2019 },
            2020 to { y: Int -> y >= 2020 }
        )

        return decadeRanges.map { (startYear, predicate) ->
            DecadeStatDto(startYear = startYear, watchedCount = years.count(predicate))
        }
    }
}
