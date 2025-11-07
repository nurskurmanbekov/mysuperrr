-- Создание таблицы департаментов
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Создание таблицы МРУ (межрегиональных управлений)
CREATE TABLE mrus (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(100) UNIQUE NOT NULL,
    department_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_mru_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE
);

-- Создание таблицы районов
CREATE TABLE districts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(100) UNIQUE NOT NULL,
    mru_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_district_mru FOREIGN KEY (mru_id) REFERENCES mrus(id) ON DELETE CASCADE
);

-- Индексы для оптимизации
CREATE INDEX idx_mrus_department_id ON mrus(department_id);
CREATE INDEX idx_districts_mru_id ON districts(mru_id);

-- Добавление поля district_id в таблицу clients
ALTER TABLE clients ADD COLUMN district_id BIGINT;
ALTER TABLE clients ADD CONSTRAINT fk_client_district FOREIGN KEY (district_id) REFERENCES districts(id) ON DELETE SET NULL;
CREATE INDEX idx_clients_district_id ON clients(district_id);

-- Добавление unique_id если отсутствует
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'clients' AND column_name = 'unique_id'
    ) THEN
        ALTER TABLE clients ADD COLUMN unique_id VARCHAR(255);
        CREATE INDEX idx_clients_unique_id ON clients(unique_id);
    END IF;
END $$;

-- Изменение INN на nullable если необходимо
ALTER TABLE clients ALTER COLUMN inn DROP NOT NULL;
