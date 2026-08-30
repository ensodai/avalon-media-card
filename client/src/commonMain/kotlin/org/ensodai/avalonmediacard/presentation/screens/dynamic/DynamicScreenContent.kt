package org.ensodai.avalonmediacard.presentation.screens.dynamic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import org.ensodai.avalonmediacard.presentation.components.textSlot.TextSlot
import org.ensodai.avalonmediacard.presentation.screens.dynamic.viewState.DynamicViewState

@Composable
fun DynamicContent(
    title: String,
    state: DynamicViewState,
    onAction: (Action) -> Unit
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

            if (state.grid != null) {
                MediaGridSlot(
                    state = state.grid.state,
                    onAction = onAction,
                    modifier = Modifier.fillMaxSize()
                )
            }

            state.description.forEach { update ->
                TextSlot(
                    state = update.state,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}
