package com.autoservice.services;

import com.autoservice.*;
import com.autoservice.model.ReportData;
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
        order.setStatus(WorkOrder.STATUS_NEW);
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
        assertThat(load).hasSize(2);
        assertThat(load.values()).allMatch(v -> v == 0);
    }

    @Test
    @Order(5)
    void testGetMastersLoadWithAppointments() {
        DataStore.load();
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        DataStore.addClient(client);
        DataStore.load();

        Appointment app1 = new Appointment(client, "Саныч", "Замена масла", "2024-06-01", "10:00");
        Appointment app2 = new Appointment(client, "Саныч", "Диагностика", "2024-06-02", "14:00");
        Appointment app3 = new Appointment(client, "Малой", "Замена фильтров", "2024-06-03", "09:00");

        DataStore.addAppointment(app1);
        DataStore.addAppointment(app2);
        DataStore.addAppointment(app3);
        DataStore.load();

        Map<String, Integer> load = StatisticsService.getMastersLoad();
        assertThat(load.get("Саныч")).isEqualTo(2);
        assertThat(load.get("Малой")).isEqualTo(1);
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
        order1.setStatus(WorkOrder.STATUS_NEW);
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
        assertThat(stats.get(WorkOrder.STATUS_NEW)).isEqualTo(1);
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

    @Test
    @Order(11)
    void testBuildReportOnEmptyDataStore() {
        DataStore.load();

        ReportData data = StatisticsService.buildReport();

        assertThat(data.totalOrders()).isZero();
        assertThat(data.totalClients()).isZero();
        assertThat(data.totalSpareParts()).isZero();
        assertThat(data.totalAppointments()).isZero();
        assertThat(data.totalRevenue()).isZero();
        assertThat(data.newOrders()).isZero();
        assertThat(data.inProgressOrders()).isZero();
        assertThat(data.closedOrders()).isZero();
        assertThat(data.averageOrderValue()).isZero();
        assertThat(data.topServices()).isEmpty();
        assertThat(data.topParts()).isEmpty();
        assertThat(data.statusCount()).isEmpty();
        assertThat(data.monthlyRevenue()).isEmpty();
        assertThat(data.generatedDate())
                .matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}");
    }

    @Test
    @Order(12)
    void testBuildReportCountsStatusesAndRevenue() {
        DataStore.load();
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        DataStore.getClients().add(client);
        DataStore.getOrders().addAll(List.of(
                orderWithStatus(client, WorkOrder.STATUS_NEW),
                orderWithStatus(client, WorkOrder.STATUS_IN_PROGRESS),
                closedOrder(client, 1500),
                closedOrder(client, 2500),
                orderWithStatus(client, "Отменён"),
                orderWithStatus(client, null)
        ));

        ReportData data = StatisticsService.buildReport();

        assertThat(data.totalOrders()).isEqualTo(6);
        assertThat(data.totalClients()).isEqualTo(1);
        assertThat(data.newOrders()).isEqualTo(2);
        assertThat(data.inProgressOrders()).isEqualTo(1);
        assertThat(data.closedOrders()).isEqualTo(2);
        assertThat(data.totalRevenue()).isEqualTo(4000.0);
        // Неизвестный статус не попадает ни в одну из категорий и не влияет на выручку.
        assertThat(data.newOrders() + data.inProgressOrders() + data.closedOrders()).isEqualTo(5);
        assertThat(data.averageOrderValue()).isEqualTo(2000.0);
        assertThat(data.statusCount()).containsEntry("Отменён", 1);
    }

    @Test
    @Order(13)
    void testBuildReportCountsClientsSparePartsAndAppointments() {
        DataStore.load();
        DataStore.getClients().addAll(List.of(
                new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163"),
                new Client("Анна", "Смирнова", "+79002223333", "Haval F7", "В456СЕ163")
        ));
        DataStore.getSpareParts().addAll(List.of(
                new SparePart("Масло моторное", 800, 1200, 20),
                new SparePart("Фильтр масляный", 300, 500, 15),
                new SparePart("Колодки тормозные", 1000, 2000, 5)
        ));
        DataStore.getAppointments().add(new Appointment(
                new Client("Пётр", "Сидоров", "+79112223333", "Haval F7", "С789ЕЕ163"),
                "Саныч",
                "Замена масла",
                "2025-01-01",
                "10:00"
        ));

        ReportData data = StatisticsService.buildReport();

        assertThat(data.totalClients()).isEqualTo(2);
        assertThat(data.totalSpareParts()).isEqualTo(3);
        assertThat(data.totalAppointments()).isEqualTo(1);
    }

    @Test
    @Order(14)
    void testBuildReportBuildsTopServicesAndParts() {
        DataStore.load();
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        SparePart oil = new SparePart("Масло моторное", 800, 1200, 20);
        SparePart filter = new SparePart("Фильтр масляный", 300, 500, 15);

        WorkOrder order1 = new WorkOrder(client);
        order1.addService("Замена масла", 1500);
        order1.addService("Диагностика", 500);
        // Количество запчасти не должно увеличивать счётчик её упоминаний.
        order1.addSparePart(oil, 5);

        WorkOrder order2 = new WorkOrder(client);
        order2.addService("Замена масла", 1500);
        order2.addSparePart(oil, 1);
        order2.addSparePart(filter, 2);

        DataStore.getOrders().addAll(List.of(order1, order2));

        ReportData data = StatisticsService.buildReport();

        assertThat(data.topServices())
                .hasSize(2)
                .containsEntry("Замена масла", 2)
                .containsEntry("Диагностика", 1);
        assertThat(data.topServices().keySet())
                .containsExactly("Замена масла", "Диагностика");

        assertThat(data.topParts())
                .hasSize(2)
                .containsEntry("Масло моторное", 2)
                .containsEntry("Фильтр масляный", 1);
        assertThat(data.topParts().keySet())
                .containsExactly("Масло моторное", "Фильтр масляный");
    }

    @Test
    @Order(15)
    void testBuildReportLimitsTopServicesToFive() {
        DataStore.load();
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");

        for (int i = 1; i <= 6; i++) {
            WorkOrder order = new WorkOrder(client);
            order.addService("Услуга " + i, 100);
            DataStore.getOrders().add(order);
        }

        ReportData data = StatisticsService.buildReport();

        assertThat(data.topServices()).hasSize(5);
    }

    @Test
    @Order(16)
    void testBuildReportCalculatesMonthlyRevenueForClosedOrders() {
        DataStore.load();
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");

        WorkOrder january = closedOrder(client, 1500);
        january.setCreatedDate("2026-01-15");
        WorkOrder february = closedOrder(client, 2500);
        february.setCreatedDate("2026-02-10");
        WorkOrder februarySecond = closedOrder(client, 500);
        februarySecond.setCreatedDate("2026-02-20");
        WorkOrder open = orderWithStatus(client, WorkOrder.STATUS_NEW);
        open.setCreatedDate("2026-03-01");
        DataStore.getOrders().addAll(List.of(january, february, februarySecond, open));

        ReportData data = StatisticsService.buildReport();

        assertThat(data.monthlyRevenue())
                .containsExactly(
                        Map.entry("2026-01", 1500.0),
                        Map.entry("2026-02", 3000.0)
                );
    }

    private static WorkOrder orderWithStatus(Client client, String status) {
        WorkOrder order = new WorkOrder(client);
        order.setStatus(status);
        return order;
    }

    private static WorkOrder closedOrder(Client client, double amount) {
        WorkOrder order = new WorkOrder(client);
        order.addService("Услуга", amount);
        order.setStatus(WorkOrder.STATUS_CLOSED);
        return order;
    }
}
