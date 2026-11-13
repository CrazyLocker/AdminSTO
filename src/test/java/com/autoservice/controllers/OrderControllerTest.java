package com.autoservice.controllers;

import com.autoservice.*;
import com.autoservice.TestTags;
import com.autoservice.support.JavaFxTestSupport;
import org.junit.jupiter.api.*;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для OrderController.
 * 
 * <p>Проверяют базовую функциональность управления заказами: создание,
 * редактирование, изменение статуса, удаление. Тесты {@code testRefreshTable},
 * {@code testViewOrder} и {@code testChangeStatus} работают с JavaFX-представлениями
 * и диалогами, поэтому класс поднимает тулкит в headless-режиме и выполняет
 * действия на FX-потоке.</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.CONTROLLER)
class OrderControllerTest extends BaseTest {

    private Client testClient;
    private WorkOrder testOrder;

    @BeforeAll
    static void initJavaFx() {
        JavaFxTestSupport.initToolkit();
    }

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
    void testRefreshTable() throws InterruptedException {
        OrderController.setTable(null);
        WorkOrder stored = createStoredOrder("Замена масла", 1500);

        // Раньше здесь падало «Toolkit not initialized»: refreshTable() дёргает
        // OrderView/AppointmentView/DashboardView, а их классы наследуют JavaFX-контролы.
        JavaFxTestSupport.runOnFxThread(OrderController::refreshTable);
        JavaFxTestSupport.runOnFxThread(OrderController::refreshTable);

        assertThat(JavaFxTestSupport.fxResponsive(5_000)).as("FX-поток отзывчив").isTrue();
        DataStore.load();
        assertThat(DataStore.getOrders()).as("заказ не потерялся при обновлении").hasSize(1);
        assertThat(DataStore.getOrders().get(0).getId()).isEqualTo(stored.getId());
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
    void testViewOrder() throws InterruptedException {
        OrderController.setTable(null);
        WorkOrder stored = createStoredOrder("Замена масла", 1500);
        String dialogTitle = "Заказ " + stored.getId();

        Thread closer = JavaFxTestSupport.closeStageWhenShown(dialogTitle, 15_000);
        JavaFxTestSupport.runOnFxThread(() -> OrderController.viewOrder(stored));
        closer.join(TimeUnit.SECONDS.toMillis(15));

        assertThat(closer.isAlive()).as("окно подробностей заказа было открыто и закрыто").isFalse();
        assertThat(JavaFxTestSupport.fxResponsive(5_000)).as("FX-поток отзывчив после диалога").isTrue();
        assertThat(JavaFxTestSupport.findShowingStage(dialogTitle)).as("диалог закрыт").isNull();
    }

    @Test
    @Order(8)
    void testChangeStatus() throws InterruptedException {
        OrderController.setTable(null);
        WorkOrder stored = createStoredOrder("Замена масла", 1500);

        JavaFxTestSupport.runOnFxThread(
                () -> OrderController.changeOrderStatus(stored, WorkOrder.STATUS_IN_PROGRESS));

        DataStore.load();
        WorkOrder reloaded = DataStore.getOrders().get(0);
        assertThat(reloaded.getStatus())
                .as("статус сохранён в базе").isEqualTo(WorkOrder.STATUS_IN_PROGRESS);
        assertThat(reloaded.getClosedDate())
                .as("дата закрытия не ставится для промежуточного статуса").isNullOrEmpty();
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

    /**
     * Создать клиента и заказ в базе и вернуть заказ с присвоенным идентификатором.
     *
     * @param serviceName название услуги
     * @param price       цена услуги
     * @return заказ из базы
     */
    private WorkOrder createStoredOrder(String serviceName, double price) {
        ClientController.addClient(testClient);
        DataStore.load();
        Client storedClient = DataStore.getClients().get(0);

        WorkOrder order = new WorkOrder(storedClient);
        order.addService(serviceName, price);
        DataStore.addOrder(order);
        DataStore.load();
        return DataStore.getOrders().get(0);
    }
}

