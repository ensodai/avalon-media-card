CREATE TABLE media_season_cache
(
    id                TEXT PRIMARY KEY,
    media_metadata_id TEXT                           NOT NULL,
    season_number     INTEGER                        NOT NULL,
    name              TEXT,
    overview          TEXT,
    poster_url        TEXT,
    air_date          TEXT,
    cached_at         TEXT                           NOT NULL,
    created_at        TEXT DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at        TEXT DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (media_metadata_id) REFERENCES media_metadata_cache (id) ON DELETE CASCADE
);

CREATE INDEX idx_media_season_cache_metadata_id ON media_season_cache (media_metadata_id);

CREATE TABLE media_episode_cache
(
    id             TEXT PRIMARY KEY,
    season_id      TEXT                           NOT NULL,
    episode_number INTEGER                        NOT NULL,
    name           TEXT,
    overview       TEXT,
    poster_url     TEXT,
    air_date       TEXT,
    runtime        INTEGER,
    created_at     TEXT DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at     TEXT DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (season_id) REFERENCES media_season_cache (id) ON DELETE CASCADE
);

CREATE INDEX idx_media_episode_cache_season_id ON media_episode_cache (season_id);
