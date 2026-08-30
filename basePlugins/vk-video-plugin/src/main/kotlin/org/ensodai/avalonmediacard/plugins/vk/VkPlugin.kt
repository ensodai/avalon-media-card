package org.ensodai.avalonmediacard.plugins.vk

import kotlinx.coroutines.flow.flow
import org.ensodai.avalonmediacard.contract.plugins.AvalonPlugin
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.PluginLogger
import org.ensodai.avalonmediacard.plugins.vk.data.network.VkApiClient
import org.ensodai.avalonmediacard.plugins.vk.data.repository.VkRepositoryImpl
import org.ensodai.avalonmediacard.plugins.vk.domain.usecase.GetVkPlaylistUseCase
import org.ensodai.avalonmediacard.plugins.vk.domain.usecase.SearchVkStreamsUseCase
import org.ensodai.avalonmediacard.plugins.vk.presentation.VkScreenRegistry
import org.ensodai.avalonmediacard.plugins.vk.presentation.playback.VkPlaybackScreenPresenter

/**
 * **VK Video Online Plugin**
 *
 * Provides high-speed direct MP4/HLS video streams up to 4K UHD (2160p) from VK Video
 * with automatic anonymous authentication, multi-quality options, subtitles,
 * and universal SDUI sub-filter season grouping for movies and series in **Avalon Media Card**.
 *
 * @see AvalonPlugin
 * @see PluginContext
 */
class VkPlugin : AvalonPlugin {

    override val id: String = "vk-video-plugin"
    override val name: String = "VK Video"
    override val version: String = "1.0.0"
    override val author: String = "Avalon Media Card"

    private lateinit var logger: PluginLogger

    override fun onInitialize(context: PluginContext) {
        logger = context.logger
        logger.info("Initializing VK Video Online Plugin [v$version] (up to 4K UHD)")

        // 1. Data Layer: Network Client & Repository
        val apiClient = VkApiClient(context.httpClient, logger)
        val repository = VkRepositoryImpl(apiClient)

        // 2. Domain Layer: Use Cases
        val searchUseCase = SearchVkStreamsUseCase(context, repository)
        val playlistUseCase = GetVkPlaylistUseCase(context, repository)

        // 3. Streams Registration: Search, Prepare & Playlist Resolution
        context.streams.onMedia { key, season, episode, userId ->
            flow {
                val streams = searchUseCase.execute(key, season, episode, userId)
                streams.forEach { emit(it) }
            }
        }

        context.streams.onPrepare { stream, _ ->
            // Direct VK Video MP4 / HLS streams are ready out of the box
            stream
        }

        context.streams.onPlaylist { key, sourceId, userId ->
            playlistUseCase.execute(key, sourceId, userId)
        }

        // 4. Presentation Layer: Presenter & Screen Slot Declaration
        val playbackPresenter = VkPlaybackScreenPresenter(id, context)
        VkScreenRegistry.register(
            context = context,
            pluginId = id,
            playbackPresenter = playbackPresenter
        )

        logger.info("VK Video Online Plugin successfully registered and ready!")
    }
}
