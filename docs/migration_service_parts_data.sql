-- Миграция данных из старой структуры service_spare_parts в новую service_parts
-- Этот скрипт копирует существующие связи в новую таблицу с меткой о типе связи

-- Создаем временную таблицу для проверки
CREATE TABLE IF NOT EXISTS _migration_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    migration_name TEXT,
    source_count INTEGER,
    target_count INTEGER,
    migrated_count INTEGER,
    created_at TEXT
);

-- Сначала проверяем количество записей в старой таблице
INSERT INTO _migration_log (migration_name, source_count, migrated_at)
SELECT 'service_spare_parts_to_service_parts', COUNT(*), datetime('now')
FROM service_spare_parts;

-- Копируем данные в новую таблицу
-- Метка 'old' указывает, что это старая структура, 'new' - новая
INSERT INTO service_parts (service_id, spare_part_id, quantity, is_required, created_date)
SELECT 
    service_id,
    spare_part_id,
    quantity,
    1 AS is_required,  -- Считаем все старые связи обязательными
    datetime('now') AS created_date
FROM service_spare_parts
WHERE active = 1;  -- Только активные связи

-- Проверяем количество мигрированных записей
INSERT INTO _migration_log (migration_name, migrated_count, check_at)
SELECT 
    'service_spare_parts_to_service_parts',
    COUNT(*),
    datetime('now')
FROM service_parts
WHERE id IN (
    SELECT MAX(id) FROM service_parts GROUP BY service_id, spare_part_id
);

-- Выводим статистику
SELECT 
    'Старая таблица service_spare_parts:' AS info,
    COUNT(*) AS total_records,
    SUM(CASE WHEN active = 1 THEN 1 ELSE 0 END) AS active_records
FROM service_spare_parts;

SELECT 
    'Новая таблица service_parts после миграции:' AS info,
    COUNT(*) AS total_records,
    SUM(CASE WHEN is_required = 1 THEN 1 ELSE 0 END) AS required_records
FROM service_parts;

-- Если миграция прошла успешно, можно удалить старые данные
-- (выполнить только после проверки)
-- DELETE FROM service_spare_parts WHERE active = 1;
