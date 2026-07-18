-- ==================================================================
-- ЗАПОЛНЕНИЕ БАЗЫ ДАННЫХ — 30 ЗАКАЗОВ
-- Формат ID: ZAK-ДД-ММ-26-NNNN (день-месяц-год-номер)
--
-- 20 заказов со статуса "Закрыт" (01.01.2026 — 17.07.2026)
-- 10 заказов со статуса "Новый" (18.07.2026 — 31.07.2026)
-- Для каждого заказа создаётся запись в календаре (appointments)
-- ==================================================================

-- ==================================================================
-- ОЧИСТКА СУЩЕСТВУЮЩИХ ДАННЫХ
-- ==================================================================
DELETE FROM appointments;
DELETE FROM order_services;
DELETE FROM order_parts;
DELETE FROM orders;

-- ==================================================================
-- ЧАСТЬ 1: 20 ЗАКРЫТЫХ ЗАКАЗОВ (01.01.2026 — 17.07.2026)
-- ==================================================================

-- 1. ZAK-01-01-26-0001 (01.01.2026) — клиент 1, замена масла + фильтр
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-01-01-26-0001', 1, 'Закрыт', 4800, '01/01/2026', '03/01/2026', 'Плановое ТО');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-01-01-26-0001', 'Замена масла ДВС', 1500, 1);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-01-01-26-0001', 'Моторное масло 5W-30 4л', 3200, 1, 1, 'л', 2200),
('ZAK-01-01-26-0001', 'Фильтр масляный Haval', 650, 1, 2, 'шт', 400);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(1, 'ZAK-01-01-26-0001', 'Иванов Иван Петрович', 'Замена масла ДВС', 1, '01/01/2026', '10:00', 'Завершён');

-- 2. ZAK-05-01-26-0002 (05.01.2026) — клиент 2, замена колодок
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-05-01-26-0002', 2, 'Закрыт', 5300, '05/01/2026', '07/01/2026', 'Стук при торможении');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-05-01-26-0002', 'Замена тормозных колодок (перед)', 3200, 3);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-05-01-26-0002', 'Колодки тормозные перед', 2100, 1, 4, 'компл', 1200);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(2, 'ZAK-05-01-26-0002', 'Иванов Иван Петрович', 'Замена тормозных колодок (перед)', 3, '05/01/2026', '11:00', 'Завершён');

-- 3. ZAK-12-01-26-0003 (12.01.2026) — клиент 3, диагностика
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-12-01-26-0003', 3, 'Закрыт', 3200, '12/01/2026', '14/01/2026', 'Проверка перед продажей');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-12-01-26-0003', 'Компьютерная диагностика', 2000, 9),
('ZAK-12-01-26-0003', 'Диагностика ходовой', 1200, 5);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(3, 'ZAK-12-01-26-0003', 'Иванов Иван Петрович', 'Компьютерная диагностика', 9, '12/01/2026', '09:00', 'Завершён');

-- 4. ZAK-20-01-26-0004 (20.01.2026) — клиент 4, шиномонтаж
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-20-01-26-0004', 4, 'Закрыт', 2400, '20/01/2026', '20/01/2026', 'Сезонная смена');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-20-01-26-0004', 'Шиномонтаж R16', 2400, 11);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(4, 'ZAK-20-01-26-0004', 'Петров Сергей Алексеевич', 'Шиномонтаж R16', 11, '20/01/2026', '14:00', 'Завершён');

-- 5. ZAK-02-02-26-0005 (02.02.2026) — клиент 5, замена свечей
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-02-02-26-0005', 5, 'Закрыт', 3000, '02/02/2026', '02/02/2026', 'Потеря мощности');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-02-02-26-0005', 'Замена свечей зажигания', 1600, 8);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-02-02-26-0005', 'Свечи зажигания NGK', 1400, 1, 7, 'компл', 800);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(5, 'ZAK-02-02-26-0005', 'Иванов Иван Петрович', 'Замена свечей зажигания', 8, '02/02/2026', '10:30', 'Завершён');

-- 6. ZAK-15-02-26-0006 (15.02.2026) — клиент 6, замена антифриза
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-15-02-26-0006', 6, 'Закрыт', 3600, '15/02/2026', '15/02/2026', 'Сезонная замена');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-15-02-26-0006', 'Замена охлаждающей жидкости', 1800, 7);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-15-02-26-0006', 'Антифриз G12 5л', 1800, 2, 6, 'л', 1100);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(6, 'ZAK-15-02-26-0006', 'Сидоров Алексей Михайлович', 'Замена охлаждающей жидкости', 7, '15/02/2026', '11:00', 'Завершён');

