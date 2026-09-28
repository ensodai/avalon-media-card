package org.ensodai.avalonmediacard.presentation.screens.watchParty.viewState

import androidx.compose.runtime.Immutable
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.WatchParticipantIntent
import org.ensodai.avalonmediacard.contract.model.WatchRoomControlMode
import org.ensodai.avalonmediacard.contract.model.WatchRoomDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantDto
import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import org.ensodai.avalonmediacard.contract.model.WatchRoomSummaryDto
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.core.SduiSlot
import org.ensodai.avalonmediacard.presentation.core.mvi.BaseViewState
import kotlin.uuid.Uuid

enum class WatchPartyStep {
    ENTRY,  // Точка входа: Создать / Ввести PIN / Список комнат
    SETUP,  // Настройка: название, серия, режим управления, источник
    LOBBY   // Лобби: PIN, участники, статусы намерений, готовность, запуск
}

@Immutable
data class WatchPartyViewState(
    val step: WatchPartyStep = WatchPartyStep.ENTRY,
    val mediaKey: MediaKey? = null,
    val mediaTitle: String = "",
    val mediaSourcesList: List<SduiSlot<SlotData.MediaSources>> = emptyList(),
    val torrentInspectorSlot: SduiSlot<SlotData.TorrentInspector>? = null,

    // Шаг 1: Вход
    val savedRooms: List<WatchRoomSummaryDto> = emptyList(),
    val isLoadingSavedRooms: Boolean = false,
    val pinInput: String = "",
    val isJoining: Boolean = false,
    val joinError: String? = null,

    // Шаг 2: Создание и выбор источника
    val roomTitleInput: String = "",
    val selectedSeason: Int? = null,
    val selectedEpisode: Int? = null,
    val controlMode: WatchRoomControlMode = WatchRoomControlMode.HOST_ONLY,
    val isPrivate: Boolean = false,
    val selectedSourceType: String? = null,
    val selectedSourceId: String? = null,
    val selectedSourceName: String? = null,
    val isSelectingSource: Boolean = false,
    val isSourceVerified: Boolean = false,
    val isTestingSource: Boolean = false,
    val isCreatingRoom: Boolean = false,
    val createError: String? = null,

    // Шаг 3: Лобби (Snapshot + Upsert Item)
    val activeRoom: WatchRoomDto? = null,
    val participantsMap: Map<Uuid, WatchRoomParticipantDto> = emptyMap(),
    val myUserId: Uuid? = null,
    val isHost: Boolean = false,
    val myIntent: WatchParticipantIntent? = null,
    val myIsReady: Boolean = false,
    val isActionPending: Boolean = false,
    val isStartingPlayback: Boolean = false,
    val systemNotice: String? = null
) : BaseViewState() {
    val participants: List<WatchRoomParticipantDto>
        get() = participantsMap.values.sortedByDescending { it.role == WatchRoomParticipantRole.HOST }
}
