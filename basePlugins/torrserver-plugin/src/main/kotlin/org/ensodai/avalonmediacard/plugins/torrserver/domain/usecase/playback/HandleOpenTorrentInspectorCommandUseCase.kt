package org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.playback

import org.ensodai.avalonmediacard.contract.rpc.SourceSelectionResult
import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.OpenTorrentInspectorCommand
import org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.torrent.InspectAndMapTorrentUseCase
import kotlin.uuid.Uuid

class HandleOpenTorrentInspectorCommandUseCase(
    private val inspectUseCase: InspectAndMapTorrentUseCase,
    private val resolveInspectorStreamUseCase: ResolveInspectorStreamUseCase
) {
    suspend fun execute(cmd: OpenTorrentInspectorCommand, userId: Uuid?): ActionResult {
        if (userId == null) return ActionResult.NoOp
        val result = inspectUseCase.execute(cmd.url, cmd.title, cmd.mediaId, cmd.mediaType, userId)

        if (result is SourceSelectionResult.Ready && cmd.mediaId != null) {
            val action = resolveInspectorStreamUseCase.execute(cmd.mediaId, cmd.mediaType, userId)
            if (action != null) {
                return action
            }
        }
        return ActionResult.NoOp
    }
}

