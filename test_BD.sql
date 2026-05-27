-- ============================================
-- ОЧИСТКА СТАРЫХ ДАННЫХ
-- ============================================
DELETE FROM order_parts;
DELETE FROM order_services;
DELETE FROM orders;
DELETE FROM appointments;
DELETE FROM spare_parts;
DELETE FROM services;
DELETE FROM clients;
DELETE FROM sqlite_sequence;

-- ============================================
-- 1. КЛИЕНТЫ (5 штук)
-- ============================================
INSERT INTO clients (name, phone, car_model, car_number) VALUES
('Иван Петров', '+79001234567', 'Toyota Camry', 'А123ВС163'),
('Елена Смирнова', '+79113456789', 'Hyundai Solaris', 'В456ЕК77'),
('Сергей Козлов', '+79224567890', 'Kia Rio', 'М789НО50'),
('Ольга Новикова', '+79335678901', 'Lada Vesta', 'О159РТ40'),
('Дмитрий Морозов', '+79446789012', 'Volkswagen Polo', 'У357АЕ99');

-- ============================================
-- 2. УСЛУГИ (5 штук)
-- ============================================
INSERT INTO services (name, price) VALUES
('Замена масла', 1500),
('Диагностика двигателя', 1200),
('Замена тормозных колодок', 2500),
('Шиномонтаж', 2000),
('Развал-схождение', 1800);

-- ============================================
-- 3. ЗАПЧАСТИ (5 штук)
-- ============================================
INSERT INTO spare_parts (name, purchase_price, retail_price, stock) VALUES
('Масло моторное 5W-30', 800, 1200, 15),
('Масляный фильтр', 200, 400, 20),
('Тормозные колодки передние', 1200, 2000, 8),
('Воздушный фильтр', 300, 600, 12),
('Свечи зажигания (комплект)', 400, 800, 10);

-- ============================================
-- 4. ЗАПИСИ В КАЛЕНДАРЕ (3 штуки)
-- ============================================

-- Запись №1: Иван Петров, 08:30, Иван
INSERT INTO appointments (client_id, order_id, master_name, service_name, appointment_date, appointment_time, status) 
VALUES (1, NULL, 'Иван', 'Замена масла', date('now'), '08:30', 'ЗАПЛАНИРОВАНО');

-- Запись №2: Елена Смирнова, 10:00, Иван
INSERT INTO appointments (client_id, order_id, master_name, service_name, appointment_date, appointment_time, status) 
VALUES (2, NULL, 'Иван', 'Диагностика двигателя', date('now'), '10:00', 'ЗАПЛАНИРОВАНО');

-- Запись №3: Сергей Козлов, 11:00, Петр
INSERT INTO appointments (client_id, order_id, master_name, service_name, appointment_date, appointment_time, status) 
VALUES (3, NULL, 'Петр', 'Шиномонтаж', date('now'), '11:00', 'ЗАПЛАНИРОВАНО');

-- ============================================
-- 5. ПРОВЕРКА (вывод статистики)
-- ============================================
SELECT 'Клиентов: ' || COUNT(*) FROM clients
UNION ALL
SELECT 'Услуг: ' || COUNT(*) FROM services
UNION ALL
SELECT 'Запчастей: ' || COUNT(*) FROM spare_parts
UNION ALL
SELECT 'Записей: ' || COUNT(*) FROM appointments;