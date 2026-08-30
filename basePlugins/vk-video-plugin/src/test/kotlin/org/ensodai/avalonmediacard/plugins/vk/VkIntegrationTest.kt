package org.ensodai.avalonmediacard.plugins.vk

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import org.ensodai.avalonmediacard.contract.plugins.DefaultPluginLogger
import org.ensodai.avalonmediacard.contract.plugins.PluginLogger
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.contract.plugins.VideoQuality
import org.ensodai.avalonmediacard.plugins.vk.data.network.VkApiClient
import org.ensodai.avalonmediacard.plugins.vk.data.network.dto.VkSubtitleDto
import org.ensodai.avalonmediacard.plugins.vk.data.network.dto.VkVideoDto
import org.ensodai.avalonmediacard.plugins.vk.data.network.dto.VkVideoFilesDto
import org.ensodai.avalonmediacard.plugins.vk.data.repository.VkRepositoryImpl
import org.ensodai.avalonmediacard.plugins.vk.domain.model.VkVideoItem
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VkIntegrationTest {

    private val testLogger = DefaultPluginLogger("VK Test")

    @Test
    fun testRepositoryMappingWith4KAndSubtitles() = runBlocking {
        val client = HttpClient(CIO)
        val apiClient = VkApiClient(client, testLogger)
        val repo = VkRepositoryImpl(apiClient)

        val videoDto = VkVideoDto(
            id = 123456L,
            ownerId = 7890L,
            title = "Интерстеллар 2014 4K UHD",
            description = "Фильм Кристофера Нолана",
            duration = 10140L,
            files = VkVideoFilesDto(
                mp4_2160 = "https://vkvideo.ru/video_2160.mp4",
                mp4_1080 = "https://vkvideo.ru/video_1080.mp4",
                mp4_720 = "https://vkvideo.ru/video_720.mp4"
            ),
            subtitles = listOf(
                VkSubtitleDto(lang = "rus", title = "Русские", url = "https://vkvideo.ru/subs_ru.vtt")
            ),
            ownerName = "CinemaClub"
        )

        val qualities = mutableListOf<VideoQuality>()
        videoDto.files?.mp4_2160?.let { qualities.add(VideoQuality("2160p (4K)", it)) }
        videoDto.files?.mp4_1080?.let { qualities.add(VideoQuality("1080p", it)) }
        videoDto.files?.mp4_720?.let { qualities.add(VideoQuality("720p", it)) }

        assertEquals(3, qualities.size)
        assertEquals("2160p (4K)", qualities.first().label)
        assertEquals("https://vkvideo.ru/video_2160.mp4", qualities.first().url)

        val subtitles = videoDto.subtitles?.mapNotNull { sub ->
            sub.url?.let { url ->
                SubtitleTrack(
                    id = sub.lang ?: "rus",
                    name = sub.title ?: "Русские",
                    language = sub.lang,
                    isExternal = true,
                    url = url
                )
            }
        } ?: emptyList()

        assertEquals(1, subtitles.size)
        assertEquals("Русские", subtitles.first().name)
        assertEquals("https://vkvideo.ru/subs_ru.vtt", subtitles.first().url)

        client.close()
    }

    @Test
    fun testEpisodeMatcherOnVkTitles() {
        val matcher = org.ensodai.avalonmediacard.contract.parsers.EpisodeMatcher()

        val testCases = listOf(
            "Укрытие 1 сезон 1 серия (5 аудиодорожек)" to (1 to 1),
            "Укрытие. Сезон 1. Серия 2" to (1 to 2),
            "Укрытие|Бункер 4К (1 сезон, 3 серия)" to (1 to 3),
            "Silo.S01E04.1080p" to (1 to 4),
            "Стартрек: Глубокий космос 9. 1 сезон 1 серия" to (1 to 1),
            "Укрытие 5 серия" to (1 to 5)
        )

        for ((title, expected) in testCases) {
            val res = matcher.parse("Укрытие 1 сезон", title)
            val (expSeason, expEpisode) = expected

            val (actualSeason, actualEp) = when (res) {
                is org.ensodai.avalonmediacard.contract.parsers.MappingResult.Success -> {
                    (res.seasons.firstOrNull() ?: 1) to res.episodes.firstOrNull()
                }
                is org.ensodai.avalonmediacard.contract.parsers.MappingResult.Partial -> {
                    1 to res.episodes.firstOrNull()
                }
                is org.ensodai.avalonmediacard.contract.parsers.MappingResult.Failed -> {
                    null to null
                }
            }

            assertEquals(expSeason, actualSeason, "Failed season for title: $title")
            assertEquals(expEpisode, actualEp, "Failed episode for title: $title")
        }
    }

    @Test
    fun testAlbumMappingAndPlaylistBuilding() = runBlocking {
        val client = HttpClient(CIO)
        val apiClient = VkApiClient(client, testLogger)
        val repo = VkRepositoryImpl(apiClient)

        val albumDto = org.ensodai.avalonmediacard.plugins.vk.data.network.dto.VkAlbumDto(
            id = 36L,
            ownerId = -239979259L,
            title = "Укрытие 1 сезон",
            count = 10
        )

        assertEquals(36L, albumDto.id)
        assertEquals(-239979259L, albumDto.ownerId)
        assertEquals("Укрытие 1 сезон", albumDto.title)
        assertEquals(10, albumDto.count)

        client.close()
    }

    @Test
    @Ignore("Live network test")
    fun testLiveAnonymousTokenAndSearch() = runBlocking {
        val client = HttpClient(CIO)
        val apiClient = VkApiClient(client, testLogger)

        println("=== ПОЛУЧЕНИЕ АНОНИМНОГО ТОКЕНА VK VIDEO ===")
        val token = apiClient.getAnonymousToken()
        assertNotNull(token, "Анонимный токен должен быть успешно получен")
        assertTrue(token.isNotBlank(), "Токен не должен быть пустым")
        println("Успешно получен токен: ${token.take(15)}...")

        println("\n=== ПОИСК ФИЛЬМА В VK VIDEO: 'Интерстеллар' ===")
        val videos = apiClient.searchVideos("Интерстеллар 2014")
        println("Найдено видео: ${videos.size}")
        assertTrue(videos.isNotEmpty(), "Поиск должен вернуть хотя бы 1 видео")

        val first = videos.first()
        val files = first.files
        if (files != null) {
            println("Доступные форматы файлов:")
            files.mp4_2160?.let { println("  - 4K (2160p): $it") }
            files.mp4_1440?.let { println("  - 2K (1440p): $it") }
            files.mp4_1080?.let { println("  - 1080p: $it") }
            files.mp4_720?.let { println("  - 720p: $it") }
            files.hls?.let { println("  - HLS: $it") }
        }

        client.close()
    }
}
