package org.ensodai.avalonmediacard.plugin.playback

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.UserEpisodeProgress
import org.ensodai.avalonmediacard.contract.slot.*
import kotlin.uuid.Uuid

class CorePlaybackSlotFactory(
    private val pluginId: String = "core",
    private val context: PluginContext
) {
    fun buildPlayButtonsFlow(key: MediaKey, userId: Uuid?, isTvShow: Boolean, locale: String = "ru"): Flow<SlotUpdate> {
        val isEn = locale.startsWith("en", ignoreCase = true)
        val moviesFlow = if (userId != null) {
            context.userMovies.observeUserMovies(userId)
        } else {
            flowOf(emptyList())
        }

        val bindingFlow = if (userId != null) {
            context.userMediaBindings.observeActiveBinding(userId, key.id)
        } else {
            flowOf(null)
        }

        return combine(bindingFlow, moviesFlow) { binding, userMovies ->

            if (binding == null) {
                return@combine SlotUpdate(
                    slotId = SlotId.PlayButtons,
                    nodeId = pluginId,
                    state = SlotState.Content(
                        SlotData.ButtonGroup(
                            buttons = listOf(
                                ButtonItem(
                                    label = if (isEn) "Watch Online" else "Смотреть онлайн",
                                    icon = IconType.PLAY,
                                    action = ActionOpenSources(key = key)
                                )
                            )
                        )
                    )
                )
            }


            if (key.type == EntityType.MOVIE || !isTvShow) {
                val userMovie = userMovies.find { it.mediaId == key.id }
                val hasProgress = userMovie != null && userMovie.progressSeconds > 0 && userMovie.status != MediaStatus.COMPLETED
                val buttonLabel = if (hasProgress) {
                    if (isEn) "Continue Watching" else "Продолжить просмотр"
                } else {
                    if (isEn) "Watch Movie" else "Смотреть фильм"
                }

                return@combine SlotUpdate(
                    slotId = SlotId.PlayButtons,
                    nodeId = pluginId,
                    state = SlotState.Content(
                        SlotData.ButtonGroup(
                            buttons = listOf(
                                ButtonItem(
                                    label = buttonLabel,
                                    icon = IconType.PLAY,
                                    action = ActionPreparePlayer(
                                        key = key,
                                        title = if (isEn) "Movie" else "Фильм"
                                    )
                                )
                            )
                        )
                    )
                )
            }

            // Для сериалов: мгновенный расчет текущей серии из локальной БД прогресса пользователя
            val userEpisodes = if (userId != null) {
                context.userEpisodes.getEpisodesProgress(userId, key.id)
            } else {
                emptyList()
            }

            var targetSeason: Int? = null
            var targetEpisode: Int? = null
            var hasActiveProgress = false

            if (userEpisodes.isNotEmpty()) {
                val recent = userEpisodes.filter { it.isWatched || it.progressSeconds > 0 }
                    .maxWithOrNull(
                        compareBy<UserEpisodeProgress> { it.lastWatchedAtEpochMs ?: 0L }
                            .thenBy { it.season }
                            .thenBy { it.episode }
                    )

                if (recent != null) {
                    val s = recent.season
                    val e = recent.episode
                    val duration = if (recent.durationSeconds > 0) recent.durationSeconds else Long.MAX_VALUE
                    val progress = recent.progressSeconds
                    hasActiveProgress = progress > 0L && progress < (duration * 0.95).toLong()

                    if (hasActiveProgress || !recent.isWatched) {
                        targetSeason = s
                        targetEpisode = e
                    } else {
                        targetSeason = s
                        targetEpisode = e + 1
                    }
                }
            }

            val buttonLabel = if (targetSeason != null && targetEpisode != null) {
                val prefix = if (hasActiveProgress) {
                    if (isEn) "Continue" else "Продолжить"
                } else {
                    if (isEn) "Watch" else "Смотреть"
                }
                val s = targetSeason.toString().padStart(2, '0')
                val e = targetEpisode.toString().padStart(2, '0')
                "$prefix S${s}E${e}"
            } else {
                if (isEn) "Watch Series" else "Смотреть сериал"
            }

            SlotUpdate(
                slotId = SlotId.PlayButtons,
                nodeId = pluginId,
                state = SlotState.Content(
                    SlotData.ButtonGroup(
                        buttons = listOf(
                            ButtonItem(
                                label = buttonLabel,
                                icon = IconType.PLAY,
                                action = ActionPreparePlayer(
                                    key = key,
                                    title = if (isEn) "Series" else "Сериал",
                                    targetSeason = targetSeason,
                                    targetEpisode = targetEpisode
                                )
                            )
                        )
                    )
                )
            )
        }
    }
}
