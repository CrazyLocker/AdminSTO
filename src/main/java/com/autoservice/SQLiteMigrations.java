package com.autoservice;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Миграции схемы SQLite.
 *
 * <p>Раньше все миграции были одним методом {@code SQLiteDatabase.migrateTables}
 * на 227 строк: добавление колонок, создание таблиц и DDL в одном теле. Теперь
 * каждая миграция — отдельный метод, DDL вынесен в константы, а порядок
 * применения задаётся списком {@link #MIGRATIONS}. Список только дополняется:
 * новые миграции добавляются в конец.</p>
 *
 * <p>Все проверки «колонки/таблицы ещё нет» сохранены, поэтому повторный запуск
 * миграций на уже обновлённой базе ничего не меняет.</p>
 */
final class SQLiteMigrations {

    private static final Logger logger = LoggerFactory.getLogger(SQLiteMigrations.class);

    // ==================== DDL: КОЛОНКИ ====================

    private static final String ADD_SPARE_PARTS_UNIT_TYPE =
            "ALTER TABLE spare_parts ADD COLUMN unit_type TEXT DEFAULT 'шт' "
                    + "CHECK(unit_type IN ('шт', 'л', 'компл'))";
    private static final String ADD_SPARE_PARTS_NOTE =
            "ALTER TABLE spare_parts ADD COLUMN note TEXT DEFAULT ''";
    private static final String ADD_SERVICES_USES_OIL =
            "ALTER TABLE services ADD COLUMN uses_oil INTEGER DEFAULT 0 CHECK(uses_oil IN (0, 1))";
    private static final String ADD_SERVICES_SPARE_PART_NAME =
            "ALTER TABLE services ADD COLUMN spare_part_name TEXT DEFAULT ''";
    private static final String ADD_SERVICES_SPARE_PART_QUANTITY =
            "ALTER TABLE services ADD COLUMN spare_part_quantity INTEGER DEFAULT 0";
    private static final String ADD_TO_PARTS_NOTE =
            "ALTER TABLE to_parts ADD COLUMN note TEXT DEFAULT ''";
    private static final String ADD_ORDER_SERVICES_SERVICE_ID =
            "ALTER TABLE order_services ADD COLUMN service_id INTEGER DEFAULT 0";
    private static final String ADD_ORDER_PARTS_SPARE_PART_ID =
            "ALTER TABLE order_parts ADD COLUMN spare_part_id INTEGER DEFAULT 0";
    private static final String ADD_ORDER_PARTS_UNIT_TYPE =
            "ALTER TABLE order_parts ADD COLUMN unit_type TEXT DEFAULT 'шт'";
    private static final String ADD_ORDER_PARTS_PURCHASE_PRICE =
            "ALTER TABLE order_parts ADD COLUMN purchase_price REAL DEFAULT 0";
    private static final String ADD_APPOINTMENTS_SERVICE_ID =
            "ALTER TABLE appointments ADD COLUMN service_id INTEGER DEFAULT 0";
    private static final String ADD_ORDERS_CLOSED_DATE =
            "ALTER TABLE orders ADD COLUMN closed_date TEXT DEFAULT ''";
    private static final String ADD_ORDERS_NOTES =
            "ALTER TABLE orders ADD COLUMN notes TEXT DEFAULT ''";
    private static final String ADD_SERVICES_CATEGORY_ID =
            "ALTER TABLE services ADD COLUMN category_id INTEGER DEFAULT 0";
    private static final String ADD_ORDERS_MILEAGE =
            "ALTER TABLE orders ADD COLUMN mileage INTEGER DEFAULT 0 CHECK(mileage >= 0)";
    private static final String ADD_SERVICES_CATEGORY =
            "ALTER TABLE services ADD COLUMN category TEXT DEFAULT 'REGULAR' "
                    + "CHECK(category IN ('REGULAR', 'MAINTENANCE', 'TUNING'))";
    private static final String ADD_SPARE_PARTS_CATEGORY =
            "ALTER TABLE spare_parts ADD COLUMN category TEXT DEFAULT 'REGULAR' "
                    + "CHECK(category IN ('REGULAR', 'MAINTENANCE', 'TUNING'))";
    private static final String ADD_CLIENTS_MAINTENANCE_STARS =
            "ALTER TABLE clients ADD COLUMN maintenance_stars INTEGER DEFAULT 0 "
                    + "CHECK(maintenance_stars >= 0)";
    private static final String ADD_CLIENTS_TUNING_TOTAL =
            "ALTER TABLE clients ADD COLUMN tuning_total REAL DEFAULT 0 CHECK(tuning_total >= 0)";

    // ==================== DDL: ТАБЛИЦЫ ====================

    private static final String CREATE_SERVICE_SPARE_PARTS_LISTS =
            "CREATE TABLE service_spare_parts_lists ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "service_id INTEGER NOT NULL CHECK(service_id > 0), "
                    + "created_date TEXT NOT NULL CHECK(length(created_date) > 0), "
                    + "active INTEGER DEFAULT 1 CHECK(active IN (0, 1)), "
                    + "FOREIGN KEY (service_id) REFERENCES services(id)"
                    + ")";

    private static final String CREATE_SERVICE_SPARE_PARTS_LIST_ITEMS =
            "CREATE TABLE service_spare_parts_list_items ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "list_id INTEGER NOT NULL CHECK(list_id > 0), "
                    + "spare_part_id INTEGER NOT NULL CHECK(spare_part_id > 0), "
                    + "quantity INTEGER DEFAULT 1 CHECK(quantity > 0), "
                    + "unit_type TEXT DEFAULT 'шт' CHECK(unit_type IN ('шт', 'л', 'компл')), "
                    + "FOREIGN KEY (list_id) REFERENCES service_spare_parts_lists(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (spare_part_id) REFERENCES spare_parts(id)"
                    + ")";

    private static final String CREATE_SERVICE_PARTS =
            "CREATE TABLE service_parts ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "service_id INTEGER NOT NULL CHECK(service_id > 0), "
                    + "spare_part_id INTEGER NOT NULL CHECK(spare_part_id > 0), "
                    + "quantity REAL DEFAULT 1 CHECK(quantity > 0), "
                    + "is_required INTEGER DEFAULT 1 CHECK(is_required IN (0, 1)), "
                    + "created_date TEXT NOT NULL CHECK(length(created_date) > 0), "
                    + "FOREIGN KEY (service_id) REFERENCES services(id), "
                    + "FOREIGN KEY (spare_part_id) REFERENCES spare_parts(id)"
                    + ")";

    private static final String CREATE_CLIENT_CARS =
            "CREATE TABLE client_cars ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "client_id INTEGER NOT NULL CHECK(client_id > 0), "
                    + "car_model TEXT NOT NULL CHECK(length(car_model) > 0), "
                    + "car_number TEXT NOT NULL CHECK(length(car_number) > 0), "
                    + "mileage INTEGER DEFAULT 0 CHECK(mileage >= 0), "
                    + "FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE"
                    + ")";

    // ==================== DDL: ИНДЕКСЫ ====================

    private static final String CREATE_INDEX_SERVICE_PARTS_SERVICE_ID =
            "CREATE INDEX IF NOT EXISTS idx_service_parts_service_id ON service_parts(service_id)";
    private static final String CREATE_INDEX_SERVICE_PARTS_SPARE_PART_ID =
            "CREATE INDEX IF NOT EXISTS idx_service_parts_spare_part_id ON service_parts(spare_part_id)";
    private static final String CREATE_INDEX_CLIENT_CARS_CLIENT_ID =
            "CREATE INDEX IF NOT EXISTS idx_client_cars_client_id ON client_cars(client_id)";

    // ==================== DDL: ПЕРЕСОЗДАНИЕ clients ====================

    private static final String RENAME_CLIENTS_TO_OLD = "ALTER TABLE clients RENAME TO clients_old";

    private static final String CREATE_CLIENTS_WITHOUT_CAR_CHECKS =
            "CREATE TABLE clients ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT NOT NULL CHECK(length(name) > 0), "
                    + "last_name TEXT DEFAULT '', "
                    + "phone TEXT NOT NULL CHECK(length(phone) > 0), "
                    + "car_model TEXT DEFAULT '', "
                    + "car_number TEXT DEFAULT '', "
                    + "last_repair_date TEXT DEFAULT ''"
                    + ")";

    private static final String COPY_CLIENTS_FROM_OLD =
            "INSERT INTO clients (id, name, last_name, phone, car_model, car_number, last_repair_date) "
                    + "SELECT id, name, last_name, phone, car_model, car_number, last_repair_date FROM clients_old";

    private static final String DROP_OLD_CLIENTS = "DROP TABLE clients_old";

    /** Одна миграция схемы. */
    @FunctionalInterface
    private interface Migration {
        void apply(Connection conn) throws SQLException;
    }

    /** Миграции в порядке применения: порядок важен для старых баз. */
    private static final List<Migration> MIGRATIONS = List.of(
            SQLiteMigrations::addUnitTypeToSpareParts,
            SQLiteMigrations::addNoteToSpareParts,
            SQLiteMigrations::addUsesOilToServices,
            SQLiteMigrations::addSparePartNameToServices,
            SQLiteMigrations::addSparePartQuantityToServices,
            SQLiteMigrations::addNoteToToParts,
            SQLiteMigrations::createServiceSparePartsLists,
            SQLiteMigrations::createServiceSparePartsListItems,
            SQLiteMigrations::addServiceIdToOrderServices,
            SQLiteMigrations::addSparePartIdToOrderParts,
            SQLiteMigrations::addUnitTypeToOrderParts,
            SQLiteMigrations::addPurchasePriceToOrderParts,
            SQLiteMigrations::addServiceIdToAppointments,
            SQLiteMigrations::addClosedDateToOrders,
            SQLiteMigrations::addNotesToOrders,
            SQLiteMigrations::createServiceParts,
            SQLiteMigrations::addCategoryIdToServices,
            SQLiteMigrations::addMileageToOrders,
            SQLiteMigrations::createClientCars,
            SQLiteMigrations::migrateClientsRemoveCheckConstraints,
            // Миграции лояльности идут после пересоздания clients: оно копирует
            // только старые колонки, поэтому новые нужно добавлять уже после него.
            SQLiteMigrations::addCategoryToServices,
            SQLiteMigrations::addCategoryToSpareParts,
            SQLiteMigrations::addMaintenanceStarsToClients,
            SQLiteMigrations::addTuningTotalToClients
    );

    private SQLiteMigrations() {
    }

    /**
     * Применить все миграции к базе.
     * @param conn соединение с базой данных
     * @throws SQLException если миграция не удалась
     */
    static void migrate(Connection conn) throws SQLException {
        for (Migration migration : MIGRATIONS) {
            migration.apply(conn);
        }
    }

    // ==================== КОЛОНКИ ====================

    private static void addUnitTypeToSpareParts(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "spare_parts", "unit_type", ADD_SPARE_PARTS_UNIT_TYPE);
    }

    private static void addNoteToSpareParts(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "spare_parts", "note", ADD_SPARE_PARTS_NOTE);
    }

    private static void addUsesOilToServices(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "services", "uses_oil", ADD_SERVICES_USES_OIL);
    }

    private static void addSparePartNameToServices(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "services", "spare_part_name", ADD_SERVICES_SPARE_PART_NAME);
    }

    private static void addSparePartQuantityToServices(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "services", "spare_part_quantity", ADD_SERVICES_SPARE_PART_QUANTITY);
    }

    private static void addNoteToToParts(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "to_parts", "note", ADD_TO_PARTS_NOTE);
    }

    private static void addServiceIdToOrderServices(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "order_services", "service_id", ADD_ORDER_SERVICES_SERVICE_ID);
    }

    private static void addSparePartIdToOrderParts(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "order_parts", "spare_part_id", ADD_ORDER_PARTS_SPARE_PART_ID);
    }

    private static void addUnitTypeToOrderParts(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "order_parts", "unit_type", ADD_ORDER_PARTS_UNIT_TYPE);
    }

    private static void addPurchasePriceToOrderParts(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "order_parts", "purchase_price", ADD_ORDER_PARTS_PURCHASE_PRICE);
    }

    private static void addServiceIdToAppointments(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "appointments", "service_id", ADD_APPOINTMENTS_SERVICE_ID);
    }

    private static void addClosedDateToOrders(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "orders", "closed_date", ADD_ORDERS_CLOSED_DATE);
    }

    private static void addNotesToOrders(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "orders", "notes", ADD_ORDERS_NOTES);
    }

    private static void addCategoryIdToServices(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "services", "category_id", ADD_SERVICES_CATEGORY_ID);
    }

    private static void addMileageToOrders(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "orders", "mileage", ADD_ORDERS_MILEAGE);
    }

    private static void addCategoryToServices(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "services", "category", ADD_SERVICES_CATEGORY);
    }

    private static void addCategoryToSpareParts(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "spare_parts", "category", ADD_SPARE_PARTS_CATEGORY);
    }

    private static void addMaintenanceStarsToClients(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "clients", "maintenance_stars", ADD_CLIENTS_MAINTENANCE_STARS);
    }

    private static void addTuningTotalToClients(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "clients", "tuning_total", ADD_CLIENTS_TUNING_TOTAL);
    }

    // ==================== ТАБЛИЦЫ ====================

    private static void createServiceSparePartsLists(Connection conn) throws SQLException {
        if (tableExists(conn, "service_spare_parts_lists")) {
            return;
        }
        execute(conn, CREATE_SERVICE_SPARE_PARTS_LISTS);
        logger.info("Создана таблица service_spare_parts_lists с CHECK constraints");
    }

    private static void createServiceSparePartsListItems(Connection conn) throws SQLException {
        if (tableExists(conn, "service_spare_parts_list_items")) {
            return;
        }
        execute(conn, CREATE_SERVICE_SPARE_PARTS_LIST_ITEMS);
        logger.info("Создана таблица service_spare_parts_list_items с CHECK constraints");
    }

    private static void createServiceParts(Connection conn) throws SQLException {
        if (tableExists(conn, "service_parts")) {
            return;
        }
        execute(conn, CREATE_SERVICE_PARTS);
        execute(conn, CREATE_INDEX_SERVICE_PARTS_SERVICE_ID);
        execute(conn, CREATE_INDEX_SERVICE_PARTS_SPARE_PART_ID);
        logger.info("Создана таблица service_parts с CHECK constraints и индексами");
    }

    private static void createClientCars(Connection conn) throws SQLException {
        if (tableExists(conn, "client_cars")) {
            return;
        }
        execute(conn, CREATE_CLIENT_CARS);
        execute(conn, CREATE_INDEX_CLIENT_CARS_CLIENT_ID);
        logger.info("Создана таблица client_cars с CHECK constraints и индексом");
    }

    // ==================== СПЕЦИАЛЬНЫЕ МИГРАЦИИ ====================

    /**
     * Удаляет CHECK-ограничения NOT NULL с car_model и car_number в таблице clients.
     * SQLite не поддерживает ALTER TABLE DROP CONSTRAINT, поэтому таблица пересоздаётся.
     */
    private static void migrateClientsRemoveCheckConstraints(Connection conn) throws SQLException {
        String createSql = clientsCreateSql(conn);
        // Если CREATE содержит CHECK(length(car_model) > 0) — нужно мигрировать
        if (createSql == null || !createSql.contains("CHECK(length(car_model) > 0)")) {
            return;
        }
        logger.info("Миграция: удаляем CHECK constraints с car_model/car_number в clients");
        // ResultSet уже закрыт: SQLite не даёт менять схему, пока открыт запрос к sqlite_master.
        execute(conn, RENAME_CLIENTS_TO_OLD);
        execute(conn, CREATE_CLIENTS_WITHOUT_CAR_CHECKS);
        execute(conn, COPY_CLIENTS_FROM_OLD);
        execute(conn, DROP_OLD_CLIENTS);
        logger.info("Миграция clients завершена");
    }

    /**
     * Прочитать DDL таблицы clients.
     * @param conn соединение с базой данных
     * @return CREATE-выражение таблицы или {@code null}, если таблицы нет
     */
    private static String clientsCreateSql(Connection conn) throws SQLException {
        String sql = "SELECT sql FROM sqlite_master WHERE type='table' AND name='clients'";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getString("sql") : null;
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Добавить колонку, если её ещё нет в таблице.
     */
    private static void addColumnIfMissing(Connection conn, String tableName,
                                           String columnName, String ddl) throws SQLException {
        if (columnExists(conn, tableName, columnName)) {
            return;
        }
        execute(conn, ddl);
        logger.info("Добавлена колонка {} в {}", columnName, tableName);
    }

    private static void execute(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName)
            throws SQLException {
        String sql = "PRAGMA table_info(" + tableName + ")";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                if (rs.getString("name").equals(columnName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
