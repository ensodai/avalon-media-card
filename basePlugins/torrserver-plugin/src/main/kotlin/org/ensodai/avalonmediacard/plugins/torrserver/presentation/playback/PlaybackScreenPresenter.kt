package org.ensodai.avalonmediacard.plugins.torrserver.presentation.playback

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.ScreenSlots
import org.ensodai.avalonmediacard.contract.slot.*
import kotlin.uuid.Uuid

class PlaybackScreenPresenter(
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
                            providerTitle = context.i18n.t("playback.torrents")
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
