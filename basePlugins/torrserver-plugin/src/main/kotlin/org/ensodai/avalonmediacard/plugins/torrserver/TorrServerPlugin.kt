package org.ensodai.avalonmediacard.plugins.torrserver

import kotlinx.coroutines.flow.flow
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.plugins.AvalonPlugin
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.PluginLogger
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.contract.slot.UploadCustomTorrentCommand
import org.ensodai.avalonmediacard.plugins.torrserver.data.network.TorrServerApiClient
import org.ensodai.avalonmediacard.plugins.torrserver.data.repository.SearchRepositoryImpl
import org.ensodai.avalonmediacard.plugins.torrserver.data.repository.SettingsRepositoryImpl
import org.ensodai.avalonmediacard.plugins.torrserver.data.repository.TorrServerRepositoryImpl
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.*
import org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.playback.HandleOpenTorrentInspectorCommandUseCase
import org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.playback.HandleRemapTorrentFileCommandUseCase
import org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.playback.HandleUploadCustomTorrentCommandUseCase
import org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.playback.ResolveInspectorStreamUseCase
import org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.playback.ResolveRemappedStreamUseCase
import org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.search.SearchTorrentsUseCase
import org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.settings.*
import org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.torrent.GetMappedStreamsUseCase
import org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.torrent.InspectAndMapTorrentUseCase
import org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.torrent.PrepareStreamUseCase
import org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.torrent.RemapTorrentUseCase
import org.ensodai.avalonmediacard.plugins.torrserver.presentation.TorrServerActionRegistry
import org.ensodai.avalonmediacard.plugins.torrserver.presentation.TorrServerScreenRegistry
import org.ensodai.avalonmediacard.plugins.torrserver.presentation.ValidationStateStoreImpl
import org.ensodai.avalonmediacard.plugins.torrserver.presentation.playback.PlaybackScreenPresenter
import org.ensodai.avalonmediacard.plugins.torrserver.presentation.settings.IntegrationsScreenPresenter

class TorrServerPlugin : AvalonPlugin {

    override val id: String = "torrserver-plugin"
    override val name: String = "Торренты"
    override val version: String = "1.1.0"
    override val author: String = "Avalon Media Card"

    private lateinit var logger: PluginLogger

    override fun provideSerializers(): SerializersModule = SerializersModule {
        polymorphic(Action::class) {
            subclass(OpenTorrentInspectorCommand::class)
            subclass(RemapTorrentFileCommand::class)
            subclass(ResetMappingCommand::class)
            subclass(SelectEpisodeCommand::class)
            subclass(TestProwlarrConnectionCommand::class)
            subclass(TestJackettConnectionCommand::class)
            subclass(TestTorrServerConnectionCommand::class)
            subclass(SaveTorrServerSettingsCommand::class)
            subclass(SaveProwlarrSettingsCommand::class)
            subclass(SaveJackettSettingsCommand::class)
        }
        polymorphic(ServerAction::class) {
            subclass(OpenTorrentInspectorCommand::class)
            subclass(RemapTorrentFileCommand::class)
            subclass(ResetMappingCommand::class)
            subclass(SelectEpisodeCommand::class)
            subclass(TestProwlarrConnectionCommand::class)
            subclass(TestJackettConnectionCommand::class)
            subclass(TestTorrServerConnectionCommand::class)
            subclass(SaveTorrServerSettingsCommand::class)
            subclass(SaveProwlarrSettingsCommand::class)
            subclass(SaveJackettSettingsCommand::class)
            subclass(UploadCustomTorrentCommand::class)
        }
    }

    override fun onInitialize(context: PluginContext) {
        logger = context.logger

        // 1. Core Data & Domain
        val torrServerApi = TorrServerApiClient(context, context.httpClient, logger)
        val torrServerRepository = TorrServerRepositoryImpl(torrServerApi)
        val searchRepository = SearchRepositoryImpl(context)
        val settingsRepository = SettingsRepositoryImpl(context)
        val validationStore = ValidationStateStoreImpl()

        val searchUseCase = SearchTorrentsUseCase(context, searchRepository)
        val mappedStreamsUseCase = GetMappedStreamsUseCase(context, torrServerRepository)
        val inspectUseCase = InspectAndMapTorrentUseCase(context, torrServerRepository)
        val remapUseCase = RemapTorrentUseCase(context)
        val prepareStreamUseCase = PrepareStreamUseCase(context, torrServerRepository)

        // 2. Streams Registration
        context.streams.onMedia { key, season, episode, userId ->
            flow {
                val streams = searchUseCase.execute(key, season, episode, userId)
                streams.forEach { emit(it) }
            }
        }
        context.streams.onPrepare { stream, userId ->
            prepareStreamUseCase.execute(stream, userId)
        }
        context.streams.onPlaylist { key, sourceId, userId ->
            var hash = sourceId
            if (sourceId.startsWith("magnet:") || sourceId.startsWith("http")) {
                val metadata = try { context.catalog.getMediaDetails(key) } catch (e: Exception) { null }
                val title = metadata?.title ?: ""
                inspectUseCase.execute(sourceId, title, key.id, key.type, userId)
                val binding = if (userId != null) context.userMediaBindings.getActiveBinding(userId, key.id) else null
                hash = binding?.sourceId ?: sourceId
            }
            mappedStreamsUseCase.execute(key, boundHash = hash, userId = userId)
        }


        // 3. Presentation Layer: Presenters
        val playbackPresenter = PlaybackScreenPresenter(id, context)
        val settingsPresenter = IntegrationsScreenPresenter(id, context)

        // 4. UI Declaration
        TorrServerScreenRegistry.register(
            context = context,
            pluginId = id,
            playbackPresenter = playbackPresenter,
            settingsPresenter = settingsPresenter
        )

        // 5. Actions Layer: UseCases
        val resolveInspectorStreamUseCase = ResolveInspectorStreamUseCase(context, mappedStreamsUseCase)
        val resolveRemappedStreamUseCase = ResolveRemappedStreamUseCase(context, mappedStreamsUseCase)

        // 6. Action Bindings Registration
        TorrServerActionRegistry.register(
            context = context,
            testProwlarr = TestProwlarrConnectionUseCase(context, context.httpClient, validationStore),
            testJackett = TestJackettConnectionUseCase(context, context.httpClient, validationStore),
            testTorrServer = TestTorrServerConnectionUseCase(context, torrServerRepository, validationStore),
            saveTorrServerSettings = SaveTorrServerSettingsUseCase(context, settingsRepository),
            saveProwlarrSettings = SaveProwlarrSettingsUseCase(context, settingsRepository),
            saveJackettSettings = SaveJackettSettingsUseCase(context, settingsRepository),
            handleOpenTorrentInspector = HandleOpenTorrentInspectorCommandUseCase(inspectUseCase, resolveInspectorStreamUseCase),
            handleRemapTorrentFile = HandleRemapTorrentFileCommandUseCase(remapUseCase, resolveRemappedStreamUseCase),
            handleUploadCustomTorrent = HandleUploadCustomTorrentCommandUseCase(inspectUseCase, resolveInspectorStreamUseCase)
        )

        logger.info("Плагин TorrServer & Jackett успешно инициализирован! \uD83C\uDF45")
    }
}
