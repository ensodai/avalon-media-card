package org.ensodai.avalonmediacard.plugins.anilibria

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.runBlocking
import org.ensodai.avalonmediacard.contract.plugins.DefaultPluginLogger
import org.ensodai.avalonmediacard.contract.plugins.PluginLogger
import org.ensodai.avalonmediacard.plugins.anilibria.data.network.AniLibriaApiClient
import org.ensodai.avalonmediacard.plugins.anilibria.data.repository.AniLibriaRepositoryImpl
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AniLibriaIntegrationTest {

    private val testLogger = DefaultPluginLogger("AniLibria Test")

    @Test
    fun testSearchAndGetReleaseDetails() = runBlocking {
        val client = HttpClient(CIO)
        val apiClient = AniLibriaApiClient(client, testLogger)
        val repository = AniLibriaRepositoryImpl(apiClient)

        // Поиск популярного релиза
        val releases = repository.searchReleases("тиран")
        println("Найдено релизов: ${releases.size}")
        assertTrue(releases.isNotEmpty(), "Поиск должен вернуть хотя бы 1 релиз")

        val first = releases.first()
        println("Первый релиз: id=${first.id}, titleRu=${first.titleRu}, titleEn=${first.titleEn}")

        val details = repository.getReleaseDetails(first.id)
        assertNotNull(details, "Детали релиза должны быть получены")
        assertTrue(details.episodes.isNotEmpty(), "У релиза должны быть эпизоды")

        val firstEp = details.episodes.first()
        println("Первый эпизод: ordinal=${firstEp.ordinal}, hls=${firstEp.bestHlsUrl}, quality=${firstEp.bestQuality}")
        assertNotNull(firstEp.bestHlsUrl, "У эпизода должен быть валидный HLS URL")
        assertTrue(firstEp.bestHlsUrl!!.startsWith("http"), "HLS URL должен начинаться с http")

        client.close()
    }

    @Test
    fun testSearchNaruto() = runBlocking {
        val client = HttpClient(CIO)
        val apiClient = AniLibriaApiClient(client, testLogger)
        val repository = AniLibriaRepositoryImpl(apiClient)

        val queries = listOf("Наруто", "Naruto")
        for (q in queries) {
            println("\n=== ПОИСК АНИЛИБРИЯ: '$q' ===")
            val releases = repository.searchReleases(q)
            println("Всего найдено релизов: ${releases.size}\n")

            for ((idx, release) in releases.withIndex()) {
                println("--- Релиз #${idx + 1} ---")
                println("ID: ${release.id}")
                println("Название (RU): ${release.titleRu}")
                println("Название (EN): ${release.titleEn}")
                println("Год: ${release.year}")
                println("Заявлено серий (episodesTotal): ${release.episodesTotal}")

                val details = repository.getReleaseDetails(release.id)
                if (details != null) {
                    println("Доступно серий в базе (episodes.size): ${details.episodes.size}")
                    val first = details.episodes.firstOrNull()
                    val last = details.episodes.lastOrNull()
                    println("Диапазон серий: от ordinal=${first?.ordinal} до ordinal=${last?.ordinal}")
                }
                println()
            }
        }

        client.close()
    }
}
