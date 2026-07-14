package com.autoservice.services;

import com.autoservice.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Тесты для StatisticsService (отчёты и статистика).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.INTEGRATION)
class StatisticsServiceTest extends BaseTest {

    @Test
    @Order(1)
    void testGetDailyRevenueEmpty() {
        DataStore.load();
        Map<String, Double> revenue = StatisticsService.getDailyRevenue(7);
        assertThat(revenue).isEmpty();
    }

    @Test
    @Order(2)
    void testGetDailyRevenueWithClosedOrders() {
        DataStore.load();
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        DataStore.addClient(client);
        DataStore.load();

        WorkOrder order1 = new WorkOrder(client);
        order1.addService("Замена масла", 1500);
        order1.setStatus(WorkOrder.STATUS_CLOSED);
        order1.setCreatedDate(LocalDate.now().toString());
        DataStore.addOrder(order1);
        DataStore.load();

        WorkOrder order2 = new WorkOrder(client);
        order2.addService("Диагностика", 1000);
        order2.setStatus(WorkOrder.STATUS_CLOSED);
        order2.setCreatedDate(LocalDate.now().minusDays(1).toString());
        DataStore.addOrder(order2);
        DataStore.load();

        Map<String, Double> revenue = StatisticsService.getDailyRevenue(7);
        assertThat(revenue).isNotEmpty();
        assertThat(revenue.values().stream().mapToDouble(Double::doubleValue).sum()).isEqualTo(2500.0);
    }

    @Test
    @Order(3)
    void testGetDailyRevenueWithOpenOrders() {
        DataStore.load();
        Client client = new Client("Петр", "Сидоров", "+79112223333", "Haval F7", "В456СЕ163");
        DataStore.addClient(client);
        DataStore.load();

        WorkOrder order = new WorkOrder(client);
        order.addService("Замена масла", 1500);
        order.setStatus(WorkOrder.STATUS_DRAFT);
        order.setCreatedDate(LocalDate.now().toString());
        DataStore.addOrder(order);
        DataStore.load();

        Map<String, Double> revenue = StatisticsService.getDailyRevenue(7);
        assertThat(revenue).isEmpty();
    }

    @Test
    @Order(4)
    void testGetMastersLoadEmpty() {
        DataStore.load();
        Map<String, Integer> load = StatisticsService.getMastersLoad();
        assertThat(load).hasSize(4);
        assertThat(load.values()).allMatch(v -> v == 0);
    }

    @Test
    @Order(5)
    void testGetMastersLoadWithAppointments() {
        DataStore.load();
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        DataStore.addClient(client);
        DataStore.load();

        Appointment app1 = new Appointment(client, "Иван", "Замена масла", "2024-06-01", "10:00");
        Appointment app2 = new Appointment(client, "Иван", "Диагностика", "2024-06-02", "14:00");
        Appointment app3 = new Appointment(client, "Петр", "Замена фильтров", "2024-06-03", "09:00");

        DataStore.addAppointment(app1);
        DataStore.addAppointment(app2);
        DataStore.addAppointment(app3);
        DataStore.load();

        Map<String, Integer> load = StatisticsService.getMastersLoad();
        assertThat(load.get("Иван")).isEqualTo(2);
        assertThat(load.get("Петр")).isEqualTo(1);
        assertThat(load.get("Сергей")).isEqualTo(0);
        assertThat(load.get("Антон")).isEqualTo(0);
    }

    @Test
    @Order(6)
    void testGetTopServicesEmpty() {
        DataStore.load();
        List<Map.Entry<String, Integer>> topServices = StatisticsService.getTopServices(5);
        assertThat(topServices).isEmpty();
    }

