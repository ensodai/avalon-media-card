package org.ensodai.avalonmediacard

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.tmdb.TmdbApi
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue

@Ignore
class TmdbApiTest {

    @Test
    fun testTrendingMovies() = runBlocking {
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
        val trending = api.getTrendingMovies(1)
        println("TRENDING MOVIES COUNT: ${trending.size}")
        trending.forEach { movie ->
            println("MOVIE: ${movie.title ?: movie.name}")
        }

        httpClient.close()
        assertTrue(trending.isNotEmpty(), "Trending movies should not be empty")
    }

    @Test
    fun testSimilarAndRecommendations() = runBlocking {
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
        val movieId = "550" // Fight Club

        val recs = api.getRecommendations(movieId, 1)
        println("RECOMMENDATIONS COUNT: ${recs.size}")
        recs.forEach { movie ->
            println("REC MOVIE: ${movie.title ?: movie.name}")
        }

        val similar = api.getSimilarMovies(movieId, 1)
        println("SIMILAR COUNT: ${similar.size}")
        similar.forEach { movie ->
            println("SIMILAR MOVIE: ${movie.title ?: movie.name}")
        }

        httpClient.close()
        assertTrue(recs.isNotEmpty(), "Recommendations should not be empty")
        assertTrue(similar.isNotEmpty(), "Similar movies should not be empty")
    }
}
