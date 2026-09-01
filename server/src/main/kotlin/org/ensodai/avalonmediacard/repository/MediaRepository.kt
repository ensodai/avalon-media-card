package org.ensodai.avalonmediacard.repository

import org.ensodai.avalonmediacard.contract.classification.AnimeClassificationEngine
import org.ensodai.avalonmediacard.contract.classification.AnimeSubType
import org.ensodai.avalonmediacard.contract.model.ActorMetadata
import org.ensodai.avalonmediacard.contract.model.GenreMetadata
import org.ensodai.avalonmediacard.contract.model.KeywordMetadata
import org.ensodai.avalonmediacard.contract.model.MediaMetadata
import org.ensodai.avalonmediacard.contract.model.SeasonMetadata
import org.ensodai.avalonmediacard.contract.plugins.GenreDictionaryProvider
import org.ensodai.avalonmediacard.contract.utils.toProxyImageUrl
import org.ensodai.avalonmediacard.database.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single

@Single
class MediaRepository(
    private val genreDictionaryProvider: GenreDictionaryProvider
) {
    suspend fun upsertMetadata(
        catalogId: String,
        externalId: String,
        mediaType: String,
        metadata: MediaMetadata,
        language: String = "ru"
    ) = dbQuery {
            val normLang = normalizeLang(language)
            val existing = MediaTable.selectAll()
                .where {
                    (MediaTable.catalogId eq catalogId) and
                            (MediaTable.externalId eq externalId)
                }
                .firstOrNull()

            val year = metadata.releaseDate?.take(4)?.toIntOrNull()
            val parsedRating = metadata.rating?.replace(',', '.')?.toDoubleOrNull()

            val mediaId = if (existing != null) {
                val id = existing[MediaTable.id]
                MediaTable.update({ MediaTable.id eq id }) {
                    it[this.mediaType] = mediaType
                    if (metadata.imdbId != null) it[imdbId] = metadata.imdbId
                    it[releaseYear] = year
                    it[tmdbRating] = parsedRating
                    it[status] = metadata.status
                    it[animeSubType] = metadata.animeSubType
                }
                id
            } else {
                MediaTable.insert {
                    it[this.catalogId] = catalogId
                    it[this.externalId] = externalId
                    it[this.mediaType] = mediaType
                    it[this.imdbId] = metadata.imdbId
                    it[releaseYear] = year
                    it[tmdbRating] = parsedRating
                    it[status] = metadata.status
                    it[animeSubType] = metadata.animeSubType
                } get MediaTable.id
            }

            // Translation
            val existingTrans = MediaTranslationTable.selectAll()
                .where { (MediaTranslationTable.mediaId eq mediaId) and (MediaTranslationTable.language eq normLang) }
                .firstOrNull()
            if (existingTrans != null) {
                MediaTranslationTable.update({ MediaTranslationTable.id eq existingTrans[MediaTranslationTable.id] }) {
                    it[title] = metadata.title.take(255)
                    it[originalTitle] = metadata.originalTitle?.take(255)
                    it[overview] = metadata.description
                }
            } else {
                MediaTranslationTable.insert {
                    it[this.mediaId] = mediaId
                    it[this.language] = normLang
                    it[title] = metadata.title.take(255)
                    it[originalTitle] = metadata.originalTitle?.take(255)
                    it[overview] = metadata.description
                }
            }

            // Images
            MediaImageTable.deleteWhere {
                (MediaImageTable.mediaId eq mediaId) and
                        (MediaImageTable.language eq normLang) and
                        (MediaImageTable.seasonId.isNull()) and
                        (MediaImageTable.episodeId.isNull())
            }
            metadata.posterUrl?.let { url ->
                MediaImageTable.insert {
                    it[this.mediaId] = mediaId
                    it[imageType] = "POSTER"
                    it[this.language] = normLang
                    it[this.url] = url.take(255)
                }
            }
            metadata.backgroundUrl?.let { url ->
                MediaImageTable.insert {
                    it[this.mediaId] = mediaId
                    it[imageType] = "BACKDROP"
                    it[this.language] = normLang
                    it[this.url] = url.take(255)
                }
            }

            // Genres
            MediaGenreTable.deleteWhere { MediaGenreTable.mediaId eq mediaId }
            metadata.genres.forEach { genre ->
                MediaGenreTable.insert {
                    it[this.mediaId] = mediaId
                    it[genreId] = genre.id
                }
            }

            // Director
            MediaCreditTable.deleteWhere { (MediaCreditTable.mediaId eq mediaId) and (MediaCreditTable.role eq "Director") }
            if (metadata.directorId != null && metadata.director != null) {
                val pId = upsertPerson(
                    metadata.directorId!!,
                    metadata.director!!,
                    metadata.directorImageUrl,
                    language,
                    mediaId
                )
                MediaCreditTable.insert {
                    it[this.mediaId] = mediaId
                    it[this.personId] = pId
                    it[this.role] = "Director"
                }
            }

            // Cast (Top 15)
            MediaCreditTable.deleteWhere { (MediaCreditTable.mediaId eq mediaId) and (MediaCreditTable.role eq "Actor") }
            val insertedActorPIds = mutableSetOf<org.jetbrains.exposed.v1.core.dao.id.EntityID<kotlin.uuid.Uuid>>()
            metadata.cast.forEach { actor ->
                val actorId = actor.id ?: ""
                val actorName = actor.name
                if (actorId.isNotEmpty()) {
                    val pId = upsertPerson(
                        actorId,
                        actorName,
                        actor.profileUrl,
                        language,
                        mediaId,
                        actor.originalName
                    )
                    if (insertedActorPIds.add(pId)) {
                        val creditId = MediaCreditTable.insert {
                            it[this.mediaId] = mediaId
                            it[this.personId] = pId
                            it[this.role] = "Actor"
                        } get MediaCreditTable.id

                        MediaCreditTranslationTable.insert {
                            it[this.creditId] = creditId
                            it[this.language] = normLang
                            it[characterName] = actor.character?.take(255)
                        }
                    }
                }
            }

            // Seasons
            metadata.seasons.forEach { season ->
                val existingSeason = MediaSeasonTable.selectAll().where {
                    (MediaSeasonTable.mediaId eq mediaId) and
                            (MediaSeasonTable.seasonNumber eq season.seasonNumber)
                }.firstOrNull()

                val seasonId = if (existingSeason != null) {
                    val sid = existingSeason[MediaSeasonTable.id]
                    MediaSeasonTable.update({ MediaSeasonTable.id eq sid }) {
                        it[airDate] = season.airDate
                        it[episodeCount] = season.episodeCount
                    }
                    sid
                } else {
                    MediaSeasonTable.insert {
                        it[this.mediaId] = mediaId
                        it[seasonNumber] = season.seasonNumber
                        it[airDate] = season.airDate
                        it[episodeCount] = season.episodeCount
                    } get MediaSeasonTable.id
                }

                val st = MediaSeasonTranslationTable.selectAll().where {
                    (MediaSeasonTranslationTable.seasonId eq seasonId) and (MediaSeasonTranslationTable.language eq normLang)
                }.firstOrNull()

                if (st != null) {
                    MediaSeasonTranslationTable.update({ MediaSeasonTranslationTable.id eq st[MediaSeasonTranslationTable.id] }) {
                        it[name] = season.name
                        it[overview] = season.overview
                    }
                } else {
                    MediaSeasonTranslationTable.insert {
                        it[this.seasonId] = seasonId
                        it[this.language] = normLang
                        it[name] = season.name
                        it[overview] = season.overview
                    }
                }

                season.posterUrl?.let { url ->
                    MediaImageTable.deleteWhere { (MediaImageTable.seasonId eq seasonId) and (MediaImageTable.language eq normLang) }
                    MediaImageTable.insert {
                        it[this.mediaId] = mediaId
                        it[this.seasonId] = seasonId
                        it[imageType] = "POSTER"
                        it[this.language] = normLang
                        it[this.url] = url.take(255)
                    }
                }
            }
        }

    private fun upsertPerson(
        tmdbId: String,
        name: String,
        avatarUrl: String?,
        language: String,
        mediaId: org.jetbrains.exposed.v1.core.dao.id.EntityID<kotlin.uuid.Uuid>,
        originalName: String? = null
    ): org.jetbrains.exposed.v1.core.dao.id.EntityID<kotlin.uuid.Uuid> {
        val normLang = normalizeLang(language)
        val safeName = name.take(255)
        val safeAvatar = avatarUrl?.take(255)
        val existing = MediaPersonTable.selectAll().where { MediaPersonTable.personId eq tmdbId }.firstOrNull()
        val pId = if (existing != null) {
            existing[MediaPersonTable.id]
        } else {
            MediaPersonTable.insert {
                it[personId] = tmdbId
            } get MediaPersonTable.id
        }

        val trans = MediaPersonTranslationTable.selectAll()
            .where { (MediaPersonTranslationTable.personId eq pId) and (MediaPersonTranslationTable.language eq normLang) }
            .firstOrNull()
        if (trans != null) {
            MediaPersonTranslationTable.update({ MediaPersonTranslationTable.id eq trans[MediaPersonTranslationTable.id] }) {
                it[this.name] = safeName
            }
        } else {
            MediaPersonTranslationTable.insert {
                it[this.personId] = pId
                it[this.language] = normLang
                it[this.name] = safeName
            }
        }

        val safeOrig = originalName?.take(255)?.takeIf { it.isNotBlank() }
        if (safeOrig != null && safeOrig != safeName) {
            val enTrans = MediaPersonTranslationTable.selectAll()
                .where { (MediaPersonTranslationTable.personId eq pId) and (MediaPersonTranslationTable.language eq "en") }
                .firstOrNull()
            if (enTrans == null) {
                MediaPersonTranslationTable.insert {
                    it[this.personId] = pId
                    it[this.language] = "en"
                    it[this.name] = safeOrig
                }
            }
        }

        if (safeAvatar != null) {
            val imgExists = MediaImageTable.selectAll()
                .where { (MediaImageTable.personId eq pId) and (MediaImageTable.imageType eq "PROFILE") }.firstOrNull()
            if (imgExists == null) {
                MediaImageTable.insert {
                    it[this.mediaId] = mediaId
                    it[this.personId] = pId
                    it[this.imageType] = "PROFILE"
                    it[this.url] = safeAvatar
                }
            } else {
                MediaImageTable.update({ MediaImageTable.id eq imgExists[MediaImageTable.id] }) {
                    it[this.url] = safeAvatar
                }
            }
        }

        return pId
    }

    private fun normalizeLang(language: String?): String {
        if (language.isNullOrBlank()) return "ru"
        val code = language.lowercase().substringBefore("-").substringBefore("_")
        return if (code in listOf("ru", "en")) code else "ru"
    }

    suspend fun getMetadataBatch(catalogId: String, externalIds: List<String>, language: String = "ru"): Map<String, MediaMetadata> {
        if (externalIds.isEmpty()) return emptyMap()
        val normLang = normalizeLang(language)
        val genreDict = genreDictionaryProvider.getLocalizedGenres(normLang)
        return dbQuery {
            val medias = MediaTable.selectAll()
                .where { (MediaTable.catalogId eq catalogId) and (MediaTable.externalId inList externalIds) }
                .associateBy { it[MediaTable.id] }

            if (medias.isEmpty()) return@dbQuery emptyMap()
            val mediaIds = medias.keys.toList()

            val allTrans = MediaTranslationTable.selectAll()
                .where { MediaTranslationTable.mediaId inList mediaIds }
                .groupBy { it[MediaTranslationTable.mediaId] }

            val allImages = MediaImageTable.selectAll()
                .where { (MediaImageTable.mediaId inList mediaIds) and (MediaImageTable.seasonId.isNull()) and (MediaImageTable.episodeId.isNull()) }
                .groupBy { it[MediaImageTable.mediaId] }

            val allGenres = MediaGenreTable.selectAll()
                .where { MediaGenreTable.mediaId inList mediaIds }
                .groupBy { it[MediaGenreTable.mediaId] }

            val allCredits = MediaCreditTable.selectAll()
                .where { MediaCreditTable.mediaId inList mediaIds }
                .toList()
            val creditsByMediaId = allCredits.groupBy { it[MediaCreditTable.mediaId] }
            val personIds = allCredits.map { it[MediaCreditTable.personId] }.distinct()

            val personTranslations = if (personIds.isNotEmpty()) {
                MediaPersonTranslationTable.selectAll()
                    .where { MediaPersonTranslationTable.personId inList personIds }
                    .groupBy { it[MediaPersonTranslationTable.personId] }
            } else emptyMap()

            val persons = if (personIds.isNotEmpty()) {
                MediaPersonTable.selectAll()
                    .where { MediaPersonTable.id inList personIds }
                    .associateBy { it[MediaPersonTable.id] }
            } else emptyMap()

            val creditTranslations = if (allCredits.isNotEmpty()) {
                MediaCreditTranslationTable.selectAll()
                    .where { MediaCreditTranslationTable.creditId inList allCredits.map { it[MediaCreditTable.id] } }
                    .groupBy { it[MediaCreditTranslationTable.creditId] }
            } else emptyMap()

            val personImages = if (personIds.isNotEmpty()) {
                MediaImageTable.selectAll()
                    .where { (MediaImageTable.personId inList personIds) and (MediaImageTable.imageType eq "PROFILE") }
                    .associateBy { it[MediaImageTable.personId] }
            } else emptyMap()

            val allSeasons = MediaSeasonTable.selectAll()
                .where { MediaSeasonTable.mediaId inList mediaIds }
                .groupBy { it[MediaSeasonTable.mediaId] }
            val seasonIds = allSeasons.values.flatten().map { it[MediaSeasonTable.id] }.distinct()

            val seasonTranslations = if (seasonIds.isNotEmpty()) {
                MediaSeasonTranslationTable.selectAll()
                    .where { MediaSeasonTranslationTable.seasonId inList seasonIds }
                    .groupBy { it[MediaSeasonTranslationTable.seasonId] }
            } else emptyMap()

            val seasonImages = if (seasonIds.isNotEmpty()) {
                MediaImageTable.selectAll()
                    .where { (MediaImageTable.seasonId inList seasonIds) and (MediaImageTable.imageType eq "POSTER") }
                    .associateBy { it[MediaImageTable.seasonId] }
            } else emptyMap()

            val allKeywords = MediaKeywordTable.selectAll()
                .where { MediaKeywordTable.mediaId inList mediaIds }
                .groupBy { it[MediaKeywordTable.mediaId] }

            medias.mapNotNull { (id, existing) ->
                val extId = existing[MediaTable.externalId]
                val tList = allTrans[id] ?: emptyList()
                val t = tList.firstOrNull { normalizeLang(it[MediaTranslationTable.language]) == normLang }
                    ?: return@mapNotNull null
                val imgs = allImages[id] ?: emptyList()
                
                val posterRow = imgs.firstOrNull { it[MediaImageTable.imageType] == "POSTER" && normalizeLang(it[MediaImageTable.language]) == normLang }
                    ?: imgs.firstOrNull { it[MediaImageTable.imageType] == "POSTER" }
                val backdropRow = imgs.firstOrNull { it[MediaImageTable.imageType] == "BACKDROP" && normalizeLang(it[MediaImageTable.language]) == normLang }
                    ?: imgs.firstOrNull { it[MediaImageTable.imageType] == "BACKDROP" }

                val genres = (allGenres[id] ?: emptyList()).map {
                    val gId = it[MediaGenreTable.genreId]
                    GenreMetadata(id = gId, name = genreDict[gId.toString()] ?: "Unknown")
                }

                val credits = creditsByMediaId[id] ?: emptyList()
                val directorRow = credits.firstOrNull { it[MediaCreditTable.role] == "Director" }
                val directorPTransList = directorRow?.let { personTranslations[it[MediaCreditTable.personId]] } ?: emptyList()
                val directorTrans = directorPTransList.firstOrNull { normalizeLang(it[MediaPersonTranslationTable.language]) == normLang }
                    ?: directorPTransList.firstOrNull()
                val director = directorTrans?.get(MediaPersonTranslationTable.name)
                val directorId = directorRow?.let { persons[it[MediaCreditTable.personId]]?.get(MediaPersonTable.personId) }
                val directorImageUrl = directorRow?.let { personImages[it[MediaCreditTable.personId]]?.get(MediaImageTable.url)?.toProxyImageUrl("w185") }

                val cast = credits.filter { it[MediaCreditTable.role] == "Actor" }.mapNotNull { row ->
                    val pId = row[MediaCreditTable.personId]
                    val cId = row[MediaCreditTable.id]
                    val p = persons[pId] ?: return@mapNotNull null
                    val pTransList = personTranslations[pId] ?: emptyList()
                    val locTrans = pTransList.firstOrNull { normalizeLang(it[MediaPersonTranslationTable.language]) == normLang }
                        ?: pTransList.firstOrNull()
                    val enTrans = pTransList.firstOrNull { normalizeLang(it[MediaPersonTranslationTable.language]) == "en" }
                    val actorName = locTrans?.get(MediaPersonTranslationTable.name)
                        ?: enTrans?.get(MediaPersonTranslationTable.name)
                        ?: p[MediaPersonTable.personId]
                    val origName = enTrans?.get(MediaPersonTranslationTable.name) ?: actorName

                    val cTransList = creditTranslations[cId] ?: emptyList()
                    val cTrans = cTransList.firstOrNull { normalizeLang(it[MediaCreditTranslationTable.language]) == normLang }
                        ?: cTransList.firstOrNull()

                    ActorMetadata(
                        name = actorName,
                        originalName = origName,
                        character = cTrans?.get(MediaCreditTranslationTable.characterName),
                        profileUrl = personImages[pId]?.get(MediaImageTable.url)?.toProxyImageUrl("w185"),
                        id = p[MediaPersonTable.personId]
                    )
                }

                val seasons = (allSeasons[id] ?: emptyList()).map { row ->
                    val sId = row[MediaSeasonTable.id]
                    val sTransList = seasonTranslations[sId] ?: emptyList()
                    val st = sTransList.firstOrNull { normalizeLang(it[MediaSeasonTranslationTable.language]) == normLang } ?: sTransList.firstOrNull()
                    SeasonMetadata(
                        id = sId.value.toString(),
                        seasonNumber = row[MediaSeasonTable.seasonNumber],
                        name = st?.get(MediaSeasonTranslationTable.name) ?: "Season ${row[MediaSeasonTable.seasonNumber]}",
                        overview = st?.get(MediaSeasonTranslationTable.overview),
                        posterUrl = seasonImages[sId]?.get(MediaImageTable.url)?.toProxyImageUrl("w342"),
                        episodeCount = row[MediaSeasonTable.episodeCount] ?: 0,
                        airDate = row[MediaSeasonTable.airDate]
                    )
                }.sortedBy { it.seasonNumber }

                val keywords = (allKeywords[id] ?: emptyList()).map {
                    KeywordMetadata(id = it[MediaKeywordTable.keywordId], name = it[MediaKeywordTable.keywordName])
                }.filter { it.id != 0 }

                var subType = existing[MediaTable.animeSubType]
                if (subType == AnimeSubType.NOT_ANIME && genres.any { it.id == 16 }) {
                    val recheck = AnimeClassificationEngine.analyze(
                        genres = genres.map { it.id },
                        keywords = keywords.map { it.name },
                        originalTitle = t.get(MediaTranslationTable.originalTitle),
                        title = t.get(MediaTranslationTable.title)
                    )
                    if (recheck.isAnime) {
                        subType = recheck.subType
                        MediaTable.update({ MediaTable.id eq id }) {
                            it[animeSubType] = subType
                        }
                    }
                }

                val meta = MediaMetadata(
                    title = t.get(MediaTranslationTable.title),
                    originalTitle = t.get(MediaTranslationTable.originalTitle),
                    imdbId = existing[MediaTable.imdbId],
                    description = t.get(MediaTranslationTable.overview),
                    posterUrl = posterRow?.get(MediaImageTable.url)?.toProxyImageUrl("w342"),
                    backgroundUrl = backdropRow?.get(MediaImageTable.url)?.toProxyImageUrl("w1280"),
                    rating = existing[MediaTable.tmdbRating]?.toString(),
                    genres = genres,
                    keywords = keywords,
                    releaseDate = existing[MediaTable.releaseYear]?.toString(),
                    status = existing[MediaTable.status],
                    director = director,
                    directorId = directorId,
                    directorImageUrl = directorImageUrl,
                    cast = cast,
                    seasons = seasons,
                    numberOfSeasons = seasons.count { it.seasonNumber > 0 }.takeIf { it > 0 } ?: seasons.size.takeIf { it > 0 },
                    animeSubType = subType
                )
                extId to meta
            }.toMap()
        }
    }

    suspend fun getMetadata(catalogId: String, externalId: String, language: String = "ru"): MediaMetadata? {
        val normLang = normalizeLang(language)
        val genreDict = genreDictionaryProvider.getLocalizedGenres(normLang)
        return dbQuery {
            val existing = MediaTable.selectAll()
                .where {
                    (MediaTable.catalogId eq catalogId) and
                            (MediaTable.externalId eq externalId)
                }
                .firstOrNull() ?: return@dbQuery null

            val id = existing[MediaTable.id]

            val transList = MediaTranslationTable.selectAll()
                .where { MediaTranslationTable.mediaId eq id }
                .toList()
            val trans = transList.firstOrNull { normalizeLang(it[MediaTranslationTable.language]) == normLang }
                ?: return@dbQuery null

            val images = MediaImageTable.selectAll()
                .where { (MediaImageTable.mediaId eq id) and (MediaImageTable.seasonId.isNull()) and (MediaImageTable.episodeId.isNull()) }
                .toList()

            val posterRow =
                images.firstOrNull { it[MediaImageTable.imageType] == "POSTER" && normalizeLang(it[MediaImageTable.language]) == normLang }
                    ?: images.firstOrNull { it[MediaImageTable.imageType] == "POSTER" }
            val backdropRow =
                images.firstOrNull { it[MediaImageTable.imageType] == "BACKDROP" && normalizeLang(it[MediaImageTable.language]) == normLang }
                    ?: images.firstOrNull { it[MediaImageTable.imageType] == "BACKDROP" }

            val genres = MediaGenreTable.selectAll()
                .where { MediaGenreTable.mediaId eq id }
                .map {
                    val gId = it[MediaGenreTable.genreId]
                    GenreMetadata(
                        id = gId,
                        name = genreDict[gId.toString()] ?: "Unknown"
                    )
                }

            val credits = MediaCreditTable.selectAll().where { MediaCreditTable.mediaId eq id }.toList()

            val directorRow = credits.firstOrNull { it[MediaCreditTable.role] == "Director" }
            val directorTransList = directorRow?.let { row ->
                MediaPersonTranslationTable.selectAll().where {
                    MediaPersonTranslationTable.personId eq row[MediaCreditTable.personId]
                }.toList()
            } ?: emptyList()
            val directorTrans = directorTransList.firstOrNull { normalizeLang(it[MediaPersonTranslationTable.language]) == normLang }
                ?: directorTransList.firstOrNull()
            val directorPerson = directorRow?.let { row ->
                MediaPersonTable.selectAll().where { MediaPersonTable.id eq row[MediaCreditTable.personId] }
                    .firstOrNull()
            }

            val director = directorTrans?.get(MediaPersonTranslationTable.name)
            val directorId = directorPerson?.get(MediaPersonTable.personId)
            val directorImageUrl = directorPerson?.let { p ->
                MediaImageTable.selectAll()
                    .where { (MediaImageTable.personId eq p[MediaPersonTable.id]) and (MediaImageTable.imageType eq "PROFILE") }
                    .firstOrNull()?.get(MediaImageTable.url)?.toProxyImageUrl("w185")
            }

            val cast = credits.filter { it[MediaCreditTable.role] == "Actor" }.mapNotNull { row ->
                val pId = row[MediaCreditTable.personId]
                val cId = row[MediaCreditTable.id]
                val p = MediaPersonTable.selectAll().where { MediaPersonTable.id eq pId }.firstOrNull() ?: return@mapNotNull null
                
                val pTransList = MediaPersonTranslationTable.selectAll()
                    .where { MediaPersonTranslationTable.personId eq pId }
                    .toList()
                val pt = pTransList.firstOrNull { normalizeLang(it[MediaPersonTranslationTable.language]) == normLang }
                    ?: pTransList.firstOrNull()
                val enTrans = pTransList.firstOrNull { normalizeLang(it[MediaPersonTranslationTable.language]) == "en" }

                val cTransList = MediaCreditTranslationTable.selectAll()
                    .where { MediaCreditTranslationTable.creditId eq cId }
                    .toList()
                val ct = cTransList.firstOrNull { normalizeLang(it[MediaCreditTranslationTable.language]) == normLang }
                    ?: cTransList.firstOrNull()

                val pImg = MediaImageTable.selectAll()
                    .where { (MediaImageTable.personId eq pId) and (MediaImageTable.imageType eq "PROFILE") }
                    .firstOrNull()

                val actorName = pt?.get(MediaPersonTranslationTable.name)
                    ?: enTrans?.get(MediaPersonTranslationTable.name)
                    ?: p[MediaPersonTable.personId]
                val origName = enTrans?.get(MediaPersonTranslationTable.name) ?: actorName

                ActorMetadata(
                    name = actorName,
                    originalName = origName,
                    character = ct?.get(MediaCreditTranslationTable.characterName),
                    profileUrl = pImg?.get(MediaImageTable.url)?.toProxyImageUrl("w185"),
                    id = p[MediaPersonTable.personId]
                )
            }

            val seasons = MediaSeasonTable.selectAll().where { MediaSeasonTable.mediaId eq id }.map { row ->
                val sId = row[MediaSeasonTable.id]
                val sTransList = MediaSeasonTranslationTable.selectAll()
                    .where { MediaSeasonTranslationTable.seasonId eq sId }
                    .toList()
                val st = sTransList.firstOrNull { normalizeLang(it[MediaSeasonTranslationTable.language]) == normLang }
                    ?: sTransList.firstOrNull()
                val sImg = MediaImageTable.selectAll()
                    .where { (MediaImageTable.seasonId eq sId) and (MediaImageTable.imageType eq "POSTER") }
                    .firstOrNull()

                SeasonMetadata(
                    id = sId.value.toString(),
                    seasonNumber = row[MediaSeasonTable.seasonNumber],
                    name = st?.get(MediaSeasonTranslationTable.name) ?: "Season ${row[MediaSeasonTable.seasonNumber]}",
                    overview = st?.get(MediaSeasonTranslationTable.overview),
                    posterUrl = sImg?.get(MediaImageTable.url)?.toProxyImageUrl("w342"),
                    episodeCount = row[MediaSeasonTable.episodeCount] ?: 0,
                    airDate = row[MediaSeasonTable.airDate]
                )
            }.sortedBy { it.seasonNumber }

            val tmdbId = externalId.toIntOrNull() ?: 0
            val mediaType = existing[MediaTable.mediaType]
            val keywords = MediaKeywordTable.selectAll()
                .where { (MediaKeywordTable.mediaId eq id) }
                .map {
                    KeywordMetadata(
                        id = it[MediaKeywordTable.keywordId],
                        name = it[MediaKeywordTable.keywordName]
                    )
                }.filter { it.id != 0 }

            var subType = existing[MediaTable.animeSubType]
            if (subType == AnimeSubType.NOT_ANIME && genres.any { it.id == 16 }) {
                val recheck = AnimeClassificationEngine.analyze(
                    genres = genres.map { it.id },
                    keywords = keywords.map { it.name },
                    originalTitle = trans?.get(MediaTranslationTable.originalTitle),
                    title = trans?.get(MediaTranslationTable.title)
                )
                if (recheck.isAnime) {
                    subType = recheck.subType
                    MediaTable.update({ MediaTable.id eq id }) {
                        it[animeSubType] = subType
                    }
                }
            }

            val tmdbRatingStr = existing[MediaTable.tmdbRating]?.toString()

            MediaMetadata(
                title = trans?.get(MediaTranslationTable.title) ?: "",
                originalTitle = trans?.get(MediaTranslationTable.originalTitle),
                imdbId = existing[MediaTable.imdbId],
                description = trans?.get(MediaTranslationTable.overview),
                posterUrl = posterRow?.get(MediaImageTable.url)?.toProxyImageUrl("w342"),
                backgroundUrl = backdropRow?.get(MediaImageTable.url)?.toProxyImageUrl("w1280"),
                rating = tmdbRatingStr,
                genres = genres,
                keywords = keywords,
                releaseDate = existing[MediaTable.releaseYear]?.toString(),
                status = existing[MediaTable.status],
                director = director,
                directorId = directorId,
                directorImageUrl = directorImageUrl,
                cast = cast,
                seasons = seasons,
                numberOfSeasons = seasons.count { it.seasonNumber > 0 }.takeIf { it > 0 } ?: seasons.size.takeIf { it > 0 },
                animeSubType = subType
            )
        }
    }

    suspend fun upsertSeasonDetails(
        catalogId: String,
        externalId: String,
        language: String,
        seasonNumber: Int,
        episodes: List<org.ensodai.avalonmediacard.contract.slot.EpisodeItem>
    ) = dbQuery {
            val metadataRow = MediaTable.selectAll().where {
                (MediaTable.catalogId eq catalogId) and
                        (MediaTable.externalId eq externalId)
            }.firstOrNull() ?: return@dbQuery

            val mediaId = metadataRow[MediaTable.id]

            val existingSeason = MediaSeasonTable.selectAll().where {
                (MediaSeasonTable.mediaId eq mediaId) and
                        (MediaSeasonTable.seasonNumber eq seasonNumber)
            }.firstOrNull()

            val seasonId = if (existingSeason != null) {
                MediaSeasonTable.update({ MediaSeasonTable.id eq existingSeason[MediaSeasonTable.id] }) {
                    it[this.episodeCount] = episodes.size
                }
                existingSeason[MediaSeasonTable.id]
            } else {
                MediaSeasonTable.insert {
                    it[this.mediaId] = mediaId
                    it[this.seasonNumber] = seasonNumber
                    it[this.episodeCount] = episodes.size
                } get MediaSeasonTable.id
            }

            MediaEpisodeTable.deleteWhere { MediaEpisodeTable.seasonId eq seasonId }

            episodes.forEach { ep ->
                val epId = MediaEpisodeTable.insert {
                    it[this.seasonId] = seasonId
                    it[this.episodeNumber] = ep.episodeNumber
                    it[this.airDate] = ep.airDate
                    it[this.runtime] = ep.runtime
                } get MediaEpisodeTable.id

                MediaEpisodeTranslationTable.insert {
                    it[this.episodeId] = epId
                    it[this.language] = language
                    it[this.name] = ep.name
                    it[this.overview] = ep.overview
                }

                ep.stillUrl?.let { url ->
                    MediaImageTable.insert {
                        it[this.mediaId] = mediaId
                        it[this.episodeId] = epId
                        it[imageType] = "POSTER"
                        it[this.language] = language
                        it[this.url] = url.take(255)
                    }
                }
            }
        }

    suspend fun getSeasonDetails(
        catalogId: String,
        externalId: String,
        language: String,
        seasonNumber: Int
    ): List<org.ensodai.avalonmediacard.contract.slot.EpisodeItem>? = dbQuery {
        val metadataRow = MediaTable.selectAll().where {
            (MediaTable.catalogId eq catalogId) and
                    (MediaTable.externalId eq externalId)
        }.firstOrNull() ?: return@dbQuery null

        val mediaId = metadataRow[MediaTable.id]

        val seasonRow = MediaSeasonTable.selectAll().where {
            (MediaSeasonTable.mediaId eq mediaId) and
                    (MediaSeasonTable.seasonNumber eq seasonNumber)
        }.firstOrNull() ?: return@dbQuery null

        val episodes = MediaEpisodeTable.selectAll().where {
            MediaEpisodeTable.seasonId eq seasonRow[MediaSeasonTable.id]
        }.map { row ->
            val epId = row[MediaEpisodeTable.id]
            val et = MediaEpisodeTranslationTable.selectAll()
                .where { (MediaEpisodeTranslationTable.episodeId eq epId) and (MediaEpisodeTranslationTable.language eq language) }
                .firstOrNull()
            val img = MediaImageTable.selectAll()
                .where { (MediaImageTable.episodeId eq epId) and (MediaImageTable.imageType eq "POSTER") }.firstOrNull()

            org.ensodai.avalonmediacard.contract.slot.EpisodeItem(
                id = epId.value.toString(),
                episodeNumber = row[MediaEpisodeTable.episodeNumber],
                name = et?.get(MediaEpisodeTranslationTable.name) ?: "",
                overview = et?.get(MediaEpisodeTranslationTable.overview),
                stillUrl = img?.get(MediaImageTable.url),
                airDate = row[MediaEpisodeTable.airDate],
                runtime = row[MediaEpisodeTable.runtime],
                voteAverage = null,
                isWatched = false,
                userRating = null
            )
        }
        val expectedCount = seasonRow[MediaSeasonTable.episodeCount] ?: 0
        if (episodes.isEmpty() || episodes.size != expectedCount) {
            return@dbQuery null
        }

        episodes
    }

    suspend fun count(): Long = dbQuery {
        MediaTable.selectAll().count()
    }

    suspend fun clearAll(): Int = dbQuery {
        MediaTable.deleteAll()
    }
}
