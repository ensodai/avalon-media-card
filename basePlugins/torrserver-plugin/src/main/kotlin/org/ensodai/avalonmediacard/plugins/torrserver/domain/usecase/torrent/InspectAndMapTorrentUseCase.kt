package org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.torrent

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.parsers.EpisodeMatcher
import org.ensodai.avalonmediacard.contract.parsers.MappingResult
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.rpc.SourceSelectionResult
import org.ensodai.avalonmediacard.contract.slot.TorrentFileItem
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.RemapTorrentFileCommand
import org.ensodai.avalonmediacard.plugins.torrserver.domain.repository.TorrServerRepository
import kotlin.uuid.Uuid

class InspectAndMapTorrentUseCase(
    private val context: PluginContext,
    private val torrServerRepository: TorrServerRepository
) {
    private fun String.isVideoFile(): Boolean {
        val ext = this.substringAfterLast('.', "").lowercase()
        return ext in listOf("mkv", "mp4", "avi", "mov", "m4v")
    }

    suspend fun execute(
        magnetUrl: String,
        title: String,
        mediaId: String?,
        mediaType: EntityType? = null,
        userId: Uuid?,
        torrentFileBytes: ByteArray? = null,
        targetSeason: Int? = null,
        targetEpisode: Int? = null
    ): SourceSelectionResult {
        val logger = context.logger
        logger.info("Запуск инспектора для $magnetUrl (type: $mediaType, title: $title)")

        val cleanUrl = magnetUrl.substringBefore("&season=").substringBefore("?season=")

        val fileBytes: ByteArray? = torrentFileBytes ?: if (cleanUrl.startsWith("http")) {
            try {
                val resp = context.httpClient.get(cleanUrl)
                if (resp.status.isSuccess()) {
                    resp.body<ByteArray>()
                } else null
            } catch (e: Exception) {
                logger.warn("Не удалось скачать .torrent файл из Prowlarr/Jackett: ${e.message}")
                null
            }
        } else null

        val hash = torrServerRepository.addTorrent(cleanUrl, fileBytes, userId)
        if (hash == null) {
            context.logger.error("InspectAndMapTorrentUseCase: Failed to add torrent to TorrServer", null)
            return SourceSelectionResult.Error("Не удалось добавить торрент в TorrServer")
        }

        val files = torrServerRepository.getFiles(hash, userId)
        if (files == null || files.isEmpty()) {
            logger.error("Не удалось получить файлы торрента из TorrServer", null)
            return SourceSelectionResult.Error("Не удалось получить список файлов из раздачи")
        }

        if (mediaType == EntityType.MOVIE) {
            logger.info("Обработка ТОРРЕНТА ДЛЯ ФИЛЬМА")
            val largestVideoFile = files
                .filter { it.path.isVideoFile() }
                .maxByOrNull { it.length }

            if (largestVideoFile != null) {
                val mapping = context.torrentMappings.saveMapping(
                    torrentHash = hash,
                    filePath = largestVideoFile.path,
                    fileSize = largestVideoFile.length,
                    fileIndex = largestVideoFile.id,
                    seasons = null,
                    episodes = null,
                    isAbsolute = false,
                    isManual = false,
                    mediaId = mediaId
                )
                logger.info("Сохранили маппинг фильма в БД: hash=$hash, path=${largestVideoFile.path}, mediaId=${mapping.mediaId}")

                if (userId != null && mediaId != null) {
                    logger.info("Привязываем пользователя $userId к торренту фильма $hash для mediaId $mediaId")
                    context.userMediaBindings.saveBinding(
                        userId = userId,
                        mediaId = mediaId,
                        sourceType = "torrserver",
                        sourceId = hash
                    )
                }
                return SourceSelectionResult.Ready(null, null)
            } else {
                logger.error("В торренте фильма не найдено подходящих видеофайлов", null)
                torrServerRepository.dropTorrent(hash, userId)
                return SourceSelectionResult.Error("В раздаче не найдено видеофайлов")
            }
        }

        val filePaths = files.map { it.path }.filter { it.isNotEmpty() && it.isVideoFile() }
        logger.info("Отправляем в парсер ${filePaths.size} видеофайлов сериала")

        val matcher = EpisodeMatcher()
        val parseResults = matcher.parseBatch(title, filePaths)

        val hasErrors = parseResults.values.any { it is MappingResult.Failed || it is MappingResult.Partial }
        if (hasErrors) {
            val errors = parseResults.filter { it.value is MappingResult.Failed || it.value is MappingResult.Partial }
            logger.error(
                "Ошибки парсинга для файлов: ${errors.keys.joinToString { it.substringAfterLast('/') }} -> ${errors.values}",
                null
            )
        }

        val successCount = parseResults.values.count { it is MappingResult.Success }
        if (successCount > 0) {
            logger.info("Парсинг завершен. Успешных файлов: $successCount. Сохраняем в БД.")
            parseResults.forEach { (path, result) ->
                if (result is MappingResult.Success) {
                    val fileObj = files.find { it.path == path }
                    val mapping = context.torrentMappings.saveMapping(
                        torrentHash = hash,
                        filePath = path,
                        fileSize = fileObj?.length,
                        fileIndex = fileObj?.id,
                        seasons = result.seasons,
                        episodes = result.episodes,
                        isAbsolute = result.isAbsolute,
                        isManual = false,
                        mediaId = mediaId
                    )
                    logger.info("Сохранили маппинг в БД: hash=$hash, path=$path, mediaId=${mapping.mediaId}")
                }
            }

            if (userId != null && mediaId != null) {
                logger.info("Привязываем пользователя $userId к торренту $hash для сериала $mediaId")
                context.userMediaBindings.saveBinding(
                    userId = userId,
                    mediaId = mediaId,
                    sourceType = "torrserver",
                    sourceId = hash
                )
            }
        }

        if (hasErrors || successCount == 0) {
            val torrentFileItems = files.map { file ->
                val isVideo = file.path.isVideoFile()
                val match = parseResults[file.path]
                val mappedSeasons: List<Int>? = when (match) {
                    is MappingResult.Success -> match.seasons
                    else -> null
                }
                val mappedEpisodes: List<Int>? = when (match) {
                    is MappingResult.Success -> match.episodes
                    is MappingResult.Partial -> match.episodes
                    else -> null
                }
                TorrentFileItem(
                    path = file.path,
                    size = file.length,
                    isVideo = isVideo,
                    mappedSeasons = mappedSeasons,
                    mappedEpisodes = mappedEpisodes,
                    fileIndex = file.id,
                    remapAction = if (isVideo && mediaId != null) {
                        RemapTorrentFileCommand(
                            mediaId = mediaId,
                            hash = hash,
                            filePath = file.path,
                            season = mappedSeasons?.firstOrNull() ?: 1,
                            episode = mappedEpisodes?.firstOrNull() ?: 1,
                            fileIndex = file.id,
                            fileSize = file.length
                        )
                    } else null
                )
            }
            return SourceSelectionResult.RequiresManualMapping(
                torrentHash = hash,
                torrentTitle = title,
                files = torrentFileItems
            )
        }


        return SourceSelectionResult.Ready(
            targetSeason = targetSeason,
            targetEpisode = targetEpisode
        )
    }
}

