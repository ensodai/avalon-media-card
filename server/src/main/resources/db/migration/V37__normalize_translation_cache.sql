-- 1. Drop existing tables
DROP TABLE IF EXISTS media_credits;
DROP TABLE IF EXISTS media_genres;
DROP TABLE IF EXISTS media_keywords;
DROP TABLE IF EXISTS media_episode_cache;
DROP TABLE IF EXISTS media_season_cache;
DROP TABLE IF EXISTS media_people;
DROP TABLE IF EXISTS media_metadata_cache;

-- 2. Create base tables
CREATE TABLE media
(
    id           VARCHAR(36) PRIMARY KEY,
    catalog_id   VARCHAR(50)                         NOT NULL,
    external_id  VARCHAR(100)                        NOT NULL,
    media_type   VARCHAR(20)                         NOT NULL,
    release_year INTEGER,
    tmdb_rating  DOUBLE PRECISION,
    status       VARCHAR(50),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (catalog_id, external_id)
);

CREATE TABLE seasons
(
    id            VARCHAR(36) PRIMARY KEY,
    media_id      VARCHAR(36)                         NOT NULL REFERENCES media (id) ON DELETE CASCADE,
    season_number INTEGER                             NOT NULL,
    episode_count INTEGER,
    air_date      VARCHAR(50),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (media_id, season_number)
);

CREATE TABLE episodes
(
    id             VARCHAR(36) PRIMARY KEY,
    season_id      VARCHAR(36)                         NOT NULL REFERENCES seasons (id) ON DELETE CASCADE,
    episode_number INTEGER                             NOT NULL,
    runtime        INTEGER,
    air_date       VARCHAR(50),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (season_id, episode_number)
);

CREATE TABLE people
(
    id         VARCHAR(36) PRIMARY KEY,
    person_id  VARCHAR(100)                        NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE credits
(
    id         VARCHAR(36) PRIMARY KEY,
    media_id   VARCHAR(36)                         NOT NULL REFERENCES media (id) ON DELETE CASCADE,
    person_id  VARCHAR(36)                         NOT NULL REFERENCES people (id) ON DELETE CASCADE,
    role       VARCHAR(50)                         NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (media_id, person_id, role)
);

CREATE TABLE media_genres
(
    id         VARCHAR(36) PRIMARY KEY,
    media_id   VARCHAR(36)                         NOT NULL REFERENCES media (id) ON DELETE CASCADE,
    genre_id   INTEGER                             NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (media_id, genre_id)
);

CREATE TABLE media_keywords
(
    media_id     VARCHAR(36)  NOT NULL REFERENCES media (id) ON DELETE CASCADE,
    keyword_id   INTEGER      NOT NULL,
    keyword_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (media_id, keyword_id)
);

-- 3. Create Media Images table
CREATE TABLE media_images
(
    id         VARCHAR(36) PRIMARY KEY,
    media_id   VARCHAR(36)                         NOT NULL REFERENCES media (id) ON DELETE CASCADE,
    season_id  VARCHAR(36) REFERENCES seasons (id) ON DELETE CASCADE,
    episode_id VARCHAR(36) REFERENCES episodes (id) ON DELETE CASCADE,
    person_id  VARCHAR(36) REFERENCES people (id) ON DELETE CASCADE,
    image_type VARCHAR(20)                         NOT NULL,
    language   VARCHAR(10),
    url        TEXT                                NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 4. Create Translation tables
CREATE TABLE media_translations
(
    id             VARCHAR(36) PRIMARY KEY,
    media_id       VARCHAR(36)                         NOT NULL REFERENCES media (id) ON DELETE CASCADE,
    language       VARCHAR(10)                         NOT NULL,
    title          VARCHAR(255)                        NOT NULL,
    original_title VARCHAR(255),
    overview       TEXT,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (media_id, language)
);

CREATE TABLE season_translations
(
    id         VARCHAR(36) PRIMARY KEY,
    season_id  VARCHAR(36)                         NOT NULL REFERENCES seasons (id) ON DELETE CASCADE,
    language   VARCHAR(10)                         NOT NULL,
    name       VARCHAR(255),
    overview   TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (season_id, language)
);

CREATE TABLE episode_translations
(
    id         VARCHAR(36) PRIMARY KEY,
    episode_id VARCHAR(36)                         NOT NULL REFERENCES episodes (id) ON DELETE CASCADE,
    language   VARCHAR(10)                         NOT NULL,
    name       VARCHAR(255),
    overview   TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (episode_id, language)
);

CREATE TABLE person_translations
(
    id         VARCHAR(36) PRIMARY KEY,
    person_id  VARCHAR(36)                         NOT NULL REFERENCES people (id) ON DELETE CASCADE,
    language   VARCHAR(10)                         NOT NULL,
    name       VARCHAR(255)                        NOT NULL,
    biography  TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (person_id, language)
);

CREATE TABLE credit_translations
(
    id             VARCHAR(36) PRIMARY KEY,
    credit_id      VARCHAR(36)                         NOT NULL REFERENCES credits (id) ON DELETE CASCADE,
    language       VARCHAR(10)                         NOT NULL,
    character_name VARCHAR(255),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (credit_id, language)
);

-- 5. Migrate user tables (drop data and recreate via Exposed output)
DROP TABLE IF EXISTS user_movies;
DROP TABLE IF EXISTS user_episodes;
DROP TABLE IF EXISTS user_media_bindings;
DROP TABLE IF EXISTS user_media_sync_queue;
DROP TABLE IF EXISTS user_media_sync_status;
DROP TABLE IF EXISTS torrent_mappings;
DROP TABLE IF EXISTS user_custom_list_items;
