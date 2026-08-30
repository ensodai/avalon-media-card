-- Кастомные списки пользователя (импортированные из Trakt и др.)
CREATE TABLE IF NOT EXISTS user_custom_lists
(
    id
    VARCHAR
(
    36
) PRIMARY KEY,
    user_id VARCHAR
(
    36
) NOT NULL,
    service VARCHAR
(
    50
) NOT NULL DEFAULT 'trakt',
    external_list_id VARCHAR
(
    100
) NOT NULL,
    slug VARCHAR
(
    200
) NOT NULL,
    name VARCHAR
(
    500
) NOT NULL,
    privacy VARCHAR
(
    20
) NOT NULL DEFAULT 'private',
    item_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE
(
    user_id,
    service,
    external_list_id
)
    );

-- Элементы кастомных списков
CREATE TABLE IF NOT EXISTS user_custom_list_items
(
    id
    VARCHAR
(
    36
) PRIMARY KEY,
    list_id VARCHAR
(
    36
) NOT NULL REFERENCES user_custom_lists
(
    id
) ON DELETE CASCADE,
    media_type VARCHAR
(
    20
) NOT NULL,
    tmdb_id INT NOT NULL,
    rank INT NOT NULL DEFAULT 0,
    title VARCHAR
(
    500
),
    listed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
    );
