CREATE TABLE IF NOT EXISTS media_discover_cache
(
    id           VARCHAR(36)  NOT NULL PRIMARY KEY,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    cache_key    VARCHAR(255) NOT NULL,
    params_hash  VARCHAR(64)  NOT NULL,
    target_type  VARCHAR(20)  NOT NULL,
    language     VARCHAR(10)  NOT NULL,
    page         INT          NOT NULL,
    results_json TEXT         NOT NULL,
    expires_at   TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS media_discover_cache_cache_key_unique ON media_discover_cache (cache_key);
CREATE INDEX IF NOT EXISTS media_discover_cache_params_hash ON media_discover_cache (params_hash);
CREATE INDEX IF NOT EXISTS media_discover_cache_expires_at ON media_discover_cache (expires_at);

CREATE TABLE IF NOT EXISTS user_feed_section_cache
(
    id            VARCHAR(36) NOT NULL PRIMARY KEY,
    created_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP   DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_id       VARCHAR(36) NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    scope         VARCHAR(32) NOT NULL,
    language      VARCHAR(10) NOT NULL,
    sections_json TEXT        NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS user_feed_section_cache_user_scope_lang ON user_feed_section_cache (user_id, scope, language);
