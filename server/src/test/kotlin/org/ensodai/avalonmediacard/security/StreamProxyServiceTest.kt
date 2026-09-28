package org.ensodai.avalonmediacard.security

import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.contract.plugins.VideoQuality
import org.ensodai.avalonmediacard.repository.SystemSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class StreamProxyServiceTest {

    private class MockSystemSettingsRepository : SystemSettingsRepository() {
        private val settings = mutableMapOf<String, String>()
        override suspend fun getSetting(key: String): String? = settings[key]
        override suspend fun saveSetting(key: String, value: String) { settings[key] = value }
    }

    private fun createService(): Pair<StreamProxyService, StreamTokenService> {
        val mockSettings = MockSystemSettingsRepository()
        val tokenService = StreamTokenService(mockSettings)
        val proxyService = StreamProxyService(tokenService)
        return proxyService to tokenService
    }

    @Test
    fun testSanitizeStreamWrapsPrimaryUrlAndAllQualitiesAndSubtitles() {
        val (proxyService, tokenService) = createService()
        val userId = Uuid.random()
        val headers = mapOf("Referer" to "https://vkvideo.ru/", "Origin" to "https://vkvideo.ru")

        val rawStream = MediaStream(
            id = "vk_12345",
            title = "Test Movie",
            url = "https://vk3-3.vkuser.net/video4k.mp4",
            type = StreamType.DirectUrl,
            quality = "2160p (4K)",
            sourceName = "VK Video",
            headers = headers,
            qualityVariants = listOf(
                VideoQuality(label = "2160p (4K)", url = "https://vk3-3.vkuser.net/video4k.mp4"),
                VideoQuality(label = "1080p", url = "https://vk3-3.vkuser.net/video1080.mp4"),
                VideoQuality(label = "720p", url = "https://vk3-3.vkuser.net/video720.mp4")
            ),
            subtitleTracks = listOf(
                SubtitleTrack(id = "ru", name = "Русский", url = "https://vk.com/subs_ru.vtt", language = "ru")
            )
        )

        val sanitized = proxyService.sanitizeStream(rawStream, userId)

        // 1. Проверяем основной URL
        assertTrue(sanitized.url.startsWith("/api/stream-proxy/"), "Основной URL должен идти через прокси")
        assertFalse(sanitized.url.contains("vkuser.net"), "Сырой CDN URL не должен светиться в открытом виде")

        // 2. Проверяем все варианты качества
        assertEquals(3, sanitized.qualityVariants.size)
        for (variant in sanitized.qualityVariants) {
            assertTrue(variant.url.startsWith("/api/stream-proxy/"), "Качество ${variant.label} должно идти через прокси")
            assertFalse(variant.url.contains("vkuser.net"), "Сырой CDN URL качества не должен быть открытым")
            assertTrue(variant.url.endsWith("/video.mp4"))
        }

        // Проверяем расшифровку токена для 1080p
        val token1080 = sanitized.qualityVariants[1].url.substringAfter("/api/stream-proxy/").substringBefore("/video.mp4")
        val payload1080 = tokenService.decryptAndValidate(token1080)
        assertNotNull(payload1080)
        assertEquals("https://vk3-3.vkuser.net/video1080.mp4", payload1080.targetUrl)
        assertEquals(userId, payload1080.userId)
        assertEquals("https://vkvideo.ru/", payload1080.headers["Referer"])
        assertEquals("https://vkvideo.ru", payload1080.headers["Origin"])

        // 3. Проверяем субтитры
        assertEquals(1, sanitized.subtitleTracks.size)
        val sub = sanitized.subtitleTracks[0]
        assertNotNull(sub.url)
        assertTrue(sub.url!!.startsWith("/api/stream-proxy/"))
        assertTrue(sub.url!!.endsWith("/subtitles.vtt"))

        val subToken = sub.url!!.substringAfter("/api/stream-proxy/").substringBefore("/subtitles.vtt")
        val subPayload = tokenService.decryptAndValidate(subToken)
        assertNotNull(subPayload)
        assertEquals("https://vk.com/subs_ru.vtt", subPayload.targetUrl)
        assertEquals(headers, subPayload.headers)
    }

    @Test
    fun testSanitizePlaylistSanitizesAllEpisodes() {
        val (proxyService, _) = createService()
        val userId = Uuid.random()
        val defaultHeaders = mapOf("Referer" to "https://kinokrad.my/")

        val playlist = listOf(
            MediaStream(
                id = "ep1",
                title = "Episode 1",
                url = "https://cdn.example.com/ep1.m3u8",
                type = StreamType.Hls,
                sourceName = "Test"
            ),
            MediaStream(
                id = "ep2",
                title = "Episode 2",
                url = "https://cdn.example.com/ep2.m3u8",
                type = StreamType.Hls,
                sourceName = "Test"
            )
        )

        val sanitizedPlaylist = proxyService.sanitizePlaylist(playlist, userId, defaultHeaders)

        assertEquals(2, sanitizedPlaylist.size)
        for (ep in sanitizedPlaylist) {
            assertTrue(ep.url.startsWith("/api/stream-proxy/"))
            assertTrue(ep.url.endsWith("/playlist.m3u8"))
        }
    }

    @Test
    fun testSanitizeUrlIdempotencyAndPassthrough() {
        val (proxyService, _) = createService()
        val userId = Uuid.random()

        // GST URL не должен трогаться
        assertEquals("/gst/live/master.m3u8", proxyService.sanitizeUrl("/gst/live/master.m3u8", userId))

        // Уже обернутый токенизированный URL не должен оборачиваться повторно
        val alreadyWrapped = "/api/stream-proxy/abc123token/video.mp4"
        assertEquals(alreadyWrapped, proxyService.sanitizeUrl(alreadyWrapped, userId))

        // Пустая строка
        assertEquals("", proxyService.sanitizeUrl("", userId))
    }
}
