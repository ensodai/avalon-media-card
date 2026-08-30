package org.ensodai.avalonmediacard.plugin.recommendation

import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaCatalog
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaMetadata
import org.ensodai.avalonmediacard.contract.model.PersonMetadata
import org.ensodai.avalonmediacard.contract.model.TmdbMovieDto
import org.ensodai.avalonmediacard.contract.model.TmdbMultiSearchDto
import org.ensodai.avalonmediacard.contract.slot.EpisodeItem
import java.io.File

class FileSystemMediaCatalog(
    private val json: Json,
    private val rootPath: String = resolveMockMetadataDir()
) : MediaCatalog {

    companion object {
        fun resolveMockMetadataDir(): String {
            val candidates = listOf(
                File("test-data/metadata"),
                File("../../test-data/metadata"),
                File("../../../test-data/metadata"),
                File("движок рекомендаций мок данные/metadata"),
                File("../../движок рекомендаций мок данные/metadata")
            )
            return candidates.firstOrNull { it.exists() && it.isDirectory }?.path
                ?: "test-data/metadata"
        }
    }

    init {
        val dir = File(rootPath)
        var count = 0
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles { file -> file.extension == "json" }?.forEach { _ ->
                count++
            }
        }
        println("[DEBUG] FileSystemMediaCatalog loaded $count items from $rootPath")
    }

    override suspend fun getMediaDetails(
        key: MediaKey,
        requireSeasons: Boolean,
        requireVideos: Boolean,
        language: String
    ): MediaMetadata {
        val prefix = if (key.type == EntityType.TV) "tv_" else "movie_"
        val file = File(rootPath, "$prefix${key.id}.json")
        if (!file.exists()) {
            throw IllegalArgumentException("Mock JSON not found for $key: ${file.absolutePath}")
        }
        return json.decodeFromString(MediaMetadata.serializer(), file.readText())
    }

    override suspend fun getTrending(page: Int, language: String): List<TmdbMovieDto> = emptyList()
    
    override suspend fun getMediaDetailsBatch(
        keys: List<MediaKey>,
        requireSeasons: Boolean,
        requireVideos: Boolean,
        language: String
    ): Map<MediaKey, MediaMetadata> {
        val result = mutableMapOf<MediaKey, MediaMetadata>()
        for (key in keys) {
            try {
                result[key] = getMediaDetails(key, requireSeasons, requireVideos, language)
            } catch (e: Exception) {
                // Ignore missing or failed items
            }
        }
        return result
    }
    override suspend fun getTopRated(page: Int, language: String): List<TmdbMovieDto> = emptyList()
    override suspend fun getUpcoming(page: Int, language: String): List<TmdbMovieDto> = emptyList()
    override suspend fun getTrendingShows(page: Int, language: String): List<TmdbMovieDto> = emptyList()
    override suspend fun getPopularShows(page: Int, language: String): List<TmdbMovieDto> = emptyList()
    override suspend fun getTopRatedShows(page: Int, language: String): List<TmdbMovieDto> = emptyList()
    override suspend fun getRecommendations(key: MediaKey, page: Int, language: String): List<TmdbMovieDto> = emptyList()
    override suspend fun getSimilar(key: MediaKey, page: Int, language: String): List<TmdbMovieDto> = emptyList()
    override suspend fun searchMedia(query: String, page: Int, language: String): List<TmdbMultiSearchDto> = emptyList()
    override suspend fun getPersonDetails(key: MediaKey, language: String): PersonMetadata = throw NotImplementedError()
    override suspend fun getSeasonDetails(key: MediaKey, seasonNumber: Int, language: String): List<EpisodeItem> = emptyList()
    override suspend fun discoverMedia(
        genres: List<Int>,
        keywords: List<Int>,
        page: Int,
        isTv: Boolean,
        language: String
    ): List<TmdbMovieDto> = emptyList()

    override suspend fun discoverMediaByParams(
        params: Map<String, String>,
        targetType: EntityType,
        page: Int,
        language: String
    ): List<TmdbMovieDto> = emptyList()
}
