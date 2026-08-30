ALTER TABLE torrent_mappings RENAME TO source_mappings;
ALTER TABLE source_mappings RENAME COLUMN torrent_hash TO source_id;
ALTER TABLE source_mappings RENAME COLUMN file_path TO item_key;
ALTER TABLE source_mappings ADD COLUMN source_type VARCHAR(64) DEFAULT 'torrserver' NOT NULL;
ALTER TABLE source_mappings ADD COLUMN stream_url TEXT;
ALTER TABLE source_mappings ADD COLUMN quality VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_source_mappings_source_id ON source_mappings(source_id);
CREATE INDEX IF NOT EXISTS idx_source_mappings_media_id ON source_mappings(media_id);
CREATE INDEX IF NOT EXISTS idx_source_mappings_source_type ON source_mappings(source_type);
