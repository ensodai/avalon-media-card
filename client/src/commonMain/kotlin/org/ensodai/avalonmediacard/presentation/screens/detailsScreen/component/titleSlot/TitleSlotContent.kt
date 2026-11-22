package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.titleSlot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import org.ensodai.avalonmediacard.designsystem.components.AvalonText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder

@Composable
fun TitleSlotContent(
    data: SlotData.Header,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AvalonText(
            text = data.title,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isLoading) Color.Transparent else Color.White,
            modifier = Modifier.shimmerPlaceholder(isLoading, RoundedCornerShape(8.dp))
        )

        val originalTitle = data.originalTitle
        if ((!originalTitle.isNullOrEmpty() && originalTitle != data.title) || isLoading) {
            val titleText =
                if (isLoading && originalTitle.isNullOrEmpty()) "Original Title Placeholder" else (originalTitle
                    ?: "")
            AvalonText(
                text = titleText,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isLoading) Color.Transparent else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp)
                    .shimmerPlaceholder(isLoading, RoundedCornerShape(4.dp))
            )
        }

        val tagline = data.tagline
        if (!tagline.isNullOrEmpty() || isLoading) {
            val taglineText =
                if (isLoading && tagline.isNullOrEmpty()) "Placeholder for tagline in skeleton loading state" else (tagline?.replace(
                    "«",
                    ""
                )?.replace("»", "")?.trim() ?: "")
            AvalonText(
                text = taglineText,
                fontSize = 15.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Light,
                color = if (isLoading) Color.Transparent else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 6.dp)
                    .shimmerPlaceholder(isLoading, RoundedCornerShape(4.dp))
            )
        }
    }
}