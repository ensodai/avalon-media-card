package org.ensodai.avalonmediacard.tmdb

import org.ensodai.avalonmediacard.contract.classification.AnimeClassificationEngine
import org.ensodai.avalonmediacard.contract.model.ActorMetadata
import org.ensodai.avalonmediacard.contract.model.FilmographyCredit
import org.ensodai.avalonmediacard.contract.model.GenreMetadata
import org.ensodai.avalonmediacard.contract.model.MediaMetadata
import org.ensodai.avalonmediacard.contract.model.PersonMetadata
import org.ensodai.avalonmediacard.contract.model.ProductionCompanyMetadata
import org.ensodai.avalonmediacard.contract.model.RelatedMediaMetadata
import org.ensodai.avalonmediacard.contract.model.SeasonMetadata
import org.ensodai.avalonmediacard.contract.model.TmdbMovieDto
import org.ensodai.avalonmediacard.contract.model.TrailerMetadata
import org.ensodai.avalonmediacard.contract.utils.toProxyImageUrl
import org.ensodai.avalonmediacard.tmdb.responses.TmdbPersonDetailResponse
import org.ensodai.avalonmediacard.tmdb.responses.TmdbSeasonDetailResponse
import org.koin.core.annotation.Single

@Single
class TmdbMetadataMapper {

