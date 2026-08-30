package org.ensodai.avalonmediacard.tmdb

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.slf4j.Logger

suspend inline fun <reified T> HttpClient.safeGet(
    url: String,
    logger: Logger,
    crossinline block: HttpRequestBuilder.() -> Unit = {}
): T {
    return try {
        val response = this.get(url) {
            block()
        }
        val statusCode = response.status.value
        if (statusCode in 200..299) {
            response.body<T>()
        } else {
            val errorBody = try {
                response.bodyAsText()
            } catch (e: Exception) {
                ""
            }
            val errorMsg = "HTTP error $statusCode for URL: $url. Response: $errorBody"
            logger.error(errorMsg)
            throw Exception(errorMsg)
        }
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        val errorMsg = "Network request failed for URL: $url"
        logger.error(errorMsg, e)
        throw Exception(errorMsg, e)
    }
}
