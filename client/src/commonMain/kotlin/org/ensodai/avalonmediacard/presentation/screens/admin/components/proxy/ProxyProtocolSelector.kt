package org.ensodai.avalonmediacard.presentation.screens.admin.components.proxy

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ensodai.avalonmediacard.contract.admin.ProxyType
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect

@Composable
fun ProxyProtocolSelector(
    selectedType: ProxyType,
    onTypeSelected: (ProxyType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProxyTypeButton(
            type = ProxyType.HTTP,
            isSelected = selectedType == ProxyType.HTTP,
            onClick = { onTypeSelected(ProxyType.HTTP) }
        )
        ProxyTypeButton(
            type = ProxyType.SOCKS5,
            isSelected = selectedType == ProxyType.SOCKS5,
            onClick = { onTypeSelected(ProxyType.SOCKS5) }
        )
    }
}

@Composable
fun ProxyTypeButton(
    type: ProxyType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    )

    Box(
        modifier = Modifier
            .height(38.dp)
            .tvAndWebHoverEffect(
                shape = shape,
                scaleTarget = 1.05f,
                clickEnabled = true,
                onClick = onClick,
                activeBorderColor = Color.White,
                activeBorderWidth = 1.5.dp
            )
            .clip(shape)
            .background(bgColor)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = type.name,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}
