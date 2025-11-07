CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       inn VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       unique_id VARCHAR(255) NOT NULL UNIQUE, -- Связь с Traccar
                       fcm_token VARCHAR(512),
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Индекс для поиска по ИНН
CREATE INDEX idx_users_inn ON users (inn);

-- Индекс для поиска по uniqueId
CREATE INDEX idx_users_unique_id ON users (unique_id);