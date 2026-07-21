package com.autoservice;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zaxxer.hikari.HikariDataSource;
import com.autoservice.utils.ExceptionHandler;
import com.autoservice.Client;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.WorkOrder;
import com.autoservice.Appointment;

/**
 * Реализация Database для H2 (используется в тестах).
 */
public class H2Database extends AbstractDatabase {
    
    private static final Logger logger = LoggerFactory.getLogger(H2Database.class);
    
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
            logger.error("Не удалось загрузить драйвер H2", e);
        }
        
        try (Connection conn = getConnection()) {
            createTables(conn);
            logger.info("Тестовая база данных (H2) подключена");
        } catch (SQLException e) {
            logger.error("Ошибка тестовой базы данных", e);
            logger.error("Технические детали: {}", ExceptionHandler.getTechnicalDetails(e));
        }
    }
    
    @Override
    protected void createTables(Connection conn) throws SQLException {
        String autoIncrement = "AUTO_INCREMENT PRIMARY KEY";
        
        String createClients = "CREATE TABLE IF NOT EXISTS clients (" +
                "id INTEGER " + autoIncrement + ", " +
                "name TEXT NOT NULL CHECK(length(name) > 0), " +
                "last_name TEXT DEFAULT '', " +
                "phone TEXT NOT NULL CHECK(length(phone) > 0), " +
                "car_model TEXT NOT NULL CHECK(length(car_model) > 0), " +
                "car_number TEXT NOT NULL CHECK(length(car_number) > 0), " +
                "last_repair_date TEXT DEFAULT ''" +
                ")";

        String createServices = "CREATE TABLE IF NOT EXISTS services (" +
                "id INTEGER " + autoIncrement + ", " +
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

        String createSpareParts = "CREATE TABLE IF NOT EXISTS spare_parts (" +
                "id INTEGER " + autoIncrement + ", " +
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
                "unit_volume REAL DEFAULT 1.0, " +
                "unit_type TEXT DEFAULT 'шт' CHECK(unit_type IN ('шт', 'л', 'компл')), " +
                "is_liquid INTEGER DEFAULT 0 CHECK(is_liquid IN (0, 1))" +
                ")";

        String createOrders = "CREATE TABLE IF NOT EXISTS orders (" +
                "id TEXT PRIMARY KEY CHECK(length(id) > 0), " +
                "client_id INTEGER NOT NULL CHECK(client_id > 0), " +
                "status TEXT NOT NULL CHECK(length(status) > 0), " +
                "total REAL NOT NULL CHECK(total >= 0), " +
                "created_date TEXT NOT NULL CHECK(length(created_date) > 0), " +
                "closed_date TEXT DEFAULT '', " +
                "notes TEXT DEFAULT '', " +
                "FOREIGN KEY (client_id) REFERENCES clients(id)" +
                ")";

        String createOrderServices = "CREATE TABLE IF NOT EXISTS order_services (" +
                "order_id TEXT NOT NULL CHECK(length(order_id) > 0), " +
                "service_name TEXT NOT NULL CHECK(length(service_name) > 0), " +
                "price REAL NOT NULL CHECK(price >= 0), " +
                "service_id INTEGER DEFAULT 0, " +
                "FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE" +
                ")";

        String createOrderParts = "CREATE TABLE IF NOT EXISTS order_parts (" +
                "order_id TEXT NOT NULL CHECK(length(order_id) > 0), " +
                "part_name TEXT NOT NULL CHECK(length(part_name) > 0), " +
                "price REAL NOT NULL CHECK(price >= 0), " +
                "quantity INTEGER NOT NULL CHECK(quantity > 0), " +
                "spare_part_id INTEGER DEFAULT 0, " +
                "unit_type TEXT DEFAULT 'шт', " +
                "purchase_price REAL DEFAULT 0, " +
                "FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE" +
                ")";

        String createAppointments = "CREATE TABLE IF NOT EXISTS appointments (" +
                "id INTEGER " + autoIncrement + ", " +
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

        String createServiceSpareParts = "CREATE TABLE IF NOT EXISTS service_spare_parts (" +
                "id INTEGER " + autoIncrement + ", " +
                "service_id INTEGER NOT NULL CHECK(service_id > 0), " +
                "spare_part_id INTEGER NOT NULL CHECK(spare_part_id > 0), " +
                "quantity INTEGER DEFAULT 1 CHECK(quantity > 0), " +
                "unit_type TEXT DEFAULT 'шт' CHECK(unit_type IN ('шт', 'л', 'компл')), " +
                "active INTEGER DEFAULT 1 CHECK(active IN (0, 1)), " +
                "FOREIGN KEY (service_id) REFERENCES services(id), " +
                "FOREIGN KEY (spare_part_id) REFERENCES spare_parts(id)" +
                ")";

        String createToParts = "CREATE TABLE IF NOT EXISTS to_parts (" +
                "id INTEGER " + autoIncrement + ", " +
                "car_model TEXT NOT NULL CHECK(length(car_model) > 0), " +
                "spare_part_id INTEGER NOT NULL CHECK(spare_part_id > 0), " +
                "quantity INTEGER DEFAULT 1 CHECK(quantity > 0), " +
                "unit_type TEXT DEFAULT 'шт' CHECK(unit_type IN ('шт', 'л', 'компл')), " +
                "note TEXT DEFAULT '', " +
                "active INTEGER DEFAULT 1 CHECK(active IN (0, 1))" +
                ")";

        String createAppSettings = "CREATE TABLE IF NOT EXISTS app_settings (" +
                "id INTEGER " + autoIncrement + ", " +
                "setting_key TEXT NOT NULL UNIQUE CHECK(length(setting_key) > 0), " +
                "setting_value TEXT NOT NULL CHECK(length(setting_value) > 0), " +
                "description TEXT DEFAULT ''" +
                ")";

        String createServiceSparePartsLists = "CREATE TABLE IF NOT EXISTS service_spare_parts_lists (" +
                "id INTEGER " + autoIncrement + ", " +
                "service_id INTEGER NOT NULL CHECK(service_id > 0), " +
                "created_date TEXT NOT NULL CHECK(length(created_date) > 0), " +
                "active INTEGER DEFAULT 1 CHECK(active IN (0, 1)), " +
                "FOREIGN KEY (service_id) REFERENCES services(id)" +
                ")";

        String createServiceSparePartsListItems = "CREATE TABLE IF NOT EXISTS service_spare_parts_list_items (" +
                "id INTEGER " + autoIncrement + ", " +
                "list_id INTEGER NOT NULL CHECK(list_id > 0), " +
                "spare_part_id INTEGER NOT NULL CHECK(spare_part_id > 0), " +
                "quantity INTEGER DEFAULT 1 CHECK(quantity > 0), " +
                "unit_type TEXT DEFAULT 'шт' CHECK(unit_type IN ('шт', 'л', 'компл')), " +
                "FOREIGN KEY (list_id) REFERENCES service_spare_parts_lists(id), " +
                "FOREIGN KEY (spare_part_id) REFERENCES spare_parts(id)" +
                ")";

        // ====== НОВАЯ ТАБЛИЦА service_parts (гибкие связи услуги и запчасти) ======
        String createServiceParts = "CREATE TABLE IF NOT EXISTS service_parts (" +
                "id INTEGER " + autoIncrement + ", " +
                "service_id INTEGER NOT NULL CHECK(service_id > 0), " +
                "spare_part_id INTEGER NOT NULL CHECK(spare_part_id > 0), " +
                "quantity REAL DEFAULT 1 CHECK(quantity > 0), " +
                "is_required INTEGER DEFAULT 1 CHECK(is_required IN (0, 1)), " +
                "created_date TEXT NOT NULL CHECK(length(created_date) > 0), " +
                "FOREIGN KEY (service_id) REFERENCES services(id), " +
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
            stmt.execute(createServiceParts);
            createIndexes(conn);
            logger.info("Таблицы и индексы созданы/проверены");
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
                "CREATE INDEX IF NOT EXISTS idx_order_parts_spare_part_id ON order_parts(spare_part_id)",
                "CREATE INDEX IF NOT EXISTS idx_order_services_service_id ON order_services(service_id)",
                "CREATE INDEX IF NOT EXISTS idx_appointments_service_id ON appointments(service_id)",
                "CREATE INDEX IF NOT EXISTS idx_spare_parts_name ON spare_parts(name)",
                "CREATE INDEX IF NOT EXISTS idx_clients_name_lastname_phone ON clients(name, last_name, phone)",
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
        };
        
        try (Statement stmt = conn.createStatement()) {
            for (String index : indexes) {
                stmt.execute(index);
            }
        }
    }
    
    @Override
    protected String generateOrderId(Connection conn) {
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yy"));
        String sql = "SELECT MAX(id) as max_id FROM orders WHERE id LIKE 'ZAK-%'";
        String lastOrderId = null;

        try (java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                lastOrderId = rs.getString("max_id");
            }
        } catch (java.sql.SQLException e) {
            logger.error("Ошибка генерации ID", e);
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
                logger.error("Ошибка парсинга ID", e);
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
        String sql = "MERGE INTO services (name, price, duration, part_number, category_id, oil_volume, uses_oil, spare_part_name, spare_part_quantity) KEY(name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
              PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, service.getName());
            pstmt.setDouble(2, service.getPrice());
            pstmt.setInt(3, service.getDuration());
            pstmt.setString(4, service.getPartNumber());
            pstmt.setInt(5, service.getCategoryId());
            pstmt.setDouble(6, service.getOilVolume());
            pstmt.setInt(7, service.isUsesOil() ? 1 : 0);
            pstmt.setString(8, service.getSparePartName());
            pstmt.setInt(9, service.getSparePartQuantity());
            pstmt.executeUpdate();
            
            // Получаем сгенерированный id
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                service.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            logger.error("Ошибка добавления услуги", e);
        }
    }
    
    // ==================== SPARE PARTS ====================
    
    @Override
    public void addSparePart(SparePart part) {
        String sql = "MERGE INTO spare_parts (name, purchase_price, retail_price, stock, part_number, manufacturer, compatible_models, note, min_stock, location, unit_type) KEY(name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, part.getName());
            pstmt.setDouble(2, part.getPurchasePrice());
            pstmt.setDouble(3, part.getRetailPrice());
            pstmt.setDouble(4, part.getStock());
            pstmt.setString(5, part.getPartNumber());
            pstmt.setString(6, part.getManufacturer());
            pstmt.setString(7, part.getCompatibleModels());
            pstmt.setString(8, part.getNote());
            pstmt.setDouble(9, part.getMinStock());
            pstmt.setString(10, part.getLocation());
            pstmt.setString(11, part.getUnitType());
            pstmt.executeUpdate();
            
            // Получаем сгенерированный id
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                part.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            logger.error("Ошибка добавления запчасти", e);
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
            logger.error("Ошибка обновления остатка запчасти", e);
        }
    }

    // ==================== ORDERS ====================

    @Override
    public void addOrder(WorkOrder order) {
        try (Connection conn = getConnection()) {
            int clientId = getClientId(order.getClient());
            if (clientId == -1) {
                logger.error("Client not found, order not saved");
                return;
            }

            String orderId = generateOrderId(conn);
            order.setId(orderId);

            // Используем дату из order, если она установлена и не пустая,
            // иначе текущую дату (как в SQLiteDatabase) — иначе нарушается CHECK(length(created_date) > 0)
            String currentDate;
            if (order.getCreatedDate() != null && !order.getCreatedDate().isEmpty()) {
                String rawDate = order.getCreatedDate().substring(0, Math.min(10, order.getCreatedDate().length()));
                try {
                    LocalDate date = LocalDate.parse(rawDate);
                    currentDate = date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } catch (Exception e) {
                    currentDate = rawDate;
                }
            } else {
                currentDate = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }

            conn.setAutoCommit(false);

            String sql = "INSERT INTO orders (id, client_id, status, total, created_date, closed_date, notes) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, orderId);
                pstmt.setInt(2, clientId);
                pstmt.setString(3, order.getStatus());
                pstmt.setDouble(4, order.getTotal());
                pstmt.setString(5, currentDate);
                pstmt.setString(6, order.getClosedDate() != null ? order.getClosedDate() : "");
                pstmt.setString(7, order.getNotes() != null ? order.getNotes() : "");
                pstmt.executeUpdate();
            }

            saveOrderServices(conn, orderId, order);
            saveOrderParts(conn, orderId, order);

            conn.commit();
            logger.info("Заказ сохранен: {}", orderId);

        } catch (SQLException e) {
            logger.error("Ошибка добавления заказа", e);
        }
    }
    
    @Override
    public void updateOrder(WorkOrder order) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            // Update order header
            String orderSql = "UPDATE orders SET client_id = ?, status = ?, total = ?, closed_date = ?, notes = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(orderSql)) {
                int clientId = getClientId(order.getClient());
                pstmt.setInt(1, clientId);
                pstmt.setString(2, order.getStatus());
                pstmt.setDouble(3, order.getTotal());
                pstmt.setString(4, order.getClosedDate() != null ? order.getClosedDate() : "");
                pstmt.setString(5, order.getNotes() != null ? order.getNotes() : "");
                pstmt.setString(6, order.getId());
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
            logger.info("Заказ обновлен: {}", order.getId());
            
        } catch (SQLException e) {
            logger.error("Ошибка обновления заказа", e);
        }
    }
    
    private void saveOrderServices(Connection conn, String orderId, WorkOrder order) throws SQLException {
        // Защитный DELETE: гарантируем, что старых услуг не осталось (защита от дубликатов).
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM order_services WHERE order_id = ?")) {
            del.setString(1, orderId);
            del.executeUpdate();
        }

        String sql = "INSERT INTO order_services (order_id, service_name, price, service_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < order.getServices().size(); i++) {
                pstmt.setString(1, orderId);
                pstmt.setString(2, order.getServices().get(i));
                pstmt.setDouble(3, order.getServicePrices().get(i));
                pstmt.setInt(4, i < order.getServiceIds().size() ? order.getServiceIds().get(i) : 0);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
    
    private void saveOrderParts(Connection conn, String orderId, WorkOrder order) throws SQLException {
        String sql = "INSERT INTO order_parts (order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < order.getSpareParts().size(); i++) {
                SparePart part = order.getSpareParts().get(i);
                pstmt.setString(1, orderId);
                pstmt.setString(2, part.getName());
                pstmt.setDouble(3, part.getRetailPrice());
                pstmt.setDouble(4, order.getSparePartQuantities().get(i));
                pstmt.setInt(5, part.getId());
                pstmt.setString(6, part.getUnitType() != null ? part.getUnitType() : "шт");
                pstmt.setDouble(7, part.getPurchasePrice());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
    
    // ==================== APPOINTMENTS ====================
    
    @Override
    public void addAppointment(Appointment appointment) {
        String sql = "INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int clientId = getClientId(appointment.getClient());
            pstmt.setInt(1, clientId);
            pstmt.setString(2, appointment.getOrderId());
            pstmt.setString(3, appointment.getMasterName());
            pstmt.setString(4, appointment.getServiceName());
            pstmt.setInt(5, appointment.getServiceId());
            pstmt.setString(6, appointment.getDate());
            pstmt.setString(7, appointment.getTime());
            pstmt.setString(8, appointment.getStatus());
            pstmt.executeUpdate();
            
            // Получаем сгенерированный id
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                appointment.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            logger.error("Ошибка добавления записи", e);
        }
    }
    
    @Override
    public void updateAppointment(Appointment appointment) {
        String sql = "UPDATE appointments SET client_id = ?, master_name = ?, service_name = ?, service_id = ?, appointment_date = ?, appointment_time = ?, status = ?, order_id = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int clientId = getClientId(appointment.getClient());
            pstmt.setInt(1, clientId);
            pstmt.setString(2, appointment.getMasterName());
            pstmt.setString(3, appointment.getServiceName());
            pstmt.setInt(4, appointment.getServiceId());
            pstmt.setString(5, appointment.getDate());
            pstmt.setString(6, appointment.getTime());
            pstmt.setString(7, appointment.getStatus());
            pstmt.setString(8, appointment.getOrderId());
            pstmt.setInt(9, appointment.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка обновления записи", e);
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
            logger.error("Delete appointment error: {}", e.getMessage());
        }
    }
}
