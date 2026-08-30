package org.ensodai.avalonmediacard.plugins.homefeed.domain

import org.ensodai.avalonmediacard.contract.model.TmdbMovieDto
import kotlin.uuid.Uuid

enum class FeedSection(
    val id: String,
    val title: String,
    val listName: String,
    val listTitle: String,
    val widgetUuid: Uuid
) {
    TRENDING(
        id = "tmdb_trending",
        title = "Популярное на этой неделе",
        listName = "trending",
        listTitle = "Популярное на этой неделе",
        widgetUuid = Uuid.parse("00000000-0000-0000-0000-000000000002")
    ),
    TOP_RATED(
        id = "tmdb_top_rated",
        title = "Лучшие фильмы (Рейтинг)",
        listName = "top_rated",
        listTitle = "Лучшие фильмы (Рейтинг)",
        widgetUuid = Uuid.parse("00000000-0000-0000-0000-000000000003")
    ),
    UPCOMING(
        id = "tmdb_upcoming",
        title = "Ожидаемые новинки",
        listName = "upcoming",
        listTitle = "Ожидаемые новинки",
        widgetUuid = Uuid.parse("00000000-0000-0000-0000-000000000004")
    ),
    TRENDING_SHOWS(
        id = "tmdb_trending_shows",
        title = "Популярные сериалы на неделе",
        listName = "trending_shows",
        listTitle = "Популярные сериалы",
        widgetUuid = Uuid.parse("00000000-0000-0000-0000-000000000005")
    ),
    POPULAR_SHOWS(
        id = "tmdb_popular_shows",
        title = "Смотрят сейчас (Сериалы)",
        listName = "popular_shows",
        listTitle = "Сейчас смотрят (Сериалы)",
        widgetUuid = Uuid.parse("00000000-0000-0000-0000-000000000006")
    ),
    TOP_RATED_SHOWS(
        id = "tmdb_top_rated_shows",
        title = "Лучшие сериалы всех времен",
        listName = "top_rated_shows",
        listTitle = "Лучшие сериалы всех времен",
        widgetUuid = Uuid.parse("00000000-0000-0000-0000-000000000007")
    ),
    DISCOVER_FOR_YOU(
        id = "discover_for_you",
        title = "Специально для вас",
        listName = "discover_for_you",
        listTitle = "Специально для вас",
        widgetUuid = Uuid.parse("00000000-0000-0000-0000-000000000008")
    );
}

sealed interface FeedSectionState {
    object Loading : FeedSectionState

    data class Success(
        val movies: List<TmdbMovieDto>
    ) : FeedSectionState

    data class Error(
        val message: String
    ) : FeedSectionState
}

data class SectionState(
    val state: FeedSectionState = FeedSectionState.Loading,
    val page: Int = 1
)

data class HomeState(
    val sections: Map<FeedSection, SectionState> = FeedSection.entries.associateWith { SectionState() }
)
