package org.ensodai.avalonmediacard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFails
import kotlin.test.assertTrue

/**
 * Тесты логики парсинга URL в PrepareStreamUseCase и TorrServerApiClient.
 * Проверяем: Base64 кодирование/декодирование, извлечение хэша, очистку URL.
 * Эти тесты НЕ требуют сервера — тестируется чистая логика.
 */
class PrepareStreamLogicTest {

    // =====================================================================
    // ТЕСТ 1: Совместимость Base64 кодирования и декодирования
    // TorrServerApiClient.buildStreamUrl() кодирует через java.util.Base64.getUrlEncoder()
    // PrepareStreamUseCase декодирует через kotlin.io.encoding.Base64.Default
    // Вопрос: совместимы ли они?
    // =====================================================================

    @Test
    fun test_Base64Default_fails_on_UrlSafe_encoded_string() {
        // Это реальный URL который buildStreamUrl генерирует для TorrServer
        val torrServerUrl = "http://127.0.0.1:8090/stream/video.mkv?link=caf36855457a3ca63267f83822e54f0d7de5192f&index=48&play"

        // buildStreamUrl кодирует через java.util.Base64.getUrlEncoder()
        val encoded = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(torrServerUrl.toByteArray())

        // PrepareStreamUseCase декодирует через kotlin.io.encoding.Base64.Default
        val decodedResult = try {
            kotlin.io.encoding.Base64.Default.decode(encoded).decodeToString()
        } catch (e: Exception) {
            null // Если упало — значит декодирование несовместимо
        }

        // Если decodedResult == null, значит Base64.Default НЕ МОЖЕТ декодировать URL-safe строку
        // Если decodedResult != null, проверяем что результат совпадает
        println("=== ТЕСТ 1: Base64 совместимость ===")
        println("Оригинальный URL: $torrServerUrl")
        println("Закодированный (UrlEncoder): $encoded")
        println("Декодированный (Base64.Default): $decodedResult")

        if (decodedResult == null) {
            println("РЕЗУЛЬТАТ: Base64.Default ПАДАЕТ на URL-safe строке! decodedUrl будет = stream.url (фоллбэк)")
        } else {
            assertEquals(torrServerUrl, decodedResult)
            println("РЕЗУЛЬТАТ: Декодирование прошло успешно")
        }
    }

    @Test
    fun test_Java_UrlDecoder_works_on_UrlSafe_encoded_string() {
        val torrServerUrl = "http://127.0.0.1:8090/stream/video.mkv?link=caf36855457a3ca63267f83822e54f0d7de5192f&index=48&play"
        val encoded = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(torrServerUrl.toByteArray())

        // Декодируем через java.util.Base64.getUrlDecoder() — тот же стандарт
        val decoded = String(java.util.Base64.getUrlDecoder().decode(encoded))

        println("=== ТЕСТ 2: java.util.Base64.getUrlDecoder() ===")
        println("Декодированный: $decoded")
        assertEquals(torrServerUrl, decoded)
        println("РЕЗУЛЬТАТ: getUrlDecoder() работает корректно")
    }

    // =====================================================================
    // ТЕСТ 3: Что происходит с existingHash когда Base64 декодирование падает
    // Если decodedUrl = stream.url (фоллбэк), а stream.url это proxy-ссылка,
    // найдёт ли код хэш?
    // =====================================================================

    @Test
    fun test_existingHash_extraction_from_proxy_url() {
        // Это типичный stream.url который приходит в PrepareStreamUseCase
        // когда пользователь повторно нажимает "Смотреть онлайн"
        val streamUrl = "http://localhost:8081/api/stream-proxy/video.mkv?url=aHR0cDovLzEyNy4wLjAuMTo4MDkwL3N0cmVhbS92aWRlby5ta3Y_bGluaz1jYWYzNjg1NTQ1N2EzY2E2MzI2N2Y4MzgyMmU1NGYwZDdkZTUxOTJmJmluZGV4PTQ4JnBsYXk&auth=QmFzaWMgZEdWemREcDBaWE4w&format=mkv"

        // Логика из PrepareStreamUseCase: пытаемся декодировать Base64
        val decodedUrl = try {
            val raw = streamUrl.substringAfter("url=", "")
            if (raw.isNotEmpty()) {
                val base64 = raw.substringBefore("&")
                kotlin.io.encoding.Base64.Default.decode(base64).decodeToString()
            } else streamUrl
        } catch (_: Exception) {
            streamUrl // фоллбэк!
        }

        // Ищем existingHash по логике PrepareStreamUseCase
        val existingHash = if (decodedUrl.contains("link=")) {
            decodedUrl.substringAfter("link=").substringBefore("&")
        } else if (streamUrl.contains("link=")) {
            streamUrl.substringAfter("link=").substringBefore("&")
        } else null

        println("=== ТЕСТ 3: Извлечение хэша из proxy URL ===")
        println("stream.url: $streamUrl")
        println("decodedUrl (после фоллбэка): ${if (decodedUrl == streamUrl) "ФОЛЛБЭК на stream.url" else decodedUrl}")
        println("existingHash: $existingHash")

        if (existingHash == null) {
            println("РЕЗУЛЬТАТ: Хэш НЕ НАЙДЕН! Код пойдёт качать cleanUrl как торрент-файл")
        } else {
            println("РЕЗУЛЬТАТ: Хэш найден: $existingHash")
        }
    }

