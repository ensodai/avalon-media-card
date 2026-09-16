package org.ensodai.avalonmediacard.presentation.screens.mediaSources.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.contract.slot.TorrentFileItem
import org.ensodai.avalonmediacard.contract.slot.withParameter
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.jetbrains.compose.resources.stringResource

@Composable
fun TorrentInspectorSection(
    component: SlotData.TorrentInspector,
    onAction: (Action) -> Unit,
    isExpanded: Boolean,
    onCloseSources: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceTarget = LocalDeviceTarget.current
    val isTv = deviceTarget.isTv

    val containerModifier = if (isTv) {
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .background(Color(0xFF1E1E2E).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    }

    Column(
        modifier = containerModifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Lucide.Info, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(if (isTv) 28.dp else 22.dp))
                Text(
                    text = stringResource(Res.string.details_inspector_title),
                    color = Color.White,
                    fontSize = if (isTv) 22.sp else 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .size(if (isTv) 44.dp else 36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onCloseSources) {
                    Icon(
                        Lucide.X,
                        contentDescription = stringResource(Res.string.common_close),
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Text(
            text = stringResource(Res.string.details_inspector_subtitle, component.torrentTitle),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = if (isTv) 16.sp else 14.sp
        )

        LazyColumn(
            modifier = if (isTv) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(component.files, key = { it.path }) { file ->
                TorrentInspectorFileRow(
                    file = file,
                    onAction = onAction,
                    isTv = isTv
                )
            }
        }
    }
}

@Composable
private fun TorrentInspectorFileRow(
    file: TorrentFileItem,
    onAction: (Action) -> Unit,
    isTv: Boolean
) {
    var season by remember(file) { mutableStateOf(file.mappedSeasons?.firstOrNull()?.toString() ?: "") }
    var episode by remember(file) { mutableStateOf(file.mappedEpisodes?.firstOrNull()?.toString() ?: "") }
    var isSaved by remember(file) { mutableStateOf(file.mappedSeasons != null && file.mappedEpisodes != null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(if (isTv) 16.dp else 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = file.path.substringAfterLast('/'),
            color = Color.White,
            fontSize = if (isTv) 15.sp else 13.sp,
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
                    placeholder = stringResource(Res.string.details_inspector_season),
                    isTv = isTv
                )
                SmallNumberInput(
                    value = episode,
                    onValueChange = { episode = it; isSaved = false },
                    placeholder = stringResource(Res.string.details_inspector_episode),
                    isTv = isTv
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
                    contentPadding = PaddingValues(horizontal = if (isTv) 16.dp else 12.dp, vertical = if (isTv) 8.dp else 6.dp),
                    modifier = Modifier.height(if (isTv) 40.dp else 32.dp)
                ) {
                    Text(
                        if (isSaved) stringResource(Res.string.details_inspector_saved) else stringResource(Res.string.details_inspector_save),
                        fontSize = if (isTv) 14.sp else 12.sp
                    )
                }
            }
        } else {
            Text(
                text = stringResource(Res.string.details_inspector_not_video),
                color = Color.Gray,
                fontSize = if (isTv) 14.sp else 12.sp
            )
        }
    }
}

@Composable
private fun SmallNumberInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isTv: Boolean = false
) {
    BasicTextField(
        value = value,
        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) onValueChange(it) },
        textStyle = TextStyle(color = Color.White, fontSize = if (isTv) 16.sp else 14.sp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .width(if (isTv) 72.dp else 60.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = if (isTv) 8.dp else 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(text = placeholder, color = Color.White.copy(alpha = 0.3f), fontSize = if (isTv) 16.sp else 14.sp)
                }
                innerTextField()
            }
        }
    )
}
