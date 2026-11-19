package org.ensodai.avalonmediacard.repository

import org.ensodai.avalonmediacard.contract.model.CriticProfileType
import org.ensodai.avalonmediacard.contract.model.LibraryStatsDto
import org.ensodai.avalonmediacard.contract.model.MediaImageType
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.PersonStatDto
import org.ensodai.avalonmediacard.contract.model.RatingDistributionDto
import org.ensodai.avalonmediacard.contract.model.TopPeopleStatsDto
import org.ensodai.avalonmediacard.database.MediaCreditTable
import org.ensodai.avalonmediacard.database.MediaCreditTranslationTable
import org.ensodai.avalonmediacard.database.MediaEpisodeTable
import org.ensodai.avalonmediacard.database.MediaGenreDictionaryTable
import org.ensodai.avalonmediacard.database.MediaGenreTable
import org.ensodai.avalonmediacard.database.MediaImageTable
import org.ensodai.avalonmediacard.database.MediaPersonTable
import org.ensodai.avalonmediacard.database.MediaPersonTranslationTable
import org.ensodai.avalonmediacard.database.MediaSeasonTable
import org.ensodai.avalonmediacard.database.MediaTable
import org.ensodai.avalonmediacard.database.UserEpisodeTable
import org.ensodai.avalonmediacard.database.UserMovieTable
import org.ensodai.avalonmediacard.database.dbQuery
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.koin.core.annotation.Single
import kotlin.math.roundToLong
import kotlin.uuid.Uuid

@Single
class UserStatsRepository {

    private companion object {
        const val SQL_CHUNK_SIZE = 400
        val SELF_ROLE_NAMES = setOf(
            "self", "himself", "herself", "themselves",
            "сам себя", "сама себя", "в роли самого себя", "в роли самой себя"
        )
    }

    /**
     * Возвращает экранное время в минутах: (moviesMinutes, showsMinutes).
     */
    suspend fun getTimeStats(userId: Uuid): Pair<Long, Long> = dbQuery {
        val moviesSec = UserMovieTable
            .select(UserMovieTable.status, UserMovieTable.progressSeconds, UserMovieTable.durationSeconds)
            .where { (UserMovieTable.userId eq userId) and (UserMovieTable.mediaType eq MediaType.MOVIE) }
            .fold(0L) { acc, row ->
                val status = row[UserMovieTable.status]
                val progress = row[UserMovieTable.progressSeconds]
                val duration = row[UserMovieTable.durationSeconds]
                val itemSec = if (status == MediaStatus.COMPLETED) {
                    maxOf(duration, progress)
                } else {
                    progress
                }
                acc + itemSec
            }

        val showsSec = UserEpisodeTable
            .select(UserEpisodeTable.isWatched, UserEpisodeTable.progressSeconds, UserEpisodeTable.durationSeconds)
            .where { UserEpisodeTable.userId eq userId }
            .fold(0L) { acc, row ->
                val isWatched = row[UserEpisodeTable.isWatched]
                val progress = row[UserEpisodeTable.progressSeconds]
                val duration = row[UserEpisodeTable.durationSeconds]
                val itemSec = if (isWatched) {
                    maxOf(duration, progress)
                } else {
                    progress
                }
                acc + itemSec
            }

        Pair(moviesSec / 60, showsSec / 60)
    }

