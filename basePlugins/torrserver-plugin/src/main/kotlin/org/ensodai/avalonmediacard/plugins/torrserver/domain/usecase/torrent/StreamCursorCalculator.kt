package org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.torrent

import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.contract.plugins.StreamCursor

fun calculateTargetCursor(streams: List<MediaStream>): StreamCursor? {
    val mapped = streams.filter { it.isMapped }
    if (mapped.isEmpty()) return null

    val recent = mapped.filter { it.isWatched || it.watchedProgressSeconds != null }
        .maxWithOrNull(
            compareBy<MediaStream> { it.lastWatchedAtEpochMs ?: 0L }
                .thenBy { it.seasonNumber ?: 0 }
                .thenBy { it.episodeNumber ?: 0 }
        )

    if (recent != null) {
        val s = recent.seasonNumber ?: 1
        val e = recent.episodeNumber ?: 1

        if (!recent.isWatched) {
            return StreamCursor(s, e, recent.watchedProgressSeconds, recent.episodeName)
        } else {
            val nextEp = mapped.find { it.seasonNumber == s && it.episodeNumber == e + 1 }
            if (nextEp != null) {
                return StreamCursor(s, e + 1, null, nextEp.episodeName)
            }
        }
    }

    val first = mapped.first()
    return StreamCursor(
        first.seasonNumber ?: 1,
        first.episodeNumber ?: 1,
        first.watchedProgressSeconds,
        first.episodeName
    )
}

fun resolveTargetStream(
    mappedStreams: List<MediaStream>,
    targetSeason: Int? = null,
    targetEpisode: Int? = null
): Pair<MediaStream, StreamCursor?>? {
    val mapped = mappedStreams.filter { it.isMapped }
    if (mapped.isEmpty()) {
        return mappedStreams.firstOrNull()?.let { it to null }
    }

    if (targetSeason != null && targetEpisode != null) {
        val target = mapped.find { it.seasonNumber == targetSeason && it.episodeNumber == targetEpisode }
            ?: mapped.firstOrNull { it.seasonNumber == targetSeason }
            ?: mapped.firstOrNull()
        val cursor = target?.let {
            StreamCursor(
                season = it.seasonNumber ?: targetSeason,
                episode = it.episodeNumber ?: targetEpisode,
                progressSeconds = it.watchedProgressSeconds,
                episodeName = it.episodeName
            )
        }
        return if (target != null) target to cursor else null
    }

    if (targetSeason != null) {
        val seasonStreams = mapped.filter { it.seasonNumber == targetSeason }
        if (seasonStreams.isNotEmpty()) {
            val cursor = calculateTargetCursor(seasonStreams)
            val stream = if (cursor != null) {
                seasonStreams.find { it.seasonNumber == cursor.season && it.episodeNumber == cursor.episode }
                    ?: seasonStreams.first()
            } else {
                seasonStreams.first()
            }
            return stream to cursor
        }
    }

    val cursor = calculateTargetCursor(mapped)
    val stream = if (cursor != null) {
        mapped.find { it.seasonNumber == cursor.season && it.episodeNumber == cursor.episode }
            ?: mapped.firstOrNull()
    } else {
        mapped.firstOrNull()
    }
    return stream?.let { it to cursor }
}


