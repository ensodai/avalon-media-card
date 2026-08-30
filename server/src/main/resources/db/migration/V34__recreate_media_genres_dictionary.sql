CREATE TABLE media_genre_dictionary_new
(
    genre_id      INT          NOT NULL,
    language_code VARCHAR(10)  NOT NULL,
    name          VARCHAR(255) NOT NULL,
    CONSTRAINT pk_media_genre_dictionary PRIMARY KEY (genre_id, language_code)
);

INSERT INTO media_genre_dictionary_new (genre_id, language_code, name)
SELECT genre_id, 'ru', name_ru
FROM media_genre_dictionary;

INSERT INTO media_genre_dictionary_new (genre_id, language_code, name)
SELECT genre_id, 'en', name_en
FROM media_genre_dictionary;

DROP TABLE media_genre_dictionary;
ALTER TABLE media_genre_dictionary_new RENAME TO media_genre_dictionary;
