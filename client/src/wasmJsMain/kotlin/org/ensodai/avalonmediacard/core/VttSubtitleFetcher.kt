package org.ensodai.avalonmediacard.core

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import org.ensodai.avalonmediacard.core.player.SubtitleSanitizer

data class SubtitleCue(
    val start: Double,
    val end: Double,
    val text: String
)

data class SubtitleChunk(
    val index: Int,
    val url: String,
    val start: Double,
    val end: Double
)

class TrackCache(
    val trackId: String,
    val resolvedUrl: String,
    val isSegmented: Boolean,
    var chunks: List<SubtitleChunk> = emptyList(),
    var isInitialized: Boolean = false
) {
    val loadedCues = mutableMapOf<Int, List<SubtitleCue>>()
    val inFlight = mutableSetOf<Int>()
    val failedAttempts = mutableMapOf<Int, Int>()
}

class SubtitleSessionCache {
    private val tracks = mutableMapOf<String, TrackCache>()

    fun getTrack(trackId: String): TrackCache? = tracks[trackId]

    fun putTrack(trackId: String, cache: TrackCache) {
        tracks[trackId] = cache
    }

    fun clear() {
        tracks.clear()
    }
}

object VttSubtitleFetcher {

    private fun parseTime(timeStr: String): Double {
        val cleanTime = timeStr.trim().split(Regex("\\s+")).firstOrNull() ?: return 0.0
        val parts = cleanTime.replace(',', '.').split(":")
        var seconds = 0.0
        if (parts.size == 3) {
            seconds += (parts[0].toDoubleOrNull() ?: 0.0) * 3600
            seconds += (parts[1].toDoubleOrNull() ?: 0.0) * 60
            seconds += (parts[2].toDoubleOrNull() ?: 0.0)
        } else if (parts.size == 2) {
            seconds += (parts[0].toDoubleOrNull() ?: 0.0) * 60
            seconds += (parts[1].toDoubleOrNull() ?: 0.0)
        }
        return seconds
    }

    private fun resolveChunkUrl(baseUrl: String, m3u8Url: String, nextLine: String, queryStr: String): String {
        return when {
            nextLine.startsWith("http://") || nextLine.startsWith("https://") -> nextLine
            nextLine.startsWith("/") -> {
                val origin = if (m3u8Url.startsWith("http://") || m3u8Url.startsWith("https://")) {
                    val schemeEnd = m3u8Url.indexOf("://") + 3
                    val slashIndex = m3u8Url.indexOf('/', schemeEnd)
                    if (slashIndex != -1) m3u8Url.substring(0, slashIndex) else m3u8Url
                } else {
                    ""
                }
                origin + nextLine
            }
            else -> {
                val q = if (queryStr.isNotEmpty() && !nextLine.contains("?")) queryStr else ""
                baseUrl + nextLine + q
            }
        }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    suspend fun fetchSubtitleChunks(m3u8Url: String): List<SubtitleChunk> {
        return try {
            withTimeoutOrNull(7000.milliseconds) {
                val response = window.fetch(m3u8Url).await()
                if (!response.ok) return@withTimeoutOrNull emptyList()
                val text = response.text().await().toString()
                
                val baseUrl = m3u8Url.substringBeforeLast("/") + "/"
                val queryStr = if (m3u8Url.contains("?")) "?" + m3u8Url.substringAfter("?") else ""
                
                val chunks = mutableListOf<SubtitleChunk>()
                var timeAccumulator = 0.0
                
                val lines = text.lines().map { it.trim() }
                for (i in lines.indices) {
                    val line = lines[i]
                    if (line.startsWith("#EXTINF:")) {
                        val duration = line.substringAfter(":").substringBefore(",").toDoubleOrNull() ?: 0.0
                        val nextLine = lines.getOrNull(i + 1) ?: ""
                        if (!nextLine.startsWith("#") && nextLine.isNotEmpty()) {
                            val chunkUrl = resolveChunkUrl(baseUrl, m3u8Url, nextLine, queryStr)
                            val chunkIndex = chunks.size
                            chunks.add(SubtitleChunk(chunkIndex, chunkUrl, timeAccumulator, timeAccumulator + duration))
                            timeAccumulator += duration
                        }
                    }
                }
                chunks
            } ?: emptyList()
        } catch (e: Exception) {
            println("Failed to fetch M3U8 subtitle playlist: ${e.message}")
            emptyList()
        }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    suspend fun fetchAndParseVtt(url: String): List<SubtitleCue>? {
        return try {
            withTimeoutOrNull(6500.milliseconds) {
                val response = window.fetch(url).await()
                if (!response.ok) return@withTimeoutOrNull null
                val text = response.text().await().toString()
                parseVttContent(text)
            }
        } catch (e: Exception) {
            println("Failed to fetch VTT: ${e.message}")
            null
        }
    }

    private fun parseVttContent(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val lines = content.replace("\r\n", "\n").replace("\r", "\n").lines().map { it.trim() }
        
        var currentStart = -1.0
        var currentEnd = -1.0
        val currentText = StringBuilder()
        
        for (line in lines) {
            if (line.contains("-->")) {
                val parts = line.split("-->")
                if (parts.size == 2) {
                    currentStart = parseTime(parts[0])
                    currentEnd = parseTime(parts[1])
                    currentText.clear()
                }
            } else if (line.isEmpty()) {
                if (currentStart != -1.0 && currentEnd != -1.0 && currentText.isNotEmpty()) {
                    val cleanText = SubtitleSanitizer.sanitize(currentText.toString())
                    cues.add(SubtitleCue(currentStart, currentEnd, cleanText))
                    currentStart = -1.0
                    currentEnd = -1.0
                    currentText.clear()
                }
            } else if (!line.startsWith("WEBVTT") && !line.startsWith("X-TIMESTAMP-MAP") && !line.startsWith("NOTE")) {
                if (currentStart != -1.0) {
                    if (currentText.isNotEmpty()) currentText.append("\n")
                    currentText.append(line)
                }
            }
        }
        
        if (currentStart != -1.0 && currentEnd != -1.0 && currentText.isNotEmpty()) {
            val cleanText = SubtitleSanitizer.sanitize(currentText.toString())
            cues.add(SubtitleCue(currentStart, currentEnd, cleanText))
        }
        
        return cues
    }
}
