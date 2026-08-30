package org.ensodai.avalonmediacard.core.player

data class SubtitleCue(
    val id: Long,
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val rawStyle: String? = null
)

object UniversalSubtitleParser {

    fun parseSubtitle(content: String): List<SubtitleCue> {
        if (content.contains("[Script Info]", ignoreCase = true) || content.contains("[Events]", ignoreCase = true)) {
            return AssSubtitleParser.parseAss(content)
        }
        return parseSrtOrVtt(content)
    }

    fun parseSrtOrVtt(content: String): List<SubtitleCue> {
        val lines = content.replace("\r\n", "\n").replace("\r", "\n").lines()
        val cues = mutableListOf<SubtitleCue>()
        var index = 0
        var currentId = 0L

        while (index < lines.size) {
            val line = lines[index].trim()

            if (line.isEmpty() || line.startsWith("WEBVTT") || line.startsWith("NOTE")) {
                index++
                continue
            }

            if (line.all { it.isDigit() }) {
                currentId = line.toLongOrNull() ?: currentId
                index++
                if (index >= lines.size) break
            }

            val timecodeLine = lines[index].trim()
            if (timecodeLine.contains("-->")) {
                val times = timecodeLine.split("-->")
                if (times.size == 2) {
                    val startMs = parseTimestamp(times[0].trim())
                    val endMs = parseTimestamp(times[1].trim().split(" ")[0])

                    index++
                    val textBuilder = StringBuilder()
                    while (index < lines.size && lines[index].trim().isNotEmpty()) {
                        if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                        textBuilder.append(lines[index].trim())
                        index++
                    }

                    cues.add(
                        SubtitleCue(
                            id = currentId++,
                            startMs = startMs,
                            endMs = endMs,
                            text = sanitizeTags(textBuilder.toString())
                        )
                    )
                }
            }
            index++
        }
        return cues.sortedBy { it.startMs }
    }

    private fun parseTimestamp(timeStr: String): Long {
        val normalized = timeStr.replace(',', '.')
        val parts = normalized.split(":")
        var hours = 0L
        var minutes = 0L
        var seconds = 0.0

        if (parts.size == 3) {
            hours = parts[0].toLongOrNull() ?: 0L
            minutes = parts[1].toLongOrNull() ?: 0L
            seconds = parts[2].toDoubleOrNull() ?: 0.0
        } else if (parts.size == 2) {
            minutes = parts[0].toLongOrNull() ?: 0L
            seconds = parts[1].toDoubleOrNull() ?: 0.0
        }

        return (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000).toLong()
    }

    private fun sanitizeTags(input: String): String {
        return input.replace(Regex("<[^>]*>"), "")
    }

    fun binarySearchActiveCues(cues: List<SubtitleCue>, currentTimeMs: Long): List<SubtitleCue> {
        if (cues.isEmpty()) return emptyList()

        var low = 0
        var high = cues.size - 1
        var matchIndex = -1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val cue = cues[mid]

            if (currentTimeMs in cue.startMs..cue.endMs) {
                matchIndex = mid
                break
            } else if (cue.startMs > currentTimeMs) {
                high = mid - 1
            } else {
                low = mid + 1
            }
        }

        if (matchIndex == -1) return emptyList()

        val result = mutableListOf<SubtitleCue>()
        var i = matchIndex
        while (i >= 0 && currentTimeMs <= cues[i].endMs) {
            if (currentTimeMs >= cues[i].startMs) result.add(cues[i])
            i--
        }
        i = matchIndex + 1
        while (i < cues.size && currentTimeMs >= cues[i].startMs) {
            if (currentTimeMs <= cues[i].endMs) result.add(cues[i])
            i++
        }

        return result
    }
}
