package org.ensodai.avalonmediacard.presentation.screens.integrations

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.presentation.screens.integrations.viewState.IntegrationsViewState
import org.jetbrains.compose.resources.stringResource

@Composable
fun IntegrationsContent(
    state: IntegrationsViewState,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 340.dp),
        modifier = modifier
            .fillMaxSize()
            .background(AvalonTheme.colors.background),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                AvalonText(
                    text = stringResource(Res.string.integrations_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = AvalonTheme.colors.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                AvalonText(
                    text = stringResource(Res.string.integrations_subtitle),
                    fontSize = 14.sp,
                    color = AvalonTheme.colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        items(state.settingsGroups, key = { it.nodeId }) { groupSlot ->
            IntegrationCard(
                state = groupSlot.state,
                onAction = onAction,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
