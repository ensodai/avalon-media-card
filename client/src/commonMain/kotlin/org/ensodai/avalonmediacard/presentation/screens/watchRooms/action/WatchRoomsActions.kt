package org.ensodai.avalonmediacard.presentation.screens.watchRooms.action

import org.ensodai.avalonmediacard.presentation.core.mvi.BaseActions
import kotlin.uuid.Uuid

data class WatchRoomsActions(
    val onRefresh: () -> Unit = {},
    val onOpenCreateModal: () -> Unit = {},
    val onOpenConnectModal: () -> Unit = {},
    val onOpenRoom: (Uuid) -> Unit = {},
    val onCloseModal: () -> Unit = {}
) : BaseActions()
