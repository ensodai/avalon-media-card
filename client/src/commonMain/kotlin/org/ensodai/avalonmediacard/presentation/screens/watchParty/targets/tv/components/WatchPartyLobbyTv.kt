package org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.tv.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.Res
import avalonmediacard.client.generated.resources.common_close
import avalonmediacard.client.generated.resources.watch_party_btn_leave
import avalonmediacard.client.generated.resources.watch_party_btn_start_playback
import avalonmediacard.client.generated.resources.watch_party_participants_label
import avalonmediacard.client.generated.resources.watch_party_pin_label
import avalonmediacard.client.generated.resources.watch_party_ready_check
import avalonmediacard.client.generated.resources.watch_party_ready_status_ready
import avalonmediacard.client.generated.resources.watch_party_source_change
import avalonmediacard.client.generated.resources.watch_party_waiting_host
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.X
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect
import org.ensodai.avalonmediacard.presentation.screens.watchParty.action.WatchPartyActions
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.WatchPartyParticipantItem
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.lobby.WatchPartyMediaHeroCard
import org.ensodai.avalonmediacard.presentation.screens.watchParty.targets.web.components.lobby.formatSourceProviderTitle
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyViewState
import org.jetbrains.compose.resources.stringResource

@Composable
fun WatchPartyLobbyTv(
    state: WatchPartyViewState,
    actions: WatchPartyActions,
    modifier: Modifier = Modifier
) {
    val mediaTitle = state.activeRoom?.mediaTitle?.takeIf { it.isNotBlank() }
        ?: state.mediaTitle.takeIf { it.isNotBlank() && it != state.activeRoom?.title }
        ?: state.activeRoom?.title
        ?: ""
    val isMovie = state.activeRoom?.mediaType == MediaType.MOVIE || state.mediaKey?.type == EntityType.MOVIE
    val seasonNum = if (isMovie) null else (state.activeRoom?.currentSeason ?: state.selectedSeason)
    val episodeNum = if (isMovie) null else (state.activeRoom?.currentEpisode ?: state.selectedEpisode)
    val rawSourceType = state.activeRoom?.sourceType ?: state.selectedSourceType
    val sourceProvider = formatSourceProviderTitle(rawSourceType)
    val sourceDetails = state.selectedSourceName
    val backdropUrl = state.activeRoom?.backdropUrl

    val totalParticipants = state.participants.size
    val readyParticipants = state.participants.count { it.isReady }
    val joinPin = state.activeRoom?.joinPin

    val screenPadding = 36.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Полноэкранный кинематографичный бэкдроп
        if (!backdropUrl.isNullOrBlank()) {
            ShimmerImage(
                model = backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.70f),
                                0.50f to Color.Black.copy(alpha = 0.85f),
                                1.0f to Color(0xFF0F0F14).copy(alpha = 0.98f)
                            )
                        )
                    )
            )
        }

        // 2. Основное содержимое лобби
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Верхняя панель: Кнопка закрытия, Название комнаты, PIN комнаты
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = screenPadding, end = screenPadding, top = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .tvAndWebHoverEffect(scaleTarget = 1.1f, shape = CircleShape, onClick = actions.onClose)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = stringResource(Res.string.common_close),
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    val roomTitle = state.activeRoom?.title?.takeIf { it.isNotBlank() } ?: mediaTitle
                    Text(
                        text = roomTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // PIN-код комнаты для подключения друзей с мобилок/веба
                if (!joinPin.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.watch_party_pin_label).replace(":", "").uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${joinPin.take(3)} ${joinPin.takeLast(3)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }

            // Центральная зона: 2 колонки (Медиа слева, Участники и Настрой справа)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = screenPadding, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Левая колонка: Hero-карточка медиа, выбранный источник, кнопка смены
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WatchPartyMediaHeroCard(
                        mediaTitle = mediaTitle,
                        seasonNum = seasonNum,
                        episodeNum = episodeNum,
                        sourceProvider = sourceProvider,
                        sourceDetails = sourceDetails,
                        backdropUrl = backdropUrl,
                        isHost = state.isHost,
                        isSelectingSource = state.isSelectingSource,
                        onToggleSelectSource = { actions.onToggleSelectSource(!state.isSelectingSource) }
                    )

                    if (state.systemNotice != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFB74D).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFFFFB74D).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = state.systemNotice,
                                fontSize = 13.sp,
                                color = Color(0xFFFFB74D),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Правая колонка: Список участников и выбор настроения
                Column(
                    modifier = Modifier.weight(1.3f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Список участников
                    Text(
                        text = stringResource(Res.string.watch_party_participants_label, state.participants.size),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.participants, key = { it.userId.toString() }) { participant ->
                            WatchPartyParticipantItem(participant = participant)
                        }
                    }

                    // Выбор настроения для ТВ
                    WatchPartyIntentSelectorTv(
                        selectedIntent = state.myIntent,
                        isReady = state.myIsReady,
                        onSelectIntent = actions.onSetMyIntent
                    )
                }
            }

            // Нижняя панель действий (Запуск / Готовность / Выход)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = screenPadding, end = screenPadding, top = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Кнопка "Покинуть комнату"
                val leaveShape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .height(52.dp)
                        .tvAndWebHoverEffect(
                            scaleTarget = 1.04f,
                            shape = leaveShape,
                            activeBorderColor = Color.White.copy(alpha = 0.6f),
                            onClick = actions.onLeaveRoom
                        )
                        .background(Color.White.copy(alpha = 0.08f), leaveShape)
                        .padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.watch_party_btn_leave),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }

                // Главная кнопка старта для хоста или переключатель готовности для участника
                if (state.isHost) {
                    val startShape = RoundedCornerShape(14.dp)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .tvAndWebHoverEffect(
                                scaleTarget = 1.03f,
                                shape = startShape,
                                activeBorderWidth = 2.dp,
                                activeBorderColor = MaterialTheme.colorScheme.primary,
                                onClick = actions.onStartPlayback
                            )
                            .background(Color.White, startShape),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (state.isStartingPlayback) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = Lucide.Play,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (totalParticipants > 0) {
                                    "${stringResource(Res.string.watch_party_btn_start_playback)} ($readyParticipants/$totalParticipants)"
                                } else {
                                    stringResource(Res.string.watch_party_btn_start_playback)
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                } else {
                    // Участник: переключатель готовности
                    val readyShape = RoundedCornerShape(14.dp)
                    val isReady = state.myIsReady
                    val readyBgColor = if (isReady) Color(0xFF4CAF50).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.10f)
                    val readyBorderColor = if (isReady) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.25f)

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .tvAndWebHoverEffect(
                                scaleTarget = 1.03f,
                                shape = readyShape,
                                activeBorderWidth = 2.dp,
                                activeBorderColor = if (isReady) Color(0xFF4CAF50) else Color.White,
                                defaultBorderWidth = 1.dp,
                                defaultBorderColor = readyBorderColor,
                                onClick = actions.onToggleReady
                            )
                            .background(readyBgColor, readyShape),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isReady) {
                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${stringResource(Res.string.watch_party_ready_status_ready)} (${stringResource(Res.string.watch_party_waiting_host)})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF81C784)
                            )
                        } else {
                            Text(
                                text = stringResource(Res.string.watch_party_ready_check),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
