package org.ensodai.avalonmediacard.plugins.mediadetails.useractions

import kotlinx.serialization.Serializable
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.slot.ServerAction

@Serializable
data class RetryLoadMediaDetailsCommand(
    val key: MediaKey
) : ServerAction
