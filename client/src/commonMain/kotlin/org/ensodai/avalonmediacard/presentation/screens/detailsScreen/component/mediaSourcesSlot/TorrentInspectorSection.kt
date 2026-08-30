package org.ensodai.avalonmediacard.presentation.screens.detailsScreen.component.mediaSourcesSlot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.contract.slot.withParameter
import org.jetbrains.compose.resources.stringResource

@Composable
fun TorrentInspectorSection(
    component: SlotData.TorrentInspector,
    onAction: (Action) -> Unit,
    isExpanded: Boolean,
    onCloseSources: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .background(Color(0xFF1E1E2E).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Lucide.Info, contentDescription = null, tint = Color(0xFFFFB300))
            Text(
                text = stringResource(Res.string.details_inspector_title),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = stringResource(Res.string.details_inspector_subtitle, component.torrentTitle),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        component.files.forEach { file ->
            var season by remember { mutableStateOf(file.mappedSeasons?.firstOrNull()?.toString() ?: "") }
            var episode by remember { mutableStateOf(file.mappedEpisodes?.firstOrNull()?.toString() ?: "") }
            var isSaved by remember { mutableStateOf(file.mappedSeasons != null && file.mappedEpisodes != null) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = file.path.substringAfterLast('/'),
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )

                if (file.isVideo) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallNumberInput(
                            value = season,
                            onValueChange = { season = it; isSaved = false },
                            placeholder = stringResource(Res.string.details_inspector_season)
                        )
                        SmallNumberInput(
                            value = episode,
                            onValueChange = { episode = it; isSaved = false },
                            placeholder = stringResource(Res.string.details_inspector_episode)
                        )

                        Button(
                            onClick = {
                                val s = season.toIntOrNull()
                                val e = episode.toIntOrNull()
                                if (s != null && e != null) {
                                    val act = file.remapAction
                                        ?.withParameter("season", s)
                                        ?.withParameter("episode", e)
                                    if (act != null) {
                                        onAction(act)
                                        isSaved = true
                                    }
                                }
                            },
                            enabled = !isSaved && season.isNotBlank() && episode.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSaved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(if (isSaved) stringResource(Res.string.details_inspector_saved) else stringResource(Res.string.details_inspector_save), fontSize = 12.sp)
                        }
                    }
                } else {
                    Text(text = stringResource(Res.string.details_inspector_not_video), color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SmallNumberInput(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    BasicTextField(
        value = value,
        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) onValueChange(it) },
        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(text = placeholder, color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp)
                }
                innerTextField()
            }
        }
    )
}
