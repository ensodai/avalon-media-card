package org.ensodai.avalonmediacard.plugins.vk.presentation

import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.slot.LayoutNode
import org.ensodai.avalonmediacard.contract.slot.SlotId
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.plugins.vk.presentation.playback.VkPlaybackScreenPresenter

/**
 * Declares and registers UI slots for VK Video provider in movie and series screens.
 */
object VkScreenRegistry {

    fun register(
        context: PluginContext,
        pluginId: String,
        playbackPresenter: VkPlaybackScreenPresenter
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
