package org.ensodai.avalonmediacard.presentation.screens.detailsScreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.time.Clock
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AdaptiveLayout
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.mobile.MediaDetailsLayoutMobile
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.tv.MediaDetailsLayoutTv
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.targets.web.MediaDetailsLayoutWeb
import org.ensodai.avalonmediacard.presentation.screens.detailsScreen.viewState.DetailsViewState

private val logger = AppLogging.logger("DetailsContent")

@Composable
fun DetailsContent(
    modifier: Modifier = Modifier,
    state: DetailsViewState,
    onAction: (Action) -> Unit,
    onClosePlayer: (() -> Unit)? = null,
    onRequestOtherSource: (() -> Unit)? = null,
    onCloseSources: (() -> Unit)? = null,
    onSelectSource: ((providerId: String, sourceId: String, seasonNumber: Int?, episodeNumber: Int?, onComplete: () -> Unit) -> Unit)? = null,
    onRefreshSources: (() -> Unit)? = null,
) {
    logger.d { "[PROFILING] DetailsContent RECOMPOSE (Header state: ${state.header?.state?.let { it::class.simpleName }}): ${Clock.System.now()}" }

    AdaptiveLayout(
        tv = {
            MediaDetailsLayoutTv(
                state = state,
                onAction = onAction,
                onClosePlayer = onClosePlayer,
                onRequestOtherSource = onRequestOtherSource,
                onCloseSources = onCloseSources,
                onSelectSource = onSelectSource,
                onRefreshSources = onRefreshSources,
                modifier = modifier
            )
        },
        mobile = {
            MediaDetailsLayoutMobile(
                state = state,
                onAction = onAction,
                onClosePlayer = onClosePlayer,
                onRequestOtherSource = onRequestOtherSource,
                onCloseSources = onCloseSources,
                onSelectSource = onSelectSource,
                onRefreshSources = onRefreshSources,
                modifier = modifier
            )
        },
        default = {
            MediaDetailsLayoutWeb(
                state = state,
                onAction = onAction,
                onClosePlayer = onClosePlayer,
                onRequestOtherSource = onRequestOtherSource,
                onCloseSources = onCloseSources,
                onSelectSource = onSelectSource,
                onRefreshSources = onRefreshSources,
                modifier = modifier
            )
        }
    )
}
