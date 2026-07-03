package com.autoservice;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;

/**
 * Тесты для модели WorkOrder (заказ-наряд).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkOrderTest extends BaseTest {

    private Client testClient;
    private Service testService;
    private SparePart testPart;

    @BeforeEach
    void setUp() {
        testClient = new Client("Иван", "", "+79001234567", "Haval Jolion", "А123ВС163");
        testService = new Service("Замена масла", 1500);
        testPart = new SparePart("Моторное масло 5W-30", 800, 1200, 10);
    }

    @Test
    @Order(1)
    void testWorkOrderCreation() {
        WorkOrder order = new WorkOrder(testClient);

        assertThat(order).isNotNull();
        assertThat(order.getClient()).isEqualTo(testClient);
        assertThat(order.getServices()).isEmpty();
        assertThat(order.getSpareParts()).isEmpty();
        assertThat(order.getTotal()).isEqualTo(0);
    }

    @Test
    @Order(2)
    void testAddServiceToOrder() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Замена масла", 1500);

        assertThat(order.getServices()).hasSize(1);
        assertThat(order.getServices().get(0)).isEqualTo("Замена масла");
        assertThat(order.getTotal()).isEqualTo(1500);
    }

    @Test
    @Order(3)
    void testAddMultipleServices() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Замена масла", 1500);
        order.addService("Замена фильтра", 500);

        assertThat(order.getServices()).hasSize(2);
        assertThat(order.getTotal()).isEqualTo(2000);
    }

    @Test
    @Order(4)
    void testAddSparePartToOrder() {
        WorkOrder order = new WorkOrder(testClient);
        order.addSparePart(testPart, 2);

        assertThat(order.getSpareParts()).hasSize(1);
        assertThat(order.getSpareParts().get(0).getName()).isEqualTo("Моторное масло 5W-30");
        assertThat(order.getTotal()).isEqualTo(2400); // 1200 * 2
    }

    @Test
    @Order(5)
    void testAddMultipleSpareParts() {
        SparePart filter = new SparePart("Фильтр масляный", 300, 500, 15);
        WorkOrder order = new WorkOrder(testClient);
        order.addSparePart(testPart, 1);
        order.addSparePart(filter, 2);

        assertThat(order.getSpareParts()).hasSize(2);
        assertThat(order.getTotal()).isEqualTo(1200 + 1000);
    }

    @Test
    @Order(6)
    void testAddServicesAndParts() {
        SparePart filter = new SparePart("Фильтр масляный", 300, 500, 15);
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Замена масла", 1500);
        order.addSparePart(testPart, 1);
        order.addSparePart(filter, 1);

        assertThat(order.getServices()).hasSize(1);
        assertThat(order.getSpareParts()).hasSize(2);
        assertThat(order.getTotal()).isEqualTo(1500 + 1200 + 500);
    }

    @Test
    @Order(7)
    void testWorkOrderStatuses() {
        assertThat(WorkOrder.STATUS_DRAFT).isEqualTo("Черновик");
        assertThat(WorkOrder.STATUS_IN_PROGRESS).isEqualTo("В работе");
        assertThat(WorkOrder.STATUS_CLOSED).isEqualTo("Закрыт");
        assertThat(WorkOrder.STATUS_CANCELLED).isEqualTo("Отменён");
    }

    @Test
    @Order(8)
    void testWorkOrderGetAllStatuses() {
        List<String> statuses = List.of(
            WorkOrder.STATUS_DRAFT,
            WorkOrder.STATUS_IN_PROGRESS,
            WorkOrder.STATUS_CLOSED,
            WorkOrder.STATUS_CANCELLED
        );
        assertThat(statuses).hasSize(4);
        assertThat(statuses).contains(
            WorkOrder.STATUS_DRAFT,
            WorkOrder.STATUS_IN_PROGRESS,
            WorkOrder.STATUS_CLOSED,
            WorkOrder.STATUS_CANCELLED
        );
    }

    @Test
    @Order(9)
    void testSetStatus() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Диагностика", 1000);

        order.setStatus(WorkOrder.STATUS_DRAFT);
        assertThat(order.getStatus()).isEqualTo(WorkOrder.STATUS_DRAFT);

        order.setStatus(WorkOrder.STATUS_IN_PROGRESS);
        assertThat(order.getStatus()).isEqualTo(WorkOrder.STATUS_IN_PROGRESS);

        order.setStatus(WorkOrder.STATUS_CLOSED);
        assertThat(order.getStatus()).isEqualTo(WorkOrder.STATUS_CLOSED);
    }

    @Test
    @Order(10)
    void testGetServicePrices() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Замена масла", 1500);
        order.addService("Диагностика", 1000);

        assertThat(order.getServicePrices()).hasSize(2);
        assertThat(order.getServicePrices().get(0)).isEqualTo(1500);
        assertThat(order.getServicePrices().get(1)).isEqualTo(1000);
    }

    @Test
    @Order(11)
    void testGetSparePartQuantities() {
        SparePart filter = new SparePart("Фильтр масляный", 300, 500, 15);
        WorkOrder order = new WorkOrder(testClient);
        order.addSparePart(testPart, 2);
        order.addSparePart(filter, 3);

        assertThat(order.getSparePartQuantities()).hasSize(2);
        assertThat(order.getSparePartQuantities().get(0)).isEqualTo(2);
        assertThat(order.getSparePartQuantities().get(1)).isEqualTo(3);
    }

    @Test
    @Order(12)
    void testEmptyOrderTotal() {
        WorkOrder order = new WorkOrder(testClient);
        assertThat(order.getTotal()).isEqualTo(0);
    }

    @Test
    @Order(13)
    void testOrderWithOnlyServices() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Компьютерная диагностика", 1500);
        order.addService("Сброс сервисного интервала", 500);

        assertThat(order.getServices()).hasSize(2);
        assertThat(order.getSpareParts()).isEmpty();
        assertThat(order.getTotal()).isEqualTo(2000);
    }

    @Test
    @Order(14)
    void testOrderWithOnlyParts() {
        SparePart filter = new SparePart("Фильтр масляный", 300, 500, 15);
        WorkOrder order = new WorkOrder(testClient);
        order.addSparePart(testPart, 2);
        order.addSparePart(filter, 2);

        assertThat(order.getServices()).isEmpty();
        assertThat(order.getSpareParts()).hasSize(2);
        assertThat(order.getTotal()).isEqualTo(2400 + 1000);
    }

    @Test
    @Order(15)
    void testOrderStatusTransitions() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Диагностика", 1000);

        assertThat(order.getStatus()).isEqualTo(WorkOrder.STATUS_DRAFT);

        order.setStatus(WorkOrder.STATUS_IN_PROGRESS);
        assertThat(order.getStatus()).isEqualTo(WorkOrder.STATUS_IN_PROGRESS);

        order.setStatus(WorkOrder.STATUS_CLOSED);
        assertThat(order.getStatus()).isEqualTo(WorkOrder.STATUS_CLOSED);
    }
}
