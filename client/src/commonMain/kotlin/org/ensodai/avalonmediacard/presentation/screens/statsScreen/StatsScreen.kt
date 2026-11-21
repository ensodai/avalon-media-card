package org.ensodai.avalonmediacard.presentation.screens.statsScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.presentation.navigation.LocalNavigation
import org.ensodai.avalonmediacard.presentation.screens.statsScreen.components.StatsBentoGrid
import org.ensodai.avalonmediacard.presentation.screens.statsScreen.components.StatsErrorState
import org.ensodai.avalonmediacard.presentation.screens.statsScreen.components.StatsHeader
import org.ensodai.avalonmediacard.presentation.screens.statsScreen.components.StatsLoadingState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = koinViewModel()
) {
    val state by viewModel.viewState.collectAsState()
    val actions = viewModel.actions
    val navigation = LocalNavigation.current

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 1240.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 1. Верхняя панель: заголовок и кнопка обновления
            StatsHeader(
                isLoading = state.isLoading,
                onRefresh = actions.onRefresh
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Состояния: загрузка, ошибка или Bento Grid
            when {
                state.isLoading && state.stats == null -> {
                    StatsLoadingState()
                }

                state.errorMessage != null && state.stats == null -> {
                    StatsErrorState(
                        errorMessage = state.errorMessage ?: stringResource(Res.string.stats_loading_error),
                        onRetry = actions.onRefresh
                    )
                }

                state.stats != null -> {
                    StatsBentoGrid(
                        stats = state.stats!!,
                        onShareArchetype = actions.onShareArchetype,
                        onMoviesClicked = { navigation.navigateTo(Screen.Movies) },
                        onShowsClicked = { navigation.navigateTo(Screen.TvShows) },
                        onPersonClicked = { personId, name ->
                            actions.onPersonClicked(personId, name)
                            navigation.navigateToPerson(
                                key = MediaKey(
                                    provider = MediaProvider.Tmdb,
                                    type = EntityType.PERSON,
                                    id = personId
                                ),
                                personName = name
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
