package com.autoservice;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Реализация Database для H2 (используется в тестах).
 */
public class H2Database extends AbstractDatabase {
    
    @Override
    public void initForTest() {
        // Закрыть старый dataSource если он существует
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        
        // Используем H2 для тестов (in-memory)
        com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5000);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(60000);
        dataSource = new com.zaxxer.hikari.HikariDataSource(config);
        
        // Загрузить драйвер H2 явно
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load H2 driver: " + e.getMessage());
        }
        
        try (Connection conn = getConnection()) {
            createTables(conn);
            System.out.println("Test database (H2) connected");
        } catch (SQLException e) {
            System.err.println("Test DB error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    protected void createTables(Connection conn) throws SQLException {
        String autoIncrement = "AUTO_INCREMENT PRIMARY KEY";
        
        String createClients = "CREATE TABLE IF NOT EXISTS clients (" +
                "id INTEGER " + autoIncrement + ", " +
                "name TEXT NOT NULL, " +
                "last_name TEXT DEFAULT '', " +
                "phone TEXT NOT NULL, " +
                "car_model TEXT NOT NULL, " +
                "car_number TEXT NOT NULL, " +
                "last_repair_date TEXT DEFAULT ''" +
                ")";

        String createServices = "CREATE TABLE IF NOT EXISTS services (" +
                "id INTEGER " + autoIncrement + ", " +
                "name TEXT NOT NULL UNIQUE, " +
                "price REAL NOT NULL, " +
                "duration INTEGER DEFAULT 60, " +
                "part_number TEXT DEFAULT '', " +
                "oil_volume REAL DEFAULT 0, " +
                "uses_oil INTEGER DEFAULT 0, " +
                "spare_part_name TEXT DEFAULT '', " +
                "spare_part_quantity INTEGER DEFAULT 0" +
                ")";

        String createSpareParts = "CREATE TABLE IF NOT EXISTS spare_parts (" +
                "id INTEGER " + autoIncrement + ", " +
                "name TEXT NOT NULL UNIQUE, " +
                "part_number TEXT DEFAULT '', " +
                "manufacturer TEXT DEFAULT '', " +
                "compatible_models TEXT DEFAULT '', " +
                "purchase_price REAL, " +
                "retail_price REAL NOT NULL, " +
                "stock REAL DEFAULT 0, " +
                "min_stock REAL DEFAULT 0, " +
                "location TEXT DEFAULT '', " +
                "unit_volume REAL DEFAULT 1.0, " +
                "unit_type TEXT DEFAULT 'шт', " +
                "is_liquid INTEGER DEFAULT 0" +
                ")";

        String createOrders = "CREATE TABLE IF NOT EXISTS orders (" +
                "id TEXT PRIMARY KEY, " +
                "client_id INTEGER NOT NULL, " +
                "status TEXT NOT NULL, " +
                "total REAL NOT NULL, " +
                "created_date TEXT NOT NULL, " +
                "FOREIGN KEY (client_id) REFERENCES clients(id)" +
                ")";

        String createOrderServices = "CREATE TABLE IF NOT EXISTS order_services (" +
                "order_id TEXT NOT NULL, " +
                "service_name TEXT NOT NULL, " +
                "price REAL NOT NULL, " +
                "FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE" +
                ")";

        String createOrderParts = "CREATE TABLE IF NOT EXISTS order_parts (" +
                "order_id TEXT NOT NULL, " +
                "part_name TEXT NOT NULL, " +
                "price REAL NOT NULL, " +
                "quantity INTEGER NOT NULL, " +
                "FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE" +
                ")";

        String createAppointments = "CREATE TABLE IF NOT EXISTS appointments (" +
                "id INTEGER " + autoIncrement + ", " +
                "client_id INTEGER NOT NULL, " +
                "order_id TEXT, " +
                "master_name TEXT NOT NULL, " +
                "service_name TEXT NOT NULL, " +
                "appointment_date TEXT NOT NULL, " +
                "appointment_time TEXT NOT NULL, " +
                "status TEXT NOT NULL, " +
                "FOREIGN KEY (client_id) REFERENCES clients(id), " +
                "FOREIGN KEY (order_id) REFERENCES orders(id)" +
                ")";

        String createServiceSpareParts = "CREATE TABLE IF NOT EXISTS service_spare_parts (" +
                "id INTEGER " + autoIncrement + ", " +
                "service_id INTEGER NOT NULL, " +
                "spare_part_id INTEGER NOT NULL, " +
                "quantity INTEGER DEFAULT 1, " +
                "unit_type TEXT DEFAULT 'шт', " +
                "active INTEGER DEFAULT 1, " +
                "FOREIGN KEY (service_id) REFERENCES services(id), " +
                "FOREIGN KEY (spare_part_id) REFERENCES spare_parts(id)" +
                ")";

        String createToParts = "CREATE TABLE IF NOT EXISTS to_parts (" +
                "id INTEGER " + autoIncrement + ", " +
                "car_model TEXT NOT NULL, " +
                "spare_part_id INTEGER NOT NULL, " +
                "quantity INTEGER DEFAULT 1, " +
                "unit_type TEXT DEFAULT 'шт', " +
                "active INTEGER DEFAULT 1" +
                ")";

        String createAppSettings = "CREATE TABLE IF NOT EXISTS app_settings (" +
                "id INTEGER " + autoIncrement + ", " +
                "setting_key TEXT NOT NULL UNIQUE, " +
                "setting_value TEXT NOT NULL, " +
                "description TEXT DEFAULT ''" +
                ")";

        String createServiceSparePartsLists = "CREATE TABLE IF NOT EXISTS service_spare_parts_lists (" +
                "id INTEGER " + autoIncrement + ", " +
                "service_id INTEGER NOT NULL, " +
                "created_date TEXT NOT NULL, " +
                "active INTEGER DEFAULT 1, " +
                "FOREIGN KEY (service_id) REFERENCES services(id)" +
                ")";

        String createServiceSparePartsListItems = "CREATE TABLE IF NOT EXISTS service_spare_parts_list_items (" +
                "id INTEGER " + autoIncrement + ", " +
                "list_id INTEGER NOT NULL, " +
                "spare_part_id INTEGER NOT NULL, " +
                "quantity INTEGER DEFAULT 1, " +
                "unit_type TEXT DEFAULT 'шт', " +
                "FOREIGN KEY (list_id) REFERENCES service_spare_parts_lists(id), " +
                "FOREIGN KEY (spare_part_id) REFERENCES spare_parts(id)" +
                ")";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createClients);
            stmt.execute(createServices);
            stmt.execute(createSpareParts);
            stmt.execute(createOrders);
            stmt.execute(createOrderServices);
            stmt.execute(createOrderParts);
            stmt.execute(createAppointments);
            stmt.execute(createServiceSpareParts);
            stmt.execute(createToParts);
            stmt.execute(createAppSettings);
            stmt.execute(createServiceSparePartsLists);
            stmt.execute(createServiceSparePartsListItems);
            createIndexes(conn);
            System.out.println("Tables and indexes created/verified");
        }
    }
    
    private void createIndexes(Connection conn) throws SQLException {
        String[] indexes = {
                "CREATE INDEX IF NOT EXISTS idx_orders_client_id ON orders(client_id)",
                "CREATE INDEX IF NOT EXISTS idx_orders_created_date ON orders(created_date)",
                "CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status)",
                "CREATE INDEX IF NOT EXISTS idx_appointments_client_id ON appointments(client_id)",
                "CREATE INDEX IF NOT EXISTS idx_appointments_date ON appointments(appointment_date)",
                "CREATE INDEX IF NOT EXISTS idx_appointments_status ON appointments(status)",
                "CREATE INDEX IF NOT EXISTS idx_order_services_order_id ON order_services(order_id)",
                "CREATE INDEX IF NOT EXISTS idx_order_parts_order_id ON order_parts(order_id)",
                "CREATE INDEX IF NOT EXISTS idx_spare_parts_name ON spare_parts(name)",
                "CREATE INDEX IF NOT EXISTS idx_clients_name_lastname_phone ON clients(name, last_name, phone)",
                "CREATE INDEX IF NOT EXISTS idx_service_spare_parts_service_id ON service_spare_parts(service_id)",
                "CREATE INDEX IF NOT EXISTS idx_service_spare_parts_spare_part_id ON service_spare_parts(spare_part_id)",
                "CREATE INDEX IF NOT EXISTS idx_to_parts_car_model ON to_parts(car_model)",
                "CREATE INDEX IF NOT EXISTS idx_to_parts_spare_part_id ON to_parts(spare_part_id)",
                "CREATE INDEX IF NOT EXISTS idx_service_spare_parts_lists_service_id ON service_spare_parts_lists(service_id)",
                "CREATE INDEX IF NOT EXISTS idx_service_spare_parts_lists_created_date ON service_spare_parts_lists(created_date)",
                "CREATE INDEX IF NOT EXISTS idx_service_spare_parts_list_items_list_id ON service_spare_parts_list_items(list_id)",
                "CREATE INDEX IF NOT EXISTS idx_service_spare_parts_list_items_spare_part_id ON service_spare_parts_list_items(spare_part_id)"
        };
        
        try (Statement stmt = conn.createStatement()) {
            for (String index : indexes) {
                stmt.execute(index);
            }
        }
    }
    
    @Override
    protected String generateOrderId(Connection conn) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yy"));
        String sql = "SELECT MAX(id) as max_id FROM orders WHERE id LIKE 'ZAK-%'";
        String lastOrderId = null;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                lastOrderId = rs.getString("max_id");
            }
        } catch (SQLException e) {
            System.err.println("Generate ID error: " + e.getMessage());
        }

        int newNumber = 1;
        if (lastOrderId != null) {
            try {
                // Extract number after the last dash
                String[] parts = lastOrderId.split("-");
                if (parts.length >= 2) {
                    newNumber = Integer.parseInt(parts[parts.length - 1]) + 1;
                }
            } catch (NumberFormatException e) {
                System.err.println("Parse ID error: " + e.getMessage());
            }
        }

        // Handle overflow
        if (newNumber > 9999) {
            newNumber = 1;
        }
        
        return String.format("ZAK-%s-%04d", date, newNumber);
    }
    
    // ==================== CLIENTS ====================
    
    // Используются методы из AbstractDatabase
    
    // ==================== SERVICES ====================
    
    @Override
    public void addService(Service service) {
        String sql = "MERGE INTO services (name, price, duration, part_number, oil_volume, uses_oil, spare_part_name, spare_part_quantity) KEY(name) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, service.getName());
            pstmt.setDouble(2, service.getPrice());
            pstmt.setInt(3, service.getDuration());
            pstmt.setString(4, service.getPartNumber());
            pstmt.setDouble(5, service.getOilVolume());
            pstmt.setInt(6, service.isUsesOil() ? 1 : 0);
            pstmt.setString(7, service.getSparePartName());
            pstmt.setInt(8, service.getSparePartQuantity());
            pstmt.executeUpdate();
            
            // Получаем сгенерированный id
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                service.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("Add service error: " + e.getMessage());
        }
    }
    
    // ==================== SPARE PARTS ====================
    
    @Override
    public void addSparePart(SparePart part) {
        String sql = "MERGE INTO spare_parts (name, purchase_price, retail_price, stock, part_number, manufacturer, compatible_models, min_stock, location, unit_type) KEY(name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, part.getName());
            pstmt.setDouble(2, part.getPurchasePrice());
            pstmt.setDouble(3, part.getRetailPrice());
            pstmt.setDouble(4, part.getStock());
            pstmt.setString(5, part.getPartNumber());
            pstmt.setString(6, part.getManufacturer());
            pstmt.setString(7, part.getCompatibleModels());
            pstmt.setDouble(8, part.getMinStock());
            pstmt.setString(9, part.getLocation());
            pstmt.setString(10, part.getUnitType());
            pstmt.executeUpdate();
            
            // Получаем сгенерированный id
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                part.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("Add spare part error: " + e.getMessage());
        }
    }

    @Override
    public void updateSparePartStock(SparePart part, double newStock) {
        String sql = "UPDATE spare_parts SET stock = ? WHERE name = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newStock);
            pstmt.setString(2, part.getName());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Update spare part stock error: " + e.getMessage());
        }
    }

    // ==================== ORDERS ====================

    @Override
    public void addOrder(WorkOrder order) {
        try (Connection conn = getConnection()) {
            int clientId = getClientId(order.getClient());
            if (clientId == -1) {
                System.err.println("Client not found, order not saved");
                return;
            }

            String orderId = generateOrderId(conn);
            order.setId(orderId);

            // Получаем текущую дату в нужном формате
            String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yy"));

            conn.setAutoCommit(false);

            String sql = "INSERT INTO orders (id, client_id, status, total, created_date) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, orderId);
                pstmt.setInt(2, clientId);
                pstmt.setString(3, order.getStatus());
                pstmt.setDouble(4, order.getTotal());
                pstmt.setString(5, currentDate);
                pstmt.executeUpdate();
            }

            saveOrderServices(conn, orderId, order);
            saveOrderParts(conn, orderId, order);

            conn.commit();
            System.out.println("Order saved: " + orderId);

        } catch (SQLException e) {
            System.err.println("Add order error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void updateOrder(WorkOrder order) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            // Update order header
            String orderSql = "UPDATE orders SET client_id = ?, status = ?, total = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(orderSql)) {
                int clientId = getClientId(order.getClient());
                pstmt.setInt(1, clientId);
                pstmt.setString(2, order.getStatus());
                pstmt.setDouble(3, order.getTotal());
                pstmt.setString(4, order.getId());
                pstmt.executeUpdate();
            }
            
            // Clear existing services and parts
            String deleteServicesSql = "DELETE FROM order_services WHERE order_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteServicesSql)) {
                pstmt.setString(1, order.getId());
                pstmt.executeUpdate();
            }
            
            String deletePartsSql = "DELETE FROM order_parts WHERE order_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deletePartsSql)) {
                pstmt.setString(1, order.getId());
                pstmt.executeUpdate();
            }
            
            // Save new services
            saveOrderServices(conn, order.getId(), order);
            
            // Save new parts
            saveOrderParts(conn, order.getId(), order);
            
            conn.commit();
            System.out.println("Order updated: " + order.getId());
            
        } catch (SQLException e) {
            System.err.println("Update order error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void saveOrderServices(Connection conn, String orderId, WorkOrder order) throws SQLException {
        String sql = "INSERT INTO order_services (order_id, service_name, price) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < order.getServices().size(); i++) {
                pstmt.setString(1, orderId);
                pstmt.setString(2, order.getServices().get(i));
                pstmt.setDouble(3, order.getServicePrices().get(i));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
    
    private void saveOrderParts(Connection conn, String orderId, WorkOrder order) throws SQLException {
        String sql = "INSERT INTO order_parts (order_id, part_name, price, quantity) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < order.getSpareParts().size(); i++) {
                pstmt.setString(1, orderId);
                pstmt.setString(2, order.getSpareParts().get(i).getName());
                pstmt.setDouble(3, order.getSpareParts().get(i).getRetailPrice());
                pstmt.setDouble(4, order.getSparePartQuantities().get(i));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
    
    // ==================== APPOINTMENTS ====================
    
    @Override
    public void addAppointment(Appointment appointment) {
        String sql = "INSERT INTO appointments (client_id, order_id, master_name, service_name, appointment_date, appointment_time, status) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int clientId = getClientId(appointment.getClient());
            pstmt.setInt(1, clientId);
            pstmt.setString(2, appointment.getOrderId());
            pstmt.setString(3, appointment.getMasterName());
            pstmt.setString(4, appointment.getServiceName());
            pstmt.setString(5, appointment.getDate());
            pstmt.setString(6, appointment.getTime());
            pstmt.setString(7, appointment.getStatus());
            pstmt.executeUpdate();
            
            // Получаем сгенерированный id
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                appointment.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("Add appointment error: " + e.getMessage());
        }
    }
    
    @Override
    public void updateAppointment(Appointment appointment) {
        String sql = "UPDATE appointments SET client_id = ?, master_name = ?, service_name = ?, appointment_date = ?, appointment_time = ?, status = ?, order_id = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int clientId = getClientId(appointment.getClient());
            pstmt.setInt(1, clientId);
            pstmt.setString(2, appointment.getMasterName());
            pstmt.setString(3, appointment.getServiceName());
            pstmt.setString(4, appointment.getDate());
            pstmt.setString(5, appointment.getTime());
            pstmt.setString(6, appointment.getStatus());
            pstmt.setString(7, appointment.getOrderId());
            pstmt.setInt(8, appointment.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Update appointment error: " + e.getMessage());
        }
    }
    
    @Override
    public void deleteAppointment(int id) {
        String sql = "DELETE FROM appointments WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Delete appointment error: " + e.getMessage());
        }
    }
}
