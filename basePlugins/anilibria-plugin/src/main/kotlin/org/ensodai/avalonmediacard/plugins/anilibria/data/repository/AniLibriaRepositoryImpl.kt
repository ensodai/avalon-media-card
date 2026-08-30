package org.ensodai.avalonmediacard.plugins.anilibria.data.repository

import org.ensodai.avalonmediacard.plugins.anilibria.data.network.AniLibriaApiClient
import org.ensodai.avalonmediacard.plugins.anilibria.domain.model.AniLibriaEpisode
import org.ensodai.avalonmediacard.plugins.anilibria.domain.model.AniLibriaRelease
import org.ensodai.avalonmediacard.plugins.anilibria.domain.model.AniLibriaReleaseDetails
import org.ensodai.avalonmediacard.plugins.anilibria.domain.repository.AniLibriaRepository

/**
 * **AniLibria Repository Implementation**
 *
 * Implements [AniLibriaRepository] by delegating to [AniLibriaApiClient] and mapping
 * network DTOs into strongly typed Domain models with in-memory caching.
 *
 * @property apiClient The low-level Ktor HTTP client.
 * @property baseUrl Hostname for resolving relative image and asset URLs.
 */
class AniLibriaRepositoryImpl(
    private val apiClient: AniLibriaApiClient,
    private val baseUrl: String = "https://anilibria.top"
) : AniLibriaRepository {

    override suspend fun searchReleases(query: String): List<AniLibriaRelease> {
        val dtos = apiClient.searchReleases(query)
        return dtos.map { dto ->
            val posterSrc = dto.poster?.src ?: dto.poster?.preview ?: dto.poster?.thumbnail
            val fullPosterUrl = if (!posterSrc.isNullOrBlank() && !posterSrc.startsWith("http")) {
                "$baseUrl$posterSrc"
            } else {
                posterSrc
            }

            AniLibriaRelease(
                id = dto.id,
                titleRu = dto.name?.main ?: "",
                titleEn = dto.name?.english,
                year = dto.year,
                alias = dto.alias,
                posterUrl = fullPosterUrl,
                episodesTotal = dto.episodesTotal
            )
        }
    }

    override suspend fun getReleaseDetails(releaseId: Long): AniLibriaReleaseDetails? {
        val dto = apiClient.getReleaseDetails(releaseId) ?: return null
        val episodes = dto.episodes?.map { epDto ->
            AniLibriaEpisode(
                id = epDto.id,
                ordinal = epDto.ordinal ?: 1,
                name = epDto.name,
                durationSeconds = epDto.duration,
                hls480 = epDto.hls480,
                hls720 = epDto.hls720,
                hls1080 = epDto.hls1080
            )
        } ?: emptyList()

        return AniLibriaReleaseDetails(
            id = dto.id,
            titleRu = dto.name?.main ?: "",
            titleEn = dto.name?.english,
            year = dto.year,
            alias = dto.alias,
            episodes = episodes
        )
    }
}