    @Test
    @Order(7)
    void testGetTopServicesWithOrders() {
        DataStore.load();
        Client client = new Client("Анна", "Иванова", "+79334445555", "Haval F5", "С789ЕЕ163");
        DataStore.addClient(client);
        DataStore.load();

        WorkOrder order1 = new WorkOrder(client);
        order1.addService("Замена масла", 1500);
        order1.addService("Диагностика", 1000);
        DataStore.addOrder(order1);
        DataStore.load();

        WorkOrder order2 = new WorkOrder(client);
        order2.addService("Замена масла", 1500);
        order2.addService("Замена фильтров", 500);
        DataStore.addOrder(order2);
        DataStore.load();

        List<Map.Entry<String, Integer>> topServices = StatisticsService.getTopServices(5);
        assertThat(topServices).hasSize(3);
        assertThat(topServices.get(0).getKey()).isEqualTo("Замена масла");
        assertThat(topServices.get(0).getValue()).isEqualTo(2);
    }

    @Test
    @Order(8)
    void testGetTopSparePartsWithOrders() {
        DataStore.load();
        Client client = new Client("Елена", "Кузнецова", "+79556667777", "Haval Big Dog", "Е456КХ163");
        DataStore.addClient(client);
        DataStore.load();

        SparePart oil = new SparePart("Масло моторное", 800, 1200, 20);
        SparePart filter = new SparePart("Фильтр масляный", 300, 500, 15);
        DataStore.addSparePart(oil);
        DataStore.addSparePart(filter);
        DataStore.load();

        WorkOrder order1 = new WorkOrder(client);
        order1.addSparePart(oil, 2);
        order1.addSparePart(filter, 1);
        DataStore.addOrder(order1);
        DataStore.load();

        WorkOrder order2 = new WorkOrder(client);
        order2.addSparePart(oil, 1);
        DataStore.addOrder(order2);
        DataStore.load();

        List<Map.Entry<String, Integer>> topParts = StatisticsService.getTopSpareParts(5);
        assertThat(topParts).hasSize(2);
        assertThat(topParts.get(0).getKey()).isEqualTo("Масло моторное");
        assertThat(topParts.get(0).getValue()).isEqualTo(2);
    }

    @Test
    @Order(9)
    void testGetStatusStatsWithOrders() {
        DataStore.load();
        Client client = new Client("Дмитрий", "Морозов", "+79667778888", "Haval Jolion", "А999ВВ163");
        DataStore.addClient(client);
        DataStore.load();

        WorkOrder order1 = new WorkOrder(client);
        order1.setStatus(WorkOrder.STATUS_DRAFT);
        DataStore.addOrder(order1);
        DataStore.load();

        WorkOrder order2 = new WorkOrder(client);
        order2.setStatus(WorkOrder.STATUS_IN_PROGRESS);
        DataStore.addOrder(order2);
        DataStore.load();

        WorkOrder order3 = new WorkOrder(client);
        order3.setStatus(WorkOrder.STATUS_CLOSED);
        DataStore.addOrder(order3);
        DataStore.load();

        Map<String, Integer> stats = StatisticsService.getStatusStats();
        assertThat(stats.get(WorkOrder.STATUS_DRAFT)).isEqualTo(1);
        assertThat(stats.get(WorkOrder.STATUS_IN_PROGRESS)).isEqualTo(1);
        assertThat(stats.get(WorkOrder.STATUS_CLOSED)).isEqualTo(1);
    }

    @Test
    @Order(10)
    void testGetDailyRevenueWithNullCreatedDate() {
        DataStore.load();
        Client client = new Client("Николай", "Соловьев", "+79556667788", "Mazda CX-5", "Т555НО163");
        DataStore.addClient(client);
        DataStore.load();

        WorkOrder order = new WorkOrder(client);
        order.addService("Замена масла", 1500);
        order.setStatus(WorkOrder.STATUS_CLOSED);
        order.setCreatedDate(null);
        DataStore.addOrder(order);
        DataStore.load();

        // При null createdDate БД подставляет текущую дату (как SQLiteDatabase),
        // поэтому заказ появляется в выручке за сегодня
        Map<String, Double> revenue = StatisticsService.getDailyRevenue(7);
        assertThat(revenue).isNotEmpty();
        assertThat(revenue.values().stream().mapToDouble(Double::doubleValue).sum()).isEqualTo(1500.0);
    }
}
