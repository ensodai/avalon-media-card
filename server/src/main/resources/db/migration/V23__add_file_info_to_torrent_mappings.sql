ALTER TABLE torrent_mappings
    ADD COLUMN file_index INTEGER NULL;
ALTER TABLE torrent_mappings
    ADD COLUMN file_size INTEGER NULL;
