package org.ensodai.avalonmediacard.plugins.rutube.presentation.playback

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.ScreenSlots
import org.ensodai.avalonmediacard.contract.slot.LayoutNode
import org.ensodai.avalonmediacard.contract.slot.ScreenStreamEvent
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.contract.slot.SlotId
import org.ensodai.avalonmediacard.contract.slot.SlotState
import org.ensodai.avalonmediacard.contract.slot.SlotUpdate
import kotlin.uuid.Uuid

/**
 * **Rutube Playback Screen Presenter**
 *
 * Emits reactive slot flows for rendering the Rutube provider entry in the host client UI.
 *
 * @property pluginId Unique identifier of this plugin (`rutube-plugin`).
 * @property context Host plugin context.
 */
class RutubePlaybackScreenPresenter(
    private val pluginId: String,
    private val context: PluginContext
) {
    /**
     * Builds the [ScreenSlots] definition and update flow for the media details screen.
     *
     * @param key Canonical [MediaKey] of the movie or series.
     * @param userId Optional active user identifier.
     * @return [ScreenSlots] containing the layout node and update stream.
     */
    fun getPlaybackSlots(key: MediaKey, userId: Uuid?): ScreenSlots {
        val mediaSourcesFlow = flow {
            emit(
                SlotUpdate(
                    slotId = SlotId.MediaSources,
                    nodeId = pluginId,
                    state = SlotState.Content(
                        SlotData.MediaSources(
                            sources = emptyList(),
                            mediaKey = key,
                            providerId = pluginId,
                            providerTitle = "Rutube"
                        )
                    )
                )
            )
        }

        return ScreenSlots(
            layout = listOf(
                LayoutNode(pluginId, SlotId.MediaSources)
            ),
            flow = mediaSourcesFlow.map { ScreenStreamEvent.Update(it) }
        )
    }
}