    fun mapMediaDetails(
        mediaId: String,
        movie: TmdbDetails,
        recs: List<TmdbMovieDto> = emptyList(),
        similar: List<TmdbMovieDto> = emptyList(),
        language: String? = null
    ): MediaMetadata {
        val isTv = mediaId.startsWith("tv:")
        val prefix = if (isTv) "tv:" else ""

        val castList = movie.credits?.cast?.sortedBy { it.order }?.take(15)?.map { actor ->
            ActorMetadata(
                name = actor.name,
                originalName = actor.originalName ?: actor.name,
                character = actor.character,
                profileUrl = actor.profilePath.toProxyImageUrl("w185"),
                id = actor.id.toString()
            )
        } ?: emptyList()

        val trailerList = movie.videos?.results?.filter { it.site == "YouTube" }?.map { video ->
            TrailerMetadata(
                name = video.name,
                videoUrl = "https://www.youtube.com/watch?v=${video.key}",
                type = video.type
            )
        } ?: emptyList()

        val recsList = recs.map { r ->
            RelatedMediaMetadata(
                mediaId = "$prefix${r.id}",
                title = r.title ?: r.name ?: "",
                posterUrl = r.posterPath.toProxyImageUrl("w342")
            )
        }

        val similarList = similar.map { s ->
            RelatedMediaMetadata(
                mediaId = "$prefix${s.id}",
                title = s.title ?: s.name ?: "",
                posterUrl = s.posterPath.toProxyImageUrl("w342")
            )
        }

        val directorCrew = movie.credits?.crew?.firstOrNull { it.job == "Director" }
            ?: movie.credits?.crew?.firstOrNull { it.job == "Creator" }
            ?: movie.credits?.crew?.firstOrNull { it.job == "Executive Producer" }
        val directorName = directorCrew?.name
        val directorImageUrl = directorCrew?.profilePath.toProxyImageUrl("w185")
        val directorId = directorCrew?.id?.toString()

        val titleText = if (movie.title?.hasUnsupportedGlyphs() == true) {
            movie.englishTitle ?: movie.title ?: ""
        } else {
            movie.title ?: ""
        }

        val subtitleText = if (movie.subtitle?.hasUnsupportedGlyphs() == true) {
            movie.englishTitle ?: movie.subtitle
        } else {
            movie.subtitle
        }

        var numSeasons: Int? = null
        var numEpisodes: Int? = null
        var showStatus: String? = null
        var networkName: String? = null
        var seasonsList: List<SeasonMetadata> = emptyList()
        var runtimeInt: Int? = null
        var companiesList: List<ProductionCompanyMetadata> = emptyList()
        val imdbIdStr = when (movie) {
            is TmdbDetails.Movie -> movie.detail.imdbId
            is TmdbDetails.Show -> movie.detail.externalIds?.imdbId
        }

        if (movie is TmdbDetails.Movie) {
            runtimeInt = movie.detail.runtime
            companiesList = movie.detail.productionCompanies.map { c ->
                ProductionCompanyMetadata(
                    id = c.id,
                    name = c.name,
                    logoUrl = c.logoPath.toProxyImageUrl("w200")
                )
            } ?: emptyList()
        } else if (movie is TmdbDetails.Show) {
            numSeasons = movie.detail.numberOfSeasons
            numEpisodes = movie.detail.numberOfEpisodes
            showStatus = movie.detail.status
            networkName = movie.detail.networks.firstOrNull()?.name
            seasonsList = movie.detail.seasons.map { season ->
                SeasonMetadata(
                    id = season.id.toString(),
                    seasonNumber = season.seasonNumber,
                    name = season.name ?: "Season ${season.seasonNumber}",
                    overview = season.overview,
                    posterUrl = season.posterPath.toProxyImageUrl("w342"),
                    episodeCount = season.episodeCount ?: 0,
                    airDate = season.airDate
                )
            }
            runtimeInt = null
            companiesList = movie.detail.productionCompanies.map { c ->
                ProductionCompanyMetadata(
                    id = c.id,
                    name = c.name,
                    logoUrl = c.logoPath.toProxyImageUrl("w200")
                )
            } ?: emptyList()
        }

        val localizedPostersMap = mutableMapOf<String, String>()
        movie.images?.posters?.forEach { img ->
            val lang = img.iso639?.lowercase()?.trim()
            val url = img.filePath.toProxyImageUrl("w342") ?: return@forEach
            if (lang == null || lang.isEmpty() || lang == "xx") {
                if (!localizedPostersMap.containsKey("original")) {
                    localizedPostersMap["original"] = url
                }
            } else {
                if (!localizedPostersMap.containsKey(lang)) {
                    localizedPostersMap[lang] = url
                }
            }
        }
        movie.posterPath?.toProxyImageUrl("w342")?.let { defaultPoster ->
            if (!localizedPostersMap.containsKey("auto")) {
                localizedPostersMap["auto"] = defaultPoster
            }
        }

        val localizedOverviewsMap = mutableMapOf<String, String>()
        movie.translations?.translations?.forEach { item ->
            val lang = item.iso639?.lowercase()?.trim()
            val text = item.data?.overview?.trim()
            if (!lang.isNullOrBlank() && !text.isNullOrBlank() && !localizedOverviewsMap.containsKey(lang)) {
                localizedOverviewsMap[lang] = text
            }
        }
        movie.overview?.trim()?.takeIf { it.isNotBlank() }?.let { defOverview ->
            if (!localizedOverviewsMap.containsKey("auto")) {
                localizedOverviewsMap["auto"] = defOverview
            }
        }

        val normLang = language?.lowercase()?.substringBefore("-")?.substringBefore("_")?.trim()
        val selectedPoster = (if (!normLang.isNullOrBlank()) localizedPostersMap[normLang] else null)
            ?: movie.posterPath.toProxyImageUrl("w342")

        val animeClassification = AnimeClassificationEngine.analyze(
            genres = movie.genres.map { it.id },
            keywords = emptyList(),
            productionCompanies = companiesList.map { it.name },
            productionCountries = movie.originCountry,
            originalLanguage = movie.originalLanguage,
            networks = listOfNotNull(networkName),
            originalTitle = subtitleText,
            title = titleText
        )

        return MediaMetadata(
            title = titleText,
            originalTitle = subtitleText,
            imdbId = imdbIdStr,
            subtitle = subtitleText,
            description = movie.overview,
            posterUrl = selectedPoster,
            backgroundUrl = movie.backdropPath.toProxyImageUrl("w1280"),
            rating = movie.voteAverage?.let { "%.1f".format(it) },
            runtime = runtimeInt,
            genres = movie.genres.map { GenreMetadata(id = it.id, name = it.name) },
            keywords = emptyList(),
            productionCompanies = companiesList,
            releaseDate = movie.releaseDate,
            tagline = movie.tagline,
            director = directorName,
            directorImageUrl = directorImageUrl,
            directorId = directorId,
            cast = castList,
            trailers = trailerList,
            recommendations = recsList,
            similar = similarList,
            numberOfSeasons = numSeasons,
            numberOfEpisodes = numEpisodes,
            status = showStatus,
            network = networkName,
            seasons = seasonsList,
            localizedPosters = localizedPostersMap,
            localizedOverviews = localizedOverviewsMap,
            animeSubType = animeClassification.subType
        )
    }

