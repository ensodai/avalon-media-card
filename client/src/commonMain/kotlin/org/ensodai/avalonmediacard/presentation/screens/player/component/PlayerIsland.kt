package org.ensodai.avalonmediacard.presentation.screens.player.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect

/**
 * Базовый визуальный островок (Floating Island) для плеера.
 * Используется в топ-баре, контролах и информационных плашках.
 */
@Composable
fun PlayerIslandContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = Color.Black.copy(alpha = 0.65f),
    borderColor: Color = Color.White.copy(alpha = 0.08f),
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Кнопка-островок с поддержкой D-Pad фокуса на ТВ и hover-эффектов.
 */
@Composable
fun PlayerIslandIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tint: Color = Color.White,
    backgroundColor: Color = Color.Black.copy(alpha = 0.65f),
    borderColor: Color = Color.White.copy(alpha = 0.08f)
) {
    Box(
        modifier = modifier
            .size(size)
            .tvAndWebHoverEffect(
                scaleTarget = 1.12f,
                shape = shape,
                activeBorderColor = MaterialTheme.colorScheme.primary,
                activeBorderWidth = 2.dp,
                defaultBorderColor = borderColor,
                defaultBorderWidth = 1.dp,
                onClick = onClick
            )
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Островок с заголовком и подзаголовком медиафайла / сериала.
 */
@Composable
fun PlayerIslandTitle(
    topText: String,
    bottomText: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp)
) {
    PlayerIslandContainer(
        modifier = modifier,
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            if (topText.isNotBlank()) {
                Text(
                    text = topText,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = bottomText,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Интерактивная кнопка-островок с иконкой и текстом (для статусов "Отметить", "Оценка" и др.)
 */
@Composable
fun PlayerIslandActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White,
    textColor: Color = Color.White,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = Color.Black.copy(alpha = 0.65f),
    borderColor: Color = Color.White.copy(alpha = 0.08f)
) {
    Box(
        modifier = modifier
            .tvAndWebHoverEffect(
                scaleTarget = 1.08f,
                shape = shape,
                activeBorderColor = MaterialTheme.colorScheme.primary,
                activeBorderWidth = 2.dp,
                defaultBorderColor = borderColor,
                defaultBorderWidth = 1.dp,
                onClick = onClick
            )
            .clip(shape)
            .background(backgroundColor)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = iconTint,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Динамическая кнопка-островок со стабильной нодой фокуса.
 * Сохраняет FocusNode и позицию фокуса на ТВ при динамической смене состояния
 * (например, переход Глазик <-> Просмотрено или Звезда <-> Оценка).
 */
@Composable
fun PlayerIslandDynamicButton(
    icon: ImageVector,
    text: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    iconTint: Color = Color.White,
    textColor: Color = Color.White,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = Color.Black.copy(alpha = 0.65f),
    borderColor: Color = Color.White.copy(alpha = 0.08f)
) {
    val hasText = !text.isNullOrBlank()
    val finalShape = if (hasText) shape else CircleShape

    Box(
        modifier = modifier
            .then(
                if (hasText) Modifier else Modifier.size(44.dp)
            )
            .tvAndWebHoverEffect(
                scaleTarget = 1.08f,
                shape = finalShape,
                activeBorderColor = MaterialTheme.colorScheme.primary,
                activeBorderWidth = 2.dp,
                defaultBorderColor = borderColor,
                defaultBorderWidth = 1.dp,
                onClick = onClick
            )
            .clip(finalShape)
            .background(backgroundColor)
            .then(
                if (hasText) Modifier.padding(horizontal = 14.dp, vertical = 9.dp) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription ?: text,
                tint = iconTint,
                modifier = Modifier.size(if (hasText) 17.dp else 22.dp)
            )
            if (hasText && text != null) {
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = text,
                    color = textColor,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


