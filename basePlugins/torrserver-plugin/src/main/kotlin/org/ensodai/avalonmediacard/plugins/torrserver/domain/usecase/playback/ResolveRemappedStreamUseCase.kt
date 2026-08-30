package org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.playback

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.resolveTargetStream
import org.ensodai.avalonmediacard.contract.slot.ActionPlayVideo
import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.RemapTorrentFileCommand
import org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.torrent.GetMappedStreamsUseCase
import kotlin.uuid.Uuid

class ResolveRemappedStreamUseCase(
    private val context: PluginContext,
    private val mappedStreamsUseCase: GetMappedStreamsUseCase
) {
    suspend fun execute(
        cmd: RemapTorrentFileCommand,
        userId: Uuid
    ): ActionResult {
        if (cmd.mediaId.isEmpty()) return ActionResult.NoOp

        val key = MediaKey(MediaProvider.Tmdb, EntityType.TV, cmd.mediaId)
        val boundHash = context.userMediaBindings.getBinding(userId, key.id, "torrserver")
        val mappedStreams = if (boundHash != null) mappedStreamsUseCase.execute(key, boundHash, userId) else emptyList()

        if (mappedStreams.isNotEmpty()) {
            val targetPair = resolveTargetStream(mappedStreams) ?: (mappedStreams.firstOrNull() to null)
            val targetStream = targetPair.first ?: return ActionResult.NoOp
            val preparedStream = context.streams.prepareStream(targetStream, userId) ?: targetStream

            return ActionResult.ExecuteAction(
                ActionPlayVideo(
                    url = preparedStream.url,
                    title = preparedStream.episodeName ?: preparedStream.title,
                    streamId = preparedStream.canonicalId,
                    durationSeconds = preparedStream.durationSeconds,
                    startPositionSeconds = targetPair.second?.progressSeconds,
                    playlist = mappedStreams.filter { it.isMapped },
                    mediaKey = key,
                    audioTracks = preparedStream.audioTracks,
                    subtitleTracks = preparedStream.subtitleTracks
                )
            )
        }
        return ActionResult.NoOp
    }
}
