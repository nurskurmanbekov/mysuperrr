CREATE TABLE face_check_events (
                                   id BIGSERIAL PRIMARY KEY,
                                   check_id VARCHAR(255), -- Связь с серверным заданием
                                   user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE, -- FK на users
                                   device_id BIGINT, -- ID устройства из Traccar (опционально)
                                   outcome VARCHAR(50) NOT NULL, -- 'ok', 'failed', 'declined', 'failed_network', 'late_ok', 'late_failed'
                                   taken_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                   distance DOUBLE PRECISION,
                                   deadline_iso VARCHAR(255),
                                   app_version VARCHAR(50),
                                   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Индекс для поиска по user_id
CREATE INDEX idx_face_events_user_id ON face_check_events (user_id);

-- Индекс для поиска по device_id
CREATE INDEX idx_face_events_device_id ON face_check_events (device_id);

-- Индекс для поиска по check_id
CREATE INDEX idx_face_events_check_id ON face_check_events (check_id);

-- Индекс для поиска по времени (важно для /events)
CREATE INDEX idx_face_events_taken_at ON face_check_events (taken_at);