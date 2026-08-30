CREATE TABLE IF NOT EXISTS user_affinity_vector
(
    id
    UUID
    PRIMARY
    KEY,
    user_id
    UUID
    NOT
    NULL
    UNIQUE
    REFERENCES
    users
(
    id
) ON DELETE CASCADE,
    vector_json TEXT NOT NULL,
    calculated_at TIMESTAMP NOT NULL,
    event_count INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
    );
