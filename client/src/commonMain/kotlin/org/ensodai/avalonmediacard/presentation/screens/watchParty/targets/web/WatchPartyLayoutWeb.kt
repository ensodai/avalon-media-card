package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.WatchPartyActions
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.WatchPartyEntryStep
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.WatchPartyHeader
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.WatchPartyLobbyStep
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.WatchPartySetupStep
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyStep
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyViewState

@Composable
fun WatchPartyLayoutWeb(
    state: WatchPartyViewState,
    actions: WatchPartyActions,
    modifier: Modifier = Modifier,
    sourcesContent: (@Composable () -> Unit)? = null
) {
    val isExpandedMode = state.step == WatchPartyStep.SETUP && state.isSelectingSource && sourcesContent != null
    val targetMaxWidth by animateDpAsState(
        targetValue = if (isExpandedMode) 1240.dp else 760.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

    Box(
        modifier = modifier
            .widthIn(max = targetMaxWidth)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF141418).copy(alpha = 0.96f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            .padding(28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            WatchPartyHeader(
                mediaTitle = state.mediaTitle,
                step = state.step,
                onBack = { actions.onSetStep(WatchPartyStep.ENTRY) },
                onClose = actions.onClose
            )

            when (state.step) {
                WatchPartyStep.ENTRY -> WatchPartyEntryStep(state = state, actions = actions)
                WatchPartyStep.SETUP -> {
                    if (isExpandedMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                WatchPartySetupStep(state = state, actions = actions)
                            }
                            Box(modifier = Modifier.weight(1.25f)) {
                                sourcesContent()
                            }
                        }
                    } else {
                        WatchPartySetupStep(state = state, actions = actions)
                    }
                }
                WatchPartyStep.LOBBY -> WatchPartyLobbyStep(state = state, actions = actions)
            }
        }
    }
}
