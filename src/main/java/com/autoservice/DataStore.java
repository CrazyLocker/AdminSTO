package com.autoservice;

import com.autoservice.model.ServiceSparePart;
import com.autoservice.model.ToPart;
import com.autoservice.model.Setting;
import com.autoservice.services.AutoAddSparePartService;

import java.util.ArrayList;
import java.util.List;

public class DataStore {
    private static List<Client> clients = new ArrayList<>();
    private static List<WorkOrder> orders = new ArrayList<>();
    private static List<Service> services = new ArrayList<>();
    private static List<SparePart> spareParts = new ArrayList<>();
    private static List<Appointment> appointments = new ArrayList<>();
    private static List<ServiceSparePart> serviceSpareParts = new ArrayList<>();
    private static List<ToPart> toParts = new ArrayList<>();
    private static List<Setting> settings = new ArrayList<>();

    private static boolean isDirty = false;

    public static void load() {
        clients = DatabaseFactory.getDatabase().getAllClients();
        services = DatabaseFactory.getDatabase().getAllServices();
        spareParts = DatabaseFactory.getDatabase().getAllSpareParts();
        orders = DatabaseFactory.getDatabase().getAllOrders();
        appointments = DatabaseFactory.getDatabase().getAllAppointments();
        serviceSpareParts = DatabaseFactory.getDatabase().getServiceSparePartsByServiceId(-1);
        toParts = DatabaseFactory.getDatabase().getToPartsByCarModel("");
        settings = DatabaseFactory.getDatabase().getAllSettings();
        isDirty = false;
        System.out.println("DataStore loaded: " + clients.size() + " clients, " +
                orders.size() + " orders, " + services.size() + " services, " +
                spareParts.size() + " spare parts, " + appointments.size() + " appointments, " +
                serviceSpareParts.size() + " service-spare part relations, " +
                toParts.size() + " to parts, " + settings.size() + " settings");
    }

    public static void save() {
        if (!isDirty) {
            System.out.println("No changes to save");
            return;
        }

        System.out.println("Saving changes...");
        long startTime = System.currentTimeMillis();

        int saved = 0;

        for (WorkOrder order : orders) {
            if (order.isDirty()) {
                Database.updateOrder(order);
                order.setDirty(false);
                saved++;
            }
        }

        for (Client client : clients) {
            if (client.isDirty()) {
                Database.updateClient(client);
                client.setDirty(false);
                saved++;
            }
        }

        for (Appointment a : appointments) {
            if (a.isDirty()) {
                Database.updateAppointment(a);
                a.setDirty(false);
                saved++;
            }
        }

        for (SparePart sp : spareParts) {
            if (sp.isDirty()) {
                Database.updateSparePartStock(sp, sp.getStock());
                sp.setDirty(false);
                saved++;
            }
        }

        for (ServiceSparePart ssp : serviceSpareParts) {
            if (ssp.isDirty()) {
                Database.updateServiceSparePart(ssp);
                ssp.setDirty(false);
                saved++;
            }
        }

        for (ToPart tp : toParts) {
            if (tp.isDirty()) {
                Database.updateToPart(tp);
                tp.setDirty(false);
                saved++;
            }
        }

        for (Setting setting : settings) {
            if (setting.isDirty()) {
                Database.updateSetting(setting);
                setting.markClean();
                saved++;
            }
        }

        isDirty = false;
        long endTime = System.currentTimeMillis();
        System.out.println("Saved " + saved + " items to DB in " + (endTime - startTime) + " ms");
    }

    public static void markDirty() {
        isDirty = true;
    }

    // ==================== CLIENTS ====================

    public static List<Client> getClients() { return clients; }

    public static void addClient(Client c) {
        DatabaseFactory.getDatabase().addClient(c);
        clients = DatabaseFactory.getDatabase().getAllClients();
        isDirty = true;
    }

    public static void updateClient(Client client) {
        client.setDirty(true);
        DatabaseFactory.getDatabase().updateClient(client);
        clients = DatabaseFactory.getDatabase().getAllClients();
        isDirty = true;
    }

