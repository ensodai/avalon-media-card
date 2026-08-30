ALTER TABLE torrent_mappings
    ADD COLUMN media_id VARCHAR(100);
CREATE INDEX IF NOT EXISTS idx_torrent_mappings_media_id ON torrent_mappings(media_id);
