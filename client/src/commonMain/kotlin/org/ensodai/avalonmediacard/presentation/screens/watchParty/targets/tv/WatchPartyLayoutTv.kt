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
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.common_close
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.WatchPartyActions
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.tv.components.WatchPartyLobbyTv
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.tv.components.WatchPartyPinEntryTv
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.WatchPartySetupStep
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyStep
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyViewState
import org.jetbrains.compose.resources.stringResource

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
                    // Режим создания комнаты на ТВ
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 32.dp, top = 28.dp)
                                .size(46.dp)
                                .tvAndWebHoverEffect(
                                    scaleTarget = 1.1f,
                                    shape = CircleShape,
                                    onClick = { actions.onSetStep(WatchPartyStep.ENTRY) }
                                )
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.ArrowLeft,
                                contentDescription = stringResource(Res.string.common_close),
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .widthIn(max = 680.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(28.dp)
                        ) {
                            WatchPartySetupStep(state = state, actions = actions)
                        }
                    }
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
