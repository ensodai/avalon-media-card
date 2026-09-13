package org.ensodai.avalonmediacard.plugins.episodes

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaStatus
import org.ensodai.avalonmediacard.contract.model.NotificationType
import org.ensodai.avalonmediacard.contract.model.SidebarItem
import org.ensodai.avalonmediacard.contract.model.SidebarItemType
import org.ensodai.avalonmediacard.contract.model.resolveTargetLanguage
import org.ensodai.avalonmediacard.contract.plugins.AvalonPlugin
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.plugins.ScreenSlots
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.ActionNavigate
import org.ensodai.avalonmediacard.contract.slot.ActionPreparePlayer
import org.ensodai.avalonmediacard.contract.slot.ActionResult
import org.ensodai.avalonmediacard.contract.slot.EpisodesSection
import org.ensodai.avalonmediacard.contract.slot.LayoutNode
import org.ensodai.avalonmediacard.contract.slot.MarkNotificationsReadCommand
import org.ensodai.avalonmediacard.contract.slot.MissedShowRollupItem
import org.ensodai.avalonmediacard.contract.slot.NewEpisodeCardItem
import org.ensodai.avalonmediacard.contract.slot.ScreenStreamEvent
import org.ensodai.avalonmediacard.contract.slot.ServerAction
import org.ensodai.avalonmediacard.contract.slot.SetStatusCommand
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.contract.slot.SlotId
import org.ensodai.avalonmediacard.contract.slot.SlotState
import org.ensodai.avalonmediacard.contract.slot.SlotUpdate
import org.ensodai.avalonmediacard.contract.slot.ToggleEpisodeWatchedCommand
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.uuid.Uuid

class EpisodesNotificationPlugin : AvalonPlugin {
    override val id: String = "org.ensodai.episodesnotification"
    override val name: String = "Episodes Notifications"
    override val version: String = "1.0.0"
    override val author: String = "Antigravity"

    override fun provideSerializers(): SerializersModule = SerializersModule {
        polymorphic(Action::class) {
            subclass(MarkNotificationsReadCommand::class)
            subclass(SetStatusCommand::class)
            subclass(ToggleEpisodeWatchedCommand::class)
            subclass(ActionPreparePlayer::class)
        }
        polymorphic(ServerAction::class) {
            subclass(MarkNotificationsReadCommand::class)
            subclass(SetStatusCommand::class)
            subclass(ToggleEpisodeWatchedCommand::class)
        }
    }

    override fun onInitialize(context: PluginContext) {
        val slotNodeId = "episodes_feed_node"

        // 1. Обработка команды сброса прочитанности уведомлений
        context.actions.bind<MarkNotificationsReadCommand> { _, userId ->
            if (userId != null) {
                context.episodeNotifications.markAllAsRead(userId)
            }
            ActionResult.NoOp
        }

        // 2. Регистрация элемента сайдбара с динамическим бейджем
        context.sidebars.onSidebar { userId ->
            if (userId == null) {
                return@onSidebar flow {
                    emit(
                        listOf(
                            SidebarItem(
                                itemId = "episodes_notifications",
                                title = context.i18n.t("sidebar.episodes_notifications"),
                                iconName = "bell",
                                screen = Screen.EpisodesNotifications,
                                type = SidebarItemType.MENU_ITEM,
                                group = 0,
                                order = 5,
                                itemsCount = null
                            )
                        )
                    )
                }
            }

            context.episodeNotifications.observeUnreadCount(userId)
                .map { unreadCount ->
                    listOf(
                        SidebarItem(
                            itemId = "episodes_notifications",
                            title = context.i18n.t("sidebar.episodes_notifications"),
                            iconName = "bell",
                            screen = Screen.EpisodesNotifications,
                            type = SidebarItemType.MENU_ITEM,
                            group = 0,
                            order = 5,
                            itemsCount = if (unreadCount > 0) unreadCount else null
                        )
                    )
                }
                .distinctUntilChanged()
        }

        // 3. Регистрация манифеста и слотов для экрана Screen.EpisodesNotifications
        context.slots.declare<Screen.EpisodesNotifications>(
            slots = listOf(SlotId.EpisodesFeed),
            manifestLayout = {
                listOf(LayoutNode(nodeId = slotNodeId, slotId = SlotId.EpisodesFeed))
            }
        )

        context.slots.onScreen<Screen.EpisodesNotifications> { _, userId ->
            if (userId == null) {
                return@onScreen ScreenSlots(
                    layout = listOf(LayoutNode(nodeId = slotNodeId, slotId = SlotId.EpisodesFeed)),
                    flow = flowOf(
                        ScreenStreamEvent.Update(
                            SlotUpdate(
                                slotId = SlotId.EpisodesFeed,
                                nodeId = slotNodeId,
                                state = SlotState.Content(
                                    SlotData.EpisodesFeed(
                                        sections = emptyList(),
                                        rollups = emptyList(),
                                        totalUnreadCount = 0,
                                        markAllReadAction = null
                                    )
                                )
                            )
                        )
                    )
                )
            }

            val feedFlow = merge(
                context.episodeNotifications.observeUnreadCount(userId),
                context.userMovies.observeUserMovies(userId)
            ).map {
                val feedData = buildEpisodesFeed(context, userId)
                ScreenStreamEvent.Update(
                    SlotUpdate(
                        slotId = SlotId.EpisodesFeed,
                        nodeId = slotNodeId,
                        state = SlotState.Content(feedData)
                    )
                )
            }

            ScreenSlots(
                layout = listOf(LayoutNode(nodeId = slotNodeId, slotId = SlotId.EpisodesFeed)),
                flow = feedFlow
            )
        }
    }

