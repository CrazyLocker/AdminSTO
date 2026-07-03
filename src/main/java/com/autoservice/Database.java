package com.autoservice;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Database {
    private static HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:autoservice.db");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        dataSource = new HikariDataSource(config);
    }

    public static void init() {
        try (Connection conn = getConnection()) {
            createTables(conn);
            System.out.println("Database connected with connection pool");
        } catch (SQLException e) {
            System.err.println("DB connection error: " + e.getMessage());
        }
    }

    public static void initForTest() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:test.db");
        config.setMaximumPoolSize(5);
        dataSource = new HikariDataSource(config);

        try (Connection conn = getConnection()) {
            createTables(conn);
            System.out.println("Test database connected");
        } catch (SQLException e) {
            System.err.println("Test DB error: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        String createClients = """
                    CREATE TABLE IF NOT EXISTS clients (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        last_name TEXT DEFAULT '',
                        phone TEXT NOT NULL,
                        car_model TEXT NOT NULL,
                        car_number TEXT NOT NULL,
                        last_repair_date TEXT DEFAULT ''
                    )
                """;

        String createServices = """
                    CREATE TABLE IF NOT EXISTS services (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL UNIQUE,
                        price REAL NOT NULL,
                        duration INTEGER DEFAULT 60,
                        part_number TEXT DEFAULT '',
                        oil_volume REAL DEFAULT 0,
                        uses_oil INTEGER DEFAULT 0,
                        spare_part_name TEXT DEFAULT '',
                        spare_part_quantity INTEGER DEFAULT 0
                    )
                """;

        String createSpareParts = """
                    CREATE TABLE IF NOT EXISTS spare_parts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL UNIQUE,
                        part_number TEXT DEFAULT '',
                        manufacturer TEXT DEFAULT '',
                        compatible_models TEXT DEFAULT '',
                        purchase_price REAL,
                        retail_price REAL NOT NULL,
                        stock REAL DEFAULT 0,
                        min_stock REAL DEFAULT 0,
                        location TEXT DEFAULT '',
                        unit_volume REAL DEFAULT 1.0,
                        unit_type TEXT DEFAULT 'шт',
                        is_liquid INTEGER DEFAULT 0
                    )
                """;

        String createOrders = """
                    CREATE TABLE IF NOT EXISTS orders (
                        id TEXT PRIMARY KEY,
                        client_id INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        total REAL NOT NULL,
                        created_date TEXT NOT NULL,
                        FOREIGN KEY (client_id) REFERENCES clients(id)
                    )
                """;

        String createOrderServices = """
                    CREATE TABLE IF NOT EXISTS order_services (
                        order_id TEXT NOT NULL,
                        service_name TEXT NOT NULL,
                        price REAL NOT NULL,
                        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
                    )
                """;

        String createOrderParts = """
                    CREATE TABLE IF NOT EXISTS order_parts (
                        order_id TEXT NOT NULL,
                        part_name TEXT NOT NULL,
                        price REAL NOT NULL,
                        quantity INTEGER NOT NULL,
                        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
                    )
                """;

        String createAppointments = """
                    CREATE TABLE IF NOT EXISTS appointments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        client_id INTEGER NOT NULL,
                        order_id TEXT,
                        master_name TEXT NOT NULL,
                        service_name TEXT NOT NULL,
                        appointment_date TEXT NOT NULL,
                        appointment_time TEXT NOT NULL,
                        status TEXT NOT NULL,
                        FOREIGN KEY (client_id) REFERENCES clients(id),
                        FOREIGN KEY (order_id) REFERENCES orders(id)
                    )
                """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createClients);
            stmt.execute(createServices);
            stmt.execute(createSpareParts);
            stmt.execute(createOrders);
            stmt.execute(createOrderServices);
            stmt.execute(createOrderParts);
            stmt.execute(createAppointments);
            createIndexes(conn);
            System.out.println("Tables and indexes created/verified");
        }
    }

    private static void createIndexes(Connection conn) throws SQLException {
        List<String> indexes = List.of(
                "CREATE INDEX IF NOT EXISTS idx_orders_client_id ON orders(client_id)",
                "CREATE INDEX IF NOT EXISTS idx_orders_created_date ON orders(created_date)",
                "CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status)",
                "CREATE INDEX IF NOT EXISTS idx_appointments_client_id ON appointments(client_id)",
                "CREATE INDEX IF NOT EXISTS idx_appointments_date ON appointments(appointment_date)",
                "CREATE INDEX IF NOT EXISTS idx_appointments_status ON appointments(status)",
                "CREATE INDEX IF NOT EXISTS idx_order_services_order_id ON order_services(order_id)",
                "CREATE INDEX IF NOT EXISTS idx_order_parts_order_id ON order_parts(order_id)",
                "CREATE INDEX IF NOT EXISTS idx_spare_parts_name ON spare_parts(name)",
                "CREATE INDEX IF NOT EXISTS idx_clients_name_lastname_phone ON clients(name, last_name, phone)"
        );

        try (Statement stmt = conn.createStatement()) {
            for (String index : indexes) {
                stmt.execute(index);
            }
        }
    }

    // ==================== ГЕНЕРАЦИЯ ID ЗАКАЗА ====================

    private static String generateOrderId(Connection conn) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yy"));
        String sql = "SELECT MAX(CAST(SUBSTR(id, INSTR(id, '-') + 1) AS INTEGER)) as max_num FROM orders";
        int lastNumber = 0;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                lastNumber = rs.getInt("max_num");
            }
        } catch (SQLException e) {
            System.err.println("Generate ID error: " + e.getMessage());
        }

        int newNumber = (lastNumber % 9999) + 1;
        return String.format("ZAK-%s-%04d", date, newNumber);
    }

    // ==================== CLIENTS ====================

    public static List<Client> getAllClients() {
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
            System.err.println("Load clients error: " + e.getMessage());
        }
        return clients;
    }

    public static void addClient(Client client) {
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
            System.err.println("Add client error: " + e.getMessage());
        }
    }

    public static void updateClient(Client client) {
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
            System.err.println("Update client error: " + e.getMessage());
        }
    }

    public static void deleteClient(Client client) {
        String sql = "DELETE FROM clients WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, client.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Delete client error: " + e.getMessage());
        }
    }

    public static Client getClientById(int id) {
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
            System.err.println("Get client error: " + e.getMessage());
        }
        return null;
    }

    public static int getClientId(Client client) {
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
            System.err.println("Get client ID error: " + e.getMessage());
        }
        return -1;
    }

    // ==================== SERVICES ====================

    public static List<Service> getAllServices() {
        List<Service> services = new ArrayList<>();
        String sql = "SELECT name, price, duration, part_number, oil_volume, uses_oil, spare_part_name, spare_part_quantity FROM services ORDER BY name";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Service service = new Service(rs.getString("name"), rs.getDouble("price"));
                service.setDuration(rs.getInt("duration"));
                service.setPartNumber(rs.getString("part_number"));
                service.setOilVolume(rs.getDouble("oil_volume"));
                service.setUsesOil(rs.getInt("uses_oil") == 1);
                service.setSparePartName(rs.getString("spare_part_name"));
                service.setSparePartQuantity(rs.getInt("spare_part_quantity"));
                services.add(service);
            }
        } catch (SQLException e) {
            System.err.println("Load services error: " + e.getMessage());
        }
        return services;
    }

    public static void addService(Service service) {
        String sql = "INSERT OR REPLACE INTO services (name, price, duration, part_number, oil_volume, uses_oil, spare_part_name, spare_part_quantity) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

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
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Add service error: " + e.getMessage());
        }
    }

    public static void deleteService(Service service) {
        String sql = "DELETE FROM services WHERE name = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, service.getName());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Delete service error: " + e.getMessage());
        }
    }

    // ==================== SPARE PARTS ====================

    public static List<SparePart> getAllSpareParts() {
        List<SparePart> parts = new ArrayList<>();
        String sql = "SELECT id, name, part_number, manufacturer, compatible_models, purchase_price, retail_price, stock, min_stock, location, unit_volume, unit_type, is_liquid FROM spare_parts ORDER BY name";

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
                        rs.getDouble("purchase_price"),
                        rs.getDouble("retail_price"),
                        rs.getDouble("stock"),
                        rs.getDouble("min_stock"),
                        rs.getDouble("unit_volume"),
                        rs.getString("unit_type"),
                        rs.getInt("is_liquid") == 1,
                        rs.getString("location")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Load spare parts error: " + e.getMessage());
        }
        return parts;
    }

    public static void addSparePart(SparePart part) {
        String sql;
        try (Connection conn = getConnection()) {
            if (part.getId() != -1) {
                sql = "UPDATE spare_parts SET name = ?, purchase_price = ?, retail_price = ?, stock = ?, " +
                        "part_number = ?, manufacturer = ?, compatible_models = ?, min_stock = ?, location = ?, " +
                        "unit_volume = ?, unit_type = ?, is_liquid = ? WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, part.getName());
                    pstmt.setDouble(2, part.getPurchasePrice());
                    pstmt.setDouble(3, part.getRetailPrice());
                    pstmt.setDouble(4, part.getStock());
                    pstmt.setString(5, part.getPartNumber());
                    pstmt.setString(6, part.getManufacturer());
                    pstmt.setString(7, part.getCompatibleModels());
                    pstmt.setDouble(8, part.getMinStock());
                    pstmt.setString(9, part.getLocation());
                    pstmt.setDouble(10, part.getUnitVolume());
                    pstmt.setString(11, part.getUnitType());
                    pstmt.setInt(12, part.isLiquid() ? 1 : 0);
                    pstmt.setInt(13, part.getId());
                    pstmt.executeUpdate();
                }
            } else {
                sql = "INSERT INTO spare_parts (name, purchase_price, retail_price, stock, part_number, manufacturer, compatible_models, min_stock, location, unit_volume, unit_type, is_liquid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, part.getName());
                    pstmt.setDouble(2, part.getPurchasePrice());
                    pstmt.setDouble(3, part.getRetailPrice());
                    pstmt.setDouble(4, part.getStock());
                    pstmt.setString(5, part.getPartNumber());
                    pstmt.setString(6, part.getManufacturer());
                    pstmt.setString(7, part.getCompatibleModels());
                    pstmt.setDouble(8, part.getMinStock());
                    pstmt.setString(9, part.getLocation());
                    pstmt.setDouble(10, part.getUnitVolume());
                    pstmt.setString(11, part.getUnitType());
                    pstmt.setInt(12, part.isLiquid() ? 1 : 0);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Add spare part error: " + e.getMessage());
        }
    }

    public static void deleteSparePart(SparePart part) {
        String sql = "DELETE FROM spare_parts WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, part.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Delete spare part error: " + e.getMessage());
        }
    }

    public static void updateSparePartStock(SparePart part, double newStock) {
        String sql = "UPDATE spare_parts SET stock = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newStock);
            pstmt.setInt(2, part.getId());
            pstmt.executeUpdate();
            part.setStock(newStock);
        } catch (SQLException e) {
            System.err.println("Update stock error: " + e.getMessage());
        }
    }

    // ==================== ORDERS ====================

    public static List<WorkOrder> getAllOrders() {
        Map<String, WorkOrder> orderMap = new LinkedHashMap<>();

        String ordersSql = """
            SELECT o.id as order_id, o.status, o.total, o.created_date,
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
                orderMap.put(rs.getString("order_id"), order);
            }
        } catch (SQLException e) {
            System.err.println("Load orders error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }

        String servicesSql = "SELECT order_id, service_name, price FROM order_services";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(servicesSql)) {
            while (rs.next()) {
                String orderId = rs.getString("order_id");
                WorkOrder order = orderMap.get(orderId);
                if (order != null) {
                    order.addService(rs.getString("service_name"), rs.getDouble("price"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Load order services error: " + e.getMessage());
        }

        String partsSql = "SELECT order_id, part_name, price, quantity FROM order_parts";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(partsSql)) {
            while (rs.next()) {
                String orderId = rs.getString("order_id");
                WorkOrder order = orderMap.get(orderId);
                if (order != null) {
                    SparePart part = new SparePart(
                            rs.getString("part_name"),
                            0,
                            rs.getDouble("price"),
                            rs.getInt("quantity")
                    );
                    order.addSparePart(part, rs.getInt("quantity"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Load order parts error: " + e.getMessage());
        }

        return new ArrayList<>(orderMap.values());
    }

    public static void addOrder(WorkOrder order) {
        try (Connection conn = getConnection()) {
            int clientId = getClientId(order.getClient());
            if (clientId == -1) {
                System.err.println("Client not found, order not saved");
                return;
            }

            String orderId = generateOrderId(conn);
            order.setId(orderId);

            conn.setAutoCommit(false);

            String sql = "INSERT INTO orders (id, client_id, status, total, created_date) VALUES (?, ?, ?, ?, datetime('now'))";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, orderId);
                pstmt.setInt(2, clientId);
                pstmt.setString(3, order.getStatus());
                pstmt.setDouble(4, order.getTotal());
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

    private static void saveOrderServices(Connection conn, String orderId, WorkOrder order) throws SQLException {
        String sql = "INSERT INTO order_services (order_id, service_name, price) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < order.getServices().size(); i++) {
                pstmt.setString(1, orderId);
                pstmt.setString(2, order.getServices().get(i));
                pstmt.setDouble(3, order.getServicePrices().get(i));
                pstmt.executeUpdate();
            }
        }
    }

    private static void saveOrderParts(Connection conn, String orderId, WorkOrder order) throws SQLException {
        String sql = "INSERT INTO order_parts (order_id, part_name, price, quantity) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < order.getSpareParts().size(); i++) {
                pstmt.setString(1, orderId);
                pstmt.setString(2, order.getSpareParts().get(i).getName());
                pstmt.setDouble(3, order.getSpareParts().get(i).getRetailPrice());
                pstmt.setInt(4, order.getSparePartQuantities().get(i));
                pstmt.executeUpdate();
            }
        }
    }

    public static void updateOrder(WorkOrder order) {
        String orderId = order.getId();
        if (orderId == null || orderId.isEmpty()) {
            System.err.println("Order ID is empty");
            return;
        }

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            String updateOrderSql = "UPDATE orders SET status = ?, total = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateOrderSql)) {
                pstmt.setString(1, order.getStatus());
                pstmt.setDouble(2, order.getTotal());
                pstmt.setString(3, orderId);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM order_services WHERE order_id = ?")) {
                pstmt.setString(1, orderId);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM order_parts WHERE order_id = ?")) {
                pstmt.setString(1, orderId);
                pstmt.executeUpdate();
            }

            saveOrderServices(conn, orderId, order);
            saveOrderParts(conn, orderId, order);

            conn.commit();
            System.out.println("Order " + orderId + " updated");

        } catch (SQLException e) {
            System.err.println("Update order error: " + e.getMessage());
        }
    }

    public static void deleteOrder(String orderId) {
        String sql = "DELETE FROM orders WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Delete order error: " + e.getMessage());
        }
    }

    // ==================== APPOINTMENTS ====================

    public static List<Appointment> getAllAppointments() {
        List<Appointment> appointments = new ArrayList<>();
        String sql = """
            SELECT a.id, a.client_id, a.order_id, a.master_name, 
                   a.service_name, a.appointment_date, a.appointment_time, a.status,
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

                appointments.add(new Appointment(
                        rs.getInt("id"),
                        client,
                        rs.getString("order_id"),
                        rs.getString("master_name"),
                        rs.getString("service_name"),
                        rs.getString("appointment_date"),
                        rs.getString("appointment_time"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Load appointments error: " + e.getMessage());
        }
        return appointments;
    }

    public static void addAppointment(Appointment appointment) {
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

            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                appointment.setId(generatedKeys.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("Add appointment error: " + e.getMessage());
        }
    }

    public static void updateAppointment(Appointment appointment) {
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

    public static void deleteAppointment(int id) {
        String sql = "DELETE FROM appointments WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Delete appointment error: " + e.getMessage());
        }
    }

    public static List<Appointment> getAppointmentsByDate(String date) {
        List<Appointment> appointments = new ArrayList<>();
        String sql = """
            SELECT a.id, a.client_id, a.order_id, a.master_name, 
                   a.service_name, a.appointment_date, a.appointment_time, a.status,
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

                appointments.add(new Appointment(
                        rs.getInt("id"),
                        client,
                        rs.getString("order_id"),
                        rs.getString("master_name"),
                        rs.getString("service_name"),
                        rs.getString("appointment_date"),
                        rs.getString("appointment_time"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Load appointments by date error: " + e.getMessage());
        }
        return appointments;
    }
}