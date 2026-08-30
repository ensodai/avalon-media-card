package org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.search

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.plugins.torrserver.domain.repository.SearchRepository
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.JackettResult
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.OpenTorrentInspectorCommand
import org.ensodai.avalonmediacard.contract.parsers.TitleParser
import kotlin.uuid.Uuid

class SearchTorrentsUseCase(
    private val context: PluginContext,
    private val searchRepository: SearchRepository
) {

    suspend fun execute(
        key: MediaKey,
        season: Int?,
        episode: Int?,
        userId: Uuid?
    ): List<MediaStream> {
        val logger = context.logger
        logger.info("[TorrServerPlugin] Запуск поиска торрентов для key=$key")

        val isTv = key.type == EntityType.TV
        val metadata = context.catalog.getMediaDetails(key)
        val year = metadata.releaseDate?.split("-")?.firstOrNull() ?: ""
        val searchTitle = metadata.originalTitle ?: metadata.title
        val query = if (isTv) searchTitle else "$searchTitle $year".trim()

        logger.info("ИЩЕМ по запросу: \"$query\"")

        val allResults = mutableListOf<JackettResult>()
        val prowlarrSettings = context.integrationManager.getProwlarrSettings(userId)
        if (prowlarrSettings != null) {
            logger.info("Поиск торрентов через Prowlarr (${prowlarrSettings.source}): ${prowlarrSettings.url}")
            allResults.addAll(searchRepository.searchProwlarr(prowlarrSettings.url, prowlarrSettings.apiKey, query))
        }

        val jackettSettings = context.integrationManager.getJackettSettings(userId)
        if (jackettSettings != null) {
            logger.info("Поиск торрентов через Jackett (${jackettSettings.source}): ${jackettSettings.url}")
            allResults.addAll(searchRepository.searchJackett(jackettSettings.url, jackettSettings.apiKey, query))
        }

        return allResults
            .asSequence()
            .filter { it.seeders >= 3 }
            .filter { it.size > if (isTv) 300 * 1024 * 1024L else 1024 * 1024 * 1024L }
            .filter { result ->
                if (!isTv && year.isNotEmpty()) {
                    val y = year.toIntOrNull()
                    y == null || result.title.contains(year) ||
                            result.title.contains((y - 1).toString()) ||
                            result.title.contains((y + 1).toString())
                } else true
            }
            .map { result ->
                val rawUrl = result.magnetUri ?: result.link ?: ""
                val url = if (rawUrl.isNotEmpty() && isTv && season != null && episode != null) {
                    val separator = if (rawUrl.contains("?")) "&" else "?"
                    "$rawUrl${separator}season=$season&episode=$episode"
                } else {
                    rawUrl
                }
                val streamType = if (url.startsWith("magnet:")) StreamType.Magnet else StreamType.Torrent
                val title = result.title

                val clickAct = OpenTorrentInspectorCommand(
                    url = url,
                    title = title,
                    mediaId = key.id,
                    mediaType = key.type
                )

                MediaStream(
                    id = url,
                    title = title,
                    url = url,
                    type = streamType,
                    quality = TitleParser.parseQuality(title),
                    format = TitleParser.parseFormat(title),
                    videoCodec = TitleParser.parseCodec(title),
                    audioCodec = TitleParser.parseAudio(title),
                    isHdr = TitleParser.parseIsHdr(title),
                    sizeBytes = result.size,
                    sourceName = result.tracker ?: "Jackett",
                    seeders = result.seeders,
                    leechers = result.peers,
                    clickAction = clickAct
                )
            }
            .filter { it.url.isNotEmpty() }
            .sortedByDescending { it.seeders ?: 0 }
            .toList()
    }
}
