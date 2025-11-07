-- V4__Add_UserType_And_MruId_To_Users.sql

-- Добавляем колонки в таблицу users
ALTER TABLE users
    ADD COLUMN user_type VARCHAR(20) NOT NULL DEFAULT 'employee', -- 'employee' или 'probationer'
ADD COLUMN mru_id VARCHAR(255); -- ID МРУ/района

-- (Опционально) Добавим индекс для ускорения поиска по mru_id
CREATE INDEX idx_users_mru_id ON users (mru_id);

-- (Опционально) Добавим ограничение для проверки значений user_type
-- ALTER TABLE users ADD CONSTRAINT chk_user_type CHECK (user_type IN ('employee', 'probationer'));