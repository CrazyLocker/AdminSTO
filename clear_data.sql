-- Скрипт для очистки заказов и записей календаря
-- Запускается перед seed_30_orders.sql

-- Удаляем существующие заказы и записи
DELETE FROM appointments;
DELETE FROM order_services;
DELETE FROM order_parts;
DELETE FROM orders;

-- Проверка
SELECT 'Orders remaining:' AS check_type, COUNT(*) AS count FROM orders;
SELECT 'Appointments remaining:' AS check_type, COUNT(*) AS count FROM appointments;
