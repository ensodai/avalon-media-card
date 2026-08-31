package org.ensodai.avalonmediacard.mockgen

import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaCatalog
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.module
import org.junit.Ignore
import org.junit.Test
import org.koin.ktor.ext.getKoin
import java.io.File

/**
 * Утилита для выгрузки реальных метаданных из TMDB в JSON-моки для тестов рекомендаций.
 */
class MockDataGenerator {
    @Test
    @Ignore("Live network data generator")
    fun generateMocks() = testApplication {
        application {
            // Мы запускаем Ktor приложение в тестовом режиме, чтобы отработал Koin и инициализировалась БД.
            module()

            val catalog = getKoin().get<MediaCatalog>()

            println("Discovering popular and top-rated media from TMDB...")
            val moviesToFetch = mutableSetOf<String>()
            // Top 40 movies
            moviesToFetch.addAll(catalog.getTopRated(1).map { it.id.toString() })
            moviesToFetch.addAll(catalog.getTopRated(2).map { it.id.toString() })
            // Trending 20 movies
            moviesToFetch.addAll(catalog.getTrending(1).map { it.id.toString() })
            // Popular 20 TV Shows
            val showsToFetch = mutableSetOf<String>()
            showsToFetch.addAll(catalog.getPopularShows(1).map { it.id.toString() })

            println("Found ${moviesToFetch.size} movies and ${showsToFetch.size} shows to fetch metadata for.")

            val json = Json { prettyPrint = true }
            val outputDir = File("движок рекомендаций мок данные/metadata")
            if (!outputDir.exists()) outputDir.mkdirs()

            run {
                var count = 1
                val total = moviesToFetch.size + showsToFetch.size
                for (id in moviesToFetch) {
                    try {
                        val key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, id)
                        println("[$count/$total] Fetching MOVIE $id...")
                        val metadata = catalog.getMediaDetails(key, requireSeasons = false, requireVideos = false)
                        val jsonStr = json.encodeToString(metadata)
                        val file = File(outputDir, "movie_$id.json")
                        file.writeText(jsonStr)
                        count++
                    } catch (e: Exception) {
                        println("Failed to fetch MOVIE $id: ${e.message}")
                    }
                }
                for (id in showsToFetch) {
                    try {
                        val key = MediaKey(MediaProvider.Tmdb, EntityType.TV, id)
                        println("[$count/$total] Fetching TV SHOW $id...")
                        val metadata = catalog.getMediaDetails(key, requireSeasons = false, requireVideos = false)
                        val jsonStr = json.encodeToString(metadata)
                        val file = File(outputDir, "tv_$id.json")
                        file.writeText(jsonStr)
                        count++
                    } catch (e: Exception) {
                        println("Failed to fetch TV SHOW $id: ${e.message}")
                    }
                }
            }

            println("Done generating mocks!")
        }
    }
}
