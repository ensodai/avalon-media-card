package org.ensodai.avalonmediacard.plugins.collaps

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.runBlocking
import org.ensodai.avalonmediacard.contract.plugins.DefaultPluginLogger
import org.ensodai.avalonmediacard.contract.plugins.PluginLogger
import org.ensodai.avalonmediacard.plugins.collaps.data.network.CollapsApiClient
import org.ensodai.avalonmediacard.plugins.collaps.data.repository.CollapsRepositoryImpl
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CollapsIntegrationTest {

    private val testLogger = DefaultPluginLogger("Collaps Test")

    @Test
    @Ignore("Live network test requiring live Collaps API connection")
    fun testSearchMovieAndGetHlsStream() = runBlocking {
        val client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
        }
        val apiClient = CollapsApiClient(client, testLogger)
        val repository = CollapsRepositoryImpl(apiClient)

        println("=== ПОИСК ФИЛЬМА В COLLAPS: '1+1' ===")
        val results = repository.searchMedia("1+1")
        println("Найдено результатов: ${results.size}")
        if (results.isEmpty()) return@runBlocking

        val first = results.first()
        println("Первый результат: id=${first.id}, name=${first.name}, year=${first.year}, iframe=${first.iframeUrl}")

        val parseResult = repository.getEmbedParseResult(first.iframeUrl)
        assertNotNull(parseResult, "Парсер должен вернуть результат")
        println("Direct HLS URL: ${parseResult.hlsUrl}")
        println("Аудиодорожки: ${parseResult.audioNames}")
        println("Субтитры: ${parseResult.subtitles.map { it.name }}")

        client.close()
    }
}
