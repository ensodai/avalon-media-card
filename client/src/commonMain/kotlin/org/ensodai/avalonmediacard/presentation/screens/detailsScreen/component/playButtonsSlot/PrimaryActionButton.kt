package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.playButtonsSlot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ensodai.avalonmediacard.contract.logging.AppLogging
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect

private val logger = AppLogging.logger("PrimaryActionButton")

@Composable
fun PrimaryActionButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(46.dp)
            .shimmerPlaceholder(isLoading, RoundedCornerShape(8.dp))
            .onFocusChanged {
                if (it.isFocused) {
                    logger.d { "[FOCUS_DEBUG] PrimaryActionButton text='$text' GOT FOCUS!" }
                }
            }
            .tvAndWebHoverEffect(
                scaleTarget = 1.04f,
                shape = RoundedCornerShape(8.dp),
                clickEnabled = !isLoading,
                onClick = { onClick() }
            )
            .background(if (isLoading) Color.Transparent else Color.White, RoundedCornerShape(8.dp))
            .padding(horizontal = 24.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (isLoading) Color.Transparent else Color.Black
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isLoading) Color.Transparent else Color.Black
        )
    }
}
