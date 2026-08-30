package org.ensodai.avalonmediacard.plugins.medialist

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.model.MediaType
import org.ensodai.avalonmediacard.contract.model.SidebarItem
import org.ensodai.avalonmediacard.contract.model.resolveTargetLanguage
import org.ensodai.avalonmediacard.contract.model.withUserSettings
import org.ensodai.avalonmediacard.contract.plugins.AvalonPlugin
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.slot.*
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen

class MediaListPlugin : AvalonPlugin {
    override val id: String = "org.ensodai.medialist"
    override val name: String = "Списки Медиа"
    override val version: String = "1.0.0"
    override val author: String = "Antigravity"

    override fun provideSerializers(): SerializersModule = SerializersModule {
        polymorphic(Action::class) {
            subclass(LoadMoreMediaList::class)
        }
        polymorphic(ServerAction::class) {
            subclass(LoadMoreMediaList::class)
        }
    }

    override fun onInitialize(context: PluginContext) {
        context.actions.bind<LoadMoreMediaList> { _, _ ->
            ActionResult.NoOp
        }

        context.sidebars.onSidebar { userId ->
            if (userId == null) return@onSidebar flowOf(emptyList())

            val customListsFlow = context.userCustomLists.observeUserLists(userId)
            val collectionCountFlow = context.userMovies.observeUserMovies(userId)
                .map { items -> items.count { it.inCollection } }
                .distinctUntilChanged()

            kotlinx.coroutines.flow.combine(customListsFlow, collectionCountFlow) { lists, collectionCount ->
                val collectionItem = SidebarItem(
                    itemId = "collection",
                    title = context.i18n.t("sidebar.collection"),
                    iconName = "heart",
                    screen = Screen.MyCollection,
                    group = 1,
                    order = 0,
                    itemsCount = collectionCount
                )

                val customListItems = lists.mapIndexed { index, customList ->
                    SidebarItem(
                        itemId = "custom_list_${customList.id}",
                        title = customList.name,
                        iconName = "list",
                        screen = Screen.CustomList(
                            listId = customList.id,
                            title = customList.name
                        ),
                        group = 2,
                        order = index,
                        itemsCount = customList.itemCount
                    )
                }
                listOf(collectionItem) + customListItems
            }
        }

        context.slots.declare<Screen.MyCollection>(listOf(SlotId.MediaGrid)) {
            listOf(org.ensodai.avalonmediacard.contract.slot.LayoutNode(id, SlotId.MediaGrid))
        }
        context.slots.onScreen<Screen.MyCollection> { _, userId ->
            if (userId == null) return@onScreen org.ensodai.avalonmediacard.contract.plugins.ScreenSlots(
                emptyList(),
                kotlinx.coroutines.flow.emptyFlow()
            )

            val flow = context.userMovies.observeUserMovies(userId)
                .map { items -> items.filter { it.inCollection } }
                .distinctUntilChanged { old, new -> old.map { it.mediaId } == new.map { it.mediaId } }
                .map { collectionItems ->
                    val userSettings = runCatching { context.userGlobalSettings.getUserSettings(userId) }.getOrNull()
                    val targetLang = userSettings.resolveTargetLanguage()
                    val keys = collectionItems.map { item ->
                        val entityType = if (item.mediaType == MediaType.MOVIE) {
                            EntityType.MOVIE
                        } else {
                            EntityType.TV
                        }
                        MediaKey(
                            provider = MediaProvider.Tmdb,
                            type = entityType,
                            id = item.mediaId
                        )
                    }
                    val detailsMap = context.catalog.getMediaDetailsBatch(
                        keys = keys,
                        requireSeasons = false,
                        requireVideos = false,
                        language = targetLang
                    )
                    val gridItems = keys.mapNotNull { key ->
                        val details = detailsMap[key] ?: return@mapNotNull null
                        val customized = details.withUserSettings(userSettings)
                        MovieCarouselItem(
                            key = key,
                            title = customized.title,
                            posterUrl = customized.posterUrl
                        )
                    }
                    SlotUpdate(
                        slotId = SlotId.MediaGrid,
                        nodeId = id,
                        state = SlotState.Content(
                            SlotData.Grid(
                                id = "collection",
                                items = gridItems
                            )
                        )
                    )
                }
                .onStart {
                    emit(
                        SlotUpdate(
                            slotId = SlotId.MediaGrid,
                            nodeId = id,
                            state = SlotState.Loading()
                        )
                    )
                }
            org.ensodai.avalonmediacard.contract.plugins.ScreenSlots(
                layout = listOf(org.ensodai.avalonmediacard.contract.slot.LayoutNode(id, SlotId.MediaGrid)),
                flow = flow.map { ScreenStreamEvent.Update(it) }
            )
        }

        context.slots.declare<Screen.CustomList>(listOf(SlotId.MediaGrid)) {
            listOf(org.ensodai.avalonmediacard.contract.slot.LayoutNode(id, SlotId.MediaGrid))
        }
        context.slots.onScreen<Screen.CustomList> { screen, userId ->
            if (userId == null) return@onScreen org.ensodai.avalonmediacard.contract.plugins.ScreenSlots(
                emptyList(),
                kotlinx.coroutines.flow.emptyFlow()
            )

            val listId = screen.listId
            val listIdStr = listId.toString()

            val flow = context.userCustomLists.observeListItems(listId)
                .distinctUntilChanged()
                .map { mediaKeys ->
                    val userSettings = runCatching { context.userGlobalSettings.getUserSettings(userId) }.getOrNull()
                    val targetLang = userSettings.resolveTargetLanguage()
                    val detailsMap = context.catalog.getMediaDetailsBatch(
                        keys = mediaKeys,
                        requireSeasons = false,
                        requireVideos = false,
                        language = targetLang
                    )
                    val gridItems = mediaKeys.mapNotNull { key ->
                        val details = detailsMap[key] ?: return@mapNotNull null
                        val customized = details.withUserSettings(userSettings)
                        MovieCarouselItem(
                            key = key,
                            title = customized.title,
                            posterUrl = customized.posterUrl
                        )
                    }
                    SlotUpdate(
                        slotId = SlotId.MediaGrid,
                        nodeId = id,
                        state = SlotState.Content(
                            SlotData.Grid(
                                id = "custom_list_$listIdStr",
                                items = gridItems
                            )
                        )
                    )
                }
                .onStart {
                    emit(
                        SlotUpdate(
                            slotId = SlotId.MediaGrid,
                            nodeId = id,
                            state = SlotState.Loading()
                        )
                    )
                }
            org.ensodai.avalonmediacard.contract.plugins.ScreenSlots(
                layout = listOf(org.ensodai.avalonmediacard.contract.slot.LayoutNode(id, SlotId.MediaGrid)),
                flow = flow.map { ScreenStreamEvent.Update(it) }
            )
        }
    }
}
