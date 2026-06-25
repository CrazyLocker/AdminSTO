package com.autoservice;

import com.autoservice.services.StatisticsService;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

import java.util.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StatisticsServiceTest {

    @BeforeAll
    static void setup() {
        Database.initForTest();
    }

    @AfterAll
    static void cleanup() {
        Database.close();
        try {
            java.io.File f = new java.io.File("test.db");
            f.delete();
        } catch (Exception e) {}
    }

    private static void clearDatabase() {
        try {
            var stmt = Database.getConnection().createStatement();
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
            System.err.println("Очистка БД: " + e.getMessage());
        }
        DataStore.load();
    }

    @Test
    @Order(1)
    void testGetDailyRevenueEmpty() {
        clearDatabase();
        Map<String, Double> revenue = StatisticsService.getDailyRevenue(7);
        assertThat(revenue.size()).isEqualTo(7);
        for (double v : revenue.values()) assertThat(v).isEqualTo(0.0);
    }

    @Test
    @Order(2)
    void testGetDailyRevenueWithClosedOrders() {
        clearDatabase();
        Client client = new Client("Клиент Доход", "+79001111111", "Haval Jolion", "А111ВС163");
        DataStore.addClient(client);
        DataStore.load();
        WorkOrder order1 = new WorkOrder(client);
        order1.addService("Услуга 1", 1500);
        DataStore.addOrder(order1);
        DataStore.load();
        WorkOrder saved = DataStore.getOrders().get(0);
        saved.setStatus(WorkOrder.STATUS_CLOSED);
        DataStore.updateOrder(saved);
        DataStore.load();
        Map<String, Double> revenue = StatisticsService.getDailyRevenue(30);
        double total = revenue.values().stream().mapToDouble(Double::doubleValue).sum();
        assertThat(total).isGreaterThan(0);
    }

    @Test
    @Order(3)
    void testGetDailyRevenueExcludesNewOrders() {
        clearDatabase();
        Client client = new Client("Клиент Новый", "+79002222222", "Haval F7", "Б222СС163");
        DataStore.addClient(client);
        DataStore.load();
        WorkOrder order = new WorkOrder(client);
        order.addService("Услуга 1", 5000);
        DataStore.addOrder(order);
        DataStore.load();
        Map<String, Double> revenue = StatisticsService.getDailyRevenue(30);
        double total = revenue.values().stream().mapToDouble(Double::doubleValue).sum();
        assertThat(total).isEqualTo(0.0);
    }

    @Test
    @Order(4)
    void testGetMastersLoadEmpty() {
        clearDatabase();
        Map<String, Integer> load = StatisticsService.getMastersLoad();
        assertThat(load.get("Иван")).isEqualTo(0);
        assertThat(load.get("Петр")).isEqualTo(0);
        assertThat(load.get("Сергей")).isEqualTo(0);
        assertThat(load.get("Антон")).isEqualTo(0);
    }

    @Test
    @Order(5)
    void testGetMastersLoadWithAppointments() {
        clearDatabase();
        Client client = new Client("Клиент Записи", "+79003333333", "Haval Dargo", "В333ЕЕ163");
        DataStore.addClient(client);
        DataStore.load();
        DataStore.addAppointment(new Appointment(client, "Иван", "Замена масла", "2024-06-01", "10:00"));
        DataStore.addAppointment(new Appointment(client, "Иван", "Диагностика", "2024-06-02", "11:00"));
        DataStore.addAppointment(new Appointment(client, "Петр", "Замена масла", "2024-06-03", "14:00"));
        DataStore.addAppointment(new Appointment(client, "Сергей", "Замена масла", "2024-06-04", "09:00"));
        DataStore.load();
        Map<String, Integer> load = StatisticsService.getMastersLoad();
        assertThat(load.get("Иван")).isEqualTo(2);
        assertThat(load.get("Петр")).isEqualTo(1);
        assertThat(load.get("Сергей")).isEqualTo(1);
        assertThat(load.get("Антон")).isEqualTo(0);
    }

    @Test
    @Order(6)
    void testGetMastersLoadUnknownMaster() {
        clearDatabase();
        Client client = new Client("Клиент Неизвестный", "+79004444444", "Haval Big Dog", "Г444КХ163");
        DataStore.addClient(client);
        DataStore.load();
        DataStore.addAppointment(new Appointment(client, "НеизвестныйМастер", "Услуга", "2024-06-01", "10:00"));
        DataStore.load();
        Map<String, Integer> load = StatisticsService.getMastersLoad();
        assertThat(load.get("НеизвестныйМастер")).isNull();
        assertThat(load.get("Иван")).isEqualTo(0);
        assertThat(load.get("Петр")).isEqualTo(0);
        assertThat(load.get("Сергей")).isEqualTo(0);
        assertThat(load.get("Антон")).isEqualTo(0);
    }

    @Test
    @Order(7)
    void testGetTopServicesEmpty() {
        clearDatabase();
        List<Map.Entry<String, Integer>> top = StatisticsService.getTopServices(5);
        assertThat(top).isEmpty();
    }

    @Test
    @Order(8)
    void testGetTopServicesWithOrders() {
        clearDatabase();
        Client client = new Client("Клиент ТопУслуги", "+79005555555", "Haval Jolion", "Д555НО163");
        DataStore.addClient(client);
        DataStore.load();
        WorkOrder order1 = new WorkOrder(client);
        order1.addService("Замена масла", 1500);
        order1.addService("Диагностика", 1000);
        order1.addService("Замена масла", 1500);
        DataStore.addOrder(order1);
        DataStore.load();
        WorkOrder order2 = new WorkOrder(client);
        order2.addService("Замена масла", 1500);
        order2.addService("Замена фильтров", 500);
        DataStore.addOrder(order2);
        DataStore.load();
        List<Map.Entry<String, Integer>> top = StatisticsService.getTopServices(5);
        assertThat(top).hasSize(3);
        assertThat(top.get(0).getKey()).isEqualTo("Замена масла");
        assertThat(top.get(0).getValue()).isEqualTo(3);
    }

    @Test
    @Order(9)
    void testGetTopServicesLimit() {
        clearDatabase();
        Client client = new Client("Клиент Лимит", "+79006666666", "Haval F7", "Е666РР163");
        DataStore.addClient(client);
        DataStore.load();
        WorkOrder order = new WorkOrder(client);
        order.addService("Услуга 1", 100);
        order.addService("Услуга 2", 200);
        order.addService("Услуга 3", 300);
        order.addService("Услуга 4", 400);
        order.addService("Услуга 5", 500);
        DataStore.addOrder(order);
        DataStore.load();
        assertThat(StatisticsService.getTopServices(3)).hasSize(3);
        assertThat(StatisticsService.getTopServices(10)).hasSize(5);
    }

    @Test
    @Order(10)
    void testGetTopSparePartsEmpty() {
        clearDatabase();
        assertThat(StatisticsService.getTopSpareParts(5)).isEmpty();
    }

    @Test
    @Order(11)
    void testGetTopSparePartsWithOrders() {
        clearDatabase();
        Client client = new Client("Клиент ТопЗапчасти", "+79007777777", "Haval F5", "Ж777СС163");
        DataStore.addClient(client);
        DataStore.load();
        SparePart oil = new SparePart("Масло", 800, 1200, 50);
        SparePart filter = new SparePart("Фильтр", 300, 500, 40);
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
        List<Map.Entry<String, Integer>> top = StatisticsService.getTopSpareParts(5);
        assertThat(top).hasSize(2);
        assertThat(top.get(0).getKey()).isEqualTo("Масло");
        assertThat(top.get(0).getValue()).isEqualTo(2);
    }

    @Test
    @Order(12)
    void testGetTopSparePartsLimit() {
        clearDatabase();
        Client client = new Client("Клиент ЛимитЗапчасти", "+79008888888", "Haval Dargo", "З888ММ163");
        DataStore.addClient(client);
        DataStore.load();
        for (int i = 1; i <= 4; i++) {
            SparePart p = new SparePart("Запчасть" + i, 100, 200, 10);
            DataStore.addSparePart(p);
        }
        DataStore.load();
        List<SparePart> parts = DataStore.getSpareParts();
        WorkOrder order = new WorkOrder(client);
        for (SparePart p : parts) order.addSparePart(p, 1);
        DataStore.addOrder(order);
        DataStore.load();
        assertThat(StatisticsService.getTopSpareParts(2)).hasSize(2);
        assertThat(StatisticsService.getTopSpareParts(10)).hasSize(4);
    }

    @Test
    @Order(13)
    void testGetStatusStatsEmpty() {
        clearDatabase();
        assertThat(StatisticsService.getStatusStats()).isEmpty();
    }

    @Test
    @Order(14)
    void testGetStatusStatsWithOrders() {
        clearDatabase();
        Client client = new Client("Клиент Статусы", "+79009999999", "Haval Big Dog", "И999ТТ163");
        DataStore.addClient(client);
        DataStore.load();
        WorkOrder newOrder = new WorkOrder(client);
        DataStore.addOrder(newOrder);
        DataStore.load();
        WorkOrder iwOrder = new WorkOrder(client);
        iwOrder.addService("Услуга", 1000);
        DataStore.addOrder(iwOrder);
        DataStore.load();
        List<WorkOrder> all = DataStore.getOrders();
        for (WorkOrder o : all) {
            if (!o.getId().equals(newOrder.getId())) {
                o.setStatus(WorkOrder.STATUS_IN_WORK);
                DataStore.updateOrder(o);
                break;
            }
        }
        DataStore.load();
        Map<String, Integer> stats = StatisticsService.getStatusStats();
        assertThat(stats.getOrDefault(WorkOrder.STATUS_NEW, 0)).isGreaterThanOrEqualTo(1);
        assertThat(stats.getOrDefault(WorkOrder.STATUS_IN_WORK, 0)).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(15)
    void testGetDailyRevenueWithZeroTotalOrder() {
        clearDatabase();
        Client client = new Client("Клиент Ноль", "+79000000001", "Haval Jolion", "К001АА163");
        DataStore.addClient(client);
        DataStore.load();
        WorkOrder order = new WorkOrder(client);
        order.setStatus(WorkOrder.STATUS_CLOSED);
        DataStore.addOrder(order);
        DataStore.load();
        double total = StatisticsService.getDailyRevenue(30).values().stream().mapToDouble(Double::doubleValue).sum();
        assertThat(total).isEqualTo(0.0);
    }

    @Test
    @Order(16)
    void testGetDailyRevenuePeriod7Days() {
        clearDatabase();
        assertThat(StatisticsService.getDailyRevenue(7).size()).isEqualTo(7);
    }

    @Test
    @Order(17)
    void testGetDailyRevenuePeriod90Days() {
        clearDatabase();
        assertThat(StatisticsService.getDailyRevenue(90).size()).isEqualTo(90);
    }

    @Test
    @Order(18)
    void testGetTopServicesMultipleOrdersSameService() {
        clearDatabase();
        Client client = new Client("Клиент ПовторУслуга", "+79000000002", "Haval F7", "К002ББ163");
        DataStore.addClient(client);
        DataStore.load();
        for (int i = 0; i < 5; i++) {
            WorkOrder order = new WorkOrder(client);
            order.addService("Замена масла", 1500);
            DataStore.addOrder(order);
            DataStore.load();
        }
        List<Map.Entry<String, Integer>> top = StatisticsService.getTopServices(10);
        assertThat(top).hasSize(1);
        assertThat(top.get(0).getValue()).isEqualTo(5);
    }

    @Test
    @Order(19)
    void testGetTopSparePartsMultipleOrdersSamePart() {
        clearDatabase();
        Client client = new Client("Клиент ПовторЗапчасть", "+79000000003", "Haval Dargo", "К003ВВ163");
        DataStore.addClient(client);
        DataStore.load();
        SparePart oil = new SparePart("Масло", 800, 1200, 100);
        DataStore.addSparePart(oil);
        DataStore.load();
        for (int i = 0; i < 4; i++) {
            WorkOrder order = new WorkOrder(client);
            order.addSparePart(oil, 1);
            DataStore.addOrder(order);
            DataStore.load();
        }
        List<Map.Entry<String, Integer>> top = StatisticsService.getTopSpareParts(10);
        assertThat(top).hasSize(1);
        assertThat(top.get(0).getValue()).isEqualTo(4);
    }

    @Test
    @Order(20)
    void testGetStatusStatsMultipleStatuses() {
        clearDatabase();
        Client client = new Client("Клиент МногоСтатусов", "+79000000004", "Haval Jolion", "К004ГГ163");
        DataStore.addClient(client);
        DataStore.load();
        for (int i = 0; i < 4; i++) {
            WorkOrder order = new WorkOrder(client);
            order.addService("Услуга", 1000 * (i + 1));
            DataStore.addOrder(order);
            DataStore.load();
        }
        List<WorkOrder> orders = DataStore.getOrders();
        assertThat(orders).hasSize(4);
        orders.get(0).setStatus(WorkOrder.STATUS_NEW);
        orders.get(1).setStatus(WorkOrder.STATUS_DIAGNOSTICS);
        orders.get(2).setStatus(WorkOrder.STATUS_WAITING_PARTS);
        orders.get(3).setStatus(WorkOrder.STATUS_READY);
        for (WorkOrder o : orders) DataStore.updateOrder(o);
        DataStore.load();
        Map<String, Integer> stats = StatisticsService.getStatusStats();
        assertThat(stats.keySet()).contains(
                WorkOrder.STATUS_NEW,
                WorkOrder.STATUS_DIAGNOSTICS,
                WorkOrder.STATUS_WAITING_PARTS,
                WorkOrder.STATUS_READY
        );
    }
}
