-- Миграция: Создание таблицы service_parts
-- Эта таблица хранит связи между услугами и запчастями для автоматического добавления в заказы

-- Создание таблицы service_parts
CREATE TABLE IF NOT EXISTS service_parts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    service_id INTEGER NOT NULL,
    spare_part_id INTEGER NOT NULL,
    quantity REAL NOT NULL DEFAULT 1,
    is_required INTEGER NOT NULL DEFAULT 1,  -- 1 = обязательно, 0 = опционально
    created_date TEXT NOT NULL,
    
    FOREIGN KEY (service_id) REFERENCES services(id) ON DELETE CASCADE,
    FOREIGN KEY (spare_part_id) REFERENCES spare_parts(id) ON DELETE CASCADE
);

-- Индексы для оптимизации запросов
CREATE INDEX IF NOT EXISTS idx_service_parts_service_id ON service_parts(service_id);
CREATE INDEX IF NOT EXISTS idx_service_parts_spare_part_id ON service_parts(spare_part_id);

-- Проверка создания таблицы
SELECT 'Таблица service_parts создана успешно' as status;
