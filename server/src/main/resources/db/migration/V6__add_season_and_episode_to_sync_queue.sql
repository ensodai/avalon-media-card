-- V6__add_season_and_episode_to_sync_queue.sql
ALTER TABLE user_media_sync_queue
    ADD COLUMN season INTEGER;
ALTER TABLE user_media_sync_queue
    ADD COLUMN episode INTEGER;
