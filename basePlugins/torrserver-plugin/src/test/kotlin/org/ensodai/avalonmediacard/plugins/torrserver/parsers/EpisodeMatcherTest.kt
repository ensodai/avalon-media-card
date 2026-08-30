package org.ensodai.avalonmediacard.plugins.torrserver.parsers

import org.ensodai.avalonmediacard.contract.parsers.EpisodeMatcher
import org.ensodai.avalonmediacard.contract.parsers.MappingResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EpisodeMatcherTest {

    private val matcher = EpisodeMatcher()

    @Test
    fun `test standard scene multi-season batch with nested folders`() {
        val root = "The Office Seasons 1-3 1080p WEB-DL"
        val files = listOf(
            "Season 1/The.Office.S01E01.mkv", // Явный Success, игнорирует корень
            "Season 2/05.mkv",               // Partial -> Success (Слияние с контекстом пути)
            "Season 3/Episode 12.mp4"        // Partial -> Success (Слияние с контекстом пути)
        )

        val results = matcher.parseBatch(root, files)

        val res1 = results["Season 1/The.Office.S01E01.mkv"] as MappingResult.Success
        assertEquals(listOf(1), res1.seasons)
        assertEquals(listOf(1), res1.episodes)

        val res2 = results["Season 2/05.mkv"] as MappingResult.Success
        assertEquals(listOf(2), res2.seasons, "Season MUST be inherited from parent directory")
        assertEquals(listOf(5), res2.episodes)

        val res3 = results["Season 3/Episode 12.mp4"] as MappingResult.Success
        assertEquals(listOf(3), res3.seasons)
        assertEquals(listOf(12), res3.episodes)
    }

    @Test
    fun `test flat directory with safe root inheritance`() {
        // Корневой торрент содержит ровно 1 сезон
        val root = "Breaking Bad Season 4"
        // Плоская структура, внутри файлов нет явных указателей на сезон
        val files = listOf("01.mkv", "02.mkv")

        val results = matcher.parseBatch(root, files)

        val res1 = results["01.mkv"] as MappingResult.Success
        assertEquals(listOf(4), res1.seasons, "Season MUST be safely inherited from root")
        assertEquals(listOf(1), res1.episodes)
    }

    @Test
    fun `test dangerous flat directory conflict resolution (Data Loss Prevention)`() {
        // Корневой торрент охватывает сезоны 1-3
        val root = "Show Name S01-S03"
        // Плоская структура. Неизвестно, к какому из 3-х сезонов относится файл.
        val files = listOf("01.mkv")

        val results = matcher.parseBatch(root, files)

        // Ожидаем Partial, так как невозможно математически безопасно слить контекст
        val res = results["01.mkv"]
        assertTrue(res is MappingResult.Partial, "Expected Partial due to mapping ambiguity to prevent data loss")
        assertEquals(listOf(1), res.episodes)
    }

    @Test
    fun `test localized russian multi-season mapping with root overrides`() {
        val root = "Доктор Хаус 1-3 сезон"
        val files = listOf(
            "Сезон 2/серия 5.mkv",
            "Слово пацана с01е02.mkv" // Переопределение корневого контекста локальным маркером файла
        )

        val results = matcher.parseBatch(root, files)

        val res1 = results["Сезон 2/серия 5.mkv"] as MappingResult.Success
        assertEquals(listOf(2), res1.seasons)
        assertEquals(listOf(5), res1.episodes)

        val res2 = results["Слово пацана с01е02.mkv"] as MappingResult.Success
        assertEquals(listOf(1), res2.seasons, "Local file marker S01 MUST override root context S01-03")
        assertEquals(listOf(2), res2.episodes)
    }

    @Test
    fun `test anime absolute numbering fallback and heavy cleaning in batches`() {
        val root = " Naruto Complete"
        val files = listOf(
            "Naruto - 135 [1080p].mkv",
            " Naruto - 136-137 (1080p).mkv"
        )

        val results = matcher.parseBatch(root, files)

        val res1 = results["Naruto - 135 [1080p].mkv"] as MappingResult.Partial
        assertTrue(res1.isAbsolute, "Fallback provider MUST set absolute flag")
        assertEquals(listOf(135), res1.episodes)

        val res2 = results[" Naruto - 136-137 (1080p).mkv"] as MappingResult.Partial
        assertTrue(res2.isAbsolute)
        assertEquals(listOf(136, 137), res2.episodes, "Range MUST be parsed to array")
    }

    @Test
    fun `test extreme garbage rejection (Years and Dimensions)`() {
        val root = "Series 2011-2013 1080p"
        val files = listOf("S02E05 1920x1080.mkv")

        val results = matcher.parseBatch(root, files)

        val res1 = results["S02E05 1920x1080.mkv"] as MappingResult.Success
        // Год 2011 не должен переопределить сезон, а 1920x1080 не должно перебить серию
        assertEquals(listOf(2), res1.seasons)
        assertEquals(listOf(5), res1.episodes)
    }
}
