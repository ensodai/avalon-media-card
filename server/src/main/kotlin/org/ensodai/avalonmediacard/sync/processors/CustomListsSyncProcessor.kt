package org.ensodai.avalonmediacard.sync.processors

import org.ensodai.avalonmediacard.repository.UserCustomListRepository
import org.koin.core.annotation.Single
import org.slf4j.LoggerFactory

@Single
class CustomListsSyncProcessor(
    private val userCustomListRepository: UserCustomListRepository
) {
    private val logger = LoggerFactory.getLogger(CustomListsSyncProcessor::class.java)

    suspend fun sync(context: SyncContext) {
        if (!context.settings.syncLists) return
        val userId = context.userId

        // 1. Пушим несинхронизированные локальные списки в Trakt
        val localLists = userCustomListRepository.getListsForUser(userId, context.service)
        val unsyncedLists = localLists.filter { !it.isSynced }
        logger.info("Found ${unsyncedLists.size} unsynced custom lists for user $userId")

        for (list in unsyncedLists) {
            try {
                val createdList = context.syncProvider.createCustomList(
                    context.accessToken,
                    list.name,
                    list.privacy
                )
                if (createdList != null) {
                    userCustomListRepository.markListSynced(list.id, createdList.externalListId, createdList.slug)
                    logger.info("Successfully pushed custom list '${list.name}' to Trakt: externalId=${createdList.externalListId}")
                }
            } catch (e: Exception) {
                logger.error("Failed to push custom list '${list.name}' to Trakt", e)
            }
        }

        // 2. Пушим несинхронизированные элементы списков в Trakt
        val updatedLists = userCustomListRepository.getListsForUser(userId, context.service)
        for (list in updatedLists) {
            if (!list.isSynced) continue
            val items = userCustomListRepository.getListItems(list.id)
            val unsyncedItems = items.filter { !it.isSynced }
            if (unsyncedItems.isNotEmpty()) {
                logger.info("Found ${unsyncedItems.size} unsynced items in list '${list.name}'")
                for (item in unsyncedItems) {
                    try {
                        val pushed = context.syncProvider.addMediaToList(
                            context.accessToken,
                            list.externalListId, // Передаем ID списка
                            item.mediaType,
                            item.tmdbId
                        )
                        if (pushed) {
                            userCustomListRepository.markListItemSynced(list.id, item.mediaType, item.tmdbId)
                            logger.info("Successfully pushed item ${item.tmdbId} to Trakt list '${list.name}'")
                        }
                    } catch (e: Exception) {
                        logger.error("Failed to push item ${item.tmdbId} to Trakt list '${list.name}'", e)
                    }
                }
            }
        }

        // 3. Скачиваем актуальные списки из Trakt и мерджим их локально
        logger.info("Fetching custom lists from Trakt for user $userId")
        val externalLists = context.syncProvider.fetchUserLists(context.accessToken)
        userCustomListRepository.syncLists(userId, context.service, externalLists)
        logger.info("Successfully pulled ${externalLists.size} custom lists from Trakt for user $userId")
    }
}
