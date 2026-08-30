package org.ensodai.avalonmediacard.plugins.torrserver.domain.model

import kotlinx.serialization.Serializable
import org.ensodai.avalonmediacard.contract.slot.ServerAction

@Serializable
data class ResetMappingCommand(
    val mediaId: String
) : ServerAction
