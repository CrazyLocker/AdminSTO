package com.autoservice;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataStoreTest {

    private static Service testService;
    private static SparePart testPart;

    @BeforeAll
    static void setup() {
        Database.initForTest();
        clearDatabase();

        testService = new Service("Тест услуга", 1000);
        DataStore.addService(testService);

        testPart = new SparePart("Тест запчасть", 500, 800, 10);
        DataStore.addSparePart(testPart);
    }

    private static void clearDatabase() {
        try {
            var stmt = Database.getConnection().createStatement();
            stmt.execute("DELETE FROM order_parts");
            stmt.execute("DELETE FROM order_services");
            stmt.execute("DELETE FROM orders");
            stmt.execute("DELETE FROM spare_parts");
            stmt.execute("DELETE FROM services");
            stmt.execute("DELETE FROM clients");
            stmt.execute("DELETE FROM appointments");
            stmt.execute("DELETE FROM sqlite_sequence");
            stmt.close();
            System.out.println("База данных очищена");
        } catch (Exception e) {
            System.out.println("Очистка БД: " + e.getMessage());
        }
    }

    private static Client createTestClient() {
        Client client = new Client("Тест Клиент", "Тестов", "+79001234567", "Test Car", "A123BC");
        DataStore.addClient(client);
        DataStore.load();
        return DataStore.getClients().stream()
                .filter(c -> c.getName().equals("Тест Клиент"))
                .findFirst()
                .orElse(null);
    }

    @AfterAll
    static void cleanup() {
        Database.close();
    }

    // ==================== ТЕСТЫ КЛИЕНТОВ ====================

    @Test
    @Order(1)
    void testAddClient() {
        Client newClient = new Client("Новый Клиент", "Новый", "+79111234567", "New Car", "B456CD");
        DataStore.addClient(newClient);

        assertThat(DataStore.getClients()).anyMatch(c -> c.getName().equals("Новый Клиент"));
    }

    @Test
    @Order(2)
    void testUpdateClient() {
        Client newClient = new Client("ДляОбновления", "ДляОбновленияФамилия", "+79000000000", "Update Car", "U123UP");
        DataStore.addClient(newClient);
        DataStore.load();

        Client toUpdate = DataStore.getClients().stream()
                .filter(c -> c.getName().equals("ДляОбновления"))
                .findFirst()
                .orElse(null);
        assertThat(toUpdate).isNotNull();

        String newName = "Обновлённый Клиент";
        String newLastName = "ОбновлённаяФамилия";
        toUpdate.setName(newName);
        toUpdate.setLastName(newLastName);
        DataStore.updateClient(toUpdate);
        DataStore.load();

        Client updated = DataStore.getClients().stream()
                .filter(c -> c.getId() == toUpdate.getId())
                .findFirst()
                .orElse(null);

        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo(newName);
        assertThat(updated.getLastName()).isEqualTo(newLastName);
    }

    @Test
    @Order(3)
    void testDeleteClient() {
        Client newClient = new Client("Для Удаления", "ДляУдаленияФамилия", "+79211234567", "Delete Car", "C789DE");
        DataStore.addClient(newClient);
        DataStore.load();

        Client toDelete = DataStore.getClients().stream()
                .filter(c -> c.getName().equals("Для Удаления"))
                .findFirst()
                .orElse(null);

        assertThat(toDelete).isNotNull();

        int sizeBefore = DataStore.getClients().size();
        DataStore.removeClient(toDelete);
        DataStore.load();
        int sizeAfter = DataStore.getClients().size();

        assertThat(sizeAfter).isEqualTo(sizeBefore - 1);
    }

    // ==================== ТЕСТЫ УСЛУГ ====================

    @Test
    @Order(4)
    void testAddService() {
        Service newService = new Service("Новая услуга", 500);
        DataStore.addService(newService);

        assertThat(DataStore.getServices()).anyMatch(s -> s.getName().equals("Новая услуга"));
    }

    @Test
    @Order(5)
    void testDeleteService() {
        Service newService = new Service("УслугаДляУдаления", 777);
        DataStore.addService(newService);

        Service toDelete = DataStore.getServices().stream()
                .filter(s -> s.getName().equals("УслугаДляУдаления"))
                .findFirst()
                .orElse(null);

        assertThat(toDelete).isNotNull();

        DataStore.removeService(toDelete);

        assertThat(DataStore.getServices()).noneMatch(s -> s.getName().equals("УслугаДляУдаления"));
    }

    // ==================== ТЕСТЫ ЗАПЧАСТЕЙ ====================

    @Test
    @Order(6)
    void testAddSparePart() {
        SparePart newPart = new SparePart("Новая запчасть", 300, 500, 20);
        DataStore.addSparePart(newPart);

        assertThat(DataStore.getSpareParts()).anyMatch(p -> p.getName().equals("Новая запчасть"));
    }

    @Test
    @Order(7)
    void testUpdateSparePartStock() {
        SparePart part = DataStore.getSpareParts().get(0);
        int oldStock = part.getStock();
        int newStock = oldStock + 5;

        DataStore.updateSparePartStock(part, newStock);

        SparePart updated = DataStore.getSpareParts().stream()
                .filter(p -> p.getName().equals(part.getName()))
                .findFirst()
                .orElse(null);

        assertThat(updated).isNotNull();
        assertThat(updated.getStock()).isEqualTo(newStock);
    }

    // ==================== ТЕСТЫ ЗАКАЗОВ ====================

    @Test
    @Order(8)
    void testCreateOrder() {
        Client testClient = createTestClient();
        assertThat(testClient).isNotNull();
        System.out.println("Создаём заказ для клиента: " + testClient.getName() + " (ID=" + testClient.getId() + ")");

        WorkOrder order = new WorkOrder(testClient);
        order.addService("Тест услуга", 1000);
        order.addSparePart(testPart, 2);

        DataStore.addOrder(order);
        DataStore.load();

        assertThat(DataStore.getOrders()).isNotEmpty();

        WorkOrder lastOrder = DataStore.getOrders().get(DataStore.getOrders().size() - 1);
        assertThat(lastOrder.getId()).startsWith("ZAK-");
        assertThat(lastOrder.getTotal()).isEqualTo(1000 + (800 * 2));
    }

    @Test
    @Order(9)
    void testOrderHasServices() {
        Client testClient = createTestClient();
        assertThat(testClient).isNotNull();

        WorkOrder order = new WorkOrder(testClient);
        order.addService("Тест услуга", 1000);
        order.addService("Вторая услуга", 500);
        DataStore.addOrder(order);
        DataStore.load();

        WorkOrder lastOrder = DataStore.getOrders().get(DataStore.getOrders().size() - 1);

        assertThat(lastOrder.getServices()).isNotEmpty();
        assertThat(lastOrder.getServices()).hasSize(2);
        assertThat(lastOrder.getServices().get(0)).isEqualTo("Тест услуга");
        assertThat(lastOrder.getServices().get(1)).isEqualTo("Вторая услуга");
    }

    @Test
    @Order(10)
    void testOrderHasSpareParts() {
        Client testClient = createTestClient();
        assertThat(testClient).isNotNull();

        WorkOrder order = new WorkOrder(testClient);
        order.addSparePart(testPart, 3);
        DataStore.addOrder(order);
        DataStore.load();

        WorkOrder lastOrder = DataStore.getOrders().get(DataStore.getOrders().size() - 1);

        assertThat(lastOrder.getSpareParts()).isNotEmpty();
        assertThat(lastOrder.getSpareParts().get(0).getName()).isEqualTo("Тест запчасть");
        assertThat(lastOrder.getSparePartQuantities().get(0)).isEqualTo(3);
    }

    @Test
    @Order(11)
    void testOrderTotalCalculation() {
        Client testClient = createTestClient();
        assertThat(testClient).isNotNull();

        WorkOrder order = new WorkOrder(testClient);
        order.addService("Услуга1", 500);
        order.addService("Услуга2", 700);
        order.addSparePart(testPart, 2);

        assertThat(order.getTotal()).isEqualTo(500 + 700 + (800 * 2));
    }

    @Test
    @Order(12)
    void testOrderIdFormat() {
        Client testClient = createTestClient();
        assertThat(testClient).isNotNull();

        WorkOrder order = new WorkOrder(testClient);
        order.addService("Тест услуга", 500);
        DataStore.addOrder(order);
        DataStore.load();

        WorkOrder lastOrder = DataStore.getOrders().get(DataStore.getOrders().size() - 1);
        String id = lastOrder.getId();

        assertThat(id).isNotNull();
        assertThat(id).startsWith("ZAK-");
        assertThat(id).matches("ZAK-\\d{2}/\\d{2}/\\d{2}-\\d{5}");
        System.out.println("ID заказа: " + id);
    }

    @Test
    @Order(13)
    void testDeleteOrder() {
        Client testClient = createTestClient();
        assertThat(testClient).isNotNull();

        WorkOrder order = new WorkOrder(testClient);
        order.addService("Услуга для удаления", 1000);
        DataStore.addOrder(order);
        DataStore.load();

        int sizeBefore = DataStore.getOrders().size();

        WorkOrder orderToDelete = DataStore.getOrders().get(sizeBefore - 1);
        String deletedId = orderToDelete.getId();
        DataStore.deleteOrder(orderToDelete);
        DataStore.load();

        int sizeAfter = DataStore.getOrders().size();
        assertThat(sizeAfter).isEqualTo(sizeBefore - 1);

        boolean found = DataStore.getOrders().stream()
                .anyMatch(o -> o.getId().equals(deletedId));
        assertThat(found).isFalse();
    }
}
