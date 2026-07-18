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

-- Вставка клиентов
INSERT INTO clients (name, last_name, phone, car_model, car_number) VALUES
('Алексей', 'Смирнов', '+79001111111', 'Haval Jolion', 'А111ВС163'),
('Мария', 'Иванова', '+79002222222', 'Kia Rio', 'В222ЕЕ163'),
('Дмитрий', 'Петров', '+79003333333', 'Toyota Camry', 'С333ОО163'),
('Елена', 'Сидорова', '+79004444444', 'Ford Focus', 'Е444АА163'),
('Андрей', 'Кузнецов', '+79005555555', 'BMW X5', 'К555НН163'),
('Ольга', 'Соколова', '+79006666666', 'Mercedes E-Class', 'М666ВВ163'),
('Сергей', 'Попов', '+79007777777', 'Lexus RX', 'Р777СС163'),
('Татьяна', 'Васильева', '+79008888888', 'Audi Q7', 'А888ТТ163'),
('Максим', 'Морозов', '+79009999999', 'Hyundai Creta', 'Н999УУ163'),
('Анна', 'Федорова', '+79010000000', 'Nissan Qashqai', 'Т000ЕЕ163');

-- Вставка услуг
INSERT INTO services (name, price, duration) VALUES
('Замена масла', 1500, 60),
('Диагностика двигателя', 2000, 90),
('Замена фильтра', 500, 30),
('Ремонт тормозной системы', 5000, 180),
('Замена свечей зажигания', 800, 60),
('Балансировка колёс', 1000, 45),
('Диагностика трансмиссии', 2500, 90),
('Замена тормозных колодок', 3500, 120),
('Промывка системы охлаждения', 1200, 90),
('Замена ремня ГРМ', 4000, 240);

-- Вставка запчастей
INSERT INTO spare_parts (name, purchase_price, retail_price, stock, unit_type) VALUES
('Моторное масло 5W-30', 800, 1200, 50, 'л'),
('Масляный фильтр', 200, 400, 30, 'шт'),
('Воздушный фильтр', 150, 300, 25, 'шт'),
('Тормозные колодки передние', 2500, 4500, 20, 'компл'),
('Тормозные колодки задние', 2000, 3800, 15, 'компл'),
('Свечи зажигания platinum', 300, 600, 40, 'шт'),
('Антифриз 5л', 400, 700, 35, 'л'),
('Ремень ГРМ комплект', 3500, 6000, 10, 'компл'),
('Тормозная жидкость', 500, 900, 20, 'л'),
('Масло коробки передач', 1500, 2500, 15, 'л');

-- Вспомогательная таблица для генерации ID заказов
-- 20 заказов с начала года (закрытые)
INSERT INTO orders (id, client_id, status, total, created_date)
SELECT 
    'ZAK-' || printf('%02d', (id % 28) + 1) || '-01-26-' || printf('%04d', id),
    (id % 10) + 1,
    'Закрыт',
    1000 + (id * 500),
    printf('%02d', (id % 28) + 1) || '/01/2026'
FROM (
    SELECT 1 as id UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
    UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
);

-- Вставка услуг в первые 20 заказов
INSERT INTO order_services (order_id, service_name, price)
SELECT 
    'ZAK-' || printf('%02d', (o.id % 28) + 1) || '-01-26-' || printf('%04d', o.id),
    s.name,
    s.price
FROM (
    SELECT 1 as id UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
    UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
) o
CROSS JOIN (
    SELECT 1 as id, 'Замена масла' as name, 1500 as price UNION ALL
    SELECT 2, 'Диагностика двигателя', 2000 UNION ALL
    SELECT 3, 'Замена фильтра', 500 UNION ALL
    SELECT 4, 'Ремонт тормозной системы', 5000 UNION ALL
    SELECT 5, 'Замена свечей зажигания', 800
) s
WHERE (o.id + s.id) % 3 = 0;

-- Вставка запчастей в первые 20 заказов
INSERT INTO order_parts (order_id, part_name, price, quantity, unit_type)
SELECT 
    'ZAK-' || printf('%02d', (o.id % 28) + 1) || '-01-26-' || printf('%04d', o.id),
    p.name,
    p.retail_price,
    (o.id % 3) + 1,
    p.unit_type
FROM (
    SELECT 1 as id UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
    UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
) o
CROSS JOIN (
    SELECT 1 as id, 'Моторное масло 5W-30' as name, 1200 as retail_price, 'л' as unit_type UNION ALL
    SELECT 2, 'Масляный фильтр', 400, 'шт' UNION ALL
    SELECT 3, 'Воздушный фильтр', 300, 'шт' UNION ALL
    SELECT 4, 'Тормозные колодки передние', 4500, 'компл' UNION ALL
    SELECT 5, 'Свечи зажигания platinum', 600, 'шт'
) p
WHERE (o.id + p.id) % 4 = 0;

