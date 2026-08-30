package org.ensodai.avalonmediacard

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.ensodai.avalonmediacard.security.StreamTokenService
import org.koin.ktor.ext.inject
import java.util.Base64
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class StreamProxyTest {

    @Test
    fun testBase64UrlEncoderDecoderCompatibility() {
        val originalUrl = "http://127.0.0.1:8090/stream/video.mp4?link=caf36855457a3ca63267f83822e54f0d7de5192f&index=48&play"
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(originalUrl.toByteArray())
        val decoded = String(Base64.getUrlDecoder().decode(encoded))
        assertEquals(originalUrl, decoded)
    }

    @Test
    fun testBase64WithDashAndUnderscore() {
        val originalUrl = "http://127.0.0.1:8090/stream/video_test-1.mkv?link=caf36855457a3ca63267f83822e54f0d7de5192f&index=48&play"
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(originalUrl.toByteArray())
        val decoded = String(Base64.getUrlDecoder().decode(encoded))
        assertEquals(originalUrl, decoded)
    }

    @Test
    @Ignore("Live network test requiring remote TorrServer connection")
    fun testStreamProxyRouteHeadMethodHandling() = testApplication {
        application {
            module()
        }
        val streamTokenService by application.inject<StreamTokenService>()
        val targetUrl = "http://127.0.0.1:8090/stream/video.mkv?link=caf36855457a3ca63267f83822e54f0d7de5192f&index=48&play"
        val auth = "Basic dXNlcjpwYXNzd29yZA=="
        val token = streamTokenService.generateToken(
            targetUrl = targetUrl,
            userId = null,
            authHeader = auth
        )
        
        val response = client.head("/api/stream-proxy/$token/video.mkv")
        
        println("STREAM_PROXY_HEAD_STATUS: ${response.status}")
        assertEquals(HttpStatusCode.OK, response.status)
    }

}
