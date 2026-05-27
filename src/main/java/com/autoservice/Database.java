package com.autoservice;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static final String DB_URL = "jdbc:sqlite:autoservice.db";
    private static Connection connection;

    public static void init() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            createTables();
            System.out.println("База данных подключена");
        } catch (SQLException e) {
            System.err.println("Ошибка подключения к БД: " + e.getMessage());
        }
    }

    public static void initForTest() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:test.db");
            createTables();
            System.out.println("Тестовая БД подключена");
        } catch (SQLException e) {
            System.err.println("Ошибка подключения к тестовой БД: " + e.getMessage());
        }
    }

    private static void createTables() throws SQLException {
        String createClients = """
            CREATE TABLE IF NOT EXISTS clients (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                phone TEXT NOT NULL,
                car_model TEXT NOT NULL,
                car_number TEXT NOT NULL
            )
        """;

        String createServices = """
            CREATE TABLE IF NOT EXISTS services (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                price REAL NOT NULL
            )
        """;

        String createSpareParts = """
            CREATE TABLE IF NOT EXISTS spare_parts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                purchase_price REAL NOT NULL,
                retail_price REAL NOT NULL,
                stock INTEGER NOT NULL
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

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createClients);
            stmt.execute(createServices);
            stmt.execute(createSpareParts);
            stmt.execute(createOrders);
            stmt.execute(createOrderServices);
            stmt.execute(createOrderParts);
            stmt.execute(createAppointments);
            System.out.println("Таблицы созданы/проверены");
        }
    }

    private static String generateOrderId() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yy"));
        String pattern = "ZAK-" + date + "-%";

        int lastNumber = 0;
        String sql = "SELECT id FROM orders WHERE id LIKE ? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, pattern);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String lastId = rs.getString("id");
                String numPart = lastId.substring(lastId.lastIndexOf("-") + 1);
                lastNumber = Integer.parseInt(numPart);
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения последнего ID: " + e.getMessage());
        }

        int newNumber = lastNumber + 1;
        return String.format("ZAK-%s-%05d", date, newNumber);
    }

    // ==================== КЛИЕНТЫ ====================

    public static List<Client> getAllClients() {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT id, name, phone, car_model, car_number FROM clients ORDER BY id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                clients.add(new Client(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("car_model"),
                        rs.getString("car_number")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка загрузки клиентов: " + e.getMessage());
        }
        return clients;
    }

    public static void addClient(Client client) {
        String normalizedPhone = Validators.cleanPhone(client.getPhone());
        String normalizedCarNumber = Validators.normalizeCarNumber(client.getCarNumber());

        String sql = "INSERT INTO clients (name, phone, car_model, car_number) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, client.getName());
            pstmt.setString(2, normalizedPhone);
            pstmt.setString(3, client.getCarModel());
            pstmt.setString(4, normalizedCarNumber);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка добавления клиента: " + e.getMessage());
        }
    }

    public static void updateClient(Client client) {
        String normalizedPhone = Validators.cleanPhone(client.getPhone());
        String normalizedCarNumber = Validators.normalizeCarNumber(client.getCarNumber());

        String sql = "UPDATE clients SET name = ?, phone = ?, car_model = ?, car_number = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, client.getName());
            pstmt.setString(2, normalizedPhone);
            pstmt.setString(3, client.getCarModel());
            pstmt.setString(4, normalizedCarNumber);
            pstmt.setInt(5, client.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка обновления клиента: " + e.getMessage());
        }
    }

    public static void deleteClient(Client client) {
        String sql = "DELETE FROM clients WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, client.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка удаления клиента: " + e.getMessage());
        }
    }

    public static Client getClientById(int id) {
        String sql = "SELECT name, phone, car_model, car_number FROM clients WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Client(rs.getString("name"), rs.getString("phone"),
                        rs.getString("car_model"), rs.getString("car_number"));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка поиска клиента: " + e.getMessage());
        }
        return null;
    }

    public static int getClientId(Client client) {
        String sql = "SELECT id FROM clients WHERE name = ? AND phone = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, client.getName());
            pstmt.setString(2, client.getPhone());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.err.println("Ошибка поиска ID клиента: " + e.getMessage());
        }
        return -1;
    }

    // ==================== УСЛУГИ ====================

    public static List<Service> getAllServices() {
        List<Service> services = new ArrayList<>();
        String sql = "SELECT name, price FROM services ORDER BY name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                services.add(new Service(rs.getString("name"), rs.getDouble("price")));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка загрузки услуг: " + e.getMessage());
        }
        return services;
    }

    public static void addService(Service service) {
        String sql = "INSERT OR REPLACE INTO services (name, price) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, service.getName());
            pstmt.setDouble(2, service.getPrice());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка добавления услуги: " + e.getMessage());
        }
    }

    public static void deleteService(Service service) {
        String sql = "DELETE FROM services WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, service.getName());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка удаления услуги: " + e.getMessage());
        }
    }

    // ==================== ЗАПЧАСТИ ====================

    public static List<SparePart> getAllSpareParts() {
        List<SparePart> parts = new ArrayList<>();
        String sql = "SELECT name, purchase_price, retail_price, stock FROM spare_parts ORDER BY name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                parts.add(new SparePart(rs.getString("name"), rs.getDouble("purchase_price"),
                        rs.getDouble("retail_price"), rs.getInt("stock")));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка загрузки запчастей: " + e.getMessage());
        }
        return parts;
    }

    public static void addSparePart(SparePart part) {
        String sql = "INSERT OR REPLACE INTO spare_parts (name, purchase_price, retail_price, stock) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, part.getName());
            pstmt.setDouble(2, part.getPurchasePrice());
            pstmt.setDouble(3, part.getRetailPrice());
            pstmt.setInt(4, part.getStock());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка добавления запчасти: " + e.getMessage());
        }
    }

    public static void deleteSparePart(SparePart part) {
        String sql = "DELETE FROM spare_parts WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, part.getName());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка удаления запчасти: " + e.getMessage());
        }
    }

    public static void updateSparePartStock(SparePart part, int newStock) {
        String sql = "UPDATE spare_parts SET stock = ? WHERE name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, newStock);
            pstmt.setString(2, part.getName());
            pstmt.executeUpdate();
            part.setStock(newStock);
        } catch (SQLException e) {
            System.err.println("Ошибка обновления остатка: " + e.getMessage());
        }
    }

    // ==================== ЗАКАЗЫ ====================

    public static List<WorkOrder> getAllOrders() {
        List<WorkOrder> orders = new ArrayList<>();
        String sql = "SELECT id, client_id, status, total, created_date FROM orders ORDER BY created_date DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String orderId = rs.getString("id");
                int clientId = rs.getInt("client_id");
                String status = rs.getString("status");
                double total = rs.getDouble("total");

                Client client = getClientById(clientId);
                if (client == null) continue;

                WorkOrder order = new WorkOrder(orderId, client, status, total);

                String servicesSql = "SELECT service_name, price FROM order_services WHERE order_id = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(servicesSql)) {
                    pstmt.setString(1, orderId);
                    ResultSet rs2 = pstmt.executeQuery();
                    while (rs2.next()) {
                        order.addService(rs2.getString("service_name"), rs2.getDouble("price"));
                    }
                }

                String partsSql = "SELECT part_name, price, quantity FROM order_parts WHERE order_id = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(partsSql)) {
                    pstmt.setString(1, orderId);
                    ResultSet rs2 = pstmt.executeQuery();
                    while (rs2.next()) {
                        String partName = rs2.getString("part_name");
                        double price = rs2.getDouble("price");
                        int qty = rs2.getInt("quantity");
                        SparePart part = new SparePart(partName, price, price, qty);
                        order.addSparePart(part, qty);
                    }
                }

                orders.add(order);
            }
        } catch (SQLException e) {
            System.err.println("Ошибка загрузки заказов: " + e.getMessage());
        }
        return orders;
    }

    public static void addOrder(WorkOrder order) {
        int clientId = getClientId(order.getClient());
        if (clientId == -1) {
            System.err.println("Клиент не найден, заказ не сохранён");
            return;
        }

        String orderId = generateOrderId();
        order.setId(orderId);

        String sql = "INSERT INTO orders (id, client_id, status, total, created_date) VALUES (?, ?, ?, ?, datetime('now'))";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            pstmt.setInt(2, clientId);
            pstmt.setString(3, order.getStatus());
            pstmt.setDouble(4, order.getTotal());
            pstmt.executeUpdate();

            saveOrderServices(orderId, order);
            saveOrderParts(orderId, order);
            System.out.println("Заказ " + orderId + " сохранён");
        } catch (SQLException e) {
            System.err.println("Ошибка добавления заказа: " + e.getMessage());
        }
    }

    private static void saveOrderServices(String orderId, WorkOrder order) {
        String sql = "INSERT INTO order_services (order_id, service_name, price) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < order.getServices().size(); i++) {
                pstmt.setString(1, orderId);
                pstmt.setString(2, order.getServices().get(i));
                pstmt.setDouble(3, order.getServicePrices().get(i));
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Ошибка сохранения услуг: " + e.getMessage());
        }
    }

    private static void saveOrderParts(String orderId, WorkOrder order) {
        String sql = "INSERT INTO order_parts (order_id, part_name, price, quantity) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < order.getSpareParts().size(); i++) {
                pstmt.setString(1, orderId);
                pstmt.setString(2, order.getSpareParts().get(i).getName());
                pstmt.setDouble(3, order.getSpareParts().get(i).getRetailPrice());
                pstmt.setInt(4, order.getSparePartQuantities().get(i));
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Ошибка сохранения запчастей: " + e.getMessage());
        }
    }

    public static void updateOrder(WorkOrder order) {
        String orderId = order.getId();
        if (orderId == null || orderId.isEmpty()) {
            System.err.println("Ошибка: ID заказа пустой");
            return;
        }

        try {
            // Обновляем статус и сумму
            String updateOrderSql = "UPDATE orders SET status = ?, total = ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(updateOrderSql)) {
                pstmt.setString(1, order.getStatus());
                pstmt.setDouble(2, order.getTotal());
                pstmt.setString(3, orderId);
                pstmt.executeUpdate();
            }

            // Удаляем старые услуги
            String deleteServicesSql = "DELETE FROM order_services WHERE order_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteServicesSql)) {
                pstmt.setString(1, orderId);
                pstmt.executeUpdate();
            }

            // Удаляем старые запчасти
            String deletePartsSql = "DELETE FROM order_parts WHERE order_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deletePartsSql)) {
                pstmt.setString(1, orderId);
                pstmt.executeUpdate();
            }

            // Добавляем новые услуги
            String insertServiceSql = "INSERT INTO order_services (order_id, service_name, price) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(insertServiceSql)) {
                for (int i = 0; i < order.getServices().size(); i++) {
                    pstmt.setString(1, orderId);
                    pstmt.setString(2, order.getServices().get(i));
                    pstmt.setDouble(3, order.getServicePrices().get(i));
                    pstmt.executeUpdate();
                }
            }

            // Добавляем новые запчасти
            String insertPartSql = "INSERT INTO order_parts (order_id, part_name, price, quantity) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(insertPartSql)) {
                for (int i = 0; i < order.getSpareParts().size(); i++) {
                    pstmt.setString(1, orderId);
                    pstmt.setString(2, order.getSpareParts().get(i).getName());
                    pstmt.setDouble(3, order.getSpareParts().get(i).getRetailPrice());
                    pstmt.setInt(4, order.getSparePartQuantities().get(i));
                    pstmt.executeUpdate();
                }
            }

            System.out.println("Заказ " + orderId + " обновлён");

        } catch (SQLException e) {
            System.err.println("Ошибка обновления заказа: " + e.getMessage());
        }
    }

    public static void deleteOrder(String orderId) {
        try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM orders WHERE id = ?")) {
            pstmt.setString(1, orderId);
            pstmt.executeUpdate();
            System.out.println("Заказ " + orderId + " удалён");
        } catch (SQLException e) {
            System.err.println("Ошибка удаления заказа: " + e.getMessage());
        }
    }

    // ==================== ЗАПИСИ ====================

    public static List<Appointment> getAllAppointments() {
        List<Appointment> appointments = new ArrayList<>();
        String sql = "SELECT id, client_id, order_id, master_name, service_name, appointment_date, appointment_time, status FROM appointments ORDER BY appointment_date, appointment_time";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int clientId = rs.getInt("client_id");
                Client client = getClientById(clientId);
                if (client == null) continue;

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
            System.err.println("Ошибка загрузки записей: " + e.getMessage());
        }
        return appointments;
    }

    public static void addAppointment(Appointment appointment) {
        String sql = "INSERT INTO appointments (client_id, order_id, master_name, service_name, appointment_date, appointment_time, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
            System.err.println("Ошибка добавления записи: " + e.getMessage());
        }
    }

    public static void updateAppointment(Appointment appointment) {
        String sql = "UPDATE appointments SET client_id = ?, master_name = ?, service_name = ?, appointment_date = ?, appointment_time = ?, status = ?, order_id = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
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
            System.err.println("Ошибка обновления записи: " + e.getMessage());
        }
    }

    public static void deleteAppointment(int id) {
        String sql = "DELETE FROM appointments WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка удаления записи: " + e.getMessage());
        }
    }

    public static List<Appointment> getAppointmentsByDate(String date) {
        List<Appointment> appointments = new ArrayList<>();
        String sql = "SELECT id, client_id, order_id, master_name, service_name, appointment_date, appointment_time, status FROM appointments WHERE appointment_date = ? ORDER BY appointment_time";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, date);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int clientId = rs.getInt("client_id");
                Client client = getClientById(clientId);
                if (client != null) {
                    System.out.println("Загружен клиент: " + client.getName() + ", авто: " + client.getCarModel() + " (" + client.getCarNumber() + ")");
                    Appointment a = new Appointment(
                            rs.getInt("id"),
                            client,
                            rs.getString("order_id"),
                            rs.getString("master_name"),
                            rs.getString("service_name"),
                            rs.getString("appointment_date"),
                            rs.getString("appointment_time"),
                            rs.getString("status")
                    );
                    appointments.add(a);
                } else {
                    System.err.println("Клиент с ID=" + clientId + " не найден");
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка загрузки записей по дате: " + e.getMessage());
        }
        return appointments;
    }

    public static Connection getConnection() {
        return connection;
    }

    public static void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            System.err.println("Ошибка закрытия БД: " + e.getMessage());
        }
    }
}