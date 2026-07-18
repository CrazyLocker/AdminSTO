package com.autoservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.autoservice.utils.ExceptionHandler;
import com.autoservice.Client;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.WorkOrder;
import com.autoservice.Appointment;

/**
 * Абстрактный базовый класс для реализаций Database.
 * Содержит общую логику для SQLite и H2.
 */
public abstract class AbstractDatabase implements DatabaseInterface {
    
    protected HikariDataSource dataSource;
    
    private static final Logger logger = LoggerFactory.getLogger(AbstractDatabase.class);
    
    /**
     * Создание таблиц базы данных.
     * @param conn соединение с базой данных
     */
    protected abstract void createTables(Connection conn) throws SQLException;
    
    /**
     * Генерация уникального ID заказа.
     * @param conn соединение с базой данных
     * @return сгенерированный ID заказа
     */
    protected abstract String generateOrderId(Connection conn);
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Получить сгенерированный ID после INSERT.
     * Использует getGeneratedKeys() для обеих баз данных.
     * @param conn соединение с базой данных
     * @param stmt prepared statement с RETURN_GENERATED_KEYS
     * @return сгенерированный ID
     */
    protected int getGeneratedKeyId(Connection conn, PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        // Fallback для SQLite: last_insert_rowid()
        String url = conn.getMetaData().getURL();
        if (url != null && url.contains("sqlite")) {
            try (Statement queryStmt = conn.createStatement();
                 ResultSet rs = queryStmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }
    
    // ==================== DEFAULT IMPLEMENTATIONS ====================
    
    @Override
    public void init() {
        throw new UnsupportedOperationException("init() must be implemented by concrete database class");
    }
    
    @Override
    public void initForTest() {
        throw new UnsupportedOperationException("initForTest() must be implemented by concrete database class");
    }
    
    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    // ==================== CLIENTS ====================
    
    @Override
    public List<Client> getAllClients() {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT id, name, last_name, phone, car_model, car_number, last_repair_date FROM clients ORDER BY id";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                clients.add(new Client(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("last_name"),
                        rs.getString("phone"),
                        rs.getString("car_model"),
                        rs.getString("car_number"),
                        rs.getString("last_repair_date")
                ));
            }
        } catch (SQLException e) {
            logger.error("Ошибка загрузки клиентов", e);
        }
        return clients;
    }
    
    @Override
    public void addClient(Client client) {
        String normalizedPhone = Validators.cleanPhone(client.getPhone());
        String normalizedCarNumber = Validators.normalizeCarNumber(client.getCarNumber());
        String sql = "INSERT INTO clients (name, last_name, phone, car_model, car_number, last_repair_date) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, client.getName());
            pstmt.setString(2, client.getLastName());
            pstmt.setString(3, normalizedPhone);
            pstmt.setString(4, client.getCarModel());
            pstmt.setString(5, normalizedCarNumber);
            pstmt.setString(6, client.getLastRepairDate());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка добавления клиента", e);
        }
    }
    
    @Override
    public void updateClient(Client client) {
        String normalizedPhone = Validators.cleanPhone(client.getPhone());
        String normalizedCarNumber = Validators.normalizeCarNumber(client.getCarNumber());
        String sql = "UPDATE clients SET name = ?, last_name = ?, phone = ?, car_model = ?, car_number = ?, last_repair_date = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, client.getName());
            pstmt.setString(2, client.getLastName());
            pstmt.setString(3, normalizedPhone);
            pstmt.setString(4, client.getCarModel());
            pstmt.setString(5, normalizedCarNumber);
            pstmt.setString(6, client.getLastRepairDate());
            pstmt.setInt(7, client.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка обновления клиента", e);
        }
    }
    
    @Override
    public void deleteClient(Client client) {
        String sql = "DELETE FROM clients WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, client.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка удаления клиента", e);
        }
    }
    
    @Override
    public Client getClientById(int id) {
        String sql = "SELECT id, name, last_name, phone, car_model, car_number, last_repair_date FROM clients WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Client(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("last_name"),
                        rs.getString("phone"),
                        rs.getString("car_model"),
                        rs.getString("car_number"),
                        rs.getString("last_repair_date")
                );
            }
        } catch (SQLException e) {
            logger.error("Ошибка получения клиента по ID", e);
        }
        return null;
    }
    
    @Override
    public int getClientId(Client client) {
        String sql = "SELECT id FROM clients WHERE name = ? AND last_name = ? AND phone = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, client.getName());
            pstmt.setString(2, client.getLastName());
            pstmt.setString(3, client.getPhone());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            logger.error("Ошибка получения ID клиента", e);
        }
        return -1;
    }
    
    // ==================== SERVICES ====================
    
    @Override
    public void addService(Service service) {
        String sql = "INSERT INTO services (name, price, duration, part_number, oil_volume, uses_oil, spare_part_name, spare_part_quantity) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

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
    
    @Override
    public List<Service> getAllServices() {
        List<Service> services = new ArrayList<>();
        String sql = "SELECT id, name, price, duration, part_number, oil_volume, uses_oil, spare_part_name, spare_part_quantity FROM services ORDER BY name";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Service service = new Service();
                service.setId(rs.getInt("id"));
                service.setName(rs.getString("name"));
                service.setPrice(rs.getDouble("price"));
                service.setDuration(rs.getInt("duration"));
                service.setPartNumber(rs.getString("part_number"));
                service.setOilVolume(rs.getDouble("oil_volume"));
                service.setUsesOil(rs.getInt("uses_oil") == 1);
                service.setSparePartName(rs.getString("spare_part_name"));
                service.setSparePartQuantity(rs.getInt("spare_part_quantity"));
                services.add(service);
            }
        } catch (SQLException e) {
            logger.error("Ошибка загрузки услуг", e);
        }
        return services;
    }
    
    @Override
    public void deleteService(Service service) {
        String sql = "DELETE FROM services WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, service.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка удаления услуги", e);
        }
    }
    
    @Override
    public void updateService(Service service) {
        String sql = "UPDATE services SET name = ?, price = ?, duration = ?, part_number = ?, oil_volume = ?, uses_oil = ?, spare_part_name = ?, spare_part_quantity = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, service.getName());
            pstmt.setDouble(2, service.getPrice());
            pstmt.setInt(3, service.getDuration());
            pstmt.setString(4, service.getPartNumber());
            pstmt.setDouble(5, service.getOilVolume());
            pstmt.setInt(6, service.isUsesOil() ? 1 : 0);
            pstmt.setString(7, service.getSparePartName());
            pstmt.setInt(8, service.getSparePartQuantity());
            pstmt.setInt(9, service.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка обновления услуги", e);
        }
    }
    
    // ==================== SPARE PARTS ====================
    
    @Override
    public List<SparePart> getAllSpareParts() {
        List<SparePart> parts = new ArrayList<>();
        String sql = "SELECT id, name, part_number, manufacturer, compatible_models, note, purchase_price, retail_price, stock, min_stock, location, unit_type FROM spare_parts ORDER BY name";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                parts.add(new SparePart(
                        rs.getInt("id"),
                        0,
                        rs.getString("name"),
                        rs.getString("part_number"),
                        rs.getString("manufacturer"),
                        rs.getString("compatible_models"),
                        rs.getString("note"),
                        rs.getDouble("purchase_price"),
                        rs.getDouble("retail_price"),
                        rs.getDouble("stock"),
                        rs.getDouble("min_stock"),
                        rs.getString("unit_type"),
                        rs.getString("location")
                ));
            }
        } catch (SQLException e) {
            logger.error("Ошибка загрузки запчастей", e);
        }
        return parts;
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
    
    @Override
    public void deleteSparePart(SparePart part) {
        String sql = "DELETE FROM spare_parts WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, part.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка удаления запчасти", e);
        }
    }
    
    // ==================== ORDERS ====================
    
    @Override
    public List<WorkOrder> getAllOrders() {
        Map<String, WorkOrder> orderMap = new LinkedHashMap<>();

        String ordersSql = """
            SELECT o.id as order_id, o.status, o.total, o.created_date,
                   o.closed_date, o.notes,
                   c.id as client_id, c.name, c.last_name, c.phone, 
                   c.car_model, c.car_number, c.last_repair_date
            FROM orders o
            LEFT JOIN clients c ON o.client_id = c.id
            ORDER BY o.created_date DESC
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(ordersSql)) {

            while (rs.next()) {
                Client client = new Client(
                        rs.getInt("client_id"),
                        rs.getString("name") != null ? rs.getString("name") : "",
                        rs.getString("last_name") != null ? rs.getString("last_name") : "",
                        rs.getString("phone") != null ? rs.getString("phone") : "",
                        rs.getString("car_model") != null ? rs.getString("car_model") : "",
                        rs.getString("car_number") != null ? rs.getString("car_number") : "",
                        rs.getString("last_repair_date") != null ? rs.getString("last_repair_date") : ""
                );

                WorkOrder order = new WorkOrder(
                        rs.getString("order_id"),
                        client,
                        rs.getString("status"),
                        rs.getDouble("total"),
                        rs.getString("created_date")
                );
                order.setClosedDate(rs.getString("closed_date") != null ? rs.getString("closed_date") : "");
                order.setNotes(rs.getString("notes") != null ? rs.getString("notes") : "");
                orderMap.put(rs.getString("order_id"), order);
            }
        } catch (SQLException e) {
            logger.error("Ошибка загрузки заказов", e);
            logger.error("Технические детали: {}", ExceptionHandler.getTechnicalDetails(e));
            return new ArrayList<>();
        }

        String servicesSql = "SELECT order_id, service_name, price, service_id FROM order_services";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(servicesSql)) {
            while (rs.next()) {
                String orderId = rs.getString("order_id");
                WorkOrder order = orderMap.get(orderId);
                if (order != null) {
                    order.addService(rs.getInt("service_id"), rs.getString("service_name"), rs.getDouble("price"));
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка загрузки услуг заказа", e);
        }

        String partsSql = "SELECT order_id, part_name, price, quantity, spare_part_id, unit_type, purchase_price FROM order_parts";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(partsSql)) {
            while (rs.next()) {
                String orderId = rs.getString("order_id");
                WorkOrder order = orderMap.get(orderId);
                if (order != null) {
                    SparePart part = new SparePart(
                            rs.getString("part_name"),
                            rs.getDouble("purchase_price"),
                            rs.getDouble("price"),
                            rs.getInt("quantity")
                    );
                    part.setId(rs.getInt("spare_part_id"));
                    part.setUnitType(rs.getString("unit_type") != null ? rs.getString("unit_type") : "шт");
                    order.addSparePart(part, rs.getInt("quantity"));
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка загрузки запчастей заказа", e);
        }

        return new ArrayList<>(orderMap.values());
    }
    
    @Override
    public void deleteOrder(String orderId) {
        String sql = "DELETE FROM orders WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка удаления заказа", e);
        }
    }
    
    @Override
    public void updateOrder(WorkOrder order) {
        String sql = "UPDATE orders SET client_id = ?, status = ?, total = ?, closed_date = ?, notes = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int clientId = getClientId(order.getClient());
            pstmt.setInt(1, clientId);
            pstmt.setString(2, order.getStatus());
            pstmt.setDouble(3, order.getTotal());
            pstmt.setString(4, order.getClosedDate() != null ? order.getClosedDate() : "");
            pstmt.setString(5, order.getNotes() != null ? order.getNotes() : "");
            pstmt.setString(6, order.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка обновления заказа", e);
        }
    }
    
    // ==================== APPOINTMENTS ====================
    
    @Override
    public List<Appointment> getAllAppointments() {
        List<Appointment> appointments = new ArrayList<>();
        String sql = """
            SELECT a.id, a.client_id, a.order_id, a.master_name, 
                   a.service_name, a.service_id, a.appointment_date, a.appointment_time, a.status,
                   c.name, c.last_name, c.phone, c.car_model, c.car_number, c.last_repair_date
            FROM appointments a
            LEFT JOIN clients c ON a.client_id = c.id
            ORDER BY a.appointment_date, a.appointment_time
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Client client = new Client(
                        rs.getInt("client_id"),
                        rs.getString("name") != null ? rs.getString("name") : "",
                        rs.getString("last_name") != null ? rs.getString("last_name") : "",
                        rs.getString("phone") != null ? rs.getString("phone") : "",
                        rs.getString("car_model") != null ? rs.getString("car_model") : "",
                        rs.getString("car_number") != null ? rs.getString("car_number") : "",
                        rs.getString("last_repair_date") != null ? rs.getString("last_repair_date") : ""
                );

                Appointment appt = new Appointment(
                        rs.getInt("id"),
                        client,
                        rs.getString("order_id"),
                        rs.getString("master_name"),
                        rs.getString("service_name"),
                        rs.getString("appointment_date"),
                        rs.getString("appointment_time"),
                        rs.getString("status")
                );
                appt.setServiceId(rs.getInt("service_id"));
                appointments.add(appt);
            }
        } catch (SQLException e) {
            logger.error("Ошибка загрузки записей", e);
        }
        return appointments;
    }
    
    @Override
    public List<Appointment> getAppointmentsByDate(String date) {
        List<Appointment> appointments = new ArrayList<>();
        String sql = """
            SELECT a.id, a.client_id, a.order_id, a.master_name, 
                   a.service_name, a.service_id, a.appointment_date, a.appointment_time, a.status,
                   c.name, c.last_name, c.phone, c.car_model, c.car_number, c.last_repair_date
            FROM appointments a
            LEFT JOIN clients c ON a.client_id = c.id
            WHERE a.appointment_date = ?
            ORDER BY a.appointment_time
        """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Client client = new Client(
                        rs.getInt("client_id"),
                        rs.getString("name") != null ? rs.getString("name") : "",
                        rs.getString("last_name") != null ? rs.getString("last_name") : "",
                        rs.getString("phone") != null ? rs.getString("phone") : "",
                        rs.getString("car_model") != null ? rs.getString("car_model") : "",
                        rs.getString("car_number") != null ? rs.getString("car_number") : "",
                        rs.getString("last_repair_date") != null ? rs.getString("last_repair_date") : ""
                );

                Appointment appt = new Appointment(
                        rs.getInt("id"),
                        client,
                        rs.getString("order_id"),
                        rs.getString("master_name"),
                        rs.getString("service_name"),
                        rs.getString("appointment_date"),
                        rs.getString("appointment_time"),
                        rs.getString("status")
                );
                appt.setServiceId(rs.getInt("service_id"));
                appointments.add(appt);
            }
        } catch (SQLException e) {
            logger.error("Ошибка загрузки записей по дате", e);
        }
        return appointments;
    }
    
    // ==================== APPOINTMENT MODIFICATION METHODS ====================
    
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

    @Override
    public Appointment getAppointmentById(int id) {
        String sql = """
            SELECT a.id, a.client_id, a.order_id, a.master_name, 
                   a.service_name, a.service_id, a.appointment_date, a.appointment_time, a.status,
                   c.name, c.last_name, c.phone, c.car_model, c.car_number, c.last_repair_date
            FROM appointments a
            LEFT JOIN clients c ON a.client_id = c.id
            WHERE a.id = ?
        """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Client client = new Client(
                            rs.getInt("client_id"),
                            rs.getString("name") != null ? rs.getString("name") : "",
                            rs.getString("last_name") != null ? rs.getString("last_name") : "",
                            rs.getString("phone") != null ? rs.getString("phone") : "",
                            rs.getString("car_model") != null ? rs.getString("car_model") : "",
                            rs.getString("car_number") != null ? rs.getString("car_number") : "",
                            rs.getString("last_repair_date") != null ? rs.getString("last_repair_date") : ""
                    );

                    Appointment appt = new Appointment(
                            rs.getInt("id"),
                            client,
                            rs.getString("order_id"),
                            rs.getString("master_name"),
                            rs.getString("service_name"),
                            rs.getString("appointment_date"),
                            rs.getString("appointment_time"),
                            rs.getString("status")
                    );
                    appt.setServiceId(rs.getInt("service_id"));
                    return appt;
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка загрузки записи по ID", e);
        }
        return null;
    }

    // ==================== SERVICE-SPARE PART RELATIONSHIPS ====================

    @Override
    public List<com.autoservice.model.ServiceSparePart> getServiceSparePartsByServiceId(int serviceId) {
        List<com.autoservice.model.ServiceSparePart> relations = new ArrayList<>();
        String sql;
        
        if (serviceId == -1) {
            // Получить все связи
            sql = "SELECT id, service_id, spare_part_id, quantity, unit_type, active FROM service_spare_parts";
        } else {
            // Получить связи для конкретной услуги
            sql = "SELECT id, service_id, spare_part_id, quantity, unit_type, active FROM service_spare_parts WHERE service_id = ?";
        }

        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs;
            
            if (serviceId == -1) {
                rs = stmt.executeQuery(sql);
            } else {
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, serviceId);
                rs = pstmt.executeQuery();
            }
            
            while (rs.next()) {
                com.autoservice.model.ServiceSparePart relation = new com.autoservice.model.ServiceSparePart();
                relation.setId(rs.getInt("id"));
                relation.setServiceId(rs.getInt("service_id"));
                relation.setSparePartId(rs.getInt("spare_part_id"));
                relation.setQuantity(rs.getInt("quantity"));
                relation.setUnitType(rs.getString("unit_type"));
                relation.setActive(rs.getInt("active") == 1);
                relations.add(relation);
            }
            rs.close();
        } catch (SQLException e) {
            logger.error("Ошибка загрузки связей услуг-запчастей", e);
        }
        return relations;
    }

    @Override
    public void addServiceSparePart(com.autoservice.model.ServiceSparePart relation) {
        String sql = "INSERT INTO service_spare_parts (service_id, spare_part_id, quantity, unit_type, active) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, relation.getServiceId());
            pstmt.setInt(2, relation.getSparePartId());
            pstmt.setInt(3, relation.getQuantity());
            pstmt.setString(4, relation.getUnitType());
            pstmt.setInt(5, relation.isActive() ? 1 : 0);
            pstmt.executeUpdate();
            
            // Получаем сгенерированный id через last_insert_rowid() для SQLite
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    relation.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка добавления связи услуги-запчасти", e);
        }
    }

    @Override
    public void deleteServiceSparePart(com.autoservice.model.ServiceSparePart relation) {
        String sql = "DELETE FROM service_spare_parts WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, relation.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка удаления связи услуги-запчасти", e);
        }
    }

    @Override
    public void deleteServiceSparePartsByServiceId(int serviceId) {
        String sql;
        
        if (serviceId == -1) {
            // Удалить все связи
            sql = "DELETE FROM service_spare_parts";
        } else {
            sql = "DELETE FROM service_spare_parts WHERE service_id = ?";
        }

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (serviceId != -1) {
                pstmt.setInt(1, serviceId);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка удаления связей услуги-запчастей по ID услуги", e);
        }
    }

    // ==================== SERVICE-SPARE PARTS LISTS (NEW STRUCTURE) ====================

    @Override
    public List<com.autoservice.model.ServiceSparePartsList> getServiceSparePartsListsByServiceId(int serviceId) {
        List<com.autoservice.model.ServiceSparePartsList> lists = new ArrayList<>();
        String sql;
        
        if (serviceId == -1) {
            // Получить все списки
            sql = "SELECT id, service_id, created_date, active FROM service_spare_parts_lists ORDER BY created_date DESC";
        } else {
            // Получить списки для конкретной услуги
            sql = "SELECT id, service_id, created_date, active FROM service_spare_parts_lists WHERE service_id = ? ORDER BY created_date DESC";
        }

        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs;
            
            if (serviceId == -1) {
                rs = stmt.executeQuery(sql);
            } else {
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, serviceId);
                rs = pstmt.executeQuery();
            }
            
            while (rs.next()) {
                com.autoservice.model.ServiceSparePartsList list = new com.autoservice.model.ServiceSparePartsList();
                list.setId(rs.getInt("id"));
                list.setServiceId(rs.getInt("service_id"));
                list.setCreatedDate(rs.getString("created_date"));
                list.setActive(rs.getInt("active") == 1);
                lists.add(list);
            }
            rs.close();
        } catch (SQLException e) {
            logger.error("Ошибка загрузки списков услуг-запчастей", e);
        }
        return lists;
    }

    @Override
    public List<com.autoservice.model.ServiceSparePartsListItem> getServiceSparePartsListItems(int listId) {
        List<com.autoservice.model.ServiceSparePartsListItem> items = new ArrayList<>();
        String sql = "SELECT id, list_id, spare_part_id, quantity, unit_type FROM service_spare_parts_list_items WHERE list_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, listId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                com.autoservice.model.ServiceSparePartsListItem item = new com.autoservice.model.ServiceSparePartsListItem();
                item.setId(rs.getInt("id"));
                item.setListId(rs.getInt("list_id"));
                item.setSparePartId(rs.getInt("spare_part_id"));
                item.setQuantity(rs.getInt("quantity"));
                item.setUnitType(rs.getString("unit_type"));
                items.add(item);
            }
            rs.close();
        } catch (SQLException e) {
            logger.error("Ошибка загрузки элементов списков услуг-запчастей", e);
        }
        return items;
    }

    @Override
    public void addServiceSparePartsList(com.autoservice.model.ServiceSparePartsList list) {
        String sql = "INSERT INTO service_spare_parts_lists (service_id, created_date, active) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, list.getServiceId());
            pstmt.setString(2, list.getCreatedDate());
            pstmt.setInt(3, list.isActive() ? 1 : 0);
            pstmt.executeUpdate();
            
            // Получаем сгенерированный id
            int listId = getGeneratedKeyId(conn, pstmt);
            if (listId > 0) {
                list.setId(listId);
            }
            
            // Сохраняем все элементы списка
            if (list.getItems() != null && !list.getItems().isEmpty()) {
                String itemSql = "INSERT INTO service_spare_parts_list_items (list_id, spare_part_id, quantity, unit_type) VALUES (?, ?, ?, ?)";
                try (PreparedStatement itemPstmt = conn.prepareStatement(itemSql, Statement.RETURN_GENERATED_KEYS)) {
                    for (com.autoservice.model.ServiceSparePartsListItem item : list.getItems()) {
                        item.setListId(list.getId());
                        itemPstmt.setInt(1, list.getId());
                        itemPstmt.setInt(2, item.getSparePartId());
                        itemPstmt.setInt(3, item.getQuantity());
                        itemPstmt.setString(4, item.getUnitType());
                        itemPstmt.executeUpdate();
                        
                        // Получаем сгенерированный id элемента
                        int itemId = getGeneratedKeyId(conn, itemPstmt);
                        if (itemId > 0) {
                            item.setId(itemId);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка добавления списка услуг-запчастей", e);
            logger.error("Технические детали: {}", ExceptionHandler.getTechnicalDetails(e));
        }
    }

    @Override
    public void addServiceSparePartsListItem(com.autoservice.model.ServiceSparePartsListItem item) {
        String sql = "INSERT INTO service_spare_parts_list_items (list_id, spare_part_id, quantity, unit_type) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, item.getListId());
            pstmt.setInt(2, item.getSparePartId());
            pstmt.setInt(3, item.getQuantity());
            pstmt.setString(4, item.getUnitType());
            pstmt.executeUpdate();
            
            // Получаем сгенерированный id
            int itemId = getGeneratedKeyId(conn, pstmt);
            if (itemId > 0) {
                item.setId(itemId);
            }
        } catch (SQLException e) {
            logger.error("Ошибка добавления элемента списка услуг-запчастей", e);
        }
    }

    @Override
    public void deleteServiceSparePartsList(com.autoservice.model.ServiceSparePartsList list) {
        // Сначала удаляем все элементы списка
        deleteServiceSparePartsListItemsByListId(list.getId());
        
        // Затем удаляем сам список
        String sql = "DELETE FROM service_spare_parts_lists WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, list.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка удаления списка услуг-запчастей", e);
        }
    }

    @Override
    public void deleteServiceSparePartsListsByServiceId(int serviceId) {
        // Сначала получаем все списки для удаления их элементов
        List<com.autoservice.model.ServiceSparePartsList> lists = getServiceSparePartsListsByServiceId(serviceId);
        for (com.autoservice.model.ServiceSparePartsList list : lists) {
            deleteServiceSparePartsListItemsByListId(list.getId());
        }
        
        // Затем удаляем сами списки
        String sql;
        
        if (serviceId == -1) {
            // Удалить все списки
            sql = "DELETE FROM service_spare_parts_lists";
        } else {
            sql = "DELETE FROM service_spare_parts_lists WHERE service_id = ?";
        }

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (serviceId != -1) {
                pstmt.setInt(1, serviceId);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка удаления списков услуг-запчастей по ID услуги", e);
        }
    }

    @Override
    public void deleteServiceSparePartsListItemsByListId(int listId) {
        String sql = "DELETE FROM service_spare_parts_list_items WHERE list_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, listId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка удаления элементов списков услуг-запчастей по ID списка", e);
        }
    }

    // ==================== TO PARTS RELATIONSHIPS ====================

    @Override
    public List<com.autoservice.model.ToPart> getToPartsByCarModel(String carModel) {
        List<com.autoservice.model.ToPart> parts = new ArrayList<>();
        String sql;
        
        if (carModel == null || carModel.isEmpty()) {
            // Получить все расходники
            sql = "SELECT id, car_model, spare_part_id, quantity, unit_type, note, active FROM to_parts";
        } else {
            // Получить расходники для конкретной модели
            sql = "SELECT id, car_model, spare_part_id, quantity, unit_type, note, active FROM to_parts WHERE car_model = ?";
        }

        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs;
            
            if (carModel == null || carModel.isEmpty()) {
                rs = stmt.executeQuery(sql);
            } else {
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, carModel);
                rs = pstmt.executeQuery();
            }
            
            while (rs.next()) {
                com.autoservice.model.ToPart part = new com.autoservice.model.ToPart();
                part.setId(rs.getInt("id"));
                part.setCarModel(rs.getString("car_model"));
                part.setSparePartId(rs.getInt("spare_part_id"));
                part.setQuantity(rs.getInt("quantity"));
                part.setUnitType(rs.getString("unit_type"));
                part.setNote(rs.getString("note"));
                part.setActive(rs.getInt("active") == 1);
                parts.add(part);
            }
            rs.close();
        } catch (SQLException e) {
            logger.error("Ошибка загрузки расходников TO", e);
        }
        return parts;
    }

    @Override
    public List<String> getAllCarModels() {
        List<String> carModels = new ArrayList<>();
        String sql = "SELECT DISTINCT car_model FROM to_parts WHERE car_model IS NOT NULL AND car_model != '' ORDER BY car_model";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String model = rs.getString("car_model");
                if (model != null && !model.trim().isEmpty()) {
                    carModels.add(model);
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка загрузки моделей авто", e);
        }
        return carModels;
    }

    @Override
    public void addToPart(com.autoservice.model.ToPart part) {
        String sql = "INSERT INTO to_parts (car_model, spare_part_id, quantity, unit_type, note, active) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, part.getCarModel());
            pstmt.setInt(2, part.getSparePartId());
            pstmt.setInt(3, part.getQuantity());
            pstmt.setString(4, part.getUnitType());
            pstmt.setString(5, part.getNote() != null ? part.getNote() : "");
            pstmt.setInt(6, part.isActive() ? 1 : 0);
            pstmt.executeUpdate();
            
            // Получаем сгенерированный id
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                part.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            logger.error("Ошибка добавления расходника TO", e);
        }
    }

    @Override
    public void updateToPart(com.autoservice.model.ToPart part) {
        String sql = "UPDATE to_parts SET spare_part_id = ?, quantity = ?, unit_type = ?, note = ?, active = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, part.getSparePartId());
            pstmt.setInt(2, part.getQuantity());
            pstmt.setString(3, part.getUnitType());
            pstmt.setString(4, part.getNote() != null ? part.getNote() : "");
            pstmt.setInt(5, part.isActive() ? 1 : 0);
            pstmt.setInt(6, part.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка обновления расходника TO", e);
        }
    }

    @Override
    public void deleteToPart(com.autoservice.model.ToPart part) {
        String sql = "DELETE FROM to_parts WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, part.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка удаления расходника TO", e);
        }
    }

    @Override
    public void deleteToPartsByCarModel(String carModel) {
        String sql;
        
        if (carModel == null || carModel.isEmpty()) {
            // Удалить все расходники
            sql = "DELETE FROM to_parts";
        } else {
            sql = "DELETE FROM to_parts WHERE car_model = ?";
        }

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (carModel != null && !carModel.isEmpty()) {
                pstmt.setString(1, carModel);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка удаления расходников TO по модели авто", e);
        }
    }

    // ==================== SETTINGS ====================

    @Override
    public List<com.autoservice.model.Setting> getAllSettings() {
        List<com.autoservice.model.Setting> settings = new ArrayList<>();
        String sql = "SELECT id, setting_key, setting_value, description FROM app_settings";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                com.autoservice.model.Setting setting = new com.autoservice.model.Setting();
                setting.setId(rs.getInt("id"));
                setting.setKey(rs.getString("setting_key"));
                setting.setValue(rs.getString("setting_value"));
                setting.setDescription(rs.getString("description"));
                settings.add(setting);
            }
        } catch (SQLException e) {
            logger.error("Ошибка загрузки настроек", e);
        }
        return settings;
    }

    @Override
    public void addSetting(com.autoservice.model.Setting setting) {
        String sql = "INSERT INTO app_settings (setting_key, setting_value, description) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, setting.getKey());
            pstmt.setString(2, setting.getValue());
            pstmt.setString(3, setting.getDescription());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка добавления настройки", e);
        }
    }

    @Override
    public void updateSetting(com.autoservice.model.Setting setting) {
        String sql = "UPDATE app_settings SET setting_value = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, setting.getValue());
            pstmt.setInt(2, setting.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка обновления настройки", e);
        }
    }

    @Override
    public void deleteSetting(com.autoservice.model.Setting setting) {
        String sql = "DELETE FROM app_settings WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, setting.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка удаления настройки", e);
        }
    }

    @Override
    public com.autoservice.model.Setting getSettingByKey(String key) {
        String sql = "SELECT id, setting_key, setting_value, description FROM app_settings WHERE setting_key = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                com.autoservice.model.Setting setting = new com.autoservice.model.Setting();
                setting.setId(rs.getInt("id"));
                setting.setKey(rs.getString("setting_key"));
                setting.setValue(rs.getString("setting_value"));
                setting.setDescription(rs.getString("description"));
                return setting;
            }
        } catch (SQLException e) {
            logger.error("Ошибка получения настройки по ключу", e);
        }
        return null;
    }
}