    // =====================================================================
    // ТЕСТ 4: cleanUrl — убирает ли episode= из URL?
    // =====================================================================

    @Test
    fun test_cleanUrl_strips_episode_param() {
        // URL с episode перед season
        val url1 = "https://rutracker.org/dl/123456.torrent?episode=5&season=1"
        val cleanUrl1 = url1.substringBefore("&season=").substringBefore("?season=")

        println("=== ТЕСТ 4: cleanUrl и episode= ===")
        println("Исходный URL: $url1")
        println("cleanUrl: $cleanUrl1")

        if (cleanUrl1.contains("episode=")) {
            println("РЕЗУЛЬТАТ: episode= НЕ вырезан из cleanUrl! Скачивание .torrent файла может сломаться")
        } else {
            println("РЕЗУЛЬТАТ: episode= вырезан корректно")
        }

        // URL с episode после season
        val url2 = "https://rutracker.org/dl/123456.torrent?season=1&episode=5"
        val cleanUrl2 = url2.substringBefore("&season=").substringBefore("?season=")

        println("Исходный URL: $url2")
        println("cleanUrl: $cleanUrl2")

        if (cleanUrl2.contains("episode=")) {
            println("РЕЗУЛЬТАТ: episode= НЕ вырезан из cleanUrl!")
        } else {
            println("РЕЗУЛЬТАТ: episode= вырезан корректно")
        }
    }

    // =====================================================================
    // ТЕСТ 5: Полная симуляция — что происходит когда stream.url = proxy URL
    // и Base64.Default не может его декодировать
    // =====================================================================

    @Test
    fun test_full_flow_proxy_url_fallback() {
        // buildStreamUrl генерирует такие URL-ы
        val torrServerRawUrl = "http://127.0.0.1:8090/stream/video.mkv?link=abc123def456&index=1&play"
        val base64Url = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(torrServerRawUrl.toByteArray())
        val streamUrl = "http://localhost:8081/api/stream-proxy/video.mkv?url=$base64Url&format=mkv"

        println("=== ТЕСТ 5: Полная симуляция потока ===")
        println("TorrServer URL: $torrServerRawUrl")
        println("Base64 URL-safe: $base64Url")
        println("stream.url (proxy): $streamUrl")

        // Шаг 1: Декодируем Base64 (логика PrepareStreamUseCase строка 36-44)
        val decodedUrl = try {
            val raw = streamUrl.substringAfter("url=", "")
            if (raw.isNotEmpty()) {
                val base64 = raw.substringBefore("&")
                kotlin.io.encoding.Base64.Default.decode(base64).decodeToString()
            } else streamUrl
        } catch (_: Exception) {
            streamUrl
        }

        val base64Failed = (decodedUrl == streamUrl)
        println("Base64.Default декодирование: ${if (base64Failed) "УПАЛО (фоллбэк)" else "OK"}")

        // Шаг 2: Ищем existingHash (строка 46-50)
        val existingHash = if (decodedUrl.contains("link=")) {
            decodedUrl.substringAfter("link=").substringBefore("&")
        } else if (streamUrl.contains("link=")) {
            streamUrl.substringAfter("link=").substringBefore("&")
        } else null

        println("existingHash: $existingHash")

        // Шаг 3: cleanUrl (строка 34)
        val cleanUrl = streamUrl.substringBefore("&season=").substringBefore("?season=")
        println("cleanUrl: $cleanUrl")

        // Шаг 4: Что произойдёт дальше?
        if (existingHash.isNullOrBlank()) {
            val willDownload = cleanUrl.startsWith("http") && !cleanUrl.contains("/stream-proxy/")
            println("Код пойдёт в ветку addTorrent")
            println("Попытается скачать cleanUrl как .torrent: $willDownload")
            if (!willDownload) {
                println("cleanUrl содержит /stream-proxy/, скачивание пропущено")
                println("addTorrent получит cleanUrl='$cleanUrl' как magnet/ссылку — ОШИБКА!")
            }
        } else {
            println("Код использует существующий хэш: $existingHash")
        }
    }
}
