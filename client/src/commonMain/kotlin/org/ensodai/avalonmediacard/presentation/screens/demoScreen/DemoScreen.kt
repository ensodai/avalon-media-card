package org.ensodai.avalonmediacard.presentation.screens.demoScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.ensodai.avalonmediacard.contract.model.ClickstreamContext
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.MovieCarouselItem
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SlotUiState
import org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.DashboardContent
import org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.viewState.DashboardViewState
import org.ensodai.avalonmediacard.presentation.screens.dashboardScreen.viewState.FeedItem

@Composable
fun DemoScreen(
    title: String = "Home",
    onAction: (Action) -> Unit = {}
) {
    val state = remember { createDemoViewState() }

    DashboardContent(
        title = title,
        state = state,
        onAction = onAction
    )
}

private fun createDemoViewState(): DashboardViewState {
    // 1. Media Items Definitions
    val sintel = MovieCarouselItem(
        key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "45745"),
        title = "Sintel",
        posterUrl = "https://image.tmdb.org/t/p/w500/2hwMOwcyyYWlHBldhonipg09kRm.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/msqeiEyIRpPAtrCeRGFNZQ9tkJL.jpg",
        badges = listOf("★ 7.5", "4K", "Fantasy")
    )

    val spring = MovieCarouselItem(
        key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "593048"),
        title = "Spring",
        posterUrl = "https://image.tmdb.org/t/p/w500/g5dSJtpoXYCSVx70srSAc3wqNFc.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/lL04DVOu1b64Xoj1EuWHZdtcfaZ.jpg",
        badges = listOf("★ 7.9", "UHD", "Masterpiece")
    )

    val spriteFright = MovieCarouselItem(
        key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "891761"),
        title = "Sprite Fright",
        posterUrl = "https://image.tmdb.org/t/p/w500/AjWJQOogG4Irpff6K49tzrUTb1s.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/aCMT1okuiKFnyKHdCOez66rJKyT.jpg",
        badges = listOf("★ 7.6", "Comedy", "Horror")
    )

    val charge = MovieCarouselItem(
        key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "1062079"),
        title = "Charge",
        posterUrl = "https://image.tmdb.org/t/p/w500/nei66j5yYOsXzX71BUSnzv9kFlZ.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/hwvuivrtKFA4tPkW81txs7oKJOx.jpg",
        badges = listOf("★ 7.2", "Cyberpunk", "Action")
    )

    val tearsOfSteel = MovieCarouselItem(
        key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "133701"),
        title = "Tears of Steel",
        posterUrl = "https://image.tmdb.org/t/p/w500/8qy3jRmaHR7f8VZh3iXCqCWfFsH.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/fOy6SL5Zs2PFcNXwqEPIDPrLB1q.jpg",
        badges = listOf("★ 6.5", "Sci-Fi", "VFX")
    )

    val bigBuckBunny = MovieCarouselItem(
        key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "10378"),
        title = "Big Buck Bunny",
        posterUrl = "https://image.tmdb.org/t/p/w500/i9jJzvoXET4D9pOkoEwncSdNNER.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/xtdybjRRZ15mCrPOvEld305myys.jpg",
        badges = listOf("★ 7.0", "Animation", "Classic")
    )

    val cosmosLaundromat = MovieCarouselItem(
        key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "358332"),
        title = "Cosmos Laundromat",
        posterUrl = "https://image.tmdb.org/t/p/w500/5ZXi0oitpEgAdoJglFTc5SZF9nt.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/f2wABsgj2lIR2dkDEfBZX8p4Iyk.jpg",
        badges = listOf("★ 7.3", "Sci-Fi", "UHD")
    )

    val wingIt = MovieCarouselItem(
        key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "1177628"),
        title = "Wing It!",
        posterUrl = "https://image.tmdb.org/t/p/w500/kVRGAgqUC2u75pLsXgDj14tOsRJ.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/o2G1kW9pCcRo7NmiZhJBm5LcVpH.jpg",
        badges = listOf("★ 7.8", "Blender 2023", "Comedy")
    )

    val elephantsDream = MovieCarouselItem(
        key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "9761"),
        title = "Elephants Dream",
        posterUrl = "https://image.tmdb.org/t/p/w500/9zROtU9TkpZQrOuEaMAp68FOWLK.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/9bJDwuhza19HQcYA99FeslLYmUm.jpg",
        badges = listOf("★ 6.8", "Sci-Fi", "Original")
    )

    val agent327 = MovieCarouselItem(
        key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "457784"),
        title = "Agent 327: Operation Barbershop",
        posterUrl = "https://image.tmdb.org/t/p/w500/tSIMZK9XZSIb1dL3iQeTESEZvHw.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/aupI9kV5CawUp02XWnI2l7Ym9SH.jpg",
        badges = listOf("★ 7.7", "Action", "Spy")
    )

    val monkaa = MovieCarouselItem(
        key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "303438"),
        title = "Monkaa",
        posterUrl = "https://image.tmdb.org/t/p/w500/cPd2cekKSZMGHDomFrh7YKpdLv1.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/cPd2cekKSZMGHDomFrh7YKpdLv1.jpg",
        badges = listOf("★ 6.6", "Action", "Open")
    )

    val hero = MovieCarouselItem(
        key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "615324"),
        title = "HERO",
        posterUrl = "https://image.tmdb.org/t/p/w500/udWCrPAYCzZh6rp48dGr90ohJ2v.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/7poXZZ6JZA0AN9nhXvt8JbzlX7T.jpg",
        badges = listOf("★ 7.0", "2D/3D Anime")
    )

    val coffeeRun = MovieCarouselItem(
        key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "717986"),
        title = "Coffee Run",
        posterUrl = "https://image.tmdb.org/t/p/w500/tFaE5KZOzTTTaNAEDD8CY1ZBjei.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/pUHJzzq6fUowt8NrfDL5JAfjbqR.jpg",
        badges = listOf("★ 7.1", "Drama")
    )

    val caminandesLlamigos = MovieCarouselItem(
        key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "406956"),
        title = "Caminandes: Llamigos",
        posterUrl = "https://image.tmdb.org/t/p/w500/753kJbZ5iS7DUomTKX9qF5Cs5NY.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/tIX2BIhkhoFq1iwtHYzTSbHbe82.jpg",
        badges = listOf("★ 7.4", "Animation")
    )

    val glassHalf = MovieCarouselItem(
        key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "420577"),
        title = "Glass Half",
        posterUrl = "https://image.tmdb.org/t/p/w500/hrAEYBMb6vitzXMvBC7RKj3ZyWt.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/AlqiewYiIpnoR4yqeo17y1SBf6S.jpg",
        badges = listOf("★ 6.9", "Comedy")
    )

    val singularity = MovieCarouselItem(
        key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "1687677"),
        title = "Singularity",
        posterUrl = "https://image.tmdb.org/t/p/w500/tsTGGB6NGVzHMuNXEV328jU0Eyx.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/kO1leUeopPmJRTeRxThR0eo15GE.jpg",
        badges = listOf("★ 6.7", "Sci-Fi")
    )

    val caminandesDrama = MovieCarouselItem(
        key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "253777"),
        title = "Caminandes: Llama Drama",
        posterUrl = "https://image.tmdb.org/t/p/w500/66VPke0YSiyfe97aobbcZ55ts56.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/mjkoC8Vo7fSHuqrbVQdI6cNwKA2.jpg",
        badges = listOf("★ 7.2", "Slapstick")
    )

    val theDailyDweebs = MovieCarouselItem(
        key = MediaKey(MediaProvider.Tmdb, EntityType.MOVIE, "498482"),
        title = "The Daily Dweebs",
        posterUrl = "https://image.tmdb.org/t/p/w500/jTTm5zgJRPYSKD2aEp6F7bVI7b.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/sed3rYCcwlEW6llZWw8xLgfmFnx.jpg",
        badges = listOf("★ 7.1", "Retro 1950s")
    )

    val pioneerOne = MovieCarouselItem(
        key = MediaKey(MediaProvider.Tmdb, EntityType.TV, "33050"),
        title = "Pioneer One",
        posterUrl = "https://image.tmdb.org/t/p/w500/p53wbsukyhJ8TieisoeU1zZr9iA.jpg",
        backdropUrl = "https://image.tmdb.org/t/p/w1280/aMUc1h3nXYZnou7P9HRdJgW2zw2.jpg",
        badges = listOf("★ 7.3", "TV Series", "Sci-Fi")
    )

    // 2. Build Feed Items
    val heroBannerSlot = FeedItem.HeroBanner(
        nodeId = "demo_hero_banner",
        state = SlotUiState(
            isLoading = false,
            data = SlotData.Hero(
                id = "demo_hero",
                title = "Featured Cinema",
                subtitle = "Masterpieces of open-source filmmaking by Blender Studio & independent creators in 4K Ultra HD",
                items = listOf(sintel, spring, spriteFright, charge, tearsOfSteel),
                telemetryContext = ClickstreamContext.HOME_PAGE
            )
        )
    )

    val backdropsCarouselSlot = FeedItem.Backdrops(
        nodeId = "demo_backdrops_carousel",
        state = SlotUiState(
            isLoading = false,
            data = SlotData.CarouselBackdrops(
                id = "demo_backdrops",
                title = "Widescreen Originals (16:9)",
                items = listOf(bigBuckBunny, cosmosLaundromat, wingIt, elephantsDream, agent327, monkaa),
                telemetryContext = ClickstreamContext.CAROUSEL_DISCOVER
            )
        )
    )

    val postersCarouselSlot = FeedItem.Carousel(
        nodeId = "demo_posters_carousel",
        state = SlotUiState(
            isLoading = false,
            data = SlotData.Carousel(
                id = "demo_posters",
                title = "Popular Animated Shorts",
                items = listOf(hero, coffeeRun, caminandesLlamigos, glassHalf, singularity, caminandesDrama),
                telemetryContext = ClickstreamContext.CAROUSEL_DISCOVER
            )
        )
    )

    val explorationSlot = FeedItem.Exploration(
        nodeId = "demo_exploration",
        state = SlotUiState(
            isLoading = false,
            data = SlotData.Exploration(
                id = "demo_explore",
                title = "Collections & Open Universes",
                items = listOf(theDailyDweebs, pioneerOne, sintel, spring, charge, wingIt),
                telemetryContext = ClickstreamContext.CAROUSEL_DISCOVER
            )
        )
    )

    val sciFiCarouselSlot = FeedItem.Carousel(
        nodeId = "demo_scifi_carousel",
        state = SlotUiState(
            isLoading = false,
            data = SlotData.Carousel(
                id = "demo_scifi",
                title = "Sci-Fi & Cyberpunk Adventures",
                items = listOf(tearsOfSteel, pioneerOne, elephantsDream, singularity, charge, cosmosLaundromat),
                telemetryContext = ClickstreamContext.CAROUSEL_DISCOVER
            )
        )
    )

    return DashboardViewState(
        feedItems = listOf(
            heroBannerSlot,
            backdropsCarouselSlot,
            postersCarouselSlot,
            explorationSlot,
            sciFiCarouselSlot
        )
    )
}
