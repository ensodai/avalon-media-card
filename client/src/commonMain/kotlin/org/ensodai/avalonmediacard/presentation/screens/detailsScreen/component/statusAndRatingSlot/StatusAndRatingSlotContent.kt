package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.statusAndRatingSlot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.*
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonDropdownMenu
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonDropdownMenuItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.AvalonTvDrawerItem
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.EpisodeRatingPopup
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvDrawerEffect
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.TvEpisodeRatingPopup
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

data class StatusOptionRes(val status: MediaStatus, val labelRes: StringResource)

val DefaultStatusOptionList = listOf(
    StatusOptionRes(MediaStatus.WATCHING, Res.string.details_status_watching),
    StatusOptionRes(MediaStatus.PLANNED, Res.string.details_status_planned),
    StatusOptionRes(MediaStatus.COMPLETED, Res.string.details_status_completed),
    StatusOptionRes(MediaStatus.DROPPED, Res.string.details_status_dropped)
)

@Composable
fun StatusAndRatingSlotContent(
    component: SlotData.UserActions,
    isLoading: Boolean = false,
    onAction: (Action) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDropdownButton(component, isLoading, onAction)

        // Тонкий вертикальный разделитель
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(16.dp)
                .background(if (isLoading) Color.Transparent else Color.White.copy(alpha = 0.2f))
        )

        RatingDropdownButton(component, isLoading, onAction)
    }
}

@Composable
fun StatusDropdownButton(
    component: SlotData.UserActions,
    isLoading: Boolean,
    onAction: (Action) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentStatusOption = DefaultStatusOptionList.find { it.status == component.currentStatus }
    val label = if (currentStatusOption != null) {
        stringResource(currentStatusOption.labelRes)
    } else {
        stringResource(Res.string.details_status_add)
    }
    val iconTint = if (currentStatusOption != null) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)

    Box {
        // Сама кнопка
        Row(
            modifier = Modifier
                .shimmerPlaceholder(isLoading, RoundedCornerShape(8.dp))
                .tvAndWebHoverEffect(
                    scaleTarget = 1.04f,
                    shape = RoundedCornerShape(8.dp),
                    clickEnabled = !isLoading,
                    onClick = { expanded = true }
                )
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isTvTarget = LocalDeviceTarget.current.isTv
            Icon(
                Lucide.Eye,
                contentDescription = null,
                tint = if (isLoading) Color.Transparent else iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isLoading) Color.Transparent else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (!isTvTarget) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Lucide.ChevronDown,
                    contentDescription = null,
                    tint = if (isLoading) Color.Transparent else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Выпадающее меню или Шторка
        if (expanded) {
            val isTvTarget = LocalDeviceTarget.current.isTv
            if (isTvTarget) {
                TvDrawerEffect(
                    title = stringResource(Res.string.details_status_my),
                    icon = Lucide.Eye,
                    onDismiss = { expanded = false }
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            AvalonTvDrawerItem(
                                title = stringResource(Res.string.details_status_remove),
                                icon = Lucide.X,
                                isSelected = component.currentStatus == MediaStatus.NONE,
                                onClick = {
                                    val action = component.statusOptions[MediaStatus.NONE]
                                    if (action != null) onAction(action)
                                    expanded = false
                                }
                            )
                        }
                        items(DefaultStatusOptionList) { option ->
                            val isSelected = component.currentStatus == option.status
                            AvalonTvDrawerItem(
                                title = stringResource(option.labelRes),
                                isSelected = isSelected,
                                onClick = {
                                    val action = component.statusOptions[option.status]
                                    if (action != null) onAction(action)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                val density = LocalDensity.current
                AvalonDropdownMenu(
                    expanded = expanded,
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, with(density) { 40.dp.roundToPx() }),
                    onDismissRequest = { expanded = false }
                ) {
                    DefaultStatusOptionList.forEach { option ->
                        val isSelected = component.currentStatus == option.status
                        val optionLabel = stringResource(option.labelRes)
                        AvalonDropdownMenuItem(
                            text = optionLabel,
                            icon = if (isSelected) Lucide.Check else null,
                            textColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                            iconColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                            onClick = {
                                val newStatus = if (isSelected) MediaStatus.NONE else option.status
                                val action = component.statusOptions[newStatus]
                                if (action != null) {
                                    onAction(action)
                                }
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RatingDropdownButton(
    component: SlotData.UserActions,
    isLoading: Boolean,
    onAction: (Action) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentRating = component.currentRating
    val label = if (currentRating != null) {
        stringResource(Res.string.details_rating_my, currentRating)
    } else {
        stringResource(Res.string.details_rating_rate)
    }
    val iconTint = if (currentRating != null) Color(0xFFFFC107) else Color.White.copy(alpha = 0.7f)

    Box {
        // Сама кнопка
        Row(
            modifier = Modifier
                .shimmerPlaceholder(isLoading, RoundedCornerShape(8.dp))
                .tvAndWebHoverEffect(
                    scaleTarget = 1.04f,
                    shape = RoundedCornerShape(8.dp),
                    clickEnabled = !isLoading,
                    onClick = { expanded = true }
                )
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isTvTarget = LocalDeviceTarget.current.isTv
            Icon(
                Lucide.Star,
                contentDescription = null,
                tint = if (isLoading) Color.Transparent else iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isLoading) Color.Transparent else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (!isTvTarget) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Lucide.ChevronDown,
                    contentDescription = null,
                    tint = if (isLoading) Color.Transparent else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Выпадающее меню с логикой звезд
        if (expanded) {
            val isTvTarget = LocalDeviceTarget.current.isTv
            if (isTvTarget) {
                TvEpisodeRatingPopup(
                    currentRating = currentRating,
                    maxRating = component.maxRating,
                    onDismiss = { expanded = false },
                    onRate = { rating ->
                        val action = component.ratingOptions[rating]
                        if (action != null) {
                            onAction(action)
                        }
                        expanded = false
                    }
                )
            } else {
                val density = LocalDensity.current
                EpisodeRatingPopup(
                    currentRating = currentRating,
                    maxRating = component.maxRating,
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, with(density) { 40.dp.roundToPx() }),
                    onDismiss = { expanded = false },
                    onRate = { rating ->
                        val action = component.ratingOptions[rating]
                        if (action != null) {
                            onAction(action)
                        }
                        expanded = false
                    }
                )
            }
        }
    }
}