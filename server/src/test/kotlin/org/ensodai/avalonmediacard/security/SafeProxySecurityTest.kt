package org.ensodai.avalonmediacard.security

import org.ensodai.avalonmediacard.repository.SystemSettingsRepository
import org.ensodai.avalonmediacard.routes.rewriteM3u8Playlist
import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class SafeProxySecurityTest {

    @Test
    fun testSsrfIpValidatorBlocksPrivateAndSpecialIps() {
        // Loopback
        assertFalse(SsrfIpValidator.isAllowed(InetAddress.getByName("127.0.0.1")))
        assertFalse(SsrfIpValidator.isAllowed(InetAddress.getByName("127.128.0.1")))

        // Private RFC 1918
        assertFalse(SsrfIpValidator.isAllowed(InetAddress.getByName("10.0.0.5")))
        assertFalse(SsrfIpValidator.isAllowed(InetAddress.getByName("172.16.0.1")))
        assertFalse(SsrfIpValidator.isAllowed(InetAddress.getByName("172.31.255.255")))
        assertFalse(SsrfIpValidator.isAllowed(InetAddress.getByName("192.168.1.100")))

        // Cloud Metadata & Link-local
        assertFalse(SsrfIpValidator.isAllowed(InetAddress.getByName("169.254.169.254")))
        assertFalse(SsrfIpValidator.isAllowed(InetAddress.getByName("169.254.1.1")))

        // Carrier-grade NAT
        assertFalse(SsrfIpValidator.isAllowed(InetAddress.getByName("100.64.0.1")))

        // Multicast & Broadcast
        assertFalse(SsrfIpValidator.isAllowed(InetAddress.getByName("224.0.0.1")))
        assertFalse(SsrfIpValidator.isAllowed(InetAddress.getByName("255.255.255.255")))

        // IPv6
        assertFalse(SsrfIpValidator.isAllowed(InetAddress.getByName("::1")))
        assertFalse(SsrfIpValidator.isAllowed(InetAddress.getByName("::ffff:127.0.0.1")))
    }

    @Test
    fun testSsrfIpValidatorAllowsPublicIps() {
        assertTrue(SsrfIpValidator.isAllowed(InetAddress.getByName("8.8.8.8")))
        assertTrue(SsrfIpValidator.isAllowed(InetAddress.getByName("1.1.1.1")))
        assertTrue(SsrfIpValidator.isAllowed(InetAddress.getByName("142.250.180.206")))
    }

    private class MockSystemSettingsRepository : SystemSettingsRepository() {
        private val settings = mutableMapOf<String, String>()
        override suspend fun getSetting(key: String): String? = settings[key]
        override suspend fun saveSetting(key: String, value: String) { settings[key] = value }
    }

    @Test
    fun testStreamTokenServiceEncryptDecryptAndValidate() {
        val mockSettings = MockSystemSettingsRepository()
        val tokenService = StreamTokenService(mockSettings)

        val targetUrl = "https://cdn.example.com/hls/master.m3u8"
        val userId = Uuid.random()
        val headers = mapOf("Referer" to "https://kinokrad.my/", "User-Agent" to "Avalon/1.0")
        val authHeader = "Bearer secret_token_123"

        val token = tokenService.generateToken(
            targetUrl = targetUrl,
            userId = userId,
            headers = headers,
            authHeader = authHeader,
            ttlSeconds = 300
        )

        // URL не должен содержать открытый текст
        assertFalse(token.contains("cdn.example.com"))
        assertFalse(token.contains("secret_token_123"))

        val payload = tokenService.decryptAndValidate(token)
        assertNotNull(payload)
        assertEquals(targetUrl, payload.targetUrl)
        assertEquals(userId, payload.userId)
        assertEquals(headers, payload.headers)
        assertEquals(authHeader, payload.authHeader)
    }

    @Test
    fun testStreamTokenServiceRejectsTamperedOrExpiredToken() {
        val mockSettings = MockSystemSettingsRepository()
        val tokenService = StreamTokenService(mockSettings)

        // Поддельный токен
        assertNull(tokenService.decryptAndValidate("invalid_tampered_token_xyz"))

        // Истекший токен (TTL = -10 секунд)
        val expiredToken = tokenService.generateToken(
            targetUrl = "https://cdn.example.com/movie.mp4",
            userId = null,
            ttlSeconds = -10
        )
        assertNull(tokenService.decryptAndValidate(expiredToken))
    }

    @Test
    fun testM3u8RewriterWithSlidingTtl() {
        val mockSettings = MockSystemSettingsRepository()
        val tokenService = StreamTokenService(mockSettings)
        val userId = Uuid.random()

        val sampleM3u8 = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="Russian",DEFAULT=YES,URI="audio/ru.m3u8"
            #EXT-X-KEY:METHOD=AES-128,URI="https://keys.cdn.com/key.bin"
            #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=1280x720
            720p/index.m3u8
            #EXTINF:10.0,
            segment_01.ts
        """.trimIndent()

        val rewritten = rewriteM3u8Playlist(
            content = sampleM3u8,
            baseUrlStr = "https://origin.cdn.com/video/master.m3u8",
            userId = userId,
            flags = 0x00,
            customHeaders = emptyMap(),
            authHeader = null,
            tokenService = tokenService
        )

        // Все дочерние ссылки должны быть переписаны на /api/stream-proxy/{token}/...
        assertTrue(rewritten.contains("/api/stream-proxy/"))
        assertFalse(rewritten.contains("720p/index.m3u8"))
        assertFalse(rewritten.contains("segment_01.ts"))
        assertFalse(rewritten.contains("audio/ru.m3u8"))
        assertFalse(rewritten.contains("https://keys.cdn.com/key.bin"))

        // Извлекаем токен из переписанной строки сегмента и проверяем, что он валиден
        val tokenSegment = rewritten.lines().first { it.contains("segment.ts") }
        val extractedToken = tokenSegment.substringAfter("/api/stream-proxy/").substringBefore("/segment.ts")
        val payload = tokenService.decryptAndValidate(extractedToken)
        assertNotNull(payload)
        assertEquals("https://origin.cdn.com/video/segment_01.ts", payload.targetUrl)
        assertEquals(userId, payload.userId)
    }

    @Test
    fun testMpdRewriter() {
        val mockSettings = MockSystemSettingsRepository()
        val tokenService = StreamTokenService(mockSettings)
        val userId = Uuid.random()

        val sampleMpd = """
            <MPD xmlns="urn:mpeg:dash:schema:mpd:2011" minBufferTime="PT1.5S">
              <BaseURL>https://dash.cdn.com/content/</BaseURL>
              <Period>
                <AdaptationSet mimeType="video/mp4">
                  <Representation id="1" bandwidth="1000000">
                    <SegmentTemplate media="chunk_${'$'}Number${'$'}.m4s" initialization="init.m4s" />
                  </Representation>
                </AdaptationSet>
              </Period>
            </MPD>
        """.trimIndent()

        val rewritten = org.ensodai.avalonmediacard.routes.rewriteMpdManifest(
            content = sampleMpd,
            baseUrlStr = "https://dash.cdn.com/content/manifest.mpd",
            userId = userId,
            flags = 0x00,
            customHeaders = emptyMap(),
            authHeader = null,
            tokenService = tokenService
        )

        assertTrue(rewritten.contains("<BaseURL>/api/stream-proxy/"))
        assertFalse(rewritten.contains("<BaseURL>https://dash.cdn.com/content/</BaseURL>"))

        val extractedToken = rewritten.substringAfter("<BaseURL>/api/stream-proxy/").substringBefore("/</BaseURL>")
        val payload = tokenService.decryptAndValidate(extractedToken)
        assertNotNull(payload)
        assertEquals("https://dash.cdn.com/content/", payload.targetUrl)
        assertEquals(userId, payload.userId)
    }
}
