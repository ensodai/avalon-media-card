package org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.torrent

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.ensodai.avalonmediacard.contract.parsers.EpisodeMatcher
import org.ensodai.avalonmediacard.contract.parsers.MappingResult
import org.ensodai.avalonmediacard.contract.parsers.TitleParser
import org.ensodai.avalonmediacard.contract.plugins.AudioTrack
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.contract.plugins.SubtitleTrack
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.TorrServerFile
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.TorrServerGstProbeInfo
import org.ensodai.avalonmediacard.plugins.torrserver.domain.repository.TorrServerRepository
import kotlin.uuid.Uuid

class PrepareStreamUseCase(
    private val context: PluginContext,
    private val torrServerRepository: TorrServerRepository
) {
    private fun String.isVideoFile(): Boolean {
        val ext = this.substringAfterLast('.', "").lowercase()
        return ext in listOf("mkv", "mp4", "avi", "mov", "m4v")
    }

    private data class ParsedStream(
        val cleanUrl: String,
        val existingHash: String?,
        val season: Int?,
        val episode: Int?,
        val fileIndex: Int?
    )

    private data class PlaybackResolution(
        val useGst: Boolean,
        val audioTracks: List<AudioTrack>,
        val subtitleTracks: List<SubtitleTrack>
    )

    suspend fun execute(stream: MediaStream, userId: Uuid?): MediaStream {
        if (stream.type != StreamType.Torrent &&
            !stream.sourceName.contains("TorrServer", ignoreCase = true) &&
            !stream.url.contains("/stream/video") &&
            !stream.url.contains("magnet:") &&
            !stream.url.endsWith(".torrent")
        ) {
            return stream
        }

        val logger = context.logger
        logger.info("Подготовка торрент-стрима: ${stream.title}")

        val parsed = parseStreamUrl(stream.url)
        val hash = getOrAddTorrent(parsed, userId)
        val files = torrServerRepository.getFiles(hash, userId) ?: emptyList()

        var fileIndex = parsed.fileIndex ?: 1

        if (files.isNotEmpty()) {
            mapEpisodesToDatabase(hash, stream.title, files)
            fileIndex = resolveTargetFileIndex(files, hash, parsed.season, parsed.episode, stream.title, parsed.fileIndex)
        }

        val fileName = files.find { it.id == fileIndex }?.path ?: "video.mp4"
        val resolution = resolvePlaybackMethod(hash, fileIndex, fileName, userId)

        val finalUrl = torrServerRepository.buildStreamUrl(
            hash = hash,
            fileIndex = fileIndex,
            filePath = fileName,
            userId = userId,
            useGst = resolution.useGst
        )

        val ext = fileName.substringAfterLast('.', "mp4")
        logger.info("Торрент подготовлен. Ссылка для плеера: $finalUrl (Формат: $ext, Эпизод index: $fileIndex, GST: ${resolution.useGst})")
        logger.info("Подготовка завершена. Найдено аудио: ${resolution.audioTracks.size}, субтитров: ${resolution.subtitleTracks.size}")

        return stream.copy(
            url = finalUrl,
            audioTracks = resolution.audioTracks,
            subtitleTracks = resolution.subtitleTracks
        )
    }

    private fun parseStreamUrl(rawUrl: String): ParsedStream {
        val urlObj = try { Url(rawUrl) } catch (e: Exception) { null }
        
        val season = urlObj?.parameters?.get("season")?.toIntOrNull() 
            ?: rawUrl.substringAfter("season=", "").substringBefore("&").toIntOrNull()
        
        val episode = urlObj?.parameters?.get("episode")?.toIntOrNull() 
            ?: rawUrl.substringAfter("episode=", "").substringBefore("&").toIntOrNull()
        
        val cleanUrl = rawUrl.substringBefore("&season=").substringBefore("?season=").substringBefore("&episode=").substringBefore("?episode=")

        val decodedUrl = try {
            val encoded = rawUrl.substringAfter("url=", "").substringBefore("&")
            if (encoded.isNotEmpty()) String(java.util.Base64.getUrlDecoder().decode(encoded)) else rawUrl
        } catch (_: Exception) {
            rawUrl
        }

        val decodedUrlObj = try { Url(decodedUrl) } catch(e: Exception) { null }

        val hash = decodedUrlObj?.parameters?.get("link") 
            ?: urlObj?.parameters?.get("link")
            ?: if (decodedUrl.contains("/gst/")) decodedUrl.substringAfter("/gst/").substringBefore("/") 
            else if (rawUrl.contains("/gst/")) rawUrl.substringAfter("/gst/").substringBefore("/") 
            else null

        val index = decodedUrlObj?.parameters?.get("index")?.toIntOrNull()
            ?: urlObj?.parameters?.get("index")?.toIntOrNull()
            ?: if (decodedUrl.contains("index=")) decodedUrl.substringAfter("index=").substringBefore("&").toIntOrNull() else null

        return ParsedStream(cleanUrl, hash, season, episode, index)
    }

    private suspend fun getOrAddTorrent(parsed: ParsedStream, userId: Uuid?): String {
        if (!parsed.existingHash.isNullOrBlank()) {
            context.logger.info("Торрент уже добавлен в TorrServer! Хэш: ${parsed.existingHash}")
            return parsed.existingHash
        }

        val torrentFileBytes: ByteArray? = if (parsed.cleanUrl.startsWith("http") && !parsed.cleanUrl.contains("/stream-proxy/")) {
            try {
                val resp = context.httpClient.get(parsed.cleanUrl)
                if (resp.status.isSuccess()) resp.body<ByteArray>() else null
            } catch (e: Exception) {
                context.logger.warn("Не удалось скачать .torrent файл: ${e.message}")
                null
            }
        } else null

        val hash = torrServerRepository.addTorrent(parsed.cleanUrl, torrentFileBytes, userId)
        return hash ?: throw Exception("TorrServer не вернул инфо-хэш торрента")
    }

    private suspend fun mapEpisodesToDatabase(
        hash: String, 
        rootTitle: String, 
        files: List<TorrServerFile>
    ) {
        val dbMappings = context.torrentMappings.getMappingsByHash(hash).toMutableList()
        val missingFiles = files.filter { f -> dbMappings.none { it.filePath == f.path } }

        if (missingFiles.isEmpty()) return

        val matcher = EpisodeMatcher()
        for (f in missingFiles) {
            if (!f.path.isVideoFile()) continue

            val result = matcher.parse(rootTitle, f.path)
            var seasons: List<Int>? = null
            var episodes: List<Int>? = null
            var isAbsolute = false

            when (result) {
                is MappingResult.Success -> {
                    seasons = result.seasons
                    episodes = result.episodes
                    isAbsolute = result.isAbsolute
                }
                is MappingResult.Partial -> {
                    episodes = result.episodes
                    isAbsolute = result.isAbsolute
                }
                is MappingResult.Failed -> {}
            }

            val newMapping = context.torrentMappings.saveMapping(
                torrentHash = hash,
                filePath = f.path,
                seasons = seasons,
                episodes = episodes,
                isAbsolute = isAbsolute,
                isManual = false,
                fileIndex = f.id,
                fileSize = f.length
            )
            dbMappings.add(newMapping)
        }
    }

    private suspend fun resolveTargetFileIndex(
        files: List<TorrServerFile>,
        hash: String,
        seasonParam: Int?,
        episodeParam: Int?,
        streamTitle: String,
        existingFileIndex: Int?
    ): Int {
        val dbMappings = context.torrentMappings.getMappingsByHash(hash)
        var fileIndex = existingFileIndex ?: 1

        if (seasonParam != null || episodeParam != null) {
            val episodeMatch = episodeParam ?: TitleParser.parseEpisodePattern(streamTitle)
            val matchedMapping = dbMappings.find { m ->
                val seasonMatches = seasonParam == null || m.seasons?.contains(seasonParam) == true
                val episodeMatches = episodeMatch == null || m.episodes?.contains(episodeMatch) == true
                m.episodes != null && seasonMatches && episodeMatches
            }

            if (matchedMapping != null) {
                files.find { it.path == matchedMapping.filePath }?.let {
                    fileIndex = it.id
                    context.logger.info("Найден файл по маппингу S$seasonParam E$episodeParam: ${it.path} (index: $fileIndex)")
                }
            } else {
                files.filter { it.path.isVideoFile() }.maxByOrNull { it.length ?: 0L }?.let {
                    fileIndex = it.id
                    context.logger.info("Маппинг не нашел S$seasonParam E$episodeParam. Фолбэк на самый большой файл: ${it.path}")
                }
            }
        } else if (existingFileIndex == null) {
            files.maxByOrNull { it.length ?: 0L }?.let {
                fileIndex = it.id ?: 1
                context.logger.info("Выбран самый большой файл для фильма: ${it.path} (index: $fileIndex)")
            }
        }
        return fileIndex
    }

    private suspend fun resolvePlaybackMethod(
        hash: String,
        fileIndex: Int,
        fileName: String,
        userId: Uuid?
    ): PlaybackResolution {
        val useGstRequested = context.integrationManager.getTorrServerUseGst(userId)
        var actualUseGst = useGstRequested
        val audioTracks = mutableListOf<AudioTrack>()
        val subtitleTracks = mutableListOf<SubtitleTrack>()

        if (useGstRequested) {
            val probeInfo = torrServerRepository.getGstProbe(hash, fileIndex, userId)
            if (!isGstSupported(probeInfo, fileName)) {
                context.logger.warn("GST probe провалился или контейнер/кодек (${fileName.substringAfterLast('.', "")}) не поддерживается GStreamer HLS. Фоллбэк на обычный поток.")
                actualUseGst = false
            } else {
                probeInfo?.tracks?.forEach { track ->
                    if (track.type == "audio") {
                        val trackName = track.title.takeIf { !it.isNullOrBlank() } ?: track.language.takeIf { !it.isNullOrBlank() } ?: ("Track " + track.index)
                        audioTracks.add(AudioTrack(track.index.toString(), trackName, track.language, track.channels ?: 2, false))
                    } else if (track.type == "subtitle") {
                        val trackName = track.title.takeIf { !it.isNullOrBlank() } ?: track.language.takeIf { !it.isNullOrBlank() } ?: ("Subtitle " + track.index)
                        subtitleTracks.add(SubtitleTrack(track.index.toString(), trackName, track.language))
                    }
                }
            }
        }
        
        return PlaybackResolution(actualUseGst, audioTracks, subtitleTracks)
    }

    private fun isGstSupported(probeInfo: TorrServerGstProbeInfo?, fileName: String): Boolean {
        if (probeInfo == null) return false
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext == "avi") return false
        val container = probeInfo.container?.lowercase() ?: ""
        if (container.contains("avi")) return false
        val videoTrack = probeInfo.tracks?.find { it.type == "video" }
        val videoCodec = videoTrack?.codec?.lowercase() ?: ""
        if (videoCodec.contains("xvid") || videoCodec.contains("divx") || videoCodec.contains("mpeg4")) return false
        return true
    }
}
