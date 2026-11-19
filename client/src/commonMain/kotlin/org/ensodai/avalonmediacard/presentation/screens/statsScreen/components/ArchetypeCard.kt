package org.ensodai.avalonmediacard.presentation.screens.statsScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.Sparkles
import org.ensodai.avalonmediacard.contract.model.ArchetypeDto
import org.ensodai.avalonmediacard.contract.model.ArchetypeType
import org.ensodai.avalonmediacard.presentation.screens.statsScreen.ArchetypeUiMapper
import org.jetbrains.compose.resources.stringResource

@Composable
fun ArchetypeCard(
    archetype: ArchetypeDto?,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientColors = ArchetypeUiMapper.getGradientColors(archetype?.type)

    val title = when (archetype?.type) {
        ArchetypeType.SCIFI_VISIONARY -> stringResource(Res.string.stats_archetype_scifi_title)
        ArchetypeType.SUSPENSE_HUNTER -> stringResource(Res.string.stats_archetype_suspense_title)
        ArchetypeType.DEEP_EMPATH -> stringResource(Res.string.stats_archetype_empath_title)
        ArchetypeType.DOPAMINE_SEEKER -> stringResource(Res.string.stats_archetype_dopamine_title)
        ArchetypeType.CURIOUS_VOYAGER -> stringResource(Res.string.stats_archetype_voyager_title)
        null -> stringResource(Res.string.stats_archetype_forming_title)
    }

    val subtitle = when (archetype?.type) {
        ArchetypeType.SCIFI_VISIONARY -> stringResource(Res.string.stats_archetype_scifi_subtitle)
        ArchetypeType.SUSPENSE_HUNTER -> stringResource(Res.string.stats_archetype_suspense_subtitle)
        ArchetypeType.DEEP_EMPATH -> stringResource(Res.string.stats_archetype_empath_subtitle)
        ArchetypeType.DOPAMINE_SEEKER -> stringResource(Res.string.stats_archetype_dopamine_subtitle)
        ArchetypeType.CURIOUS_VOYAGER -> stringResource(Res.string.stats_archetype_voyager_subtitle)
        null -> stringResource(Res.string.stats_archetype_forming_subtitle)
    }

    val description = when (archetype?.type) {
        ArchetypeType.SCIFI_VISIONARY -> stringResource(Res.string.stats_archetype_scifi_desc)
        ArchetypeType.SUSPENSE_HUNTER -> stringResource(Res.string.stats_archetype_suspense_desc)
        ArchetypeType.DEEP_EMPATH -> stringResource(Res.string.stats_archetype_empath_desc)
        ArchetypeType.DOPAMINE_SEEKER -> stringResource(Res.string.stats_archetype_dopamine_desc)
        ArchetypeType.CURIOUS_VOYAGER -> stringResource(Res.string.stats_archetype_voyager_desc)
        null -> stringResource(Res.string.stats_archetype_forming_desc)
    }

    Surface(
        modifier = modifier.defaultMinSize(minHeight = 220.dp),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Lucide.Sparkles,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(Res.string.stats_archetype_badge),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    if (archetype != null) {
                        ElevatedButton(
                            onClick = onShare,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = Color.White.copy(alpha = 0.25f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Lucide.Share2,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(Res.string.stats_archetype_share),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 20.sp
                )
            }
        }
    }
}
