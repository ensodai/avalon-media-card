package org.ensodai.avalonmediacard.plugins.torrserver.parsers

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.ensodai.avalonmediacard.contract.parsers.EpisodeMatcher
import org.ensodai.avalonmediacard.contract.parsers.MappingResult
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore
class MappingOffsetTest {

    @Test
    fun `test ds9 mapping and save to file`() = runBlocking {
        // 1. Читаем все файлы из M3U
        val m3uFile = File("basePlugins/torrserver-plugin/ds9.m3u")
        if (!m3uFile.exists()) {
            println("Skipping manual test: ds9.m3u does not exist")
            return@runBlocking
        }
        val videoFiles = mutableListOf<String>()
        for (line in m3uFile.readLines()) {
            if (line.startsWith("#EXTINF")) {
                videoFiles.add(line.substringAfterLast(",").trim())
            }
        }

        // 2. Парсим торрент
        val matcher = EpisodeMatcher()
        val parseResults = matcher.parseBatch("Deep Space Nine [960p]", videoFiles)

        val sortedMappings = mutableListOf<Pair<String, MappingResult.Success>>()
        for (file in videoFiles) {
            val result = parseResults[file]
            if (result is MappingResult.Success) {
                sortedMappings.add(Pair(file, result))
            }
        }

        // 3. Функция для загрузки данных из TMDB для любого сезона
        val token = System.getenv("TMDB_READ_TOKEN") ?: ""
        if (token.isBlank()) {
            println("Skipping TMDB fetch: TMDB_READ_TOKEN not configured")
            return@runBlocking
        }

        data class TmdbEp(val episodeNumber: Int, val name: String)

        val seasonCache = mutableMapOf<Int, List<TmdbEp>>()

        fun getTmdbSeason(season: Int): List<TmdbEp> {
            return seasonCache.getOrPut(season) {
                val url = URL("https://api.themoviedb.org/3/tv/580/season/$season?language=en-US")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Accept", "application/json")

                val tmdbResponse = try {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } catch (e: java.io.FileNotFoundException) {
                    "{}" // Возвращаем пустой JSON, если сезон не найден (HTTP 404)
                } catch (e: Exception) {
                    "{}"
                }

                val tmdbJson = Json { ignoreUnknownKeys = true }.parseToJsonElement(tmdbResponse).jsonObject
                val tmdbEpisodesArray = tmdbJson["episodes"]?.jsonArray ?: buildJsonArray {}

                val tmdbEpisodes = mutableListOf<TmdbEp>()
                for (epElement in tmdbEpisodesArray) {
                    val epObj = epElement.jsonObject
                    val epNum = epObj["episode_number"]?.jsonPrimitive?.int ?: 0
                    val epName = epObj["name"]?.jsonPrimitive?.content ?: "UNKNOWN"
                    tmdbEpisodes.add(TmdbEp(epNum, epName))
                }
                tmdbEpisodes
            }
        }

        // 4. Прогоняем маппинг ВСЕГО ФАЙЛА
        val outputFile = File("build/mapping_result_full.txt")
        outputFile.parentFile.mkdirs()
        val writer = outputFile.bufferedWriter()

        writer.write("--- ПОЛНЫЙ РЕЗУЛЬТАТ МАППИНГА ВСЕХ СЕЗОНОВ ---\n\n")

        val seasonOffsets = mutableMapOf<Int, Int>()

        for ((fileName, result) in sortedMappings) {
            val seasonsArray = result.seasons
            val episodesArray = result.episodes

            if (seasonsArray.isEmpty() || episodesArray.isEmpty()) continue

            val season = seasonsArray.first()
            val episode = episodesArray.first()

            // Получаем сдвиг ИМЕННО ДЛЯ ЭТОГО СЕЗОНА
            val currentOffset = seasonOffsets.getOrDefault(season, 0)
            val targetTmdbEpisode = episode - currentOffset

            // Вытягиваем базу TMDB для этого сезона
            val tmdbEpisodes = getTmdbSeason(season)
            val matchedTmdb = tmdbEpisodes.find { it.episodeNumber == targetTmdbEpisode }
            val tmdbName = matchedTmdb?.name ?: "UNKNOWN (NOT FOUND)"

            writer.write("Файл: $fileName\n")
            writer.write("  -> Распарсен массив серий: $episodesArray для сезона $season\n")
            writer.write("  -> Сдвиг для сезона $season: $currentOffset\n")
            writer.write("  -> РЕЗУЛЬТАТ СКЛЕЙКИ: Привязана серия TMDB -> [S${season}E${targetTmdbEpisode}] $tmdbName\n\n")

            // Обновляем сдвиг ДЛЯ ЭТОГО СЕЗОНА
            seasonOffsets[season] = currentOffset + (episodesArray.size - 1)
        }

        writer.close()
    }
}
