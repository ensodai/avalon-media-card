package org.ensodai.avalonmediacard.plugins.torrserver.data.network.providers

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.plugins.PluginLogger
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.JackettResult
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.ProwlarrResult
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.toJackettResult

class ProwlarrClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val apiKey: String,
    private val logger: PluginLogger
) : TorrentSearchProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun search(query: String): List<JackettResult> {
        val searchUrl = "${baseUrl.trimEnd('/')}/api/v1/search"

        return try {
            val response = httpClient.get(searchUrl) {
                header("X-Api-Key", apiKey)
                parameter("query", query)
                parameter("type", "search")
                accept(ContentType.Application.Json)
                timeout {
                    requestTimeoutMillis = 120000
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val responseBody = response.body<String>()
                val prowlarrResults = try {
                    json.decodeFromString<List<ProwlarrResult>>(responseBody)
                } catch (e: Exception) {
                    logger.error("Ошибка парсинга ответа Prowlarr: ${e.message}", e)
                    emptyList()
                }
                prowlarrResults.map { it.toJackettResult() }
            } else {
                logger.error("Ошибка запроса к Prowlarr: ${response.status}", null)
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("Ошибка сети при поиске в Prowlarr", e)
            emptyList()
        }
    }

    companion object {
        suspend fun testConnection(httpClient: HttpClient, url: String, apiKey: String): org.ensodai.avalonmediacard.plugins.torrserver.domain.model.ConnectionResult {
            return try {
                val response = httpClient.get("${url.trimEnd('/')}/api/v1/system/status") {
                    header("X-Api-Key", apiKey)
                }
                when (response.status) {
                    HttpStatusCode.OK -> org.ensodai.avalonmediacard.plugins.torrserver.domain.model.ConnectionResult.Success
                    HttpStatusCode.Unauthorized -> org.ensodai.avalonmediacard.plugins.torrserver.domain.model.ConnectionResult.AuthError("Неверный API ключ")
                    else -> org.ensodai.avalonmediacard.plugins.torrserver.domain.model.ConnectionResult.NetworkError("HTTP ${response.status}")
                }
            } catch (e: Exception) {
                org.ensodai.avalonmediacard.plugins.torrserver.domain.model.ConnectionResult.NetworkError(e.message)
            }
        }
    }
}
