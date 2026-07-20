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
 * Реализация Database для SQLite (используется в production).
 */
public class SQLiteDatabase extends AbstractDatabase {
    
    private static final Logger logger = LoggerFactory.getLogger(SQLiteDatabase.class);
    
    @Override
    public void init() {
        // Закрыть старый dataSource если он существует
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        
        // Используем SQLite для production
        com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:autoservice.db");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        dataSource = new com.zaxxer.hikari.HikariDataSource(config);

        try (Connection conn = getConnection()) {
            // Сначала создаем таблицы (если они не существуют)
            createTables(conn);
            // Затем мигрируем существующие таблицы
            migrateTables(conn);
            logger.info("База данных подключена с пулом соединений");
        } catch (SQLException e) {
            logger.error("Ошибка подключения к базе данных", e);
            logger.error("Технические детали: {}", ExceptionHandler.getTechnicalDetails(e));
        }
    }
    
    @Override
    protected void createTables(Connection conn) throws SQLException {
        String autoIncrement = "PRIMARY KEY AUTOINCREMENT";
        
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
                "unit_type TEXT DEFAULT 'шт' CHECK(unit_type IN ('шт', 'л', 'компл'))" +
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
                "key TEXT NOT NULL UNIQUE CHECK(length(key) > 0), " +
                "value TEXT NOT NULL CHECK(length(value) > 0), " +
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
                "FOREIGN KEY (list_id) REFERENCES service_spare_parts_lists(id) ON DELETE CASCADE, " +
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
            logger.info("Tables and indexes created/verified");
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
        LocalDate today = LocalDate.now();
        
        // Ищем максимальный порядковый номер среди ВСЕХ заказов (сквозная нумерация)
        // Формат ID: ZAK-ДД/ММ/ГГ-0001
        String sql = "SELECT MAX(id) as max_id FROM orders WHERE id LIKE 'ZAK-%-%'";
        String lastOrderId = null;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    lastOrderId = rs.getString("max_id");
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка генерации ID", e);
        }

        int newNumber = 1;
        if (lastOrderId != null) {
            try {
                // Извлекаем номер после последнего дефиса
                String[] parts = lastOrderId.split("-");
                if (parts.length >= 2) {
                    newNumber = Integer.parseInt(parts[parts.length - 1]) + 1;
                } else {
                    logger.warn("Неожиданный формат ID: {}", lastOrderId);
                }
            } catch (NumberFormatException e) {
                logger.error("Ошибка парсинга ID: {}", lastOrderId, e);
            }
        } else {
            logger.debug("Нет существующих заказов");
        }

        // Обработка переполнения (максимум 9999 заказов)
        if (newNumber > 9999) {
            logger.warn("Переполнение порядкового номера, сброс в 1");
            newNumber = 1;
        }
        
        // Генерируем формат даты для ID: dd/MM/yy (с косыми чертами, как в существующих данных)
        String date = today.format(DateTimeFormatter.ofPattern("dd/MM/yy"));
        String orderId = String.format("ZAK-%s-%04d", date, newNumber);
        
        logger.debug("Сгенерирован ID заказа: {} (последний: {}, номер: {})", orderId, lastOrderId, newNumber);
        
