-- AdminSTO: тестовые данные для демонстрационной базы.
-- Сгенерировано tools/seed_test_data.py
-- Дата генерации: 14.11.2026 12:29

BEGIN TRANSACTION;

-- Справочник услуг
INSERT INTO services (id, name, price, duration, part_number, category) VALUES (1, 'Замена моторного масла', 2500.0, 40, 'SRV-OIL-01', 'MAINTENANCE');
INSERT INTO services (id, name, price, duration, part_number, category) VALUES (2, 'Комплексное ТО', 8500.0, 180, 'SRV-TO-02', 'MAINTENANCE');
INSERT INTO services (id, name, price, duration, part_number, category) VALUES (3, 'Замена воздушного фильтра', 900.0, 20, 'SRV-AF-03', 'MAINTENANCE');
INSERT INTO services (id, name, price, duration, part_number, category) VALUES (4, 'Чип-тюнинг Stage 1', 45000.0, 120, 'SRV-TUN-04', 'TUNING');
INSERT INTO services (id, name, price, duration, part_number, category) VALUES (5, 'Установка спортивного выхлопа', 120000.0, 240, 'SRV-TUN-05', 'TUNING');
INSERT INTO services (id, name, price, duration, part_number, category) VALUES (6, 'Тюнинг подвески (занижение)', 250000.0, 480, 'SRV-TUN-06', 'TUNING');
INSERT INTO services (id, name, price, duration, part_number, category) VALUES (7, 'Замена тормозных колодок', 3200.0, 90, 'SRV-BRK-07', 'REGULAR');
INSERT INTO services (id, name, price, duration, part_number, category) VALUES (8, 'Диагностика подвески', 1500.0, 60, 'SRV-DIAG-08', 'REGULAR');
INSERT INTO services (id, name, price, duration, part_number, category) VALUES (9, 'Ремонт кондиционера', 6500.0, 150, 'SRV-AC-09', 'REGULAR');

-- Справочник запчастей
INSERT INTO spare_parts (id, name, part_number, manufacturer, retail_price, purchase_price, stock, min_stock, location, category, unit_type) VALUES (1, 'Масло моторное 5W-40', 'OIL-5W40', 'Lukoil', 800.0, 600.0, 40.0, 10.0, 'A-01', 'MAINTENANCE', 'л');
INSERT INTO spare_parts (id, name, part_number, manufacturer, retail_price, purchase_price, stock, min_stock, location, category, unit_type) VALUES (2, 'Масляный фильтр', 'MF-210', 'Mann', 850.0, 600.0, 25.0, 5.0, 'A-02', 'MAINTENANCE', 'шт');
INSERT INTO spare_parts (id, name, part_number, manufacturer, retail_price, purchase_price, stock, min_stock, location, category, unit_type) VALUES (3, 'Воздушный фильтр', 'AF-310', 'Mann', 1200.0, 800.0, 18.0, 4.0, 'A-03', 'MAINTENANCE', 'шт');
INSERT INTO spare_parts (id, name, part_number, manufacturer, retail_price, purchase_price, stock, min_stock, location, category, unit_type) VALUES (4, 'Тормозные колодки передние', 'BP-150', 'Brembo', 5200.0, 3800.0, 12.0, 2.0, 'B-01', 'REGULAR', 'компл');
INSERT INTO spare_parts (id, name, part_number, manufacturer, retail_price, purchase_price, stock, min_stock, location, category, unit_type) VALUES (5, 'Свечи зажигания', 'SP-77', 'NGK', 900.0, 600.0, 60.0, 12.0, 'B-02', 'REGULAR', 'шт');
INSERT INTO spare_parts (id, name, part_number, manufacturer, retail_price, purchase_price, stock, min_stock, location, category, unit_type) VALUES (6, 'Выхлопная система Sport', 'EX-900', 'Borla', 95000.0, 70000.0, 3.0, 1.0, 'C-01', 'TUNING', 'компл');
INSERT INTO spare_parts (id, name, part_number, manufacturer, retail_price, purchase_price, stock, min_stock, location, category, unit_type) VALUES (7, 'Пружины заниженные', 'SPR-45', 'H&R', 45000.0, 30000.0, 5.0, 1.0, 'C-02', 'TUNING', 'компл');

