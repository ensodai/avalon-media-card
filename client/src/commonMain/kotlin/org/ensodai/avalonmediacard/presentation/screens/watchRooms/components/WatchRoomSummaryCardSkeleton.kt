package org.ensodai.avalonmediacard.presentation.screens.watchRooms.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder

/**
 * Типизированный скелетон карточки комнаты совместного просмотра.
 *
 * Точно повторяет геометрию [WatchRoomSummaryCard]:
 * - Высота: 210 dp
 * - Скругление: RoundedCornerShape(14.dp)
 * - Верхний ряд: плашка статуса (76.dp x 22.dp)
 * - Нижний блок: заголовок (fillMaxWidth(0.65f) x 18.dp), подзаголовок (fillMaxWidth(0.40f) x 14.dp), социальная строка с аватарками
 */
@Composable
fun WatchRoomSummaryCardSkeleton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Верхняя строка: бейдж статуса (слева) + хост (справа)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 76.dp, height = 22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmerPlaceholder(isLoading = true, shape = RoundedCornerShape(6.dp))
                )

                Box(
                    modifier = Modifier
                        .size(width = 52.dp, height = 20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmerPlaceholder(isLoading = true, shape = RoundedCornerShape(6.dp))
                )
            }

            // Нижний блок: Заголовок + Метаданные + Социальная строка
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Название комнаты
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerPlaceholder(isLoading = true, shape = RoundedCornerShape(4.dp))
                )

                // Подзаголовок: Тайтл фильма/сериала + сезон/эпизод
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.40f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerPlaceholder(isLoading = true, shape = RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Социальная строка: аватарки участников + время
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .shimmerPlaceholder(isLoading = true, shape = CircleShape)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(width = 60.dp, height = 12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerPlaceholder(isLoading = true, shape = RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}
