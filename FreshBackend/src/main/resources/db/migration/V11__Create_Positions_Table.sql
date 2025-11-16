-- Создание таблицы positions для хранения GPS координат
CREATE TABLE positions (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    altitude DOUBLE PRECISION,
    speed DOUBLE PRECISION,
    accuracy DOUBLE PRECISION,
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_positions_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);

CREATE INDEX idx_positions_client_timestamp ON positions(client_id, timestamp);

-- Добавляем тестовые точки GPS для демонстрации воспроизведения
-- Симулируем маршрут от дома до работы в течение 30 минут
INSERT INTO positions (client_id, latitude, longitude, speed, accuracy, timestamp)
SELECT
    c.id,
    43.2566 + (seq * 0.001), -- Движение на север
    76.9286 + (seq * 0.0015), -- Движение на восток
    CASE
        WHEN seq < 5 THEN 0 -- Стоит
        WHEN seq < 25 THEN 20 + RANDOM() * 10 -- Едет
        ELSE 0 -- Остановился
    END as speed,
    10.0 + RANDOM() * 5 as accuracy,
    CURRENT_TIMESTAMP - INTERVAL '30 minutes' + (seq * INTERVAL '1 minute') as timestamp
FROM
    clients c,
    generate_series(0, 30) as seq
WHERE c.unique_id = 'test_client_001'
LIMIT 31;

COMMENT ON TABLE positions IS 'GPS координаты осужденных с временными метками';
