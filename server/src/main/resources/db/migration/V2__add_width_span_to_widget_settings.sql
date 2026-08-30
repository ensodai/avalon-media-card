-- Добавляем колонку width_span для сохранения ширины виджетов дашборда
ALTER TABLE widget_settings
    ADD COLUMN width_span INTEGER NOT NULL DEFAULT 2;
