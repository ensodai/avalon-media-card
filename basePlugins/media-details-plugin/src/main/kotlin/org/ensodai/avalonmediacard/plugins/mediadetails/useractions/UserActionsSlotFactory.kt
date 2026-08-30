package org.ensodai.avalonmediacard.plugins.mediadetails.useractions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.ensodai.avalonmediacard.contract.i18n.PluginI18n
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.slot.*
import kotlin.uuid.Uuid

class UserActionsSlotFactory(
    private val pluginId: String,
    private val stateManager: UserActionsStateManager,
    private val i18n: PluginI18n
) {
    fun buildCollectionButtonsFlow(key: MediaKey, userId: Uuid): Flow<SlotUpdate> {
        return stateManager.getStateFlow(userId, key)
            .map { state ->
                val inCollection = state.item?.inCollection == true

                // The icons for Collection in SDUI
                val collectionIcon = if (inCollection) IconType.HEART_FILLED else IconType.HEART
                val collectionLabel = if (inCollection) i18n.t("details.in_collection") else i18n.t("details.add_to_collection")

                val customLists = state.customLists.map { list ->
                    CustomListItem(
                        id = list.id,
                        name = list.name,
                        isAdded = list.isAdded,
                        toggleAction = ToggleCustomListCommand(key, list.id)
                    )
                }

                val buttons = listOf(
                    ButtonItem(
                        label = collectionLabel,
                        icon = collectionIcon,
                        action = ToggleCollectionCommand(key, !inCollection)
                    ),
                    ButtonItem(
                        label = i18n.t("details.add_to_list"),
                        icon = IconType.PLUS,
                        action = null, // No primary action, it's a dropdown
                        customLists = customLists,
                        createListActionTemplate = CreateCustomListCommand(key, "")
                    )
                )

                SlotUpdate(
                    slotId = SlotId.CollectionButtons,
                    nodeId = pluginId,
                    state = if (state.isLoading) {
                        org.ensodai.avalonmediacard.contract.slot.SlotState.Loading()
                    } else {
                        org.ensodai.avalonmediacard.contract.slot.SlotState.Content(SlotData.ButtonGroup(buttons))
                    }
                )
            }.onStart { stateManager.loadInitial(userId, key) }
    }

    fun buildUserActionsFlow(key: MediaKey, userId: Uuid): Flow<SlotUpdate> {
        return stateManager.getStateFlow(userId, key)
            .map { state ->
                val rating = state.item?.userRating ?: 0
                val status = state.item?.status ?: MediaStatus.NONE

                val statusText = when (status) {
                    MediaStatus.WATCHING -> i18n.t("status.watching")
                    MediaStatus.COMPLETED -> i18n.t("status.completed")
                    MediaStatus.DROPPED -> i18n.t("status.dropped")
                    MediaStatus.PLANNED -> i18n.t("status.planned")
                    else -> i18n.t("status.add")
                }

                val actionsData = SlotData.UserActions(
                    mediaKey = key,
                    currentStatus = status,
                    currentRating = rating,
                    maxRating = 10,
                    statusOptions = MediaStatus.entries.associate { it to SetStatusCommand(key, it) },
                    ratingOptions = (1..10).associate { it to SetRatingCommand(key, it) }
                )
                SlotUpdate(
                    slotId = SlotId.UserActions,
                    nodeId = pluginId,
                    state = if (state.isLoading) {
                        org.ensodai.avalonmediacard.contract.slot.SlotState.Loading()
                    } else {
                        org.ensodai.avalonmediacard.contract.slot.SlotState.Content(actionsData)
                    }
                )
            }
    }
}
