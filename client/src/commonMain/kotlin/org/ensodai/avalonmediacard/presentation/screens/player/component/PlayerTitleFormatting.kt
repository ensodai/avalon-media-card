package org.ensodai.avalonmediacard.presentation.screens.player.component

import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.plugins.MediaStream
import org.ensodai.avalonmediacard.presentation.screens.player.viewState.PlayerTitleData

fun getPlayerTitleTexts(
    mediaType: EntityType,
    seriesTitle: String?,
    title: String,
    currentEpisode: MediaStream?
): PlayerTitleData {
    val isMovie = mediaType == EntityType.MOVIE
    val season = currentEpisode?.seasonNumber
    val episode = currentEpisode?.episodeNumber
    val epNameRaw = currentEpisode?.episodeName ?: ""
    val cleanEpName = if (episode != null && epNameRaw.startsWith("$episode. ")) {
        epNameRaw.removePrefix("$episode. ").trim()
    } else {
        epNameRaw
    }

    val topText = if (isMovie) {
        ""
    } else {
        if (!seriesTitle.isNullOrBlank()) seriesTitle
        else if (!title.equals(cleanEpName, ignoreCase = true)) title
        else ""
    }

    val bottomText = if (isMovie) {
        if (!seriesTitle.isNullOrBlank()) seriesTitle
        else if (cleanEpName.isNotBlank()) cleanEpName
        else title
    } else if (season != null && episode != null) {
        val s = season.toString().padStart(2, '0')
        val e = episode.toString().padStart(2, '0')
        if (cleanEpName.isNotBlank()) {
            "S${s}E${e} • $cleanEpName"
        } else {
            "S${s}E${e}"
        }
    } else if (cleanEpName.isNotBlank()) {
        cleanEpName
    } else {
        title
    }

    return PlayerTitleData(topText, bottomText)
}
