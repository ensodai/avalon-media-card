package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Users
import com.composables.icons.lucide.X
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon as Icon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText as Text
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyStep
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchPartyHeader(
    mediaTitle: String,
    step: WatchPartyStep,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    roomTitle: String = "",
    isHost: Boolean = false,
    joinPin: String? = null
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes
    val elevation = AvalonTheme.elevation
    val typography = AvalonTheme.typography

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (step == WatchPartyStep.LOBBY) {
            Text(
                text = roomTitle.ifBlank { mediaTitle },
                style = typography.titleLarge,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (step == WatchPartyStep.SETUP) {
                    val backShape = CircleShape
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .tvAndWebHoverEffect(
                                scaleTarget = 1.08f,
                                shape = backShape,
                                activeBorderColor = colors.borderFocus,
                                onClick = onBack
                            )
                            .background(colors.bgElevated, backShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.ArrowLeft,
                            contentDescription = stringResource(Res.string.common_previous),
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.bgElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Users,
                            contentDescription = null,
                            tint = colors.accentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = when (step) {
                            WatchPartyStep.ENTRY -> stringResource(Res.string.watch_party_title)
                            WatchPartyStep.SETUP -> stringResource(Res.string.watch_party_step_setup)
                            WatchPartyStep.LOBBY -> roomTitle.ifBlank { mediaTitle }
                        },
                        style = typography.titleLarge,
                        color = colors.textPrimary
                    )
                    if (mediaTitle.isNotBlank()) {
                        Text(
                            text = mediaTitle,
                            style = typography.metadata,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (step == WatchPartyStep.LOBBY && isHost && !joinPin.isNullOrBlank()) {
                val clipboard = LocalClipboardManager.current
                var copied by remember { mutableStateOf(false) }
                val formattedPin = if (joinPin.length == 6) "${joinPin.take(3)} ${joinPin.takeLast(3)}" else joinPin

                val pinShape = shapes.control
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .tvAndWebHoverEffect(
                            scaleTarget = 1.05f,
                            shape = pinShape,
                            activeBorderColor = colors.borderFocus,
                            defaultBorderWidth = elevation.borderWidth,
                            defaultBorderColor = colors.accentPrimary.copy(alpha = 0.40f),
                            onClick = {
                                clipboard.setText(AnnotatedString(joinPin))
                                copied = true
                            }
                        )
                        .background(colors.accentPrimary.copy(alpha = 0.15f), pinShape)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "PIN:",
                        style = typography.microCaption,
                        color = colors.textSecondary
                    )
                    Text(
                        text = formattedPin,
                        style = typography.monoTech,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        color = colors.accentPrimary
                    )
                    Icon(
                        imageVector = if (copied) Lucide.Check else Lucide.Copy,
                        contentDescription = null,
                        tint = if (copied) colors.statusLive else colors.textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            val closeShape = CircleShape
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .tvAndWebHoverEffect(
                        scaleTarget = 1.08f,
                        shape = closeShape,
                        activeBorderColor = colors.borderFocus,
                        onClick = onClose
                    )
                    .background(colors.bgElevated, closeShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.X,
                    contentDescription = stringResource(Res.string.common_close),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
