package org.ensodai.avalonmediacard.service

import org.ensodai.avalonmediacard.contract.model.ActivityHeatmapDto
import org.ensodai.avalonmediacard.contract.model.DayActivityDto
import org.ensodai.avalonmediacard.contract.model.DayActivityItemDto
import org.ensodai.avalonmediacard.contract.model.DecadeStatDto
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.GenreStatDto
import org.ensodai.avalonmediacard.contract.model.MediaCatalog
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaMetadata
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.model.TimeMilestoneDto
import org.ensodai.avalonmediacard.contract.model.TimeMilestoneType
import org.ensodai.avalonmediacard.contract.model.TimeStatsDto
import org.ensodai.avalonmediacard.contract.model.UserStatsDto
import org.ensodai.avalonmediacard.repository.UserSettingsRepository
import org.ensodai.avalonmediacard.repository.UserStatsRepository
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis
import kotlin.uuid.Uuid

@Single
class UserStatsService(
    private val userStatsRepository: UserStatsRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val archetypeClassifier: CinematicArchetypeClassifier,
    private val mediaCatalog: MediaCatalog
) {
    private val logger = LoggerFactory.getLogger(UserStatsService::class.java)

    suspend fun getUserStats(userId: Uuid): UserStatsDto {
        val startTotal = System.currentTimeMillis()
        val settings = userSettingsRepository.getUserSettings(userId)
        val locale = settings.uiLocale.takeIf { it.isNotBlank() } ?: "ru"

        // 1. Время (Time Overview)
        lateinit var timeOverview: TimeStatsDto
        val tTime = measureTimeMillis {
            val (moviesMinutes, showsMinutes) = userStatsRepository.getTimeStats(userId)
            val totalMinutes = moviesMinutes + showsMinutes
            val milestone = calculateTimeMilestone(totalMinutes)
            timeOverview = TimeStatsDto(
                totalMinutes = totalMinutes,
                moviesMinutes = moviesMinutes,
                showsMinutes = showsMinutes,
                milestone = milestone
            )
        }

        // 2. Библиотека (Library Overview)
        lateinit var libraryOverview: org.ensodai.avalonmediacard.contract.model.LibraryStatsDto
        val tLib = measureTimeMillis {
            libraryOverview = userStatsRepository.getLibraryStats(userId)
        }

        // 3. Список просмотренных медиа
        lateinit var watchedMediaIds: Set<Uuid>
        val tWatched = measureTimeMillis {
            watchedMediaIds = userStatsRepository.getWatchedMediaIds(userId)
        }

        // 4. Оценки (Rating Distribution)
        lateinit var ratingDistribution: org.ensodai.avalonmediacard.contract.model.RatingDistributionDto
        val tRatings = measureTimeMillis {
            ratingDistribution = userStatsRepository.getRatingDistribution(userId)
        }

        // 5. Жанры (Genre Breakdown)
        lateinit var genreBreakdown: List<GenreStatDto>
        val tGenres = measureTimeMillis {
            val rawGenres = userStatsRepository.getMediaGenres(watchedMediaIds, locale)
            val genreCounts = rawGenres.groupingBy { it }.eachCount()
            val totalGenreHits = genreCounts.values.sum().coerceAtLeast(1)

            genreBreakdown = genreCounts.entries
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
        }

        // 6. Десятилетия (Decades Breakdown)
        lateinit var decadesBreakdown: List<DecadeStatDto>
        val tYears = measureTimeMillis {
            val releaseYears = userStatsRepository.getMediaReleaseYears(watchedMediaIds)
            decadesBreakdown = calculateDecades(releaseYears)
        }

        // 7. Архетип личности (Cinematic Archetype)
        var archetype: org.ensodai.avalonmediacard.contract.model.ArchetypeDto? = null
        val tArchetype = measureTimeMillis {
            archetype = archetypeClassifier.classify(genreBreakdown, decadesBreakdown)
        }

        // 8. Топ персон (Top People: Режиссеры и актеры)
        lateinit var topPeople: org.ensodai.avalonmediacard.contract.model.TopPeopleStatsDto
        val tPeople = measureTimeMillis {
            val ratingsMap = userStatsRepository.getUserRatingsMap(userId)
            topPeople = userStatsRepository.getTopPeople(watchedMediaIds, locale, ratingsMap)
        }

        // 9. Тепловая карта активности (Activity Heatmap)
        lateinit var activityHeatmap: ActivityHeatmapDto
        val tHeatmap = measureTimeMillis {
            activityHeatmap = userStatsRepository.getActivityHeatmap(userId)
        }

        val totalMs = System.currentTimeMillis() - startTotal
        logger.info("[UserStatsService] Stats for user $userId calculated in ${totalMs}ms (time: ${tTime}ms, lib: ${tLib}ms, watched: ${tWatched}ms, ratings: ${tRatings}ms, genres: ${tGenres}ms, years: ${tYears}ms, archetype: ${tArchetype}ms, people: ${tPeople}ms, heatmap: ${tHeatmap}ms)")

        return UserStatsDto(
            timeOverview = timeOverview,
            libraryOverview = libraryOverview,
            genreBreakdown = genreBreakdown,
            ratingDistribution = ratingDistribution,
            topPeople = topPeople,
            archetype = archetype,
            decadesBreakdown = decadesBreakdown,
            activityHeatmap = activityHeatmap
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

    suspend fun getDayActivity(userId: Uuid, date: String): DayActivityDto {
        val settings = userSettingsRepository.getUserSettings(userId)
        val locale = settings.uiLocale.takeIf { it.isNotBlank() } ?: "ru"
        val rawItems = userStatsRepository.getDayActivityRaw(userId, date, locale)

        if (rawItems.isEmpty()) {
            return DayActivityDto(
                date = date,
                totalMinutes = 0L,
                items = emptyList(),
                averageRating = null
            )
        }

        val keys = rawItems.map { item ->
            val provider = when (item.catalogId?.lowercase()) {
                "tmdb" -> MediaProvider.Tmdb
                "trakt" -> MediaProvider.Trakt
                "youtube" -> MediaProvider.Youtube
                null, "" -> MediaProvider.Tmdb
                else -> MediaProvider.Custom(item.catalogId)
            }
            val entityType = if (item.mediaType.equals("movie", ignoreCase = true)) EntityType.MOVIE else EntityType.TV
            MediaKey(provider, entityType, item.externalId)
        }.distinct()

        val detailsMap: Map<MediaKey, MediaMetadata> = runCatching {
            mediaCatalog.getMediaDetailsBatch(
                keys = keys,
                requireSeasons = false,
                requireVideos = false,
                language = locale
            )
        }.getOrDefault(emptyMap())

        val ratings = mutableListOf<Int>()
        var totalSec = 0L

        val activityItems = rawItems.map { raw ->
            val provider = when (raw.catalogId?.lowercase()) {
                "tmdb" -> MediaProvider.Tmdb
                "trakt" -> MediaProvider.Trakt
                "youtube" -> MediaProvider.Youtube
                null, "" -> MediaProvider.Tmdb
                else -> MediaProvider.Custom(raw.catalogId)
            }
            val entityType = if (raw.mediaType.equals("movie", ignoreCase = true)) EntityType.MOVIE else EntityType.TV
            val key = MediaKey(provider, entityType, raw.externalId)
            val details = detailsMap[key]

            totalSec += raw.watchedSeconds
            raw.userRating?.let { ratings.add(it) }

            DayActivityItemDto(
                mediaKey = key,
                title = details?.title ?: raw.externalId,
                posterUrl = details?.posterUrl,
                isEpisode = raw.isEpisode,
                seasonNumber = raw.seasonNumber,
                episodeNumber = raw.episodeNumber,
                episodeTitle = raw.episodeName,
                watchedMinutes = (raw.watchedSeconds / 60).toInt().coerceAtLeast(1),
                durationMinutes = (raw.durationSeconds / 60).toInt(),
                isCompleted = raw.isCompleted,
                userRating = raw.userRating
            )
        }

        val avgRating = if (ratings.isNotEmpty()) {
            (ratings.average() * 10).roundToInt() / 10.0
        } else null

        return DayActivityDto(
            date = date,
            totalMinutes = totalSec / 60,
            items = activityItems,
            averageRating = avgRating
        )
    }
}
