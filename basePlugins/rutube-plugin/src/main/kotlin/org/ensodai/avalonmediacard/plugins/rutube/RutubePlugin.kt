package org.ensodai.avalonmediacard.plugins.rutube

import kotlinx.coroutines.flow.flow
import org.ensodai.avalonmediacard.contract.plugins.AvalonPlugin
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.PluginLogger
import org.ensodai.avalonmediacard.plugins.rutube.data.network.RutubeApiClient
import org.ensodai.avalonmediacard.plugins.rutube.data.repository.RutubeRepositoryImpl
import org.ensodai.avalonmediacard.plugins.rutube.domain.usecase.GetRutubePlaylistUseCase
import org.ensodai.avalonmediacard.plugins.rutube.domain.usecase.SearchRutubeStreamsUseCase
import org.ensodai.avalonmediacard.plugins.rutube.presentation.RutubeScreenRegistry
import org.ensodai.avalonmediacard.plugins.rutube.presentation.playback.RutubePlaybackScreenPresenter

/**
 * **Rutube Online Plugin**
 *
 * Provides high-speed direct HLS video streams (1080p Full HD) from Rutube CDN
 * for movies and TV shows in **Avalon Media Card**.
 *
 * @see AvalonPlugin
 * @see PluginContext
 */
class RutubePlugin : AvalonPlugin {

    override val id: String = "rutube-plugin"
    override val name: String = "Rutube"
    override val version: String = "1.0.0"
    override val author: String = "Avalon Media Card"

    private lateinit var logger: PluginLogger

    override fun onInitialize(context: PluginContext) {
        logger = context.logger
        logger.info("Initializing Rutube Online Plugin [v$version]")

        // 1. Data Layer: Network Client & Repository
        val apiClient = RutubeApiClient(context.httpClient, logger)
        val repository = RutubeRepositoryImpl(apiClient)

        // 2. Domain Layer: Use Cases
        val searchUseCase = SearchRutubeStreamsUseCase(context, repository)
        val playlistUseCase = GetRutubePlaylistUseCase(context, repository)

        // 3. Streams Registration: Search, Prepare & Playlist Resolution
        context.streams.onMedia { key, season, episode, userId ->
            flow {
                val streams = searchUseCase.execute(key, season, episode, userId)
                streams.forEach { emit(it) }
            }
        }

        context.streams.onPrepare { stream, _ ->
            if (stream.url.contains("/play/embed/") || stream.url.isBlank()) {
                val videoId = stream.id.removePrefix("rutube_ep_").removePrefix("rutube_")
                val streamInfo = repository.getStreamInfo(videoId)
                if (streamInfo != null) {
                    stream.copy(
                        url = streamInfo.masterHlsUrl,
                        qualityVariants = streamInfo.qualities,
                        quality = streamInfo.qualities.firstOrNull()?.label ?: stream.quality
                    )
                } else {
                    stream
                }
            } else {
                stream
            }
        }

        context.streams.onPlaylist { key, sourceId, userId ->
            playlistUseCase.execute(key, sourceId, userId)
        }

        // 4. Presentation Layer: Presenter & Screen Slot Declaration
        val playbackPresenter = RutubePlaybackScreenPresenter(id, context)
        RutubeScreenRegistry.register(
            context = context,
            pluginId = id,
            playbackPresenter = playbackPresenter
        )

        logger.info("Rutube Online Plugin successfully registered and ready!")
    }
}
