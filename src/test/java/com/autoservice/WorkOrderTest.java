package com.autoservice;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для класса WorkOrder
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkOrderTest {

    private Client testClient;
    private Service testService;
    private SparePart testPart;

    @BeforeEach
    void setUp() {
        testClient = new Client("Иван", "+79001234567", "Haval Jolion", "А123ВС163");
        testService = new Service("Замена масла", 1500);
        testPart = new SparePart("Моторное масло 5W-30", 800, 1200, 10);
    }

    @Test
    @Order(1)
    void testWorkOrderConstructor() {
        WorkOrder order = new WorkOrder(testClient);
        
        assertThat(order.getClient()).isEqualTo(testClient);
        assertThat(order.getStatus()).isEqualTo(WorkOrder.STATUS_NEW);
        assertThat(order.getTotal()).isEqualTo(0);
        assertThat(order.getServices()).isEmpty();
        assertThat(order.getSpareParts()).isEmpty();
        assertThat(order.getId()).isNull();
    }

    @Test
    @Order(2)
    void testWorkOrderConstructorWithId() {
        WorkOrder order = new WorkOrder("ZAK-15/01/24-00001", testClient, WorkOrder.STATUS_IN_WORK, 5000);
        
        assertThat(order.getId()).isEqualTo("ZAK-15/01/24-00001");
        assertThat(order.getClient()).isEqualTo(testClient);
        assertThat(order.getStatus()).isEqualTo(WorkOrder.STATUS_IN_WORK);
        assertThat(order.getTotal()).isEqualTo(5000);
    }

    @Test
    @Order(3)
    void testWorkOrderStatuses() {
        assertThat(WorkOrder.STATUS_NEW).isEqualTo("НОВЫЙ");
        assertThat(WorkOrder.STATUS_DIAGNOSTICS).isEqualTo("ДИАГНОСТИКА");
        assertThat(WorkOrder.STATUS_IN_WORK).isEqualTo("В РАБОТЕ");
        assertThat(WorkOrder.STATUS_WAITING_PARTS).isEqualTo("ОЖИДАНИЕ ЗАПЧАСТЕЙ");
        assertThat(WorkOrder.STATUS_READY).isEqualTo("ГОТОВ");
        assertThat(WorkOrder.STATUS_CLOSED).isEqualTo("ЗАКРЫТ");
    }

    @Test
    @Order(4)
    void testWorkOrderGetAllStatuses() {
        String[] statuses = WorkOrder.getAllStatuses();
        
        assertThat(statuses).hasSize(6);
        assertThat(statuses).contains(
            WorkOrder.STATUS_NEW,
            WorkOrder.STATUS_DIAGNOSTICS,
            WorkOrder.STATUS_IN_WORK,
            WorkOrder.STATUS_WAITING_PARTS,
            WorkOrder.STATUS_READY,
            WorkOrder.STATUS_CLOSED
        );
    }

    @Test
    @Order(5)
    void testWorkOrderAddService() {
        WorkOrder order = new WorkOrder(testClient);
        
        order.addService("Замена масла", 1500);
        
        assertThat(order.getServices()).hasSize(1);
        assertThat(order.getServices().get(0)).isEqualTo("Замена масла");
        assertThat(order.getServicePrices().get(0)).isEqualTo(1500);
        assertThat(order.getTotal()).isEqualTo(1500);
    }

    @Test
    @Order(6)
    void testWorkOrderAddMultipleServices() {
        WorkOrder order = new WorkOrder(testClient);
        
        order.addService("Замена масла", 1500);
        order.addService("Замена фильтра", 500);
        order.addService("Диагностика", 1000);
        
        assertThat(order.getServices()).hasSize(3);
        assertThat(order.getTotal()).isEqualTo(3000);
    }

    @Test
    @Order(7)
    void testWorkOrderRemoveService() {
        WorkOrder order = new WorkOrder(testClient);
        
        order.addService("Услуга 1", 1000);
        order.addService("Услуга 2", 2000);
        order.addService("Услуга 3", 3000);
        
        assertThat(order.getTotal()).isEqualTo(6000);
        
        order.removeService(1);
        
        assertThat(order.getServices()).hasSize(2);
        assertThat(order.getTotal()).isEqualTo(4000);
    }

    @Test
    @Order(8)
    void testWorkOrderRemoveServiceInvalidIndex() {
        WorkOrder order = new WorkOrder(testClient);
        
        order.addService("Услуга 1", 1000);
        
        order.removeService(-1);
        order.removeService(5);
        
        assertThat(order.getServices()).hasSize(1);
        assertThat(order.getTotal()).isEqualTo(1000);
    }

    @Test
    @Order(9)
    void testWorkOrderAddSparePart() {
        WorkOrder order = new WorkOrder(testClient);
        
        order.addSparePart(testPart, 2);
        
        assertThat(order.getSpareParts()).hasSize(1);
        assertThat(order.getSpareParts().get(0)).isEqualTo(testPart);
        assertThat(order.getSparePartQuantities().get(0)).isEqualTo(2);
        assertThat(order.getTotal()).isEqualTo(2400);
    }

    @Test
    @Order(10)
    void testWorkOrderAddMultipleSpareParts() {
        SparePart part2 = new SparePart("Масляный фильтр", 300, 500, 20);
        
        WorkOrder order = new WorkOrder(testClient);
        
        order.addSparePart(testPart, 2);
        order.addSparePart(part2, 3);
        
        assertThat(order.getSpareParts()).hasSize(2);
        assertThat(order.getSparePartQuantities()).containsExactly(2, 3);
        assertThat(order.getTotal()).isEqualTo(2400 + 1500);
    }

    @Test
    @Order(11)
    void testWorkOrderRemoveSparePart() {
        SparePart part2 = new SparePart("Масляный фильтр", 300, 500, 20);
        
        WorkOrder order = new WorkOrder(testClient);
        
        order.addSparePart(testPart, 2);
        order.addSparePart(part2, 3);
        
        assertThat(order.getTotal()).isEqualTo(3900);
        
        order.removeSparePart(0);
        
        assertThat(order.getSpareParts()).hasSize(1);
        assertThat(order.getTotal()).isEqualTo(1500);
    }

    @Test
    @Order(12)
    void testWorkOrderRemoveSparePartInvalidIndex() {
        WorkOrder order = new WorkOrder(testClient);
        
        order.addSparePart(testPart, 2);
        
        order.removeSparePart(-1);
        order.removeSparePart(5);
        
        assertThat(order.getSpareParts()).hasSize(1);
        assertThat(order.getTotal()).isEqualTo(2400);
    }

    @Test
    @Order(13)
    void testWorkOrderRecalculateTotal() {
        WorkOrder order = new WorkOrder(testClient);
        
        order.addService("Услуга", 1000);
        order.addSparePart(testPart, 2);
        
        assertThat(order.getTotal()).isEqualTo(3400);
        
        order.recalculateTotal();
        
        assertThat(order.getTotal()).isEqualTo(3400);
    }

    @Test
    @Order(14)
    void testWorkOrderSetters() {
        WorkOrder order = new WorkOrder(testClient);
        
        order.setId("ZAK-15/01/24-00001");
        order.setStatus(WorkOrder.STATUS_IN_WORK);
        
        assertThat(order.getId()).isEqualTo("ZAK-15/01/24-00001");
        assertThat(order.getStatus()).isEqualTo(WorkOrder.STATUS_IN_WORK);
    }

    @Test
    @Order(15)
    void testWorkOrderToString() {
        WorkOrder order = new WorkOrder("ZAK-15/01/24-00001", testClient, WorkOrder.STATUS_NEW, 5000);
        
        String result = order.toString();
        assertThat(result).contains("ZAK-15/01/24-00001");
        assertThat(result).contains("Иван");
        assertThat(result).contains("5000");
        assertThat(result).contains("НОВЫЙ");
    }

    @Test
    @Order(16)
    void testWorkOrderEmptyOrder() {
        WorkOrder order = new WorkOrder(testClient);
        
        assertThat(order.getServices()).isEmpty();
        assertThat(order.getSpareParts()).isEmpty();
        assertThat(order.getTotal()).isEqualTo(0);
    }

    @Test
    @Order(17)
    void testWorkOrderOnlyServices() {
        WorkOrder order = new WorkOrder(testClient);
        
        order.addService("Услуга 1", 1000);
        order.addService("Услуга 2", 2000);
        
        assertThat(order.getServices()).hasSize(2);
        assertThat(order.getSpareParts()).isEmpty();
        assertThat(order.getTotal()).isEqualTo(3000);
    }

    @Test
    @Order(18)
    void testWorkOrderOnlySpareParts() {
        WorkOrder order = new WorkOrder(testClient);
        
        order.addSparePart(testPart, 3);
        
        assertThat(order.getServices()).isEmpty();
        assertThat(order.getSpareParts()).hasSize(1);
        assertThat(order.getTotal()).isEqualTo(3600);
    }
}
