-- Создание администратора по умолчанию
-- Пароль: admin123
-- BCrypt хеш для "admin123": $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cFQxg9NkYkHbFCJ6wMq3kPz3w3tB2

INSERT INTO users (inn, password_hash, unique_id, fcm_token, user_type, mru_id, attributes, created_at, updated_at)
VALUES (
    'admin_user',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cFQxg9NkYkHbFCJ6wMq3kPz3w3tB2',
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
