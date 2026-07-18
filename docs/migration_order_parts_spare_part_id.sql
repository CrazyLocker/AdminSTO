-- ==================================================================
-- МИГРАЦИЯ БД: Удаление заказов + структурные изменения
-- Дата: 2026-07-16
-- 
-- ВНИМАНИЕ: Резервная копия БД уже сделана!
-- Скрипт выполнять в DB Browser (SQLite)
-- ==================================================================

-- ====== 1. УДАЛЕНИЕ ВСЕХ ЗАКАЗОВ И СВЯЗАННЫХ ДАННЫХ ======
-- Порядок важен: сначала дочерние таблицы, потом родительская

-- Удаляем запчасти из заказов
DELETE FROM order_parts;

-- Удаляем услуги из заказов
DELETE FROM order_services;

-- Обнуляем ссылки на заказы в записях календаря (сами записи оставляем)
UPDATE appointments SET order_id = NULL WHERE order_id IS NOT NULL;

-- Удаляем заказы
DELETE FROM orders;

-- ====== 2. СТРУКТУРНЫЕ ИЗМЕНЕНИЯ: order_parts ======
-- spare_part_id — FK на spare_parts (главное исправление)
ALTER TABLE order_parts ADD COLUMN spare_part_id INTEGER DEFAULT 0;

-- unit_type — снапшот типа единицы измерения на момент заказа
ALTER TABLE order_parts ADD COLUMN unit_type TEXT DEFAULT 'шт';

-- purchase_price — снапшот закупочной цены для расчёта прибыли
ALTER TABLE order_parts ADD COLUMN purchase_price REAL DEFAULT 0;

-- ====== 3. СТРУКТУРНЫЕ ИЗМЕНЕНИЯ: orders ======
-- closed_date — дата закрытия заказа
ALTER TABLE orders ADD COLUMN closed_date TEXT DEFAULT '';

-- notes — примечания к заказу
ALTER TABLE orders ADD COLUMN notes TEXT DEFAULT '';

-- ====== 4. ИНДЕКСЫ ДЛЯ НОВЫХ КОЛОНОК ======
CREATE INDEX IF NOT EXISTS idx_order_parts_spare_part_id ON order_parts(spare_part_id);

-- ====== 5. ОПТИМИЗАЦИЯ ======
-- Сжать файл БД после удаления данных
VACUUM;

-- ====== ПРОВЕРКА ======
-- Убедитесь, что:
-- SELECT COUNT(*) FROM orders;          -- должно быть 0
-- SELECT COUNT(*) FROM order_parts;      -- должно быть 0
-- SELECT COUNT(*) FROM order_services;   -- должно быть 0
-- SELECT COUNT(*) FROM clients;          -- данные сохранены
-- SELECT COUNT(*) FROM services;         -- данные сохранены
-- SELECT COUNT(*) FROM spare_parts;      -- данные сохранены
-- PRAGMA table_info(order_parts);        -- должны быть новые колонки
-- PRAGMA table_info(orders);             -- должны быть новые колонки
