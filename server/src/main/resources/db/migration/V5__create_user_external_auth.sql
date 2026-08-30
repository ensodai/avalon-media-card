-- V5__create_user_external_auth.sql
CREATE TABLE user_external_auth
(
    id            VARCHAR(36) PRIMARY KEY,
    user_id       VARCHAR(36) NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    service       VARCHAR(50) NOT NULL, -- 'trakt', 'myshows'
    access_token  TEXT        NOT NULL,
    refresh_token TEXT,
    expires_in    BIGINT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_user_external_auth_user_service ON user_external_auth (user_id, service);
