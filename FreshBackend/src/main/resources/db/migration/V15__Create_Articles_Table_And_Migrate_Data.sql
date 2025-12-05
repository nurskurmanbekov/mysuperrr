-- Создание таблицы articles для хранения множественных статей осуждения
CREATE TABLE IF NOT EXISTS articles (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    article VARCHAR(255),
    part VARCHAR(255),
    point VARCHAR(255),
    CONSTRAINT fk_articles_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);

-- Создание индекса для быстрого поиска статей по клиенту
CREATE INDEX idx_articles_client_id ON articles(client_id);

-- Миграция существующих данных из clients в таблицу articles
-- Только если в старых полях что-то заполнено
INSERT INTO articles (client_id, article, part, point)
SELECT id, article, part, point
FROM clients
WHERE article IS NOT NULL OR part IS NOT NULL OR point IS NOT NULL;

-- Удаление старых колонок code, article, part, point из таблицы clients
ALTER TABLE clients DROP COLUMN IF EXISTS code;
ALTER TABLE clients DROP COLUMN IF EXISTS article;
ALTER TABLE clients DROP COLUMN IF EXISTS part;
ALTER TABLE clients DROP COLUMN IF EXISTS point;

-- Комментарии для документирования
COMMENT ON TABLE articles IS 'Таблица для хранения множественных статей осуждения клиентов';
COMMENT ON COLUMN articles.client_id IS 'ID клиента (внешний ключ на clients.id)';
COMMENT ON COLUMN articles.article IS 'Номер статьи УК (например, 158)';
COMMENT ON COLUMN articles.part IS 'Часть статьи (например, 3)';
COMMENT ON COLUMN articles.point IS 'Пункт статьи (например, а, б)';
