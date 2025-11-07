-- Создание таблицы геозон
CREATE TABLE geozones (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    client_id BIGINT NOT NULL,
    polygon_coordinates JSONB NOT NULL, -- Хранение координат полигона в формате JSON
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_geozone_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);

-- Создание таблицы нарушений геозон
CREATE TABLE geozone_violations (
    id BIGSERIAL PRIMARY KEY,
    geozone_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    violation_type VARCHAR(50) NOT NULL, -- 'EXIT' или 'ENTRY'
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    notification_sent BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_violation_geozone FOREIGN KEY (geozone_id) REFERENCES geozones(id) ON DELETE CASCADE,
    CONSTRAINT fk_violation_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);

-- Индексы для оптимизации
CREATE INDEX idx_geozones_client_id ON geozones(client_id);
CREATE INDEX idx_geozones_is_active ON geozones(is_active);
CREATE INDEX idx_geozone_violations_geozone_id ON geozone_violations(geozone_id);
CREATE INDEX idx_geozone_violations_client_id ON geozone_violations(client_id);
CREATE INDEX idx_geozone_violations_created_at ON geozone_violations(created_at);
