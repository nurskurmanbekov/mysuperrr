-- V5__Add_Attributes_To_Users.sql

-- Добавляем колонку attributes в таблицу users
ALTER TABLE users
    ADD COLUMN attributes jsonb; -- Тип данных jsonb для PostgreSQL