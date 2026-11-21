package org.ensodai.avalonmediacard.presentation.screens.statsScreen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Clapperboard
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Share2
import org.ensodai.avalonmediacard.contract.model.ArchetypeDto
import org.ensodai.avalonmediacard.contract.model.ArchetypeType
import org.ensodai.avalonmediacard.presentation.theme.AvalonTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun ArchetypeCard(
    archetype: ArchetypeDto?,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes

    Surface(
        modifier = modifier.defaultMinSize(minHeight = 220.dp),
        shape = shapes.container,
        color = colors.bgSurface,
        border = BorderStroke(1.dp, colors.borderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Шапка карточки: категория + кнопка шеринга
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Lucide.Clapperboard,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.stats_archetype_badge),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary
                    )
                }

                if (archetype != null) {
                    Surface(
                        onClick = onShare,
                        shape = shapes.control,
                        color = colors.bgElevated
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Share2,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(Res.string.stats_archetype_share),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Название архетипа
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary
            )

            // Подзаголовок-наножанр
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = shapes.control,
                color = colors.bgElevated
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                lineHeight = 21.sp
            )
        }
    }
}
