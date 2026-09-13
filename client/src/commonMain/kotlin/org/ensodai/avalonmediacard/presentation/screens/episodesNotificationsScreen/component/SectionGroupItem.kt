package org.ensodai.avalonmediacard.presentation.screens.episodesNotificationsScreen.component

import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.NotificationType
import org.ensodai.avalonmediacard.contract.slot.NewEpisodeCardItem

sealed interface SectionGroupItem {
    data class Show(
        val mediaKey: MediaKey,
        val title: String,
        val posterUrl: String?,
        val episodes: List<NewEpisodeCardItem>
    ) : SectionGroupItem

    data class Movie(
        val item: NewEpisodeCardItem
    ) : SectionGroupItem
}

fun groupEpisodesByShow(episodes: List<NewEpisodeCardItem>): List<SectionGroupItem> {
    val result = mutableListOf<SectionGroupItem>()
    val seenShowKeys = mutableSetOf<MediaKey>()
    val episodesByShow = episodes
        .filter { it.notificationType != NotificationType.MOVIE_RELEASE }
        .groupBy { it.mediaKey }

    for (item in episodes) {
        if (item.notificationType == NotificationType.MOVIE_RELEASE) {
            result.add(SectionGroupItem.Movie(item))
        } else {
            if (item.mediaKey !in seenShowKeys) {
                seenShowKeys.add(item.mediaKey)
                val showEpisodes = episodesByShow[item.mediaKey].orEmpty()
                result.add(
                    SectionGroupItem.Show(
                        mediaKey = item.mediaKey,
                        title = item.showTitle,
                        posterUrl = item.showPosterUrl,
                        episodes = showEpisodes
                    )
                )
            }
        }
    }
    return result
}