    /**
     * Возвращает агрегированную статистику библиотеки пользователя.
     */
    suspend fun getLibraryStats(userId: Uuid): LibraryStatsDto = dbQuery {
        val totalMoviesCount = UserMovieTable
            .selectAll()
            .where {
                (UserMovieTable.userId eq userId) and
                (UserMovieTable.mediaType eq MediaType.MOVIE) and
                ((UserMovieTable.status eq MediaStatus.COMPLETED) or (UserMovieTable.progressSeconds greaterEq 60L))
            }
            .count()
            .toInt()

        val totalEpisodesCount = UserEpisodeTable
            .selectAll()
            .where { (UserEpisodeTable.userId eq userId) and (UserEpisodeTable.isWatched eq true) }
            .count()
            .toInt()

        val tvRows = UserMovieTable
            .select(UserMovieTable.mediaId, UserMovieTable.status)
            .where { (UserMovieTable.userId eq userId) and (UserMovieTable.mediaType eq MediaType.TV) }
            .toList()

        val watchingShows = tvRows.count { it[UserMovieTable.status] == MediaStatus.WATCHING }
        val completedShows = tvRows.count { it[UserMovieTable.status] == MediaStatus.COMPLETED }
        val onHoldShows = tvRows.count { it[UserMovieTable.status] == MediaStatus.PLANNED }
        val dnfShows = tvRows.count { it[UserMovieTable.status] == MediaStatus.DROPPED }

        val tvMediaIdsFromMovies = tvRows.map { it[UserMovieTable.mediaId].value }.toSet()

        val tvMediaIdsFromEpisodes = (UserEpisodeTable innerJoin MediaEpisodeTable innerJoin MediaSeasonTable)
            .select(MediaSeasonTable.mediaId)
            .where { (UserEpisodeTable.userId eq userId) and (UserEpisodeTable.isWatched eq true) }
            .withDistinct(true)
            .map { it[MediaSeasonTable.mediaId].value }
            .toSet()

        val totalShowsCount = (tvMediaIdsFromMovies + tvMediaIdsFromEpisodes).size

        LibraryStatsDto(
            totalMoviesCount = totalMoviesCount,
            totalEpisodesCount = totalEpisodesCount,
            totalShowsCount = totalShowsCount,
            watchingShowsCount = watchingShows,
            completedShowsCount = completedShows,
            onHoldShowsCount = onHoldShows,
            dnfShowsCount = dnfShows
        )
    }

    /**
     * Возвращает список всех просмотренных mediaId (фильмов и сериалов) для анализа жанров, персон и эпох.
     */
    suspend fun getWatchedMediaIds(userId: Uuid): Set<Uuid> = dbQuery {
        val movieMediaIds = UserMovieTable
            .select(UserMovieTable.mediaId)
            .where {
                (UserMovieTable.userId eq userId) and
                ((UserMovieTable.status eq MediaStatus.COMPLETED) or (UserMovieTable.progressSeconds greaterEq 60L))
            }
            .map { it[UserMovieTable.mediaId].value }
            .toSet()

        val showMediaIdsFromEpisodes = (UserEpisodeTable innerJoin MediaEpisodeTable innerJoin MediaSeasonTable)
            .select(MediaSeasonTable.mediaId)
            .where { (UserEpisodeTable.userId eq userId) and (UserEpisodeTable.isWatched eq true) }
            .withDistinct(true)
            .map { it[MediaSeasonTable.mediaId].value }
            .toSet()

        movieMediaIds + showMediaIdsFromEpisodes
    }

    /**
     * Возвращает карту пользовательских оценок по mediaId.
     */
    suspend fun getUserRatingsMap(userId: Uuid): Map<Uuid, Int> = dbQuery {
        UserMovieTable
            .select(UserMovieTable.mediaId, UserMovieTable.userRating)
            .where { (UserMovieTable.userId eq userId) and UserMovieTable.userRating.isNotNull() }
            .associate { it[UserMovieTable.mediaId].value to it[UserMovieTable.userRating]!! }
    }

