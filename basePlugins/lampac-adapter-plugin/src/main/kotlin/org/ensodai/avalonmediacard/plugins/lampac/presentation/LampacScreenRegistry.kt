package org.ensodai.avalonmediacard.plugins.lampac.presentation

import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.slot.LayoutNode
import org.ensodai.avalonmediacard.contract.slot.SlotId
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.plugins.lampac.presentation.playback.LampacPlaybackScreenPresenter

/**
 * **Lampac Screen Slot Registry**
 *
 * Declares and registers UI slots for [Screen.MovieDetails] and [Screen.TvShowDetails].
 */
object LampacScreenRegistry {

    fun register(
        context: PluginContext,
        pluginId: String,
        playbackPresenter: LampacPlaybackScreenPresenter
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
    }
}
