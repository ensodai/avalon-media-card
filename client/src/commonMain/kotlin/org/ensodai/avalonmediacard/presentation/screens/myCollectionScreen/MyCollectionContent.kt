package org.ensodai.avalonmediacard.presentation.screens.myCollectionScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.presentation.components.mediaGridSlot.MediaGridSlot
import org.ensodai.avalonmediacard.presentation.screens.myCollectionScreen.viewState.MyCollectionViewState
import org.jetbrains.compose.resources.stringResource

@Composable
fun MyCollectionContent(
    state: MyCollectionViewState,
    onAction: (Action) -> Unit,
    expectedItemsCount: Int? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
        ) {
            Text(
                text = stringResource(Res.string.my_collection_favorites),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                state.grids.forEach { slot ->
                    MediaGridSlot(
                        state = slot.state,
                        onAction = onAction,
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp),
                        expectedItemsCount = expectedItemsCount
                    )
                }
            }
        }
    }
}