-- 7. ZAK-01-03-26-0007 (01.03.2026) — клиент 7, замена масла + фильтр
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-01-03-26-0007', 7, 'Закрыт', 4800, '01/03/2026', '01/03/2026', 'Плановое ТО');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-01-03-26-0007', 'Замена масла ДВС', 1500, 1);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-01-03-26-0007', 'Моторное масло 5W-30 4л', 3200, 1, 1, 'л', 2200),
('ZAK-01-03-26-0007', 'Фильтр масляный Haval', 650, 1, 2, 'шт', 400);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(7, 'ZAK-01-03-26-0007', 'Иванов Иван Петрович', 'Замена масла ДВС', 1, '01/03/2026', '10:00', 'Завершён');

-- 8. ZAK-10-03-26-0008 (10.03.2026) — клиент 8, замена стоек
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-10-03-26-0008', 8, 'Закрыт', 5900, '10/03/2026', '12/03/2026', 'Вибрация на неровностях');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-10-03-26-0008', 'Замена стоек амортизаторов (перед)', 4000, 12);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-10-03-26-0008', 'Амортизатор перед', 7800, 1, 10, 'шт', 4500);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(8, 'ZAK-10-03-26-0008', 'Петров Сергей Алексеевич', 'Замена стоек амортизаторов (перед)', 12, '10/03/2026', '09:00', 'Завершён');

-- 9. ZAK-25-03-26-0009 (25.03.2026) — клиент 9, комплексное ТО
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-25-03-26-0009', 9, 'Закрыт', 8300, '25/03/2026', '27/03/2026', 'ТО-10000');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-25-03-26-0009', 'Замена масла ДВС', 1500, 1),
('ZAK-25-03-26-0009', 'Замена воздушного фильтра', 800, 2),
('ZAK-25-03-26-0009', 'Балансировка колес', 1000, 6);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-25-03-26-0009', 'Моторное масло 5W-30 4л', 3200, 1, 1, 'л', 2200),
('ZAK-25-03-26-0009', 'Фильтр воздушный Haval', 950, 1, 3, 'шт', 600);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(9, 'ZAK-25-03-26-0009', 'Иванов Иван Петрович', 'Замена масла ДВС', 1, '25/03/2026', '10:00', 'Завершён');

-- 10. ZAK-08-04-26-0010 (08.04.2026) — клиент 10, замена ремня ГРМ
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-08-04-26-0010', 10, 'Закрыт', 11300, '08/04/2026', '10/04/2026', 'Срочная замена');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-08-04-26-0010', 'Замена ремня ГРМ', 5500, 10);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-08-04-26-0010', 'Ремень ГРМ комплект', 5800, 1, 8, 'компл', 3500);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(10, 'ZAK-08-04-26-0010', 'Сидоров Алексей Михайлович', 'Замена ремня ГРМ', 10, '08/04/2026', '09:00', 'Завершён');

-- 11. ZAK-20-04-26-0011 (20.04.2026) — клиент 1, замена колодок зад
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-20-04-26-0011', 1, 'Закрыт', 4000, '20/04/2026', '21/04/2026', 'Износ колодок');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-20-04-26-0011', 'Замена тормозных колодок (зад)', 2400, 4);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-20-04-26-0011', 'Колодки тормозные зад', 1600, 1, 5, 'компл', 900);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(1, 'ZAK-20-04-26-0011', 'Иванов Иван Петрович', 'Замена тормозных колодок (зад)', 4, '20/04/2026', '11:00', 'Завершён');

-- 12. ZAK-05-05-26-0012 (05.05.2026) — клиент 2, промывка инжектора
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-05-05-26-0012', 2, 'Закрыт', 4500, '05/05/2026', '05/05/2026', 'Нестабильные обороты');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-05-05-26-0012', 'Промывка инжектора', 2500, 14),
('ZAK-05-05-26-0012', 'Компьютерная диагностика', 2000, 9);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(2, 'ZAK-05-05-26-0012', 'Иванов Иван Петрович', 'Промывка инжектора', 14, '05/05/2026', '14:00', 'Завершён');