    /**
     * Возвращает распределение оценок от 1 до 10 и средний балл.
     */
    suspend fun getRatingDistribution(userId: Uuid): RatingDistributionDto = dbQuery {
        val movieRatings = UserMovieTable
            .select(UserMovieTable.userRating)
            .where { (UserMovieTable.userId eq userId) and UserMovieTable.userRating.isNotNull() }
            .mapNotNull { it[UserMovieTable.userRating] }
            .filter { it in 1..10 }

        val episodeRatings = UserEpisodeTable
            .select(UserEpisodeTable.userRating)
            .where { (UserEpisodeTable.userId eq userId) and UserEpisodeTable.userRating.isNotNull() }
            .mapNotNull { it[UserEpisodeTable.userRating] }
            .filter { it in 1..10 }

        val allRatings = movieRatings + episodeRatings
        val distMap = (1..10).associateWith { 0 }.toMutableMap()
        for (r in allRatings) {
            distMap[r] = (distMap[r] ?: 0) + 1
        }

        val avg = if (allRatings.isNotEmpty()) {
            ((allRatings.average() * 10.0).roundToLong()) / 10.0
        } else null

        val criticProfile = when {
            avg == null -> CriticProfileType.NO_RATINGS
            avg >= 8.0 -> CriticProfileType.ENTHUSIAST
            avg >= 7.0 -> CriticProfileType.GENEROUS
            avg >= 6.0 -> CriticProfileType.BALANCED
            else -> CriticProfileType.STRICT
        }

        RatingDistributionDto(
            averageRating = avg,
            totalRatedCount = allRatings.size,
            distribution = distMap,
            criticProfile = criticProfile
        )
    }

    /**
     * Возвращает список пар (genreId, genreName) для просмотренных тайтлов.
     */
    suspend fun getMediaGenres(mediaIds: Set<Uuid>, locale: String): List<Pair<Int, String>> = dbQuery {
        if (mediaIds.isEmpty()) return@dbQuery emptyList()

        // 1. Выбираем все genreId для просмотренных медиа
        val genreIds = mutableListOf<Int>()
        for (chunk in mediaIds.chunked(SQL_CHUNK_SIZE)) {
            val chunkGenreIds = MediaGenreTable
                .select(MediaGenreTable.genreId)
                .where { MediaGenreTable.mediaId inList chunk }
                .map { it[MediaGenreTable.genreId] }
            genreIds.addAll(chunkGenreIds)
        }

        if (genreIds.isEmpty()) return@dbQuery emptyList()

        val uniqueIds = genreIds.distinct()

        // 2. Получаем названия жанров из справочника по локали
        val namesMap = MediaGenreDictionaryTable
            .select(MediaGenreDictionaryTable.genreId, MediaGenreDictionaryTable.name)
            .where {
                (MediaGenreDictionaryTable.genreId inList uniqueIds) and
                (MediaGenreDictionaryTable.languageCode eq locale)
            }
            .associate { it[MediaGenreDictionaryTable.genreId] to it[MediaGenreDictionaryTable.name] }
            .toMutableMap()

        val missingIds = uniqueIds - namesMap.keys
        if (missingIds.isNotEmpty()) {
            val fallbackLang = if (locale != "ru") "ru" else "en"
            val fallbackNames = MediaGenreDictionaryTable
                .select(MediaGenreDictionaryTable.genreId, MediaGenreDictionaryTable.name)
                .where {
                    (MediaGenreDictionaryTable.genreId inList missingIds) and
                    (MediaGenreDictionaryTable.languageCode eq fallbackLang)
                }
                .associate { it[MediaGenreDictionaryTable.genreId] to it[MediaGenreDictionaryTable.name] }
            namesMap.putAll(fallbackNames)
        }

        // 3. Собираем пары (genreId, genreName) с сохранением частотности
        genreIds.mapNotNull { gid ->
            val name = namesMap[gid] ?: return@mapNotNull null
            gid to name
        }
    }

    /**
     * Возвращает годы релиза для просмотренных медиа.
     */
    suspend fun getMediaReleaseYears(mediaIds: Set<Uuid>): List<Int> = dbQuery {
        if (mediaIds.isEmpty()) return@dbQuery emptyList()

        val years = mutableListOf<Int>()
        for (chunk in mediaIds.chunked(SQL_CHUNK_SIZE)) {
            val chunkYears = MediaTable
                .select(MediaTable.releaseYear)
                .where { (MediaTable.id inList chunk) and MediaTable.releaseYear.isNotNull() }
                .mapNotNull { it[MediaTable.releaseYear] }
            years.addAll(chunkYears)
        }
        years
    }

