package org.ensodai.avalonmediacard.core.player

object SubtitleSanitizer {

    private val HTML_ENTITIES = mapOf(
        "&apos;" to "'",
        "&quot;" to "\"",
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&nbsp;" to " ",
        "&ndash;" to "–",
        "&mdash;" to "—",
        "&hellip;" to "…",
        "&lsquo;" to "'",
        "&rsquo;" to "'",
        "&ldquo;" to "\"",
        "&rdquo;" to "\""
    )

    private val NUMERIC_ENTITY_REGEX = Regex("&#([xX]?[0-9a-fA-F]+);")
    private val TAGS_REGEX = Regex("<[^>]*>|\\{[^}]*\\}")
    private val PIPE_SEPARATOR_REGEX = Regex("""\s*\|\s*""")
    private val MULTI_NEWLINE_REGEX = Regex("""\n{3,}""")

    /**
     * Очищает текст субтитров:
     * 1. Снимает HTML/WebVTT теги (<v ...>, <i>, <b>, <c.color>) и ASS стили ({\an8}, {\pos(...)})
     * 2. Декодирует числовые и именованные HTML-сущности (&apos; -> ', &quot; -> ", &#39; -> ')
     * 3. Преобразует легаси MicroDVD разделители строк (|) в переносы строк \n
     * 4. Сжимает избыточные пустые строки
     */
    fun sanitize(rawText: String): String {
        if (rawText.isBlank()) return ""

        var text = rawText

        // 1. Снятие тегов WebVTT и ASS
        text = text.replace(TAGS_REGEX, "")

        // 2. Обработка экранированных переводов строк ASS (\N, \n, \h)
        text = text
            .replace("\\N", "\n")
            .replace("\\n", "\n")
            .replace("\\h", " ")

        // 3. Декодирование числовых HTML-сущностей (&#39;, &#x27;)
        text = NUMERIC_ENTITY_REGEX.replace(text) { match ->
            val codeStr = match.groupValues[1]
            val charCode = if (codeStr.startsWith("x", ignoreCase = true)) {
                codeStr.substring(1).toIntOrNull(16)
            } else {
                codeStr.toIntOrNull()
            }
            if (charCode != null && charCode > 0) {
                charCode.toChar().toString()
            } else {
                match.value
            }
        }

        // 4. Декодирование именованных HTML-сущностей
        for ((entity, replacement) in HTML_ENTITIES) {
            if (text.contains(entity)) {
                text = text.replace(entity, replacement)
            }
        }

        // 5. Преобразование MicroDVD пайпов (|) в переносы строк
        text = text.replace(PIPE_SEPARATOR_REGEX, "\n")

        // 6. Сжатие лишних пустых строк и обрезка
        return text.replace(MULTI_NEWLINE_REGEX, "\n\n").trim()
    }
}
