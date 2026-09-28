package org.ensodai.avalonmediacard.presentation.screens.watchParty.action

import org.ensodai.avalonmediacard.contract.model.WatchParticipantIntent
import org.ensodai.avalonmediacard.contract.model.WatchRoomControlMode
import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.presentation.core.mvi.BaseActions
import org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState.WatchPartyStep
import kotlin.uuid.Uuid

data class WatchPartyActions(
    val onSetStep: (WatchPartyStep) -> Unit,
    val onPinChanged: (String) -> Unit,
    val onJoinByPin: () -> Unit,
    val onJoinById: (Uuid) -> Unit,
    val onRoomTitleChanged: (String) -> Unit,
    val onControlModeChanged: (WatchRoomControlMode) -> Unit,
    val onSeasonEpisodeChanged: (season: Int?, episode: Int?) -> Unit,
    val onSelectSource: (sourceType: String, sourceId: String, sourceName: String, seasonNumber: Int?, episodeNumber: Int?) -> Unit,
    val onToggleSelectSource: (Boolean) -> Unit,
    val onTestSource: () -> Unit,
    val onTestSourceVerified: (Boolean) -> Unit,
    val onTestSourceCancel: () -> Unit,
    val onCreateRoom: () -> Unit,
    val onSetMyIntent: (WatchParticipantIntent) -> Unit,
    val onToggleReady: () -> Unit,
    val onStartPlayback: () -> Unit,
    val onLeaveRoom: () -> Unit,
    val onRefreshSources: () -> Unit,
    val onExecuteServerAction: (ServerAction) -> Unit,
    val onClose: () -> Unit
) : BaseActions()
