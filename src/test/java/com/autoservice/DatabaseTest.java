package com.autoservice;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;

/**
 * Тесты для слоя доступа к БД (DatabaseFactory).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.INTEGRATION)
class DatabaseTest extends BaseTest {

    @Test
    @Order(1)
    void testCreateTables() {
        DatabaseFactory.initForTest();
        assertThat(true).isTrue();
    }

    @Test
    @Order(2)
    void testAddClient() {
        clearDatabase();

        Client client = new Client("Иван", "Петров", "+79001234567", "Toyota Camry", "А123ВС163");
        DatabaseFactory.getDatabase().addClient(client);

        List<Client> clients = DatabaseFactory.getDatabase().getAllClients();
        assertThat(clients).hasSize(1);
        assertThat(clients.get(0).getName()).isEqualTo("Иван");
        assertThat(clients.get(0).getLastName()).isEqualTo("Петров");
    }

    @Test
    @Order(3)
    void testAddMultipleClients() {
        clearDatabase();

        DatabaseFactory.getDatabase().addClient(new Client("Анна", "Смирнова", "+79002223333", "Kia Rio", "В456СЕ163"));
        DatabaseFactory.getDatabase().addClient(new Client("Сергей", "Васильев", "+79003334444", "Ford Focus", "Е789КХ163"));

        List<Client> clients = DatabaseFactory.getDatabase().getAllClients();
        assertThat(clients).hasSize(2);
    }

    @Test
    @Order(4)
    void testGetClientById() {
        clearDatabase();

        Client client = new Client("Дмитрий", "Соколов", "+79005556666", "Haval F7", "М123НО163");
        DatabaseFactory.getDatabase().addClient(client);

        Client saved = DatabaseFactory.getDatabase().getAllClients().get(0);
        Client retrieved = DatabaseFactory.getDatabase().getClientById(saved.getId());

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getName()).isEqualTo("Дмитрий");
    }

    @Test
    @Order(5)
    void testUpdateClient() {
        clearDatabase();

        Client client = new Client("Олег", "Иванов", "+79001112233", "Haval Jolion", "А111ВС163");
        DatabaseFactory.getDatabase().addClient(client);

        Client saved = DatabaseFactory.getDatabase().getAllClients().get(0);
        saved.setPhone("+79998887766");
        DatabaseFactory.getDatabase().updateClient(saved);

        Client updated = DatabaseFactory.getDatabase().getClientById(saved.getId());
        assertThat(updated.getPhone()).isEqualTo("+79998887766");
    }

    @Test
    @Order(6)
    void testDeleteClient() {
        clearDatabase();

        Client client = new Client("Мария", "Кузнецова", "+79112223344", "Mercedes E-Class", "К111ОО163");
        DatabaseFactory.getDatabase().addClient(client);

        assertThat(DatabaseFactory.getDatabase().getAllClients()).hasSize(1);

        DatabaseFactory.getDatabase().deleteClient(DatabaseFactory.getDatabase().getAllClients().get(0));
        assertThat(DatabaseFactory.getDatabase().getAllClients()).isEmpty();
    }

    @Test
    @Order(7)
    void testAddService() {
        clearDatabase();

        Service service = new Service("Замена масла", 1500);
        DatabaseFactory.getDatabase().addService(service);

        List<Service> services = DatabaseFactory.getDatabase().getAllServices();
        assertThat(services).hasSize(1);
        assertThat(services.get(0).getName()).isEqualTo("Замена масла");
        assertThat(services.get(0).getPrice()).isEqualTo(1500);
    }

    @Test
    @Order(8)
    void testAddMultipleServices() {
        clearDatabase();

        DatabaseFactory.getDatabase().addService(new Service("Диагностика", 1000));
        DatabaseFactory.getDatabase().addService(new Service("Замена масла", 1500));
        DatabaseFactory.getDatabase().addService(new Service("Замена фильтров", 500));

        List<Service> services = DatabaseFactory.getDatabase().getAllServices();
        assertThat(services).hasSize(3);
    }

    @Test
    @Order(9)
    void testDeleteService() {
        clearDatabase();

        DatabaseFactory.getDatabase().addService(new Service("Замена масла", 1500));
        assertThat(DatabaseFactory.getDatabase().getAllServices()).hasSize(1);

        DatabaseFactory.getDatabase().deleteService(DatabaseFactory.getDatabase().getAllServices().get(0));
        assertThat(DatabaseFactory.getDatabase().getAllServices()).isEmpty();
    }

    @Test
    @Order(10)
    void testAddSparePart() {
        clearDatabase();

        SparePart part = new SparePart("Масло моторное", 800, 1200, 20);
        DatabaseFactory.getDatabase().addSparePart(part);

        List<SparePart> parts = DatabaseFactory.getDatabase().getAllSpareParts();
        assertThat(parts).hasSize(1);
        assertThat(parts.get(0).getName()).isEqualTo("Масло моторное");
        assertThat(parts.get(0).getRetailPrice()).isEqualTo(1200);
    }

    @Test
    @Order(11)
    void testAddMultipleSpareParts() {
        clearDatabase();

        DatabaseFactory.getDatabase().addSparePart(new SparePart("Масло моторное", 800, 1200, 20));
        DatabaseFactory.getDatabase().addSparePart(new SparePart("Фильтр масляный", 300, 500, 15));

        List<SparePart> parts = DatabaseFactory.getDatabase().getAllSpareParts();
        assertThat(parts).hasSize(2);
    }

    @Test
    @Order(12)
    void testUpdateSparePartStock() {
        clearDatabase();

        SparePart part = new SparePart("Масло моторное", 800, 1200, 20);
        DatabaseFactory.getDatabase().addSparePart(part);

        DatabaseFactory.getDatabase().updateSparePartStock(part, 15);
        List<SparePart> parts = DatabaseFactory.getDatabase().getAllSpareParts();
        assertThat(parts.get(0).getStock()).isEqualTo(15);
    }

    @Test
    @Order(13)
    void testAddOrderWithServices() {
        clearDatabase();

        Client client = new Client("Алексей", "Новиков", "+79009990000", "BMW X5", "О789УУ163");
        DatabaseFactory.getDatabase().addClient(client);

        WorkOrder order = new WorkOrder(client);
        order.addService("Диагностика", 1000);
        order.addService("Замена масла", 1500);
        DatabaseFactory.getDatabase().addOrder(order);

        List<WorkOrder> orders = DatabaseFactory.getDatabase().getAllOrders();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getServices()).hasSize(2);
        assertThat(orders.get(0).getTotal()).isEqualTo(2500);
    }

    @Test
    @Order(14)
    void testAddOrderWithSpareParts() {
        clearDatabase();

        Client client = new Client("Елена", "Морозова", "+79007778888", "Toyota RAV4", "С456ТЕ163");
        DatabaseFactory.getDatabase().addClient(client);

        SparePart oil = new SparePart("Масло моторное", 800, 1200, 20);
        SparePart filter = new SparePart("Фильтр масляный", 300, 500, 15);

        WorkOrder order = new WorkOrder(client);
        order.addSparePart(oil, 1);
        order.addSparePart(filter, 1);
        DatabaseFactory.getDatabase().addOrder(order);

        List<WorkOrder> orders = DatabaseFactory.getDatabase().getAllOrders();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getSpareParts()).hasSize(2);
    }

    @Test
    @Order(15)
    void testUpdateOrderStatus() {
        clearDatabase();

        Client client = new Client("Петр", "Сидоров", "+79112223333", "Haval F7", "В456СЕ163");
        DatabaseFactory.getDatabase().addClient(client);

        WorkOrder order = new WorkOrder(client);
        order.addService("Диагностика", 1000);
        DatabaseFactory.getDatabase().addOrder(order);

        WorkOrder saved = DatabaseFactory.getDatabase().getAllOrders().get(0);
        saved.setStatus(WorkOrder.STATUS_IN_PROGRESS);
        DatabaseFactory.getDatabase().updateOrder(saved);

        WorkOrder updated = DatabaseFactory.getDatabase().getAllOrders().get(0);
        assertThat(updated.getStatus()).isEqualTo(WorkOrder.STATUS_IN_PROGRESS);
    }

    @Test
    @Order(16)
    void testDeleteOrder() {
        clearDatabase();

        Client client = new Client("Ольга", "Попов", "+79223334455", "Lexus RX", "Н222СС163");
        DatabaseFactory.getDatabase().addClient(client);

        WorkOrder order = new WorkOrder(client);
        order.addService("Диагностика", 1000);
        DatabaseFactory.getDatabase().addOrder(order);

        assertThat(DatabaseFactory.getDatabase().getAllOrders()).hasSize(1);

        DatabaseFactory.getDatabase().deleteOrder(DatabaseFactory.getDatabase().getAllOrders().get(0).getId());
        assertThat(DatabaseFactory.getDatabase().getAllOrders()).isEmpty();
    }

    @Test
    @Order(17)
    void testAddAppointment() {
        clearDatabase();

        Client client = new Client("Николай", "Соловьев", "+79556667788", "Mazda CX-5", "Т555НО163");
        DatabaseFactory.getDatabase().addClient(client);

        Appointment appointment = new Appointment(
            client, "Иван", "Замена масла",
            "2024-06-01", "10:00"
        );
        DatabaseFactory.getDatabase().addAppointment(appointment);

        List<Appointment> appointments = DatabaseFactory.getDatabase().getAllAppointments();
        assertThat(appointments).hasSize(1);
        assertThat(appointments.get(0).getMasterName()).isEqualTo("Иван");
    }

    @Test
    @Order(18)
    void testAddMultipleAppointments() {
        clearDatabase();

        Client client = new Client("Татьяна", "Лебедева", "+79990001111", "Haval Big Dog", "Е333РР163");
        DatabaseFactory.getDatabase().addClient(client);

        DatabaseFactory.getDatabase().addAppointment(new Appointment(client, "Иван", "Замена масла", "2024-05-15", "09:00"));
        DatabaseFactory.getDatabase().addAppointment(new Appointment(client, "Петр", "Диагностика", "2024-05-15", "11:00"));

        List<Appointment> appointments = DatabaseFactory.getDatabase().getAllAppointments();
        assertThat(appointments).hasSize(2);
    }

    @Test
    @Order(19)
    void testGetAppointmentsByDate() {
        clearDatabase();

        Client client = new Client("Максим", "Тест19", "+79001112222", "Haval Jolion", "А222СС165");
        DatabaseFactory.getDatabase().addClient(client);

        DatabaseFactory.getDatabase().addAppointment(new Appointment(client, "Иван", "Замена масла", "2024-05-15", "09:00"));
        DatabaseFactory.getDatabase().addAppointment(new Appointment(client, "Петр", "Диагностика", "2024-05-15", "11:00"));
        DatabaseFactory.getDatabase().addAppointment(new Appointment(client, "Сергей", "Замена фильтров", "2024-05-16", "10:00"));

        List<Appointment> day1 = DatabaseFactory.getDatabase().getAppointmentsByDate("2024-05-15");
        List<Appointment> day2 = DatabaseFactory.getDatabase().getAppointmentsByDate("2024-05-16");

        assertThat(day1).hasSize(2);
        assertThat(day2).hasSize(1);
    }

    @Test
    @Order(20)
    void testUpdateAppointmentStatus() {
        clearDatabase();

        Client client = new Client("Екатерина", "Лебедева", "+79445556677", "Nissan Qashqai", "С444ММ163");
        DatabaseFactory.getDatabase().addClient(client);

        Appointment appointment = new Appointment(
            client, "Иван", "Замена масла",
            "2024-06-01", "10:00"
        );
        DatabaseFactory.getDatabase().addAppointment(appointment);

        Appointment saved = DatabaseFactory.getDatabase().getAllAppointments().get(0);
        saved.setStatus(Appointment.STATUS_COMPLETED);
        DatabaseFactory.getDatabase().updateAppointment(saved);

        Appointment updated = DatabaseFactory.getDatabase().getAllAppointments().get(0);
        assertThat(updated.getStatus()).isEqualTo(Appointment.STATUS_COMPLETED);
    }

    @Test
    @Order(21)
    void testDeleteAppointment() {
        clearDatabase();

        Client client = new Client("Анна", "Смирнова", "+79002223333", "Kia Rio", "В456СЕ163");
        DatabaseFactory.getDatabase().addClient(client);

        Appointment appointment = new Appointment(
            client, "Иван", "Замена масла",
            "2024-06-01", "10:00"
        );
        DatabaseFactory.getDatabase().addAppointment(appointment);

        assertThat(DatabaseFactory.getDatabase().getAllAppointments()).hasSize(1);

        DatabaseFactory.getDatabase().deleteAppointment(DatabaseFactory.getDatabase().getAllAppointments().get(0).getId());
        assertThat(DatabaseFactory.getDatabase().getAllAppointments()).isEmpty();
    }

    @Test
    @Order(22)
    void testOrderStatusConstants() {
        assertThat(WorkOrder.STATUS_NEW).isEqualTo("Новый");
        assertThat(WorkOrder.STATUS_IN_PROGRESS).isEqualTo("В работе");
        assertThat(WorkOrder.STATUS_CLOSED).isEqualTo("Закрыт");
    }

    @Test
    @Order(23)
    void testAppointmentStatusConstants() {
        assertThat(Appointment.STATUS_NEW).isEqualTo("Новая");
        assertThat(Appointment.STATUS_COMPLETED).isEqualTo("Выполнено");
    }

    @Test
    @Order(24)
    void testClientIdLookup() {
        clearDatabase();

        Client client = new Client("Виктор", "Тест24", "+79009999999", "Haval Big Dog", "Е666РР164");
        DatabaseFactory.getDatabase().addClient(client);

        int clientId = DatabaseFactory.getDatabase().getClientId(client);
        assertThat(clientId).isGreaterThan(0);
    }

    @Test
    @Order(25)
    void testGenerateOrderId() {
        clearDatabase();

        Client client = new Client("Иван", "Тест25", "+79001111111", "Haval Jolion", "А111ВС163");
        DatabaseFactory.getDatabase().addClient(client);

        WorkOrder order = new WorkOrder(client);
        order.addService("Диагностика", 1000);
        DatabaseFactory.getDatabase().addOrder(order);

        List<WorkOrder> orders = DatabaseFactory.getDatabase().getAllOrders();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getId()).startsWith("ZAK-");
    }

    @Test
    @Order(26)
    void testGetClientByIdNotFound() {
        clearDatabase();
        Client found = DatabaseFactory.getDatabase().getClientById(99999);
        assertThat(found).isNull();
    }

    @Test
    @Order(27)
    void testUpdateClientNotFound() {
        clearDatabase();
        Client client = new Client("Тест", "Тестов", "+79000000000", "Test", "А000ВС000");
        client.setId(99999);
        DatabaseFactory.getDatabase().updateClient(client);
        assertThat(DatabaseFactory.getDatabase().getAllClients()).isEmpty();
    }

    @Test
    @Order(28)
    void testDeleteClientNotFound() {
        clearDatabase();
        Client client = new Client("Тест", "Тестов", "+79000000000", "Test", "А000ВС000");
        client.setId(99999);
        DatabaseFactory.getDatabase().deleteClient(client);
        assertThat(DatabaseFactory.getDatabase().getAllClients()).isEmpty();
    }

    @Test
    @Order(29)
    void testAddServiceDuplicate() {
        clearDatabase();
        DatabaseFactory.getDatabase().addService(new Service("Услуга", 1000));
        DatabaseFactory.getDatabase().addService(new Service("Услуга", 2000));
        List<Service> services = DatabaseFactory.getDatabase().getAllServices();
        assertThat(services).hasSize(1);
        assertThat(services.get(0).getPrice()).isEqualTo(2000);
    }

    @Test
    @Order(30)
    void testDeleteServiceNotFound() {
        clearDatabase();
        Service service = new Service("Неизвестная услуга", 1000);
        DatabaseFactory.getDatabase().deleteService(service);
        assertThat(DatabaseFactory.getDatabase().getAllServices()).isEmpty();
    }

    @Test
    @Order(31)
    void testAddSparePartDuplicate() {
        clearDatabase();
        DatabaseFactory.getDatabase().addSparePart(new SparePart("Масло", 800, 1200, 10));
        DatabaseFactory.getDatabase().addSparePart(new SparePart("Масло", 900, 1300, 20));
        List<SparePart> parts = DatabaseFactory.getDatabase().getAllSpareParts();
        assertThat(parts).hasSize(1);
        assertThat(parts.get(0).getStock()).isEqualTo(20);
    }

    @Test
    @Order(32)
    void testUpdateSparePartStockNotFound() {
        clearDatabase();
        SparePart part = new SparePart("Неизвестная запчасть", 800, 1200, 10);
        part.setId(99999);
        DatabaseFactory.getDatabase().updateSparePartStock(part, 5);
    }

    @Test
    @Order(33)
    void testDeleteSparePartNotFound() {
        clearDatabase();
        SparePart part = new SparePart("Неизвестная запчасть", 800, 1200, 10);
        part.setId(99999);
        DatabaseFactory.getDatabase().deleteSparePart(part);
        assertThat(DatabaseFactory.getDatabase().getAllSpareParts()).isEmpty();
    }

    @Test
    @Order(34)
    void testAddOrderNoClient() {
        clearDatabase();
        Client client = new Client("Тест", "Тестов", "+79000000000", "Test", "А000ВС000");
        WorkOrder order = new WorkOrder(client);
        order.addService("Услуга", 1000);
        DatabaseFactory.getDatabase().addOrder(order);
        assertThat(DatabaseFactory.getDatabase().getAllOrders()).isEmpty();
    }

    @Test
    @Order(35)
    void testGetOrdersEmpty() {
        clearDatabase();
        assertThat(DatabaseFactory.getDatabase().getAllOrders()).isEmpty();
    }

    @Test
    @Order(36)
    void testGetAppointmentsByDateNotFound() {
        clearDatabase();
        assertThat(DatabaseFactory.getDatabase().getAppointmentsByDate("2024-12-31")).isEmpty();
    }

    @Test
    @Order(37)
    void testUpdateAppointmentNotFound() {
        clearDatabase();
        Client client = new Client("Тест", "Тестов", "+79000000000", "Test", "А000ВС000");
        Appointment app = new Appointment(client, "Иван", "Услуга", "2024-06-01", "10:00");
        app.setId(99999);
        DatabaseFactory.getDatabase().updateAppointment(app);
    }

    @Test
    @Order(38)
    void testDeleteAppointmentNotFound() {
        clearDatabase();
        DatabaseFactory.getDatabase().deleteAppointment(99999);
        assertThat(DatabaseFactory.getDatabase().getAllAppointments()).isEmpty();
    }
}
