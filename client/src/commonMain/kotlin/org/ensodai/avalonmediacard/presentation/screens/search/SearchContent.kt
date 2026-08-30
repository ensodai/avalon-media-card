package org.ensodai.avalonmediacard.presentation.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Search
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.presentation.components.mediaGridSlot.MediaGridSlot
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonTextField
import org.ensodai.avalonmediacard.presentation.screens.search.viewState.SearchViewState
import org.jetbrains.compose.resources.stringResource

@Composable
fun SearchContent(
    initialQuery: String,
    state: SearchViewState,
    onAction: (Action) -> Unit,
    onSearchQueryChanged: (String) -> Unit
) {
    var query by remember { mutableStateOf(initialQuery) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Input Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvalonTextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearchQueryChanged(it)
                },
                placeholder = stringResource(Res.string.search_placeholder),
                leadingIcon = {
                    Icon(
                        Lucide.Search,
                        contentDescription = stringResource(Res.string.search_btn),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color.White.copy(alpha = 0.05f)))

        val gridUpdate = state.resultsGrid
        if (gridUpdate != null) {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                MediaGridSlot(
                    state = gridUpdate.state,
                    onAction = onAction,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
