ALTER TABLE media
    ADD COLUMN imdb_id VARCHAR(30) DEFAULT NULL;

CREATE INDEX idx_media_imdb_id ON media(imdb_id);
