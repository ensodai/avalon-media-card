package org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.viewState

import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SduiViewState
import org.ensodai.avalonmediacard.presentation.core.SlotUiState

sealed interface FeedItem {
    val nodeId: String

    data class HeroBanner(
        override val nodeId: String,
        val state: SlotUiState<SlotData.Hero> = SlotUiState()
    ) : FeedItem

    data class Carousel(
        override val nodeId: String,
        val state: SlotUiState<SlotData.Carousel> = SlotUiState()
    ) : FeedItem

    data class Backdrops(
        override val nodeId: String,
        val state: SlotUiState<SlotData.CarouselBackdrops> = SlotUiState()
    ) : FeedItem

    data class Exploration(
        override val nodeId: String,
        val state: SlotUiState<SlotData.Exploration> = SlotUiState()
    ) : FeedItem

    data class Banner(
        override val nodeId: String,
        val state: SlotUiState<SlotData.Banner> = SlotUiState()
    ) : FeedItem
}

data class DashboardViewState(
    override val loadingActions: Set<ServerAction> = emptySet(),
    val feedItems: List<FeedItem> = emptyList()
) : SduiViewState