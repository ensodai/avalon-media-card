package org.ensodai.avalonmediacard.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import org.ensodai.avalonmediacard.auth.ExternalCustomList
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.IntegrationService
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.plugins.CustomListInfo
import org.ensodai.avalonmediacard.contract.plugins.CustomListStatus
import org.ensodai.avalonmediacard.contract.plugins.UserCustomListProvider
import org.ensodai.avalonmediacard.database.MediaTable
import org.ensodai.avalonmediacard.database.UserCustomListItemTable
import org.ensodai.avalonmediacard.database.UserCustomListTable
import org.ensodai.avalonmediacard.database.dbQuery
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class CustomList(
    val id: Uuid,
    val userId: Uuid,
    val service: IntegrationService,
    val externalListId: String,
    val slug: String,
    val name: String,
    val privacy: String,
    val itemCount: Int,
    val isSynced: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CustomListItem(
    val id: Uuid,
    val listId: Uuid,
    val mediaId: Uuid,
    val mediaType: MediaType,
    val tmdbId: Int,
    val rank: Int,
    val listedAt: Instant?,
    val isSynced: Boolean
)

@Single(binds = [UserCustomListRepository::class, UserCustomListProvider::class])
class UserCustomListRepository : UserCustomListProvider {

    private val _updates = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val updates = _updates.asSharedFlow()

    override fun observeUserLists(userId: Uuid): Flow<List<CustomListInfo>> {
        return flow {
            emit(getListsForUser(userId).map { CustomListInfo(it.id, it.name, it.itemCount) })
            updates.collect {
                emit(getListsForUser(userId).map { CustomListInfo(it.id, it.name, it.itemCount) })
            }
        }
    }

    override fun observeListItems(listId: Uuid): Flow<List<MediaKey>> {
        return flow {
            val getKeys = suspend {
                getListItems(listId).map {
                    MediaKey(
                        provider = MediaProvider.Tmdb,
                        type = if (it.mediaType == MediaType.MOVIE) EntityType.MOVIE else EntityType.TV,
                        id = it.tmdbId.toString()
                    )
                }
            }
            emit(getKeys())
            updates.collect {
                emit(getKeys())
            }
        }
    }

    suspend fun getListsForUser(
        userId: Uuid,
        service: IntegrationService = IntegrationService.TRAKT
    ): List<CustomList> = dbQuery {
        UserCustomListTable.selectAll()
            .where { (UserCustomListTable.userId eq userId) and (UserCustomListTable.service eq service) }
            .orderBy(UserCustomListTable.createdAt to org.jetbrains.exposed.v1.core.SortOrder.DESC)
            .map { row ->
                CustomList(
                    id = row[UserCustomListTable.id].value,
                    userId = row[UserCustomListTable.userId],
                    service = row[UserCustomListTable.service],
                    externalListId = row[UserCustomListTable.externalListId],
                    slug = row[UserCustomListTable.slug],
                    name = row[UserCustomListTable.name],
                    privacy = row[UserCustomListTable.privacy],
                    itemCount = row[UserCustomListTable.itemCount],
                    isSynced = row[UserCustomListTable.isSynced],
                    createdAt = row[UserCustomListTable.createdAt],
                    updatedAt = row[UserCustomListTable.updatedAt]
                )
            }
    }

    suspend fun getListItems(listId: Uuid): List<CustomListItem> = dbQuery {
        (UserCustomListItemTable innerJoin MediaTable).selectAll()
            .where { UserCustomListItemTable.listId eq listId }
            .orderBy(UserCustomListItemTable.rank)
            .map { row ->
                CustomListItem(
                    id = row[UserCustomListItemTable.id].value,
                    listId = row[UserCustomListItemTable.listId],
                    mediaId = row[MediaTable.id].value,
                    mediaType = if (row[MediaTable.mediaType].equals(
                            "movie",
                            ignoreCase = true
                        )
                    ) MediaType.MOVIE else MediaType.TV,
                    tmdbId = row[MediaTable.externalId].toIntOrNull() ?: 0,
                    rank = row[UserCustomListItemTable.rank],
                    listedAt = row[UserCustomListItemTable.listedAt],
                    isSynced = row[UserCustomListItemTable.isSynced]
                )
            }
    }

    private fun resolveMediaId(tmdbId: Int, mediaType: MediaType): Uuid {
        val externalIdStr = tmdbId.toString()
        val mTypeStr = if (mediaType == MediaType.MOVIE) "movie" else "tv"
        val existing = MediaTable.selectAll().where {
            (MediaTable.catalogId eq "tmdb") and (MediaTable.externalId eq externalIdStr)
        }.firstOrNull()

        if (existing != null) {
            return existing[MediaTable.id].value
        }

        val newId = Uuid.random()
        MediaTable.insert {
            it[id] = newId
            it[catalogId] = "tmdb"
            it[externalId] = externalIdStr
            it[this.mediaType] = mTypeStr
        }
        return newId
    }

    suspend fun syncLists(userId: Uuid, service: IntegrationService, lists: List<ExternalCustomList>) {
        dbQuery {
            val localLists = UserCustomListTable.selectAll()
                .where { (UserCustomListTable.userId eq userId) and (UserCustomListTable.service eq service) }
                .map { row ->
                    CustomList(
                        id = row[UserCustomListTable.id].value,
                        userId = row[UserCustomListTable.userId],
                        service = row[UserCustomListTable.service],
                        externalListId = row[UserCustomListTable.externalListId],
                        slug = row[UserCustomListTable.slug],
                        name = row[UserCustomListTable.name],
                        privacy = row[UserCustomListTable.privacy],
                        itemCount = row[UserCustomListTable.itemCount],
                        isSynced = row[UserCustomListTable.isSynced],
                        createdAt = row[UserCustomListTable.createdAt],
                        updatedAt = row[UserCustomListTable.updatedAt]
                    )
                }

            val externalIdsInIncoming = lists.map { it.externalListId }.toSet()

            val now = Clock.System.now()
            val threshold = now - 5.minutes

            val listsToRemove = localLists.filter {
                it.isSynced &&
                        !externalIdsInIncoming.contains(it.externalListId) &&
                        it.updatedAt < threshold
            }
            for (listToRemove in listsToRemove) {
                UserCustomListItemTable.deleteWhere { UserCustomListItemTable.listId eq listToRemove.id }
                UserCustomListTable.deleteWhere { UserCustomListTable.id eq listToRemove.id }
            }

            for (incomingList in lists) {
                val existingLocalList = localLists.find { it.externalListId == incomingList.externalListId }

                val listId = if (existingLocalList != null) {
                    UserCustomListTable.update({ UserCustomListTable.id eq existingLocalList.id }) {
                        it[UserCustomListTable.name] = incomingList.name
                        it[UserCustomListTable.slug] = incomingList.slug
                        it[UserCustomListTable.privacy] = incomingList.privacy
                        it[UserCustomListTable.itemCount] = incomingList.items.size
                        it[UserCustomListTable.isSynced] = true
                        it[UserCustomListTable.updatedAt] = Clock.System.now()
                    }
                    existingLocalList.id
                } else {
                    val newId = Uuid.random()
                    UserCustomListTable.insert {
                        it[UserCustomListTable.id] = newId
                        it[UserCustomListTable.userId] = userId
                        it[UserCustomListTable.service] = service
                        it[UserCustomListTable.externalListId] = incomingList.externalListId
                        it[UserCustomListTable.slug] = incomingList.slug
                        it[UserCustomListTable.name] = incomingList.name
                        it[UserCustomListTable.privacy] = incomingList.privacy
                        it[UserCustomListTable.itemCount] = incomingList.items.size
                        it[UserCustomListTable.isSynced] = true
                        it[UserCustomListTable.createdAt] = Clock.System.now()
                        it[UserCustomListTable.updatedAt] = Clock.System.now()
                    }
                    newId
                }

                val localItems = (UserCustomListItemTable innerJoin MediaTable).selectAll()
                    .where { UserCustomListItemTable.listId eq listId }
                    .map { row ->
                        CustomListItem(
                            id = row[UserCustomListItemTable.id].value,
                            listId = row[UserCustomListItemTable.listId],
                            mediaId = row[MediaTable.id].value,
                            mediaType = if (row[MediaTable.mediaType].equals(
                                    "movie",
                                    ignoreCase = true
                                )
                            ) MediaType.MOVIE else MediaType.TV,
                            tmdbId = row[MediaTable.externalId].toIntOrNull() ?: 0,
                            rank = row[UserCustomListItemTable.rank],
                            listedAt = row[UserCustomListItemTable.listedAt],
                            isSynced = row[UserCustomListItemTable.isSynced]
                        )
                    }

                val incomingTmdbIds = incomingList.items.map { it.tmdbId }.toSet()

                val itemsToDelete = localItems.filter { localItem ->
                    localItem.isSynced &&
                            !incomingTmdbIds.contains(localItem.tmdbId)
                }

                for (itemToDelete in itemsToDelete) {
                    UserCustomListItemTable.deleteWhere {
                        (UserCustomListItemTable.listId eq listId) and (UserCustomListItemTable.id eq itemToDelete.id)
                    }
                }

                for (incomingItem in incomingList.items) {
                    val existingItem = localItems.find { it.tmdbId == incomingItem.tmdbId }
                    if (existingItem != null) {
                        if (existingItem.isSynced) {
                            UserCustomListItemTable.update({ UserCustomListItemTable.id eq existingItem.id }) {
                                it[UserCustomListItemTable.rank] = incomingItem.rank
                                it[UserCustomListItemTable.listedAt] = incomingItem.listedAt
                            }
                        }
                    } else {
                        val mediaId = resolveMediaId(incomingItem.tmdbId, incomingItem.mediaType)
                        UserCustomListItemTable.insert {
                            it[UserCustomListItemTable.id] = Uuid.random()
                            it[UserCustomListItemTable.listId] = listId
                            it[UserCustomListItemTable.mediaId] = mediaId
                            it[UserCustomListItemTable.rank] = incomingItem.rank
                            it[UserCustomListItemTable.listedAt] = incomingItem.listedAt
                            it[UserCustomListItemTable.isSynced] = true
                        }
                    }
                }
            }
        }
        _updates.emit(Unit)
    }

    suspend fun addListItem(listId: Uuid, mediaType: MediaType, tmdbId: Int) = dbQuery {
        val mediaId = resolveMediaId(tmdbId, mediaType)
        val exists = UserCustomListItemTable.selectAll()
            .where { (UserCustomListItemTable.listId eq listId) and (UserCustomListItemTable.mediaId eq mediaId) }
            .any()
        if (!exists) {
            val maxRank = UserCustomListItemTable.selectAll()
                .where { UserCustomListItemTable.listId eq listId }
                .map { it[UserCustomListItemTable.rank] }
                .maxOrNull() ?: 0

            UserCustomListItemTable.insert {
                it[UserCustomListItemTable.id] = Uuid.random()
                it[UserCustomListItemTable.listId] = listId
                it[UserCustomListItemTable.mediaId] = mediaId
                it[UserCustomListItemTable.rank] = maxRank + 1
                it[UserCustomListItemTable.listedAt] = Clock.System.now()
                it[UserCustomListItemTable.isSynced] = false
            }
        }
    }

    suspend fun removeListItem(listId: Uuid, mediaType: MediaType, tmdbId: Int) = dbQuery {
        val mediaId = resolveMediaId(tmdbId, mediaType)
        UserCustomListItemTable.deleteWhere {
            (UserCustomListItemTable.listId eq listId) and (UserCustomListItemTable.mediaId eq mediaId)
        }
    }

    suspend fun createCustomList(
        userId: Uuid,
        name: String,
        externalListId: String,
        slug: String,
        service: IntegrationService = IntegrationService.TRAKT,
        isSynced: Boolean = false
    ): Uuid {
        val listId = dbQuery {
            val id = Uuid.random()
            UserCustomListTable.insert {
                it[UserCustomListTable.id] = id
                it[UserCustomListTable.userId] = userId
                it[UserCustomListTable.service] = service
                it[UserCustomListTable.externalListId] = externalListId
                it[UserCustomListTable.slug] = slug
                it[UserCustomListTable.name] = name
                it[UserCustomListTable.privacy] = "private"
                it[UserCustomListTable.itemCount] = 0
                it[UserCustomListTable.isSynced] = isSynced
                it[UserCustomListTable.createdAt] = Clock.System.now()
                it[UserCustomListTable.updatedAt] = Clock.System.now()
            }
            id
        }
        _updates.emit(Unit)
        return listId
    }

    suspend fun markListSynced(listId: Uuid, externalListId: String, slug: String) = dbQuery {
        UserCustomListTable.update({ UserCustomListTable.id eq listId }) {
            it[UserCustomListTable.externalListId] = externalListId
            it[UserCustomListTable.slug] = slug
            it[UserCustomListTable.isSynced] = true
            it[UserCustomListTable.updatedAt] = Clock.System.now()
        }
    }

    suspend fun markListItemSynced(listId: Uuid, mediaType: MediaType, tmdbId: Int) = dbQuery {
        val mediaId = resolveMediaId(tmdbId, mediaType)
        UserCustomListItemTable.update({ (UserCustomListItemTable.listId eq listId) and (UserCustomListItemTable.mediaId eq mediaId) }) {
            it[UserCustomListItemTable.isSynced] = true
        }
    }

    override suspend fun getCustomListsWithStatus(userId: Uuid, mediaKey: MediaKey): List<CustomListStatus> {
        val lists = getListsForUser(userId)
        val tmdbId = mediaKey.id.toIntOrNull() ?: return emptyList()
        val statuses = mutableListOf<CustomListStatus>()

        for (list in lists) {
            val items = getListItems(list.id)
            val isAdded = items.any { it.tmdbId == tmdbId }
            statuses.add(CustomListStatus(list.id.toString(), list.name, isAdded))
        }
        return statuses
    }

    override suspend fun toggleList(userId: Uuid, listId: String, mediaKey: MediaKey) {
        val uuid = Uuid.parse(listId)
        val tmdbId = mediaKey.id.toIntOrNull() ?: return

        val mediaType = if (mediaKey.type.name.equals("MOVIE", ignoreCase = true)) MediaType.MOVIE else MediaType.TV

        val items = getListItems(uuid)
        val exists = items.any { it.tmdbId == tmdbId }

        if (exists) {
            removeListItem(uuid, mediaType, tmdbId)
        } else {
            addListItem(uuid, mediaType, tmdbId)
        }
        _updates.emit(Unit)
    }

    override suspend fun createList(userId: Uuid, listName: String, mediaKey: MediaKey) {
        val tmdbId = mediaKey.id.toIntOrNull() ?: return
        val mediaType = if (mediaKey.type.name.equals("MOVIE", ignoreCase = true)) MediaType.MOVIE else MediaType.TV

        val listId = createCustomList(
            userId = userId,
            name = listName,
            externalListId = "local_${Uuid.random()}",
            slug = listName.lowercase().replace(" ", "-"),
            service = IntegrationService.TRAKT,
            isSynced = false
        )
        addListItem(listId, mediaType, tmdbId)
        _updates.emit(Unit)
    }
}
