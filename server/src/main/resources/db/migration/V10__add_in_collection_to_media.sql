-- Добавляем колонку in_collection в таблицы user_movies и user_episodes для поддержки синхронизации коллекции Trakt
ALTER TABLE user_movies
    ADD COLUMN in_collection BOOLEAN NOT NULL DEFAULT 0;
ALTER TABLE user_episodes
    ADD COLUMN in_collection BOOLEAN NOT NULL DEFAULT 0;