-- Клиенты и их автомобили
INSERT INTO clients (id, name, last_name, phone, car_model, car_number, last_repair_date, maintenance_stars, tuning_total) VALUES (2, 'Иван', 'Петров', '+7 900 111-00-02', 'Lada Vesta', 'А111АА163', '03/11/2026', 0, 0.0);
INSERT INTO clients (id, name, last_name, phone, car_model, car_number, last_repair_date, maintenance_stars, tuning_total) VALUES (3, 'Мария', 'Соколова', '+7 900 111-00-03', 'Kia Rio', 'В222ВВ163', '', 0, 0.0);
INSERT INTO clients (id, name, last_name, phone, car_model, car_number, last_repair_date, maintenance_stars, tuning_total) VALUES (4, 'Алексей', 'Кузнецов', '+7 900 111-00-04', 'Toyota Camry', 'С333СС163', '05/11/2026', 3, 0.0);
INSERT INTO clients (id, name, last_name, phone, car_model, car_number, last_repair_date, maintenance_stars, tuning_total) VALUES (5, 'Дмитрий', 'Волков', '+7 900 111-00-05', 'Skoda Octavia', 'Е444ЕЕ163', '08/11/2026', 3, 0.0);
INSERT INTO clients (id, name, last_name, phone, car_model, car_number, last_repair_date, maintenance_stars, tuning_total) VALUES (6, 'Анна', 'Морозова', '+7 900 111-00-06', 'Hyundai Creta', 'К555КК163', '12/11/2026', 4, 0.0);
INSERT INTO clients (id, name, last_name, phone, car_model, car_number, last_repair_date, maintenance_stars, tuning_total) VALUES (7, 'Наталья', 'Лебедева', '+7 900 111-00-07', 'Volkswagen Polo', 'М666ММ163', '10/11/2026', 5, 0.0);
INSERT INTO clients (id, name, last_name, phone, car_model, car_number, last_repair_date, maintenance_stars, tuning_total) VALUES (8, 'Сергей', 'Титов', '+7 900 111-00-08', 'Audi A4', 'Н777НН163', '17/08/2026', 0, 90000.0);
INSERT INTO clients (id, name, last_name, phone, car_model, car_number, last_repair_date, maintenance_stars, tuning_total) VALUES (9, 'Виктор', 'Громов', '+7 900 111-00-09', 'BMW 320i', 'О888ОО163', '14/09/2026', 0, 210000.0);
INSERT INTO clients (id, name, last_name, phone, car_model, car_number, last_repair_date, maintenance_stars, tuning_total) VALUES (10, 'Олег', 'Белов', '+7 900 111-00-10', 'Mercedes-Benz C200', 'Р999РР163', '09/10/2026', 0, 420000.0);
INSERT INTO clients (id, name, last_name, phone, car_model, car_number, last_repair_date, maintenance_stars, tuning_total) VALUES (11, 'Ирина', 'Смирнова', '+7 900 111-00-11', 'Renault Logan', 'Т111ТТ163', '21/10/2026', 0, 0.0);
INSERT INTO clients (id, name, last_name, phone, car_model, car_number, last_repair_date, maintenance_stars, tuning_total) VALUES (12, 'Павел', 'Козлов', '+7 900 111-00-12', 'Lada Granta', 'У222УУ163', '', 0, 0.0);

