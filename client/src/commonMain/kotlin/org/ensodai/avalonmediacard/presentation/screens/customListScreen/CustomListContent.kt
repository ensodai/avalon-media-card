package org.ensodai.avalonmediacard.presentation.screens.customListScreen

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
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.presentation.components.mediaGridSlot.MediaGridSlot
import org.ensodai.avalonmediacard.presentation.screens.customListScreen.viewState.CustomListViewState

@Composable
fun CustomListContent(
    title: String,
    state: CustomListViewState,
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
                text = title,
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
