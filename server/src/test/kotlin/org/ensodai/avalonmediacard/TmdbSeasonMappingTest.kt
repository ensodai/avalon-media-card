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
import kotlin.test.Test
import kotlin.test.Ignore
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
    fun testGetSeasonDetailsForShow119051() = runBlocking {
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

        val movieDetails = api.getMovieDetails("tv:119051", language = "ru")
        println("MovieDetails for 119051: $movieDetails")

        val season1 = api.getSeasonDetails("tv:119051", 1, "ru")
        println("Season 1 episodes count for 119051: ${season1?.episodes?.size}")

        httpClient.close()
    }
}
