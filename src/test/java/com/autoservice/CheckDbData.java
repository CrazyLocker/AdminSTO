package com.autoservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class CheckDbData {
    private static final Logger logger = LoggerFactory.getLogger(CheckDbData.class);

    public static void main(String[] args) {
        try {
            Class.forName("org.sqlite.JDBC");
            // Use environment variable for DB URL, fallback to default
            String dbUrl = System.getenv("DB_URL");
            if (dbUrl == null || dbUrl.isBlank()) {
                String dbPath = System.getenv("DB_PATH") != null ? System.getenv("DB_PATH") : "autoservice.db";
                dbUrl = "jdbc:sqlite:" + dbPath;
            }
            Connection conn = DriverManager.getConnection(dbUrl);
            
            // Проверяем таблицу service_spare_parts
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM service_spare_parts")) {
                ResultSet rs = stmt.executeQuery();
                rs.next();
                System.out.println("Service-Spare Parts count: " + rs.getInt(1));
            }
            
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM service_spare_parts")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    System.out.println("  ID: " + rs.getInt("id") + 
                        ", Service ID: " + rs.getInt("service_id") + 
                        ", Spare Part ID: " + rs.getInt("spare_part_id") + 
                        ", Quantity: " + rs.getInt("quantity") + 
                        ", Unit Type: " + rs.getString("unit_type"));
                }
            }
            
            // Проверяем таблицу services
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM services")) {
                ResultSet rs = stmt.executeQuery();
                rs.next();
                System.out.println("Services count: " + rs.getInt(1));
            }
            
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM services")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    System.out.println("  Service ID: " + rs.getInt("id") + ", Name: " + rs.getString("name"));
                }
            }
            
            // Проверяем таблицу spare_parts
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM spare_parts")) {
                ResultSet rs = stmt.executeQuery();
                rs.next();
                System.out.println("Spare Parts count: " + rs.getInt(1));
            }
            
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM spare_parts")) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    System.out.println("  Spare Part ID: " + rs.getInt("id") + ", Name: " + rs.getString("name"));
                }
            }
            
            conn.close();
        } catch (Exception e) {
            logger.error("Ошибка при подключении к БД: {}", e.getMessage());
            System.err.println("Не удалось подключиться к базе данных. Проверьте настройки соединения.");
        }
    }
}
