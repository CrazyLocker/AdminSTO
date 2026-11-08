package com.autoservice;

import java.sql.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zaxxer.hikari.HikariDataSource;
import com.autoservice.utils.ExceptionHandler;
import com.autoservice.SparePart;
import com.autoservice.WorkOrder;
import com.autoservice.Appointment;

/**
 * Реализация Database для SQLite (используется в production).
 */
public class SQLiteDatabase extends AbstractDatabase {
    
    private static final Logger logger = LoggerFactory.getLogger(SQLiteDatabase.class);
    
    /**
     * Инициализировать подключение к SQLite (production) и применить миграции схемы.
     */
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
    
    // ==================== CLIENTS ====================
    
    // Используются методы из AbstractDatabase
    
    // ==================== SPARE PARTS ====================
    
    /**
     * Добавить запчасть или обновить существующую с тем же названием.
     * @param part запчасть для добавления; после вызова получает ID
     */
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

    /**
     * Обновить остаток запчасти.
     * @param part запчасть
     * @param newStock новый остаток
     */
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

    // ==================== ORDERS ====================

    /**
     * Добавить заказ и его услуги с запчастями в одной транзакции; ID генерируется с защитой от параллельных вставок.
     * @param order заказ для добавления
     */
    @Override
    public void addOrder(WorkOrder order) {
        logger.info("=== SQLiteDatabase.addOrder для заказа {} ===", order.getId());
        logger.info("Услуг в заказе: {}", order.getServices().size());
        for (int i = 0; i < order.getServices().size(); i++) {
            logger.info("  Услуга {}: {} (price={}, serviceId={})", i, order.getServices().get(i), order.getServicePrices().get(i), i < order.getServiceIds().size() ? order.getServiceIds().get(i) : 0);
        }
        
        try (Connection conn = getConnection()) {
            // Генерация ID (SELECT MAX(id)) и вставка должны быть атомарны,
            // иначе параллельные потоки получают одинаковый номер заказа.
            synchronized (ORDER_ID_LOCK) {
                int clientId = findClientId(conn, order.getClient());
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
            }

        } catch (SQLException e) {
            logger.error("Ошибка добавления заказа", e);
        }
    }
    
    /**
     * Обновить заказ и пересоздать его услуги и запчасти в одной транзакции.
     * @param order заказ с обновлёнными данными
     */
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
                int clientId = findClientId(conn, order.getClient());
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
    
    // ==================== APPOINTMENTS ====================
    
    /**
     * Добавить запись на обслуживание.
     * @param appointment запись для добавления
     */
    @Override
    public void addAppointment(Appointment appointment) {
        String sql = "INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int clientId = findClientId(conn, appointment.getClient());
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
    
    // ==================== MIGRATION ====================
    
    /**
     * Применить миграции схемы SQLite. Каждая миграция — отдельный метод в
     * {@link SQLiteMigrations}, порядок задаётся списком миграций.
     * @param conn соединение с базой данных
     * @throws SQLException если миграция не удалась
     */
    private void migrateTables(Connection conn) throws SQLException {
        SQLiteMigrations.migrate(conn);
    }
}
