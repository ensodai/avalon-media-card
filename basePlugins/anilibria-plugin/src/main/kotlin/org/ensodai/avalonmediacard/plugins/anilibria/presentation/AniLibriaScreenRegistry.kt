package org.ensodai.avalonmediacard.plugins.anilibria.presentation

import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.slot.LayoutNode
import org.ensodai.avalonmediacard.contract.slot.SlotId
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.plugins.anilibria.presentation.playback.AniLibriaPlaybackScreenPresenter

/**
 * **AniLibria Screen Slot Registry**
 *
 * Declares and registers UI slots for [Screen.MovieDetails] and [Screen.TvShowDetails].
 * Informs the host client where and how to render the AniLibria source drawer items.
 */
object AniLibriaScreenRegistry {

    /**
     * Registers media source slots into the host system.
     *
     * @param context Host plugin context.
     * @param pluginId Unique identifier of the plugin.
     * @param playbackPresenter Presenter providing the reactive slot streams.
     */
    fun register(
        context: PluginContext,
        pluginId: String,
        playbackPresenter: AniLibriaPlaybackScreenPresenter
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
