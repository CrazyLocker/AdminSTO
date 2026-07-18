-- Скрипт для создания 30 заказов с записями в календаре
-- 20 заказов с начала года (статус Закрыт)
-- 10 заказов с сегодня до конца месяца (статус Новый)

-- Вставка клиентов (если еще нет)
INSERT INTO clients (name, last_name, phone, car_model, car_plate) VALUES
('Алексей', 'Смирнов', '+79001111111', 'Haval Jolion', 'А111ВС163'),
('Мария', 'Иванова', '+79002222222', 'Kia Rio', 'В222ЕЕ163'),
('Дмитрий', 'Петров', '+79003333333', 'Toyota Camry', 'С333ОО163'),
('Елена', 'Сидорова', '+79004444444', 'Ford Focus', 'Е444АА163'),
('Андрей', 'Кузнецов', '+79005555555', 'BMW X5', 'К555НН163'),
('Ольга', 'Соколова', '+79006666666', 'Mercedes E-Class', 'М666ВВ163'),
('Сергей', 'Попов', '+79007777777', 'Lexus RX', 'Р777СС163'),
('Татьяна', 'Васильева', '+79008888888', 'Audi Q7', 'А888ТТ163'),
('Максим', 'Морозов', '+79009999999', 'Hyundai Creta', 'Н999УУ163'),
('Анна', 'Федорова', '+79010000000', 'Nissan Qashqai', 'Т000ЕЕ163')
ON CONFLICT DO NOTHING;

-- Вставка услуг (если еще нет)
INSERT INTO services (name, price) VALUES
('Замена масла', 1500),
('Диагностика двигателя', 2000),
('Замена фильтра', 500),
('Ремонт тормозной системы', 5000),
('Замена свечей зажигания', 800),
('Балансировка колёс', 1000),
('Диагностика трансмиссии', 2500),
('Замена тормозных колодок', 3500),
('Промывка системы охлаждения', 1200),
('Замена ремня ГРМ', 4000)
ON CONFLICT DO NOTHING;

-- Вставка запчастей (если еще нет)
INSERT INTO spare_parts (name, purchase_price, retail_price, stock) VALUES
('Моторное масло 5W-30', 800, 1200, 50),
('Масляный фильтр', 200, 400, 30),
('Воздушный фильтр', 150, 300, 25),
('Тормозные колодки передние', 2500, 4500, 20),
('Тормозные колодки задние', 2000, 3800, 15),
('Свечи зажигания platinum', 300, 600, 40),
('Антифриз 5л', 400, 700, 35),
('Ремень ГРМ комплект', 3500, 6000, 10),
('Тормозная жидкость', 500, 900, 20),
('Масло коробки передач', 1500, 2500, 15)
ON CONFLICT DO NOTHING;

-- Удаляем существующие заказы и записи
DELETE FROM appointments;
DELETE FROM order_services;
DELETE FROM order_parts;
DELETE FROM orders;

-- Сброс автоинкремента
DELETE FROM sqlite_sequence WHERE name='orders';

-- Вспомогательная таблица для генерации дат
-- Создаем 30 заказов

-- 20 заказов с начала года (закрытые)
INSERT INTO orders (client_id, master_name, status, created_date, total, description)
SELECT 
    (id % 10) + 1,
    CASE (id % 4) 
        WHEN 0 THEN 'Иван'
        WHEN 1 THEN 'Петр'
        WHEN 2 THEN 'Сергей'
        WHEN 3 THEN 'Антон'
    END,
    'Закрыт',
    '2026-' || printf('%02d', (id % 6) + 1) || '-' || printf('%02d', (id % 28) + 1),
    1000 + (id * 500),
    'Обслуживание автомобиля'
FROM (
    SELECT 1 as id UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
    UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
);

-- Вставляем услуги в первые 20 заказов
INSERT INTO order_services (order_id, service_name, price)
SELECT 
    o.id,
    s.name,
    s.price
FROM orders o
CROSS JOIN services s
WHERE o.id <= 20 AND (o.id + s.id) % 3 = 0;

-- Вставляем запчасти в первые 20 заказов
INSERT INTO order_parts (order_id, part_id, quantity)
SELECT 
    o.id,
    (p.id % 10) + 1,
    (o.id % 3) + 1
FROM orders o
CROSS JOIN spare_parts p
WHERE o.id <= 20 AND (o.id + p.id) % 4 = 0;

-- 10 заказов с сегодня до конца месяца (новые)
INSERT INTO orders (client_id, master_name, status, created_date, total, description)
SELECT 
    (id % 10) + 1,
    CASE (id % 4) 
        WHEN 0 THEN 'Иван'
        WHEN 1 THEN 'Петр'
        WHEN 2 THEN 'Сергей'
        WHEN 3 THEN 'Антон'
    END,
    'Новый',
    '2026-07-' || printf('%02d', (id % 15) + 18),
    1500 + (id * 300),
    'Текущее обслуживание'
FROM (
    SELECT 1 as id UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
);

-- Вставляем услуги в последние 10 заказов
INSERT INTO order_services (order_id, service_name, price)
SELECT 
    o.id,
    s.name,
    s.price
FROM orders o
CROSS JOIN services s
WHERE o.id > 20 AND (o.id + s.id) % 2 = 0;

-- Вставляем запчасти в последние 10 заказов
INSERT INTO order_parts (order_id, part_id, quantity)
SELECT 
    o.id,
    (p.id % 10) + 1,
    (o.id % 4) + 1
FROM orders o
CROSS JOIN spare_parts p
WHERE o.id > 20 AND (o.id + p.id) % 3 = 0;

-- Создаем записи в календаре для всех заказов
INSERT INTO appointments (client_id, order_id, master_name, service_name, date, time, status)
SELECT 
    client_id,
    id as order_id,
    master_name,
    'Обслуживание заказа ' || id,
    CASE 
        WHEN id <= 20 THEN '2026-' || printf('%02d', (id % 6) + 1) || '-' || printf('%02d', (id % 28) + 1)
        ELSE '2026-07-' || printf('%02d', (id % 15) + 18)
    END,
    CASE (id % 5)
        WHEN 0 THEN '09:00'
        WHEN 1 THEN '10:00'
        WHEN 2 THEN '11:00'
        WHEN 3 THEN '14:00'
        WHEN 4 THEN '15:00'
    END,
    CASE 
        WHEN id <= 20 THEN 'Выполнено'
        ELSE 'Новая'
    END
FROM orders;

-- Проверка результатов
SELECT 'Всего заказов:' AS info, COUNT(*) AS count FROM orders;
SELECT 'Закрытые заказы:' AS info, COUNT(*) AS count FROM orders WHERE status = 'Закрыт';
SELECT 'Новые заказы:' AS info, COUNT(*) AS count FROM orders WHERE status = 'Новый';
SELECT 'Записей в календаре:' AS info, COUNT(*) AS count FROM appointments;
