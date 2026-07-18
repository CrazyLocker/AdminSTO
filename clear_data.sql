-- Скрипт для создания 30 заказов с записями в календаре
-- 20 заказов с начала года (статус Закрыт)
-- 10 заказов с сегодня до конца месяца (статус Новый)
-- Формат ID: ZAK-01-01-26-0001

-- Удаляем существующие заказы и записи
DELETE FROM appointments;
DELETE FROM order_services;
DELETE FROM order_parts;
DELETE FROM orders;

-- Удаляем клиентов (оставим первые 10 для примера)
DELETE FROM clients WHERE id > 10;

-- Удаляем услуги (оставим первые 10 для примера)
DELETE FROM services WHERE id > 10;

-- Удаляем запчасти (оставим первые 10 для примера)
DELETE FROM spare_parts WHERE id > 10;

-- Проверка удаления
SELECT 'Orders count:' AS check_type, COUNT(*) AS count FROM orders;
SELECT 'Appointments count:' AS check_type, COUNT(*) AS count FROM appointments;
