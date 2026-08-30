package org.ensodai.avalonmediacard.data.serialization

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.ensodai.avalonmediacard.contract.slot.*

val SduiClientSerializersModule = SerializersModule {
    // Handle unknown Actions (e.g. from plugins) in SlotData deserialization
    polymorphic(Action::class) {
        defaultDeserializer { className ->
            createUnknownActionDeserializer(className)
        }
        subclass(ActionNavigate::class, ActionNavigate.serializer())
        subclass(ActionPlayVideo::class, ActionPlayVideo.serializer())
        subclass(ActionPreparePlayer::class, ActionPreparePlayer.serializer())
        subclass(ActionOpenSources::class, ActionOpenSources.serializer())
        subclass(ActionOpenUrl::class, ActionOpenUrl.serializer())

        subclass(SearchQueryCommand::class, SearchQueryCommand.serializer())
        subclass(RefreshIntegrationsCommand::class, RefreshIntegrationsCommand.serializer())
        subclass(UpdateIntegrationSettingCommand::class, UpdateIntegrationSettingCommand.serializer())
        subclass(ToggleEpisodeWatchedCommand::class, ToggleEpisodeWatchedCommand.serializer())
        subclass(RateEpisodeCommand::class, RateEpisodeCommand.serializer())
        subclass(SaveEpisodeProgressCommand::class, SaveEpisodeProgressCommand.serializer())
        subclass(SaveMovieProgressCommand::class, SaveMovieProgressCommand.serializer())
        subclass(UploadCustomTorrentCommand::class, UploadCustomTorrentCommand.serializer())
        subclass(ToggleCollectionCommand::class, ToggleCollectionCommand.serializer())
        subclass(SetRatingCommand::class, SetRatingCommand.serializer())
        subclass(SetStatusCommand::class, SetStatusCommand.serializer())
        subclass(ToggleCustomListCommand::class, ToggleCustomListCommand.serializer())
        subclass(CreateCustomListCommand::class, CreateCustomListCommand.serializer())
        subclass(MarkSeasonWatchedCommand::class, MarkSeasonWatchedCommand.serializer())
        subclass(SelectSeasonCommand::class, SelectSeasonCommand.serializer())
    }

    polymorphic(ServerAction::class) {
        defaultDeserializer { className ->
            createUnknownActionDeserializer(className)
        }
        subclass(SearchQueryCommand::class, SearchQueryCommand.serializer())
        subclass(RefreshIntegrationsCommand::class, RefreshIntegrationsCommand.serializer())
        subclass(UpdateIntegrationSettingCommand::class, UpdateIntegrationSettingCommand.serializer())
        subclass(ToggleEpisodeWatchedCommand::class, ToggleEpisodeWatchedCommand.serializer())
        subclass(RateEpisodeCommand::class, RateEpisodeCommand.serializer())
        subclass(SaveEpisodeProgressCommand::class, SaveEpisodeProgressCommand.serializer())
        subclass(SaveMovieProgressCommand::class, SaveMovieProgressCommand.serializer())
        subclass(UploadCustomTorrentCommand::class, UploadCustomTorrentCommand.serializer())
        subclass(ToggleCollectionCommand::class, ToggleCollectionCommand.serializer())
        subclass(SetRatingCommand::class, SetRatingCommand.serializer())
        subclass(SetStatusCommand::class, SetStatusCommand.serializer())
        subclass(ToggleCustomListCommand::class, ToggleCustomListCommand.serializer())
        subclass(CreateCustomListCommand::class, CreateCustomListCommand.serializer())
        subclass(MarkSeasonWatchedCommand::class, MarkSeasonWatchedCommand.serializer())
        subclass(SelectSeasonCommand::class, SelectSeasonCommand.serializer())
    }

    // Register a custom top-level serializer for sending ServerActions via kRPC
    contextual(ServerAction::class, ClientServerActionSerializer)
}
