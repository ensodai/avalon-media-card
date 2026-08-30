CREATE UNIQUE INDEX user_affinity_vector_user_id ON user_affinity_vector (user_id);
CREATE UNIQUE INDEX media_people_person_id ON media_people (person_id);
CREATE UNIQUE INDEX user_media_bindings_user_id_media_id_source_type ON user_media_bindings (user_id, media_id, source_type);
CREATE UNIQUE INDEX users_username ON users (username);
CREATE UNIQUE INDEX media_metadata_cache_catalog_id_external_id_language ON media_metadata_cache (catalog_id, external_id, "language");
CREATE UNIQUE INDEX torrent_mappings_torrent_hash_file_path ON torrent_mappings (torrent_hash, file_path);

DROP INDEX IF EXISTS idx_media_episode_cache_season_id;
DROP INDEX IF EXISTS idx_user_episodes_user_cat_med_ep;
DROP INDEX IF EXISTS idx_user_media_sync_queue_status;
DROP INDEX IF EXISTS idx_media_season_cache_metadata_id;
DROP INDEX IF EXISTS idx_user_external_auth_user_service;
DROP INDEX IF EXISTS idx_media_ext_ids_movie_ext_source;
DROP INDEX IF EXISTS idx_sync_status_user_media_service;
DROP INDEX IF EXISTS idx_user_movies_user_catalog_media;
DROP INDEX IF EXISTS idx_media_keywords_keyword_id;
