package com.autoservice;

import java.util.List;

/**
 * Обертка над DatabaseFactory для удобства использования.
 * Все методы передаются в DatabaseFactory.
 */
public class Database {
    
    // ==================== Инициализация ====================
    
    public static void init() {
        DatabaseFactory.init();
    }
    
    public static void initForTest() {
        DatabaseFactory.initForTest();
    }
    
    public static void close() {
        DatabaseFactory.close();
    }
    
    // ==================== CLIENTS ====================
    
    public static List<Client> getAllClients() {
        return DatabaseFactory.getDatabase().getAllClients();
    }
    
    public static void addClient(Client client) {
        DatabaseFactory.getDatabase().addClient(client);
    }
    
    public static void updateClient(Client client) {
        DatabaseFactory.getDatabase().updateClient(client);
    }
    
    public static void deleteClient(Client client) {
        DatabaseFactory.getDatabase().deleteClient(client);
    }
    
    public static Client getClientById(int id) {
        return DatabaseFactory.getDatabase().getClientById(id);
    }
    
    public static int getClientId(Client client) {
        return DatabaseFactory.getDatabase().getClientId(client);
    }
    
    // ==================== SERVICES ====================
    
    public static List<Service> getAllServices() {
        return DatabaseFactory.getDatabase().getAllServices();
    }
    
    public static void addService(Service service) {
        DatabaseFactory.getDatabase().addService(service);
    }
    
    public static void updateService(Service service) {
        DatabaseFactory.getDatabase().updateService(service);
    }
    
    public static void deleteService(Service service) {
        DatabaseFactory.getDatabase().deleteService(service);
    }
    
    // ==================== SPARE PARTS ====================
    
    public static List<SparePart> getAllSpareParts() {
        return DatabaseFactory.getDatabase().getAllSpareParts();
    }
    
    public static void addSparePart(SparePart part) {
        DatabaseFactory.getDatabase().addSparePart(part);
    }
    
    public static void updateSparePart(SparePart part) {
        DatabaseFactory.getDatabase().updateSparePart(part);
    }
    
    public static void deleteSparePart(SparePart part) {
        DatabaseFactory.getDatabase().deleteSparePart(part);
    }
    
    public static void updateSparePartStock(SparePart part, double newStock) {
        DatabaseFactory.getDatabase().updateSparePartStock(part, newStock);
    }
    
    // ==================== ORDERS ====================
    
    public static List<WorkOrder> getAllOrders() {
        return DatabaseFactory.getDatabase().getAllOrders();
    }
    
    public static void addOrder(WorkOrder order) {
        DatabaseFactory.getDatabase().addOrder(order);
    }
    
    public static void updateOrder(WorkOrder order) {
        DatabaseFactory.getDatabase().updateOrder(order);
    }
    
    public static void deleteOrder(String orderId) {
        DatabaseFactory.getDatabase().deleteOrder(orderId);
    }
    
    // ==================== APPOINTMENTS ====================
    
    public static List<Appointment> getAllAppointments() {
        return DatabaseFactory.getDatabase().getAllAppointments();
    }
    
    public static void addAppointment(Appointment appointment) {
        DatabaseFactory.getDatabase().addAppointment(appointment);
    }
    
    public static void updateAppointment(Appointment appointment) {
        DatabaseFactory.getDatabase().updateAppointment(appointment);
    }
    
    public static void deleteAppointment(int id) {
        DatabaseFactory.getDatabase().deleteAppointment(id);
    }
    
    public static List<Appointment> getAppointmentsByDate(String date) {
        return DatabaseFactory.getDatabase().getAppointmentsByDate(date);
    }
    
    // ==================== SERVICE-SPARE PART RELATIONSHIPS ====================
    
    public static List<com.autoservice.model.ServiceSparePart> getServiceSparePartsByServiceId(int serviceId) {
        return DatabaseFactory.getDatabase().getServiceSparePartsByServiceId(serviceId);
    }
    
    public static void addServiceSparePart(com.autoservice.model.ServiceSparePart relation) {
        DatabaseFactory.getDatabase().addServiceSparePart(relation);
    }
    
    public static void deleteServiceSparePart(com.autoservice.model.ServiceSparePart relation) {
        DatabaseFactory.getDatabase().deleteServiceSparePart(relation);
    }
    
    public static void deleteServiceSparePartsByServiceId(int serviceId) {
        DatabaseFactory.getDatabase().deleteServiceSparePartsByServiceId(serviceId);
    }
    
    // ==================== TO PARTS RELATIONSHIPS ====================
    
    public static List<com.autoservice.model.ToPart> getToPartsByCarModel(String carModel) {
        return DatabaseFactory.getDatabase().getToPartsByCarModel(carModel);
    }
    
    public static void addToPart(com.autoservice.model.ToPart part) {
        DatabaseFactory.getDatabase().addToPart(part);
    }
    
    public static void updateToPart(com.autoservice.model.ToPart part) {
        DatabaseFactory.getDatabase().updateToPart(part);
    }
    
    public static void deleteToPart(com.autoservice.model.ToPart part) {
        DatabaseFactory.getDatabase().deleteToPart(part);
    }
    
    public static void deleteToPartsByCarModel(String carModel) {
        DatabaseFactory.getDatabase().deleteToPartsByCarModel(carModel);
    }
    
    // ==================== SETTINGS ====================
    
    public static List<com.autoservice.model.Setting> getAllSettings() {
        return DatabaseFactory.getDatabase().getAllSettings();
    }
    
    public static void addSetting(com.autoservice.model.Setting setting) {
        DatabaseFactory.getDatabase().addSetting(setting);
    }
    
    public static void updateSetting(com.autoservice.model.Setting setting) {
        DatabaseFactory.getDatabase().updateSetting(setting);
    }
    
    public static void deleteSetting(com.autoservice.model.Setting setting) {
        DatabaseFactory.getDatabase().deleteSetting(setting);
    }
    
    public static com.autoservice.model.Setting getSettingByKey(String key) {
        return DatabaseFactory.getDatabase().getSettingByKey(key);
    }
}

