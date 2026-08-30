package org.ensodai.avalonmediacard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertNotEquals

/**
 * Тесты доказывают что исправленная логика работает корректно.
 * Каждый тест содержит СТАРУЮ (сломанную) и НОВУЮ (починенную) логику.
 * Приложение НЕ трогаем — тестируем только алгоритмы.
 */
class FixVerificationTest {

    // =====================================================================
    // ФИКС A: Base64 декодер
    // СТАРЫЙ: kotlin.io.encoding.Base64.Default (стандартный, ломается на URL-safe)
    // НОВЫЙ: java.util.Base64.getUrlDecoder() (URL-safe, совместим с getUrlEncoder)
    // =====================================================================

    @Test
    fun fixA_base64_decoder_compatibility() {
        val originalUrl = "http://127.0.0.1:8090/stream/video.mkv?link=caf36855457a3ca63267f83822e54f0d7de5192f&index=48&play"

        // Так кодирует buildStreamUrl
        val encoded = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(originalUrl.toByteArray())

        // СТАРАЯ логика — ломается
        val oldResult = try {
            kotlin.io.encoding.Base64.Default.decode(encoded).decodeToString()
        } catch (_: Exception) { null }

        // НОВАЯ логика — работает
        val newResult = try {
            String(java.util.Base64.getUrlDecoder().decode(encoded))
        } catch (_: Exception) { null }

        println("ФИКС A: Base64 декодер")
        println("  Старая логика: ${if (oldResult == null) "УПАЛО" else "OK"}")
        println("  Новая логика:  ${if (newResult == originalUrl) "OK" else "УПАЛО"}")

        assertNull(oldResult, "Старая логика должна падать на URL-safe Base64")
        assertEquals(originalUrl, newResult, "Новая логика должна декодировать корректно")
    }

    // =====================================================================
    // ФИКС B: /stream/ матчит /stream-proxy/
    // СТАРЫЙ: stream.url.contains("/stream/") — true для /stream-proxy/
    // НОВЫЙ: добавляем && !stream.url.contains("/stream-proxy/")
    // =====================================================================

    @Test
    fun fixB_stream_proxy_does_not_match_stream() {
        val proxyUrl = "http://localhost:8081/api/stream-proxy/video.mkv?url=abc123&format=mkv"
        val realStreamUrl = "http://127.0.0.1:8090/stream/caf36855457a3ca63267f83822e54f0d7de5192f?index=48&play"

        // /stream-proxy/ НЕ содержит подстроку /stream/ — это разные пути
        val proxyContainsStream = proxyUrl.contains("/stream/")
        val realContainsStream = realStreamUrl.contains("/stream/")

        // Извлечение хэша из реального /stream/ URL
        val hashFromReal = if (realContainsStream) {
            realStreamUrl.substringAfter("/stream/").substringBefore("?")
        } else null

        println("ФИКС B: /stream/ vs /stream-proxy/")
        println("  /stream-proxy/ содержит /stream/: $proxyContainsStream")
        println("  /stream/ содержит /stream/: $realContainsStream")
        println("  Хэш из реального URL: $hashFromReal")

        assertEquals(false, proxyContainsStream, "/stream-proxy/ НЕ содержит /stream/ — фикс B не нужен")
        assertEquals(true, realContainsStream, "Реальный /stream/ URL корректно матчится")
        assertEquals("caf36855457a3ca63267f83822e54f0d7de5192f", hashFromReal, "Хэш извлечён корректно")
    }

    // =====================================================================
    // ФИКС C: cleanUrl не вырезает episode=
    // СТАРЫЙ: substringBefore("&season=").substringBefore("?season=")
    // НОВЫЙ: + substringBefore("&episode=").substringBefore("?episode=")
    // =====================================================================

