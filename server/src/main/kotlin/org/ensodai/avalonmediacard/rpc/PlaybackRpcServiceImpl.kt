package org.ensodai.avalonmediacard.rpc

import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import org.ensodai.avalonmediacard.contract.auth.AuthState
import org.ensodai.avalonmediacard.contract.model.MediaCatalog
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.withUserSettings
import org.ensodai.avalonmediacard.contract.plugins.StreamType
import org.ensodai.avalonmediacard.contract.plugins.UserMediaBindingProvider
import org.ensodai.avalonmediacard.contract.plugins.resolveTargetStream
import org.ensodai.avalonmediacard.contract.rpc.PlaybackMetadataResult
import org.ensodai.avalonmediacard.contract.rpc.PlaybackRpcService
import org.ensodai.avalonmediacard.contract.rpc.SourceSelectionResult
import org.ensodai.avalonmediacard.contract.rpc.StreamPlaybackResult
import org.ensodai.avalonmediacard.plugin.PluginManager
import org.ensodai.avalonmediacard.repository.UserSettingsRepository
import org.ensodai.avalonmediacard.security.RpcSessionContext
import org.ensodai.avalonmediacard.security.StreamProxyService
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

@Factory
class PlaybackRpcServiceImpl(
    @InjectedParam private val session: RpcSessionContext,
    private val userMediaBindings: UserMediaBindingProvider,
    private val pluginManager: PluginManager,
    private val mediaCatalog: MediaCatalog,
    private val streamProxyService: StreamProxyService,
    private val userSettingsRepository: UserSettingsRepository
) : PlaybackRpcService {

    private val logger = LoggerFactory.getLogger(PlaybackRpcServiceImpl::class.java)

    private fun currentUserId(): Uuid? {
        val state = session.state.value
        return (state as? AuthState.Authorized)?.userId
    }

    override suspend fun getPlaybackMetadata(
        key: MediaKey,
        seasonNumber: Int?,
        episodeNumber: Int?,
        sourceType: String?,
        sourceId: String?
    ): PlaybackMetadataResult {
        val userId = currentUserId()
            ?: return PlaybackMetadataResult.Error("Пользователь не авторизован")

        val targetSeason = seasonNumber?.takeIf { it > 0 }
        val targetEpisode = episodeNumber?.takeIf { it > 0 }

        try {
            val resolvedProviderId: String
            val resolvedSourceId: String

            if (!sourceType.isNullOrBlank() && !sourceId.isNullOrBlank()) {
                resolvedProviderId = sourceType
                resolvedSourceId = sourceId
            } else {
                val activeBinding = userMediaBindings.getActiveBinding(userId, key.id)
                    ?: return PlaybackMetadataResult.NoSourceBound
                resolvedProviderId = activeBinding.sourceType
                resolvedSourceId = activeBinding.sourceId
            }

            val mappedStreams = pluginManager.getPlaylistForMedia(
                key = key,
                sourceId = resolvedSourceId,
                userId = userId,
                providerId = resolvedProviderId
            ) ?: emptyList()

            if (mappedStreams.isEmpty()) {
                return PlaybackMetadataResult.NoSourceBound
            }

            val targetPair = resolveTargetStream(mappedStreams, targetSeason, targetEpisode)
                ?: (mappedStreams.firstOrNull() to null)

            val targetStream = targetPair.first
                ?: return PlaybackMetadataResult.NoSourceBound

            val targetCursor = targetPair.second
            val userSettings = if (userId != null) runCatching { userSettingsRepository.getUserSettings(userId) }.getOrNull() else null
            val targetLang = userSettings?.uiLocale?.takeIf { it.isNotBlank() } ?: "ru"
            val mediaDetails = runCatching { mediaCatalog.getMediaDetails(key, language = targetLang) }.getOrNull()
            val customizedDetails = mediaDetails?.withUserSettings(userSettings)
            val canonicalSeriesTitle = customizedDetails?.title?.takeIf { it.isNotBlank() }

            val fallbackDuration = targetStream.durationSeconds
                ?: customizedDetails?.runtime?.takeIf { it > 0 }?.let { (it * 60).toDouble() }

            val preparedStream = runCatching { pluginManager.prepareStream(targetStream, userId) }.getOrDefault(targetStream)
            val sanitizedPlaylist = streamProxyService.sanitizePlaylist(
                streams = mappedStreams,
                userId = userId,
                defaultHeaders = preparedStream.headers
            )

            return PlaybackMetadataResult.Ready(
                currentSeason = targetStream.seasonNumber ?: targetCursor?.season,
                currentEpisode = targetStream.episodeNumber ?: targetCursor?.episode,
                episodeTitle = targetStream.episodeName ?: targetStream.title,
                seriesTitle = canonicalSeriesTitle ?: targetStream.title,
                durationSeconds = fallbackDuration,
                startPositionSeconds = targetStream.watchedProgressSeconds ?: targetCursor?.progressSeconds,
                playlist = sanitizedPlaylist,
                boundSourceTitle = targetStream.sourceName.ifBlank { resolvedProviderId }
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
        episodeNumber: Int?,
        sourceType: String?,
        sourceId: String?
    ): StreamPlaybackResult {
        val userId = currentUserId()
            ?: return StreamPlaybackResult.Error("Пользователь не авторизован")

        val targetSeason = seasonNumber?.takeIf { it > 0 }
        val targetEpisode = episodeNumber?.takeIf { it > 0 }

        try {
            val resolvedProviderId: String
            val resolvedSourceId: String

            if (!sourceType.isNullOrBlank() && !sourceId.isNullOrBlank()) {
                resolvedProviderId = sourceType
                resolvedSourceId = sourceId
            } else {
                val activeBinding = userMediaBindings.getActiveBinding(userId, key.id)
                    ?: return StreamPlaybackResult.NoSourceBound("Источник не выбран")
                resolvedProviderId = activeBinding.sourceType
                resolvedSourceId = activeBinding.sourceId
            }

            val mappedStreams = pluginManager.getPlaylistForMedia(
                key = key,
                sourceId = resolvedSourceId,
                userId = userId,
                providerId = resolvedProviderId
            ) ?: emptyList()

            val targetPair = resolveTargetStream(mappedStreams, targetSeason, targetEpisode)
                ?: (mappedStreams.firstOrNull() to null)

            val targetStream = targetPair.first
                ?: return StreamPlaybackResult.NoSourceBound("Серия не найдена в текущем источнике")

            val preparedStream = pluginManager.prepareStream(targetStream, userId)
            val sanitizedTarget = streamProxyService.sanitizeStream(
                stream = preparedStream,
                userId = userId
            )
            val sanitizedPlaylist = streamProxyService.sanitizePlaylist(
                streams = mappedStreams,
                userId = userId,
                defaultHeaders = preparedStream.headers
            )

            val fallbackDuration = preparedStream.durationSeconds
                ?: targetStream.durationSeconds
                ?: runCatching { mediaCatalog.getMediaDetails(key, language = "ru") }.getOrNull()?.runtime?.takeIf { it > 0 }?.let { (it * 60).toDouble() }

            return StreamPlaybackResult.Ready(
                streamUrl = sanitizedTarget.url,
                streamId = sanitizedTarget.canonicalId,
                durationSeconds = fallbackDuration,
                startPositionSeconds = targetPair.second?.progressSeconds,
                audioTracks = sanitizedTarget.audioTracks,
                subtitleTracks = sanitizedTarget.subtitleTracks,
                playlist = sanitizedPlaylist
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logger.error("Ошибка подготовки потока для key=${key.id}: ${e.message}", e)
            return StreamPlaybackResult.Error("Ошибка подготовки потока: ${e.message}")
        }
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
