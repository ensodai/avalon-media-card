CREATE TABLE IF NOT EXISTS torrent_mappings
(
    id
    VARCHAR
(
    36
) NOT NULL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    torrent_hash VARCHAR
(
    64
) NOT NULL,
    file_path TEXT NOT NULL,
    seasons TEXT NULL,
    episodes TEXT NULL,
    is_absolute BOOLEAN DEFAULT 0 NOT NULL,
    is_manual BOOLEAN DEFAULT 0 NOT NULL,
    media_id VARCHAR
(
    36
) NULL REFERENCES media
(
    id
) ON DELETE SET NULL,
    file_index INT NULL,
    file_size BIGINT NULL
    );
CREATE INDEX IF NOT EXISTS torrent_mappings_torrent_hash ON torrent_mappings (torrent_hash);
CREATE INDEX IF NOT EXISTS torrent_mappings_media_id ON torrent_mappings (media_id);
CREATE UNIQUE INDEX IF NOT EXISTS torrent_mappings_torrent_hash_file_path ON torrent_mappings (torrent_hash, file_path);

CREATE TABLE IF NOT EXISTS user_custom_list_items
(
    id
    VARCHAR
(
    36
) NOT NULL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    list_id VARCHAR
(
    36
) NOT NULL REFERENCES user_custom_lists
(
    id
) ON DELETE CASCADE,
    media_id VARCHAR
(
    36
) NOT NULL REFERENCES media
(
    id
)
  ON DELETE CASCADE,
    rank INT DEFAULT 0 NOT NULL,
    listed_at TIMESTAMP NULL,
    is_synced BOOLEAN DEFAULT 0 NOT NULL
    );

CREATE TABLE IF NOT EXISTS user_episodes
(
    id
    VARCHAR
(
    36
) NOT NULL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_id VARCHAR
(
    36
) NOT NULL REFERENCES users
(
    id
) ON DELETE CASCADE,
    episode_id VARCHAR
(
    36
) NOT NULL REFERENCES episodes
(
    id
)
  ON DELETE CASCADE,
    progress_seconds BIGINT DEFAULT 0 NOT NULL,
    duration_seconds BIGINT DEFAULT 0 NOT NULL,
    is_watched BOOLEAN DEFAULT 0 NOT NULL,
    in_collection BOOLEAN DEFAULT 0 NOT NULL,
    user_rating INT NULL,
    last_watched_at TIMESTAMP NOT NULL,
    last_source_provider_id VARCHAR
(
    100
) NULL,
    last_source_id VARCHAR
(
    255
) NULL,
    last_source_payload TEXT NULL
    );

CREATE TABLE IF NOT EXISTS user_media_bindings
(
    id
    VARCHAR
(
    36
) NOT NULL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_id VARCHAR
(
    36
) NOT NULL REFERENCES users
(
    id
) ON DELETE CASCADE,
    media_id VARCHAR
(
    36
) NOT NULL REFERENCES media
(
    id
)
  ON DELETE CASCADE,
    source_type VARCHAR
(
    64
) NOT NULL,
    source_id VARCHAR
(
    256
) NOT NULL
    );
CREATE UNIQUE INDEX IF NOT EXISTS user_media_bindings_user_id_media_id_source_type ON user_media_bindings (user_id, media_id, source_type);

CREATE TABLE IF NOT EXISTS user_media_sync_queue
(
    id
    VARCHAR
(
    36
) NOT NULL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_id VARCHAR
(
    36
) NOT NULL REFERENCES users
(
    id
) ON DELETE CASCADE,
    media_type VARCHAR
(
    20
) NOT NULL,
    media_id VARCHAR
(
    36
) NOT NULL REFERENCES media
(
    id
)
  ON DELETE CASCADE,
    service VARCHAR
(
    50
) NOT NULL,
    action VARCHAR
(
    50
) NOT NULL,
    progress_seconds BIGINT NULL,
    duration_seconds BIGINT NULL,
    rating INT NULL,
    episode_id VARCHAR
(
    36
) NULL REFERENCES episodes
(
    id
)
  ON DELETE CASCADE,
    status VARCHAR
(
    20
) DEFAULT 'PENDING' NOT NULL,
    attempts INT DEFAULT 0 NOT NULL,
    last_attempt_at TIMESTAMP NULL
    );

CREATE TABLE IF NOT EXISTS user_media_sync_status
(
    id
    VARCHAR
(
    36
) NOT NULL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_id VARCHAR
(
    36
) NOT NULL REFERENCES users
(
    id
) ON DELETE CASCADE,
    media_type VARCHAR
(
    20
) NOT NULL,
    media_id VARCHAR
(
    36
) NOT NULL REFERENCES media
(
    id
)
  ON DELETE CASCADE,
    service VARCHAR
(
    50
) NOT NULL,
    status VARCHAR
(
    20
) NOT NULL,
    last_synced_at TIMESTAMP NULL,
    error_message TEXT NULL
    );

CREATE TABLE IF NOT EXISTS user_movies
(
    id
    VARCHAR
(
    36
) NOT NULL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_id VARCHAR
(
    36
) NOT NULL REFERENCES users
(
    id
) ON DELETE CASCADE,
    media_id VARCHAR
(
    36
) NOT NULL REFERENCES media
(
    id
)
  ON DELETE CASCADE,
    media_type VARCHAR
(
    20
) NOT NULL,
    status VARCHAR
(
    20
) DEFAULT 'WATCHING' NOT NULL,
    user_rating INT NULL,
    progress_seconds BIGINT DEFAULT 0 NOT NULL,
    duration_seconds BIGINT DEFAULT 0 NOT NULL,
    in_collection BOOLEAN DEFAULT 0 NOT NULL,
    last_watched_at TIMESTAMP NOT NULL,
    last_source_provider_id VARCHAR
(
    100
) NULL,
    last_source_id VARCHAR
(
    255
) NULL,
    last_source_payload TEXT NULL
    );

ALTER TABLE media_keywords
    ADD COLUMN media_type VARCHAR(50) NOT NULL DEFAULT 'MOVIE';
CREATE UNIQUE INDEX IF NOT EXISTS season_translations_season_id_language ON season_translations (season_id, language);
CREATE UNIQUE INDEX IF NOT EXISTS seasons_media_id_season_number ON seasons (media_id, season_number);
CREATE UNIQUE INDEX IF NOT EXISTS media_catalog_id_external_id ON media (catalog_id, external_id);
CREATE UNIQUE INDEX IF NOT EXISTS people_person_id ON people (person_id);
CREATE UNIQUE INDEX IF NOT EXISTS person_translations_person_id_language ON person_translations (person_id, language);
CREATE UNIQUE INDEX IF NOT EXISTS credit_translations_credit_id_language ON credit_translations (credit_id, language);
CREATE UNIQUE INDEX IF NOT EXISTS media_translations_media_id_language ON media_translations (media_id, language);
CREATE UNIQUE INDEX IF NOT EXISTS episodes_season_id_episode_number ON episodes (season_id, episode_number);
CREATE UNIQUE INDEX IF NOT EXISTS episode_translations_episode_id_language ON episode_translations (episode_id, language);
