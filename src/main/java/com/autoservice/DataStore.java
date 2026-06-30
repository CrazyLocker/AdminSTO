package com.autoservice;

import java.util.ArrayList;
import java.util.List;

public class DataStore {
    private static List<Client> clients = new ArrayList<>();
    private static List<WorkOrder> orders = new ArrayList<>();
    private static List<Service> services = new ArrayList<>();
    private static List<SparePart> spareParts = new ArrayList<>();
    private static List<Appointment> appointments = new ArrayList<>();

    // Флаг, что данные были изменены
    private static boolean isDirty = false;

    public static void load() {
        clients = Database.getAllClients();
        services = Database.getAllServices();
        spareParts = Database.getAllSpareParts();
        orders = Database.getAllOrders();
        appointments = Database.getAllAppointments();
        isDirty = false;
        System.out.println("DataStore loaded: " + clients.size() + " clients, " +
                orders.size() + " orders, " + services.size() + " services, " +
                spareParts.size() + " spare parts, " + appointments.size() + " appointments");
    }

    public static void save() {
        if (!isDirty) {
            System.out.println("No changes to save");
            return;
        }

        System.out.println("Saving changes...");
        long startTime = System.currentTimeMillis();

        // Сохраняем только изменённые заказы
        for (WorkOrder order : orders) {
            if (order.isDirty()) {
                Database.updateOrder(order);
                order.setDirty(false);
            }
        }

        // Сохраняем только изменённых клиентов
        for (Client client : clients) {
            if (client.isDirty()) {
                Database.updateClient(client);
                client.setDirty(false);
            }
        }

        // Сохраняем только изменённые записи
        for (Appointment a : appointments) {
            if (a.isDirty()) {
                Database.updateAppointment(a);
                a.setDirty(false);
            }
        }

        // Сохраняем только изменённые запчасти
        for (SparePart sp : spareParts) {
            if (sp.isDirty()) {
                Database.updateSparePartStock(sp, sp.getStock());
                sp.setDirty(false);
            }
        }

        isDirty = false;
        long endTime = System.currentTimeMillis();
        System.out.println("Data saved to DB in " + (endTime - startTime) + " ms");
    }

    public static void markDirty() {
        isDirty = true;
    }

    // ==================== CLIENTS ====================

    public static List<Client> getClients() { return clients; }

    public static void addClient(Client c) {
        Database.addClient(c);
        clients = Database.getAllClients();
        isDirty = true;
    }

    public static void updateClient(Client client) {
        client.setDirty(true);
        Database.updateClient(client);
        clients = Database.getAllClients();
        isDirty = true;
    }

    public static void removeClient(Client c) {
        Database.deleteClient(c);
        clients = Database.getAllClients();
        isDirty = true;
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
        clients.remove(client);
        isDirty = true;
    }

    // ==================== ORDERS ====================

    public static List<WorkOrder> getOrders() { return orders; }

    public static void addOrder(WorkOrder o) {
        Database.addOrder(o);
        orders = Database.getAllOrders();
        isDirty = true;
        System.out.println("Orders after add: " + orders.size());
    }

    public static void updateOrder(WorkOrder o) {
        o.setDirty(true);
        Database.updateOrder(o);
        orders = Database.getAllOrders();
        isDirty = true;
    }

    public static void deleteOrder(WorkOrder order) {
        String orderId = order.getId();
        if (orderId != null && !orderId.isEmpty()) {
            Database.deleteOrder(orderId);
            orders.removeIf(o -> o.getId().equals(orderId));
            isDirty = true;
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
        isDirty = true;
    }

    public static void removeService(Service s) {
        Database.deleteService(s);
        services = Database.getAllServices();
        isDirty = true;
    }

    // ==================== SPARE PARTS ====================

    public static List<SparePart> getSpareParts() { return spareParts; }

    public static void addSparePart(SparePart sp) {
        Database.addSparePart(sp);
        spareParts = Database.getAllSpareParts();
        isDirty = true;
    }

    public static void removeSparePart(SparePart sp) {
        Database.deleteSparePart(sp);
        spareParts = Database.getAllSpareParts();
        isDirty = true;
    }

    public static void updateSparePartStock(SparePart part, int newStock) {
        part.setDirty(true);
        Database.updateSparePartStock(part, newStock);
        spareParts = Database.getAllSpareParts();
        isDirty = true;
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
        isDirty = true;
    }

    public static void updateAppointment(Appointment a) {
        a.setDirty(true);
        Database.updateAppointment(a);
        appointments = Database.getAllAppointments();
        isDirty = true;
    }

    public static void deleteAppointment(int id) {
        Database.deleteAppointment(id);
        appointments.removeIf(a -> a.getId() == id);
        isDirty = true;
    }
}