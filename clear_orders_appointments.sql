-- Скрипт для очистки всех заказов и записей в календаре
-- Выполняет очистку с соблюдением внешних ключей
-- Для DB Browser SQLite

-- Удаление всех записей в календаре
DELETE FROM appointments;

-- Удаление всех записей о услугах в заказах
DELETE FROM order_services;

-- Удаление всех записей о запчастях в заказах
DELETE FROM order_parts;

-- Удаление самих заказов
DELETE FROM orders;

-- Проверка удаления (должны вернуть 0 строк)
SELECT 'Orders count:' AS check_type, COUNT(*) AS count FROM orders;
SELECT 'Appointments count:' AS check_type, COUNT(*) AS count FROM appointments;
SELECT 'Order services count:' AS check_type, COUNT(*) AS count FROM order_services;
SELECT 'Order parts count:' AS check_type, COUNT(*) AS count FROM order_parts;
