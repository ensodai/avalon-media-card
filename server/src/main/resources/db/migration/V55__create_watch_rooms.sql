CREATE TABLE watch_rooms
(
    id                    VARCHAR(36)                         NOT NULL PRIMARY KEY,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    title                 VARCHAR(200)                        NOT NULL,
    media_id              VARCHAR(36)                         NOT NULL REFERENCES media (id) ON DELETE CASCADE,
    media_type            VARCHAR(20)                         NOT NULL,
    current_season        INT                                 NULL,
    current_episode       INT                                 NULL,
    last_position_seconds BIGINT    DEFAULT 0                 NOT NULL,
    host_user_id          VARCHAR(36)                         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    source_type           VARCHAR(64)                         NULL,
    source_id             TEXT                                NULL,
    join_pin              VARCHAR(10)                         NULL,
    control_mode          VARCHAR(20) DEFAULT 'HOST_ONLY'     NOT NULL,
    status                VARCHAR(20) DEFAULT 'ACTIVE'        NOT NULL,
    is_private            BOOLEAN   DEFAULT 0                 NOT NULL
);

CREATE INDEX IF NOT EXISTS watch_rooms_media_status ON watch_rooms (media_id, status);
CREATE INDEX IF NOT EXISTS watch_rooms_join_pin ON watch_rooms (join_pin);

CREATE TABLE watch_room_participants
(
    id         VARCHAR(36)                         NOT NULL PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    room_id    VARCHAR(36)                         NOT NULL REFERENCES watch_rooms (id) ON DELETE CASCADE,
    user_id    VARCHAR(36)                         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role       VARCHAR(20) DEFAULT 'MEMBER'        NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS watch_room_participant_unique ON watch_room_participants (room_id, user_id);
CREATE INDEX IF NOT EXISTS watch_room_participants_user_id ON watch_room_participants (user_id);
