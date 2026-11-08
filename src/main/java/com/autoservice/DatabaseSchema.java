package com.autoservice;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Единое описание схемы БД для реализаций {@link AbstractDatabase}.
 *
 * <p>DDL хранится в одном месте и параметризуется только синтаксисом
 * автоинкремента, который различается у SQLite и H2. Раньше схемы
 * дублировались в {@code SQLiteDatabase} и {@code H2Database} и успели
 * разойтись: в H2 оставались legacy-колонки {@code unit_volume}/{@code is_liquid},
 * а у внешнего ключа {@code service_spare_parts_list_items.list_id} не было
 * {@code ON DELETE CASCADE}. Теперь обе СУБД используют одну схему.
 */
final class DatabaseSchema {

    /** Автоинкремент SQLite. */
    private static final String AUTOINCREMENT_SQLITE = "PRIMARY KEY AUTOINCREMENT";

    /** Автоинкремент H2. */
    private static final String AUTOINCREMENT_H2 = "AUTO_INCREMENT PRIMARY KEY";

    private static final String CREATE_CLIENTS = "CREATE TABLE IF NOT EXISTS clients (" +
            "id INTEGER %s, " +
            "name TEXT NOT NULL CHECK(length(name) > 0), " +
            "last_name TEXT DEFAULT '', " +
            "phone TEXT NOT NULL CHECK(length(phone) > 0), " +
            "car_model TEXT DEFAULT '', " +
            "car_number TEXT DEFAULT '', " +
            "last_repair_date TEXT DEFAULT ''" +
            ")";

    private static final String CREATE_CLIENT_CARS = "CREATE TABLE IF NOT EXISTS client_cars (" +
            "id INTEGER %s, " +
            "client_id INTEGER NOT NULL CHECK(client_id > 0), " +
            "car_model TEXT NOT NULL CHECK(length(car_model) > 0), " +
            "car_number TEXT NOT NULL CHECK(length(car_number) > 0), " +
            "mileage INTEGER DEFAULT 0 CHECK(mileage >= 0), " +
            "FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE" +
            ")";

    private static final String CREATE_SERVICES = "CREATE TABLE IF NOT EXISTS services (" +
            "id INTEGER %s, " +
            "name TEXT NOT NULL UNIQUE CHECK(length(name) > 0), " +
            "price REAL NOT NULL CHECK(price >= 0), " +
            "duration INTEGER DEFAULT 60 CHECK(duration >= 0), " +
            "part_number TEXT DEFAULT '', " +
            "category_id INTEGER DEFAULT 0, " +
            "oil_volume REAL DEFAULT 0 CHECK(oil_volume >= 0), " +
            "uses_oil INTEGER DEFAULT 0 CHECK(uses_oil IN (0, 1)), " +
            "spare_part_name TEXT DEFAULT '', " +
            "spare_part_quantity INTEGER DEFAULT 0 CHECK(spare_part_quantity >= 0)" +
            ")";

    private static final String CREATE_SPARE_PARTS = "CREATE TABLE IF NOT EXISTS spare_parts (" +
            "id INTEGER %s, " +
            "name TEXT NOT NULL UNIQUE CHECK(length(name) > 0), " +
            "part_number TEXT DEFAULT '', " +
            "manufacturer TEXT DEFAULT '', " +
            "compatible_models TEXT DEFAULT '', " +
            "note TEXT DEFAULT '', " +
            "purchase_price REAL CHECK(purchase_price >= 0), " +
            "retail_price REAL NOT NULL CHECK(retail_price >= 0), " +
            "stock REAL DEFAULT 0 CHECK(stock >= 0), " +
            "min_stock REAL DEFAULT 0 CHECK(min_stock >= 0), " +
            "location TEXT DEFAULT '', " +
            "unit_type TEXT DEFAULT 'шт' CHECK(unit_type IN ('шт', 'л', 'компл'))" +
            ")";

    private static final String CREATE_ORDERS = "CREATE TABLE IF NOT EXISTS orders (" +
            "id TEXT PRIMARY KEY CHECK(length(id) > 0), " +
            "client_id INTEGER NOT NULL CHECK(client_id > 0), " +
            "status TEXT NOT NULL CHECK(length(status) > 0), " +
            "total REAL NOT NULL CHECK(total >= 0), " +
            "created_date TEXT NOT NULL CHECK(length(created_date) > 0), " +
            "mileage INTEGER DEFAULT 0 CHECK(mileage >= 0), " +
            "closed_date TEXT DEFAULT '', " +
            "notes TEXT DEFAULT '', " +
            "FOREIGN KEY (client_id) REFERENCES clients(id)" +
            ")";

    private static final String CREATE_ORDER_SERVICES = "CREATE TABLE IF NOT EXISTS order_services (" +
            "order_id TEXT NOT NULL CHECK(length(order_id) > 0), " +
            "service_name TEXT NOT NULL CHECK(length(service_name) > 0), " +
            "price REAL NOT NULL CHECK(price >= 0), " +
            "service_id INTEGER DEFAULT 0, " +
            "FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE" +
            ")";