INSERT INTO client_cars (client_id, car_model, car_number, mileage) VALUES (2, 'Lada Vesta', 'А111АА163', 42000);
INSERT INTO client_cars (client_id, car_model, car_number, mileage) VALUES (3, 'Kia Rio', 'В222ВВ163', 18000);
INSERT INTO client_cars (client_id, car_model, car_number, mileage) VALUES (4, 'Toyota Camry', 'С333СС163', 22000);
INSERT INTO client_cars (client_id, car_model, car_number, mileage) VALUES (5, 'Skoda Octavia', 'Е444ЕЕ163', 22000);
INSERT INTO client_cars (client_id, car_model, car_number, mileage) VALUES (5, 'Renault Duster', 'Е445ЕЕ163', 61000);
INSERT INTO client_cars (client_id, car_model, car_number, mileage) VALUES (6, 'Hyundai Creta', 'К555КК163', 29000);
INSERT INTO client_cars (client_id, car_model, car_number, mileage) VALUES (7, 'Volkswagen Polo', 'М666ММ163', 36000);
INSERT INTO client_cars (client_id, car_model, car_number, mileage) VALUES (8, 'Audi A4', 'Н777НН163', 88000);
INSERT INTO client_cars (client_id, car_model, car_number, mileage) VALUES (9, 'BMW 320i', 'О888ОО163', 74000);
INSERT INTO client_cars (client_id, car_model, car_number, mileage) VALUES (10, 'Mercedes-Benz C200', 'Р999РР163', 52000);
INSERT INTO client_cars (client_id, car_model, car_number, mileage) VALUES (11, 'Renault Logan', 'Т111ТТ163', 95000);
INSERT INTO client_cars (client_id, car_model, car_number, mileage) VALUES (12, 'Lada Granta', 'У222УУ163', 12000);

