package org.ensodai.avalonmediacard.plugins.torrserver.presentation

import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.slot.LayoutNode
import org.ensodai.avalonmediacard.contract.slot.SlotId
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.plugins.torrserver.presentation.playback.PlaybackScreenPresenter
import org.ensodai.avalonmediacard.plugins.torrserver.presentation.settings.IntegrationsScreenPresenter

object TorrServerScreenRegistry {
    fun register(
        context: PluginContext,
        pluginId: String,
        playbackPresenter: PlaybackScreenPresenter,
        settingsPresenter: IntegrationsScreenPresenter
    ) {
        val sourcesLayout = listOf(
            LayoutNode(pluginId, SlotId.MediaSources)
        )

        context.slots.declare<Screen.MovieDetails>(
            listOf(SlotId.MediaSources)
        ) { sourcesLayout }
        context.slots.onScreen<Screen.MovieDetails> { screen, userId ->
            playbackPresenter.getPlaybackSlots(screen.key, userId)
        }

        context.slots.declare<Screen.TvShowDetails>(
            listOf(SlotId.MediaSources)
        ) { sourcesLayout }
        context.slots.onScreen<Screen.TvShowDetails> { screen, userId ->
            playbackPresenter.getPlaybackSlots(screen.key, userId)
        }

        context.slots.declare<Screen.Integrations>(
            listOf(SlotId.Integrations)
        ) {
            settingsPresenter.layoutNodes
        }

        context.slots.onScreen<Screen.Integrations> { _, userId ->
            settingsPresenter.getIntegrationsSlots(userId)
        }
    }
}
