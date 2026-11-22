package org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import org.ensodai.avalonmediacard.designsystem.components.AvalonCard as Card
import org.ensodai.avalonmediacard.designsystem.components.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget

@Composable
fun ShowGroupCardSkeleton(
    modifier: Modifier = Modifier
) {
    val isTv = LocalDeviceTarget.current.isTv
    val cardWidth = if (isTv) 320.dp else 290.dp

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AvalonTheme.colors.surfaceVariant.copy(alpha = 0.2f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = AvalonTheme.colors.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            // 1. Шапка сериала (скелетон)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .shimmerPlaceholder(isLoading = true, shape = RoundedCornerShape(6.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Box(
                            modifier = Modifier
                                .width(180.dp)
                                .height(18.dp)
                                .shimmerPlaceholder(isLoading = true, shape = RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(12.dp)
                                .shimmerPlaceholder(isLoading = true, shape = RoundedCornerShape(4.dp))
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .shimmerPlaceholder(isLoading = true, shape = RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Горизонтальный ряд серий (скелетоны)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(3) {
                    Box(
                        modifier = Modifier
                            .width(cardWidth)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141418))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .shimmerPlaceholder(isLoading = true, shape = RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(14.dp)
                                    .shimmerPlaceholder(isLoading = true, shape = RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(11.dp)
                                    .shimmerPlaceholder(isLoading = true, shape = RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .shimmerPlaceholder(isLoading = true, shape = RoundedCornerShape(8.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .shimmerPlaceholder(isLoading = true, shape = RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
