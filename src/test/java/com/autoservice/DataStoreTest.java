package com.autoservice;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;

/**
 * Тесты для DataStore (слой кэша и координатор CRUD).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.INTEGRATION)
class DataStoreTest extends BaseTest {

    @Test
    @Order(1)
    void testInitialLoad() {
        clearDatabase();

        DataStore.load();

        assertThat(DataStore.getClients()).isEmpty();
        assertThat(DataStore.getServices()).isEmpty();
        assertThat(DataStore.getSpareParts()).isEmpty();
        assertThat(DataStore.getOrders()).isEmpty();
        assertThat(DataStore.getAppointments()).isEmpty();
    }

    @Test
    @Order(2)
    void testAddClient() {
        clearDatabase();

        DataStore.load();

        Client client = new Client("Иван", "Петров", "+79001234567", "Toyota Camry", "А123ВС163");
        DataStore.addClient(client);
        DataStore.load();

        assertThat(DataStore.getClients()).hasSize(1);
        assertThat(DataStore.getClients().get(0).getName()).isEqualTo("Иван");
    }

    @Test
    @Order(3)
    void testAddMultipleClients() {
        clearDatabase();

        DataStore.load();

        DataStore.addClient(new Client("Анна", "Смирнова", "+79002223333", "Kia Rio", "В456СЕ163"));
        DataStore.addClient(new Client("Сергей", "Васильев", "+79003334444", "Ford Focus", "Е789КХ163"));
        DataStore.load();

        assertThat(DataStore.getClients()).hasSize(2);
    }

    @Test
    @Order(4)
    void testAddService() {
        clearDatabase();

        DataStore.load();

        Service service = new Service("Замена масла", 1500);
        DataStore.addService(service);
        DataStore.load();

        assertThat(DataStore.getServices()).hasSize(1);
        assertThat(DataStore.getServices().get(0).getName()).isEqualTo("Замена масла");
    }

    @Test
    @Order(5)
    void testAddSparePart() {
        clearDatabase();

        DataStore.load();

        SparePart part = new SparePart("Масло моторное", 800, 1200, 20);
        DataStore.addSparePart(part);
        DataStore.load();

        assertThat(DataStore.getSpareParts()).hasSize(1);
        assertThat(DataStore.getSpareParts().get(0).getName()).isEqualTo("Масло моторное");
    }

    @Test
    @Order(6)
    void testAddOrderWithServices() {
        clearDatabase();

        Client client = new Client("Дмитрий", "Соколов", "+79005556666", "Haval F7", "М123НО163");
        DataStore.addClient(client);
        DataStore.load();

        DataStore.addService(new Service("Диагностика", 1000));
        DataStore.addService(new Service("Замена масла", 1500));
        DataStore.load();

        WorkOrder order = new WorkOrder(client);
        order.addService("Диагностика", 1000);
        order.addService("Замена масла", 1500);

        DataStore.addOrder(order);
        DataStore.load();

        assertThat(DataStore.getOrders()).hasSize(1);
        WorkOrder savedOrder = DataStore.getOrders().get(0);
        assertThat(savedOrder.getServices()).hasSize(2);
        assertThat(savedOrder.getTotal()).isEqualTo(2500);
    }

    @Test
    @Order(7)
    void testAddOrderWithSpareParts() {
        clearDatabase();

        Client client = new Client("Елена", "Морозова", "+79007778888", "Toyota RAV4", "С456ТЕ163");
        DataStore.addClient(client);
        DataStore.load();

        SparePart oil = new SparePart("Масло моторное", 800, 1200, 20);
        SparePart filter = new SparePart("Фильтр масляный", 300, 500, 15);
        DataStore.addSparePart(oil);
        DataStore.addSparePart(filter);
        DataStore.load();

        WorkOrder order = new WorkOrder(client);
        order.addSparePart(oil, 1);
        order.addSparePart(filter, 1);

        DataStore.addOrder(order);
        DataStore.load();

        assertThat(DataStore.getOrders()).hasSize(1);
        WorkOrder savedOrder = DataStore.getOrders().get(0);
        assertThat(savedOrder.getSpareParts()).hasSize(2);
        assertThat(savedOrder.getTotal()).isEqualTo(1700);
    }

    @Test
    @Order(8)
    void testAddOrderWithServicesAndParts() {
        clearDatabase();

        Client client = new Client("Алексей", "Новиков", "+79009990000", "BMW X5", "О789УУ163");
        DataStore.addClient(client);
        DataStore.load();

        DataStore.addService(new Service("Замена масла", 1500));
        DataStore.load();

        SparePart oil = new SparePart("Масло моторное", 800, 1200, 20);
        DataStore.addSparePart(oil);
        DataStore.load();

        WorkOrder order = new WorkOrder(client);
        order.addService("Замена масла", 1500);
        order.addSparePart(oil, 1);

        DataStore.addOrder(order);
        DataStore.load();

        assertThat(DataStore.getOrders()).hasSize(1);
        WorkOrder savedOrder = DataStore.getOrders().get(0);
        assertThat(savedOrder.getServices()).hasSize(1);
        assertThat(savedOrder.getSpareParts()).hasSize(1);
        assertThat(savedOrder.getTotal()).isEqualTo(2700);
    }

    @Test
    @Order(9)
    void testUpdateOrder() {
        clearDatabase();

        Client client = new Client("Мария", "Кузнецова", "+79112223344", "Mercedes E-Class", "К111ОО163");
        DataStore.addClient(client);
        DataStore.load();

        WorkOrder order = new WorkOrder(client);
        order.addService("Диагностика", 1000);
        DataStore.addOrder(order);
        DataStore.load();

        WorkOrder savedOrder = DataStore.getOrders().get(0);
        savedOrder.addService("Замена масла", 1500);
        DataStore.updateOrder(savedOrder);
        DataStore.load();

        WorkOrder updatedOrder = DataStore.getOrders().stream()
                .filter(o -> o.getId().equals(savedOrder.getId()))
                .findFirst()
                .orElse(null);

        assertThat(updatedOrder).isNotNull();
        assertThat(updatedOrder.getServices()).hasSize(2);
    }

    @Test
    @Order(10)
    void testDeleteOrder() {
        clearDatabase();

        Client client = new Client("Ольга", "Попов", "+79223334455", "Lexus RX", "Н222СС163");
        DataStore.addClient(client);
        DataStore.load();

        WorkOrder order = new WorkOrder(client);
        order.addService("Диагностика", 1000);
        DataStore.addOrder(order);
        DataStore.load();

        assertThat(DataStore.getOrders()).hasSize(1);

        WorkOrder savedOrder = DataStore.getOrders().get(0);
        DataStore.deleteOrder(savedOrder);
        DataStore.load();

        assertThat(DataStore.getOrders()).isEmpty();
    }

    @Test
    @Order(11)
    void testGetOrdersByClient() {
        clearDatabase();

        Client client = new Client("Павел", "Волков", "+79334445566", "Audi Q7", "Р333ВВ163");
        DataStore.addClient(client);
        DataStore.load();

        WorkOrder order1 = new WorkOrder(client);
        order1.addService("Диагностика", 1000);
        DataStore.addOrder(order1);
        DataStore.load();

        WorkOrder order2 = new WorkOrder(client);
        order2.addService("Замена масла", 1500);
        DataStore.addOrder(order2);
        DataStore.load();

        assertThat(DataStore.getOrders()).hasSize(2);

        long clientOrdersCount = DataStore.getOrders().stream()
                .filter(o -> o.getClient().getName().equals("Павел"))
                .count();

        assertThat(clientOrdersCount).isEqualTo(2);
    }

    @Test
    @Order(12)
    void testOrderStatus() {
        clearDatabase();

        Client client = new Client("Екатерина", "Лебедева", "+79445556677", "Nissan Qashqai", "С444ММ163");
        DataStore.addClient(client);
        DataStore.load();

        WorkOrder order = new WorkOrder(client);
        order.setStatus(WorkOrder.STATUS_NEW);
        DataStore.addOrder(order);

        order.setStatus(WorkOrder.STATUS_IN_PROGRESS);
        DataStore.updateOrder(order);
        DataStore.load();

        assertThat(DataStore.getOrders().get(0).getStatus()).isEqualTo(WorkOrder.STATUS_IN_PROGRESS);
    }

    @Test
    @Order(13)
    void testAddAppointment() {
        clearDatabase();

        Client client = new Client("Николай", "Соловьев", "+79556667788", "Mazda CX-5", "Т555НО163");
        DataStore.addClient(client);
        DataStore.load();

        Appointment appointment = new Appointment(
            client, "Иван", "Замена масла",
            "2024-06-01", "10:00"
        );

        DataStore.addAppointment(appointment);
        DataStore.load();

        assertThat(DataStore.getAppointments()).hasSize(1);
        assertThat(DataStore.getAppointments().get(0).getMasterName()).isEqualTo("Иван");
    }

    @Test
    @Order(14)
    void testUpdateAppointment() {
        clearDatabase();

        Client client = new Client("Ольга", "Федорова", "+79667778899", "Hyundai Creta", "У666КХ163");
        DataStore.addClient(client);
        DataStore.load();

        Appointment appointment = new Appointment(
            client, "Петр", "Диагностика",
            "2024-06-02", "14:00"
        );

        DataStore.addAppointment(appointment);
        DataStore.load();

        Appointment saved = DataStore.getAppointments().get(0);
        saved.setStatus(Appointment.STATUS_COMPLETED);
        DataStore.updateAppointment(saved);
        DataStore.load();

        assertThat(DataStore.getAppointments().get(0).getStatus()).isEqualTo(Appointment.STATUS_COMPLETED);
    }

    @Test
    @Order(15)
    void testGetAppointmentsByDate() {
        clearDatabase();

        Client client = new Client("Татьяна", "Лебедева", "+79990001111", "Haval Big Dog", "Е333РР163");
        DataStore.addClient(client);
        DataStore.load();

        Appointment appointment1 = new Appointment(
            client, "Иван", "Замена масла",
            "2024-05-15", "09:00"
        );
        Appointment appointment2 = new Appointment(
            client, "Петр", "Диагностика",
            "2024-05-15", "11:00"
        );
        DataStore.addAppointment(appointment1);
        DataStore.addAppointment(appointment2);
        DataStore.load();

        List<Appointment> appointments = DataStore.getAppointmentsByDate("2024-05-15");
        assertThat(appointments).hasSize(2);
    }

    @Test
    @Order(16)
    void testGetActiveOrdersCount() {
        clearDatabase();

        DataStore.load();

        Client client = new Client("Максим", "Тест16", "+79001112222", "Haval Jolion", "А222СС165");
        DataStore.addClient(client);
        DataStore.load();

        WorkOrder activeOrder = new WorkOrder(client);
        activeOrder.addService("Диагностика Тест16", 500);
        activeOrder.setStatus(WorkOrder.STATUS_NEW);
        DataStore.addOrder(activeOrder);

        WorkOrder closedOrder = new WorkOrder(client);
        closedOrder.addService("Замена масла Тест16", 1500);
        closedOrder.setStatus(WorkOrder.STATUS_CLOSED);
        DataStore.addOrder(closedOrder);

        DataStore.load();

        assertThat(DataStore.getActiveOrdersCount()).isEqualTo(1);
    }

    @Test
    @Order(17)
    void testLoadData() {
        clearDatabase();

        Client client = new Client("Загрузочный", "Клиент", "+79110001111", "Haval F7", "В000СС163");
        DataStore.addClient(client);
        DataStore.load();

        DataStore.load();

        assertThat(DataStore.getClients()).hasSize(1);
        assertThat(DataStore.getClients().get(0).getName()).isEqualTo("Загрузочный");
    }

    @Test
    @Order(18)
    void testDataIsolation() {
        clearDatabase();

        Client client1 = new Client("Клиент", "1", "+79001111111", "Toyota", "А001СС163");
        Client client2 = new Client("Клиент", "2", "+79002222222", "Honda", "В002СС163");
        DataStore.addClient(client1);
        DataStore.addClient(client2);
        DataStore.load();

        assertThat(DataStore.getClients()).hasSize(2);

        clearDatabase();
        DataStore.load();

        assertThat(DataStore.getClients()).isEmpty();
    }

    @Test
    @Order(19)
    void testUpdateClient() {
        clearDatabase();
        DataStore.load();

        Client client = new Client("Тест", "Тестов", "+79000000000", "Test", "А000ВС000");
        DataStore.addClient(client);
        DataStore.load();

        Client updatedClient = DataStore.getClients().get(0);
        updatedClient.setPhone("+79998887766");
        DataStore.updateClient(updatedClient);
        DataStore.load();

        assertThat(DataStore.getClients().get(0).getPhone()).isEqualTo("+79998887766");
    }

    @Test
    @Order(20)
    void testUpdateService() {
        clearDatabase();
        DataStore.load();

        Service service = new Service("Услуга", 1000);
        DataStore.addService(service);
        DataStore.load();

        Service updatedService = DataStore.getServices().get(0);
        updatedService.setPrice(2000);
        DataStore.updateService(updatedService);
        DataStore.load();

        assertThat(DataStore.getServices().get(0).getPrice()).isEqualTo(2000);
    }

    @Test
    @Order(21)
    void testUpdateSparePart() {
        clearDatabase();
        DataStore.load();

        SparePart part = new SparePart("Масло", 800, 1200, 10);
        DataStore.addSparePart(part);
        DataStore.load();

        SparePart updatedPart = DataStore.getSpareParts().get(0);
        updatedPart.setStock(20);
        DataStore.updateSparePart(updatedPart);
        DataStore.load();

        assertThat(DataStore.getSpareParts().get(0).getStock()).isEqualTo(20);
    }

    @Test
    @Order(22)
    void testUpdateOrderNotFound() {
        clearDatabase();
        DataStore.load();

        Client client = new Client("Тест", "Тестов", "+79000000000", "Test", "А000ВС000");
        WorkOrder order = new WorkOrder(client);
        order.setId("ZAK-99/99/99-9999");
        order.setStatus("Новый");
        DataStore.updateOrder(order);
        assertThat(DataStore.getOrders()).isEmpty();
    }

    @Test
    @Order(23)
    void testDeleteSparePart() {
        clearDatabase();
        DataStore.load();

        SparePart part = new SparePart("Масло", 800, 1200, 10);
        DataStore.addSparePart(part);
        DataStore.load();

        DataStore.removeSparePart(part);
        DataStore.load();

        assertThat(DataStore.getSpareParts()).isEmpty();
    }

    @Test
    @Order(24)
    void testGetClientsEmpty() {
        clearDatabase();
        DataStore.load();
        assertThat(DataStore.getClients()).isEmpty();
    }

    @Test
    @Order(25)
    void testGetServicesEmpty() {
        clearDatabase();
        DataStore.load();
        assertThat(DataStore.getServices()).isEmpty();
    }

    @Test
    @Order(26)
    void testGetSparePartsEmpty() {
        clearDatabase();
        DataStore.load();
        assertThat(DataStore.getSpareParts()).isEmpty();
    }

    @Test
    @Order(27)
    void testGetOrdersEmpty() {
        clearDatabase();
        DataStore.load();
        assertThat(DataStore.getOrders()).isEmpty();
    }

    @Test
    @Order(28)
    void testGetAppointmentsEmpty() {
        clearDatabase();
        DataStore.load();
        assertThat(DataStore.getAppointments()).isEmpty();
    }

    @Test
    @Order(29)
    void testGetAppointmentsByDateEmpty() {
        clearDatabase();
        DataStore.load();
        assertThat(DataStore.getAppointmentsByDate("2024-12-31")).isEmpty();
    }
}
