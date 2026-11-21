-- V57: Add performance indexes for heatmap and activity lookups by last_watched_at
CREATE INDEX IF NOT EXISTS idx_user_movies_user_last_watched ON user_movies (user_id, last_watched_at);
CREATE INDEX IF NOT EXISTS idx_user_episodes_user_last_watched ON user_episodes (user_id, last_watched_at);
