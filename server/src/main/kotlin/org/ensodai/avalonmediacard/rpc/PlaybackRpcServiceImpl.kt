package org.ensodai.avalonmediacard.rpc

import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import org.ensodai.avalonmediacard.contract.auth.AuthState
import org.ensodai.avalonmediacard.contract.model.MediaCatalog
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.contract.plugins.UserMediaBindingProvider
import org.ensodai.avalonmediacard.contract.plugins.resolveTargetStream
import org.ensodai.avalonmediacard.contract.rpc.PlaybackMetadataResult
import org.ensodai.avalonmediacard.contract.rpc.PlaybackRpcService
import org.ensodai.avalonmediacard.contract.rpc.SourceSelectionResult
import org.ensodai.avalonmediacard.contract.rpc.StreamPlaybackResult
import org.ensodai.avalonmediacard.plugin.PluginManager
import org.ensodai.avalonmediacard.security.RpcSessionContext
import org.ensodai.avalonmediacard.security.StreamTokenService
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import org.slf4j.LoggerFactory
import java.util.Base64
import kotlin.uuid.Uuid

@Factory
class PlaybackRpcServiceImpl(
    @InjectedParam private val session: RpcSessionContext,
    private val userMediaBindings: UserMediaBindingProvider,
    private val pluginManager: PluginManager,
    private val mediaCatalog: MediaCatalog,
    private val streamTokenService: StreamTokenService
) : PlaybackRpcService {

    private val logger = LoggerFactory.getLogger(PlaybackRpcServiceImpl::class.java)

    private fun currentUserId(): Uuid? {
        val state = session.state.value
        return (state as? AuthState.Authorized)?.userId
    }

    override suspend fun getPlaybackMetadata(
        key: MediaKey,
        seasonNumber: Int?,
        episodeNumber: Int?
    ): PlaybackMetadataResult {
        val userId = currentUserId()
            ?: return PlaybackMetadataResult.Error("Пользователь не авторизован")

        val targetSeason = seasonNumber?.takeIf { it > 0 }
        val targetEpisode = episodeNumber?.takeIf { it > 0 }

        try {
            val activeBinding = userMediaBindings.getActiveBinding(userId, key.id)
                ?: return PlaybackMetadataResult.NoSourceBound

            val mappedStreams = pluginManager.getPlaylistForMedia(
                key = key,
                sourceId = activeBinding.sourceId,
                userId = userId,
                providerId = activeBinding.sourceType
            ) ?: emptyList()

            if (mappedStreams.isEmpty()) {
                return PlaybackMetadataResult.NoSourceBound
            }

            val targetPair = resolveTargetStream(mappedStreams, targetSeason, targetEpisode)
                ?: (mappedStreams.firstOrNull() to null)

            val targetStream = targetPair.first
                ?: return PlaybackMetadataResult.NoSourceBound

            val targetCursor = targetPair.second
            val mediaDetails = runCatching { mediaCatalog.getMediaDetails(key) }.getOrNull()
            val canonicalSeriesTitle = mediaDetails?.title?.takeIf { it.isNotBlank() }

            return PlaybackMetadataResult.Ready(
                currentSeason = targetStream.seasonNumber ?: targetCursor?.season,
                currentEpisode = targetStream.episodeNumber ?: targetCursor?.episode,
                episodeTitle = targetStream.episodeName ?: targetStream.title,
                seriesTitle = canonicalSeriesTitle ?: targetStream.title,
                durationSeconds = targetStream.durationSeconds,
                startPositionSeconds = targetStream.watchedProgressSeconds ?: targetCursor?.progressSeconds,
                playlist = mappedStreams,
                boundSourceTitle = targetStream.sourceName.ifBlank { activeBinding.sourceType }
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.error("Ошибка получения метаданных воспроизведения для key=${key.id}: ${e.message}", e)
            return PlaybackMetadataResult.Error("Ошибка получения метаданных: ${e.message}")
        }
    }

    override suspend fun getStreamUrl(
        key: MediaKey,
        seasonNumber: Int?,
        episodeNumber: Int?
    ): StreamPlaybackResult {
        val userId = currentUserId()
            ?: return StreamPlaybackResult.Error("Пользователь не авторизован")

        val targetSeason = seasonNumber?.takeIf { it > 0 }
        val targetEpisode = episodeNumber?.takeIf { it > 0 }

        try {
            val activeBinding = userMediaBindings.getActiveBinding(userId, key.id)
                ?: return StreamPlaybackResult.NoSourceBound("Источник не выбран")

            val mappedStreams = pluginManager.getPlaylistForMedia(
                key = key,
                sourceId = activeBinding.sourceId,
                userId = userId,
                providerId = activeBinding.sourceType
            ) ?: emptyList()

            val targetPair = resolveTargetStream(mappedStreams, targetSeason, targetEpisode)
                ?: (mappedStreams.firstOrNull() to null)

            val targetStream = targetPair.first
                ?: return StreamPlaybackResult.NoSourceBound("Серия не найдена в текущем источнике")

            val preparedStream = pluginManager.prepareStream(targetStream, userId)
            val secureUrl = sanitizeAndWrapStreamUrl(preparedStream.url, userId, preparedStream.type)

            return StreamPlaybackResult.Ready(
                streamUrl = secureUrl,
                streamId = preparedStream.canonicalId,
                durationSeconds = preparedStream.durationSeconds,
                startPositionSeconds = targetPair.second?.progressSeconds,
                audioTracks = preparedStream.audioTracks,
                subtitleTracks = preparedStream.subtitleTracks,
                playlist = mappedStreams
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.error("Ошибка подготовки потока для key=${key.id}: ${e.message}", e)
            return StreamPlaybackResult.Error("Ошибка подготовки потока: ${e.message}")
        }
    }

    private fun sanitizeAndWrapStreamUrl(rawUrl: String, userId: Uuid?, streamType: StreamType? = null): String {
        if (rawUrl.startsWith("/gst/")) {
            return rawUrl
        }
        if (rawUrl.startsWith("/api/stream-proxy/") && !rawUrl.contains("?url=")) {
            return rawUrl
        }

        // Если источник вернул query-формат /api/stream-proxy?url=base64...
        if (rawUrl.startsWith("/api/stream-proxy") && rawUrl.contains("?url=")) {
            try {
                val queryParams = parseQueryString(rawUrl.substringAfter("?"))
                val encodedUrl = queryParams["url"] ?: return rawUrl
                val decodedTarget = String(Base64.getUrlDecoder().decode(encodedUrl), Charsets.UTF_8)
                val customHeaders = mutableMapOf<String, String>()

                queryParams["referer"]?.let {
                    val dec = runCatching { String(Base64.getUrlDecoder().decode(it), Charsets.UTF_8) }.getOrDefault(it)
                    customHeaders[HttpHeaders.Referrer] = dec
                }
                queryParams["origin"]?.let {
                    val dec = runCatching { String(Base64.getUrlDecoder().decode(it), Charsets.UTF_8) }.getOrDefault(it)
                    customHeaders["Origin"] = dec
                }
                queryParams["userAgent"]?.let {
                    val dec = runCatching { String(Base64.getUrlDecoder().decode(it), Charsets.UTF_8) }.getOrDefault(it)
                    customHeaders[HttpHeaders.UserAgent] = dec
                }
                val authHeader = queryParams["auth"]?.let {
                    if (it.startsWith("Basic ")) it
                    else runCatching { String(Base64.getUrlDecoder().decode(it)) }.getOrDefault(it)
                }

                return streamTokenService.wrapUrl(
                    targetUrl = decodedTarget,
                    userId = userId,
                    streamType = streamType,
                    headers = customHeaders,
                    authHeader = authHeader
                )
            } catch (e: Exception) {
                logger.warn("Не удалось преобразовать старый URL стрим-прокси: {}", e.message)
            }
        }

        // Если это прямой внешний URL (http/https)
        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            return streamTokenService.wrapUrl(
                targetUrl = rawUrl,
                userId = userId,
                streamType = streamType
            )
        }

        return rawUrl
    }

    override suspend fun selectSource(
        key: MediaKey,
        providerId: String,
        sourceId: String,
        seasonNumber: Int?,
        episodeNumber: Int?
    ): SourceSelectionResult {
        val userId = currentUserId() ?: return SourceSelectionResult.Error("Пользователь не авторизован")
        logger.info("selectSource: key=${key.id}, provider=$providerId, sourceId=$sourceId, season=$seasonNumber, episode=$episodeNumber")

        userMediaBindings.saveBinding(userId, key.id, providerId, sourceId)
        val playlist = pluginManager.getPlaylistForMedia(key, sourceId, userId, providerId)

        if (playlist.isNullOrEmpty() && (providerId.contains("torrserver", ignoreCase = true) || sourceId.startsWith("magnet:"))) {
            return SourceSelectionResult.Error("Не удалось получить потоки из выбранного источника")
        }

        return SourceSelectionResult.Ready(
            targetSeason = seasonNumber,
            targetEpisode = episodeNumber
        )
    }

    override suspend fun searchSources(
        key: MediaKey,
        forceRefresh: Boolean
    ): Boolean {
        val userId = currentUserId() ?: return false
        pluginManager.searchSources(key, userId, forceRefresh)
        return true
    }
}