-- Заказы, их услуги и запчасти
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-12/07/26-0001', 4, 'Закрыт', 7450.0, '12/07/2026', 8000, '12/07/2026', 'Плановое ТО: масло и фильтры');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-12/07/26-0001', 'Замена моторного масла', 2500.0, 1);
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-12/07/26-0001', 'Замена воздушного фильтра', 900.0, 3);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-12/07/26-0001', 'Масло моторное 5W-40', 800.0, 4, 1, 'л', 600.0);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-12/07/26-0001', 'Масляный фильтр', 850.0, 1, 2, 'шт', 600.0);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-20/09/26-0002', 4, 'Закрыт', 13750.0, '20/09/2026', 15000, '20/09/2026', 'Комплексное ТО');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-20/09/26-0002', 'Комплексное ТО', 8500.0, 2);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-20/09/26-0002', 'Масло моторное 5W-40', 800.0, 4, 1, 'л', 600.0);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-20/09/26-0002', 'Масляный фильтр', 850.0, 1, 2, 'шт', 600.0);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-20/09/26-0002', 'Воздушный фильтр', 1200.0, 1, 3, 'шт', 800.0);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-05/11/26-0003', 4, 'Закрыт', 6600.0, '05/11/2026', 22000, '05/11/2026', 'ТО по регламенту');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-05/11/26-0003', 'Замена моторного масла', 2500.0, 1);
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-05/11/26-0003', 'Замена воздушного фильтра', 900.0, 3);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-05/11/26-0003', 'Масло моторное 5W-40', 800.0, 4, 1, 'л', 600.0);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-18/07/26-0004', 5, 'Закрыт', 7450.0, '18/07/2026', 8000, '18/07/2026', 'Плановое ТО: масло и фильтры');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-18/07/26-0004', 'Замена моторного масла', 2500.0, 1);
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-18/07/26-0004', 'Замена воздушного фильтра', 900.0, 3);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-18/07/26-0004', 'Масло моторное 5W-40', 800.0, 4, 1, 'л', 600.0);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-18/07/26-0004', 'Масляный фильтр', 850.0, 1, 2, 'шт', 600.0);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-26/09/26-0005', 5, 'Закрыт', 13750.0, '26/09/2026', 15000, '26/09/2026', 'Комплексное ТО');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-26/09/26-0005', 'Комплексное ТО', 8500.0, 2);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-26/09/26-0005', 'Масло моторное 5W-40', 800.0, 4, 1, 'л', 600.0);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-26/09/26-0005', 'Масляный фильтр', 850.0, 1, 2, 'шт', 600.0);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-26/09/26-0005', 'Воздушный фильтр', 1200.0, 1, 3, 'шт', 800.0);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-08/11/26-0006', 5, 'Закрыт', 6600.0, '08/11/2026', 22000, '08/11/2026', 'ТО по регламенту');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-08/11/26-0006', 'Замена моторного масла', 2500.0, 1);
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-08/11/26-0006', 'Замена воздушного фильтра', 900.0, 3);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-08/11/26-0006', 'Масло моторное 5W-40', 800.0, 4, 1, 'л', 600.0);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-05/06/26-0007', 6, 'Закрыт', 7450.0, '05/06/2026', 8000, '05/06/2026', 'Плановое ТО: масло и фильтры');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-05/06/26-0007', 'Замена моторного масла', 2500.0, 1);
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-05/06/26-0007', 'Замена воздушного фильтра', 900.0, 3);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-05/06/26-0007', 'Масло моторное 5W-40', 800.0, 4, 1, 'л', 600.0);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-05/06/26-0007', 'Масляный фильтр', 850.0, 1, 2, 'шт', 600.0);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-10/08/26-0008', 6, 'Закрыт', 13750.0, '10/08/2026', 15000, '10/08/2026', 'Комплексное ТО');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-10/08/26-0008', 'Комплексное ТО', 8500.0, 2);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-10/08/26-0008', 'Масло моторное 5W-40', 800.0, 4, 1, 'л', 600.0);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-10/08/26-0008', 'Масляный фильтр', 850.0, 1, 2, 'шт', 600.0);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-10/08/26-0008', 'Воздушный фильтр', 1200.0, 1, 3, 'шт', 800.0);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-14/10/26-0009', 6, 'Закрыт', 6600.0, '14/10/2026', 22000, '14/10/2026', 'ТО по регламенту');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-14/10/26-0009', 'Замена моторного масла', 2500.0, 1);
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-14/10/26-0009', 'Замена воздушного фильтра', 900.0, 3);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-14/10/26-0009', 'Масло моторное 5W-40', 800.0, 4, 1, 'л', 600.0);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-12/11/26-0010', 6, 'Закрыт', 7450.0, '12/11/2026', 29000, '12/11/2026', 'Плановое ТО: масло и фильтры');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-12/11/26-0010', 'Замена моторного масла', 2500.0, 1);
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-12/11/26-0010', 'Замена воздушного фильтра', 900.0, 3);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-12/11/26-0010', 'Масло моторное 5W-40', 800.0, 4, 1, 'л', 600.0);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-12/11/26-0010', 'Масляный фильтр', 850.0, 1, 2, 'шт', 600.0);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-20/04/26-0011', 7, 'Закрыт', 7450.0, '20/04/2026', 8000, '20/04/2026', 'Плановое ТО: масло и фильтры');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-20/04/26-0011', 'Замена моторного масла', 2500.0, 1);
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-20/04/26-0011', 'Замена воздушного фильтра', 900.0, 3);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-20/04/26-0011', 'Масло моторное 5W-40', 800.0, 4, 1, 'л', 600.0);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-20/04/26-0011', 'Масляный фильтр', 850.0, 1, 2, 'шт', 600.0);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-03/07/26-0012', 7, 'Закрыт', 13750.0, '03/07/2026', 15000, '03/07/2026', 'Комплексное ТО');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-03/07/26-0012', 'Комплексное ТО', 8500.0, 2);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-03/07/26-0012', 'Масло моторное 5W-40', 800.0, 4, 1, 'л', 600.0);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-03/07/26-0012', 'Масляный фильтр', 850.0, 1, 2, 'шт', 600.0);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-03/07/26-0012', 'Воздушный фильтр', 1200.0, 1, 3, 'шт', 800.0);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-19/09/26-0013', 7, 'Закрыт', 6600.0, '19/09/2026', 22000, '19/09/2026', 'ТО по регламенту');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-19/09/26-0013', 'Замена моторного масла', 2500.0, 1);
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-19/09/26-0013', 'Замена воздушного фильтра', 900.0, 3);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-19/09/26-0013', 'Масло моторное 5W-40', 800.0, 4, 1, 'л', 600.0);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-24/10/26-0014', 7, 'Закрыт', 7450.0, '24/10/2026', 29000, '24/10/2026', 'Плановое ТО: масло и фильтры');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-24/10/26-0014', 'Замена моторного масла', 2500.0, 1);
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-24/10/26-0014', 'Замена воздушного фильтра', 900.0, 3);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-24/10/26-0014', 'Масло моторное 5W-40', 800.0, 4, 1, 'л', 600.0);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-24/10/26-0014', 'Масляный фильтр', 850.0, 1, 2, 'шт', 600.0);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-10/11/26-0015', 7, 'Закрыт', 13750.0, '10/11/2026', 36000, '10/11/2026', 'Комплексное ТО');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-10/11/26-0015', 'Комплексное ТО', 8500.0, 2);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-10/11/26-0015', 'Масло моторное 5W-40', 800.0, 4, 1, 'л', 600.0);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-10/11/26-0015', 'Масляный фильтр', 850.0, 1, 2, 'шт', 600.0);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-10/11/26-0015', 'Воздушный фильтр', 1200.0, 1, 3, 'шт', 800.0);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-15/08/26-0016', 8, 'Закрыт', 90000.0, '15/08/2026', 61000, '17/08/2026', 'Чип-тюнинг и пружины');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-15/08/26-0016', 'Чип-тюнинг Stage 1', 45000.0, 4);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-15/08/26-0016', 'Пружины заниженные', 45000.0, 1, 7, 'компл', 30000.0);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-11/09/26-0017', 9, 'Закрыт', 210000.0, '11/09/2026', 58000, '14/09/2026', 'Выхлоп и чип-тюнинг');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-11/09/26-0017', 'Установка спортивного выхлопа', 120000.0, 5);
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-11/09/26-0017', 'Чип-тюнинг Stage 1', 45000.0, 4);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-11/09/26-0017', 'Пружины заниженные', 45000.0, 1, 7, 'компл', 30000.0);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-02/10/26-0018', 10, 'Закрыт', 420000.0, '02/10/2026', 47000, '09/10/2026', 'Комплексный тюнинг подвески и выхлопа');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-02/10/26-0018', 'Тюнинг подвески (занижение)', 250000.0, 6);
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-02/10/26-0018', 'Установка спортивного выхлопа', 120000.0, 5);
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-02/10/26-0018', 'Чип-тюнинг Stage 1', 50000.0, 4);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-21/10/26-0019', 11, 'Закрыт', 9900.0, '21/10/2026', 95000, '21/10/2026', 'Тормоза и диагностика');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-21/10/26-0019', 'Замена тормозных колодок', 3200.0, 7);
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-21/10/26-0019', 'Диагностика подвески', 1500.0, 8);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-21/10/26-0019', 'Тормозные колодки передние', 5200.0, 1, 4, 'компл', 3800.0);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-03/11/26-0020', 2, 'Закрыт', 5100.0, '03/11/2026', 40000, '03/11/2026', 'Диагностика и свечи');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-03/11/26-0020', 'Диагностика подвески', 1500.0, 8);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-03/11/26-0020', 'Свечи зажигания', 900.0, 4, 5, 'шт', 600.0);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-10/11/26-0021', 11, 'Новый', 45000.0, '10/11/2026', 95000, '', 'Заказан чип-тюнинг (в работе не начат)');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-10/11/26-0021', 'Чип-тюнинг Stage 1', 45000.0, 4);
INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES ('ZAK-12/11/26-0022', 12, 'В работе', 11700.0, '12/11/2026', 12000, '', 'Комплексное ТО, ожидает запчасти');
INSERT INTO order_services (order_id, service_name, price, service_id) VALUES ('ZAK-12/11/26-0022', 'Комплексное ТО', 8500.0, 2);
INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES ('ZAK-12/11/26-0022', 'Масло моторное 5W-40', 800.0, 4, 1, 'л', 600.0);

COMMIT;

-- Итого: клиентов 11, услуг 9, запчастей 7, заказов 22, строк услуг в заказах 36, строк запчастей 36
