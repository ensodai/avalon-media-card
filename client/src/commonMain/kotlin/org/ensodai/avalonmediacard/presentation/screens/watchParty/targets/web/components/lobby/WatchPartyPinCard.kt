package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.designsystem.components.AvalonIcon
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import org.ensodai.avalonmediacard.designsystem.theme.AvalonTheme
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchPartyPinCard(
    pin: String,
    modifier: Modifier = Modifier
) {
    val colors = AvalonTheme.colors
    val shapes = AvalonTheme.shapes
    val elevation = AvalonTheme.elevation
    val typography = AvalonTheme.typography

    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.container)
            .background(colors.bgElevated)
            .border(elevation.borderWidth, colors.borderFocus.copy(alpha = 0.4f), shapes.container)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            AvalonText(
                text = stringResource(Res.string.watch_party_pin_label).uppercase(),
                style = typography.microCaption,
                color = colors.textSecondary,
                letterSpacing = 1.sp
            )
            AvalonText(
                text = "${pin.take(3)} ${pin.takeLast(3)}",
                style = typography.monoTech,
                fontSize = 26.sp,
                color = colors.accentPrimary,
                letterSpacing = 4.sp
            )
        }

        val copyBtnShape = shapes.control
        Row(
            modifier = Modifier
                .height(38.dp)
                .tvAndWebHoverEffect(
                    scaleTarget = 1.05f,
                    shape = copyBtnShape,
                    activeBorderColor = colors.borderFocus,
                    onClick = {
                        clipboard.setText(AnnotatedString(pin))
                        copied = true
                    }
                )
                .background(colors.bgSurface, copyBtnShape)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AvalonIcon(
                imageVector = if (copied) Lucide.Check else Lucide.Copy,
                contentDescription = null,
                tint = if (copied) colors.statusLive else colors.textPrimary,
                modifier = Modifier.size(14.dp)
            )
            AvalonText(
                text = if (copied) stringResource(Res.string.watch_party_pin_copied) else stringResource(Res.string.watch_party_pin_copy),
                style = typography.uiLabel,
                color = colors.textPrimary
            )
        }
    }
}
