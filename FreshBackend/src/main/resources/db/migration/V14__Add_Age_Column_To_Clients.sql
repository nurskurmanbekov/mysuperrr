-- Добавление колонки age в таблицу clients
ALTER TABLE clients ADD COLUMN IF NOT EXISTS age INTEGER;

-- Комментарий для документации
COMMENT ON COLUMN clients.age IS 'Возраст клиента (вычисляется из birth_date, но хранится отдельно)';