-- 13. ZAK-18-05-26-0013 (18.05.2026) — клиент 3, замена масла
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-18-05-26-0013', 3, 'Закрыт', 4800, '18/05/2026', '18/05/2026', 'Плановое ТО');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-18-05-26-0013', 'Замена масла ДВС', 1500, 1);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-18-05-26-0013', 'Моторное масло 5W-40 4л', 3600, 1, 14, 'л', 2500),
('ZAK-18-05-26-0013', 'Фильтр масляный Haval', 650, 1, 2, 'шт', 400);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(3, 'ZAK-18-05-26-0013', 'Петров Сергей Алексеевич', 'Замена масла ДВС', 1, '18/05/2026', '10:00', 'Завершён');

-- 14. ZAK-01-06-26-0014 (01.06.2026) — клиент 4, замена аккумулятора
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-01-06-26-0014', 4, 'Закрыт', 9900, '01/06/2026', '01/06/2026', 'Не крутит стартер');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-01-06-26-0014', 'Замена аккумулятора', 1000, 13);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-01-06-26-0014', 'Аккумулятор 60Ah', 8900, 1, 11, 'шт', 5500);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(4, 'ZAK-01-06-26-0014', 'Иванов Иван Петрович', 'Замена аккумулятора', 13, '01/06/2026', '09:00', 'Завершён');

-- 15. ZAK-12-06-26-0015 (12.06.2026) — клиент 5, замена масла + воздушный фильтр
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-12-06-26-0015', 5, 'Закрыт', 5550, '12/06/2026', '12/06/2026', 'Плановое ТО');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-12-06-26-0015', 'Замена масла ДВС', 1500, 1),
('ZAK-12-06-26-0015', 'Замена воздушного фильтра', 800, 2);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-12-06-26-0015', 'Моторное масло 5W-30 4л', 3200, 1, 1, 'л', 2200),
('ZAK-12-06-26-0015', 'Фильтр воздушный Haval', 950, 1, 3, 'шт', 600);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(5, 'ZAK-12-06-26-0015', 'Сидоров Алексей Михайлович', 'Замена масла ДВС', 1, '12/06/2026', '10:30', 'Завершён');

-- 16. ZAK-22-06-26-0016 (22.06.2026) — клиент 6, замена свечей + диагностика
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-22-06-26-0016', 6, 'Закрыт', 3600, '22/06/2026', '22/06/2026', 'Троит двигатель');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-22-06-26-0016', 'Замена свечей зажигания', 1600, 8),
('ZAK-22-06-26-0016', 'Компьютерная диагностика', 2000, 9);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-22-06-26-0016', 'Свечи зажигания NGK', 1400, 1, 7, 'компл', 800);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(6, 'ZAK-22-06-26-0016', 'Иванов Иван Петрович', 'Замена свечей зажигания', 8, '22/06/2026', '11:00', 'Завершён');

-- 17. ZAK-02-07-26-0017 (02.07.2026) — клиент 7, замена масла + антифриз
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-02-07-26-0017', 7, 'Закрыт', 5400, '02/07/2026', '02/07/2026', 'Подготовка к лету');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-02-07-26-0017', 'Замена масла ДВС', 1500, 1),
('ZAK-02-07-26-0017', 'Замена охлаждающей жидкости', 1800, 7);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-02-07-26-0017', 'Моторное масло 5W-30 4л', 3200, 1, 1, 'л', 2200),
('ZAK-02-07-26-0017', 'Антифриз G12 5л', 1800, 1, 6, 'л', 1100);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(7, 'ZAK-02-07-26-0017', 'Петров Сергей Алексеевич', 'Замена масла ДВС', 1, '02/07/2026', '10:00', 'Завершён');

-- 18. ZAK-10-07-26-0018 (10.07.2026) — клиент 8, шиномонтаж + баланс
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-10-07-26-0018', 8, 'Закрыт', 3400, '10/07/2026', '10/07/2026', 'Сезонная смена');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-10-07-26-0018', 'Шиномонтаж R16', 2400, 11),
('ZAK-10-07-26-0018', 'Балансировка колес', 1000, 6);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(8, 'ZAK-10-07-26-0018', 'Иванов Иван Петрович', 'Шиномонтаж R16', 11, '10/07/2026', '14:00', 'Завершён');

