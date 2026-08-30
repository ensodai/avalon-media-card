package org.ensodai.avalonmediacard.plugins.torrserver.data.network.providers

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import org.ensodai.avalonmediacard.contract.plugins.PluginLogger
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.JackettResponse
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.JackettResult

class JackettClient(
    private val httpClient: HttpClient,
    private val jackettUrl: String,
    private val apiKey: String,
    private val logger: PluginLogger
) : TorrentSearchProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun search(query: String): List<JackettResult> {
        val searchUrl = "$jackettUrl/api/v2.0/indexers/all/results"

        return try {
            val response = httpClient.get(searchUrl) {
                parameter("apikey", apiKey)
                parameter("Query", query)
                accept(ContentType.Application.Json)
                timeout {
                    requestTimeoutMillis = 120000
                }
            }

            if (response.status == HttpStatusCode.OK) {
                val responseBody = response.body<String>()
                val jackettResponse = try {
                    json.decodeFromString<JackettResponse>(responseBody)
                } catch (e: Exception) {
                    logger.error("Ошибка парсинга ответа Jackett: ${e.message}", e)
                    JackettResponse(emptyList())
                }
                jackettResponse.Results
            } else {
                logger.error("Ошибка запроса к Jackett: ${response.status}", null)
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("Ошибка сети при поиске в Jackett", e)
            emptyList()
        }
    }

    companion object {
        suspend fun testConnection(httpClient: HttpClient, url: String, apiKey: String): org.ensodai.avalonmediacard.plugins.torrserver.domain.model.ConnectionResult {
            return try {
                val response = httpClient.get("${url.trimEnd('/')}/api/v2.0/indexers/all/results") {
                    parameter("apikey", apiKey)
                    parameter("Query", "test_connection_dummy_123")
                }
                when (response.status) {
                    HttpStatusCode.OK -> org.ensodai.avalonmediacard.plugins.torrserver.domain.model.ConnectionResult.Success
                    HttpStatusCode.Forbidden, HttpStatusCode.Unauthorized -> org.ensodai.avalonmediacard.plugins.torrserver.domain.model.ConnectionResult.AuthError("Неверный API ключ")
                    else -> org.ensodai.avalonmediacard.plugins.torrserver.domain.model.ConnectionResult.NetworkError("HTTP ${response.status}")
                }
            } catch (e: Exception) {
                org.ensodai.avalonmediacard.plugins.torrserver.domain.model.ConnectionResult.NetworkError(e.message)
            }
        }
    }
}
