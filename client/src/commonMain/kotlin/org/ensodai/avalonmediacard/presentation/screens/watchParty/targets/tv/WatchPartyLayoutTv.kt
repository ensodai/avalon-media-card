package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.WatchPartyActions
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.tv.components.WatchPartyLobbyTv
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.tv.components.WatchPartyPinEntryTv
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.tv.components.WatchPartySetupTv
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyStep
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyViewState

@Composable
fun WatchPartyLayoutTv(
    state: WatchPartyViewState,
    actions: WatchPartyActions,
    modifier: Modifier = Modifier,
    sourcesContent: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Если открыт полноэкранный выбор источников для ТВ
        if (state.isSelectingSource && sourcesContent != null) {
            sourcesContent()
        } else {
            when (state.step) {
                WatchPartyStep.ENTRY -> {
                    WatchPartyPinEntryTv(
                        state = state,
                        actions = actions,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                WatchPartyStep.SETUP -> {
                    WatchPartySetupTv(
                        state = state,
                        actions = actions,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                WatchPartyStep.LOBBY -> {
                    WatchPartyLobbyTv(
                        state = state,
                        actions = actions,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
