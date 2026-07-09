package com.autoservice;

import java.sql.*;

public class CheckDbData {
    public static void main(String[] args) {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection("jdbc:sqlite:autoservice.db");
            
            // Проверяем таблицу service_spare_parts
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM service_spare_parts");
            rs.next();
            System.out.println("Service-Spare Parts count: " + rs.getInt(1));
            
            rs = stmt.executeQuery("SELECT * FROM service_spare_parts");
            while (rs.next()) {
                System.out.println("  ID: " + rs.getInt("id") + 
                    ", Service ID: " + rs.getInt("service_id") + 
                    ", Spare Part ID: " + rs.getInt("spare_part_id") + 
                    ", Quantity: " + rs.getInt("quantity") + 
                    ", Unit Type: " + rs.getString("unit_type"));
            }
            
            // Проверяем таблицу services
            rs = stmt.executeQuery("SELECT COUNT(*) FROM services");
            rs.next();
            System.out.println("Services count: " + rs.getInt(1));
            
            rs = stmt.executeQuery("SELECT * FROM services");
            while (rs.next()) {
                System.out.println("  Service ID: " + rs.getInt("id") + ", Name: " + rs.getString("name"));
            }
            
            // Проверяем таблицу spare_parts
            rs = stmt.executeQuery("SELECT COUNT(*) FROM spare_parts");
            rs.next();
            System.out.println("Spare Parts count: " + rs.getInt(1));
            
            rs = stmt.executeQuery("SELECT * FROM spare_parts");
            while (rs.next()) {
                System.out.println("  Spare Part ID: " + rs.getInt("id") + ", Name: " + rs.getString("name"));
            }
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
