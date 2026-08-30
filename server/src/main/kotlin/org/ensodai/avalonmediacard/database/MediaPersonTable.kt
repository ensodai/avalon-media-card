package org.ensodai.avalonmediacard.database

object MediaPersonTable : BaseUuidTable("people") {
    val personId = varchar("person_id", 100).uniqueIndex() // ID из TMDB
}