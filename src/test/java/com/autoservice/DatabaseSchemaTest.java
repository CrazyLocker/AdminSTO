package com.autoservice;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет, что общий DDL {@link DatabaseSchema} выполняется на обоих диалектах
 * и что схема {@code spare_parts} больше не содержит legacy-колонок H2.
 */
class DatabaseSchemaTest {

    private static final List<String> EXPECTED_TABLES = List.of(
            "app_settings", "appointments", "client_cars", "clients",
            "order_parts", "order_services", "orders", "service_parts",
            "service_spare_parts", "service_spare_parts_list_items",
            "service_spare_parts_lists", "services", "spare_parts", "to_parts");

    @Test
    void sharedSchemaIsCreatedForSqlite() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            DatabaseSchema.createTables(conn, true);
            assertThat(tableNames(conn)).containsAll(EXPECTED_TABLES);
            assertThat(columnNames(conn, "spare_parts")).doesNotContain("unit_volume", "is_liquid");
        }
    }

    @Test
    void sharedSchemaIsCreatedForH2() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:schema-test;DB_CLOSE_DELAY=-1")) {
            DatabaseSchema.createTables(conn, false);
            assertThat(tableNames(conn)).containsAll(EXPECTED_TABLES);
            assertThat(columnNames(conn, "spare_parts")).doesNotContain("unit_volume", "is_liquid");
        }
    }

    private static List<String> tableNames(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();
        String url = conn.getMetaData().getURL();
        String sql = url != null && url.contains("sqlite")
                ? "SELECT name FROM sqlite_master WHERE type='table'"
                : "SELECT table_name FROM information_schema.tables WHERE table_schema = 'PUBLIC'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tables.add(rs.getString(1).toLowerCase());
            }
        }
        return tables;
    }

    private static List<String> columnNames(Connection conn, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + table + " WHERE 1 = 0")) {
            ResultSetMetaData metaData = rs.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                columns.add(metaData.getColumnName(i).toLowerCase());
            }
        }
        return columns;
    }
}
