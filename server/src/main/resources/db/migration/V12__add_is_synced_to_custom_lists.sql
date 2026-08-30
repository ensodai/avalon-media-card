-- Добавление флага is_synced для кастомных списков и их элементов
ALTER TABLE user_custom_lists
    ADD COLUMN is_synced BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE user_custom_list_items
    ADD COLUMN is_synced BOOLEAN NOT NULL DEFAULT FALSE;
