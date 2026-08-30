package org.ensodai.avalonmediacard.core.player

object AssSubtitleParser {

    fun parseAss(content: String): List<SubtitleCue> {
        val lines = content.replace("\r\n", "\n").replace("\r", "\n").lines()
        val cues = mutableListOf<SubtitleCue>()
        var inEventsSection = false
        var currentId = 0L

        var startIdx = 1
        var endIdx = 2
        var textIdx = 9

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.equals("[Events]", ignoreCase = true)) {
                inEventsSection = true
                continue
            }

            if (trimmed.startsWith("[") && trimmed.endsWith("]") && !trimmed.equals("[Events]", ignoreCase = true)) {
                inEventsSection = false
                continue
            }

            if (inEventsSection) {
                if (trimmed.startsWith("Format:", ignoreCase = true)) {
                    val formatParts = trimmed.substringAfter("Format:").split(",").map { it.trim() }
                    startIdx =
                        formatParts.indexOfFirst { it.equals("Start", ignoreCase = true) }.takeIf { it != -1 } ?: 1
                    endIdx = formatParts.indexOfFirst { it.equals("End", ignoreCase = true) }.takeIf { it != -1 } ?: 2
                    textIdx = formatParts.indexOfFirst { it.equals("Text", ignoreCase = true) }.takeIf { it != -1 } ?: 9
                } else if (trimmed.startsWith("Dialogue:", ignoreCase = true)) {
                    val payload = trimmed.substringAfter("Dialogue:").trim()
                    val parts = payload.split(",", limit = textIdx + 1)
                    if (parts.size > textIdx) {
                        val startMs = parseAssTimestamp(parts[startIdx].trim())
                        val endMs = parseAssTimestamp(parts[endIdx].trim())
                        val rawText = parts[textIdx]
                        val cleanText = sanitizeAssTags(rawText)

                        if (cleanText.isNotEmpty() && endMs > startMs) {
                            cues.add(
                                SubtitleCue(
                                    id = currentId++,
                                    startMs = startMs,
                                    endMs = endMs,
                                    text = cleanText,
                                    rawStyle = rawText
                                )
                            )
                        }
                    }
                }
            }
        }
        return cues.sortedBy { it.startMs }
    }

    private fun parseAssTimestamp(timestamp: String): Long {
        val parts = timestamp.split(":")
        if (parts.size != 3) return 0L
        val hours = parts[0].toLongOrNull() ?: 0L
        val minutes = parts[1].toLongOrNull() ?: 0L
        val secondsParts = parts[2].split(".")
        val seconds = secondsParts[0].toLongOrNull() ?: 0L
        val cs = if (secondsParts.size > 1) secondsParts[1].padEnd(2, '0').take(2).toLongOrNull() ?: 0L else 0L

        return (hours * 3600000L) + (minutes * 60000L) + (seconds * 1000L) + (cs * 10L)
    }

    private fun sanitizeAssTags(text: String): String {
        return text
            .replace(Regex("\\{[^}]*\\}"), "")
            .replace("\\N", "\n")
            .replace("\\n", "\n")
            .replace("\\h", " ")
            .trim()
    }
}
