package org.ensodai.avalonmediacard.plugins.vk.presentation.playback

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
 * Emits reactive slot flows for rendering VK Video provider tab in the client UI.
 */
class VkPlaybackScreenPresenter(
    private val pluginId: String,
    private val context: PluginContext
) {
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
                            providerTitle = "VK Video"
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
