-- Создаем таблицу для кэширования прогресса просмотра сериалов с Trakt.tv
CREATE TABLE user_show_progress
(
    id                   VARCHAR(36) PRIMARY KEY,
    user_id              VARCHAR(36) NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    show_tmdb_id         INT         NOT NULL,
    next_season          INT         NOT NULL,
    next_episode         INT         NOT NULL,
    title                VARCHAR(255),
    next_episode_tmdb_id INT,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_user_show_progress_user_show ON user_show_progress (user_id, show_tmdb_id);
