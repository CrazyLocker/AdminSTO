-- ============================================
-- ТЕСТОВАЯ БАЗА ДАННЫХ ДЛЯ АВТОТЕСТОВ
-- ============================================

-- Удаляем старые таблицы (если есть)
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS order_parts;
DROP TABLE IF EXISTS order_services;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS spare_parts;
DROP TABLE IF EXISTS services;
DROP TABLE IF EXISTS clients;

-- ============================================
-- СОЗДАНИЕ ТАБЛИЦ
-- ============================================

CREATE TABLE clients (
                         id INTEGER PRIMARY KEY AUTOINCREMENT,
                         name TEXT NOT NULL,
                         phone TEXT NOT NULL,
                         car_model TEXT NOT NULL,
                         car_number TEXT NOT NULL
);

CREATE TABLE services (
                          id INTEGER PRIMARY KEY AUTOINCREMENT,
                          name TEXT NOT NULL UNIQUE,
                          price REAL NOT NULL
);

CREATE TABLE spare_parts (
                             id INTEGER PRIMARY KEY AUTOINCREMENT,
                             name TEXT NOT NULL UNIQUE,
                             purchase_price REAL NOT NULL,
                             retail_price REAL NOT NULL,
                             stock INTEGER NOT NULL
);

CREATE TABLE orders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        client_id INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        total REAL NOT NULL,
                        created_date TEXT NOT NULL,
                        FOREIGN KEY (client_id) REFERENCES clients(id)
);

CREATE TABLE order_services (
                                order_id INTEGER NOT NULL,
                                service_name TEXT NOT NULL,
                                price REAL NOT NULL,
                                FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE TABLE order_parts (
                             order_id INTEGER NOT NULL,
                             part_name TEXT NOT NULL,
                             price REAL NOT NULL,
                             quantity INTEGER NOT NULL,
                             FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE TABLE appointments (
                              id INTEGER PRIMARY KEY AUTOINCREMENT,
                              client_id INTEGER NOT NULL,
                              order_id INTEGER,
                              master_name TEXT NOT NULL,
                              service_name TEXT NOT NULL,
                              appointment_date TEXT NOT NULL,
                              appointment_time TEXT NOT NULL,
                              status TEXT NOT NULL,
                              FOREIGN KEY (client_id) REFERENCES clients(id),
                              FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- ============================================
-- ТЕСТОВЫЕ ДАННЫЕ
-- ============================================

-- 1. Клиент для тестов
INSERT INTO clients (name, phone, car_model, car_number) VALUES
    ('Тест Клиент', '+79001234567', 'Test Car', 'A123BC');

-- 2. Услуга для тестов
INSERT INTO services (name, price) VALUES
    ('Тест услуга', 1000);

-- 3. Запчасть для тестов
INSERT INTO spare_parts (name, purchase_price, retail_price, stock) VALUES
    ('Тест запчасть', 500, 800, 10);

-- 4. Второй клиент для тестов
INSERT INTO clients (name, phone, car_model, car_number) VALUES
    ('Тест Клиент 2', '+79111234567', 'Test Car 2', 'B456CD');

-- 5. Вторая услуга
INSERT INTO services (name, price) VALUES
    ('Тест услуга 2', 2000);

-- 6. Вторая запчасть
INSERT INTO spare_parts (name, purchase_price, retail_price, stock) VALUES
    ('Тест запчасть 2', 300, 500, 20);

-- ============================================
-- ПРОВЕРКА (вывод статистики)
-- ============================================
SELECT 'Clients: ' || COUNT(*) FROM clients
UNION ALL
SELECT 'Services: ' || COUNT(*) FROM services
UNION ALL
SELECT 'Spare parts: ' || COUNT(*) FROM spare_parts;