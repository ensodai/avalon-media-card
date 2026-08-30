DROP TABLE user_integration_settings;

CREATE TABLE user_integration_settings
(
    id            VARCHAR(36) PRIMARY KEY,
    user_id       VARCHAR(36)  NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plugin_id     VARCHAR(128) NOT NULL,
    setting_key   VARCHAR(255) NOT NULL,
    setting_value TEXT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_plugin_key UNIQUE (user_id, plugin_id, setting_key)
);

CREATE INDEX idx_user_integration_settings_user_plugin ON user_integration_settings (user_id, plugin_id);
