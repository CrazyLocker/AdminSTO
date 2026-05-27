package com.autoservice;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataStoreTest {

    private static Client testClient;
    private static Service testService;
    private static SparePart testPart;

    @BeforeAll
    static void setup() {
        Database.initForTest();
    }

    @BeforeEach
    void cleanBeforeEach() {
        clearDatabase();

        testClient = new Client("Тест Клиент", "+79001234567", "Test Car", "A123BC");
        DataStore.addClient(testClient);

        testService = new Service("Тест услуга", 1000);
        DataStore.addService(testService);

        testPart = new SparePart("Тест запчасть", 500, 800, 10);
        DataStore.addSparePart(testPart);
    }

    private static void clearDatabase() {
        try {
            java.sql.Statement stmt = Database.getConnection().createStatement();
            stmt.execute("DELETE FROM order_parts");
            stmt.execute("DELETE FROM order_services");
            stmt.execute("DELETE FROM appointments");
            stmt.execute("DELETE FROM orders");
            stmt.execute("DELETE FROM spare_parts");
            stmt.execute("DELETE FROM services");
            stmt.execute("DELETE FROM clients");
            stmt.execute("DELETE FROM sqlite_sequence");
            stmt.close();
        } catch (Exception e) {
            System.out.println("Очистка БД: " + e.getMessage());
        }
    }

    @AfterAll
    static void cleanup() {
        Database.close();
    }

    @Test
    @Order(1)
    void testCreateOrder() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Тест услуга", 1000);
        order.addSparePart(testPart, 2);

        DataStore.addOrder(order);

        assertThat(DataStore.getOrders()).isNotEmpty();
        assertThat(order.getId()).isNotNull();
        assertThat(order.getId()).startsWith("ZAK-");
    }

    @Test
    @Order(2)
    void testOrderHasServices() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Тест услуга", 1000);
        order.addSparePart(testPart, 2);
        DataStore.addOrder(order);

        WorkOrder lastOrder = DataStore.getOrders().get(DataStore.getOrders().size() - 1);
        assertThat(lastOrder.getServices()).isNotEmpty();
        assertThat(lastOrder.getServices().get(0)).isEqualTo("Тест услуга");
    }

    @Test
    @Order(3)
    void testOrderHasSpareParts() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Тест услуга", 1000);
        order.addSparePart(testPart, 2);
        DataStore.addOrder(order);

        WorkOrder lastOrder = DataStore.getOrders().get(DataStore.getOrders().size() - 1);
        assertThat(lastOrder.getSpareParts()).isNotEmpty();
        assertThat(lastOrder.getSpareParts().get(0).getName()).isEqualTo("Тест запчасть");
        assertThat(lastOrder.getSparePartQuantities().get(0)).isEqualTo(2);
    }

    @Test
    @Order(4)
    void testOrderTotal() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Тест услуга", 1000);
        order.addSparePart(testPart, 2);
        DataStore.addOrder(order);

        WorkOrder lastOrder = DataStore.getOrders().get(DataStore.getOrders().size() - 1);
        double expectedTotal = 1000 + (800 * 2);
        assertThat(lastOrder.getTotal()).isEqualTo(expectedTotal);
    }

    @Test
    @Order(5)
    void testDeleteOrder() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Тест услуга", 1000);
        DataStore.addOrder(order);

        int sizeBefore = DataStore.getOrders().size();
        assertThat(sizeBefore).isEqualTo(1);

        WorkOrder orderToDelete = DataStore.getOrders().get(0);
        DataStore.deleteOrder(orderToDelete);

        int sizeAfter = DataStore.getOrders().size();
        assertThat(sizeAfter).isEqualTo(0);
    }

    @Test
    @Order(6)
    void testOrderIdFormat() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Тест услуга", 1000);
        DataStore.addOrder(order);

        WorkOrder savedOrder = DataStore.getOrders().get(DataStore.getOrders().size() - 1);
        String id = savedOrder.getId();

        // Проверяем формат ZAK-XX/XX/XX-XXXXX
        assertThat(id).matches("ZAK-\\d{2}/\\d{2}/\\d{2}-\\d{5}");
    }
}