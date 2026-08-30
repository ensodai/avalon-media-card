ALTER TABLE user_movies
    ADD COLUMN last_source_provider_id VARCHAR(100);
ALTER TABLE user_movies
    ADD COLUMN last_source_id VARCHAR(255);
ALTER TABLE user_movies
    ADD COLUMN last_source_payload TEXT;

ALTER TABLE user_episodes
    ADD COLUMN last_source_provider_id VARCHAR(100);
ALTER TABLE user_episodes
    ADD COLUMN last_source_id VARCHAR(255);
ALTER TABLE user_episodes
    ADD COLUMN last_source_payload TEXT;
