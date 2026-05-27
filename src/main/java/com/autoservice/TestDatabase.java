package com.autoservice;

import java.sql.*;

public class TestDatabase {
    private static final String TEST_DB_URL = "jdbc:sqlite:test.db";
    private static Connection connection;

    public static void init() {
        try {
            connection = DriverManager.getConnection(TEST_DB_URL);
            createTables();
            System.out.println("Тестовая база данных подключена");
        } catch (SQLException e) {
            System.err.println("Ошибка подключения к тестовой БД: " + e.getMessage());
        }
    }

    private static void createTables() throws SQLException {
        String createClients = "CREATE TABLE IF NOT EXISTS clients (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, phone TEXT NOT NULL, " +
                "car_model TEXT NOT NULL, car_number TEXT NOT NULL)";

        String createServices = "CREATE TABLE IF NOT EXISTS services (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE, price REAL NOT NULL)";

        String createSpareParts = "CREATE TABLE IF NOT EXISTS spare_parts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE, purchase_price REAL NOT NULL, " +
                "retail_price REAL NOT NULL, stock INTEGER NOT NULL)";

        String createOrders = "CREATE TABLE IF NOT EXISTS orders (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "client_id INTEGER NOT NULL, status TEXT NOT NULL, " +
                "total REAL NOT NULL, created_date TEXT NOT NULL)";

        String createOrderServices = "CREATE TABLE IF NOT EXISTS order_services (" +
                "order_id INTEGER NOT NULL, service_name TEXT NOT NULL, price REAL NOT NULL)";

        String createOrderParts = "CREATE TABLE IF NOT EXISTS order_parts (" +
                "order_id INTEGER NOT NULL, part_name TEXT NOT NULL, " +
                "price REAL NOT NULL, quantity INTEGER NOT NULL)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createClients);
            stmt.execute(createServices);
            stmt.execute(createSpareParts);
            stmt.execute(createOrders);
            stmt.execute(createOrderServices);
            stmt.execute(createOrderParts);
        }
    }

    public static void clearAll() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM order_parts");
            stmt.execute("DELETE FROM order_services");
            stmt.execute("DELETE FROM orders");
            stmt.execute("DELETE FROM spare_parts");
            stmt.execute("DELETE FROM services");
            stmt.execute("DELETE FROM clients");
            stmt.execute("DELETE FROM sqlite_sequence");
        } catch (SQLException e) {
            System.err.println("Ошибка очистки: " + e.getMessage());
        }
    }

    public static Connection getConnection() {
        return connection;
    }

    public static void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            System.err.println("Ошибка закрытия: " + e.getMessage());
        }
    }
}