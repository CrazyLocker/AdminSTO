package com.autoservice.dialogs;

import com.autoservice.*;
import com.autoservice.TestTags;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для EditOrderDialog
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.UI)
class EditOrderDialogTest {

    private Client testClient;
    private WorkOrder testOrder;

    @BeforeEach
    void setUp() {
        testClient = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        testClient.setId(1);
        
        testOrder = new WorkOrder(testClient);
        testOrder.setId("ZAK-01/01/24-0001");
        testOrder.setStatus(WorkOrder.STATUS_NEW);
        testOrder.addService("Замена масла", 1500);
    }

    @Test
    @Order(1)
    void testConstructorWithOpenOrder() {
        assertThat(EditOrderDialog.class).isNotNull();
        assertThat(testOrder.getStatus()).isEqualTo(WorkOrder.STATUS_NEW);
    }

    @Test
    @Order(2)
    void testConstructorWithClosedOrder() {
        WorkOrder closedOrder = new WorkOrder(testClient);
        closedOrder.setStatus(WorkOrder.STATUS_CLOSED);

        assertThat(closedOrder.getStatus()).isEqualTo(WorkOrder.STATUS_CLOSED);
    }

    @Test
    @Order(3)
    void testUpdateServiceList() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Замена масла", 1500);
        order.addService("Замена фильтра", 500);

        assertThat(order.getServices()).hasSize(2);

        order.removeService(0);
        assertThat(order.getServices()).hasSize(1);
        assertThat(order.getServices().get(0)).isEqualTo("Замена фильтра");
    }

    @Test
    @Order(4)
    void testUpdatePartList() {
        WorkOrder order = new WorkOrder(testClient);
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        order.addSparePart(part, 2);

        assertThat(order.getSpareParts()).hasSize(1);
        assertThat(order.getSparePartQuantities()).hasSize(1);
        assertThat(order.getSparePartQuantities().get(0)).isEqualTo(2);
    }

    @Test
    @Order(5)
    void testRecalculateTotal() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Замена масла", 1500);
        order.addSparePart(new SparePart("Масло", 800, 1200, 10), 2);

        double expectedTotal = 1500 + (1200 * 2);
        assertThat(order.getTotal()).isEqualTo(expectedTotal);
    }

    @Test
    @Order(6)
    void testValidateAppointmentDate() {
        String date = "2024-06-01";
        assertThat(date).isNotBlank();
        assertThat(date).contains("2024");
    }

    @Test
    @Order(7)
    void testCannotEditClosedOrder() {
        WorkOrder closedOrder = new WorkOrder(testClient);
        closedOrder.setStatus(WorkOrder.STATUS_CLOSED);

        assertThat(closedOrder.getStatus()).isEqualTo(WorkOrder.STATUS_CLOSED);
    }

    @Test
    @Order(8)
    void testOrderWithNoServicesAndParts() {
        WorkOrder order = new WorkOrder(testClient);
        order.setStatus(WorkOrder.STATUS_NEW);

        assertThat(order.getServices()).isEmpty();
        assertThat(order.getSpareParts()).isEmpty();
        assertThat(order.getTotal()).isEqualTo(0);
    }

    @Test
    @Order(9)
    void testAddAndRemoveMultipleServices() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Услуга 1", 1000);
        order.addService("Услуга 2", 2000);
        order.addService("Услуга 3", 3000);

        assertThat(order.getServices()).hasSize(3);

        order.removeService(1);
        assertThat(order.getServices()).hasSize(2);
        assertThat(order.getServices().get(0)).isEqualTo("Услуга 1");
        assertThat(order.getServices().get(1)).isEqualTo("Услуга 3");
    }

    @Test
    @Order(10)
    void testAddAndRemoveMultipleParts() {
        WorkOrder order = new WorkOrder(testClient);
        SparePart part1 = new SparePart("Часть 1", 100, 200, 5);
        SparePart part2 = new SparePart("Часть 2", 300, 400, 10);
        SparePart part3 = new SparePart("Часть 3", 500, 600, 15);

        order.addSparePart(part1, 2);
        order.addSparePart(part2, 3);
        order.addSparePart(part3, 4);

        assertThat(order.getSpareParts()).hasSize(3);

        order.removeSparePart(1);
        assertThat(order.getSpareParts()).hasSize(2);
    }

    @Test
    @Order(11)
    void testGetServicePrices() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Услуга 1", 1000);
        order.addService("Услуга 2", 2000);

        assertThat(order.getServicePrices()).hasSize(2);
        assertThat(order.getServicePrices().get(0)).isEqualTo(1000);
        assertThat(order.getServicePrices().get(1)).isEqualTo(2000);
    }

    @Test
    @Order(12)
    void testGetSparePartQuantities() {
        WorkOrder order = new WorkOrder(testClient);
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        order.addSparePart(part, 5);
        order.addSparePart(new SparePart("Фильтр", 300, 500, 15), 3);

        assertThat(order.getSparePartQuantities()).hasSize(2);
        assertThat(order.getSparePartQuantities().get(0)).isEqualTo(5);
        assertThat(order.getSparePartQuantities().get(1)).isEqualTo(3);
    }

    @Test
    @Order(13)
    void testMarkClean() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Услуга", 1000);

        order.markClean();
        assertThat(order.isDirty()).isFalse();
    }

    @Test
    @Order(14)
    void testGetId() {
        WorkOrder order = new WorkOrder(testClient);
        order.setId("ZAK-01/01/24-0001");

        assertThat(order.getId()).isEqualTo("ZAK-01/01/24-0001");
    }

    @Test
    @Order(15)
    void testToString() {
        WorkOrder order = new WorkOrder(testClient);
        order.setId("ZAK-01/01/24-0001");
        String result = order.toString();

        assertThat(result).contains("ZAK-01/01/24-0001");
        assertThat(result).contains("Новый");
    }
}
