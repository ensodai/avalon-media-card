package org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.playback

import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.RemapTorrentFileCommand
import org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.torrent.RemapTorrentUseCase
import kotlin.uuid.Uuid

class HandleRemapTorrentFileCommandUseCase(
    private val remapUseCase: RemapTorrentUseCase,
    private val resolveRemappedStreamUseCase: ResolveRemappedStreamUseCase
) {
    suspend fun execute(cmd: RemapTorrentFileCommand, userId: Uuid?): ActionResult {
        if (userId == null) return ActionResult.NoOp
        remapUseCase.execute(cmd, userId)

        val action = resolveRemappedStreamUseCase.execute(cmd, userId)
        if (action != ActionResult.NoOp) {
            return action
        }
        return ActionResult.NoOp
    }
}
