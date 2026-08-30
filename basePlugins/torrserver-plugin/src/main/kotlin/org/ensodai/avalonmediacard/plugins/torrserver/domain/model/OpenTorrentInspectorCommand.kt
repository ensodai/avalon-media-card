package org.ensodai.avalonmediacard.plugins.torrserver.domain.model

import kotlinx.serialization.Serializable
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.slot.ServerAction

@Serializable
data class OpenTorrentInspectorCommand(
    val url: String,
    val title: String,
    val mediaId: String? = null,
    val mediaType: EntityType? = null
) : ServerAction
