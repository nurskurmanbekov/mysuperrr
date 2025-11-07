CREATE TABLE clients (
                         id BIGSERIAL PRIMARY KEY,
                         fio VARCHAR(512) NOT NULL,
                         inn VARCHAR(255) NOT NULL UNIQUE, -- Дублируем ИНН
                         identifier VARCHAR(255),
                         unit VARCHAR(255),
                         obs_type VARCHAR(255),
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                         updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    -- Дополнительные поля
                         birth_date DATE,
                         sex CHAR(1), -- 'М' или 'Ж'
                         passport VARCHAR(255),
                         reg_address TEXT,
                         fact_address TEXT,
                         contact1 VARCHAR(255),
                         contact2 VARCHAR(255),
                         erp_number VARCHAR(255),
                         obs_start DATE,
                         obs_end DATE,
                         degree VARCHAR(255),
                         ud_number VARCHAR(255),
                         code VARCHAR(255),
                         article VARCHAR(255),
                         part VARCHAR(255),
                         point VARCHAR(255),
                         extra_info TEXT,
                         measures TEXT,
                         app_password VARCHAR(255) NOT NULL, -- Пароль для приложения
                         photo_key VARCHAR(255) -- Ключ для фото
);

-- Индекс для поиска по ИНН
CREATE INDEX idx_clients_inn ON clients (inn);

-- Индекс для поиска по идентификатору
CREATE INDEX idx_clients_identifier ON clients (identifier);