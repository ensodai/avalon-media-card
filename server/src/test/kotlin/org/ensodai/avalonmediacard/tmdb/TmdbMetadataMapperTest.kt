package org.ensodai.avalonmediacard.tmdb

import org.ensodai.avalonmediacard.contract.classification.AnimeSubType
import org.ensodai.avalonmediacard.tmdb.responses.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TmdbMetadataMapperTest {

    private val mapper = TmdbMetadataMapper()

    @Test
    fun testMapAnimeTvShowResponse() {
        val showResponse = TmdbShowDetailResponse(
            id = 85937,
            name = "Клинок, рассекающий демонов",
            originalName = "Kimetsu no Yaiba",
            overview = "История Танджиро...",
            genres = listOf(TmdbGenreResponse(id = 16, name = "Animation"), TmdbGenreResponse(id = 10759, name = "Action & Adventure")),
            productionCompanies = listOf(TmdbCompanyResponse(id = 1, name = "ufotable"), TmdbCompanyResponse(id = 2, name = "Aniplex")),
            originalLanguage = "ja",
            originCountry = listOf("JP"),
            networks = listOf(TmdbNetworkResponse(id = 1, name = "Tokyo MX"))
        )

        val details = TmdbDetails.Show(showResponse)
        val metadata = mapper.mapMediaDetails(mediaId = "tv:85937", movie = details, language = "ru")

        assertEquals("Клинок, рассекающий демонов", metadata.title)
        assertEquals("Kimetsu no Yaiba", metadata.originalTitle)
        assertEquals(AnimeSubType.JAPANESE_ANIME, metadata.animeSubType)
        assertTrue(metadata.isAnime)
    }

    @Test
    fun testMapLiveActionJapaneseMovieResponse() {
        val movieResponse = TmdbMovieDetailResponse(
            id = 346,
            title = "Семь самураев",
            originalTitle = "七人の侍",
            overview = "Шедевр Куросавы...",
            genres = listOf(TmdbGenreResponse(id = 28, name = "Action"), TmdbGenreResponse(id = 18, name = "Drama")),
            productionCompanies = listOf(TmdbCompanyResponse(id = 1, name = "Toho")),
            originalLanguage = "ja",
            originCountry = listOf("JP")
        )

        val details = TmdbDetails.Movie(movieResponse)
        val metadata = mapper.mapMediaDetails(mediaId = "movie:346", movie = details, language = "ru")

        assertEquals("Семь самураев", metadata.title)
        assertEquals(AnimeSubType.NOT_ANIME, metadata.animeSubType)
        assertFalse(metadata.isAnime)
    }

    @Test
    fun testMapChineseDonghuaResponse() {
        val showResponse = TmdbShowDetailResponse(
            id = 7046092,
            name = "Агент времени",
            originalName = "时光代理人",
            overview = "Фотоателье Время...",
            genres = listOf(TmdbGenreResponse(id = 16, name = "Animation"), TmdbGenreResponse(id = 9648, name = "Mystery")),
            productionCompanies = listOf(TmdbCompanyResponse(id = 1, name = "bilibili"), TmdbCompanyResponse(id = 2, name = "Haoliners Animation League")),
            originalLanguage = "zh",
            originCountry = listOf("CN"),
            networks = listOf(TmdbNetworkResponse(id = 1, name = "bilibili"))
        )

        val details = TmdbDetails.Show(showResponse)
        val metadata = mapper.mapMediaDetails(mediaId = "tv:7046092", movie = details, language = "ru")

        assertEquals("Агент времени", metadata.title)
        assertEquals("时光代理人", metadata.originalTitle)
        assertEquals(AnimeSubType.CHINESE_DONGHUA, metadata.animeSubType)
        assertTrue(metadata.isAnime)
    }

    @Test
    fun testMapWesternAnimeInspiredResponse() {
        val showResponse = TmdbShowDetailResponse(
            id = 94605,
            name = "Аркейн",
            originalName = "Arcane",
            overview = "История сестёр...",
            genres = listOf(TmdbGenreResponse(id = 16, name = "Animation"), TmdbGenreResponse(id = 10765, name = "Sci-Fi & Fantasy")),
            productionCompanies = listOf(TmdbCompanyResponse(id = 1, name = "Fortiche Production"), TmdbCompanyResponse(id = 2, name = "Riot Games")),
            originalLanguage = "en",
            originCountry = listOf("US"),
            networks = listOf(TmdbNetworkResponse(id = 1, name = "Netflix"))
        )

        val details = TmdbDetails.Show(showResponse)
        val metadata = mapper.mapMediaDetails(mediaId = "tv:94605", movie = details, language = "ru")

        assertEquals("Аркейн", metadata.title)
        assertEquals(AnimeSubType.ANIME_INSPIRED, metadata.animeSubType)
        assertTrue(metadata.isAnime)
    }
}
