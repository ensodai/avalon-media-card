package org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.torrent

import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.RemapTorrentFileCommand
import kotlin.uuid.Uuid

class RemapTorrentUseCase(
    private val context: PluginContext
) {
    suspend fun execute(cmd: RemapTorrentFileCommand, userId: Uuid?) {
        if (cmd.mediaId.isNotEmpty()) {
            context.logger.info("Сохраняем ручной маппинг для ${cmd.filePath} -> S${cmd.season}E${cmd.episode}")

            context.torrentMappings.saveMapping(
                torrentHash = cmd.hash,
                filePath = cmd.filePath,
                seasons = listOf(cmd.season),
                episodes = listOf(cmd.episode),
                isAbsolute = false,
                isManual = true,
                mediaId = cmd.mediaId,
                fileIndex = cmd.fileIndex,
                fileSize = cmd.fileSize
            )

            if (userId != null && cmd.mediaId.isNotEmpty()) {
                context.userMediaBindings.saveBinding(userId, cmd.mediaId, "torrserver", cmd.hash)
            }
        }
    }
}
