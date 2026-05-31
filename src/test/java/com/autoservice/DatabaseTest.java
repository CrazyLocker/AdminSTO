package com.autoservice;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Интеграционные тесты для класса Database
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseTest {

    @TempDir
    static File tempDir;

    private static final String TEST_DB_PATH = "jdbc:sqlite:test_integration.db";

    @BeforeAll
    static void setup() {
        Database.initForTest();
        clearDatabase();
    }

    @AfterAll
    static void cleanup() {
        Database.close();
        // Удаляем файл тестовой базы
        new File("test_integration.db").delete();
    }

    private static void clearDatabase() {
        try {
            Connection conn = Database.getConnection();
            if (conn != null) {
                var stmt = conn.createStatement();
                stmt.execute("DELETE FROM order_parts");
                stmt.execute("DELETE FROM order_services");
                stmt.execute("DELETE FROM appointments");
                stmt.execute("DELETE FROM orders");
                stmt.execute("DELETE FROM spare_parts");
                stmt.execute("DELETE FROM services");
                stmt.execute("DELETE FROM clients");
                stmt.execute("DELETE FROM sqlite_sequence");
                stmt.close();
            }
        } catch (SQLException e) {
            System.err.println("Очистка БД: " + e.getMessage());
        }
    }

    // ==================== КЛИЕНТЫ ====================

    @Test
    @Order(1)
    void testAddAndLoadClient() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        Database.addClient(client);

        List<Client> clients = Database.getAllClients();
        assertThat(clients).isNotEmpty();

        Client loaded = clients.stream()
                .filter(c -> c.getName().equals("Иван"))
                .findFirst()
                .orElse(null);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getLastName()).isEqualTo("Петров");
        assertThat(loaded.getPhone()).isEqualTo("+79001234567");
        assertThat(loaded.getCarModel()).isEqualTo("Haval Jolion");
        assertThat(loaded.getCarNumber()).isEqualTo("А123ВС163");
    }

    @Test
    @Order(2)
    void testUpdateClient() {
        Client client = new Client("Петр", "Сидоров", "+79112223333", "Haval F7", "В456СЕ163");
        Database.addClient(client);

        List<Client> clients = Database.getAllClients();
        Client toUpdate = clients.stream()
                .filter(c -> c.getName().equals("Петр"))
                .findFirst()
                .orElse(null);

        assertThat(toUpdate).isNotNull();

        toUpdate.setName("Петр Иванович");
        toUpdate.setPhone("+79223334444");
        Database.updateClient(toUpdate);

        List<Client> updated = Database.getAllClients();
        Client loaded = updated.stream()
                .filter(c -> c.getId() == toUpdate.getId())
                .findFirst()
                .orElse(null);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getName()).isEqualTo("Петр Иванович");
        assertThat(loaded.getPhone()).isEqualTo("+79223334444");
    }

    @Test
    @Order(3)
    void testDeleteClient() {
        Client client = new Client("Анна", "Иванова", "+79334445555", "Haval F5", "С789ЕЕ163");
        Database.addClient(client);

        List<Client> before = Database.getAllClients();
        Client toDelete = before.stream()
                .filter(c -> c.getName().equals("Анна"))
                .findFirst()
                .orElse(null);

        assertThat(toDelete).isNotNull();

        Database.deleteClient(toDelete);

        List<Client> after = Database.getAllClients();
        assertThat(after).noneMatch(c -> c.getId() == toDelete.getId());
    }

    @Test
    @Order(4)
    void testGetClientById() {
        Client client = new Client("Ольга", "Смирнова", "+79445556666", "Haval Dargo", "М123НО163");
        Database.addClient(client);

        List<Client> clients = Database.getAllClients();
        Client toFind = clients.stream()
                .filter(c -> c.getName().equals("Ольга"))
                .findFirst()
                .orElse(null);

        assertThat(toFind).isNotNull();

        Client loaded = Database.getClientById(toFind.getId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getName()).isEqualTo("Ольга");
    }

    @Test
    @Order(5)
    void testGetClientId() {
        Client client = new Client("Елена", "Кузнецова", "+79556667777", "Haval Big Dog", "Е456КХ163");
        Database.addClient(client);

        List<Client> clients = Database.getAllClients();
        Client toFind = clients.stream()
                .filter(c -> c.getName().equals("Елена"))
                .findFirst()
                .orElse(null);

        assertThat(toFind).isNotNull();

        int clientId = Database.getClientId(toFind);
        assertThat(clientId).isGreaterThan(0);
    }

    // ==================== УСЛУГИ ====================

    @Test
    @Order(6)
    void testAddAndLoadService() {
        Service service = new Service("Замена масла", 1500);
        Database.addService(service);

        List<Service> services = Database.getAllServices();
        assertThat(services).anyMatch(s -> s.getName().equals("Замена масла"));
    }

    @Test
    @Order(7)
    void testDeleteService() {
        Service service = new Service("Замена тормозных колодок", 2000);
        Database.addService(service);

        Database.deleteService(service);

        List<Service> services = Database.getAllServices();
        assertThat(services).noneMatch(s -> s.getName().equals("Замена тормозных колодок"));
    }

    // ==================== ЗАПЧАСТИ ====================

    @Test
    @Order(8)
    void testAddAndLoadSparePart() {
        SparePart part = new SparePart(
            -1, 0, "Моторное масло 5W-30", "ML-5W30",
            "Shell", "Haval Jolion, Haval F7",
            800, 1200, 10, 3, "Склад А-1"
        );
        Database.addSparePart(part);

        List<SparePart> parts = Database.getAllSpareParts();
        SparePart loaded = parts.stream()
                .filter(p -> p.getName().equals("Моторное масло 5W-30"))
                .findFirst()
                .orElse(null);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getPartNumber()).isEqualTo("ML-5W30");
        assertThat(loaded.getManufacturer()).isEqualTo("Shell");
        assertThat(loaded.getStock()).isEqualTo(10);
    }

    @Test
    @Order(9)
    void testUpdateSparePartStock() {
        SparePart part = new SparePart("Фильтр воздушный", 200, 400, 15);
        Database.addSparePart(part);

        List<SparePart> parts = Database.getAllSpareParts();
        SparePart toUpdate = parts.stream()
                .filter(p -> p.getName().equals("Фильтр воздушный"))
                .findFirst()
                .orElse(null);

        assertThat(toUpdate).isNotNull();

        Database.updateSparePartStock(toUpdate, 25);

        SparePart loaded = Database.getAllSpareParts().stream()
                .filter(p -> p.getId() == toUpdate.getId())
                .findFirst()
                .orElse(null);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getStock()).isEqualTo(25);
    }

    @Test
    @Order(10)
    void testDeleteSparePart() {
        SparePart part = new SparePart("Свеча зажигания", 500, 900, 30);
        Database.addSparePart(part);

        List<SparePart> parts = Database.getAllSpareParts();
        SparePart toDelete = parts.stream()
                .filter(p -> p.getName().equals("Свеча зажигания"))
                .findFirst()
                .orElse(null);

        assertThat(toDelete).isNotNull();

        Database.deleteSparePart(toDelete);

        assertThat(Database.getAllSpareParts()).noneMatch(p -> p.getName().equals("Свеча зажигания"));
    }

    // ==================== ЗАКАЗЫ ====================

    @Test
    @Order(11)
    void testAddAndLoadOrder() {
        Client client = new Client("Дмитрий", "Морозов", "+79667778888", "Haval Jolion", "А999ВВ163");
        Database.addClient(client);
        Database.getAllClients();

        Service service = new Service("Компьютерная диагностика", 1000);
        Database.addService(service);

        WorkOrder order = new WorkOrder(client);
        order.addService("Компьютерная диагностика", 1000);

        Database.addOrder(order);

        List<WorkOrder> orders = Database.getAllOrders();
        assertThat(orders).isNotEmpty();

        WorkOrder lastOrder = orders.get(0);
        assertThat(lastOrder.getId()).startsWith("ZAK-");
        assertThat(lastOrder.getClient()).isEqualTo(client);
        assertThat(lastOrder.getTotal()).isEqualTo(1000);
        assertThat(lastOrder.getServices()).contains("Компьютерная диагностика");
    }

    @Test
    @Order(12)
    void testUpdateOrder() {
        Client client = new Client("Сергей", "Волков", "+79778889999", "Haval F7", "В888СС163");
        Database.addClient(client);
        Database.getAllClients();

        WorkOrder order = new WorkOrder(client);
        order.addService("Замена масла", 1500);
        Database.addOrder(order);

        List<WorkOrder> orders = Database.getAllOrders();
        WorkOrder toUpdate = orders.get(0);

        toUpdate.setStatus(WorkOrder.STATUS_IN_WORK);
        Database.updateOrder(toUpdate);

        List<WorkOrder> updated = Database.getAllOrders();
        WorkOrder loaded = updated.stream()
                .filter(o -> o.getId().equals(toUpdate.getId()))
                .findFirst()
                .orElse(null);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getStatus()).isEqualTo(WorkOrder.STATUS_IN_WORK);
    }

    @Test
    @Order(13)
    void testDeleteOrder() {
        Client client = new Client("Алексей", "Новиков", "+79889990000", "Haval F5", "С777ММ163");
        Database.addClient(client);
        Database.getAllClients();

        WorkOrder order = new WorkOrder(client);
        order.addService("Диагностика", 500);
        Database.addOrder(order);

        List<WorkOrder> before = Database.getAllOrders();
        WorkOrder toDelete = before.get(0);

        String deletedId = toDelete.getId();
        Database.deleteOrder(deletedId);

        List<WorkOrder> after = Database.getAllOrders();
        assertThat(after).noneMatch(o -> o.getId().equals(deletedId));
    }

    // ==================== ЗАПИСИ ====================

    @Test
    @Order(14)
    void testAddAndLoadAppointment() {
        Client client = new Client("Максим", "Соколов", "+79990001111", "Haval Dargo", "М555НН163");
        Database.addClient(client);
        Database.getAllClients();

        Appointment appointment = new Appointment(
            client, "Иван", "Замена масла",
            "2024-03-15", "10:00"
        );
        Database.addAppointment(appointment);

        List<Appointment> appointments = Database.getAllAppointments();
        assertThat(appointments).isNotEmpty();

        Appointment loaded = appointments.stream()
                .filter(a -> a.getDate().equals("2024-03-15"))
                .findFirst()
                .orElse(null);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getMasterName()).isEqualTo("Иван");
        assertThat(loaded.getTime()).isEqualTo("10:00");
    }

    @Test
    @Order(15)
    void testUpdateAppointment() {
        Client client = new Client("Наталья", "Лебедева", "+79001112222", "Haval Big Dog", "Е333РР163");
        Database.addClient(client);
        Database.getAllClients();

        Appointment appointment = new Appointment(
            client, "Петр", "Диагностика",
            "2024-03-20", "14:00"
        );
        Database.addAppointment(appointment);

        List<Appointment> appointments = Database.getAllAppointments();
        Appointment toUpdate = appointments.get(0);

        toUpdate.setStatus(Appointment.STATUS_COMPLETED);
        toUpdate.setTime("15:00");
        Database.updateAppointment(toUpdate);

        List<Appointment> updated = Database.getAllAppointments();
        Appointment loaded = updated.stream()
                .filter(a -> a.getId() == toUpdate.getId())
                .findFirst()
                .orElse(null);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getStatus()).isEqualTo(Appointment.STATUS_COMPLETED);
        assertThat(loaded.getTime()).isEqualTo("15:00");
    }

    @Test
    @Order(16)
    void testDeleteAppointment() {
        Client client = new Client("Татьяна", "Попова", "+79112223333", "Haval Jolion", "А222СС163");
        Database.addClient(client);
        Database.getAllClients();

        Appointment appointment = new Appointment(
            client, "Сергей", "Замена фильтров",
            "2024-03-25", "11:00"
        );
        Database.addAppointment(appointment);

        List<Appointment> before = Database.getAllAppointments();
        Appointment toDelete = before.get(0);

        Database.deleteAppointment(toDelete.getId());

        List<Appointment> after = Database.getAllAppointments();
        assertThat(after).noneMatch(a -> a.getId() == toDelete.getId());
    }

    @Test
    @Order(17)
    void testGetAppointmentsByDate() {
        Client client = new Client("Виктор", "Зайцев", "+79223334444", "Haval F7", "В111ЕЕ163");
        Database.addClient(client);
        Database.getAllClients();

        Appointment appointment1 = new Appointment(
            client, "Иван", "Замена масла",
            "2024-04-01", "09:00"
        );
        Appointment appointment2 = new Appointment(
            client, "Петр", "Диагностика",
            "2024-04-01", "11:00"
        );
        Database.addAppointment(appointment1);
        Database.addAppointment(appointment2);

        List<Appointment> appointments = Database.getAppointmentsByDate("2024-04-01");
        assertThat(appointments).hasSize(2);
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ ====================

    @Test
    @Order(18)
    void testGetConnection() {
        Connection conn = Database.getConnection();
        assertThat(conn).isNotNull();
    }

    @Test
    @Order(19)
    void testClientIdNotFound() {
        Client client = new Client("Не существующий", "+79000000000", "Test", "X000XX000");
        int id = Database.getClientId(client);
        assertThat(id).isEqualTo(-1);
    }

    @Test
    @Order(20)
    void testGetClientByIdNotFound() {
        Client client = Database.getClientById(99999);
        assertThat(client).isNull();
    }
}
