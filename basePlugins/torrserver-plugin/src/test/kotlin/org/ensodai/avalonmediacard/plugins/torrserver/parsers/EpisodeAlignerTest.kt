package org.ensodai.avalonmediacard.plugins.torrserver.parsers

import org.ensodai.avalonmediacard.contract.slot.EpisodeItem
import org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.torrent.EpisodeAligner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EpisodeAlignerTest {

    private fun ep(num: Int, name: String, runtime: Int = 44): EpisodeItem {
        return EpisodeItem(
            id = "ep_$num",
            episodeNumber = num,
            name = name,
            overview = null,
            stillUrl = "https://image.tmdb.org/t/p/w500/ep_$num.jpg",
            airDate = null,
            voteAverage = null,
            runtime = runtime
        )
    }

    @Test
    fun `test star trek enterprise season 1 gap alignment`() {
        // Files from CasStudio rutracker torrent (25 files numbered S01E01..S01E25)
        val files = listOf(
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E01.Broken.Bow.Part.1.2.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(1)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E02.Fight.or.Flight.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(2)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E03.Strange.New.World.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(3)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E04.Unexpected.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(4)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E05.Terra.Nova.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(5)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E06.The.Andorian.Incident.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(6)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E07.Breaking.the.Ice.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(7)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E08.Civilization.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(8)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E09.Fortunate.Son.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(9)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E10.Cold.Front.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(10)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E11.Silent.Enemy.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(11)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E12.Dear.Doctor.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(12)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E13.Sleeping.Dogs.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(13)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E14.Shadows.of.PJem.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(14)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E15.Shuttlepod.One.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(15)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E16.Fusion.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(16)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E17.Rogue.Planet.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(17)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E18.Acquisition.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(18)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E19.Oasis.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(19)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E20.Detained.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(20)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E21.Vox.Sola.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(21)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E22.Fallen.Hero.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(22)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E23.Desert.Crossing.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(23)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E24.Two.Days.and.Two.Nights.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(24)),
            EpisodeAligner.FileInput("Star.Trek.Enterprise.S01E25.Shockwave.Part.1.WEB-DL.Rus.Eng.Subs.CasStudio.mkv", 1, listOf(25))
        )

        // TMDB Season 1 (25 episodes: 1, 3..26; Episode 2 is missing/deleted upstream)
        val tmdbRu = listOf(
            ep(1, "Разорванный круг", 86),
            ep(3, "Бежать или сражаться"),
            ep(4, "Этот странный новый мир…"),
            ep(5, "Неожиданность"),
            ep(6, "Терра Нова"),
            ep(7, "Андорианский инцидент"),
            ep(8, "Взламывая льды"),
            ep(9, "Цивилизация"),
            ep(10, "Сын Фортуны"),
            ep(11, "Холодный фронт"),
            ep(12, "Молчаливый враг"),
            ep(13, "Дорогой доктор"),
            ep(14, "Спящие псы"),
            ep(15, "Тени Пи’Джем"),
            ep(16, "Шаттл один", 45),
            ep(17, "Слияние"),
            ep(18, "Блуждающая планета"),
            ep(19, "Приобретение"),
            ep(20, "Оазис"),
            ep(21, "Задержанные"),
            ep(22, "Vox Sola"),
            ep(23, "Падший герой"),
            ep(24, "Пересечение пустыни"),
            ep(25, "Два дня и две ночи"),
            ep(26, "Ударная волна (часть 1)")
        )

        val tmdbEn = listOf(
            ep(1, "Broken Bow", 86),
            ep(3, "Fight or Flight"),
            ep(4, "Strange New World"),
            ep(5, "Unexpected"),
            ep(6, "Terra Nova"),
            ep(7, "The Andorian Incident"),
            ep(8, "Breaking the Ice"),
            ep(9, "Civilization"),
            ep(10, "Fortunate Son"),
            ep(11, "Cold Front"),
            ep(12, "Silent Enemy"),
            ep(13, "Dear Doctor"),
            ep(14, "Sleeping Dogs"),
            ep(15, "Shadows of P'Jem"),
            ep(16, "Shuttlepod One", 45),
            ep(17, "Fusion"),
            ep(18, "Rogue Planet"),
            ep(19, "Acquisition"),
            ep(20, "Oasis"),
            ep(21, "Detained"),
            ep(22, "Vox Sola"),
            ep(23, "Fallen Hero"),
            ep(24, "Desert Crossing"),
            ep(25, "Two Days and Two Nights"),
            ep(26, "Shockwave (1)")
        )

        val aligned = EpisodeAligner.alignSeason(1, files, tmdbRu, tmdbEn)

        assertEquals(25, aligned.size)

        // File 1: Broken Bow -> Episode 1
        val res1 = aligned["Star.Trek.Enterprise.S01E01.Broken.Bow.Part.1.2.WEB-DL.Rus.Eng.Subs.CasStudio.mkv"]
        assertNotNull(res1)
        assertEquals(1, res1.episodeNumber)
        assertEquals("Разорванный круг", res1.matchedEpisode?.name)

        // File 2: Fight or Flight -> MUST ALIGN TO EPISODE 3 (NOT EMPTY EPISODE 2!)
        val res2 = aligned["Star.Trek.Enterprise.S01E02.Fight.or.Flight.WEB-DL.Rus.Eng.Subs.CasStudio.mkv"]
        assertNotNull(res2)
        assertEquals(3, res2.episodeNumber)
        assertEquals("Бежать или сражаться", res2.matchedEpisode?.name)

        // File 3: Strange New World -> MUST ALIGN TO EPISODE 4
        val res3 = aligned["Star.Trek.Enterprise.S01E03.Strange.New.World.WEB-DL.Rus.Eng.Subs.CasStudio.mkv"]
        assertNotNull(res3)
        assertEquals(4, res3.episodeNumber)
        assertEquals("Этот странный новый мир…", res3.matchedEpisode?.name)

        // File 25: Shockwave -> MUST ALIGN TO EPISODE 26
        val res25 = aligned["Star.Trek.Enterprise.S01E25.Shockwave.Part.1.WEB-DL.Rus.Eng.Subs.CasStudio.mkv"]
        assertNotNull(res25)
        assertEquals(26, res25.episodeNumber)
        assertEquals("Ударная волна (часть 1)", res25.matchedEpisode?.name)
    }

    @Test
    fun `test anonymous filenames sequence gap alignment`() {
        // Filenames without titles: 25 files, S01E01..S01E25
        val files = (1..25).map { epNum ->
            EpisodeAligner.FileInput("Show.S01E%02d.mkv".format(epNum), 1, listOf(epNum))
        }

        // TMDB has gap at 2: 1, 3..26
        val tmdbNumbers = listOf(1) + (3..26).toList()
        val tmdbEpisodes = tmdbNumbers.map { ep(it, "Episode $it") }

        val aligned = EpisodeAligner.alignSeason(1, files, tmdbEpisodes)

        assertEquals(25, aligned.size)
        assertEquals(1, aligned["Show.S01E01.mkv"]?.episodeNumber)
        assertEquals(3, aligned["Show.S01E02.mkv"]?.episodeNumber)
        assertEquals(4, aligned["Show.S01E03.mkv"]?.episodeNumber)
        assertEquals(26, aligned["Show.S01E25.mkv"]?.episodeNumber)
    }

    @Test
    fun `test standard series 1-to-1 matching`() {
        val files = (1..5).map { epNum ->
            EpisodeAligner.FileInput("Breaking.Bad.S01E%02d.mkv".format(epNum), 1, listOf(epNum))
        }
        val tmdb = (1..5).map { ep(it, "BB Ep $it") }

        val aligned = EpisodeAligner.alignSeason(1, files, tmdb)

        for (i in 1..5) {
            assertEquals(i, aligned["Breaking.Bad.S01E%02d.mkv".format(i)]?.episodeNumber)
        }
    }

    @Test
    fun `test manual mapping preservation`() {
        val files = listOf(
            EpisodeAligner.FileInput("FileA.mkv", 1, listOf(5), isManual = true),
            EpisodeAligner.FileInput("FileB.mkv", 1, listOf(2), isManual = false)
        )
        val tmdb = listOf(ep(1, "Ep 1"), ep(2, "Ep 2"), ep(5, "Ep 5"))

        val aligned = EpisodeAligner.alignSeason(1, files, tmdb)

        assertEquals(5, aligned["FileA.mkv"]?.episodeNumber)
        assertEquals(true, files[0].isManual)
    }
}
