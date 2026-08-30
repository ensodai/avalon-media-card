package org.ensodai.avalonmediacard.plugins.traktmetadata

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.plugins.AvalonPlugin
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.slot.*
import org.ensodai.avalonmediacard.plugins.traktmetadata.domain.GetMediaCommentsUseCase
import org.ensodai.avalonmediacard.plugins.traktmetadata.domain.TraktMetadataRepositoryImpl

@Serializable
data class LoadMoreComments(
    val mediaKey: MediaKey,
    val page: Int
) : ServerAction

data class CommentsState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val comments: List<CommentItem> = emptyList(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val error: String? = null
)

class TraktMetadataPlugin : AvalonPlugin {
    override val id: String = "org.ensodai.traktmetadata"
    override val name: String = "Интеграция метаданных Trakt"
    override val version: String = "1.0.0"
    override val author: String = "Antigravity"

    private val commentsStateMap = java.util.concurrent.ConcurrentHashMap<MediaKey, MutableStateFlow<CommentsState>>()
    private fun getCommentsState(key: MediaKey): kotlinx.coroutines.flow.MutableStateFlow<CommentsState> {
        return commentsStateMap.getOrPut(key) { MutableStateFlow(CommentsState(isLoading = true)) }
    }

    override fun provideSerializers(): SerializersModule = SerializersModule {
        polymorphic(Action::class) {
            subclass(LoadMoreComments::class)
        }
        polymorphic(ServerAction::class) {
            subclass(LoadMoreComments::class)
        }
    }

    override fun onInitialize(context: PluginContext) {
        val repository = TraktMetadataRepositoryImpl(context)
        val getMediaCommentsUseCase = GetMediaCommentsUseCase(repository)

        fun buildPromises(key: MediaKey, userId: kotlin.uuid.Uuid?): org.ensodai.avalonmediacard.contract.plugins.ScreenSlots {
            val flow = kotlinx.coroutines.flow.flow {
                if (userId == null || !context.integrationManager.hasTraktAuth(userId)) {
                    emit(ScreenStreamEvent.Update(SlotUpdate(SlotId.Comments, id, org.ensodai.avalonmediacard.contract.slot.SlotState.Empty)))
                    return@flow
                }
                
                getCommentsState(key).collect { state ->
                    if (state.isLoading && state.comments.isEmpty()) {
                        emit(ScreenStreamEvent.Update(SlotUpdate(
                            slotId = SlotId.Comments,
                            nodeId = id,
                            state = org.ensodai.avalonmediacard.contract.slot.SlotState.Loading()
                        )))
                    } else if (state.error != null && state.comments.isEmpty()) {
                        emit(ScreenStreamEvent.Update(SlotUpdate(SlotId.Comments, id, org.ensodai.avalonmediacard.contract.slot.SlotState.Empty)))
                    } else {
                        val nextPage = state.currentPage + 1
                        val loadMoreAction = if (state.hasMore && !state.isLoadingMore) {
                            LoadMoreComments(key, nextPage)
                        } else null

                        emit(ScreenStreamEvent.Update(SlotUpdate(
                            slotId = SlotId.Comments,
                            nodeId = id,
                            state = org.ensodai.avalonmediacard.contract.slot.SlotState.Content(
                                SlotData.Comments(
                                    title = context.i18n.t("trakt.reviews_title"),
                                    comments = state.comments,
                                    currentPage = state.currentPage,
                                    loadMoreAction = loadMoreAction
                                )
                            )
                        )))
                    }
                }
            }.onStart {
                if (userId != null && context.integrationManager.hasTraktAuth(userId)) {
                    loadComments(key, getMediaCommentsUseCase, context)
                }
            }
            
            return org.ensodai.avalonmediacard.contract.plugins.ScreenSlots(
                layout = listOf(org.ensodai.avalonmediacard.contract.slot.LayoutNode(id, SlotId.Comments)),
                flow = flow
            )
        }

        val commentsLayout = listOf(org.ensodai.avalonmediacard.contract.slot.LayoutNode(id, SlotId.Comments))
        
        context.slots.declare<Screen.MovieDetails>(listOf(SlotId.Comments)) { commentsLayout }
        context.slots.onScreen<Screen.MovieDetails> { screen, userId ->
            buildPromises(screen.key, userId)
        }

        context.slots.declare<Screen.TvShowDetails>(listOf(SlotId.Comments)) { commentsLayout }
        context.slots.onScreen<Screen.TvShowDetails> { screen, userId ->
            buildPromises(screen.key, userId)
        }

        context.actions.bind<LoadMoreComments> { cmd, _ ->
            loadNextPage(cmd.mediaKey, cmd.page, getMediaCommentsUseCase, context)
            ActionResult.NoOp
        }
    }

    private fun loadComments(key: MediaKey, useCase: GetMediaCommentsUseCase, context: PluginContext) {
        val flow = commentsStateMap.getOrPut(key) { MutableStateFlow(CommentsState()) }
        if (!flow.value.isLoading && flow.value.comments.isNotEmpty()) {
            return
        }

        flow.value = CommentsState(isLoading = true)

        context.scope.launch {
            try {
                val comments = useCase(key, page = 1, limit = 10)
                flow.value = CommentsState(
                    comments = comments,
                    currentPage = 1,
                    hasMore = comments.isNotEmpty(),
                    isLoading = false
                )
            } catch (e: Exception) {
                flow.value = CommentsState(
                    error = e.message ?: "Failed to load comments",
                    isLoading = false
                )
            }
        }
    }

    private fun loadNextPage(key: MediaKey, page: Int, useCase: GetMediaCommentsUseCase, context: PluginContext) {
        val flow = commentsStateMap.getOrPut(key) { MutableStateFlow(CommentsState()) }
        val state = flow.value
        if (state.isLoadingMore || !state.hasMore) return

        flow.value = state.copy(isLoadingMore = true)

        context.scope.launch {
            try {
                val nextComments = useCase(key, page = page, limit = 10)
                val updatedState = flow.value
                val allComments = updatedState.comments + nextComments
                flow.value = updatedState.copy(
                    comments = allComments,
                    currentPage = page,
                    isLoadingMore = false,
                    hasMore = nextComments.isNotEmpty()
                )
            } catch (e: Exception) {
                val updatedState = flow.value
                flow.value = updatedState.copy(
                    isLoadingMore = false,
                    error = e.message ?: "Failed to load more comments"
                )
            }
        }
    }
}