        return orderId;
    }
    
    // ==================== CLIENTS ====================
    
    // Используются методы из AbstractDatabase
    
    // ==================== SERVICES ====================
    
    @Override
    public void addService(Service service) {
        String sql = "INSERT OR REPLACE INTO services (name, price, duration, part_number, oil_volume, uses_oil, spare_part_name, spare_part_quantity) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

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
            logger.error("Ошибка добавления услуги", e);
        }
    }
    
    // ==================== SPARE PARTS ====================
    
    @Override
    public void addSparePart(SparePart part) {
        // Сначала проверяем, существует ли запчасть с таким именем
        String sqlCheck = "SELECT id FROM spare_parts WHERE name = ?";
        String sqlInsert = "INSERT INTO spare_parts (name, purchase_price, retail_price, stock, part_number, manufacturer, compatible_models, note, min_stock, location, unit_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlUpdate = "UPDATE spare_parts SET purchase_price = ?, retail_price = ?, stock = ?, part_number = ?, manufacturer = ?, compatible_models = ?, note = ?, min_stock = ?, location = ?, unit_type = ? WHERE name = ?";

        try (Connection conn = getConnection()) {
            // Проверяем существование
            Integer existingId = null;
            try (PreparedStatement pstmt = conn.prepareStatement(sqlCheck)) {
                pstmt.setString(1, part.getName());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        existingId = rs.getInt("id");
                    }
                }
            }

            if (existingId != null) {
                // Обновляем существующую запись
                try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
                    pstmt.setDouble(1, part.getPurchasePrice());
                    pstmt.setDouble(2, part.getRetailPrice());
                    pstmt.setDouble(3, part.getStock());
                    pstmt.setString(4, part.getPartNumber());
                    pstmt.setString(5, part.getManufacturer());
                    pstmt.setString(6, part.getCompatibleModels());
                    pstmt.setString(7, part.getNote());
                    pstmt.setDouble(8, part.getMinStock());
                    pstmt.setString(9, part.getLocation());
                    pstmt.setString(10, part.getUnitType());
                    pstmt.setString(11, part.getName());
                    pstmt.executeUpdate();
                    // Обновляем id запчасти
                    part.setId(existingId);
                }
            } else {
                // Вставляем новую запись
                try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
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
                    
                    // Получаем ID через last_insert_rowid() (SQLite-specific)
                    String selectIdSql = "SELECT last_insert_rowid()";
                    try (PreparedStatement selectStmt = conn.prepareStatement(selectIdSql);
                         ResultSet rs = selectStmt.executeQuery()) {
                        if (rs.next()) {
                            part.setId(rs.getInt(1));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка добавления запчасти", e);
        }
    }

    @Override
    public void updateSparePartStock(SparePart part, double newStock) {
        String sql = "UPDATE spare_parts SET stock = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newStock);
            pstmt.setInt(2, part.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка обновления остатка запчасти", e);
        }
    }

    @Override
    public void updateSparePart(SparePart part) {
        String sql = "UPDATE spare_parts SET name = ?, purchase_price = ?, retail_price = ?, stock = ?, " +
                "part_number = ?, manufacturer = ?, compatible_models = ?, note = ?, min_stock = ?, location = ?, " +
                "unit_type = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
            pstmt.setInt(12, part.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка обновления запчасти", e);
        }
    }

    // ==================== ORDERS ====================

    @Override
    public void addOrder(WorkOrder order) {
        logger.info("=== SQLiteDatabase.addOrder для заказа {} ===", order.getId());
        logger.info("Услуг в заказе: {}", order.getServices().size());
        for (int i = 0; i < order.getServices().size(); i++) {
            logger.info("  Услуга {}: {} (price={}, serviceId={})", i, order.getServices().get(i), order.getServicePrices().get(i), i < order.getServiceIds().size() ? order.getServiceIds().get(i) : 0);
        }
        
        try (Connection conn = getConnection()) {
            int clientId = getClientId(order.getClient());
            if (clientId == -1) {
                logger.error("Клиент не найден, заказ не сохранен");
                return;
            }

            // Генерируем ID и проверяем, что он уникален
            String orderId;
            int attempts = 0;
            final int MAX_ATTEMPTS = 5;
            
            do {
                orderId = generateOrderId(conn);
                
                // Явная проверка дубликатов (хотя PRIMARY KEY уже защищает)
                String checkSql = "SELECT COUNT(*) FROM orders WHERE id = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setString(1, orderId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            logger.warn("Сгенерированный ID {} уже существует, пробуем снова", orderId);
                            orderId = null; // Повторная генерация
                        }
                    }
                } catch (SQLException e) {
                    logger.error("Ошибка проверки дубликата", e);
                    return;
                }
                
                attempts++;
            } while (orderId == null && attempts < MAX_ATTEMPTS);
            
            if (orderId == null) {
                logger.error("Не удалось сгенерировать уникальный ID после {} попыток", MAX_ATTEMPTS);
                return;
            }
            
            order.setId(orderId);

            // Получаем текущую дату в формате dd/MM/yyyy
            String currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

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
        logger.info("=== SQLiteDatabase.updateOrder для заказа {} ===", order.getId());
        logger.info("Услуг в заказе: {}", order.getServices().size());
        for (int i = 0; i < order.getServices().size(); i++) {
            logger.info("  Услуга {}: {} (price={}, serviceId={})", i, order.getServices().get(i), order.getServicePrices().get(i), i < order.getServiceIds().size() ? order.getServiceIds().get(i) : 0);
        }
        
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
        logger.debug("=== saveOrderServices для заказа {} ===", orderId);
        logger.debug("Количество услуг: {}", order.getServices().size());
        for (int i = 0; i < order.getServices().size(); i++) {
            logger.debug("  Услуга {}: {} (price={}, serviceId={})", i, order.getServices().get(i), order.getServicePrices().get(i), i < order.getServiceIds().size() ? order.getServiceIds().get(i) : 0);
        }

        // Защитный DELETE: гарантируем, что старых услуг не осталось, даже если
        // saveOrderServices вызван вне контекста updateOrder (защита от дубликатов).
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
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
            logger.error("Ошибка удаления записи", e);
        }
    }
    
    // ==================== MIGRATION ====================
    
    private void migrateTables(Connection conn) throws SQLException {
        // Добавляем CHECK constraints для существующих таблиц
        
        // Для SQLite нужно пересоздавать таблицу с новыми constraints
        // Это делается через ALTER TABLE ADD COLUMN для простых случаев
        // Но для CHECK constraints требуется пересоздание таблицы
        
        // Проверяем и обновляем таблицу spare_parts
        if (!columnExists(conn, "spare_parts", "unit_type")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE spare_parts ADD COLUMN unit_type TEXT DEFAULT 'шт' CHECK(unit_type IN ('шт', 'л', 'компл'))");
                logger.info("Добавлена колонка unit_type в spare_parts с CHECK constraint");
            }
        }
        
        // Добавляем колонку note в таблицу spare_parts, если её нет
        if (!columnExists(conn, "spare_parts", "note")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE spare_parts ADD COLUMN note TEXT DEFAULT ''");
                logger.info("Добавлена колонка note в spare_parts");
            }
        }
        
        // Проверяем и обновляем таблицу services
        if (!columnExists(conn, "services", "uses_oil")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE services ADD COLUMN uses_oil INTEGER DEFAULT 0 CHECK(uses_oil IN (0, 1))");
                logger.info("Добавлена колонка uses_oil в services с CHECK constraint");
            }
        }
        
        // Добавляем CHECK constraints для price и retail_price если их нет
        try (Statement stmt = conn.createStatement()) {
            // Проверяем, есть ли уже CHECK constraint для price в spare_parts
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(spare_parts)");
            boolean hasRetailPriceConstraint = false;
            while (rs.next()) {
                String name = rs.getString("name");
                String dflt = rs.getString("dflt_value");
                String pk = rs.getString("pk");
                // Простая проверка - retail_price не NULL и не DEFAULT
                if ("retail_price".equals(name)) {
                    hasRetailPriceConstraint = true;
                }
            }
            rs.close();
            
            // Если retail_price не имеет CHECK, добавляем через ALTER TABLE
            // SQLite не поддерживает ADD CONSTRAINT для существующих таблиц,
            // поэтому добавляем только к новым колонкам
        }
        
        // Добавляем колонку spare_part_name в таблицу services, если её нет
        if (!columnExists(conn, "services", "spare_part_name")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE services ADD COLUMN spare_part_name TEXT DEFAULT ''");
                logger.info("Добавлена колонка spare_part_name в services");
            }
        }
        
        // Добавляем колонку spare_part_quantity в таблицу services, если её нет
        if (!columnExists(conn, "services", "spare_part_quantity")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE services ADD COLUMN spare_part_quantity INTEGER DEFAULT 0");
                logger.info("Добавлена колонка spare_part_quantity в services");
            }
        }
        
        // Добавляем колонку note в таблицу to_parts, если её нет
        if (!columnExists(conn, "to_parts", "note")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE to_parts ADD COLUMN note TEXT DEFAULT ''");
                logger.info("Добавлена колонка note в to_parts");
            }
        }
        
        // Создаем таблицу service_spare_parts_lists, если её нет
        if (!tableExists(conn, "service_spare_parts_lists")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE service_spare_parts_lists (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "service_id INTEGER NOT NULL CHECK(service_id > 0), " +
                        "created_date TEXT NOT NULL CHECK(length(created_date) > 0), " +
                        "active INTEGER DEFAULT 1 CHECK(active IN (0, 1)), " +
                        "FOREIGN KEY (service_id) REFERENCES services(id)" +
                        ")");
                logger.info("Создана таблица service_spare_parts_lists с CHECK constraints");
            }
        }
        
        // Создаем таблицу service_spare_parts_list_items, если её нет
        if (!tableExists(conn, "service_spare_parts_list_items")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE service_spare_parts_list_items (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "list_id INTEGER NOT NULL CHECK(list_id > 0), " +
                        "spare_part_id INTEGER NOT NULL CHECK(spare_part_id > 0), " +
                        "quantity INTEGER DEFAULT 1 CHECK(quantity > 0), " +
                        "unit_type TEXT DEFAULT 'шт' CHECK(unit_type IN ('шт', 'л', 'компл')), " +
                        "FOREIGN KEY (list_id) REFERENCES service_spare_parts_lists(id) ON DELETE CASCADE, " +
                        "FOREIGN KEY (spare_part_id) REFERENCES spare_parts(id)" +
                        ")");
                logger.info("Создана таблица service_spare_parts_list_items с CHECK constraints");
            }
        }
        
        // ====== МИГРАЦИЯ ДЛЯ ID УСЛУГ И ЗАПЧАСТЕЙ В ЗАКАЗАХ ======
        
        // Добавляем колонку service_id в order_services, если её нет
        if (!columnExists(conn, "order_services", "service_id")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE order_services ADD COLUMN service_id INTEGER DEFAULT 0");
                logger.info("Добавлена колонка service_id в order_services");
            }
        }
        
        // Добавляем колонку spare_part_id в order_parts, если её нет
        if (!columnExists(conn, "order_parts", "spare_part_id")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE order_parts ADD COLUMN spare_part_id INTEGER DEFAULT 0");
                logger.info("Добавлена колонка spare_part_id в order_parts");
            }
        }
        
        // Добавляем колонку unit_type в order_parts, если её нет
        if (!columnExists(conn, "order_parts", "unit_type")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE order_parts ADD COLUMN unit_type TEXT DEFAULT 'шт'");
                logger.info("Добавлена колонка unit_type в order_parts");
            }
        }
        
        // Добавляем колонку purchase_price в order_parts, если её нет
        if (!columnExists(conn, "order_parts", "purchase_price")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE order_parts ADD COLUMN purchase_price REAL DEFAULT 0");
                logger.info("Добавлена колонка purchase_price в order_parts");
            }
        }
        
        // Добавляем колонку service_id в appointments, если её нет
        if (!columnExists(conn, "appointments", "service_id")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE appointments ADD COLUMN service_id INTEGER DEFAULT 0");
                logger.info("Добавлена колонка service_id в appointments");
            }
        }
        
        // Добавляем колонку closed_date в orders, если её нет
        if (!columnExists(conn, "orders", "closed_date")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE orders ADD COLUMN closed_date TEXT DEFAULT ''");
                logger.info("Добавлена колонка closed_date в orders");
            }
        }
        
        // Добавляем колонку notes в orders, если её нет
        if (!columnExists(conn, "orders", "notes")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE orders ADD COLUMN notes TEXT DEFAULT ''");
                logger.info("Добавлена колонка notes в orders");
            }
        }
    }
    
    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    private boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        String sql = "PRAGMA table_info(" + tableName + ")";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                if (rs.getString("name").equals(columnName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
