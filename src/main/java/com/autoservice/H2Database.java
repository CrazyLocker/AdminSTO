package com.autoservice;

import java.sql.*;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zaxxer.hikari.HikariDataSource;
import com.autoservice.utils.ExceptionHandler;
import com.autoservice.SparePart;
import com.autoservice.WorkOrder;
import com.autoservice.Appointment;

/**
 * Реализация Database для H2 (используется в тестах).
 */
public class H2Database extends AbstractDatabase {
    
    private static final Logger logger = LoggerFactory.getLogger(H2Database.class);
    
    /**
     * Инициализировать H2 in-memory для тестов и создать таблицы.
     */
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
        // Тот же размер пула и таймауты, что и у SQLite: параллельные тесты
        // используют до 10 потоков, и меньшее значение приводило к таймаутам
        // получения соединения и «зависанию» тестов.
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
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
    
    // ==================== CLIENTS ====================
    
    // Используются методы из AbstractDatabase
    
    // ==================== SPARE PARTS ====================
    
    /**
     * Добавить или обновить запчасть ({@code MERGE ... KEY(name)}).
     * @param part запчасть для добавления; после вызова получает ID
     */
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
            
            // Получаем сгенерированный id (работает в H2 для INSERT и MERGE)
            int id = getGeneratedKeyId(conn, pstmt);
            if (id > 0) {
                part.setId(id);
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
        try (Connection conn = getConnection()) {
            // Генерация ID (SELECT MAX(id)) и вставка должны быть атомарны,
            // иначе параллельные потоки получают одинаковый номер заказа.
            synchronized (ORDER_ID_LOCK) {
                int clientId = findClientId(conn, order.getClient());
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

                String sql = "INSERT INTO orders (id, client_id, status, total, created_date, mileage, closed_date, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, orderId);
                    pstmt.setInt(2, clientId);
                    pstmt.setString(3, order.getStatus());
                    pstmt.setDouble(4, order.getTotal());
                    pstmt.setString(5, currentDate);
                    pstmt.setInt(6, order.getMileage());
                    pstmt.setString(7, order.getClosedDate() != null ? order.getClosedDate() : "");
                    pstmt.setString(8, order.getNotes() != null ? order.getNotes() : "");
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
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            // Update order header
            String orderSql = "UPDATE orders SET client_id = ?, status = ?, total = ?, mileage = ?, closed_date = ?, notes = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(orderSql)) {
                int clientId = findClientId(conn, order.getClient());
                pstmt.setInt(1, clientId);
                pstmt.setString(2, order.getStatus());
                pstmt.setDouble(3, order.getTotal());
                pstmt.setInt(4, order.getMileage());
                pstmt.setString(5, order.getClosedDate() != null ? order.getClosedDate() : "");
                pstmt.setString(6, order.getNotes() != null ? order.getNotes() : "");
                pstmt.setString(7, order.getId());
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
    
    
    // ==================== APPOINTMENTS ====================
    
    /**
     * Добавить запись на обслуживание.
     * @param appointment запись для добавления
     */
    @Override
    public void addAppointment(Appointment appointment) {
        String sql = "INSERT INTO appointments (client_id, order_id, master_name, service_name, service_id, appointment_date, appointment_time, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
            
            // Получаем сгенерированный id
            int id = getGeneratedKeyId(conn, pstmt);
            if (id > 0) {
                appointment.setId(id);
            }
        } catch (SQLException e) {
            logger.error("Ошибка добавления записи", e);
        }
    }
    
    
    /**
     * Удалить запись на обслуживание.
     * @param id идентификатор записи
     */
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
