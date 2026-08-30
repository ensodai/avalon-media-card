-- SQLite ALTER TABLE support for dropping columns is only in newer versions, 
-- but since the table is empty (we just cleared it), we can safely drop and recreate it.
DROP TABLE media_metadata_cache;

CREATE TABLE media_metadata_cache
(
    id           TEXT PRIMARY KEY,
    catalog_id   VARCHAR(50)                         NOT NULL,
    external_id  VARCHAR(100)                        NOT NULL,
    title        VARCHAR(255)                        NOT NULL,
    overview     TEXT,
    poster_url   VARCHAR(255),
    backdrop_url VARCHAR(255),
    media_type   VARCHAR(20)                         NOT NULL,
    release_year INTEGER,
    tmdb_rating DOUBLE,
    cached_at    TIMESTAMP                           NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (catalog_id, external_id)
);

CREATE TABLE media_genres
(
    id                TEXT PRIMARY KEY,
    media_metadata_id TEXT                                NOT NULL,
    genre_name        VARCHAR(100)                        NOT NULL,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (media_metadata_id) REFERENCES media_metadata_cache (id) ON DELETE CASCADE
);

CREATE TABLE media_people
(
    id         TEXT PRIMARY KEY,
    person_id  VARCHAR(100)                        NOT NULL UNIQUE,
    name       VARCHAR(255)                        NOT NULL,
    avatar_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE media_credits
(
    id                TEXT PRIMARY KEY,
    media_metadata_id TEXT                                NOT NULL,
    person_id         TEXT                                NOT NULL,
    role              VARCHAR(50)                         NOT NULL,
    character_name    VARCHAR(255),
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (media_metadata_id) REFERENCES media_metadata_cache (id) ON DELETE CASCADE,
    FOREIGN KEY (person_id) REFERENCES media_people (id) ON DELETE CASCADE
);
