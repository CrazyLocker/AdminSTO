package com.autoservice;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет, что разбитые по методам миграции SQLite доводят старую схему до
 * актуальной и что повторный запуск ничего не ломает.
 */
class SQLiteMigrationsTest {

    /** Схема примерно того состояния, в котором база была до появления миграций. */
    private static final List<String> LEGACY_DDL = List.of(
            "CREATE TABLE clients ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT NOT NULL CHECK(length(name) > 0), "
                    + "last_name TEXT DEFAULT '', "
                    + "phone TEXT NOT NULL CHECK(length(phone) > 0), "
                    + "car_model TEXT NOT NULL CHECK(length(car_model) > 0), "
                    + "car_number TEXT NOT NULL CHECK(length(car_number) > 0), "
                    + "last_repair_date TEXT DEFAULT ''"
                    + ")",
            "CREATE TABLE spare_parts ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT NOT NULL UNIQUE, "
                    + "retail_price REAL NOT NULL, "
                    + "stock REAL DEFAULT 0"
                    + ")",
            "CREATE TABLE services ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT NOT NULL UNIQUE, "
                    + "price REAL NOT NULL"
                    + ")",
            "CREATE TABLE to_parts (id INTEGER PRIMARY KEY AUTOINCREMENT, part_name TEXT DEFAULT '')",
            "CREATE TABLE order_services (order_id TEXT, service_name TEXT, price REAL)",
            "CREATE TABLE order_parts (order_id TEXT, part_name TEXT, price REAL, quantity INTEGER)",
            "CREATE TABLE appointments ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, order_id TEXT, master_name TEXT, "
                    + "service_name TEXT, date TEXT, time TEXT, status TEXT"
                    + ")",
            "CREATE TABLE orders ("
                    + "id TEXT PRIMARY KEY, client_id INTEGER, status TEXT, total REAL, created_date TEXT"
                    + ")"
    );

    @Test
    void migrationsBringLegacySchemaUpToDate() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            createLegacySchema(conn);
            execute(conn, "INSERT INTO clients (name, phone, car_model, car_number) "
                    + "VALUES ('Иван', '+79001234567', 'Lada', 'А123ВС163')");
            execute(conn, "INSERT INTO orders (id, client_id, status, total, created_date) "
                    + "VALUES ('ZAK-01/01/24-0001', 1, 'NEW', 0, '2024-01-01')");

            SQLiteMigrations.migrate(conn);

            assertThat(tableNames(conn)).contains(
                    "service_spare_parts_lists", "service_spare_parts_list_items",
                    "service_parts", "client_cars");
            assertThat(indexNames(conn)).contains(
                    "idx_service_parts_service_id", "idx_service_parts_spare_part_id",
                    "idx_client_cars_client_id");
            assertThat(columnNames(conn, "spare_parts")).contains("unit_type", "note");
            assertThat(columnNames(conn, "services")).contains(
                    "uses_oil", "spare_part_name", "spare_part_quantity", "category_id");
            assertThat(columnNames(conn, "to_parts")).contains("note");
            assertThat(columnNames(conn, "order_services")).contains("service_id");
            assertThat(columnNames(conn, "order_parts")).contains(
                    "spare_part_id", "unit_type", "purchase_price");
            assertThat(columnNames(conn, "appointments")).contains("service_id");
            assertThat(columnNames(conn, "orders")).contains("closed_date", "notes", "mileage");

            // Миграция clients пересоздаёт таблицу: CHECK на car_model/car_number снят,
            // а данные сохранены.
            String clientsSql = singleString(conn,
                    "SELECT sql FROM sqlite_master WHERE type='table' AND name='clients'");
            assertThat(clientsSql).doesNotContain("CHECK(length(car_model) > 0)");
            assertThat(count(conn, "clients")).isEqualTo(1);
            assertThat(singleString(conn, "SELECT car_model FROM clients"))
                    .isEqualTo("Lada");
        }
    }

    @Test
    void migrationsAreIdempotent() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            DatabaseSchema.createTables(conn, true);

            SQLiteMigrations.migrate(conn);
            SQLiteMigrations.migrate(conn);

            assertThat(columnNames(conn, "orders")).contains("closed_date", "notes", "mileage");
        }
    }

    private static void createLegacySchema(Connection conn) throws SQLException {
        for (String ddl : LEGACY_DDL) {
            execute(conn, ddl);
        }
    }

    private static void execute(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private static List<String> tableNames(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {
            while (rs.next()) {
                tables.add(rs.getString(1).toLowerCase());
            }
        }
        return tables;
    }

    private static List<String> indexNames(Connection conn) throws SQLException {
        List<String> indexes = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='index' AND name NOT LIKE 'sqlite_%'")) {
            while (rs.next()) {
                indexes.add(rs.getString(1).toLowerCase());
            }
        }
        return indexes;
    }

    private static List<String> columnNames(Connection conn, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + table + " WHERE 1 = 0")) {
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                columns.add(rs.getMetaData().getColumnName(i).toLowerCase());
            }
        }
        return columns;
    }

    private static int count(Connection conn, String table) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static String singleString(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }
}
