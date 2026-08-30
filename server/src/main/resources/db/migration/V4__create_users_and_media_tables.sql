-- 1. Таблица пользователей
CREATE TABLE users
(
    id            VARCHAR(36) PRIMARY KEY,
    username      VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255)        NOT NULL,
    role          VARCHAR(20)         NOT NULL DEFAULT 'USER', -- 'ADMIN', 'USER'
    created_at    TIMESTAMP                    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP                    DEFAULT CURRENT_TIMESTAMP
);

-- 2. Таблица личной активности (фильмы / общее состояние сериалов)
CREATE TABLE user_movies
(
    id               VARCHAR(36) PRIMARY KEY,
    user_id          VARCHAR(36)  NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    catalog_id       VARCHAR(50)  NOT NULL DEFAULT 'tmdb',     -- 'tmdb', 'myshows' и др.
    media_id         VARCHAR(100) NOT NULL,                    -- Наш внутренний ID медиа
    media_type       VARCHAR(20)  NOT NULL,                    -- 'movie', 'tv'
    status           VARCHAR(20)  NOT NULL DEFAULT 'WATCHING', -- 'WATCHING', 'PLANNED', 'COMPLETED', 'DROPPED'
    user_rating      INT,                                      -- Личная оценка от 1 до 10
    progress_seconds BIGINT       NOT NULL DEFAULT 0,          -- Только для фильмов
    duration_seconds BIGINT       NOT NULL DEFAULT 0,          -- Только для фильмов
    last_watched_at  TIMESTAMP    NOT NULL,
    created_at       TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP             DEFAULT CURRENT_TIMESTAMP
);

-- 3. Таблица личного прогресса по сериям (только для сериалов)
CREATE TABLE user_episodes
(
    id               VARCHAR(36) PRIMARY KEY,
    user_id          VARCHAR(36)  NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    catalog_id       VARCHAR(50)  NOT NULL DEFAULT 'tmdb',
    media_id         VARCHAR(100) NOT NULL, -- Ссылка на внутренний ID сериала
    season           INT          NOT NULL,
    episode          INT          NOT NULL,
    progress_seconds BIGINT       NOT NULL DEFAULT 0,
    duration_seconds BIGINT       NOT NULL DEFAULT 0,
    is_watched       BOOLEAN      NOT NULL DEFAULT FALSE,
    last_watched_at  TIMESTAMP    NOT NULL,
    created_at       TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP             DEFAULT CURRENT_TIMESTAMP
);

-- 4. Таблица соответствия внешних ID каталогов
CREATE TABLE media_external_ids
(
    id              VARCHAR(36) PRIMARY KEY,
    user_movie_id   VARCHAR(36)  NOT NULL REFERENCES user_movies (id) ON DELETE CASCADE,
    external_source VARCHAR(50)  NOT NULL, -- 'tmdb', 'imdb', 'tvdb', 'myshows', 'trakt'
    external_id     VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Таблица кэша метаданных от разных провайдеров
CREATE TABLE media_metadata_cache
(
    id            VARCHAR(36) PRIMARY KEY,
    catalog_id    VARCHAR(50)  NOT NULL, -- 'tmdb', 'myshows' и др.
    external_id   VARCHAR(100) NOT NULL, -- ID в этом каталоге
    title         VARCHAR(255) NOT NULL,
    overview      TEXT,
    poster_url    TEXT,
    backdrop_url  TEXT,
    rating        DOUBLE PRECISION,      -- Рейтинг на этом сервисе
    metadata_json TEXT,                  -- Специфичные данные (жанры, актеры и т.д.)
    cached_at     TIMESTAMP    NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Таблица очереди синхронизации медиа-активности с внешними каталогами
CREATE TABLE user_media_sync_queue
(
    id               VARCHAR(36) PRIMARY KEY,
    user_id          VARCHAR(36)  NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    media_type       VARCHAR(20)  NOT NULL,                   -- 'MOVIE', 'TV'
    media_id         VARCHAR(100) NOT NULL,
    service          VARCHAR(50)  NOT NULL,                   -- 'trakt', 'myshows'
    action           VARCHAR(50)  NOT NULL,                   -- 'WATCH', 'UNWATCH', 'RATE', 'PROGRESS'
    progress_seconds BIGINT,
    duration_seconds BIGINT,
    rating           INT,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'FAILED', 'SUCCESS'
    attempts         INT          NOT NULL DEFAULT 0,
    last_attempt_at  TIMESTAMP,
    created_at       TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP             DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_media_sync_queue_status ON user_media_sync_queue (status);

-- Индексы уникальности
CREATE UNIQUE INDEX idx_user_movies_user_catalog_media ON user_movies (user_id, catalog_id, media_id);
CREATE UNIQUE INDEX idx_user_episodes_user_cat_med_ep ON user_episodes (user_id, catalog_id, media_id, season, episode);
CREATE UNIQUE INDEX idx_media_ext_ids_movie_ext_source ON media_external_ids (user_movie_id, external_source);
CREATE UNIQUE INDEX idx_media_metadata_cache_cat_ext ON media_metadata_cache (catalog_id, external_id);

-- Удаляем старую временную таблицу watch_history, если она была создана в V3
DROP TABLE IF EXISTS watch_history;
