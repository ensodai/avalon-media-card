package org.ensodai.avalonmediacard.plugins.lampac.presentation.playback

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.ScreenSlots
import org.ensodai.avalonmediacard.contract.slot.*
import kotlin.uuid.Uuid

/**
 * **Lampac Playback Screen Presenter**
 *
 * Emits reactive slot flows for rendering the Lampac provider entry in the host client UI.
 */
class LampacPlaybackScreenPresenter(
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
                            providerTitle = context.i18n.t("lampac.provider_title")
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
