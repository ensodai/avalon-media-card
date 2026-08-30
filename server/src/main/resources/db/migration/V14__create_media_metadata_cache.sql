CREATE TABLE IF NOT EXISTS media_metadata_cache
(
    id
    TEXT
    PRIMARY
    KEY,
    catalog_id
    VARCHAR
(
    50
) NOT NULL,
    external_id VARCHAR
(
    100
) NOT NULL,
    title VARCHAR
(
    255
) NOT NULL,
    overview TEXT,
    poster_url TEXT,
    backdrop_url TEXT,
    rating DOUBLE,
    metadata_json TEXT,
    cached_at TIMESTAMP NOT NULL,
    UNIQUE
(
    catalog_id,
    external_id
)
    );
