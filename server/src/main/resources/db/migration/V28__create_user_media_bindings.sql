CREATE TABLE IF NOT EXISTS user_media_bindings
(
    id
    UUID
    PRIMARY
    KEY,
    user_id
    UUID
    NOT
    NULL,
    media_id
    VARCHAR
(
    128
) NOT NULL,
    source_type VARCHAR
(
    64
) NOT NULL,
    source_id VARCHAR
(
    256
) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY
(
    user_id
) REFERENCES users
(
    id
) ON DELETE CASCADE,
    UNIQUE
(
    user_id,
    media_id,
    source_type
)
    );
