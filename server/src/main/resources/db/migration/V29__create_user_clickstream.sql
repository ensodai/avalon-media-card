CREATE TABLE user_clickstream
(
    id            UUID PRIMARY KEY,
    user_id       UUID                                NOT NULL,
    event_type    VARCHAR(50)                         NOT NULL,
    target_type   VARCHAR(50),
    target_id     VARCHAR(100),
    context       VARCHAR(50)                         NOT NULL,
    dwell_time_ms BIGINT    DEFAULT 0                 NOT NULL,
    payload       TEXT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_user_clickstream_user_id ON user_clickstream (user_id);
