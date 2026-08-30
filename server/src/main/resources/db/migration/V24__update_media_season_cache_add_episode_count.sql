-- Add episode_count to media_season_cache
ALTER TABLE media_season_cache
    ADD COLUMN episode_count INTEGER DEFAULT NULL;

-- Note: We intentionally do not DROP COLUMN cached_at from media_metadata_cache and media_season_cache
-- to maintain compatibility with older SQLite versions that do not support DROP COLUMN.
-- The columns will simply be ignored by Exposed ORM.
