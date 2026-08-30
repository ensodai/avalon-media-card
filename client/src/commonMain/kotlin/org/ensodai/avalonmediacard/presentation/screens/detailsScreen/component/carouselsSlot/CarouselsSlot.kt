package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.carouselsSlot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.MovieCarousel
import org.ensodai.avalonmediacard.presentation.core.SduiSlot

@Composable
fun CarouselsSlot(
    state: List<SduiSlot<SlotData.Carousel>>,
    onAction: (Action) -> Unit
) {

    Column {
        state.forEach { carousel ->
            MovieCarousel(state = carousel.state, onAction = onAction)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
