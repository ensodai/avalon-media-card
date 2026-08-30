package org.ensodai.avalonmediacard.presentation.screens.detailsScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.ClickstreamContext
import org.ensodai.avalonmediacard.contract.model.ClickstreamTargetType
import org.ensodai.avalonmediacard.presentation.core.SduiCoordinator
import org.ensodai.avalonmediacard.presentation.telemetry.TrackPageView
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DetailsScreen(
    mediaKey: MediaKey,
    viewModel: DetailsViewModel = koinViewModel(key = mediaKey.id) { parametersOf(mediaKey) }
) {
    val state by viewModel.viewState.collectAsState()

    val targetType = when (mediaKey.type) {
        EntityType.PERSON -> ClickstreamTargetType.PERSON
        EntityType.TV -> ClickstreamTargetType.MEDIA_TV
        else -> ClickstreamTargetType.MEDIA_MOVIE
    }
    TrackPageView(
        screenId = mediaKey.id,
        context = ClickstreamContext.DETAILS_PAGE,
        targetType = targetType,
        targetId = mediaKey.id
    )

    SduiCoordinator(viewModel) { dispatch ->
        DetailsContent(
            state = state,
            onAction = dispatch,
            onClosePlayer = { viewModel.closePlayer() },
            onRequestOtherSource = { viewModel.openSourcesSheet() },
            onCloseSources = { viewModel.toggleSources(false) },
            onSelectSource = { providerId, sourceId, season, episode, onComplete ->
                viewModel.selectSource(providerId, sourceId, season, episode, onComplete)
            },
            onRefreshSources = { viewModel.openSourcesSheet(forceRefresh = true) }
        )

    }
}

