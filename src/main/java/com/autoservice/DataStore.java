package com.autoservice;

import java.util.ArrayList;
import java.util.List;

public class DataStore {
    private static List<Client> clients = new ArrayList<>();
    private static List<WorkOrder> orders = new ArrayList<>();
    private static List<Service> services = new ArrayList<>();
    private static List<SparePart> spareParts = new ArrayList<>();
    private static List<Appointment> appointments = new ArrayList<>();

    public static void load() {
        clients = Database.getAllClients();
        services = Database.getAllServices();
        spareParts = Database.getAllSpareParts();
        orders = Database.getAllOrders();
        appointments = Database.getAllAppointments();

        System.out.println("DataStore loaded: " + clients.size() + " clients, " +
                orders.size() + " orders, " + services.size() + " services, " +
                spareParts.size() + " spare parts, " + appointments.size() + " appointments");
    }

    public static void save() {
        for (WorkOrder order : orders) {
            Database.updateOrder(order);
        }
        for (Client client : clients) {
            Database.updateClient(client);
        }
        for (Appointment a : appointments) {
            Database.updateAppointment(a);
        }
        for (SparePart sp : spareParts) {
            Database.updateSparePartStock(sp, sp.getStock());
        }
        System.out.println("Data saved to DB");
    }

    // ==================== CLIENTS ====================

    public static List<Client> getClients() { return clients; }

    public static void addClient(Client c) {
        Database.addClient(c);
        // Инкрементальное обновление вместо полной перезагрузки
        Client lastClient = Database.getClientById(getLastClientId());
        if (lastClient != null) {
            clients.add(lastClient);
        } else {
            clients = Database.getAllClients(); // fallback
        }
    }

    private static int getLastClientId() {
        int maxId = 0;
        for (Client c : clients) {
            if (c.getId() > maxId) maxId = c.getId();
        }
        // Проверяем в БД, возможно там есть новые записи
        List<Client> all = Database.getAllClients();
        for (Client c : all) {
            if (c.getId() > maxId) maxId = c.getId();
        }
        return maxId;
    }

    public static void updateClient(Client client) {
        Database.updateClient(client);
        // Обновляем в кеше
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).getId() == client.getId()) {
                clients.set(i, client);
                return;
            }
        }
        // Если не нашли — перезагружаем
        clients = Database.getAllClients();
    }

    public static void removeClient(Client c) {
        Database.deleteClient(c);
        clients.removeIf(client -> client.getId() == c.getId());
    }

    public static void deleteClient(Client client) {
        List<WorkOrder> ordersToDelete = new ArrayList<>();
        for (WorkOrder order : orders) {
            if (order.getClient().getId() == client.getId()) {
                ordersToDelete.add(order);
            }
        }
        for (WorkOrder order : ordersToDelete) {
            Database.deleteOrder(order.getId());
            orders.remove(order);
        }

        Database.deleteClient(client);
        clients.removeIf(c -> c.getId() == client.getId());
    }

    // ==================== ORDERS ====================

    public static List<WorkOrder> getOrders() { return orders; }

    public static void addOrder(WorkOrder o) {
        Database.addOrder(o);
        orders = Database.getAllOrders();
        System.out.println("Orders after add: " + orders.size());
    }

    public static void updateOrder(WorkOrder o) {
        Database.updateOrder(o);
        // Обновляем в кеше
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i).getId().equals(o.getId())) {
                orders.set(i, o);
                return;
            }
        }
        // Если не нашли — перезагружаем
        orders = Database.getAllOrders();
    }

    public static void deleteOrder(WorkOrder order) {
        String orderId = order.getId();
        if (orderId != null && !orderId.isEmpty()) {
            Database.deleteOrder(orderId);
            orders.removeIf(o -> o.getId().equals(orderId));
        } else {
            System.err.println("Cannot delete order with ID=" + orderId);
        }
    }

    public static int getActiveOrdersCount() {
        int count = 0;
        for (WorkOrder order : orders) {
            if (!order.getStatus().equals(WorkOrder.STATUS_CLOSED)) {
                count++;
            }
        }
        return count;
    }

    // ==================== SERVICES ====================

    public static List<Service> getServices() { return services; }

    public static void addService(Service s) {
        Database.addService(s);
        services = Database.getAllServices();
    }

    public static void removeService(Service s) {
        Database.deleteService(s);
        services.removeIf(service -> service.getName().equals(s.getName()));
    }

    // ==================== SPARE PARTS ====================

    public static List<SparePart> getSpareParts() { return spareParts; }

    public static void addSparePart(SparePart sp) {
        Database.addSparePart(sp);
        spareParts = Database.getAllSpareParts();
    }

    public static void removeSparePart(SparePart sp) {
        Database.deleteSparePart(sp);
        spareParts.removeIf(part -> part.getId() == sp.getId());
    }

    public static void updateSparePartStock(SparePart part, int newStock) {
        Database.updateSparePartStock(part, newStock);
        // Обновляем в кеше
        for (SparePart sp : spareParts) {
            if (sp.getId() == part.getId()) {
                sp.setStock(newStock);
                return;
            }
        }
        // Если не нашли — перезагружаем
        spareParts = Database.getAllSpareParts();
    }

    // ==================== APPOINTMENTS ====================

    public static List<Appointment> getAppointments() {
        return appointments;
    }

    public static List<Appointment> getAppointmentsByDate(String date) {
        return Database.getAppointmentsByDate(date);
    }

    public static void addAppointment(Appointment a) {
        Database.addAppointment(a);
        appointments = Database.getAllAppointments();
    }

    public static void updateAppointment(Appointment a) {
        Database.updateAppointment(a);
        // Обновляем в кеше
        for (int i = 0; i < appointments.size(); i++) {
            if (appointments.get(i).getId() == a.getId()) {
                appointments.set(i, a);
                return;
            }
        }
        appointments = Database.getAllAppointments();
    }

    public static void deleteAppointment(int id) {
        Database.deleteAppointment(id);
        appointments.removeIf(a -> a.getId() == id);
    }
}