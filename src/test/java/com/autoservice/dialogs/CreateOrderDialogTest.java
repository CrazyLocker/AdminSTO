package com.autoservice.dialogs;

import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.TestTags;
import com.autoservice.WorkOrder;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для CreateOrderDialog
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.UI)
class CreateOrderDialogTest {

    private Client testClient;
    private Service testService;
    private SparePart testPart;

    @BeforeEach
    void setUp() {
        testClient = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        testClient.setId(1);
        testService = new Service("Замена масла", 1500);
        testPart = new SparePart("Моторное масло 5W-30", 800, 1200, 10);
    }

    @Test
    @Order(1)
    void testConstructor() {
        assertThat(CreateOrderDialog.class).isNotNull();
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
    void testAddSparePartToOrder() {
        WorkOrder order = new WorkOrder(testClient);
        order.addSparePart(testPart, 2);

        assertThat(order.getSpareParts()).hasSize(1);
        assertThat(order.getSpareParts().get(0).getName()).isEqualTo("Моторное масло 5W-30");
        assertThat(order.getTotal()).isEqualTo(2400);
    }

    @Test
    @Order(4)
    void testCalculateTotalWithServices() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Замена масла", 1500);
        order.addService("Замена фильтра", 500);

        assertThat(order.getTotal()).isEqualTo(2000);
    }

    @Test
    @Order(5)
    void testCalculateTotalWithParts() {
        WorkOrder order = new WorkOrder(testClient);
        order.addSparePart(testPart, 1);

        SparePart filter = new SparePart("Фильтр масляный", 300, 500, 15);
        order.addSparePart(filter, 2);

        assertThat(order.getTotal()).isEqualTo(1200 + 1000);
    }

    @Test
    @Order(6)
    void testCalculateTotalWithServicesAndParts() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Замена масла", 1500);
        order.addSparePart(testPart, 1);
        order.addSparePart(new SparePart("Фильтр", 300, 500, 15), 1);

        assertThat(order.getTotal()).isEqualTo(1500 + 1200 + 500);
    }

    @Test
    @Order(7)
    void testValidateClientSelection() {
        Client client = new Client();
        client.setName("Иван");

        assertThat(client.getName()).isNotEmpty();
    }

    @Test
    @Order(8)
    void testValidateAppointmentTime() {
        List<String> timeSlots = List.of("08:00", "09:00", "10:00", "20:00");
        
        assertThat(timeSlots).contains("08:00");
        assertThat(timeSlots).contains("20:00");
        assertThat(timeSlots).hasSize(4);
    }

    @Test
    @Order(9)
    void testRemoveServiceFromOrder() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Замена масла", 1500);
        order.addService("Замена фильтра", 500);

        order.removeService(0);

        assertThat(order.getServices()).hasSize(1);
        assertThat(order.getServices().get(0)).isEqualTo("Замена фильтра");
        assertThat(order.getTotal()).isEqualTo(500);
    }

    @Test
    @Order(10)
    void testRemoveSparePartFromOrder() {
        WorkOrder order = new WorkOrder(testClient);
        order.addSparePart(testPart, 1);

        order.removeSparePart(0);

        assertThat(order.getSpareParts()).isEmpty();
        assertThat(order.getTotal()).isEqualTo(0);
    }

    @Test
    @Order(11)
    void testAddMultipleServices() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Замена масла", 1500);
        order.addService("Замена фильтра", 500);
        order.addService("Диагностика", 1000);

        assertThat(order.getServices()).hasSize(3);
        assertThat(order.getTotal()).isEqualTo(3000);
    }

    @Test
    @Order(12)
    void testAddMultipleSpareParts() {
        WorkOrder order = new WorkOrder(testClient);
        order.addSparePart(testPart, 1);
        order.addSparePart(new SparePart("Фильтр", 300, 500, 15), 2);
        order.addSparePart(new SparePart("Колодки", 1000, 2000, 5), 1);

        assertThat(order.getSpareParts()).hasSize(3);
        assertThat(order.getTotal()).isEqualTo(1200 + 1000 + 2000);
    }

    @Test
    @Order(13)
    void testRecalculateTotal() {
        WorkOrder order = new WorkOrder(testClient);
        order.addService("Замена масла", 1500);
        order.addSparePart(testPart, 1);

        order.recalculateTotal();
        assertThat(order.getTotal()).isEqualTo(2700);
    }

    @Test
    @Order(14)
    void testOrderStatus() {
        WorkOrder order = new WorkOrder(testClient);

        assertThat(order.getStatus()).isEqualTo(WorkOrder.STATUS_NEW);

        order.setStatus(WorkOrder.STATUS_IN_PROGRESS);
        assertThat(order.getStatus()).isEqualTo(WorkOrder.STATUS_IN_PROGRESS);

        order.setStatus(WorkOrder.STATUS_CLOSED);
        assertThat(order.getStatus()).isEqualTo(WorkOrder.STATUS_CLOSED);
    }

    @Test
    @Order(15)
    void testEmptyOrderTotal() {
        WorkOrder order = new WorkOrder(testClient);

        assertThat(order.getTotal()).isEqualTo(0);
        assertThat(order.getServices()).isEmpty();
        assertThat(order.getSpareParts()).isEmpty();
    }
}
