package org.ensodai.avalonmediacard.core

import kotlinx.browser.window
import kotlinx.coroutines.await

data class SubtitleCue(
    val start: Double,
    val end: Double,
    val text: String
)

sealed interface ChunkState {
    object Idle : ChunkState
    object Loading : ChunkState
    data class Loaded(val cues: List<SubtitleCue>) : ChunkState
    data class Error(val retries: Int, val lastAttemptMs: Double) : ChunkState
}

data class SubtitleChunk(
    val url: String,
    val start: Double,
    val end: Double,
    var state: ChunkState = ChunkState.Idle
)

object VttSubtitleFetcher {

    private fun parseTime(timeStr: String): Double {
        val parts = timeStr.trim().split(":")
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

    @OptIn(ExperimentalWasmJsInterop::class)
    suspend fun fetchSubtitleChunks(m3u8Url: String): List<SubtitleChunk> {
        return try {
            val response = window.fetch(m3u8Url).await()
            if (!response.ok) return emptyList()
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
                        val chunkUrl = baseUrl + nextLine + queryStr
                        chunks.add(SubtitleChunk(chunkUrl, timeAccumulator, timeAccumulator + duration))
                        timeAccumulator += duration
                    }
                }
            }
            chunks
        } catch (e: Exception) {
            println("Failed to fetch M3U8 subtitle playlist: ${e.message}")
            emptyList()
        }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    suspend fun fetchAndParseVtt(url: String): List<SubtitleCue> {
        return try {
            val response = window.fetch(url).await()
            if (!response.ok) return emptyList()
            val text = response.text().await().toString()
            parseVttContent(text)
        } catch (e: Exception) {
            println("Failed to fetch VTT: ${e.message}")
            emptyList()
        }
    }

    private fun parseVttContent(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val lines = content.lines().map { it.trim() }
        
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
                    cues.add(SubtitleCue(currentStart, currentEnd, currentText.toString().trim()))
                    currentStart = -1.0
                    currentEnd = -1.0
                    currentText.clear()
                }
            } else if (!line.startsWith("WEBVTT") && !line.startsWith("X-TIMESTAMP-MAP") && !line.startsWith("NOTE")) {
                // If it's a timestamp line or ID line, we just append if we already parsed a timestamp
                // But VTT can have IDs above the timestamp. We ignore IDs.
                if (currentStart != -1.0) {
                    if (currentText.isNotEmpty()) currentText.append("\n")
                    currentText.append(line)
                }
            }
        }
        
        // Add the last cue if file didn't end with a blank line
        if (currentStart != -1.0 && currentEnd != -1.0 && currentText.isNotEmpty()) {
            cues.add(SubtitleCue(currentStart, currentEnd, currentText.toString().trim()))
        }
        
        return cues
    }
}
