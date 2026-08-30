package org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.playback

import org.ensodai.avalonmediacard.contract.rpc.SourceSelectionResult
import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.contract.slot.UploadCustomTorrentCommand
import org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.torrent.InspectAndMapTorrentUseCase
import kotlin.uuid.Uuid

class HandleUploadCustomTorrentCommandUseCase(
    private val inspectUseCase: InspectAndMapTorrentUseCase,
    private val resolveInspectorStreamUseCase: ResolveInspectorStreamUseCase
) {
    suspend fun execute(command: UploadCustomTorrentCommand, userId: Uuid?): ActionResult {
        if (userId == null) return ActionResult.NoOp
        return try {
            val title = command.fileName.removeSuffix(".torrent")
            val result = inspectUseCase.execute(
                magnetUrl = "",
                title = title,
                mediaId = command.key.id,
                mediaType = command.key.type,
                userId = userId,
                torrentFileBytes = command.fileBytes
            )
            if (result is SourceSelectionResult.Ready) {
                val action = resolveInspectorStreamUseCase.execute(command.key.id, command.key.type, userId)
                action ?: ActionResult.Error(500, "Не удалось подготовить поток для воспроизведения")
            } else if (result is SourceSelectionResult.Error) {
                ActionResult.Error(500, result.message)
            } else {
                ActionResult.Error(500, "Не удалось распознать серии или добавить торрент в TorrServer")
            }
        } catch (e: Exception) {
            ActionResult.Error(500, "Ошибка при загрузке торрент-файла: ${e.message}")
        }
    }
}

