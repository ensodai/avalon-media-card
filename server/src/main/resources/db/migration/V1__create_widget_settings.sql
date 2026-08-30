-- Таблица настроек виджетов для главного экрана
CREATE TABLE widget_settings
(
    id          VARCHAR(36) PRIMARY KEY,
    plugin_id   VARCHAR(100) NOT NULL,
    is_visible  BOOLEAN      NOT NULL DEFAULT 1,
    order_index INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMP             DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP             DEFAULT CURRENT_TIMESTAMP NOT NULL
);
