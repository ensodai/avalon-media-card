package org.ensodai.avalonmediacard.plugins.anilibria

import kotlinx.coroutines.flow.flow
import org.ensodai.avalonmediacard.contract.plugins.AvalonPlugin
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.PluginLogger
import org.ensodai.avalonmediacard.plugins.anilibria.data.network.AniLibriaApiClient
import org.ensodai.avalonmediacard.plugins.anilibria.data.repository.AniLibriaRepositoryImpl
import org.ensodai.avalonmediacard.plugins.anilibria.domain.usecase.GetAniLibriaPlaylistUseCase
import org.ensodai.avalonmediacard.plugins.anilibria.domain.usecase.SearchAniLibriaStreamsUseCase
import org.ensodai.avalonmediacard.plugins.anilibria.presentation.AniLibriaScreenRegistry
import org.ensodai.avalonmediacard.plugins.anilibria.presentation.playback.AniLibriaPlaybackScreenPresenter

/**
 * **AniLibria Online Plugin (Reference Implementation)**
 *
 * Serves as the gold standard template for building high-performance, clean-architecture
 * online video streaming providers in **Avalon Media Card**.
 *
 * ### Key Responsibilities:
 * 1. **Search Integration**: Scans AniLibria API for releases matching TMDB title and year.
 * 2. **HLS Streaming**: Delivers multi-quality direct HLS video streams (1080p, 720p, 480p).
 * 3. **Episode Mapping**: Accurately maps continuous absolute episode numbers (e.g. 1..500)
 *    to relative TMDB season and episode structures.
 * 4. **User Progress**: Attaches watch history and progress indicators to every stream item.
 * 5. **Slot Presentation**: Declares UI slot nodes for media sources drawer tabs.
 *
 * @see AvalonPlugin
 * @see PluginContext
 */
class AniLibriaPlugin : AvalonPlugin {

    override val id: String = "anilibria-plugin"
    override val name: String = "AniLibria"
    override val version: String = "1.0.0"
    override val author: String = "Avalon Media Card"

    private lateinit var logger: PluginLogger

    /**
     * Initializes the plugin, wires up data sources, repositories, use cases,
     * stream handlers, and registers UI slots into the host system.
     *
     * @param context The host system context containing HTTP client, logger, catalog, and registries.
     */
    override fun onInitialize(context: PluginContext) {
        logger = context.logger
        logger.info("Initializing AniLibria Online Plugin [v$version]")

        // 1. Data Layer: Network Client & Repository Setup
        val apiClient = AniLibriaApiClient(context.httpClient, logger)
        val repository = AniLibriaRepositoryImpl(apiClient)

        // 2. Domain Layer: Use Cases
        val searchUseCase = SearchAniLibriaStreamsUseCase(context, repository)
        val playlistUseCase = GetAniLibriaPlaylistUseCase(context, repository)

        // 3. Streams Registration: Search, Prepare & Playlist Resolution
        context.streams.onMedia { key, season, episode, userId ->
            flow {
                val streams = searchUseCase.execute(key, season, episode, userId)
                streams.forEach { emit(it) }
            }
        }

        context.streams.onPrepare { stream, _ ->
            // Direct AniLibria HLS streams do not require pre-buffering (ready out of the box)
            stream
        }

        context.streams.onPlaylist { key, sourceId, userId ->
            playlistUseCase.execute(key, sourceId, userId)
        }

        // 4. Presentation Layer: Presenter & Screen Slot Declaration
        val playbackPresenter = AniLibriaPlaybackScreenPresenter(id, context)
        AniLibriaScreenRegistry.register(
            context = context,
            pluginId = id,
            playbackPresenter = playbackPresenter
        )

        logger.info("AniLibria Online Plugin successfully registered and ready!")
    }
}
