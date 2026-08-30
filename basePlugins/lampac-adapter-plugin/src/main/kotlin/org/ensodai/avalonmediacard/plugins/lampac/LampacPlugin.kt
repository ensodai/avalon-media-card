package org.ensodai.avalonmediacard.plugins.lampac

import kotlinx.coroutines.flow.flow
import org.ensodai.avalonmediacard.contract.plugins.AvalonPlugin
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.PluginLogger
import org.ensodai.avalonmediacard.plugins.lampac.data.network.LampacApiClient
import org.ensodai.avalonmediacard.plugins.lampac.data.repository.LampacRepositoryImpl
import org.ensodai.avalonmediacard.plugins.lampac.domain.usecase.GetLampacPlaylistUseCase
import org.ensodai.avalonmediacard.plugins.lampac.domain.usecase.SearchLampacStreamsUseCase
import org.ensodai.avalonmediacard.plugins.lampac.presentation.LampacScreenRegistry
import org.ensodai.avalonmediacard.plugins.lampac.presentation.playback.LampacPlaybackScreenPresenter

/**
 * **Lampac Gateway Adapter Plugin**
 *
 * Provides access to 70+ online balancers, JacRed torrent search, and HLS streaming proxy
 * via local or remote Lampac NextGen instance for **Avalon Media Card**.
 *
 * @see AvalonPlugin
 * @see PluginContext
 */
class LampacPlugin : AvalonPlugin {

    override val id: String = "lampac-adapter-plugin"
    override val name: String = "Адаптер Lampac"
    override val version: String = "1.0.0"
    override val author: String = "Avalon Media Card"

    private lateinit var logger: PluginLogger

    override fun onInitialize(context: PluginContext) {
        logger = context.logger
        logger.info("Initializing Lampac Gateway Adapter Plugin [v$version]")

        // 1. Data Layer: Network Client & Repository
        val apiClient = LampacApiClient(context.httpClient, logger)
        val repository = LampacRepositoryImpl(apiClient)

        // 2. Domain Layer: Use Cases
        val searchUseCase = SearchLampacStreamsUseCase(context, repository)
        val playlistUseCase = GetLampacPlaylistUseCase(context, repository)

        // 3. Streams Registration: Search, Prepare & Playlist Resolution
        context.streams.onMedia { key, season, episode, userId ->
            flow {
                val streams = searchUseCase.execute(key, season, episode, userId)
                streams.forEach { emit(it) }
            }
        }

        context.streams.onPrepare { stream, _ ->
            // Lampac streams are pre-proxied and ready for immediate playback
            stream
        }

        context.streams.onPlaylist { key, sourceId, userId ->
            playlistUseCase.execute(key, sourceId, userId)
        }

        // 4. Presentation Layer: Presenter & Screen Slot Declaration
        val playbackPresenter = LampacPlaybackScreenPresenter(id, context)
        LampacScreenRegistry.register(
            context = context,
            pluginId = id,
            playbackPresenter = playbackPresenter
        )

        logger.info("Lampac Gateway Adapter Plugin successfully registered and ready!")
    }
}
