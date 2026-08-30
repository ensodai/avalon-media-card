-- Добавляем колонку user_rating в таблицу user_episodes
ALTER TABLE user_episodes
    ADD COLUMN user_rating INT DEFAULT NULL;
