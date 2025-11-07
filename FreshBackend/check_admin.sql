-- Проверка существования администратора
SELECT
    id,
    inn,
    password_hash,
    unique_id,
    user_type,
    attributes
FROM users
WHERE inn = 'admin_user';
