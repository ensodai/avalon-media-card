package org.ensodai.avalonmediacard.core.player

import kotlin.test.Test
import kotlin.test.assertEquals

class SubtitleSanitizerTest {

    @Test
    fun testUnescapesAposAndQuotes() {
        val input = "С А&apos;Лен, их инженером."
        val expected = "С А'Лен, их инженером."
        assertEquals(expected, SubtitleSanitizer.sanitize(input))
    }

    @Test
    fun testUnescapesCommonHtmlEntities() {
        val input = "&quot;Hello&quot; &amp; I&apos;m fine &lt;world&gt;&nbsp;!"
        val expected = "\"Hello\" & I'm fine <world> !"
        assertEquals(expected, SubtitleSanitizer.sanitize(input))
    }

    @Test
    fun testUnescapesNumericEntities() {
        val input = "&#39;single&#39; and &#x22;double&#x22;"
        val expected = "'single' and \"double\""
        assertEquals(expected, SubtitleSanitizer.sanitize(input))
    }

    @Test
    fun testReplacesMicroDvdPipesWithNewlines() {
        val input = "Первая строка | Вторая строка"
        val expected = "Первая строка\nВторая строка"
        assertEquals(expected, SubtitleSanitizer.sanitize(input))
    }

    @Test
    fun testStripsHtmlAndAssTags() {
        val input = "<v Host><i>Привет</i></v> {\\an8}мир"
        val expected = "Привет мир"
        assertEquals(expected, SubtitleSanitizer.sanitize(input))
    }
}
