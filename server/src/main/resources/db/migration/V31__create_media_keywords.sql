CREATE TABLE media_keywords
(
    media_type   VARCHAR(50)  NOT NULL,
    media_id     INT          NOT NULL,
    keyword_id   INT          NOT NULL,
    keyword_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (media_type, media_id, keyword_id)
);

-- Индекс для быстрого подсчета documentFrequency
CREATE INDEX idx_media_keywords_keyword_id ON media_keywords (keyword_id);