-- 19. ZAK-14-07-26-0019 (14.07.2026) — клиент 9, замена колодок + масло
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-14-07-26-0019', 9, 'Закрыт', 7500, '14/07/2026', '15/07/2026', 'Комплексное обслуживание');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-14-07-26-0019', 'Замена масла ДВС', 1500, 1),
('ZAK-14-07-26-0019', 'Замена тормозных колодок (перед)', 3200, 3);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-14-07-26-0019', 'Моторное масло 5W-30 4л', 3200, 1, 1, 'л', 2200),
('ZAK-14-07-26-0019', 'Колодки тормозные перед', 2100, 1, 4, 'компл', 1200);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(9, 'ZAK-14-07-26-0019', 'Сидоров Алексей Михайлович', 'Замена масла ДВС', 1, '14/07/2026', '09:00', 'Завершён');

-- 20. ZAK-17-07-26-0020 (17.07.2026) — клиент 10, замена стойки + диагностика
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-17-07-26-0020', 10, 'Закрыт', 3800, '17/07/2026', '17/07/2026', 'Стук в подвеске');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-17-07-26-0020', 'Диагностика ходовой', 1200, 5),
('ZAK-17-07-26-0020', 'Замена стойки стабилизатора', 1900, 9);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-17-07-26-0020', 'Стойка стабилизатора', 1900, 1, 9, 'шт', 1100);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(10, 'ZAK-17-07-26-0020', 'Иванов Иван Петрович', 'Диагностика ходовой', 5, '17/07/2026', '10:00', 'Завершён');

-- ==================================================================
-- ЧАСТЬ 2: 10 НОВЫХ ЗАКАЗОВ (18.07.2026 — 31.07.2026)
-- ==================================================================

-- 21. ZAK-18-07-26-0021 (18.07.2026) — клиент 1, замена масла
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-18-07-26-0021', 1, 'Новый', 4800, '18/07/2026', '', 'Плановое ТО');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-18-07-26-0021', 'Замена масла ДВС', 1500, 1);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-18-07-26-0021', 'Моторное масло 5W-30 4л', 3200, 1, 1, 'л', 2200),
('ZAK-18-07-26-0021', 'Фильтр масляный Haval', 650, 1, 2, 'шт', 400);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(1, 'ZAK-18-07-26-0021', 'Иванов Иван Петрович', 'Замена масла ДВС', 1, '18/07/2026', '10:00', 'Запланирован');

-- 22. ZAK-19-07-26-0022 (19.07.2026) — клиент 2, диагностика + колодки
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-19-07-26-0022', 2, 'Новый', 6400, '19/07/2026', '', 'Скрип при торможении');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-19-07-26-0022', 'Диагностика ходовой', 1200, 5),
('ZAK-19-07-26-0022', 'Замена тормозных колодок (перед)', 3200, 3);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-19-07-26-0022', 'Колодки тормозные перед', 2100, 1, 4, 'компл', 1200);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(2, 'ZAK-19-07-26-0022', 'Петров Сергей Алексеевич', 'Диагностика ходовой', 5, '19/07/2026', '09:00', 'Запланирован');

-- 23. ZAK-21-07-26-0023 (21.07.2026) — клиент 3, замена масла + антифриз
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-21-07-26-0023', 3, 'Новый', 6600, '21/07/2026', '', 'Сезонное обслуживание');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-21-07-26-0023', 'Замена масла ДВС', 1500, 1),
('ZAK-21-07-26-0023', 'Замена охлаждающей жидкости', 1800, 7);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-21-07-26-0023', 'Моторное масло 5W-30 4л', 3200, 1, 1, 'л', 2200),
('ZAK-21-07-26-0023', 'Антифриз G12 5л', 1800, 1, 6, 'л', 1100);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(3, 'ZAK-21-07-26-0023', 'Иванов Иван Петрович', 'Замена масла ДВС', 1, '21/07/2026', '10:00', 'Запланирован');

-- 24. ZAK-23-07-26-0024 (23.07.2026) — клиент 4, замена свечей
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-23-07-26-0024', 4, 'Новый', 3000, '23/07/2026', '', 'Троит двигатель');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-23-07-26-0024', 'Замена свечей зажигания', 1600, 8);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-23-07-26-0024', 'Свечи зажигания NGK', 1400, 1, 7, 'компл', 800);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(4, 'ZAK-23-07-26-0024', 'Сидоров Алексей Михайлович', 'Замена свечей зажигания', 8, '23/07/2026', '11:00', 'Запланирован');

