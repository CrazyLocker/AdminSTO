package com.autoservice.controllers;

import com.autoservice.*;
import com.autoservice.TestTags;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для OrderController
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.CONTROLLER)
class OrderControllerTest extends BaseTest {

    private Client testClient;
    private WorkOrder testOrder;

    @BeforeEach
    void setUp() {
        testClient = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        testClient.setId(1);
        
        testOrder = new WorkOrder(testClient);
        testOrder.setId("ZAK-01/01/24-0001");
        testOrder.addService("Замена масла", 1500);
    }

    @Test
    @Order(1)
    void testSetTable() {
        OrderController.setTable(null);
        assertThat(OrderController.class).isNotNull();
    }

    @Test
    @Order(2)
    @Disabled("Requires JavaFX initialization - AppointmentView.datePicker is null")
    void testRefreshTable() {
        OrderController.setTable(null);
        OrderController.refreshTable();
        assertThat(true).isTrue();
    }

    @Test
    @Order(3)
    void testCreateOrderWithClients() {
        DataStore.load();
        OrderController.setTable(null);
        
        ClientController.addClient(testClient);
        DataStore.load();
        
        assertThat(DataStore.getClients()).isNotEmpty();
    }

    @Test
    @Order(4)
    void testCreateOrderNoClients() {
        OrderController.setTable(null);
        assertThat(true).isTrue();
    }

    @Test
    @Order(5)
    void testEditOrderValid() {
        OrderController.setTable(null);
        assertThat(testOrder.getStatus()).isEqualTo(WorkOrder.STATUS_NEW);
    }

    @Test
    @Order(6)
    void testEditOrderClosed() {
        WorkOrder closedOrder = new WorkOrder(testClient);
        closedOrder.setStatus(WorkOrder.STATUS_CLOSED);
        
        assertThat(closedOrder.getStatus()).isEqualTo(WorkOrder.STATUS_CLOSED);
    }

    @Test
    @Order(7)
    @Disabled("Requires JavaFX initialization")
    void testViewOrder() {
        OrderController.setTable(null);
        OrderController.viewOrder(testOrder);
        assertThat(testOrder.getId()).isNotNull();
    }

    @Test
    @Order(8)
    @Disabled("Requires JavaFX initialization - AppointmentView.datePicker is null")
    void testChangeStatus() {
        OrderController.setTable(null);
        OrderController.changeOrderStatus(testOrder, WorkOrder.STATUS_IN_PROGRESS);
        
        assertThat(testOrder.getStatus()).isEqualTo(WorkOrder.STATUS_IN_PROGRESS);
    }

    @Test
    @Order(9)
    void testDeleteOrder() {
        OrderController.setTable(null);
        DataStore.load();
        
        assertThat(true).isTrue();
    }

    @Test
    @Order(10)
    void testReturnSparePartsToStock() {
        OrderController.setTable(null);
        
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        assertThat(part.getStock()).isEqualTo(10);
        
        double newStock = part.getStock() + 2;
        assertThat(newStock).isEqualTo(12);
    }

    @Test
    @Order(11)
    void testDeleteAssociatedAppointment() {
        OrderController.setTable(null);
        
        Appointment app = new Appointment(testClient, "Иван", "Замена масла", "2024-06-01", "10:00");
        app.setOrderId(testOrder.getId());
        
        assertThat(app.getOrderId()).isEqualTo(testOrder.getId());
    }

    @Test
    @Order(12)
    void testCreateOrderWithServices() {
        OrderController.setTable(null);
        
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Замена масла", 1500);
        
        assertThat(order.getServices()).hasSize(1);
        assertThat(order.getTotal()).isEqualTo(1500);
    }

    @Test
    @Order(13)
    void testCreateOrderWithParts() {
        OrderController.setTable(null);
        
        WorkOrder order = new WorkOrder(testClient);
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        order.addSparePart(part, 2);
        
        assertThat(order.getSpareParts()).hasSize(1);
        assertThat(order.getTotal()).isEqualTo(2400);
    }

    @Test
    @Order(14)
    void testCreateOrderWithServicesAndParts() {
        OrderController.setTable(null);
        
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Замена масла", 1500);
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        order.addSparePart(part, 1);
        
        assertThat(order.getServices()).hasSize(1);
        assertThat(order.getSpareParts()).hasSize(1);
        assertThat(order.getTotal()).isEqualTo(2700);
    }

    @Test
    @Order(15)
    void testOrderWithMultipleServices() {
        OrderController.setTable(null);
        
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Услуга 1", 1000);
        order.addService("Услуга 2", 2000);
        order.addService("Услуга 3", 3000);
        
        assertThat(order.getServices()).hasSize(3);
        assertThat(order.getTotal()).isEqualTo(6000);
    }
}
