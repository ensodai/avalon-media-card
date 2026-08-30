package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.mediaDescriptionSlot

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect

@Composable
fun MediaDescriptionSlotContent(
    component: SlotData.Text,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var hasVisualOverflow by remember { mutableStateOf(false) }
    var isActive by remember { mutableStateOf(false) }

    val cleanedText = remember(component.content) {
        component.content.replace(Regex("(@Википедия|@Wikipedia).*$", RegexOption.IGNORE_CASE), "").trim()
    }

    val textColor by animateColorAsState(
        targetValue = if (isActive) Color.White else Color.White.copy(alpha = 0.7f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .tvAndWebHoverEffect(
                    scaleTarget = 1.0f,
                    activeBorderColor = Color.Transparent,
                    activeBorderWidth = 0.dp,
                    defaultBorderWidth = 0.dp,
                    shape = RoundedCornerShape(8.dp),
                    clickEnabled = hasVisualOverflow || isExpanded,
                    onStateChange = { isActive = it }
                )
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (hasVisualOverflow || isExpanded) {
                        Modifier.clickable(enabled = !isLoading) { isExpanded = !isExpanded }
                    } else Modifier
                )
                .padding(8.dp)
        ) {
            Text(
                text = cleanedText,
                color = if (isLoading) Color.Transparent else textColor,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult ->
                    if (!isExpanded) {
                        hasVisualOverflow = textLayoutResult.hasVisualOverflow
                    }
                },
                modifier = Modifier.shimmerPlaceholder(isLoading, RoundedCornerShape(8.dp))
            )

            if ((hasVisualOverflow || isExpanded) && !isLoading) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isExpanded) stringResource(Res.string.details_desc_collapse) else stringResource(Res.string.details_desc_expand),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (isActive) 1f else 0.8f)
                )
            }
        }
    }
}