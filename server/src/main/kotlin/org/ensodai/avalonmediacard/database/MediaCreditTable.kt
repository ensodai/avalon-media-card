package org.ensodai.avalonmediacard.database

object MediaCreditTable : BaseUuidTable("credits") {
    val mediaId = reference("media_id", MediaTable)
    val personId = reference("person_id", MediaPersonTable)
    val role = varchar("role", 50) // "Actor", "Director"
}
