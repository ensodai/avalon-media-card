CREATE TABLE IF NOT EXISTS user_media_sync_status
(
    id
    TEXT
    PRIMARY
    KEY,
    user_id
    TEXT
    NOT
    NULL,
    media_type
    TEXT
    NOT
    NULL,
    media_id
    TEXT
    NOT
    NULL,
    service
    TEXT
    NOT
    NULL,
    status
    TEXT
    NOT
    NULL,
    last_synced_at
    TEXT,
    error_message
    TEXT,
    created_at
    TEXT
    DEFAULT
    CURRENT_TIMESTAMP,
    updated_at
    TEXT
    DEFAULT
    CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_sync_status_user_media_service
    ON user_media_sync_status(user_id, media_id, service);
