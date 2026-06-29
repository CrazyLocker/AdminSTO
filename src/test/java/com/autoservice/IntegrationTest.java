package com.autoservice;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;

/**
 * Комплексные интеграционные тесты системы автосервиса
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTest {

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
        } catch (Exception e) {
            // Игнорируем
        }
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
    void testFullOrderCycle() {
        clearDatabase();
        
        Client client = new Client("Андрей Соколов Тест1", "+79001112233", "Haval Jolion", "А111ВС163");
        DataStore.addClient(client);
        DataStore.load();

        Client savedClient = DataStore.getClients().stream()
                .filter(c -> c.getName().equals("Андрей Соколов Тест1"))
                .findFirst()
                .orElse(null);
        assertThat(savedClient).isNotNull();

        Service oilChange = new Service("Замена масла Тест1", 1500);
        Service filterChange = new Service("Замена фильтра Тест1", 500);
        DataStore.addService(oilChange);
        DataStore.addService(filterChange);
        DataStore.load();

        assertThat(DataStore.getServices()).hasSize(2);

        SparePart oil = new SparePart("Масло Тест1", 800, 1200, 20);
        SparePart filter = new SparePart("Фильтр Тест1", 300, 500, 15);
        DataStore.addSparePart(oil);
        DataStore.addSparePart(filter);
        DataStore.load();

        assertThat(DataStore.getSpareParts()).hasSize(2);

        WorkOrder order = new WorkOrder(savedClient);
        order.addService("Замена масла Тест1", 1500);
        order.addService("Замена фильтра Тест1", 500);
        order.addSparePart(oil, 1);
        order.addSparePart(filter, 1);

        DataStore.addOrder(order);
        DataStore.load();

        WorkOrder savedOrder = DataStore.getOrders().stream()
                .filter(o -> o.getId().equals(order.getId()))
                .findFirst()
                .orElse(null);

        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getTotal()).isEqualTo(1500 + 500 + 1200 + 500);
        assertThat(savedOrder.getServices()).hasSize(2);
        assertThat(savedOrder.getSpareParts()).hasSize(2);

        Appointment appointment = new Appointment(
            savedClient, "Иван", "Замена масла Тест1",
            "2024-06-01", "10:00"
        );
        DataStore.addAppointment(appointment);
        DataStore.load();

        assertThat(DataStore.getAppointments()).hasSize(1);

        savedOrder.setStatus(WorkOrder.STATUS_IN_WORK);
        DataStore.updateOrder(savedOrder);
        DataStore.load();

        WorkOrder updatedOrder = DataStore.getOrders().stream()
                .filter(o -> o.getId().equals(savedOrder.getId()))
                .findFirst()
                .orElse(null);

        assertThat(updatedOrder.getStatus()).isEqualTo(WorkOrder.STATUS_IN_WORK);

        assertThat(DataStore.getActiveOrdersCount()).isEqualTo(1);

        updatedOrder.setStatus(WorkOrder.STATUS_CLOSED);
        DataStore.updateOrder(updatedOrder);
        DataStore.load();

        assertThat(DataStore.getActiveOrdersCount()).isEqualTo(0);
    }

    @Test
    @Order(2)
    void testMultipleClientsAndOrders() {
        clearDatabase();
        
        Client client1 = new Client("Иван Тест2", "+79001111111", "Haval F7", "А123ВС164");
        Client client2 = new Client("Олег Тест2", "+79002222222", "Haval F5", "В456СЕ164");
        Client client3 = new Client("Анна Тест2", "+79003333333", "Haval Dargo", "С789ЕЕ164");

        DataStore.addClient(client1);
        DataStore.addClient(client2);
        DataStore.addClient(client3);
        DataStore.load();

        assertThat(DataStore.getClients()).hasSize(3);

        DataStore.addService(new Service("Диагностика Тест2", 1000));
        DataStore.addService(new Service("Замена масла Тест2", 1500));
        DataStore.load();

        SparePart part1 = new SparePart("Масло Тест2", 800, 1200, 30);
        SparePart part2 = new SparePart("Фильтр Тест2", 300, 500, 25);
        DataStore.addSparePart(part1);
        DataStore.addSparePart(part2);
        DataStore.load();

        WorkOrder order1 = new WorkOrder(client1);
        order1.addService("Диагностика Тест2", 1000);
        DataStore.addOrder(order1);
        DataStore.load();

        WorkOrder order2 = new WorkOrder(client2);
        order2.addService("Замена масла Тест2", 1500);
        order2.addSparePart(part1, 1);
        DataStore.addOrder(order2);
        DataStore.load();

        WorkOrder order3 = new WorkOrder(client3);
        order3.addService("Диагностика Тест2", 1000);
        order3.addService("Замена масла Тест2", 1500);
        order3.addSparePart(part1, 1);
        order3.addSparePart(part2, 1);
        DataStore.addOrder(order3);
        DataStore.load();

        assertThat(DataStore.getOrders()).hasSize(3);

        WorkOrder o1 = DataStore.getOrders().stream()
                .filter(o -> o.getClient().getName().equals("Иван Тест2"))
                .findFirst().orElse(null);
        assertThat(o1.getTotal()).isEqualTo(1000);

        WorkOrder o2 = DataStore.getOrders().stream()
                .filter(o -> o.getClient().getName().equals("Олег Тест2"))
                .findFirst().orElse(null);
        assertThat(o2.getTotal()).isEqualTo(1500 + 1200);

        WorkOrder o3 = DataStore.getOrders().stream()
                .filter(o -> o.getClient().getName().equals("Анна Тест2"))
                .findFirst().orElse(null);
        assertThat(o3.getTotal()).isEqualTo(1000 + 1500 + 1200 + 500);
    }

    @Test
    @Order(3)
    void testAppointmentScheduling() {
        clearDatabase();
        
        Client client = new Client("Елена Тест3", "+79004444444", "Haval Big Dog", "М111НО164");
        DataStore.addClient(client);
        DataStore.load();

        Appointment app1 = new Appointment(client, "Иван", "Замена масла Тест3", "2024-07-01", "10:00");
        Appointment app2 = new Appointment(client, "Петр", "Диагностика Тест3", "2024-07-01", "14:00");
        Appointment app3 = new Appointment(client, "Иван", "Замена фильтров Тест3", "2024-07-02", "10:00");

        DataStore.addAppointment(app1);
        DataStore.addAppointment(app2);
        DataStore.addAppointment(app3);
        DataStore.load();

        assertThat(DataStore.getAppointments()).hasSize(3);

        List<Appointment> day1 = DataStore.getAppointmentsByDate("2024-07-01");
        assertThat(day1).hasSize(2);

        List<Appointment> day2 = DataStore.getAppointmentsByDate("2024-07-02");
        assertThat(day2).hasSize(1);

        Appointment appToUpdate = DataStore.getAppointments().get(0);
        appToUpdate.setStatus(Appointment.STATUS_COMPLETED);
        DataStore.updateAppointment(appToUpdate);
        DataStore.load();

        Appointment updated = DataStore.getAppointments().stream()
                .filter(a -> a.getId() == appToUpdate.getId())
                .findFirst().orElse(null);
        assertThat(updated.getStatus()).isEqualTo(Appointment.STATUS_COMPLETED);
    }

    @Test
    @Order(4)
    void testStockManagement() {
        clearDatabase();
        
        SparePart part1 = new SparePart("Масло Тест4", 800, 1200, 50);
        SparePart part2 = new SparePart("Фильтр Тест4", 300, 500, 40);
        SparePart part3 = new SparePart("Колодки Тест4", 1000, 2000, 30);

        DataStore.addSparePart(part1);
        DataStore.addSparePart(part2);
        DataStore.addSparePart(part3);
        DataStore.load();

        assertThat(DataStore.getSpareParts()).hasSize(3);

        Client client = new Client("Дмитрий Тест4", "+79005555555", "Haval Jolion", "Е222КХ164");
        DataStore.addClient(client);
        DataStore.load();

        WorkOrder order1 = new WorkOrder(client);
        order1.addSparePart(part1, 5);
        DataStore.addOrder(order1);
        DataStore.load();

        WorkOrder order2 = new WorkOrder(client);
        order2.addSparePart(part2, 3);
        order2.addSparePart(part3, 2);
        DataStore.addOrder(order2);
        DataStore.load();

        // Проверяем что заказы созданы
        assertThat(DataStore.getOrders()).hasSize(2);

        // Проверяем что запчасти существуют
        SparePart p1 = DataStore.getSpareParts().stream()
                .filter(p -> p.getName().equals("Масло Тест4"))
                .findFirst().orElse(null);
        assertThat(p1).isNotNull();

        SparePart p2 = DataStore.getSpareParts().stream()
                .filter(p -> p.getName().equals("Фильтр Тест4"))
                .findFirst().orElse(null);
        assertThat(p2).isNotNull();

        // Проверяем что у заказов есть запчасти
        // order1 — 1 запчасть (Масло Тест4, qty 5), order2 — 2 запчасти
        // Ищем конкретно order1 по первой добавленной запчасти
        WorkOrder o1 = DataStore.getOrders().stream()
                .filter(o -> o.getServices().isEmpty() && !o.getSpareParts().isEmpty())
                .filter(o -> o.getSpareParts().stream().anyMatch(p -> p.getName().equals("Масло Тест4")))
                .findFirst().orElse(null);
        assertThat(o1).isNotNull();
        assertThat(o1.getSpareParts()).hasSize(1);
        assertThat(o1.getSparePartQuantities().get(0)).isEqualTo(5);
    }

    @Test
    @Order(5)
    void testClientHistory() {
        clearDatabase();
        
        Client client = new Client("Сергей Тест5", "+79006666666", "Haval F7", "В333СС164");
        DataStore.addClient(client);
        DataStore.load();

        DataStore.addService(new Service("Замена масла Тест5", 1500));
        DataStore.addService(new Service("Диагностика Тест5", 1000));
        DataStore.load();

        for (int i = 0; i < 3; i++) {
            WorkOrder order = new WorkOrder(client);
            order.addService("Замена масла Тест5", 1500);
            order.setStatus(WorkOrder.STATUS_CLOSED);
            DataStore.addOrder(order);
            DataStore.load();
        }

        assertThat(DataStore.getOrders().stream()
                .filter(o -> o.getClient().getName().equals("Сергей Тест5"))
                .filter(o -> o.getStatus().equals(WorkOrder.STATUS_CLOSED))
                .count()).isEqualTo(3);
    }

    @Test
    @Order(6)
    void testOrderWithOnlyServices() {
        clearDatabase();
        
        Client client = new Client("Алексей Тест6", "+79007777777", "Haval F5", "С444ММ164");
        DataStore.addClient(client);
        DataStore.load();

        DataStore.addService(new Service("Компьютерная диагностика Тест6", 1500));
        DataStore.addService(new Service("Сброс сервисного интервала Тест6", 500));
        DataStore.load();

        WorkOrder order = new WorkOrder(client);
        order.addService("Компьютерная диагностика Тест6", 1500);
        order.addService("Сброс сервисного интервала Тест6", 500);

        DataStore.addOrder(order);
        DataStore.load();

        WorkOrder saved = DataStore.getOrders().get(0);
        assertThat(saved.getServices()).hasSize(2);
        assertThat(saved.getSpareParts()).isEmpty();
        assertThat(saved.getTotal()).isEqualTo(2000);
    }

    @Test
    @Order(7)
    void testOrderWithOnlyParts() {
        clearDatabase();
        
        Client client = new Client("Наталья Тест7", "+79008888888", "Haval Dargo", "М555НН164");
        DataStore.addClient(client);
        DataStore.load();

        SparePart oil = new SparePart("Масло Тест7", 800, 1200, 100);
        SparePart filter = new SparePart("Фильтр Тест7", 300, 500, 80);
        DataStore.addSparePart(oil);
        DataStore.addSparePart(filter);
        DataStore.load();

        WorkOrder order = new WorkOrder(client);
        order.addSparePart(oil, 2);
        order.addSparePart(filter, 2);

        DataStore.addOrder(order);
        DataStore.load();

        WorkOrder saved = DataStore.getOrders().get(0);
        assertThat(saved.getServices()).isEmpty();
        assertThat(saved.getSpareParts()).hasSize(2);
        assertThat(saved.getTotal()).isEqualTo(2400 + 1000);
    }

    @Test
    @Order(8)
    void testStatusTransitions() {
        clearDatabase();
        
        Client client = new Client("Виктор Тест8", "+79009999999", "Haval Big Dog", "Е666РР164");
        DataStore.addClient(client);
        DataStore.load();

        WorkOrder order = new WorkOrder(client);
        order.addService("Диагностика Тест8", 1000);
        DataStore.addOrder(order);
        DataStore.load();

        WorkOrder saved = DataStore.getOrders().get(0);

        assertThat(saved.getStatus()).isEqualTo(WorkOrder.STATUS_NEW);

        saved.setStatus(WorkOrder.STATUS_DIAGNOSTICS);
        DataStore.updateOrder(saved);
        DataStore.load();

        saved.setStatus(WorkOrder.STATUS_IN_WORK);
        DataStore.updateOrder(saved);
        DataStore.load();

        saved.setStatus(WorkOrder.STATUS_WAITING_PARTS);
        DataStore.updateOrder(saved);
        DataStore.load();

        saved.setStatus(WorkOrder.STATUS_READY);
        DataStore.updateOrder(saved);
        DataStore.load();

        saved.setStatus(WorkOrder.STATUS_CLOSED);
        DataStore.updateOrder(saved);
        DataStore.load();

        WorkOrder finalOrder = DataStore.getOrders().get(0);
        assertThat(finalOrder.getStatus()).isEqualTo(WorkOrder.STATUS_CLOSED);
    }
}