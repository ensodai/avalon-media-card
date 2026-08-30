CREATE TABLE IF NOT EXISTS torrent_mappings
(
    id
    UUID
    PRIMARY
    KEY,
    torrent_hash
    VARCHAR
(
    64
) NOT NULL,
    file_path TEXT NOT NULL,
    seasons TEXT,
    episodes TEXT,
    is_absolute BOOLEAN DEFAULT FALSE NOT NULL,
    is_manual BOOLEAN DEFAULT FALSE NOT NULL,
    UNIQUE
(
    torrent_hash,
    file_path
)
    );
CREATE INDEX IF NOT EXISTS idx_torrent_mappings_torrent_hash ON torrent_mappings(torrent_hash);