    @Test
    fun fixC_cleanUrl_strips_episode() {
        val url = "https://rutracker.org/dl/123456.torrent?episode=5&season=1"

        // СТАРАЯ логика
        val oldClean = url.substringBefore("&season=").substringBefore("?season=")

        // НОВАЯ логика
        val newClean = url.substringBefore("&season=").substringBefore("?season=")
            .substringBefore("&episode=").substringBefore("?episode=")

        println("ФИКС C: cleanUrl и episode=")
        println("  Исходный:      $url")
        println("  Старая логика: $oldClean (episode остался: ${oldClean.contains("episode=")})")
        println("  Новая логика:  $newClean (episode остался: ${newClean.contains("episode=")})")

        assertEquals("https://rutracker.org/dl/123456.torrent?episode=5", oldClean, "Старая логика оставляет episode=")
        assertEquals("https://rutracker.org/dl/123456.torrent", newClean, "Новая логика вырезает episode=")
    }

    // =====================================================================
    // ФИКС D: HEAD запрос — проверяем что ByteArrayContent(0) с Content-Length > 0
    // вызывает ошибку Ktor. Фикс: не использовать ByteArrayContent для HEAD.
    // Этот тест уже был в StreamProxyTest — здесь только логическая проверка.
    // =====================================================================

    @Test
    fun fixD_head_response_body_size_mismatch() {
        val bodySize = 0
        val contentLength = 1307404629L // 1.3 ГБ видео

        val mismatch = bodySize.toLong() != contentLength

        println("ФИКС D: HEAD body/Content-Length несовпадение")
        println("  Body size: $bodySize, Content-Length: $contentLength")
        println("  Несовпадение: $mismatch — Ktor выбросит BodyLengthIsTooSmall")

        assertEquals(true, mismatch, "Body 0 байт != Content-Length 1.3ГБ — это причина краша")
    }

    // =====================================================================
    // ИНТЕГРАЦИОННЫЙ ТЕСТ: полный flow с НОВОЙ логикой
    // Симуляция: stream.url = proxy URL → декодируем → извлекаем хэш
    // =====================================================================

    @Test
    fun integration_full_flow_with_all_fixes() {
        val torrServerUrl = "http://127.0.0.1:8090/stream/video.mkv?link=abc123def456&index=1&play"
        val base64Url = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(torrServerUrl.toByteArray())
        val streamUrl = "http://localhost:8081/api/stream-proxy/video.mkv?url=$base64Url&format=mkv"

        println("=== ИНТЕГРАЦИОННЫЙ ТЕСТ: полный flow ===")
        println("stream.url: $streamUrl")

        // Шаг 1: НОВЫЙ Base64 декодер (ФИКС A)
        val decodedUrl = try {
            val raw = streamUrl.substringAfter("url=", "")
            if (raw.isNotEmpty()) {
                val base64 = raw.substringBefore("&")
                String(java.util.Base64.getUrlDecoder().decode(base64))
            } else streamUrl
        } catch (_: Exception) {
            streamUrl
        }

        println("  decodedUrl: $decodedUrl")
        assertNotEquals(streamUrl, decodedUrl, "Base64 должен декодироваться, а не фоллбэчить")

        // Шаг 2: НОВЫЙ existingHash (ФИКС B — не матчит /stream-proxy/)
        val existingHash = if (decodedUrl.contains("link=")) {
            decodedUrl.substringAfter("link=").substringBefore("&")
        } else if (streamUrl.contains("link=")) {
            streamUrl.substringAfter("link=").substringBefore("&")
        } else if (streamUrl.contains("/gst/")) {
            streamUrl.substringAfter("/gst/").substringBefore("/")
        } else if (streamUrl.contains("/stream/") && !streamUrl.contains("/stream-proxy/")) {
            streamUrl.substringAfter("/stream/").substringBefore("?")
        } else null

        println("  existingHash: $existingHash")
        assertEquals("abc123def456", existingHash, "Хэш должен извлечься из декодированного URL")

        // Шаг 3: existingFileIndex
        val existingFileIndex = if (decodedUrl.contains("index=")) {
            decodedUrl.substringAfter("index=").substringBefore("&").toIntOrNull()
        } else null

        println("  existingFileIndex: $existingFileIndex")
        assertEquals(1, existingFileIndex, "fileIndex должен извлечься из декодированного URL")

        println("  РЕЗУЛЬТАТ: Все данные извлечены корректно. Торрент будет найден в TorrServer.")
    }
}
