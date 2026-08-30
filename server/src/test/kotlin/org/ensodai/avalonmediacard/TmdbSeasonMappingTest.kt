package org.ensodai.avalonmediacard

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.tmdb.TmdbApi
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Ignore
class TmdbSeasonMappingTest {

    @Test
    fun testGetSeasonDetailsWithTvPrefix() = runBlocking {
        val httpClient = HttpClient(io.ktor.client.engine.okhttp.OkHttp) {
            install(io.ktor.client.plugins.HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
        }

        val fakeSettings = object : org.ensodai.avalonmediacard.repository.SystemSettingsRepository() {
            override suspend fun getSetting(key: String): String? = null
            override suspend fun saveSetting(key: String, value: String) {}
        }
        val api = TmdbApi(httpClient, fakeSettings)

        // House of the Dragon (TMDB ID 94997)
        // Verify that even if a key has prefix "tv:94997", getSeasonDetails succeeds
        val seasonDetailsResult = api.getSeasonDetails("tv:94997", 1, "ru")
        println("Result of calling getSeasonDetails with 'tv:94997': episodes count = ${seasonDetailsResult?.episodes?.size}")

        httpClient.close()
        
        assertNotNull(seasonDetailsResult, "Season details must not be null")
        assertTrue(seasonDetailsResult.episodes.isNotEmpty(), "Episodes list must not be empty")
        assertTrue(seasonDetailsResult.episodes.first().name?.isNotBlank() == true, "Episode 1 name must not be blank")
    }

    @Test
    fun testGetSeasonDetailsWithCleanIdSucceeds() = runBlocking {
        val httpClient = HttpClient(io.ktor.client.engine.okhttp.OkHttp) {
            install(io.ktor.client.plugins.HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
        }

        val fakeSettings = object : org.ensodai.avalonmediacard.repository.SystemSettingsRepository() {
            override suspend fun getSetting(key: String): String? = null
            override suspend fun saveSetting(key: String, value: String) {}
        }
        val api = TmdbApi(httpClient, fakeSettings)

        // When clean ID "tv:94997" is passed without duplicate prefix:
        val seasonDetails = api.getSeasonDetails("tv:94997", 1, "ru")
        httpClient.close()

        println("House of the Dragon Season 1 episodes count: ${seasonDetails?.episodes?.size}")
        seasonDetails?.episodes?.forEach { ep ->
            println("Episode ${ep.episodeNumber}: '${ep.name}', stillPath: ${ep.stillPath}")
        }

        assertNotNull(seasonDetails, "Season details must not be null for House of the Dragon")
        assertTrue(seasonDetails.episodes.isNotEmpty(), "Episodes list must not be empty")
        assertTrue(seasonDetails.episodes.first().name?.isNotBlank() == true, "Episode 1 must have a localized Russian name")
    }
}