    private static final String CREATE_ORDER_PARTS = "CREATE TABLE IF NOT EXISTS order_parts (" +
            "order_id TEXT NOT NULL CHECK(length(order_id) > 0), " +
            "part_name TEXT NOT NULL CHECK(length(part_name) > 0), " +
            "price REAL NOT NULL CHECK(price >= 0), " +
            "quantity INTEGER NOT NULL CHECK(quantity > 0), " +
            "spare_part_id INTEGER DEFAULT 0, " +
            "unit_type TEXT DEFAULT 'шт', " +
            "purchase_price REAL DEFAULT 0, " +
            "FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE" +
            ")";

    private static final String CREATE_APPOINTMENTS = "CREATE TABLE IF NOT EXISTS appointments (" +
            "id INTEGER %s, " +
            "client_id INTEGER NOT NULL CHECK(client_id > 0), " +
            "order_id TEXT, " +
            "master_name TEXT NOT NULL CHECK(length(master_name) > 0), " +
            "service_name TEXT NOT NULL CHECK(length(service_name) > 0), " +
            "service_id INTEGER DEFAULT 0, " +
            "appointment_date TEXT NOT NULL CHECK(length(appointment_date) > 0), " +
            "appointment_time TEXT NOT NULL CHECK(length(appointment_time) > 0), " +
            "status TEXT NOT NULL CHECK(length(status) > 0), " +
            "FOREIGN KEY (client_id) REFERENCES clients(id), " +
            "FOREIGN KEY (order_id) REFERENCES orders(id)" +
            ")";

    private static final String CREATE_SERVICE_SPARE_PARTS = "CREATE TABLE IF NOT EXISTS service_spare_parts (" +
            "id INTEGER %s, " +
            "service_id INTEGER NOT NULL CHECK(service_id > 0), " +
            "spare_part_id INTEGER NOT NULL CHECK(spare_part_id > 0), " +
            "quantity INTEGER DEFAULT 1 CHECK(quantity > 0), " +
            "unit_type TEXT DEFAULT 'шт' CHECK(unit_type IN ('шт', 'л', 'компл')), " +
            "active INTEGER DEFAULT 1 CHECK(active IN (0, 1)), " +
            "FOREIGN KEY (service_id) REFERENCES services(id), " +
            "FOREIGN KEY (spare_part_id) REFERENCES spare_parts(id)" +
            ")";

    private static final String CREATE_TO_PARTS = "CREATE TABLE IF NOT EXISTS to_parts (" +
            "id INTEGER %s, " +
            "car_model TEXT NOT NULL CHECK(length(car_model) > 0), " +
            "spare_part_id INTEGER NOT NULL CHECK(spare_part_id > 0), " +
            "quantity INTEGER DEFAULT 1 CHECK(quantity > 0), " +
            "unit_type TEXT DEFAULT 'шт' CHECK(unit_type IN ('шт', 'л', 'компл')), " +
            "note TEXT DEFAULT '', " +
            "active INTEGER DEFAULT 1 CHECK(active IN (0, 1))" +
            ")";

    private static final String CREATE_APP_SETTINGS = "CREATE TABLE IF NOT EXISTS app_settings (" +
            "id INTEGER %s, " +
            "setting_key TEXT NOT NULL UNIQUE CHECK(length(setting_key) > 0), " +
            "setting_value TEXT NOT NULL CHECK(length(setting_value) > 0), " +
            "description TEXT DEFAULT ''" +
            ")";

    private static final String CREATE_SERVICE_SPARE_PARTS_LISTS = "CREATE TABLE IF NOT EXISTS service_spare_parts_lists (" +
            "id INTEGER %s, " +
            "service_id INTEGER NOT NULL CHECK(service_id > 0), " +
            "created_date TEXT NOT NULL CHECK(length(created_date) > 0), " +
            "active INTEGER DEFAULT 1 CHECK(active IN (0, 1)), " +
            "FOREIGN KEY (service_id) REFERENCES services(id)" +
            ")";

    private static final String CREATE_SERVICE_SPARE_PARTS_LIST_ITEMS = "CREATE TABLE IF NOT EXISTS service_spare_parts_list_items (" +
            "id INTEGER %s, " +
            "list_id INTEGER NOT NULL CHECK(list_id > 0), " +
            "spare_part_id INTEGER NOT NULL CHECK(spare_part_id > 0), " +
            "quantity INTEGER DEFAULT 1 CHECK(quantity > 0), " +
            "unit_type TEXT DEFAULT 'шт' CHECK(unit_type IN ('шт', 'л', 'компл')), " +
            "FOREIGN KEY (list_id) REFERENCES service_spare_parts_lists(id) ON DELETE CASCADE, " +
            "FOREIGN KEY (spare_part_id) REFERENCES spare_parts(id)" +
            ")";

