package org.ensodai.avalonmediacard.database

import org.ensodai.avalonmediacard.contract.model.WatchRoomParticipantRole
import org.jetbrains.exposed.v1.core.ReferenceOption

object WatchRoomParticipantTable : BaseUuidTable("watch_room_participants") {
    val roomId = reference("room_id", WatchRoomTable, onDelete = ReferenceOption.CASCADE)
    val userId = uuid("user_id").references(UserTable.id, onDelete = ReferenceOption.CASCADE)
    val role = enumerationByName("role", 20, WatchRoomParticipantRole::class).default(WatchRoomParticipantRole.MEMBER)

    init {
        uniqueIndex("watch_room_participant_unique", roomId, userId)
        index("watch_room_participants_user_id", false, userId)
    }
}
