-- Обновление пароля администратора
-- Этот скрипт обновляет пароль на "admin123"
-- Хеш сгенерирован с помощью BCrypt (сила 10)

UPDATE users
SET password_hash = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cFQxg9NkYkHbFCJ6wMq3kPz3w3tB2',
    updated_at = CURRENT_TIMESTAMP
WHERE inn = 'admin_user';

-- Проверка результата
SELECT
    inn,
    user_type,
    attributes,
    'Пароль обновлен!' as status
FROM users
WHERE inn = 'admin_user';