    private static final String CREATE_SERVICE_PARTS = "CREATE TABLE IF NOT EXISTS service_parts (" +
            "id INTEGER %s, " +
            "service_id INTEGER NOT NULL CHECK(service_id > 0), " +
            "spare_part_id INTEGER NOT NULL CHECK(spare_part_id > 0), " +
            "quantity REAL DEFAULT 1 CHECK(quantity > 0), " +
            "is_required INTEGER DEFAULT 1 CHECK(is_required IN (0, 1)), " +
            "created_date TEXT NOT NULL CHECK(length(created_date) > 0), " +
            "FOREIGN KEY (service_id) REFERENCES services(id), " +
            "FOREIGN KEY (spare_part_id) REFERENCES spare_parts(id)" +
            ")";

    private static final List<String> INDEXES = List.of(
            "CREATE INDEX IF NOT EXISTS idx_orders_client_id ON orders(client_id)",
            "CREATE INDEX IF NOT EXISTS idx_orders_created_date ON orders(created_date)",
            "CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status)",
            "CREATE INDEX IF NOT EXISTS idx_appointments_client_id ON appointments(client_id)",
            "CREATE INDEX IF NOT EXISTS idx_appointments_date ON appointments(appointment_date)",
            "CREATE INDEX IF NOT EXISTS idx_appointments_status ON appointments(status)",
            "CREATE INDEX IF NOT EXISTS idx_order_services_order_id ON order_services(order_id)",
            "CREATE INDEX IF NOT EXISTS idx_order_parts_order_id ON order_parts(order_id)",
            "CREATE INDEX IF NOT EXISTS idx_order_parts_spare_part_id ON order_parts(spare_part_id)",
            "CREATE INDEX IF NOT EXISTS idx_order_services_service_id ON order_services(service_id)",
            "CREATE INDEX IF NOT EXISTS idx_appointments_service_id ON appointments(service_id)",
            "CREATE INDEX IF NOT EXISTS idx_spare_parts_name ON spare_parts(name)",
            "CREATE INDEX IF NOT EXISTS idx_clients_name_lastname_phone ON clients(name, last_name, phone)",
            "CREATE INDEX IF NOT EXISTS idx_client_cars_client_id ON client_cars(client_id)",
            "CREATE INDEX IF NOT EXISTS idx_service_spare_parts_service_id ON service_spare_parts(service_id)",
            "CREATE INDEX IF NOT EXISTS idx_service_spare_parts_spare_part_id ON service_spare_parts(spare_part_id)",
            "CREATE INDEX IF NOT EXISTS idx_to_parts_car_model ON to_parts(car_model)",
            "CREATE INDEX IF NOT EXISTS idx_to_parts_spare_part_id ON to_parts(spare_part_id)",
            "CREATE INDEX IF NOT EXISTS idx_service_spare_parts_lists_service_id ON service_spare_parts_lists(service_id)",
            "CREATE INDEX IF NOT EXISTS idx_service_spare_parts_lists_created_date ON service_spare_parts_lists(created_date)",
            "CREATE INDEX IF NOT EXISTS idx_service_spare_parts_list_items_list_id ON service_spare_parts_list_items(list_id)",
            "CREATE INDEX IF NOT EXISTS idx_service_spare_parts_list_items_spare_part_id ON service_spare_parts_list_items(spare_part_id)",
            "CREATE INDEX IF NOT EXISTS idx_service_parts_service_id ON service_parts(service_id)",
            "CREATE INDEX IF NOT EXISTS idx_service_parts_spare_part_id ON service_parts(spare_part_id)"
    );

    private DatabaseSchema() {
    }

    /**
     * Создать все таблицы и индексы.
     *
     * @param conn         соединение с базой данных
     * @param sqliteDialect {@code true} для SQLite, {@code false} для H2
     * @throws SQLException если DDL не удалось выполнить
     */
    static void createTables(Connection conn, boolean sqliteDialect) throws SQLException {
        String autoIncrement = sqliteDialect ? AUTOINCREMENT_SQLITE : AUTOINCREMENT_H2;
        List<String> statements = List.of(
                String.format(CREATE_CLIENTS, autoIncrement),
                String.format(CREATE_CLIENT_CARS, autoIncrement),
                String.format(CREATE_SERVICES, autoIncrement),
                String.format(CREATE_SPARE_PARTS, autoIncrement),
                String.format(CREATE_ORDERS, autoIncrement),
                String.format(CREATE_ORDER_SERVICES, autoIncrement),
                String.format(CREATE_ORDER_PARTS, autoIncrement),
                String.format(CREATE_APPOINTMENTS, autoIncrement),
                String.format(CREATE_SERVICE_SPARE_PARTS, autoIncrement),
                String.format(CREATE_TO_PARTS, autoIncrement),
                String.format(CREATE_APP_SETTINGS, autoIncrement),
                String.format(CREATE_SERVICE_SPARE_PARTS_LISTS, autoIncrement),
                String.format(CREATE_SERVICE_SPARE_PARTS_LIST_ITEMS, autoIncrement),
                String.format(CREATE_SERVICE_PARTS, autoIncrement)
        );

        try (Statement stmt = conn.createStatement()) {
            for (String ddl : statements) {
                stmt.execute(ddl);
            }
            for (String index : INDEXES) {
                stmt.execute(index);
            }
        }
    }
}
