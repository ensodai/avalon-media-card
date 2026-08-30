package org.ensodai.avalonmediacard.plugin.recommendation

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TmdbMappingTest {

    private fun getTmdbToken(): String {
        val fromEnv = System.getenv("TMDB_READ_TOKEN")
        if (!fromEnv.isNullOrBlank()) return fromEnv.trim().removeSurrounding("\"")

        val candidates = listOf(
            File(".env"),
            File("../../.env"),
            File("../../../.env")
        )
        for (envFile in candidates) {
            if (envFile.exists()) {
                val tokenLine = envFile.readLines().firstOrNull { it.startsWith("TMDB_READ_TOKEN=") }
                if (tokenLine != null) {
                    val token = tokenLine.substringAfter("=").trim().removeSurrounding("\"")
                    if (token.isNotEmpty()) return token
                }
            }
        }
        return ""
    }

    @Test
    fun `test hardcoded genre IDs match TMDB API realities`() {
        val token = getTmdbToken()
        if (token.isBlank()) {
            println("Skipping TmdbMappingTest: TMDB_READ_TOKEN not configured")
            return
        }
        val url = URL("https://api.themoviedb.org/3/genre/movie/list?language=en-US")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("accept", "application/json")

        val responseCode = connection.responseCode
        assertEquals(200, responseCode, "TMDB API failed to return genres")

        val responseString = connection.inputStream.bufferedReader().readText()
        println("TMDB Genres Response: $responseString")

        // 28 = Action
        assertTrue(
            responseString.contains("\"id\":28") && responseString.contains("Action"),
            "Жанр 28 больше не является Action на TMDB!"
        )

        // 16 = Animation
        assertTrue(
            responseString.contains("\"id\":16") && responseString.contains("Animation"),
            "Жанр 16 больше не является Animation на TMDB!"
        )

        // 878 = Science Fiction
        assertTrue(
            responseString.contains("\"id\":878") && responseString.contains("Science Fiction"),
            "Жанр 878 больше не является Science Fiction на TMDB!"
        )

        // 99 = Documentary
        assertTrue(
            responseString.contains("\"id\":99") && responseString.contains("Documentary"),
            "Жанр 99 больше не является Documentary на TMDB!"
        )
    }
}