    fun mapPersonDetails(person: TmdbPersonDetailResponse): PersonMetadata {
        val extIds = mutableMapOf<String, String>()

        val creditsList = mutableListOf<FilmographyCredit>()
        person.combinedCredits?.let { credits ->
            credits.crew
                .filter { it.job == "Director" }
                .sortedByDescending { it.voteAverage ?: 0.0 }
                .distinctBy { it.id }
                .forEach { movie ->
                    creditsList.add(
                        FilmographyCredit(
                            mediaId = movie.id.toString(),
                            title = movie.title ?: movie.name ?: "",
                            posterUrl = movie.posterPath.toProxyImageUrl("w342"),
                            releaseDate = null,
                            mediaType = movie.mediaType ?: "",
                            department = "Directing",
                            jobOrCharacter = movie.job ?: "Director"
                        )
                    )
                }

            credits.crew
                .filter { it.job in listOf("Writer", "Screenplay", "Story") }
                .sortedByDescending { it.voteAverage ?: 0.0 }
                .distinctBy { it.id }
                .forEach { movie ->
                    creditsList.add(
                        FilmographyCredit(
                            mediaId = movie.id.toString(),
                            title = movie.title ?: movie.name ?: "",
                            posterUrl = movie.posterPath.toProxyImageUrl("w342"),
                            releaseDate = null,
                            mediaType = movie.mediaType ?: "",
                            department = "Writing",
                            jobOrCharacter = movie.job ?: "Writer"
                        )
                    )
                }

            credits.crew
                .filter { it.job in listOf("Producer", "Executive Producer") }
                .sortedByDescending { it.voteAverage ?: 0.0 }
                .distinctBy { it.id }
                .forEach { movie ->
                    creditsList.add(
                        FilmographyCredit(
                            mediaId = movie.id.toString(),
                            title = movie.title ?: movie.name ?: "",
                            posterUrl = movie.posterPath.toProxyImageUrl("w342"),
                            releaseDate = null,
                            mediaType = movie.mediaType ?: "",
                            department = "Production",
                            jobOrCharacter = movie.job ?: "Producer"
                        )
                    )
                }

            credits.cast
                .sortedByDescending { it.voteAverage ?: 0.0 }
                .distinctBy { it.id }
                .forEach { movie ->
                    creditsList.add(
                        FilmographyCredit(
                            mediaId = movie.id.toString(),
                            title = movie.title ?: movie.name ?: "",
                            posterUrl = movie.posterPath.toProxyImageUrl("w342"),
                            releaseDate = null,
                            mediaType = movie.mediaType ?: "",
                            department = "Acting",
                            jobOrCharacter = movie.character ?: if (movie.mediaType == "tv") "TV" else "Movie"
                        )
                    )
                }
        }

        return PersonMetadata(
            name = person.name ?: "",
            biography = person.biography,
            birthday = person.birthday,
            deathday = person.deathday,
            placeOfBirth = person.placeOfBirth,
            profileUrl = person.profilePath.toProxyImageUrl("w342"),
            knownForDepartment = person.knownForDepartment,
            externalIds = extIds,
            images = emptyList(),
            filmography = creditsList
        )
    }

    fun mapSeasonDetails(season: TmdbSeasonDetailResponse): List<org.ensodai.avalonmediacard.contract.slot.EpisodeItem> {
        return season.episodes.map { ep ->
            org.ensodai.avalonmediacard.contract.slot.EpisodeItem(
                id = ep.id.toString(),
                episodeNumber = ep.episodeNumber,
                name = ep.name ?: "Episode ${ep.episodeNumber}",
                overview = ep.overview,
                stillUrl = ep.stillPath.toProxyImageUrl("w342"),
                airDate = ep.airDate,
                voteAverage = ep.voteAverage,
                runtime = ep.runtime
            )
        }
    }
}

private fun String.hasUnsupportedGlyphs(): Boolean {
    for (char in this) {
        val block = Character.UnicodeBlock.of(char)
        if (block != Character.UnicodeBlock.BASIC_LATIN &&
            block != Character.UnicodeBlock.CYRILLIC &&
            block != Character.UnicodeBlock.CYRILLIC_SUPPLEMENTARY &&
            block != Character.UnicodeBlock.LATIN_1_SUPPLEMENT &&
            !char.isWhitespace() &&
            !char.isDigit() &&
            char != '–' && char != '—' && char != '-' && char != ':' && char != ',' && char != '.' && char != '?' && char != '!' && char != '&' && char != '(' && char != ')'
        ) {
            return true
        }
    }
    return false
}