    private suspend fun buildEpisodesFeed(
        context: PluginContext,
        userId: Uuid
    ): SlotData.EpisodesFeed {
        val notifications = context.episodeNotifications.getNotifications(userId)

        val now = Clock.System.now()
        val todayStr = now.toString().substringBefore("T")
        val sevenDaysAgoStr = (now - 7.days).toString().substringBefore("T")
        val thirtyDaysAgoStr = (now - 30.days).toString().substringBefore("T")

        val todayEpisodes = mutableListOf<NewEpisodeCardItem>()
        val thisWeekEpisodes = mutableListOf<NewEpisodeCardItem>()
        val earlierEpisodes = mutableListOf<NewEpisodeCardItem>()
        val rollups = mutableListOf<MissedShowRollupItem>()

        val userLang = runCatching {
            context.userGlobalSettings.getUserSettings(userId).resolveTargetLanguage()
        }.getOrDefault("ru")

        // Группируем по сериалу для формирования Smart Rollup при накоплении пропущенных серий
        val showEpisodesMap = notifications.groupBy { it.mediaKey }

        for ((key, showNotifications) in showEpisodesMap) {
            val showOlderEpisodes = mutableListOf<NewEpisodeCardItem>()
            val isMovie = key.type == EntityType.MOVIE

            val mediaDetails = runCatching {
                context.catalog.getMediaDetails(key, requireSeasons = !isMovie, requireVideos = false, language = userLang)
            }.getOrNull()

            val showTitle = mediaDetails?.title?.takeIf { it.isNotBlank() } ?: key.id
            val showPosterUrl = mediaDetails?.posterUrl

            val progressMap = if (!isMovie) {
                runCatching {
                    context.userEpisodes.getEpisodesProgress(userId, key.id, key.provider.id.lowercase())
                }.getOrDefault(emptyList()).associateBy { "${it.season}_${it.episode}" }
            } else emptyMap()

            val seasonEpisodesMap = if (!isMovie) {
                val distinctSeasons = showNotifications.mapNotNull { it.seasonNumber }.distinct()
                distinctSeasons.flatMap { sNum ->
                    runCatching {
                        context.catalog.getSeasonDetails(key, sNum, language = userLang)
                    }.getOrDefault(emptyList()).map { sNum to it }
                }.associateBy { (sNum, ep) -> "${sNum}_${ep.episodeNumber}" }
            } else emptyMap()

            for (item in showNotifications) {
                val itemIsMovie = isMovie || item.notificationType == NotificationType.MOVIE_RELEASE
                val epProgress = progressMap["${item.seasonNumber}_${item.episodeNumber}"]
                val isWatched = epProgress?.isWatched ?: false
                val userRating = epProgress?.userRating

                val epDetail = seasonEpisodesMap["${item.seasonNumber}_${item.episodeNumber}"]?.second

                val episodeTitle = if (itemIsMovie) {
                    context.i18n.t("feed.movie_premiere")
                } else {
                    epDetail?.name?.takeIf { it.isNotBlank() }
                }

                val stillUrl = if (itemIsMovie) {
                    mediaDetails?.backgroundUrl ?: mediaDetails?.posterUrl
                } else {
                    epDetail?.stillUrl ?: mediaDetails?.backgroundUrl
                }

                val overview = if (itemIsMovie) {
                    mediaDetails?.description
                } else {
                    epDetail?.overview?.takeIf { it.isNotBlank() } ?: mediaDetails?.description
                }

                val durationMinutes = if (itemIsMovie) {
                    mediaDetails?.runtime
                } else {
                    epDetail?.runtime ?: mediaDetails?.runtime
                }

                val airDate = item.airDate ?: epDetail?.airDate ?: ""

                val playAction = if (itemIsMovie) {
                    ActionPreparePlayer(
                        key = key,
                        title = showTitle
                    )
                } else {
                    ActionPreparePlayer(
                        key = key,
                        title = "$showTitle S${item.seasonNumber ?: 1}E${item.episodeNumber ?: 1}",
                        targetSeason = item.seasonNumber,
                        targetEpisode = item.episodeNumber
                    )
                }

                val markWatchedAction = if (itemIsMovie) {
                    SetStatusCommand(
                        key = key,
                        status = MediaStatus.COMPLETED
                    )
                } else {
                    ToggleEpisodeWatchedCommand(
                        key = key,
                        seasonNumber = item.seasonNumber ?: 1,
                        episodeNumber = item.episodeNumber ?: 1,
                        isWatched = !isWatched
                    )
                }

                val openDetailsAction = if (itemIsMovie) {
                    ActionNavigate(Screen.MovieDetails(key))
                } else {
                    ActionNavigate(Screen.Details(key))
                }

                val cardItem = NewEpisodeCardItem(
                    id = item.id.toString(),
                    mediaKey = key,
                    showTitle = showTitle,
                    showPosterUrl = showPosterUrl,
                    episodeTitle = episodeTitle,
                    seasonNumber = item.seasonNumber,
                    episodeNumber = item.episodeNumber,
                    airDate = airDate,
                    durationMinutes = durationMinutes,
                    stillUrl = stillUrl,
                    overview = overview,
                    isNew = !item.isRead,
                    isWatched = isWatched,
                    voteAverage = epDetail?.voteAverage,
                    userRating = userRating,
                    playAction = playAction,
                    markWatchedAction = markWatchedAction,
                    openDetailsAction = openDetailsAction,
                    notificationType = item.notificationType
                )

                when {
                    airDate == todayStr -> todayEpisodes.add(cardItem)
                    airDate >= sevenDaysAgoStr -> thisWeekEpisodes.add(cardItem)
                    airDate >= thirtyDaysAgoStr -> earlierEpisodes.add(cardItem)
                    else -> showOlderEpisodes.add(cardItem)
                }
            }

            // Smart Rollup: если у сериала 3+ серий старше 30 дней, сворачиваем в пачку
            if (!isMovie && showOlderEpisodes.size >= 3) {
                val firstEp = showOlderEpisodes.minByOrNull { (it.seasonNumber ?: 0) * 1000 + (it.episodeNumber ?: 0) } ?: showOlderEpisodes.first()
                val lastEp = showOlderEpisodes.maxByOrNull { (it.seasonNumber ?: 0) * 1000 + (it.episodeNumber ?: 0) } ?: showOlderEpisodes.last()

                rollups.add(
                    MissedShowRollupItem(
                        mediaKey = key,
                        showTitle = showTitle,
                        showPosterUrl = showPosterUrl,
                        missedEpisodesCount = showOlderEpisodes.size,
                        startSeason = firstEp.seasonNumber ?: 1,
                        startEpisode = firstEp.episodeNumber ?: 1,
                        endSeason = lastEp.seasonNumber ?: 1,
                        endEpisode = lastEp.episodeNumber ?: 1,
                        continueAction = ActionPreparePlayer(
                            key = key,
                            title = "$showTitle S${firstEp.seasonNumber ?: 1}E${firstEp.episodeNumber ?: 1}",
                            targetSeason = firstEp.seasonNumber,
                            targetEpisode = firstEp.episodeNumber
                        ),
                        openDetailsAction = ActionNavigate(Screen.Details(key))
                    )
                )
            } else {
                earlierEpisodes.addAll(showOlderEpisodes)
            }
        }

        // Сортировка по дате выхода (свежие сверху)
        todayEpisodes.sortByDescending { it.airDate }
        thisWeekEpisodes.sortByDescending { it.airDate }
        earlierEpisodes.sortByDescending { it.airDate }

        val sections = mutableListOf<EpisodesSection>()
        if (todayEpisodes.isNotEmpty()) {
            sections.add(
                EpisodesSection(
                    sectionId = "today",
                    title = context.i18n.t("feed.section.today"),
                    episodes = todayEpisodes
                )
            )
        }
        if (thisWeekEpisodes.isNotEmpty()) {
            sections.add(
                EpisodesSection(
                    sectionId = "this_week",
                    title = context.i18n.t("feed.section.this_week"),
                    episodes = thisWeekEpisodes
                )
            )
        }
        if (earlierEpisodes.isNotEmpty()) {
            sections.add(
                EpisodesSection(
                    sectionId = "earlier",
                    title = context.i18n.t("feed.section.earlier"),
                    episodes = earlierEpisodes
                )
            )
        }

        val totalUnread = notifications.count { !it.isRead }
        val recentUnread = todayEpisodes.count { it.isNew } + thisWeekEpisodes.count { it.isNew }
        val allEpisodes = todayEpisodes + thisWeekEpisodes + earlierEpisodes
        val unwatched = allEpisodes.count { !it.isWatched }

        return SlotData.EpisodesFeed(
            sections = sections,
            rollups = rollups,
            totalUnreadCount = totalUnread,
            recentUnreadCount = recentUnread,
            unwatchedCount = unwatched,
            markAllReadAction = MarkNotificationsReadCommand
        )
    }
}
