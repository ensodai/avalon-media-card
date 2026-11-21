package org.ensodai.avalonmediacard.repository

import org.ensodai.avalonmediacard.contract.model.ActivityHeatmapDto
import org.ensodai.avalonmediacard.contract.model.CriticProfileType
import org.ensodai.avalonmediacard.contract.model.HeatmapDayDto
import org.ensodai.avalonmediacard.contract.model.LibraryStatsDto
import org.ensodai.avalonmediacard.contract.model.MediaImageType
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.PersonStatDto
import org.ensodai.avalonmediacard.contract.model.RatingDistributionDto
import org.ensodai.avalonmediacard.contract.model.TopPeopleStatsDto
import org.ensodai.avalonmediacard.contract.utils.toProxyImageUrl
import org.ensodai.avalonmediacard.database.MediaCreditTable
import org.ensodai.avalonmediacard.database.MediaCreditTranslationTable
import org.ensodai.avalonmediacard.database.MediaEpisodeTable
import org.ensodai.avalonmediacard.database.MediaEpisodeTranslationTable
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
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.koin.core.annotation.Single
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.Instant
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

        val movieRows = UserMovieTable
            .select(UserMovieTable.status, UserMovieTable.progressSeconds)
            .where { (UserMovieTable.userId eq userId) and (UserMovieTable.mediaType eq MediaType.MOVIE) }
            .toList()

        val watchingMovies = movieRows.count { it[UserMovieTable.status] == MediaStatus.WATCHING || (it[UserMovieTable.status] == MediaStatus.NONE && it[UserMovieTable.progressSeconds] >= 60L) }
        val completedMovies = movieRows.count { it[UserMovieTable.status] == MediaStatus.COMPLETED }
        val onHoldMovies = movieRows.count { it[UserMovieTable.status] == MediaStatus.PLANNED }
        val dnfMovies = movieRows.count { it[UserMovieTable.status] == MediaStatus.DROPPED }

        LibraryStatsDto(
            totalMoviesCount = totalMoviesCount,
            totalEpisodesCount = totalEpisodesCount,
            totalShowsCount = totalShowsCount,
            watchingShowsCount = watchingShows,
            completedShowsCount = completedShows,
            onHoldShowsCount = onHoldShows,
            dnfShowsCount = dnfShows,
            watchingMoviesCount = watchingMovies,
            completedMoviesCount = completedMovies,
            onHoldMoviesCount = onHoldMovies,
            dnfMoviesCount = dnfMovies
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

        data class PersonMediaRef(
            val personUuid: Uuid,
            val tmdbPersonId: String,
            val mediaId: Uuid,
            val creditId: Uuid
        )

        // 1. Извлекаем режиссеров напрямую (фильтрация по роли Director в БД)
        val directorRefs = mutableListOf<PersonMediaRef>()
        for (chunk in mediaIds.chunked(SQL_CHUNK_SIZE)) {
            val chunkDirectors = (MediaCreditTable innerJoin MediaPersonTable)
                .select(
                    MediaPersonTable.id,
                    MediaPersonTable.personId,
                    MediaCreditTable.mediaId,
                    MediaCreditTable.id
                )
                .where { (MediaCreditTable.mediaId inList chunk) and (MediaCreditTable.role eq "Director") }
                .map { row ->
                    PersonMediaRef(
                        personUuid = row[MediaPersonTable.id].value,
                        tmdbPersonId = row[MediaPersonTable.personId],
                        mediaId = row[MediaCreditTable.mediaId].value,
                        creditId = row[MediaCreditTable.id].value
                    )
                }
            directorRefs.addAll(chunkDirectors)
        }

        // 2. Извлекаем актеров напрямую (фильтрация по роли Actor в БД)
        val actorRefs = mutableListOf<PersonMediaRef>()
        for (chunk in mediaIds.chunked(SQL_CHUNK_SIZE)) {
            val chunkActors = (MediaCreditTable innerJoin MediaPersonTable)
                .select(
                    MediaPersonTable.id,
                    MediaPersonTable.personId,
                    MediaCreditTable.mediaId,
                    MediaCreditTable.id
                )
                .where { (MediaCreditTable.mediaId inList chunk) and (MediaCreditTable.role eq "Actor") }
                .map { row ->
                    PersonMediaRef(
                        personUuid = row[MediaPersonTable.id].value,
                        tmdbPersonId = row[MediaPersonTable.personId],
                        mediaId = row[MediaCreditTable.mediaId].value,
                        creditId = row[MediaCreditTable.id].value
                    )
                }
            actorRefs.addAll(chunkActors)
        }

        val minCountThreshold = if (mediaIds.size > 10) 2 else 1

        // 3. Вычисляем кандидатов в топ режиссеров (топ-5)
        val candidateDirectors = directorRefs
            .groupBy { it.personUuid to it.tmdbPersonId }
            .mapNotNull { (key, refs) ->
                val distinctMedia = refs.map { it.mediaId }.distinct()
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

        // 4. Предварительный отбор топ-кандидатов актеров (берем до 25 кандидатов для проверки камео)
        val candidateActorsGrouped = actorRefs
            .groupBy { it.personUuid to it.tmdbPersonId }
            .filter { (_, refs) -> refs.map { it.mediaId }.distinct().size >= minCountThreshold }
            .entries
            .sortedByDescending { it.value.map { r -> r.mediaId }.distinct().size }
            .take(25)

        // Исключаем камео (Self) ТОЛЬКО для отобранных кандидатов-актеров
        val candidateCreditIds = candidateActorsGrouped.flatMap { it.value.map { r -> r.creditId } }
        val selfCreditIds = mutableSetOf<Uuid>()
        if (candidateCreditIds.isNotEmpty()) {
            for (chunk in candidateCreditIds.chunked(SQL_CHUNK_SIZE)) {
                MediaCreditTranslationTable
                    .select(MediaCreditTranslationTable.creditId, MediaCreditTranslationTable.characterName)
                    .where {
                        (MediaCreditTranslationTable.creditId inList chunk) and
                        MediaCreditTranslationTable.characterName.isNotNull()
                    }
                    .forEach { row ->
                        val charName = row[MediaCreditTranslationTable.characterName]?.trim()?.lowercase()
                        if (charName != null && SELF_ROLE_NAMES.any { charName.contains(it) }) {
                            selfCreditIds.add(row[MediaCreditTranslationTable.creditId].value)
                        }
                    }
            }
        }

        val candidateActors = candidateActorsGrouped
            .mapNotNull { (key, refs) ->
                val validRefs = refs.filterNot { it.creditId in selfCreditIds }
                val distinctMedia = validRefs.map { it.mediaId }.distinct()
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
                photoUrl = photosMap[personUuid]?.toProxyImageUrl("w185"),
                watchedCount = stats.first,
                averageRating = stats.second
            )
        }

        val actors = candidateActors.mapNotNull { (personUuid, tmdbPersonId, stats) ->
            val name = namesMap[personUuid] ?: return@mapNotNull null
            PersonStatDto(
                personId = tmdbPersonId,
                name = name,
                photoUrl = photosMap[personUuid]?.toProxyImageUrl("w185"),
                watchedCount = stats.first,
                averageRating = stats.second
            )
        }

        TopPeopleStatsDto(directors = directors, actors = actors)
    }

    /**
     * Возвращает тепловую карту активности за последние 52 недели (365+ дней).
     */
    suspend fun getActivityHeatmap(userId: Uuid): ActivityHeatmapDto = dbQuery {
        val now = Clock.System.now()
        val today = java.time.Instant.ofEpochMilli(now.toEpochMilliseconds())
            .atZone(java.time.ZoneOffset.UTC)
            .toLocalDate()

        val daysSinceMonday = today.dayOfWeek.value - 1 // 0 = Mon .. 6 = Sun
        // Отматываем ровно 52 полные недели назад от текущего понедельника, чтобы сетка начиналась с Пн
        val startDate = today.minusDays(52L * 7 + daysSinceMonday)
        val startInstant = Instant.fromEpochMilliseconds(
            startDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        )

        class DayAccumulator(
            var totalSeconds: Long = 0L,
            var count: Int = 0,
            val ratings: MutableList<Int> = mutableListOf()
        )

        val dayMap = mutableMapOf<String, DayAccumulator>()

        // 1. Фильмы
        UserMovieTable
            .select(
                UserMovieTable.lastWatchedAt,
                UserMovieTable.status,
                UserMovieTable.progressSeconds,
                UserMovieTable.durationSeconds,
                UserMovieTable.userRating
            )
            .where {
                (UserMovieTable.userId eq userId) and
                (UserMovieTable.mediaType eq MediaType.MOVIE) and
                (UserMovieTable.lastWatchedAt greaterEq startInstant) and
                ((UserMovieTable.status eq MediaStatus.COMPLETED) or (UserMovieTable.progressSeconds greaterEq 60L))
            }
            .forEach { row ->
                val instant = row[UserMovieTable.lastWatchedAt]
                val dateStr = java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds())
                    .atZone(java.time.ZoneOffset.UTC)
                    .toLocalDate()
                    .toString()

                val durationSec = if (row[UserMovieTable.status] == MediaStatus.COMPLETED) {
                    maxOf(row[UserMovieTable.durationSeconds], row[UserMovieTable.progressSeconds])
                } else {
                    row[UserMovieTable.progressSeconds]
                }

                val acc = dayMap.getOrPut(dateStr) { DayAccumulator() }
                acc.totalSeconds += durationSec
                acc.count += 1
                row[UserMovieTable.userRating]?.let { acc.ratings.add(it) }
            }

        // 2. Сериалы (эпизоды)
        UserEpisodeTable
            .select(
                UserEpisodeTable.lastWatchedAt,
                UserEpisodeTable.isWatched,
                UserEpisodeTable.progressSeconds,
                UserEpisodeTable.durationSeconds,
                UserEpisodeTable.userRating
            )
            .where {
                (UserEpisodeTable.userId eq userId) and
                (UserEpisodeTable.lastWatchedAt greaterEq startInstant) and
                ((UserEpisodeTable.isWatched eq true) or (UserEpisodeTable.progressSeconds greaterEq 60L))
            }
            .forEach { row ->
                val instant = row[UserEpisodeTable.lastWatchedAt]
                val dateStr = java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds())
                    .atZone(java.time.ZoneOffset.UTC)
                    .toLocalDate()
                    .toString()

                val durationSec = if (row[UserEpisodeTable.isWatched]) {
                    maxOf(row[UserEpisodeTable.durationSeconds], row[UserEpisodeTable.progressSeconds])
                } else {
                    row[UserEpisodeTable.progressSeconds]
                }

                val acc = dayMap.getOrPut(dateStr) { DayAccumulator() }
                acc.totalSeconds += durationSec
                acc.count += 1
                row[UserEpisodeTable.userRating]?.let { acc.ratings.add(it) }
            }

        // 3. Формируем непрерывный календарь от startDate до today
        val days = mutableListOf<HeatmapDayDto>()
        var curDate = startDate
        while (!curDate.isAfter(today)) {
            val dateStr = curDate.toString()
            val acc = dayMap[dateStr]
            val minutes = (acc?.totalSeconds ?: 0L) / 60
            val count = acc?.count ?: 0
            val avgRating = if (acc != null && acc.ratings.isNotEmpty()) {
                (acc.ratings.average() * 10).roundToInt() / 10.0
            } else null

            days.add(
                HeatmapDayDto(
                    date = dateStr,
                    minutes = minutes,
                    count = count,
                    avgRating = avgRating
                )
            )
            curDate = curDate.plusDays(1)
        }

        val totalActiveDays = days.count { it.minutes > 0 }

        // Поиск пикового дня
        val peakDay = days.maxByOrNull { it.minutes }
        val peakDayDate = if (peakDay != null && peakDay.minutes > 0) peakDay.date else null
        val peakDayMinutes = peakDay?.minutes ?: 0L

        // Расчет стриков
        var longestStreak = 0
        var currentRunningStreak = 0
        for (day in days) {
            if (day.minutes > 0) {
                currentRunningStreak++
                if (currentRunningStreak > longestStreak) {
                    longestStreak = currentRunningStreak
                }
            } else {
                currentRunningStreak = 0
            }
        }

        // Текущий стрик от сегодняшнего или вчерашнего дня
        val todayStr = today.toString()
        val yesterdayStr = today.minusDays(1).toString()
        val todayIndex = days.indexOfLast { it.date == todayStr }
        var currentStreak = 0

        if (todayIndex >= 0) {
            val startIdx = if (days[todayIndex].minutes > 0) {
                todayIndex
            } else if (todayIndex > 0 && days[todayIndex - 1].date == yesterdayStr && days[todayIndex - 1].minutes > 0) {
                todayIndex - 1
            } else {
                -1
            }

            if (startIdx >= 0) {
                for (i in startIdx downTo 0) {
                    if (days[i].minutes > 0) {
                        currentStreak++
                    } else {
                        break
                    }
                }
            }
        }

        ActivityHeatmapDto(
            days = days,
            totalActiveDays = totalActiveDays,
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak,
            peakDayDate = peakDayDate,
            peakDayMinutes = peakDayMinutes
        )
    }

    suspend fun getDayActivityRaw(userId: Uuid, date: String, locale: String): List<DayActivityRawItem> = dbQuery {
        val localDate = runCatching { java.time.LocalDate.parse(date) }.getOrNull() ?: return@dbQuery emptyList()
        val startInstant = Instant.fromEpochMilliseconds(
            localDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        val endInstant = Instant.fromEpochMilliseconds(
            localDate.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        )

        val movies = (UserMovieTable innerJoin MediaTable)
            .select(
                MediaTable.catalogId,
                MediaTable.externalId,
                MediaTable.mediaType,
                UserMovieTable.status,
                UserMovieTable.progressSeconds,
                UserMovieTable.durationSeconds,
                UserMovieTable.userRating,
                UserMovieTable.lastWatchedAt
            )
            .where {
                (UserMovieTable.userId eq userId) and
                (UserMovieTable.mediaType eq MediaType.MOVIE) and
                (UserMovieTable.lastWatchedAt greaterEq startInstant) and
                (UserMovieTable.lastWatchedAt less endInstant) and
                ((UserMovieTable.status eq MediaStatus.COMPLETED) or (UserMovieTable.progressSeconds greaterEq 60L))
            }
            .map { row ->
                val isCompleted = row[UserMovieTable.status] == MediaStatus.COMPLETED
                val duration = row[UserMovieTable.durationSeconds]
                val progress = row[UserMovieTable.progressSeconds]
                val watchedSec = if (isCompleted) maxOf(duration, progress) else progress
                DayActivityRawItem(
                    catalogId = row[MediaTable.catalogId],
                    externalId = row[MediaTable.externalId],
                    mediaType = row[MediaTable.mediaType],
                    isEpisode = false,
                    seasonNumber = null,
                    episodeNumber = null,
                    episodeName = null,
                    watchedSeconds = watchedSec,
                    durationSeconds = duration,
                    isCompleted = isCompleted,
                    userRating = row[UserMovieTable.userRating],
                    lastWatchedAt = row[UserMovieTable.lastWatchedAt]
                )
            }

        val episodes = (UserEpisodeTable innerJoin MediaEpisodeTable innerJoin MediaSeasonTable innerJoin MediaTable)
            .select(
                MediaTable.catalogId,
                MediaTable.externalId,
                MediaTable.mediaType,
                MediaSeasonTable.seasonNumber,
                MediaEpisodeTable.id,
                MediaEpisodeTable.episodeNumber,
                UserEpisodeTable.isWatched,
                UserEpisodeTable.progressSeconds,
                UserEpisodeTable.durationSeconds,
                UserEpisodeTable.userRating,
                UserEpisodeTable.lastWatchedAt
            )
            .where {
                (UserEpisodeTable.userId eq userId) and
                (UserEpisodeTable.lastWatchedAt greaterEq startInstant) and
                (UserEpisodeTable.lastWatchedAt less endInstant) and
                ((UserEpisodeTable.isWatched eq true) or (UserEpisodeTable.progressSeconds greaterEq 60L))
            }
            .map { row ->
                val isWatched = row[UserEpisodeTable.isWatched]
                val duration = row[UserEpisodeTable.durationSeconds]
                val progress = row[UserEpisodeTable.progressSeconds]
                val watchedSec = if (isWatched) maxOf(duration, progress) else progress
                val epId = row[MediaEpisodeTable.id]

                val epName = MediaEpisodeTranslationTable
                    .select(MediaEpisodeTranslationTable.name)
                    .where {
                        (MediaEpisodeTranslationTable.episodeId eq epId) and
                        (MediaEpisodeTranslationTable.language eq locale)
                    }
                    .mapNotNull { it[MediaEpisodeTranslationTable.name] }
                    .singleOrNull()

                DayActivityRawItem(
                    catalogId = row[MediaTable.catalogId],
                    externalId = row[MediaTable.externalId],
                    mediaType = row[MediaTable.mediaType],
                    isEpisode = true,
                    seasonNumber = row[MediaSeasonTable.seasonNumber],
                    episodeNumber = row[MediaEpisodeTable.episodeNumber],
                    episodeName = epName,
                    watchedSeconds = watchedSec,
                    durationSeconds = duration,
                    isCompleted = isWatched,
                    userRating = row[UserEpisodeTable.userRating],
                    lastWatchedAt = row[UserEpisodeTable.lastWatchedAt]
                )
            }

        (movies + episodes).sortedByDescending { it.lastWatchedAt }
    }
}

data class DayActivityRawItem(
    val catalogId: String?,
    val externalId: String,
    val mediaType: String,
    val isEpisode: Boolean,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeName: String? = null,
    val watchedSeconds: Long,
    val durationSeconds: Long,
    val isCompleted: Boolean,
    val userRating: Int? = null,
    val lastWatchedAt: Instant
)
