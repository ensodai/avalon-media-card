package org.ensodai.avalonmediacard.plugins.torrserver.domain.model

import kotlinx.serialization.Serializable
import org.ensodai.avalonmediacard.contract.slot.ServerAction

@Serializable
data class SelectEpisodeCommand(
    val mediaId: String,
    val seasonNumber: Int,
    val episodeNumber: Int
) : ServerAction
