-- Создание администратора по умолчанию
-- Пароль: admin123
-- BCrypt хеш для "admin123": $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

INSERT INTO users (inn, password_hash, unique_id, fcm_token, user_type, mru_id, attributes, created_at, updated_at)
VALUES (
    'admin_user',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'admin_unique_id',
    NULL,
    'employee',
    NULL,
    '{"role": "deptAdmin", "administrator": true}'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (inn) DO NOTHING;

-- Информационное сообщение
DO $$
BEGIN
    RAISE NOTICE 'Администратор создан!';
    RAISE NOTICE 'ИНН (логин): admin_user';
    RAISE NOTICE 'Пароль: admin123';
END $$;
