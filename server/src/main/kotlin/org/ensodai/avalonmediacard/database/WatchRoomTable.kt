package org.ensodai.avalonmediacard.database

import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.WatchRoomControlMode
import org.ensodai.avalonmediacard.contract.model.WatchRoomStatus
import org.jetbrains.exposed.v1.core.ReferenceOption

object WatchRoomTable : BaseUuidTable("watch_rooms") {
    val title = varchar("title", 200)
    val mediaId = reference("media_id", MediaTable, onDelete = ReferenceOption.CASCADE)
    val mediaType = enumerationByName("media_type", 20, MediaType::class)
    val currentSeason = integer("current_season").nullable()
    val currentEpisode = integer("current_episode").nullable()
    val lastPositionSeconds = long("last_position_seconds").default(0L)
    val hostUserId = uuid("host_user_id").references(UserTable.id, onDelete = ReferenceOption.CASCADE)
    val sourceType = varchar("source_type", 64).nullable()
    val sourceId = text("source_id").nullable()
    val joinPin = varchar("join_pin", 10).nullable().index()
    val controlMode = enumerationByName("control_mode", 20, WatchRoomControlMode::class).default(WatchRoomControlMode.HOST_ONLY)
    val status = enumerationByName("status", 20, WatchRoomStatus::class).default(WatchRoomStatus.ACTIVE)
    val isPrivate = bool("is_private").default(false)

    init {
        index("watch_rooms_media_status", false, mediaId, status)
    }
}
