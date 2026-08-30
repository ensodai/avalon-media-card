package org.ensodai.avalonmediacard.plugins.persondetails

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.ensodai.avalonmediacard.contract.model.EntityType
import org.ensodai.avalonmediacard.contract.model.MediaKey
import org.ensodai.avalonmediacard.contract.model.MediaProvider
import org.ensodai.avalonmediacard.contract.ui.navigation.Screen
import org.ensodai.avalonmediacard.contract.plugins.AvalonPlugin
import org.ensodai.avalonmediacard.contract.plugins.PluginContext
import org.ensodai.avalonmediacard.contract.slot.*
import org.ensodai.avalonmediacard.plugins.persondetails.domain.GetPersonDetailsUseCase
import org.ensodai.avalonmediacard.plugins.persondetails.domain.PersonDetailsRepositoryImpl
import org.ensodai.avalonmediacard.plugins.persondetails.domain.PersonDetailsState

class PersonDetailsPlugin : AvalonPlugin {
    override val id: String = "org.ensodai.persondetails"
    override val name: String = "Детали Персоны и Фильмография"
    override val version: String = "1.0.0"
    override val author: String = "Antigravity"

    private val personDetailsStatesMap =
        java.util.concurrent.ConcurrentHashMap<MediaKey, MutableStateFlow<PersonDetailsState>>()

    private fun getPersonDetailsState(key: MediaKey): kotlinx.coroutines.flow.MutableStateFlow<PersonDetailsState> {
        return personDetailsStatesMap.getOrPut(key) { MutableStateFlow(PersonDetailsState(isLoading = true)) }
    }

    override fun onInitialize(context: PluginContext) {
        val repository = PersonDetailsRepositoryImpl(context)
        val getPersonDetailsUseCase = GetPersonDetailsUseCase(repository)

        val personLayout = listOf(
            org.ensodai.avalonmediacard.contract.slot.LayoutNode(id, SlotId.PersonHeader),
            org.ensodai.avalonmediacard.contract.slot.LayoutNode(id, SlotId.PersonBio),
            org.ensodai.avalonmediacard.contract.slot.LayoutNode(id, SlotId.PersonCredits)
        )
        context.slots.declare<Screen.Person>(listOf(SlotId.PersonHeader, SlotId.PersonBio, SlotId.PersonCredits)) { personLayout }

        context.slots.onScreen<Screen.Person> { screen, _ ->
            val key = screen.key

            val headerFlow = getPersonDetailsState(key)
                .map { state ->
                    val meta = state.metadata
                    if (state.isLoading || meta == null) {
                        SlotUpdate(
                            slotId = SlotId.PersonHeader,
                            nodeId = id,
                            state = org.ensodai.avalonmediacard.contract.slot.SlotState.Loading()
                        )
                    } else {
                        val bornStr = meta.birthday?.let { context.i18n.t("person.born_fmt", it) }
                        val taglineText = buildString {
                            if (bornStr != null) append("$bornStr ")
                            if (meta.placeOfBirth != null) append("(${meta.placeOfBirth})")
                        }.takeIf { it.isNotBlank() }

                        SlotUpdate(
                            slotId = SlotId.PersonHeader,
                            nodeId = id,
                            state = org.ensodai.avalonmediacard.contract.slot.SlotState.Content(
                                SlotData.Header(
                                    title = meta.name,
                                    subtitle = meta.knownForDepartment,
                                    tagline = taglineText,
                                    posterUrl = meta.profileUrl
                                )
                            )
                        )
                    }
                }.onStart { loadPersonDetails(key, getPersonDetailsUseCase, context) }

            val bioFlow = getPersonDetailsState(key)
                .map { state ->
                    val text = state.metadata?.biography ?: ""
                    if (state.isLoading && text.isBlank()) {
                        SlotUpdate(
                            slotId = SlotId.PersonBio,
                            nodeId = id,
                            state = org.ensodai.avalonmediacard.contract.slot.SlotState.Loading()
                        )
                    } else if (text.isNotBlank()) {
                        SlotUpdate(
                            slotId = SlotId.PersonBio,
                            nodeId = id,
                            state = org.ensodai.avalonmediacard.contract.slot.SlotState.Content(SlotData.Text(text))
                        )
                    } else {
                        SlotUpdate(
                            slotId = SlotId.PersonBio,
                            nodeId = id,
                            state = org.ensodai.avalonmediacard.contract.slot.SlotState.Empty
                        )
                    }
                }

            val creditsFlow = getPersonDetailsState(key)
                .map { state ->
                    val filmography = state.metadata?.filmography ?: emptyList()
                    if (filmography.isEmpty()) {
                        if (state.isLoading) {
                            SlotUpdate(
                                slotId = SlotId.PersonCredits,
                                nodeId = id,
                                state = org.ensodai.avalonmediacard.contract.slot.SlotState.Loading()
                            )
                        } else {
                            SlotUpdate(
                                slotId = SlotId.PersonCredits,
                                nodeId = id,
                                state = org.ensodai.avalonmediacard.contract.slot.SlotState.Empty
                            )
                        }
                    } else {
                        val items = filmography.map { credit ->
                            val type = if (credit.mediaType == "tv") EntityType.TV else EntityType.MOVIE
                            val itemKey = MediaKey(MediaProvider.Tmdb, type, credit.mediaId)
                            MovieCarouselItem(
                                key = itemKey,
                                title = credit.title,
                                posterUrl = credit.posterUrl
                            )
                        }

                        SlotUpdate(
                            slotId = SlotId.PersonCredits,
                            nodeId = id,
                            state = org.ensodai.avalonmediacard.contract.slot.SlotState.Content(
                                SlotData.Carousel(
                                    id = "credits_${key.id}",
                                    title = context.i18n.t("person.filmography"),
                                    items = items,
                                    telemetryContext = org.ensodai.avalonmediacard.contract.model.ClickstreamContext.CAROUSEL_PERSON
                                )
                            )
                        )
                    }
                }

            org.ensodai.avalonmediacard.contract.plugins.ScreenSlots(
                layout = listOf(
                    org.ensodai.avalonmediacard.contract.slot.LayoutNode(id, SlotId.PersonHeader),
                    org.ensodai.avalonmediacard.contract.slot.LayoutNode(id, SlotId.PersonBio),
                    org.ensodai.avalonmediacard.contract.slot.LayoutNode(id, SlotId.PersonCredits)
                ),
                flow = kotlinx.coroutines.flow.merge(headerFlow, bioFlow, creditsFlow)
                    .map { ScreenStreamEvent.Update(it) }
            )
        }
    }

    private fun loadPersonDetails(key: MediaKey, useCase: GetPersonDetailsUseCase, context: PluginContext) {
        val flow = personDetailsStatesMap.getOrPut(key) { MutableStateFlow(PersonDetailsState(isLoading = true)) }
        if (flow.value.metadata != null) return

        flow.value = PersonDetailsState(isLoading = true)

        context.scope.launch {
            try {
                val metadata = useCase(key)
                flow.value = PersonDetailsState(metadata = metadata)
            } catch (e: Exception) {
                flow.value = PersonDetailsState(error = e.message ?: "Ошибка получения деталей персоны")
            }
        }
    }
}
