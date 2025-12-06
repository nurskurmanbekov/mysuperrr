-- Таблица для хранения GPS позиций устройств
-- Backend сам хранит координаты, чтобы Frontend не зависел от Traccar

CREATE TABLE device_positions (
    id BIGSERIAL PRIMARY KEY,

    -- Связь с клиентом
    device_id BIGINT NOT NULL,
    unique_id VARCHAR(50) NOT NULL,

    -- GPS координаты
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    speed DOUBLE PRECISION DEFAULT 0,
    bearing DOUBLE PRECISION DEFAULT 0,
    altitude DOUBLE PRECISION DEFAULT 0,
    accuracy DOUBLE PRECISION DEFAULT 0,
    battery DOUBLE PRECISION DEFAULT 0,

    -- Временные метки
    timestamp TIMESTAMP NOT NULL,
    server_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Статус
    status VARCHAR(20) NOT NULL DEFAULT 'online',

    -- Интеграция с Traccar
    sent_to_traccar BOOLEAN NOT NULL DEFAULT false,
    traccar_position_id BIGINT,

    -- Внешний ключ
    FOREIGN KEY (device_id) REFERENCES clients(id) ON DELETE CASCADE
);

-- Индексы для быстрого поиска
CREATE INDEX idx_device_positions_device_id ON device_positions(device_id);
CREATE INDEX idx_device_positions_unique_id ON device_positions(unique_id);
CREATE INDEX idx_device_positions_timestamp ON device_positions(timestamp DESC);
CREATE INDEX idx_device_positions_server_time ON device_positions(server_time DESC);

-- Комментарии к таблице
COMMENT ON TABLE device_positions IS 'Хранение GPS позиций устройств. Backend - единый источник данных для Frontend';
COMMENT ON COLUMN device_positions.device_id IS 'ID клиента в системе';
COMMENT ON COLUMN device_positions.unique_id IS 'Уникальный ID устройства (ИНН)';
COMMENT ON COLUMN device_positions.timestamp IS 'Время получения GPS данных от устройства';
COMMENT ON COLUMN device_positions.server_time IS 'Время получения данных сервером';
COMMENT ON COLUMN device_positions.status IS 'Статус: online (< 5 мин) или offline';
COMMENT ON COLUMN device_positions.sent_to_traccar IS 'Флаг успешной отправки в Traccar';
