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
import com.autoservice.utils.Formatters;
import com.autoservice.Client;
import com.autoservice.Car;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.WorkOrder;
import com.autoservice.Appointment;

/**
 * Абстрактный базовый класс для реализаций DatabaseInterface.
 * Содержит общую логику для SQLite и H2.
 */
public abstract class AbstractDatabase implements DatabaseInterface {
    
    protected HikariDataSource dataSource;
    
    /**
     * Блокировка генерации и вставки заказа.
     * Генерация ID выполняется через SELECT MAX(id), поэтому параллельные
     * вставки без синхронизации получают одинаковый номер и падают на PRIMARY KEY.
     */
    protected static final Object ORDER_ID_LOCK = new Object();

    private static final Logger logger = LoggerFactory.getLogger(AbstractDatabase.class);
    
    /**
     * Создание таблиц базы данных. DDL общий для обеих СУБД,
     * различия диалектов учитываются в {@link DatabaseSchema}.
     * @param conn соединение с базой данных
     */
    protected void createTables(Connection conn) throws SQLException {
        DatabaseSchema.createTables(conn, isSqlite(conn));
    }
    
    /**
     * Генерация уникального ID заказа.
     * @param conn соединение с базой данных
     * @return сгенерированный ID заказа
     */
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
                int id = rs.getInt(1);
                if (id > 0) {
                    return id;
                }
            }
        }
        // Fallback только для SQLite: H2 не знает функцию last_insert_rowid().
        if (isSqlite(conn)) {
            try (PreparedStatement queryStmt = conn.prepareStatement("SELECT last_insert_rowid()");
                 ResultSet rs = queryStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    /**
     * Проверяет, что соединение указывает на SQLite.
     * Нужно, чтобы SQLite-специфичные вызовы (last_insert_rowid, PRAGMA)
     * не попадали в H2 и другие СУБД.
     */
    protected boolean isSqlite(Connection conn) throws SQLException {
        String url = conn.getMetaData().getURL();
        return url != null && url.contains("sqlite");
    }

    /**
     * Проверяет, что соединение указывает на H2.
     */
    protected boolean isH2(Connection conn) throws SQLException {
        String url = conn.getMetaData().getURL();
        return url != null && url.contains("h2");
    }
    
    // ==================== DEFAULT IMPLEMENTATIONS ====================
    
    /**
     * Инициализировать production-базу данных.
     *
     * <p>Базовый класс не содержит реализации: метод должна переопределить конкретная СУБД.
     * @throws UnsupportedOperationException если метод не переопределён
     */
    @Override
    public void init() {
        throw new UnsupportedOperationException("init() must be implemented by concrete database class");
    }
    
    /**
     * Инициализировать тестовую базу данных.
     *
     * <p>Базовый класс не содержит реализации: метод должна переопределить конкретная СУБД.
     * @throws UnsupportedOperationException если метод не переопределён
     */
    @Override
    public void initForTest() {
        throw new UnsupportedOperationException("initForTest() must be implemented by concrete database class");
    }
    
    /**
     * Закрыть пул соединений и освободить ресурсы.
     */
    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
    
    /**
     * Получить соединение из пула.
     * @return соединение с базой данных
     * @throws SQLException если соединение недоступно
     */
    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    // ==================== CLIENTS ====================
    
    /**
     * Получить всех клиентов вместе с их автомобилями.
     * @return список клиентов; пустой список, если клиентов нет
     */
    @Override
    public List<Client> getAllClients() {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT id, name, last_name, phone, car_model, car_number, last_repair_date FROM clients ORDER BY id";

        // Все запросы выполняются на одном соединении: вложенный getConnection()
        // на каждый автомобиль исчерпывал пул (H2: 5 соединений) и приводил к
        // взаимной блокировке параллельных тестов.
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
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
            }
            for (Client client : clients) {
                client.setCars(loadCarsByClientId(conn, client.getId()));
            }
        } catch (SQLException e) {
            logger.error("Ошибка загрузки клиентов", e);
        }
        return clients;
    }
    
    /**
     * Добавить клиента.
     * @param client клиент для добавления; после вызова получает сгенерированный ID
     */
    @Override
    public void addClient(Client client) {
        String normalizedPhone = Formatters.cleanPhone(client.getPhone());
        String normalizedCarNumber = Formatters.normalizeCarNumber(client.getCarNumber());
        String sql = "INSERT INTO clients (name, last_name, phone, car_model, car_number, last_repair_date) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, client.getName());
            pstmt.setString(2, client.getLastName());
            pstmt.setString(3, normalizedPhone);
            pstmt.setString(4, client.getCarModel());
            pstmt.setString(5, normalizedCarNumber);
            pstmt.setString(6, client.getLastRepairDate());
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                logger.error("Ошибка добавления клиента: не удалось вставить запись");
                return;
            }
            
            // Получаем ID через RETURN_GENERATED_KEYS (может не работать для SQLite)
            try {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        client.setId(rs.getInt(1));
                        return;
                    }
                }
            } catch (SQLException e) {
                logger.debug("getGeneratedKeys не поддерживается, используем fallback", e);
            }
            
            // Fallback: получаем ID через last_insert_rowid() для SQLite или MAX(id) для H2
            if (isH2(conn)) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT MAX(id) FROM clients")) {
                    if (rs.next()) {
                        client.setId(rs.getInt(1));
                    }
                }
            } else if (isSqlite(conn)) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        client.setId(rs.getInt(1));
                    }
                }
            }
            logger.info("Client ID after insert: {}", client.getId());
        } catch (SQLException e) {
            logger.error("Ошибка добавления клиента", e);
        }
    }
    
    /**
     * Обновить данные клиента.
     * @param client клиент с обновлёнными данными
     */
    @Override
    public void updateClient(Client client) {
        String normalizedPhone = Formatters.cleanPhone(client.getPhone());
        String normalizedCarNumber = Formatters.normalizeCarNumber(client.getCarNumber());
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
    
    /**
     * Удалить клиента.
     * @param client удаляемый клиент
     */
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

    // ==================== CAR CRUD ====================

    /**
     * Получить автомобили клиента.
     * @param clientId идентификатор клиента
     * @return список автомобилей; пустой список, если автомобилей нет
     */
    @Override
    public List<Car> getCarsByClientId(int clientId) {
        try (Connection conn = getConnection()) {
            return loadCarsByClientId(conn, clientId);
        } catch (SQLException e) {
            logger.error("Ошибка загрузки автомобилей клиента", e);
            return new ArrayList<>();
        }
    }

    /**
     * Загружает автомобили клиента через переданное соединение.
     * Используется внутри методов, которые уже держат соединение,
     * чтобы не занимать второе соединение из пула.
     */
    protected List<Car> loadCarsByClientId(Connection conn, int clientId) throws SQLException {
        List<Car> cars = new ArrayList<>();
        String sql = "SELECT id, client_id, car_model, car_number, mileage FROM client_cars WHERE client_id = ? ORDER BY id";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, clientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    cars.add(new Car(
                            rs.getInt("id"),
                            rs.getInt("client_id"),
                            rs.getString("car_model"),
                            rs.getString("car_number"),
                            rs.getInt("mileage")
                    ));
                }
            }
        }
        return cars;
    }

    /**
     * Добавить автомобиль клиента.
     * @param car автомобиль для добавления
     * @throws SQLException при ошибке сохранения
     */
    @Override
    public void addCar(Car car) throws SQLException {
        String normalizedNumber = Formatters.normalizeCarNumber(car.getCarNumber());
        String sql = "INSERT INTO client_cars (client_id, car_model, car_number, mileage) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, car.getClientId());
            pstmt.setString(2, car.getCarModel());
            pstmt.setString(3, normalizedNumber);
            pstmt.setInt(4, car.getMileage());
            pstmt.executeUpdate();

            try {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        car.setId(rs.getInt(1));
                    }
                }
            } catch (SQLException e) {
                logger.debug("getGeneratedKeys не поддерживается, используем fallback для car", e);
                if (isSqlite(conn)) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                        if (rs.next()) {
                            car.setId(rs.getInt(1));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка добавления автомобиля", e);
            throw e;
        }
    }

    /**
     * Обновить данные автомобиля.
     * @param car автомобиль с обновлёнными данными
     */
    @Override
    public void updateCar(Car car) {
        String normalizedNumber = Formatters.normalizeCarNumber(car.getCarNumber());
        String sql = "UPDATE client_cars SET car_model = ?, car_number = ?, mileage = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, car.getCarModel());
            pstmt.setString(2, normalizedNumber);
            pstmt.setInt(3, car.getMileage());
            pstmt.setInt(4, car.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка обновления автомобиля", e);
        }
    }

    /**
     * Удалить автомобиль.
     * @param car удаляемый автомобиль
     */
    @Override
    public void deleteCar(Car car) {
        String sql = "DELETE FROM client_cars WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, car.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка удаления автомобиля", e);
        }
    }
    
    /**
     * Получить клиента по идентификатору.
     * @param id идентификатор клиента
     * @return клиент или {@code null}, если не найден
     */
    @Override
    public Client getClientById(int id) {
        String sql = "SELECT id, name, last_name, phone, car_model, car_number, last_repair_date FROM clients WHERE id = ?";

        try (Connection conn = getConnection()) {
            Client client = null;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        client = new Client(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("last_name"),
                                rs.getString("phone"),
                                rs.getString("car_model"),
                                rs.getString("car_number"),
                                rs.getString("last_repair_date")
                        );
                    }
                }
            }
            if (client != null) {
                client.setCars(loadCarsByClientId(conn, id));
            }
            return client;
        } catch (SQLException e) {
            logger.error("Ошибка получения клиента по ID", e);
        }
        return null;
    }
    
    /**
     * Получить идентификатор клиента по его имени, фамилии и телефону.
     * @param client клиент
     * @return идентификатор или {@code -1}, если клиент не найден
     */
    @Override
    public int getClientId(Client client) {
        try (Connection conn = getConnection()) {
            return findClientId(conn, client);
        } catch (SQLException e) {
            logger.error("Ошибка получения ID клиента", e);
            return -1;
        }
    }

    /**
     * Ищет ID клиента через переданное соединение.
     * Используется внутри транзакций (addOrder/updateOrder/addAppointment),
     * чтобы не запрашивать второе соединение из пула и не блокировать его.
     */
    protected int findClientId(Connection conn, Client client) throws SQLException {
        String normalizedPhone = Formatters.cleanPhone(client.getPhone());
        String normalizedLastName = client.getLastName() != null ? client.getLastName() : "";
        String sql = "SELECT id FROM clients WHERE name = ? AND last_name = ? AND phone = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, client.getName());
            pstmt.setString(2, normalizedLastName);
            pstmt.setString(3, normalizedPhone);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return -1;
    }
    
    // ==================== SERVICES ====================
    
    /**
     * Добавить услугу; при совпадении названия существующая запись заменяется.
     * @param service услуга для добавления; после вызова получает сгенерированный ID
     */
    @Override
    public void addService(Service service) {
        try (Connection conn = getConnection();
              PreparedStatement pstmt = conn.prepareStatement(addServiceSql(conn), Statement.RETURN_GENERATED_KEYS)) {
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
            int id = getGeneratedKeyId(conn, pstmt);
            if (id > 0) {
                service.setId(id);
            }
        } catch (SQLException e) {
            logger.error("Ошибка добавления услуги", e);
        }
    }

    /**
     * SQL для добавления услуги с заменой записи с тем же названием.
     * SQLite и H2 не поддерживают общий upsert-синтаксис ({@code INSERT OR REPLACE}
     * и {@code MERGE ... KEY(name)} соответственно).
     * @param conn соединение с базой данных
     * @return SQL-запрос для текущего диалекта
     */
    private String addServiceSql(Connection conn) throws SQLException {
        if (isSqlite(conn)) {
            return "INSERT OR REPLACE INTO services (name, price, duration, part_number, category_id, oil_volume, uses_oil, spare_part_name, spare_part_quantity) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }
        if (isH2(conn)) {
            return "MERGE INTO services (name, price, duration, part_number, category_id, oil_volume, uses_oil, spare_part_name, spare_part_quantity) KEY(name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }
        return "INSERT INTO services (name, price, duration, part_number, category_id, oil_volume, uses_oil, spare_part_name, spare_part_quantity) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }
    
    /**
     * Получить все услуги.
     * @return список услуг, отсортированный по названию
     */
    @Override
    public List<Service> getAllServices() {
        List<Service> services = new ArrayList<>();
        String sql = "SELECT id, name, price, duration, part_number, category_id, oil_volume, uses_oil, spare_part_name, spare_part_quantity FROM services ORDER BY name";

        try (Connection conn = getConnection();
              Statement stmt = conn.createStatement();
              ResultSet rs = stmt.executeQuery(sql)) {
            // Безопасно: запрос без параметров, Statement используется корректно
            while (rs.next()) {
                Service service = new Service();
                service.setId(rs.getInt("id"));
                service.setName(rs.getString("name"));
                service.setPrice(rs.getDouble("price"));
                service.setDuration(rs.getInt("duration"));
                service.setPartNumber(rs.getString("part_number"));
                service.setCategoryId(rs.getInt("category_id"));
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
    
    /**
     * Удалить услугу.
     * @param service удаляемая услуга
     */
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
    
    /**
     * Обновить услугу.
     * @param service услуга с обновлёнными данными
     */
    @Override
    public void updateService(Service service) {
        String sql = "UPDATE services SET name = ?, price = ?, duration = ?, part_number = ?, category_id = ?, oil_volume = ?, uses_oil = ?, spare_part_name = ?, spare_part_quantity = ? WHERE id = ?";

        try (Connection conn = getConnection();
              PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, service.getName());
            pstmt.setDouble(2, service.getPrice());
            pstmt.setInt(3, service.getDuration());
            pstmt.setString(4, service.getPartNumber());
            pstmt.setInt(5, service.getCategoryId());
            pstmt.setDouble(6, service.getOilVolume());
            pstmt.setInt(7, service.isUsesOil() ? 1 : 0);
            pstmt.setString(8, service.getSparePartName());
            pstmt.setInt(9, service.getSparePartQuantity());
            pstmt.setInt(10, service.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка обновления услуги", e);
        }
    }
    
    // ==================== SPARE PARTS ====================
    
    /**
     * Получить все запчасти.
     * @return список запчастей, отсортированный по названию
     */
    @Override
    public List<SparePart> getAllSpareParts() {
        List<SparePart> parts = new ArrayList<>();
        String sql = "SELECT id, name, part_number, manufacturer, compatible_models, note, purchase_price, retail_price, stock, min_stock, location, unit_type FROM spare_parts ORDER BY name";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            // Безопасно: запрос без параметров, Statement используется корректно
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
    
    /**
     * Обновить запчасть.
     * @param part запчасть с обновлёнными данными
     */
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
    
    /**
     * Удалить запчасть.
     * @param part удаляемая запчасть
     */
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
    
    /**
     * Получить все заказы вместе с услугами и запчастями.
     * @return список заказов, отсортированный по дате создания (новые первыми)
     */
    @Override
    public List<WorkOrder> getAllOrders() {
        Map<String, WorkOrder> orders = new LinkedHashMap<>();
        if (!loadOrders(orders)) {
            return new ArrayList<>();
        }
        loadOrderServices(orders);
        loadOrderSpareParts(orders);
        return new ArrayList<>(orders.values());
    }

    /**
     * Загрузить заказы и их клиентов.
     * @param orders карта «ID заказа — заказ», которую нужно заполнить
     * @return {@code true}, если заказы удалось загрузить
     */
    private boolean loadOrders(Map<String, WorkOrder> orders) {
        String sql = """
            SELECT o.id as order_id, o.status, o.total, o.created_date, o.mileage,
                    o.closed_date, o.notes,
                    c.id as client_id, c.name, c.last_name, c.phone, 
                    c.car_model, c.car_number, c.last_repair_date
            FROM orders o
            LEFT JOIN clients c ON o.client_id = c.id
            ORDER BY o.created_date DESC
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            // Безопасно: запрос без параметров, Statement используется корректно
            while (rs.next()) {
                orders.put(rs.getString("order_id"), mapOrder(rs));
            }
            return true;
        } catch (SQLException e) {
            logger.error("Ошибка загрузки заказов", e);
            logger.error("Технические детали: {}", ExceptionHandler.getTechnicalDetails(e));
            return false;
        }
    }

    /**
     * Собрать заказ и его клиента из текущей строки выборки.
     * @param rs строка результата с колонками заказа и клиента
     * @return заказ с данными клиента
     * @throws SQLException при ошибке чтения результата
     */
    private WorkOrder mapOrder(ResultSet rs) throws SQLException {
        Client client = new Client(
                rs.getInt("client_id"),
                valueOrDefault(rs.getString("name"), ""),
                valueOrDefault(rs.getString("last_name"), ""),
                valueOrDefault(rs.getString("phone"), ""),
                valueOrDefault(rs.getString("car_model"), ""),
                valueOrDefault(rs.getString("car_number"), ""),
                valueOrDefault(rs.getString("last_repair_date"), "")
        );

        WorkOrder order = new WorkOrder(
                rs.getString("order_id"),
                client,
                rs.getString("status"),
                rs.getDouble("total"),
                rs.getString("created_date")
        );
        order.setMileage(rs.getInt("mileage"));
        order.setClosedDate(valueOrDefault(rs.getString("closed_date"), ""));
        order.setNotes(valueOrDefault(rs.getString("notes"), ""));
        return order;
    }

    /**
     * Дозагрузить услуги в уже собранные заказы.
     * @param orders карта «ID заказа — заказ»
     */
    private void loadOrderServices(Map<String, WorkOrder> orders) {
        String sql = "SELECT order_id, service_name, price, service_id FROM order_services";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            // Безопасно: запрос без параметров, Statement используется корректно
            while (rs.next()) {
                WorkOrder order = orders.get(rs.getString("order_id"));
                if (order != null) {
                    order.addService(rs.getInt("service_id"),
                            rs.getString("service_name"), rs.getDouble("price"));
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка загрузки услуг заказа", e);
        }
    }

    /**
     * Дозагрузить запчасти в уже собранные заказы.
     * @param orders карта «ID заказа — заказ»
     */
    private void loadOrderSpareParts(Map<String, WorkOrder> orders) {
        String sql = "SELECT order_id, part_name, price, quantity, spare_part_id, unit_type, "
                + "purchase_price FROM order_parts";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            // Безопасно: запрос без параметров, Statement используется корректно
            while (rs.next()) {
                WorkOrder order = orders.get(rs.getString("order_id"));
                if (order != null) {
                    SparePart part = new SparePart(
                            rs.getString("part_name"),
                            rs.getDouble("purchase_price"),
                            rs.getDouble("price"),
                            rs.getInt("quantity")
                    );
                    part.setId(rs.getInt("spare_part_id"));
                    part.setUnitType(valueOrDefault(rs.getString("unit_type"), "шт"));
                    order.addSparePart(part, rs.getInt("quantity"));
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка загрузки запчастей заказа", e);
        }
    }

    /**
     * Вернуть значение из базы или значение по умолчанию, если оно {@code null}.
     * @param value значение из результата запроса
     * @param defaultValue значение по умолчанию
     * @return значение или значение по умолчанию
     */
    private static String valueOrDefault(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    /**
     * Удалить заказ.
     * @param orderId идентификатор заказа
     */
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
    
    /**
     * Обновить заказ, его услуги и запчасти.
     * @param order заказ с обновлёнными данными
     */
    @Override
    public void updateOrder(WorkOrder order) {
        String sql = "UPDATE orders SET client_id = ?, status = ?, total = ?, mileage = ?, closed_date = ?, notes = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int clientId = findClientId(conn, order.getClient());
            pstmt.setInt(1, clientId);
            pstmt.setString(2, order.getStatus());
            pstmt.setDouble(3, order.getTotal());
            pstmt.setInt(4, order.getMileage());
            pstmt.setString(5, order.getClosedDate() != null ? order.getClosedDate() : "");
            pstmt.setString(6, order.getNotes() != null ? order.getNotes() : "");
            pstmt.setString(7, order.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка обновления заказа", e);
        }
    }
    
    /**
     * Сохранить запчасти заказа в {@code order_parts}.
     * Используется внутри транзакций {@code addOrder}/{@code updateOrder}.
     * @param conn соединение с базой данных
     * @param orderId идентификатор заказа
     * @param order заказ с запчастями
     * @throws SQLException если вставка не удалась
     */
    protected void saveOrderParts(Connection conn, String orderId, WorkOrder order) throws SQLException {
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
    
    /**
     * Получить все записи на обслуживание.
     * @return список записей, отсортированный по дате и времени
     */
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
            // Безопасно: запрос без параметров, Statement используется корректно
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
    
    /**
     * Получить записи на обслуживание на указанную дату.
     * @param date дата в формате {@code dd/MM/yyyy}
     * @return список записей, отсортированный по времени
     */
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
    
    /**
     * Обновить запись на обслуживание.
     * @param appointment запись с обновлёнными данными
     */
    @Override
    public void updateAppointment(Appointment appointment) {
        String sql = "UPDATE appointments SET client_id = ?, master_name = ?, service_name = ?, service_id = ?, appointment_date = ?, appointment_time = ?, status = ?, order_id = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int clientId = findClientId(conn, appointment.getClient());
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
            logger.error("Ошибка удаления записи", e);
        }
    }

    /**
     * Получить запись на обслуживание по идентификатору.
     * @param id идентификатор записи
     * @return запись или {@code null}, если не найдена
     */
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

    /**
     * Получить связи услуги с запчастями.
     * @param serviceId идентификатор услуги; {@code -1} — вернуть все связи
     * @return список связей
     */
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

    /**
     * Добавить связь услуги с запчастью.
     * @param relation связь для добавления; после вызова получает сгенерированный ID
     */
    @Override
    public void addServiceSparePart(com.autoservice.model.ServiceSparePart relation) {
        String sql = "INSERT INTO service_spare_parts (service_id, spare_part_id, quantity, unit_type, active) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, relation.getServiceId());
            pstmt.setInt(2, relation.getSparePartId());
            pstmt.setInt(3, relation.getQuantity());
            pstmt.setString(4, relation.getUnitType());
            pstmt.setInt(5, relation.isActive() ? 1 : 0);
            pstmt.executeUpdate();
            
            // Универсальный способ: getGeneratedKeys работает и в SQLite, и в H2,
            // last_insert_rowid() вызывается только как SQLite-fallback.
            int id = getGeneratedKeyId(conn, pstmt);
            if (id > 0) {
                relation.setId(id);
            }
        } catch (SQLException e) {
            logger.error("Ошибка добавления связи услуги-запчасти", e);
        }
    }

    /**
     * Удалить связь услуги с запчастью.
     * @param relation удаляемая связь
     */
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

    /**
     * Удалить все связи услуги с запчастями.
     *
     * <p>Вызов с {@code serviceId == -1} блокируется для защиты от массового удаления.
     * @param serviceId идентификатор услуги
     */
    @Override
    public void deleteServiceSparePartsByServiceId(int serviceId) {
        String sql;
        
        if (serviceId == -1) {
            // Блокировка массового удаления всех связей для безопасности
            logger.error("Попытка массового удаления всех связей услуги-запчасти (serviceId=-1) - заблокировано");
            return;
        } else {
            sql = "DELETE FROM service_spare_parts WHERE service_id = ?";
        }

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, serviceId);
            int deletedCount = pstmt.executeUpdate();
            logger.info("Удалено {} связей услуги-запчасти для serviceId={}", deletedCount, serviceId);
        } catch (SQLException e) {
            logger.error("Ошибка удаления связей услуги-запчастей по ID услуги", e);
        }
    }

    /**
     * Удалить дубликаты связей услуги с запчастями, оставив по одной связи на пару «услуга — запчасть».
     */
    @Override
    public void deleteDuplicateServiceSpareParts() {
        // Удаляет дубликаты связей для одной услуги-запчасти, оставляя только одну
        String sql = "DELETE FROM service_spare_parts WHERE id NOT IN (" +
                "SELECT MIN(id) FROM service_spare_parts GROUP BY service_id, spare_part_id)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int deletedCount = pstmt.executeUpdate();
            logger.info("Удалено дубликатов связей услуги-запчасти: {}", deletedCount);
        } catch (SQLException e) {
            logger.error("Ошибка удаления дубликатов связей услуги-запчасти", e);
        }
    }

    // ==================== SERVICE-PART RELATIONSHIPS (NEW STRUCTURE) ====================

    /**
     * Получить все связи услуг с запчастями (новая структура).
     * @return список связей
     */
    @Override
    public List<com.autoservice.model.ServicePart> getAllServiceParts() {
        List<com.autoservice.model.ServicePart> parts = new ArrayList<>();
        String sql = "SELECT id, service_id, spare_part_id, quantity, is_required, created_date FROM service_parts";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            // Безопасно: запрос без параметров, Statement используется корректно
            while (rs.next()) {
                com.autoservice.model.ServicePart part = new com.autoservice.model.ServicePart();
                part.setId(rs.getInt("id"));
                part.setServiceId(rs.getInt("service_id"));
                part.setSparePartId(rs.getInt("spare_part_id"));
                part.setQuantity(rs.getDouble("quantity"));
                part.setRequired(rs.getInt("is_required") == 1);
                part.setCreatedDate(rs.getString("created_date"));
                parts.add(part);
            }
            rs.close();
        } catch (SQLException e) {
            logger.error("Ошибка загрузки связей услуг-запчастей (service_parts)", e);
        }
        return parts;
    }

    /**
     * Добавить связь услуги с запчастью (новая структура).
     * @param part связь для добавления; после вызова получает сгенерированный ID
     */
    @Override
    public void addServicePart(com.autoservice.model.ServicePart part) {
        String sql = "INSERT INTO service_parts (service_id, spare_part_id, quantity, is_required, created_date) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
              PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, part.getServiceId());
            pstmt.setInt(2, part.getSparePartId());
            pstmt.setDouble(3, part.getQuantity());
            pstmt.setInt(4, part.isRequired() ? 1 : 0);
            pstmt.setString(5, part.getCreatedDate());
            pstmt.executeUpdate();
            
            // Получаем сгенерированный id
            try {
                int id = getGeneratedKeyId(conn, pstmt);
                if (id > 0) {
                    part.setId(id);
                }
            } catch (SQLException e) {
                // Fallback только для SQLite: H2 не знает last_insert_rowid().
                if (isSqlite(conn)) {
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                        if (rs.next()) {
                            part.setId(rs.getInt(1));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка добавления связи услуги-запчасти (service_parts)", e);
        }
    }

    /**
     * Обновить связь услуги с запчастью (новая структура).
     * @param part связь с обновлёнными данными
     */
    @Override
    public void updateServicePart(com.autoservice.model.ServicePart part) {
        String sql = "UPDATE service_parts SET service_id = ?, spare_part_id = ?, quantity = ?, is_required = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, part.getServiceId());
            pstmt.setInt(2, part.getSparePartId());
            pstmt.setDouble(3, part.getQuantity());
            pstmt.setInt(4, part.isRequired() ? 1 : 0);
            pstmt.setInt(5, part.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка обновления связи услуги-запчасти (service_parts)", e);
        }
    }

    /**
     * Удалить связь услуги с запчастью (новая структура).
     * @param part удаляемая связь
     */
    @Override
    public void deleteServicePart(com.autoservice.model.ServicePart part) {
        String sql = "DELETE FROM service_parts WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, part.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Ошибка удаления связи услуги-запчасти (service_parts)", e);
        }
    }

    /**
     * Удалить все связи услуги с запчастями (новая структура).
     *
     * <p>Вызов с {@code serviceId == -1} блокируется для защиты от массового удаления.
     * @param serviceId идентификатор услуги
     */
    @Override
    public void deleteServicePartsByServiceId(int serviceId) {
        String sql;
        
        if (serviceId == -1) {
            // Блокировка массового удаления всех связей для безопасности
            logger.error("Попытка массового удаления всех связей услуги-запчасти (serviceId=-1) - заблокировано");
            return;
        } else {
            sql = "DELETE FROM service_parts WHERE service_id = ?";
        }

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, serviceId);
            int deletedCount = pstmt.executeUpdate();
            logger.info("Удалено {} связей услуги-запчасти для serviceId={}", deletedCount, serviceId);
        } catch (SQLException e) {
            logger.error("Ошибка удаления связей услуги-запчасти по ID услуги (service_parts)", e);
        }
    }

    /**
     * Удалить все связи запчасти с услугами (новая структура).
     *
     * <p>Вызов с {@code sparePartId == -1} блокируется для защиты от массового удаления.
     * @param sparePartId идентификатор запчасти
     */
    @Override
    public void deleteServicePartsBySparePartId(int sparePartId) {
        String sql;
        
        if (sparePartId == -1) {
            // Блокировка массового удаления всех связей для безопасности
            logger.error("Попытка массового удаления всех связей услуги-запчасти (sparePartId=-1) - заблокировано");
            return;
        } else {
            sql = "DELETE FROM service_parts WHERE spare_part_id = ?";
        }

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sparePartId);
            int deletedCount = pstmt.executeUpdate();
            logger.info("Удалено {} связей услуги-запчасти для sparePartId={}", deletedCount, sparePartId);
        } catch (SQLException e) {
            logger.error("Ошибка удаления связей услуги-запчасти по ID запчасти (service_parts)", e);
        }
    }

    // ==================== SERVICE-SPARE PARTS LISTS (NEW STRUCTURE) ====================

    /**
     * Получить списки запчастей услуги.
     * @param serviceId идентификатор услуги; {@code -1} — вернуть все списки
     * @return список списков, отсортированный по дате создания (новые первыми)
     */
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

    /**
     * Получить элементы списка запчастей.
     * @param listId идентификатор списка
     * @return список элементов
     */
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

    /**
     * Добавить список запчастей вместе с его элементами.
     * @param list список для добавления; после вызова получает сгенерированный ID
     */
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

    /**
     * Добавить элемент списка запчастей.
     * @param item элемент для добавления; после вызова получает сгенерированный ID
     */
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

    /**
     * Удалить список запчастей вместе с его элементами.
     * @param list удаляемый список
     */
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

    /**
     * Удалить все списки запчастей услуги вместе с их элементами.
     *
     * <p>Вызов с {@code serviceId == -1} блокируется для защиты от массового удаления.
     * @param serviceId идентификатор услуги
     */
    @Override
    public void deleteServiceSparePartsListsByServiceId(int serviceId) {
        // Блокировка массового удаления для безопасности
        if (serviceId == -1) {
            logger.error("Попытка массового удаления всех списков услуг-запчастей (serviceId=-1) - заблокировано");
            return;
        }
        
        // Сначала получаем все списки для удаления их элементов
        List<com.autoservice.model.ServiceSparePartsList> lists = getServiceSparePartsListsByServiceId(serviceId);
        for (com.autoservice.model.ServiceSparePartsList list : lists) {
            deleteServiceSparePartsListItemsByListId(list.getId());
        }
        
        // Затем удаляем сами списки
        String sql = "DELETE FROM service_spare_parts_lists WHERE service_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, serviceId);
            int deletedCount = pstmt.executeUpdate();
            logger.info("Удалено {} списков услуг-запчастей для serviceId={}", deletedCount, serviceId);
        } catch (SQLException e) {
            logger.error("Ошибка удаления списков услуг-запчастей по ID услуги", e);
        }
    }

    /**
     * Удалить все элементы списка запчастей.
     * @param listId идентификатор списка
     */
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

    /**
     * Получить расходники TO для модели автомобиля.
     * @param carModel модель автомобиля; {@code null} или пустая строка — вернуть все расходники
     * @return список расходников
     */
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

    /**
     * Получить все модели автомобилей, для которых заданы расходники TO.
     * @return список моделей без дубликатов
     */
    @Override
    public List<String> getAllCarModels() {
        List<String> carModels = new ArrayList<>();
        String sql = "SELECT DISTINCT car_model FROM to_parts WHERE car_model IS NOT NULL AND car_model != '' ORDER BY car_model";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            // Безопасно: запрос без параметров, Statement используется корректно
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

    /**
     * Добавить расходник TO.
     * @param part расходник для добавления; после вызова получает сгенерированный ID
     */
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

    /**
     * Обновить расходник TO.
     * @param part расходник с обновлёнными данными
     */
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

    /**
     * Удалить расходник TO.
     * @param part удаляемый расходник
     */
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

    /**
     * Удалить расходники TO для модели автомобиля.
     * @param carModel модель автомобиля; {@code null} или пустая строка — удалить все расходники
     */
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

    /**
     * Получить все настройки приложения.
     * @return список настроек
     */
    @Override
    public List<com.autoservice.model.Setting> getAllSettings() {
        List<com.autoservice.model.Setting> settings = new ArrayList<>();
        String sql = "SELECT id, setting_key, setting_value, description FROM app_settings";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            // Безопасно: запрос без параметров, Statement используется корректно
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

    /**
     * Добавить настройку.
     * @param setting настройка для добавления; после вызова получает сгенерированный ID
     */
    @Override
    public void addSetting(com.autoservice.model.Setting setting) {
        String sql = "INSERT INTO app_settings (setting_key, setting_value, description) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, setting.getKey());
            pstmt.setString(2, setting.getValue());
            pstmt.setString(3, setting.getDescription());
            pstmt.executeUpdate();
            
            // Получаем сгенерированный id
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                setting.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            logger.error("Ошибка добавления настройки", e);
        }
    }

    /**
     * Обновить значение настройки.
     * @param setting настройка с новым значением
     */
    @Override
    public void updateSetting(com.autoservice.model.Setting setting) {
        String sql = "UPDATE app_settings SET setting_value = ? WHERE id = ?";
        logger.info("AbstractDatabase.updateSetting: id={}, value={}, sql={}", setting.getId(), setting.getValue(), sql);

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, setting.getValue());
            pstmt.setInt(2, setting.getId());
            int rows = pstmt.executeUpdate();
            logger.info("  UPDATE completed, rows affected={}", rows);
            if (rows == 0) {
                logger.error("  WARNING: No rows updated! id={} may not exist in DB", setting.getId());
            }
        } catch (SQLException e) {
            logger.error("Ошибка обновления настройки: id={}, value={}", setting.getId(), setting.getValue(), e);
        }
    }

    /**
     * Удалить настройку.
     * @param setting удаляемая настройка
     */
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

    /**
     * Получить настройку по ключу.
     * @param key ключ настройки
     * @return настройка или {@code null}, если не найдена
     */
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
