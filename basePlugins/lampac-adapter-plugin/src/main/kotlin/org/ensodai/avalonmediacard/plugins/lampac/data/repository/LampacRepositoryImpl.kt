package org.ensodai.avalonmediacard.plugins.lampac.data.repository

import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.contract.plugins.VideoQuality
import org.ensodai.avalonmediacard.plugins.lampac.data.network.LampacApiClient
import org.ensodai.avalonmediacard.plugins.lampac.data.network.dto.JacRedTorrentDto
import org.ensodai.avalonmediacard.plugins.lampac.data.network.dto.LampacItemDto
import org.ensodai.avalonmediacard.plugins.lampac.data.network.dto.LampacResponseDto
import org.ensodai.avalonmediacard.plugins.lampac.domain.model.*
import org.ensodai.avalonmediacard.plugins.lampac.domain.repository.LampacRepository

class LampacRepositoryImpl(
    private val apiClient: LampacApiClient
) : LampacRepository {

    override suspend fun isGatewayAvailable(): Boolean {
        return apiClient.ping()
    }

    override suspend fun getAvailableBalancers(
        title: String,
        originalTitle: String?,
        year: Int?,
        tmdbId: Long?,
        imdbId: String?,
        kinopoiskId: Long?,
        isSerial: Boolean,
        isAnime: Boolean,
        originalLanguage: String?
    ): List<LampacBalancer> {
        val dtos = apiClient.getAvailableBalancers(
            title = title,
            originalTitle = originalTitle,
            year = year,
            tmdbId = tmdbId,
            imdbId = imdbId,
            kinopoiskId = kinopoiskId,
            isSerial = isSerial,
            isAnime = isAnime,
            originalLanguage = originalLanguage
        )
        return dtos.map { dto ->
            LampacBalancer(
                id = dto.balanser,
                name = dto.name,
                endpointUrl = dto.url
            )
        }
    }

    override suspend fun getMovieStreams(
        balancer: String,
        title: String,
        originalTitle: String?,
        year: Int?,
        tmdbId: Long?,
        imdbId: String?,
        kinopoiskId: Long?
    ): List<LampacStreamInfo> {
        var responses = apiClient.fetchFromBalancer(
            balancer = balancer,
            title = title,
            originalTitle = originalTitle,
            year = year,
            tmdbId = tmdbId,
            imdbId = imdbId,
            kinopoiskId = kinopoiskId,
            isSerial = false
        )

        return extractMovieStreams(responses, balancer, title, year)
    }

    private suspend fun extractMovieStreams(
        responses: List<LampacResponseDto>,
        balancer: String,
        defaultTitle: String,
        year: Int?
    ): List<LampacStreamInfo> {
        val results = mutableListOf<LampacStreamInfo>()

        // 1. Follow "similar" link if present
        val similarResponse = responses.firstOrNull { it.type == "similar" && !it.data.isNullOrEmpty() }
        if (similarResponse != null) {
            val bestCandidate = similarResponse.data?.find { it.year == year } ?: similarResponse.data?.firstOrNull()
            val candidateUrl = bestCandidate?.url
            if (!candidateUrl.isNullOrBlank()) {
                val nestedResponses = apiClient.fetchUrl(candidateUrl)
                return extractMovieStreams(nestedResponses, balancer, defaultTitle, year)
            }
        }

        // 2. Iterate through responses
        for (resp in responses) {
            // If the response itself is a playable stream
            if (resp.method == "play" || !resp.stream.isNullOrBlank() || (!resp.url.isNullOrBlank() && resp.method != "call")) {
                toStreamInfoFromResponse(resp, balancer, defaultTitle)?.let { results.add(it) }
            } else if (resp.method == "call") {
                if (!resp.stream.isNullOrBlank()) {
                    toStreamInfoFromResponse(resp, balancer, defaultTitle)?.let { results.add(it) }
                } else if (!resp.url.isNullOrBlank()) {
                    val calledResponses = apiClient.fetchUrl(resp.url)
                    results.addAll(extractMovieStreams(calledResponses, balancer, defaultTitle, year))
                }
            }

            // If response has data array
            resp.data?.forEach { item ->
                if (item.method == "play" || !item.stream.isNullOrBlank()) {
                    toStreamInfoFromItem(item, resp, balancer, defaultTitle)?.let { results.add(it) }
                } else if (item.method == "call") {
                    if (!item.stream.isNullOrBlank()) {
                        toStreamInfoFromItem(item, resp, balancer, defaultTitle)?.let { results.add(it) }
                    } else if (!item.url.isNullOrBlank()) {
                        val calledResponses = apiClient.fetchUrl(item.url)
                        results.addAll(extractMovieStreams(calledResponses, balancer, defaultTitle, year))
                    }
                } else if (!item.url.isNullOrBlank() && item.method != "link") {
                    toStreamInfoFromItem(item, resp, balancer, defaultTitle)?.let { results.add(it) }
                }
            }
        }

        return results
    }

    private suspend fun toStreamInfoFromItem(
        item: LampacItemDto,
        parent: LampacResponseDto,
        balancer: String,
        defaultTitle: String
    ): LampacStreamInfo? {
        var qualities = item.quality?.map { (label, url) ->
            VideoQuality(label = label, url = url)
        } ?: emptyList()

        val streamUrl = item.stream ?: qualities.firstOrNull()?.url ?: item.url ?: return null
        val translateName = item.translate ?: parent.translate

        if (qualities.isEmpty() && streamUrl.contains(".m3u8")) {
            val resolved = apiClient.resolveHls(streamUrl)
            if (resolved.qualityVariants.isNotEmpty()) {
                qualities = resolved.qualityVariants
            }
        }

        val subtitles = item.subtitles?.map { sub ->
            SubtitleTrack(
                id = sub.label,
                name = sub.label,
                language = sub.label,
                isExternal = true,
                url = sub.url
            )
        } ?: parent.subtitles?.map { sub ->
            SubtitleTrack(
                id = sub.label,
                name = sub.label,
                language = sub.label,
                isExternal = true,
                url = sub.url
            )
        } ?: emptyList()

        val audioTracks = item.voice?.mapNotNull { v ->
            val id = v.id ?: return@mapNotNull null
            AudioTrack(
                id = "${balancer}_$id",
                name = v.name ?: id,
                isDefault = v.active
            )
        } ?: parent.voice?.mapNotNull { v ->
            val id = v.id ?: return@mapNotNull null
            AudioTrack(
                id = "${balancer}_$id",
                name = v.name ?: id,
                isDefault = v.active
            )
        } ?: if (!translateName.isNullOrBlank()) {
            listOf(AudioTrack(id = "${balancer}_def", name = translateName, isDefault = true))
        } else {
            emptyList()
        }

        val title = item.title ?: item.name ?: parent.title ?: defaultTitle

        return LampacStreamInfo(
            title = title,
            translation = translateName,
            streamUrl = streamUrl,
            qualities = qualities,
            audioTracks = audioTracks,
            subtitles = subtitles,
            headers = item.headers ?: parent.headers ?: emptyMap()
        )
    }

    private suspend fun toStreamInfoFromResponse(
        resp: LampacResponseDto,
        balancer: String,
        defaultTitle: String
    ): LampacStreamInfo? {
        var qualities = resp.quality?.map { (label, url) ->
            VideoQuality(label = label, url = url)
        } ?: emptyList()

        val streamUrl = resp.stream ?: qualities.firstOrNull()?.url ?: resp.url ?: return null
        val translateName = resp.translate

        if (qualities.isEmpty() && streamUrl.contains(".m3u8")) {
            val resolved = apiClient.resolveHls(streamUrl)
            if (resolved.qualityVariants.isNotEmpty()) {
                qualities = resolved.qualityVariants
            }
        }

        val subtitles = resp.subtitles?.map { sub ->
            SubtitleTrack(
                id = sub.label,
                name = sub.label,
                language = sub.label,
                isExternal = true,
                url = sub.url
            )
        } ?: emptyList()

        val audioTracks = resp.voice?.mapNotNull { v ->
            val id = v.id ?: return@mapNotNull null
            AudioTrack(
                id = "${balancer}_$id",
                name = v.name ?: id,
                isDefault = v.active
            )
        } ?: if (!translateName.isNullOrBlank()) {
            listOf(AudioTrack(id = "${balancer}_def", name = translateName, isDefault = true))
        } else {
            emptyList()
        }

        val title = resp.title ?: defaultTitle

        return LampacStreamInfo(
            title = title,
            translation = translateName,
            streamUrl = streamUrl,
            qualities = qualities,
            audioTracks = audioTracks,
            subtitles = subtitles,
            headers = resp.headers ?: emptyMap()
        )
    }

    override suspend fun getSeasons(
        balancer: String,
        title: String,
        originalTitle: String?,
        year: Int?,
        tmdbId: Long?,
        imdbId: String?,
        kinopoiskId: Long?
    ): List<LampacSeason> {
        var responses = apiClient.fetchFromBalancer(
            balancer = balancer,
            title = title,
            originalTitle = originalTitle,
            year = year,
            tmdbId = tmdbId,
            imdbId = imdbId,
            kinopoiskId = kinopoiskId,
            isSerial = true
        )

        // If balancer responded with "similar", follow the candidate URL directly!
        val similarResponse = responses.firstOrNull { it.type == "similar" && !it.data.isNullOrEmpty() }
        if (similarResponse != null && responses.none { it.type == "season" }) {
            val bestCandidate = similarResponse.data?.find { it.year == year } ?: similarResponse.data?.firstOrNull()
            val candidateUrl = bestCandidate?.url
            if (!candidateUrl.isNullOrBlank()) {
                responses = apiClient.fetchUrl(candidateUrl)
            }
        }

        val seasonResponse = responses.firstOrNull { it.type == "season" || it.data?.any { item -> item.seasonNumber != null } == true }
            ?: return emptyList()

        val voices = seasonResponse.voice?.mapNotNull { v ->
            val id = v.id ?: return@mapNotNull null
            LampacVoice(id = id, name = v.name ?: id, isActive = v.active)
        } ?: emptyList()

        return seasonResponse.data?.mapNotNull { item ->
            val sNum = item.seasonNumber ?: return@mapNotNull null
            LampacSeason(
                seasonNumber = sNum,
                name = item.title ?: item.name ?: "$sNum сезон",
                url = item.url ?: "",
                voices = voices,
                maxQuality = seasonResponse.maxquality
            )
        } ?: emptyList()
    }

    override suspend fun getEpisodes(
        balancer: String,
        title: String,
        season: Int,
        originalTitle: String?,
        year: Int?,
        tmdbId: Long?,
        imdbId: String?,
        kinopoiskId: Long?,
        translationId: String?
    ): List<LampacEpisode> {
        var responses = apiClient.fetchFromBalancer(
            balancer = balancer,
            title = title,
            originalTitle = originalTitle,
            year = year,
            tmdbId = tmdbId,
            imdbId = imdbId,
            kinopoiskId = kinopoiskId,
            season = season,
            translationId = translationId,
            isSerial = true
        )

        val similarResponse = responses.firstOrNull { it.type == "similar" && !it.data.isNullOrEmpty() }
        if (similarResponse != null && responses.none { it.type == "episode" || it.data?.any { item -> item.episodeNumber != null } == true }) {
            val bestCandidate = similarResponse.data?.find { it.year == year } ?: similarResponse.data?.firstOrNull()
            val candidateUrl = bestCandidate?.url
            if (!candidateUrl.isNullOrBlank()) {
                responses = apiClient.fetchUrl(candidateUrl, season = season, translationId = translationId)
            }
        }

        val episodeResponse = responses.firstOrNull { it.type == "episode" || it.data?.any { item -> item.episodeNumber != null } == true }
            ?: responses.firstOrNull() ?: return emptyList()

        val voices = episodeResponse.voice?.mapNotNull { v ->
            val id = v.id ?: return@mapNotNull null
            LampacVoice(id = id, name = v.name ?: id, isActive = v.active)
        } ?: emptyList()

        return episodeResponse.data?.mapNotNull { item ->
            val epNum = item.episodeNumber ?: return@mapNotNull null
            val sNum = item.seasonNumber ?: season

            var itemQualities = item.quality?.map { (lbl, u) -> VideoQuality(lbl, u) } ?: emptyList()
            val itemSubs = item.subtitles?.map { SubtitleTrack(it.label, it.label, language = it.label, isExternal = true, url = it.url) } ?: emptyList()
            val streamUrl = item.stream ?: itemQualities.firstOrNull()?.url ?: item.url ?: ""

            if (itemQualities.isEmpty() && streamUrl.contains(".m3u8")) {
                val resolved = apiClient.resolveHls(streamUrl)
                if (resolved.qualityVariants.isNotEmpty()) {
                    itemQualities = resolved.qualityVariants
                }
            }

            LampacEpisode(
                seasonNumber = sNum,
                episodeNumber = epNum,
                name = item.name ?: item.title ?: "$epNum серия",
                title = item.title ?: item.name ?: "$epNum серия",
                url = streamUrl,
                translation = episodeResponse.translate ?: item.details,
                qualities = itemQualities,
                subtitles = itemSubs,
                voices = voices
            )
        } ?: emptyList()
    }

    override suspend fun resolveStream(descriptor: LampacSourceDescriptor): LampacStreamInfo? {
        val streams = getMovieStreams(
            balancer = descriptor.balancer,
            title = descriptor.title ?: "",
            originalTitle = descriptor.originalTitle,
            year = descriptor.year,
            tmdbId = descriptor.tmdbId,
            imdbId = descriptor.imdbId,
            kinopoiskId = descriptor.kinopoiskId
        )
        return streams.firstOrNull()
    }

    override suspend fun searchTorrents(title: String, year: Int?): List<JacRedTorrentDto> {
        return apiClient.searchTorrents(title, year)
    }
}
