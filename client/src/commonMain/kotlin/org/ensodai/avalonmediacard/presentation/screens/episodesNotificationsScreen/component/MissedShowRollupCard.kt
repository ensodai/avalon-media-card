package org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component

import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import org.ensodai.avalonmediacard.designsystem.components.AvalonButton as Button
import org.ensodai.avalonmediacard.designsystem.components.AvalonCard as Card
import org.ensodai.avalonmediacard.designsystem.components.CardDefaults
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon as Icon
import org.ensodai.avalonmediacard.designsystem.components.AvalonIconButton as IconButton
import org.ensodai.avalonmediacard.designsystem.components.AvalonText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.MissedShowRollupItem
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.jetbrains.compose.resources.stringResource

@Composable
fun MissedShowRollupCard(
    rollup: MissedShowRollupItem,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    val continueAction = rollup.continueAction
    val openDetailsAction = rollup.openDetailsAction

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isHighlighted = isFocused || isHovered

    val cardScale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.01f else 1.0f,
        label = "RollupCardScale"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) {
                AvalonTheme.colors.surfaceVariant.copy(alpha = 0.5f)
            } else {
                AvalonTheme.colors.surfaceVariant.copy(alpha = 0.25f)
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) AvalonTheme.colors.primary else Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Постер тайтла
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AvalonTheme.colors.surfaceVariant)
            ) {
                ShimmerImage(
                    model = rollup.showPosterUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Текстовый блок
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Lucide.Folder,
                        contentDescription = null,
                        tint = AvalonTheme.colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = rollup.showTitle,
                        style = AvalonTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = AvalonTheme.colors.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(
                        Res.string.episodes_notifications_rollup_info,
                        rollup.missedEpisodesCount,
                        rollup.startSeason,
                        rollup.startEpisode,
                        rollup.endSeason,
                        rollup.endEpisode
                    ),
                    style = AvalonTheme.typography.bodyMedium.copy(
                        color = AvalonTheme.colors.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Кнопки
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (continueAction != null) {
                    Button(
                        onClick = { onAction(continueAction) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Play,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(
                                Res.string.episodes_notifications_rollup_start,
                                rollup.startSeason,
                                rollup.startEpisode
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (openDetailsAction != null) {
                    IconButton(
                        onClick = { onAction(openDetailsAction) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Info,
                            contentDescription = stringResource(Res.string.episodes_notifications_about_show),
                            tint = AvalonTheme.colors.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