-- 10 заказов с сегодня до конца месяца (новые)
INSERT INTO orders (id, client_id, status, total, created_date)
SELECT 
    'ZAK-' || printf('%02d', (id % 15) + 18) || '-07-26-' || printf('%04d', id + 20),
    (id % 10) + 1,
    'Новый',
    1500 + (id * 300),
    printf('%02d', (id % 15) + 18) || '/07/2026'
FROM (
    SELECT 1 as id UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
);

-- Вставка услуг в последние 10 заказов
INSERT INTO order_services (order_id, service_name, price)
SELECT 
    'ZAK-' || printf('%02d', (o.id % 15) + 18) || '-07-26-' || printf('%04d', o.id + 20),
    s.name,
    s.price
FROM (
    SELECT 1 as id UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) o
CROSS JOIN (
    SELECT 1 as id, 'Замена масла' as name, 1500 as price UNION ALL
    SELECT 2, 'Диагностика двигателя', 2000 UNION ALL
    SELECT 3, 'Замена фильтра', 500 UNION ALL
    SELECT 4, 'Ремонт тормозной системы', 5000 UNION ALL
    SELECT 5, 'Замена свечей зажигания', 800
) s
WHERE (o.id + s.id) % 2 = 0;

-- Вставка запчастей в последние 10 заказов
INSERT INTO order_parts (order_id, part_name, price, quantity, unit_type)
SELECT 
    'ZAK-' || printf('%02d', (o.id % 15) + 18) || '-07-26-' || printf('%04d', o.id + 20),
    p.name,
    p.retail_price,
    (o.id % 4) + 1,
    p.unit_type
FROM (
    SELECT 1 as id UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) o
CROSS JOIN (
    SELECT 1 as id, 'Моторное масло 5W-30' as name, 1200 as retail_price, 'л' as unit_type UNION ALL
    SELECT 2, 'Масляный фильтр', 400, 'шт' UNION ALL
    SELECT 3, 'Воздушный фильтр', 300, 'шт' UNION ALL
    SELECT 4, 'Тормозные колодки передние', 4500, 'компл' UNION ALL
    SELECT 5, 'Свечи зажигания platinum', 600, 'шт'
) p
WHERE (o.id + p.id) % 3 = 0;

-- Создаем записи в календаре для всех заказов
INSERT INTO appointments (client_id, order_id, master_name, service_name, appointment_date, appointment_time, status)
SELECT 
    client_id,
    'ZAK-' || CASE 
        WHEN id <= 20 THEN printf('%02d', (id % 28) + 1) || '-01-26-' || printf('%04d', id)
        ELSE printf('%02d', (id % 15) + 18) || '-07-26-' || printf('%04d', id + 20)
    END,
    CASE (id % 4) 
        WHEN 0 THEN 'Иван'
        WHEN 1 THEN 'Петр'
        WHEN 2 THEN 'Сергей'
        WHEN 3 THEN 'Антон'
    END,
    'Обслуживание заказа ' || id,
    CASE 
        WHEN id <= 20 THEN printf('%02d', (id % 28) + 1) || '/01/2026'
        ELSE printf('%02d', (id % 15) + 18) || '/07/2026'
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
FROM (
    SELECT 1 as id UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
    UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
    UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25
    UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29 UNION ALL SELECT 30
);

-- Проверка результатов
SELECT 'Всего заказов:' AS info, COUNT(*) AS count FROM orders;
SELECT 'Закрытые заказы (с начала года):' AS info, COUNT(*) AS count FROM orders WHERE status = 'Закрыт';
SELECT 'Новые заказы (сегодня до конца месяца):' AS info, COUNT(*) AS count FROM orders WHERE status = 'Новый';
SELECT 'Записей в календаре:' AS info, COUNT(*) AS count FROM appointments;

-- Примеры данных
SELECT 'Пример заказа (закрытый):' AS info, id, client_id, status, total, created_date FROM orders WHERE status = 'Закрыт' LIMIT 3;
SELECT 'Пример заказа (новый):' AS info, id, client_id, status, total, created_date FROM orders WHERE status = 'Новый' LIMIT 3;
SELECT 'Пример записи:' AS info, client_id, order_id, master_name, service_name, appointment_date, appointment_time, status FROM appointments LIMIT 5;
