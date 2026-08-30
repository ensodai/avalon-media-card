package org.ensodai.avalonmediacard.routes

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*

fun Route.imageProxyRoutes(httpClient: HttpClient) {
    get("/api/media/image/{size}/{path...}") {
        val size = call.parameters["size"] ?: "w500"
        val pathList = call.parameters.getAll("path")
        if (pathList.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, "Missing image path")
            return@get
        }
        val imagePath = pathList.joinToString("/")
        val tmdbUrl = "https://image.tmdb.org/t/p/$size/$imagePath"

        try {
            val response = httpClient.get(tmdbUrl)
            if (response.status.isSuccess()) {
                val contentType = response.contentType() ?: ContentType.Image.JPEG
                call.response.header(HttpHeaders.CacheControl, "public, max-age=604800, immutable")
                call.respondBytesWriter(contentType, response.status) {
                    response.bodyAsChannel().copyTo(this)
                }
            } else {
                call.respond(response.status)
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadGateway, "Failed to fetch image from TMDB: ${e.message}")
        }
    }
}