    /**
     * Возвращает лидерборд режиссеров и актеров с умной фильтрацией:
     * - Исключение камео «Self» / «В роли самого себя»
     * - Защита от «одного хита» (минимум 2 работы, если библиотека насчитывает > 10 тайтлов)
     * - Сортировка по количеству просмотров, затем по среднему баллу
     */
    suspend fun getTopPeople(
        mediaIds: Set<Uuid>,
        locale: String,
        movieRatingMap: Map<Uuid, Int>
    ): TopPeopleStatsDto = dbQuery {
        if (mediaIds.isEmpty()) return@dbQuery TopPeopleStatsDto(emptyList(), emptyList())

        // 1. Извлекаем титры с ID людей и ролями
        data class RawCredit(
            val creditId: Uuid,
            val mediaId: Uuid,
            val personUuid: Uuid,
            val tmdbPersonId: String,
            val role: String
        )

        val rawCredits = mutableListOf<RawCredit>()
        for (chunk in mediaIds.chunked(SQL_CHUNK_SIZE)) {
            val chunkCredits = (MediaCreditTable innerJoin MediaPersonTable)
                .select(
                    MediaCreditTable.id,
                    MediaCreditTable.mediaId,
                    MediaCreditTable.role,
                    MediaPersonTable.id,
                    MediaPersonTable.personId
                )
                .where { MediaCreditTable.mediaId inList chunk }
                .map { row ->
                    RawCredit(
                        creditId = row[MediaCreditTable.id].value,
                        mediaId = row[MediaCreditTable.mediaId].value,
                        personUuid = row[MediaPersonTable.id].value,
                        tmdbPersonId = row[MediaPersonTable.personId],
                        role = row[MediaCreditTable.role]
                    )
                }
            rawCredits.addAll(chunkCredits)
        }

        if (rawCredits.isEmpty()) return@dbQuery TopPeopleStatsDto(emptyList(), emptyList())

        // 2. Исключаем документальные камео (Self) для актеров
        val actorCreditIds = rawCredits.filter { it.role.equals("Actor", ignoreCase = true) }.map { it.creditId }
        val selfCreditIds = mutableSetOf<Uuid>()
        for (chunk in actorCreditIds.chunked(SQL_CHUNK_SIZE)) {
            MediaCreditTranslationTable
                .select(MediaCreditTranslationTable.creditId, MediaCreditTranslationTable.characterName)
                .where { (MediaCreditTranslationTable.creditId inList chunk) and MediaCreditTranslationTable.characterName.isNotNull() }
                .forEach { row ->
                    val charName = row[MediaCreditTranslationTable.characterName]?.trim()?.lowercase()
                    if (charName != null && SELF_ROLE_NAMES.any { charName.contains(it) }) {
                        selfCreditIds.add(row[MediaCreditTranslationTable.creditId].value)
                    }
                }
        }

        val filteredCredits = rawCredits.filterNot { it.creditId in selfCreditIds }
        val minCountThreshold = if (mediaIds.size > 10) 2 else 1

        // 3. Вычисляем кандидатов в топ режиссеров (топ-5)
        val candidateDirectors = filteredCredits
            .filter { it.role.equals("Director", ignoreCase = true) }
            .groupBy { it.personUuid to it.tmdbPersonId }
            .mapNotNull { (key, credits) ->
                val distinctMedia = credits.map { it.mediaId }.distinct()
                if (distinctMedia.size < minCountThreshold) return@mapNotNull null
                val ratings = distinctMedia.mapNotNull { movieRatingMap[it] }
                val avg = if (ratings.isNotEmpty()) {
                    ((ratings.average() * 10).roundToLong()) / 10.0
                } else null
                Triple(key.first, key.second, distinctMedia.size to avg)
            }
            .sortedWith(
                compareByDescending<Triple<Uuid, String, Pair<Int, Double?>>> { it.third.first }
                    .thenByDescending { it.third.second ?: 0.0 }
            )
            .take(5)

        // 4. Вычисляем кандидатов в топ актеров (топ-10)
        val candidateActors = filteredCredits
            .filter { it.role.equals("Actor", ignoreCase = true) }
            .groupBy { it.personUuid to it.tmdbPersonId }
            .mapNotNull { (key, credits) ->
                val distinctMedia = credits.map { it.mediaId }.distinct()
                if (distinctMedia.size < minCountThreshold) return@mapNotNull null
                val ratings = distinctMedia.mapNotNull { movieRatingMap[it] }
                val avg = if (ratings.isNotEmpty()) {
                    ((ratings.average() * 10).roundToLong()) / 10.0
                } else null
                Triple(key.first, key.second, distinctMedia.size to avg)
            }
            .sortedWith(
                compareByDescending<Triple<Uuid, String, Pair<Int, Double?>>> { it.third.first }
                    .thenByDescending { it.third.second ?: 0.0 }
            )
            .take(10)

        // 5. Загружаем локализованные имена и фото только для отобранных персон (макс. 15!)
        val targetPersonUuids = (candidateDirectors.map { it.first } + candidateActors.map { it.first }).distinct()
        if (targetPersonUuids.isEmpty()) return@dbQuery TopPeopleStatsDto(emptyList(), emptyList())

        val namesMap = mutableMapOf<Uuid, String>()
        for (chunk in targetPersonUuids.chunked(SQL_CHUNK_SIZE)) {
            val transRows = MediaPersonTranslationTable
                .select(MediaPersonTranslationTable.personId, MediaPersonTranslationTable.name)
                .where {
                    (MediaPersonTranslationTable.personId inList chunk) and
                    (MediaPersonTranslationTable.language eq locale)
                }
                .associate { it[MediaPersonTranslationTable.personId].value to it[MediaPersonTranslationTable.name] }
            namesMap.putAll(transRows)

            val missing = chunk.filterNot { namesMap.containsKey(it) }
            if (missing.isNotEmpty()) {
                val fallbackLang = if (locale != "ru") "ru" else "en"
                val fallbackRows = MediaPersonTranslationTable
                    .select(MediaPersonTranslationTable.personId, MediaPersonTranslationTable.name)
                    .where {
                        (MediaPersonTranslationTable.personId inList missing) and
                        (MediaPersonTranslationTable.language eq fallbackLang)
                    }
                    .associate { it[MediaPersonTranslationTable.personId].value to it[MediaPersonTranslationTable.name] }
                namesMap.putAll(fallbackRows)
            }
        }

        val photosMap = mutableMapOf<Uuid, String>()
        for (chunk in targetPersonUuids.chunked(SQL_CHUNK_SIZE)) {
            val imageRows = MediaImageTable
                .select(MediaImageTable.personId, MediaImageTable.url)
                .where {
                    (MediaImageTable.personId inList chunk) and
                    (MediaImageTable.imageType eq MediaImageType.PROFILE)
                }
                .associate { it[MediaImageTable.personId]!!.value to it[MediaImageTable.url] }
            photosMap.putAll(imageRows)
        }

        val directors = candidateDirectors.mapNotNull { (personUuid, tmdbPersonId, stats) ->
            val name = namesMap[personUuid] ?: return@mapNotNull null
            PersonStatDto(
                personId = tmdbPersonId,
                name = name,
                photoUrl = photosMap[personUuid],
                watchedCount = stats.first,
                averageRating = stats.second
            )
        }

        val actors = candidateActors.mapNotNull { (personUuid, tmdbPersonId, stats) ->
            val name = namesMap[personUuid] ?: return@mapNotNull null
            PersonStatDto(
                personId = tmdbPersonId,
                name = name,
                photoUrl = photosMap[personUuid],
                watchedCount = stats.first,
                averageRating = stats.second
            )
        }

        TopPeopleStatsDto(directors = directors, actors = actors)
    }
}
