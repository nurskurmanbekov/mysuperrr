-- Вставка центрального департамента
INSERT INTO departments (name, code) VALUES ('Центральный департамент', 'CENTRAL');

-- Вставка 4 МРУ
INSERT INTO mrus (name, code, department_id) VALUES
    ('МРУ Север', 'MRU_NORTH', (SELECT id FROM departments WHERE code = 'CENTRAL')),
    ('МРУ Юг', 'MRU_SOUTH', (SELECT id FROM departments WHERE code = 'CENTRAL')),
    ('МРУ Восток', 'MRU_EAST', (SELECT id FROM departments WHERE code = 'CENTRAL')),
    ('МРУ Запад', 'MRU_WEST', (SELECT id FROM departments WHERE code = 'CENTRAL'));

-- Вставка примерно 50 районов (по 12-13 на каждое МРУ)
-- МРУ Север
INSERT INTO districts (name, code, mru_id) VALUES
    ('Северный район 1', 'NORTH_D01', (SELECT id FROM mrus WHERE code = 'MRU_NORTH')),
    ('Северный район 2', 'NORTH_D02', (SELECT id FROM mrus WHERE code = 'MRU_NORTH')),
    ('Северный район 3', 'NORTH_D03', (SELECT id FROM mrus WHERE code = 'MRU_NORTH')),
    ('Северный район 4', 'NORTH_D04', (SELECT id FROM mrus WHERE code = 'MRU_NORTH')),
    ('Северный район 5', 'NORTH_D05', (SELECT id FROM mrus WHERE code = 'MRU_NORTH')),
    ('Северный район 6', 'NORTH_D06', (SELECT id FROM mrus WHERE code = 'MRU_NORTH')),
    ('Северный район 7', 'NORTH_D07', (SELECT id FROM mrus WHERE code = 'MRU_NORTH')),
    ('Северный район 8', 'NORTH_D08', (SELECT id FROM mrus WHERE code = 'MRU_NORTH')),
    ('Северный район 9', 'NORTH_D09', (SELECT id FROM mrus WHERE code = 'MRU_NORTH')),
    ('Северный район 10', 'NORTH_D10', (SELECT id FROM mrus WHERE code = 'MRU_NORTH')),
    ('Северный район 11', 'NORTH_D11', (SELECT id FROM mrus WHERE code = 'MRU_NORTH')),
    ('Северный район 12', 'NORTH_D12', (SELECT id FROM mrus WHERE code = 'MRU_NORTH')),
    ('Северный район 13', 'NORTH_D13', (SELECT id FROM mrus WHERE code = 'MRU_NORTH'));

-- МРУ Юг
INSERT INTO districts (name, code, mru_id) VALUES
    ('Южный район 1', 'SOUTH_D01', (SELECT id FROM mrus WHERE code = 'MRU_SOUTH')),
    ('Южный район 2', 'SOUTH_D02', (SELECT id FROM mrus WHERE code = 'MRU_SOUTH')),
    ('Южный район 3', 'SOUTH_D03', (SELECT id FROM mrus WHERE code = 'MRU_SOUTH')),
    ('Южный район 4', 'SOUTH_D04', (SELECT id FROM mrus WHERE code = 'MRU_SOUTH')),
    ('Южный район 5', 'SOUTH_D05', (SELECT id FROM mrus WHERE code = 'MRU_SOUTH')),
    ('Южный район 6', 'SOUTH_D06', (SELECT id FROM mrus WHERE code = 'MRU_SOUTH')),
    ('Южный район 7', 'SOUTH_D07', (SELECT id FROM mrus WHERE code = 'MRU_SOUTH')),
    ('Южный район 8', 'SOUTH_D08', (SELECT id FROM mrus WHERE code = 'MRU_SOUTH')),
    ('Южный район 9', 'SOUTH_D09', (SELECT id FROM mrus WHERE code = 'MRU_SOUTH')),
    ('Южный район 10', 'SOUTH_D10', (SELECT id FROM mrus WHERE code = 'MRU_SOUTH')),
    ('Южный район 11', 'SOUTH_D11', (SELECT id FROM mrus WHERE code = 'MRU_SOUTH')),
    ('Южный район 12', 'SOUTH_D12', (SELECT id FROM mrus WHERE code = 'MRU_SOUTH'));

-- МРУ Восток
INSERT INTO districts (name, code, mru_id) VALUES
    ('Восточный район 1', 'EAST_D01', (SELECT id FROM mrus WHERE code = 'MRU_EAST')),
    ('Восточный район 2', 'EAST_D02', (SELECT id FROM mrus WHERE code = 'MRU_EAST')),
    ('Восточный район 3', 'EAST_D03', (SELECT id FROM mrus WHERE code = 'MRU_EAST')),
    ('Восточный район 4', 'EAST_D04', (SELECT id FROM mrus WHERE code = 'MRU_EAST')),
    ('Восточный район 5', 'EAST_D05', (SELECT id FROM mrus WHERE code = 'MRU_EAST')),
    ('Восточный район 6', 'EAST_D06', (SELECT id FROM mrus WHERE code = 'MRU_EAST')),
    ('Восточный район 7', 'EAST_D07', (SELECT id FROM mrus WHERE code = 'MRU_EAST')),
    ('Восточный район 8', 'EAST_D08', (SELECT id FROM mrus WHERE code = 'MRU_EAST')),
    ('Восточный район 9', 'EAST_D09', (SELECT id FROM mrus WHERE code = 'MRU_EAST')),
    ('Восточный район 10', 'EAST_D10', (SELECT id FROM mrus WHERE code = 'MRU_EAST')),
    ('Восточный район 11', 'EAST_D11', (SELECT id FROM mrus WHERE code = 'MRU_EAST')),
    ('Восточный район 12', 'EAST_D12', (SELECT id FROM mrus WHERE code = 'MRU_EAST')),
    ('Восточный район 13', 'EAST_D13', (SELECT id FROM mrus WHERE code = 'MRU_EAST'));

-- МРУ Запад
INSERT INTO districts (name, code, mru_id) VALUES
    ('Западный район 1', 'WEST_D01', (SELECT id FROM mrus WHERE code = 'MRU_WEST')),
    ('Западный район 2', 'WEST_D02', (SELECT id FROM mrus WHERE code = 'MRU_WEST')),
    ('Западный район 3', 'WEST_D03', (SELECT id FROM mrus WHERE code = 'MRU_WEST')),
    ('Западный район 4', 'WEST_D04', (SELECT id FROM mrus WHERE code = 'MRU_WEST')),
    ('Западный район 5', 'WEST_D05', (SELECT id FROM mrus WHERE code = 'MRU_WEST')),
    ('Западный район 6', 'WEST_D06', (SELECT id FROM mrus WHERE code = 'MRU_WEST')),
    ('Западный район 7', 'WEST_D07', (SELECT id FROM mrus WHERE code = 'MRU_WEST')),
    ('Западный район 8', 'WEST_D08', (SELECT id FROM mrus WHERE code = 'MRU_WEST')),
    ('Западный район 9', 'WEST_D09', (SELECT id FROM mrus WHERE code = 'MRU_WEST')),
    ('Западный район 10', 'WEST_D10', (SELECT id FROM mrus WHERE code = 'MRU_WEST')),
    ('Западный район 11', 'WEST_D11', (SELECT id FROM mrus WHERE code = 'MRU_WEST')),
    ('Западный район 12', 'WEST_D12', (SELECT id FROM mrus WHERE code = 'MRU_WEST'));
