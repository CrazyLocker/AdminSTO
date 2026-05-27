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

        System.out.println("DataStore загружен: " + clients.size() + " клиентов, " +
                orders.size() + " заказов, " + services.size() + " услуг, " +
                spareParts.size() + " запчастей, " + appointments.size() + " записей");
    }

    public static void save() {
        System.out.println("Данные сохранены в БД");
    }

    // ==================== КЛИЕНТЫ ====================

    public static List<Client> getClients() { return clients; }

    public static void addClient(Client c) {
        Database.addClient(c);
        clients = Database.getAllClients();
    }

    public static void updateClient(Client client) {
        Database.updateClient(client);
        clients = Database.getAllClients();
    }

    public static void removeClient(Client c) {
        Database.deleteClient(c);
        clients = Database.getAllClients();
    }

    // ==================== ЗАКАЗЫ ====================

    public static List<WorkOrder> getOrders() { return orders; }

    public static void addOrder(WorkOrder o) {
        Database.addOrder(o);
        orders = Database.getAllOrders();
    }

    public static void updateOrder(WorkOrder o) {
        Database.updateOrder(o);
        orders = Database.getAllOrders();
    }

    public static void deleteOrder(WorkOrder order) {
        String orderId = order.getId();
        if (orderId != null && !orderId.isEmpty()) {
            Database.deleteOrder(orderId);
            orders = Database.getAllOrders();
        } else {
            System.err.println("Невозможно удалить заказ с ID=" + orderId);
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

    // ==================== УСЛУГИ ====================

    public static List<Service> getServices() { return services; }

    public static void addService(Service s) {
        Database.addService(s);
        services = Database.getAllServices();
    }

    public static void removeService(Service s) {
        Database.deleteService(s);
        services = Database.getAllServices();
    }

    // ==================== ЗАПЧАСТИ ====================

    public static List<SparePart> getSpareParts() { return spareParts; }

    public static void addSparePart(SparePart sp) {
        Database.addSparePart(sp);
        spareParts = Database.getAllSpareParts();
    }

    public static void removeSparePart(SparePart sp) {
        Database.deleteSparePart(sp);
        spareParts = Database.getAllSpareParts();
    }

    public static void updateSparePartStock(SparePart part, int newStock) {
        Database.updateSparePartStock(part, newStock);
        spareParts = Database.getAllSpareParts();
    }

    // ==================== ЗАПИСИ ====================

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
        appointments = Database.getAllAppointments();
    }

    public static void deleteAppointment(int id) {
        Database.deleteAppointment(id);
        appointments = Database.getAllAppointments();
    }
}