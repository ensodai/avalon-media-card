CREATE TABLE watch_history
(
    id               VARCHAR(36) PRIMARY KEY,
    media_id         VARCHAR(100) NOT NULL,
    catalog_id       VARCHAR(100) NOT NULL,
    media_type       VARCHAR(20)  NOT NULL,
    title            VARCHAR(255) NOT NULL,
    poster_url       TEXT         NOT NULL,
    backdrop_url     TEXT,
    progress_seconds BIGINT       NOT NULL,
    duration_seconds BIGINT       NOT NULL,
    last_watched_at  BIGINT       NOT NULL,
    season           INT,
    episode          INT,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_watch_history_media_catalog ON watch_history (media_id, catalog_id);
