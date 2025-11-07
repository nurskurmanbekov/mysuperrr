-- Скрипт для создания администратора вручную
-- Запустите этот скрипт в pgAdmin или через psql

-- Подключитесь к базе данных probationmob:
-- psql -U postgres -d probationmob -f create_admin.sql

-- Создание администратора
-- ИНН (логин): admin_user
-- Пароль: admin123

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

-- Проверка
SELECT id, inn, user_type, attributes FROM users WHERE inn = 'admin_user';

-- Вывод информации
SELECT 'Администратор успешно создан!' as message;
SELECT 'ИНН (логин): admin_user' as login_info;
SELECT 'Пароль: admin123' as password_info;
