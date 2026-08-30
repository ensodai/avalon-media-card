package org.ensodai.avalonmediacard.database

import org.ensodai.avalonmediacard.contract.model.ClickstreamContext
import org.ensodai.avalonmediacard.contract.model.ClickstreamEventType
import org.ensodai.avalonmediacard.contract.model.ClickstreamTargetType

object UserClickstreamTable : BaseUuidTable("user_clickstream") {
    val userId = uuid("user_id").index()
    val eventType = enumerationByName("event_type", 50, ClickstreamEventType::class)
    val targetType = enumerationByName("target_type", 50, ClickstreamTargetType::class).nullable()
    val targetId = varchar("target_id", 512).nullable()
    val context = enumerationByName("context", 50, ClickstreamContext::class)
    val dwellTimeMs = long("dwell_time_ms").default(0)
    val payload = text("payload").nullable()
}