-- 25. ZAK-25-07-26-0025 (25.07.2026) — клиент 5, шиномонтаж
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-25-07-26-0025', 5, 'Новый', 2400, '25/07/2026', '', 'Сезонная смена');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-25-07-26-0025', 'Шиномонтаж R16', 2400, 11);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(5, 'ZAK-25-07-26-0025', 'Иванов Иван Петрович', 'Шиномонтаж R16', 11, '25/07/2026', '14:00', 'Запланирован');

-- 26. ZAK-26-07-26-0026 (26.07.2026) — клиент 6, замена масла + фильтр
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-26-07-26-0026', 6, 'Новый', 4800, '26/07/2026', '', 'Плановое ТО');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-26-07-26-0026', 'Замена масла ДВС', 1500, 1);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-26-07-26-0026', 'Моторное масло 5W-30 4л', 3200, 1, 1, 'л', 2200),
('ZAK-26-07-26-0026', 'Фильтр масляный Haval', 650, 1, 2, 'шт', 400);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(6, 'ZAK-26-07-26-0026', 'Петров Сергей Алексеевич', 'Замена масла ДВС', 1, '26/07/2026', '10:00', 'Запланирован');

-- 27. ZAK-28-07-26-0027 (28.07.2026) — клиент 7, замена ремня ГРМ
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-28-07-26-0027', 7, 'Новый', 11300, '28/07/2026', '', 'Замена по регламенту');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-28-07-26-0027', 'Замена ремня ГРМ', 5500, 10);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-28-07-26-0027', 'Ремень ГРМ комплект', 5800, 1, 8, 'компл', 3500);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(7, 'ZAK-28-07-26-0027', 'Иванов Иван Петрович', 'Замена ремня ГРМ', 10, '28/07/2026', '09:00', 'Запланирован');

-- 28. ZAK-29-07-26-0028 (29.07.2026) — клиент 8, замена аккумулятора + диагностика
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-29-07-26-0028', 8, 'Новый', 10900, '29/07/2026', '', 'Не заводится');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-29-07-26-0028', 'Замена аккумулятора', 1000, 13),
('ZAK-29-07-26-0028', 'Компьютерная диагностика', 2000, 9);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-29-07-26-0028', 'Аккумулятор 60Ah', 8900, 1, 11, 'шт', 5500);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(8, 'ZAK-29-07-26-0028', 'Сидоров Алексей Михайлович', 'Замена аккумулятора', 13, '29/07/2026', '10:00', 'Запланирован');

-- 29. ZAK-30-07-26-0029 (30.07.2026) — клиент 9, замена колодок + масло
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-30-07-26-0029', 9, 'Новый', 7500, '30/07/2026', '', 'Комплексное обслуживание');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-30-07-26-0029', 'Замена масла ДВС', 1500, 1),
('ZAK-30-07-26-0029', 'Замена тормозных колодок (зад)', 2400, 4);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-30-07-26-0029', 'Моторное масло 5W-30 4л', 3200, 1, 1, 'л', 2200),
('ZAK-30-07-26-0029', 'Колодки тормозные зад', 1600, 1, 5, 'компл', 900);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(9, 'ZAK-30-07-26-0029', 'Иванов Иван Петрович', 'Замена масла ДВС', 1, '30/07/2026', '11:00', 'Запланирован');

-- 30. ZAK-31-07-26-0030 (31.07.2026) — клиент 10, промывка + фильтр салона
INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES
('ZAK-31-07-26-0030', 10, 'Новый', 3100, '31/07/2026', '', 'Запах в салоне');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES
('ZAK-31-07-26-0030', 'Промывка инжектора', 2500, 14);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES
('ZAK-31-07-26-0030', 'Фильтр салона', 600, 1, 13, 'шт', 350);
INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES
(10, 'ZAK-31-07-26-0030', 'Петров Сергей Алексеевич', 'Промывка инжектора', 14, '31/07/2026', '09:00', 'Запланирован');

-- ==================================================================
-- ПРОВЕРКА РЕЗУЛЬТАТОВ
-- ==================================================================
-- SELECT COUNT(*) FROM orders WHERE status = 'Закрыт';   -- должно быть 20
-- SELECT COUNT(*) FROM orders WHERE status = 'Новый';     -- должно быть 10
-- SELECT COUNT(*) FROM appointments;                      -- должно быть 30
