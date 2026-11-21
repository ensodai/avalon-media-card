-- V56: Add performance indexes for media credits, genres, user history and people lookups
CREATE INDEX IF NOT EXISTS idx_credits_media_id ON credits (media_id);
CREATE INDEX IF NOT EXISTS idx_credits_person_id_role ON credits (person_id, role);
CREATE INDEX IF NOT EXISTS idx_media_genres_media_id ON media_genres (media_id);
CREATE INDEX IF NOT EXISTS idx_media_genres_genre_id ON media_genres (genre_id);
CREATE INDEX IF NOT EXISTS idx_user_movies_user_status ON user_movies (user_id, status);
CREATE INDEX IF NOT EXISTS idx_user_movies_user_type ON user_movies (user_id, media_type);
CREATE INDEX IF NOT EXISTS idx_user_episodes_user_watched ON user_episodes (user_id, is_watched);
CREATE INDEX IF NOT EXISTS idx_media_images_person_type ON media_images (person_id, image_type);
CREATE INDEX IF NOT EXISTS idx_person_translations_person_lang ON person_translations (person_id, language);
