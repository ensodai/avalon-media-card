package org.ensodai.avalonmediacard.plugins.torrserver.domain.model

import kotlinx.serialization.Serializable
import org.ensodai.avalonmediacard.contract.slot.TemplateAction

@Serializable
data class RemapTorrentFileCommand(
    val mediaId: String,
    val hash: String,
    val filePath: String = "",
    val season: Int = 0,
    val episode: Int = 0,
    val fileIndex: Int? = null,
    val fileSize: Long? = null
) : TemplateAction {
    override fun withParameter(key: String, value: Any): TemplateAction {
        return when (key) {
            "filePath" -> copy(filePath = value.toString())
            "season" -> copy(season = value.toString().toIntOrNull() ?: season)
            "episode" -> copy(episode = value.toString().toIntOrNull() ?: episode)
            else -> this
        }
    }
}