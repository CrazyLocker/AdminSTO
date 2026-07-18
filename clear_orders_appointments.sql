-- Скрипт для очистки всех заказов и записей в календаре
-- Выполняет очистку с соблюдением внешних ключей

-- Удаление всех записей в календаре
DELETE FROM appointments;

-- Удаление всех заказов (和服务和部分的关系表)
DELETE FROM order_services;
DELETE FROM order_parts;

-- Удаление самих заказов
DELETE FROM orders;

-- Проверка удаления (должны вернуть 0 строк)
SELECT 'Orders count:', COUNT(*) FROM orders;
SELECT 'Appointments count:', COUNT(*) FROM appointments;
SELECT 'Order services count:', COUNT(*) FROM order_services;
SELECT 'Order parts count:', COUNT(*) FROM order_parts;
