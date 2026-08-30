package org.ensodai.avalonmediacard.plugins.torrserver.presentation

import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.slot.UploadCustomTorrentCommand
import org.ensodai.avalonmediacard.plugins.torrserver.domain.model.*
import org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.playback.*
import org.ensodai.avalonmediacard.plugins.torrserver.domain.usecase.settings.*

object TorrServerActionRegistry {
    fun register(
        context: PluginContext,
        testProwlarr: TestProwlarrConnectionUseCase,
        testJackett: TestJackettConnectionUseCase,
        testTorrServer: TestTorrServerConnectionUseCase,
        saveTorrServerSettings: SaveTorrServerSettingsUseCase,
        saveProwlarrSettings: SaveProwlarrSettingsUseCase,
        saveJackettSettings: SaveJackettSettingsUseCase,
        handleOpenTorrentInspector: HandleOpenTorrentInspectorCommandUseCase,
        handleRemapTorrentFile: HandleRemapTorrentFileCommandUseCase,
        handleUploadCustomTorrent: HandleUploadCustomTorrentCommandUseCase
    ) {
        // --- Settings Actions ---
        context.actions.bind<TestProwlarrConnectionCommand> { cmd, userId ->
            testProwlarr.execute(userId, cmd.url, cmd.apiKey)
        }
        
        context.actions.bind<TestJackettConnectionCommand> { cmd, userId ->
            testJackett.execute(userId, cmd.url, cmd.apiKey)
        }

        context.actions.bind<TestTorrServerConnectionCommand> { cmd, userId ->
            testTorrServer.execute(userId, cmd.host, cmd.login, cmd.password, cmd.useGst)
        }

        context.actions.bind<SaveTorrServerSettingsCommand> { cmd, userId ->
            saveTorrServerSettings.execute(userId, cmd)
        }

        context.actions.bind<SaveProwlarrSettingsCommand> { cmd, userId ->
            saveProwlarrSettings.execute(userId, cmd)
        }

        context.actions.bind<SaveJackettSettingsCommand> { cmd, userId ->
            saveJackettSettings.execute(userId, cmd)
        }

        // --- Torrent Inspector & Mapping Actions ---
        context.actions.bind<OpenTorrentInspectorCommand> { cmd, userId ->
            handleOpenTorrentInspector.execute(cmd, userId)
        }

        context.actions.bind<RemapTorrentFileCommand> { cmd, userId ->
            handleRemapTorrentFile.execute(cmd, userId)
        }

        context.actions.bind<UploadCustomTorrentCommand> { cmd, userId ->
            handleUploadCustomTorrent.execute(cmd, userId)
        }
    }
}