    public static void removeClient(Client c) {
        DatabaseFactory.getDatabase().deleteClient(c);
        clients = DatabaseFactory.getDatabase().getAllClients();
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
            DatabaseFactory.getDatabase().deleteOrder(order.getId());
            orders.remove(order);
        }

        DatabaseFactory.getDatabase().deleteClient(client);
        clients.remove(client);
        isDirty = true;
    }

    // ==================== ORDERS ====================

    public static List<WorkOrder> getOrders() { return orders; }

    public static void addOrder(WorkOrder o) {
        DatabaseFactory.getDatabase().addOrder(o);
        orders = DatabaseFactory.getDatabase().getAllOrders();
        isDirty = true;
        System.out.println("Orders after add: " + orders.size());
    }

    public static void updateOrder(WorkOrder o) {
        o.setDirty(true);
        DatabaseFactory.getDatabase().updateOrder(o);
        orders = DatabaseFactory.getDatabase().getAllOrders();
        isDirty = true;
    }

    public static void deleteOrder(WorkOrder order) {
        String orderId = order.getId();
        if (orderId != null && !orderId.isEmpty()) {
            DatabaseFactory.getDatabase().deleteOrder(orderId);
            orders.removeIf(o -> o.getId().equals(orderId));
            isDirty = true;
        } else {
            System.err.println("Cannot delete order with ID=" + orderId);
        }
    }

    public static int getActiveOrdersCount() {
        int count = 0;
        for (WorkOrder order : orders) {
            if (!WorkOrder.STATUS_CLOSED.equals(order.getStatus())) {
                count++;
            }
        }
        return count;
    }

    // ==================== SERVICES ====================

    public static List<Service> getServices() { return services; }

    public static void addService(Service s) {
        DatabaseFactory.getDatabase().addService(s);
        services = DatabaseFactory.getDatabase().getAllServices();
        isDirty = true;
    }

    public static void removeService(Service s) {
        DatabaseFactory.getDatabase().deleteService(s);
        services = DatabaseFactory.getDatabase().getAllServices();
        isDirty = true;
    }

    public static Service getServiceByName(String name) {
        return services.stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public static SparePart getSparePartById(int id) {
        return spareParts.stream()
                .filter(s -> s.getId() == id)
                .findFirst()
                .orElse(null);
    }

    // ==================== SPARE PARTS ====================

    public static List<SparePart> getSpareParts() { return spareParts; }

    public static void addSparePart(SparePart sp) {
        DatabaseFactory.getDatabase().addSparePart(sp);
        spareParts = DatabaseFactory.getDatabase().getAllSpareParts();
        isDirty = true;
    }

    public static void removeSparePart(SparePart sp) {
        DatabaseFactory.getDatabase().deleteSparePart(sp);
        spareParts = DatabaseFactory.getDatabase().getAllSpareParts();
        isDirty = true;
    }

    public static void updateSparePartStock(SparePart part, double newStock) {
        part.setDirty(true);
        part.setStock(newStock);
        DatabaseFactory.getDatabase().updateSparePartStock(part, newStock);
        spareParts = DatabaseFactory.getDatabase().getAllSpareParts();
        isDirty = true;
    }

    public static void updateSparePartStock(String serviceName, int qty) {
        // Получаем запчасти, связанные с услугой
        List<AutoAddSparePartService.SparePartWithQuantity> parts = AutoAddSparePartService.getSparePartsByService(serviceName);
        if (!parts.isEmpty()) {
            SparePart part = parts.get(0).getSparePart();
            if (part != null) {
                part.setStock(part.getStock() - qty);
                DataStore.updateSparePartStock(part, part.getStock());
            }
        }
    }

    // ==================== APPOINTMENTS ====================

    public static List<Appointment> getAppointments() {
        return appointments;
    }

    public static List<Appointment> getAppointmentsByDate(String date) {
        return Database.getAppointmentsByDate(date);
    }

    public static void addAppointment(Appointment a) {
        DatabaseFactory.getDatabase().addAppointment(a);
        appointments = DatabaseFactory.getDatabase().getAllAppointments();
        isDirty = true;
    }

    public static void updateAppointment(Appointment a) {
        a.setDirty(true);
        DatabaseFactory.getDatabase().updateAppointment(a);
        appointments = DatabaseFactory.getDatabase().getAllAppointments();
        isDirty = true;
    }

    public static void deleteAppointment(int id) {
        DatabaseFactory.getDatabase().deleteAppointment(id);
        appointments.removeIf(a -> a.getId() == id);
        isDirty = true;
    }

    // ==================== SERVICE-SPARE PART RELATIONSHIPS ====================

    public static List<ServiceSparePart> getServiceSparePartsByServiceId(int serviceId) {
        if (serviceId == -1) {
            return serviceSpareParts;
        }
        return DatabaseFactory.getDatabase().getServiceSparePartsByServiceId(serviceId);
    }

    public static void addServiceSparePart(ServiceSparePart relation) {
        DatabaseFactory.getDatabase().addServiceSparePart(relation);
        serviceSpareParts.add(relation);
        isDirty = true;
    }

    public static void updateServiceSparePart(ServiceSparePart relation) {
        relation.setDirty(true);
        DatabaseFactory.getDatabase().updateServiceSparePart(relation);
        serviceSpareParts = DatabaseFactory.getDatabase().getServiceSparePartsByServiceId(-1);
        isDirty = true;
    }

    public static void deleteServiceSparePart(ServiceSparePart relation) {
        DatabaseFactory.getDatabase().deleteServiceSparePart(relation);
        serviceSpareParts.remove(relation);
        isDirty = true;
    }

    public static void deleteServiceSparePartsByServiceId(int serviceId) {
        DatabaseFactory.getDatabase().deleteServiceSparePartsByServiceId(serviceId);
        serviceSpareParts.removeIf(s -> s.getServiceId() == serviceId);
        isDirty = true;
    }

    // ==================== TO PARTS ====================

    public static List<ToPart> getToPartsByCarModel(String carModel) {
        if (carModel == null || carModel.isEmpty()) {
            return toParts;
        }
        return DatabaseFactory.getDatabase().getToPartsByCarModel(carModel);
    }

    public static void addToPart(ToPart part) {
        DatabaseFactory.getDatabase().addToPart(part);
        toParts.add(part);
        isDirty = true;
    }

    public static void updateToPart(ToPart part) {
        part.setDirty(true);
        DatabaseFactory.getDatabase().updateToPart(part);
        toParts = DatabaseFactory.getDatabase().getToPartsByCarModel("");
        isDirty = true;
    }

    public static void deleteToPart(ToPart part) {
        DatabaseFactory.getDatabase().deleteToPart(part);
        toParts.remove(part);
        isDirty = true;
    }

    public static void deleteToPartsByCarModel(String carModel) {
        DatabaseFactory.getDatabase().deleteToPartsByCarModel(carModel);
        toParts.removeIf(t -> t.getCarModel().equals(carModel));
        isDirty = true;
    }

    // ==================== SETTINGS ====================

    public static List<Setting> getAllSettings() {
        return settings;
    }

    public static void addSetting(Setting setting) {
        DatabaseFactory.getDatabase().addSetting(setting);
        settings.add(setting);
        isDirty = true;
    }

    public static void updateSetting(Setting setting) {
        setting.setDirty(true);
        DatabaseFactory.getDatabase().updateSetting(setting);
        settings = DatabaseFactory.getDatabase().getAllSettings();
        isDirty = true;
    }

    public static void deleteSetting(Setting setting) {
        DatabaseFactory.getDatabase().deleteSetting(setting);
        settings.remove(setting);
        isDirty = true;
    }

    public static Setting getSettingByKey(String key) {
        return DatabaseFactory.getDatabase().getSettingByKey(key);
    }
}