package org.ensodai.avalonmediacard.plugins.mediadetails.useractions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.UserMovieItem
import org.ensodai.avalonmediacard.contract.model.MediaCatalog
import org.ensodai.avalonmediacard.contract.model.UserEpisodeItem
import org.ensodai.avalonmediacard.contract.plugins.CustomListStatus
import org.ensodai.avalonmediacard.contract.plugins.UserCustomListProvider
import org.ensodai.avalonmediacard.contract.plugins.UserMovieProvider
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.uuid.Uuid

data class UserMediaState(
    val item: UserMovieItem? = null,
    val customLists: List<CustomListStatus> = emptyList(),
    val isLoading: Boolean = true
)

class UserActionsStateManager(
    private val scope: CoroutineScope,
    private val userMovieProvider: UserMovieProvider,
    private val userCustomListProvider: UserCustomListProvider,
    private val catalog: MediaCatalog
) {
    // Map: UserId + MediaId -> UserMediaState
    private val statesMap = ConcurrentHashMap<String, MutableStateFlow<UserMediaState>>()
    fun getStateFlow(userId: Uuid, key: MediaKey): StateFlow<UserMediaState> {
        return statesMap.getOrPut(getStateKey(userId, key)) { MutableStateFlow(UserMediaState(isLoading = true)) }
    }

    private fun getStateKey(userId: Uuid, key: MediaKey) = "${userId}-${key.id}"

    fun loadInitial(userId: Uuid, key: MediaKey) {
        val stateKey = getStateKey(userId, key)
        val flow = statesMap.getOrPut(stateKey) { MutableStateFlow(UserMediaState(isLoading = true)) }
        if (!flow.value.isLoading && flow.value.item != null) return

        scope.launch {
            try {
                val movies = userMovieProvider.getUserMovies(userId)
                val expectedType =
                    if (key.type == EntityType.MOVIE) MediaType.MOVIE else MediaType.TV
                val item = movies.find { it.mediaId == key.id && it.mediaType == expectedType }

                val lists = userCustomListProvider.getCustomListsWithStatus(userId, key)

                flow.value = UserMediaState(item = item, customLists = lists, isLoading = false)
            } catch (e: Exception) {
                flow.value = flow.value.copy(isLoading = false)
            }
        }
    }

    fun toggleCollection(userId: Uuid, key: MediaKey, inCollection: Boolean) {
        val stateKey = getStateKey(userId, key)
        val flow = statesMap.getOrPut(stateKey) { MutableStateFlow(UserMediaState()) }
        val currentState: UserMediaState? = flow.value
        val currentItem = currentState?.item

        val updated = currentItem?.copy(
            inCollection = inCollection,
            mediaType = if (key.type == EntityType.MOVIE) MediaType.MOVIE else MediaType.TV
        )
            ?: UserMovieItem(
                id = Uuid.random(),
                userId = userId,
                catalogId = key.provider.id.lowercase(),
                mediaId = key.id,
                mediaType = if (key.type == EntityType.MOVIE) MediaType.MOVIE else MediaType.TV,
                status = MediaStatus.NONE,
                inCollection = inCollection,
                lastWatchedAt = Clock.System.now()
            )

        flow.value =
            currentState?.copy(item = updated, isLoading = false) ?: UserMediaState(item = updated, isLoading = false)

        scope.launch {
            try {
                userMovieProvider.updateUserMovie(updated)
            } catch (e: Exception) {
                // Revert or log
            }
        }
    }

    fun setRating(userId: Uuid, key: MediaKey, rating: Int) {
        val stateKey = getStateKey(userId, key)
        val flow = statesMap.getOrPut(stateKey) { MutableStateFlow(UserMediaState()) }
        val currentState: UserMediaState? = flow.value
        val currentItem = currentState?.item

        val updated = currentItem?.copy(
            userRating = rating,
            mediaType = if (key.type == EntityType.MOVIE) MediaType.MOVIE else MediaType.TV
        )
            ?: UserMovieItem(
                id = Uuid.random(),
                userId = userId,
                catalogId = key.provider.id.lowercase(),
                mediaId = key.id,
                mediaType = if (key.type == EntityType.MOVIE) MediaType.MOVIE else MediaType.TV,
                status = MediaStatus.NONE,
                userRating = rating,
                lastWatchedAt = Clock.System.now()
            )

        flow.value =
            currentState?.copy(item = updated, isLoading = false) ?: UserMediaState(item = updated, isLoading = false)

        scope.launch {
            try {
                userMovieProvider.updateUserMovie(updated)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun setStatus(userId: Uuid, key: MediaKey, status: MediaStatus) {
        val stateKey = getStateKey(userId, key)
        val flow = statesMap.getOrPut(stateKey) { MutableStateFlow(UserMediaState()) }
        val currentState: UserMediaState? = flow.value
        val currentItem = currentState?.item

        val updated = currentItem?.copy(
            status = status,
            mediaType = if (key.type == EntityType.MOVIE) MediaType.MOVIE else MediaType.TV
        )
            ?: UserMovieItem(
                id = Uuid.random(),
                userId = userId,
                catalogId = key.provider.id.lowercase(),
                mediaId = key.id,
                mediaType = if (key.type == EntityType.MOVIE) MediaType.MOVIE else MediaType.TV,
                status = status,
                lastWatchedAt = Clock.System.now()
            )

        flow.value =
            currentState?.copy(item = updated, isLoading = false) ?: UserMediaState(item = updated, isLoading = false)

        scope.launch {
            try {
                userMovieProvider.updateUserMovie(updated)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun toggleCustomList(userId: Uuid, key: MediaKey, listId: String) {
        val stateKey = getStateKey(userId, key)
        val flow = statesMap.getOrPut(stateKey) { MutableStateFlow(UserMediaState()) }
        val current: UserMediaState? = flow.value

        // Optimistic UI update
        if (current != null) {
            val updatedLists = current.customLists.map {
                if (it.id == listId) it.copy(isAdded = !it.isAdded) else it
            }
            flow.value = current.copy(customLists = updatedLists)
        }

        scope.launch {
            try {
                userCustomListProvider.toggleList(userId, listId, key)
                // Reload lists to sync with server
                val lists = userCustomListProvider.getCustomListsWithStatus(userId, key)
                val newCurrent = flow.value
                flow.value = newCurrent.copy(customLists = lists)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun createCustomList(userId: Uuid, key: MediaKey, listName: String) {
        val stateKey = getStateKey(userId, key)
        val flow = statesMap.getOrPut(stateKey) { MutableStateFlow(UserMediaState()) }
        val current: UserMediaState? = flow.value
        if (current == null) return

        // Optimistic UI update
        val optimisticList = CustomListStatus(
            id = "temp_${Uuid.random()}",
            name = listName,
            isAdded = true
        )
        flow.value = current.copy(customLists = current.customLists + optimisticList)

        scope.launch {
            try {
                userCustomListProvider.createList(userId, listName, key)
                val lists = userCustomListProvider.getCustomListsWithStatus(userId, key)
                val newCurrent = flow.value
                if (newCurrent != null) {
                    flow.value = newCurrent.copy(customLists = lists)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun toggleEpisodeWatched(userId: Uuid, key: MediaKey, seasonNumber: Int, episodeNumber: Int, isWatched: Boolean) {
        scope.launch {
            try {
                // Here we fetch existing episode or create new one
                val episodes = userMovieProvider.getUserEpisodes(userId, key.id)
                val existing = episodes.find { it.season == seasonNumber && it.episode == episodeNumber }

                val item = existing?.copy(isWatched = isWatched, lastWatchedAt = kotlin.time.Clock.System.now())
                    ?: UserEpisodeItem(
                        id = Uuid.random(),
                        userId = userId,
                        catalogId = key.provider.id.lowercase(),
                        mediaId = key.id,
                        season = seasonNumber,
                        episode = episodeNumber,
                        isWatched = isWatched,
                        lastWatchedAt = Clock.System.now()
                    )

                userMovieProvider.updateUserEpisode(item)

                // If it's watched, ensure the main show status is at least WATCHING
                if (isWatched) {
                    val movies = userMovieProvider.getUserMovies(userId)
                    val expectedType =
                        if (key.type == EntityType.MOVIE) MediaType.MOVIE else MediaType.TV
                    val show = movies.find { it.mediaId == key.id && it.mediaType == expectedType }
                    if (show == null || show.status == MediaStatus.NONE) {
                        setStatus(userId, key, MediaStatus.WATCHING)
                    }
                }
                userMovieProvider.notifyUpdate()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun rateEpisode(userId: Uuid, key: MediaKey, seasonNumber: Int, episodeNumber: Int, rating: Int) {
        scope.launch {
            try {
                val episodes = userMovieProvider.getUserEpisodes(userId, key.id)
                val existing = episodes.find { it.season == seasonNumber && it.episode == episodeNumber }

                val item = existing?.copy(userRating = rating, lastWatchedAt = kotlin.time.Clock.System.now())
                    ?: UserEpisodeItem(
                        id = Uuid.random(),
                        userId = userId,
                        catalogId = key.provider.id.lowercase(),
                        mediaId = key.id,
                        season = seasonNumber,
                        episode = episodeNumber,
                        userRating = rating,
                        lastWatchedAt = Clock.System.now()
                    )

                userMovieProvider.updateUserEpisode(item)
                userMovieProvider.notifyUpdate()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun markSeasonWatched(userId: Uuid, key: MediaKey, seasonNumber: Int, isWatched: Boolean) {
        scope.launch {
            try {
                val episodes = catalog.getSeasonDetails(key, seasonNumber)
                val existingEpisodes = userMovieProvider.getUserEpisodes(userId, key.id)

                episodes.forEach { ep ->
                    val item = existingEpisodes.find { it.season == seasonNumber && it.episode == ep.episodeNumber }
                        ?.copy(
                            isWatched = isWatched,
                            lastWatchedAt = kotlin.time.Clock.System.now()
                        )
                        ?: UserEpisodeItem(
                            id = Uuid.random(),
                            userId = userId,
                            catalogId = key.provider.id.lowercase(),
                            mediaId = key.id,
                            season = seasonNumber,
                            episode = ep.episodeNumber,
                            isWatched = isWatched,
                            inCollection = false,
                            lastWatchedAt = Clock.System.now()
                        )
                    userMovieProvider.updateUserEpisode(item)
                }

                // If watched, also auto-update show status to WATCHING if needed
                if (isWatched) {
                    val userMovie = userMovieProvider.getUserMovies(userId)
                        .find { it.mediaId == key.id && it.mediaType == MediaType.TV }
                    if (userMovie == null || userMovie.status == MediaStatus.NONE) {
                        userMovieProvider.updateUserMovie(
                            (userMovie ?: UserMovieItem(
                                id = Uuid.random(),
                                userId = userId,
                                catalogId = key.provider.id.lowercase(),
                                mediaId = key.id,
                                mediaType = MediaType.TV,
                                status = MediaStatus.NONE,
                                lastWatchedAt = Clock.System.now()
                            )).copy(
                                status = MediaStatus.WATCHING,
                                lastWatchedAt = kotlin.time.Clock.System.now()
                            )
                        )
                    }
                }

                userMovieProvider.notifyUpdate()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
