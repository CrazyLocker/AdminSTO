package com.autoservice;

import java.sql.Connection;
import java.util.List;

/**
 * Интерфейс для работы с базой данных.
 * Позволяет использовать разные реализации (H2, SQLite).
 */
public interface DatabaseInterface {
    
    /**
     * Инициализация для production (используется SQLite).
     */
    void init();
    
    /**
     * Инициализация для тестов (используется H2 in-memory).
     */
    void initForTest();
    
    /**
     * Получить соединение из пула.
     */
    Connection getConnection() throws Exception;
    
    /**
     * Закрыть DataSource и освободить ресурсы.
     */
    void close();
    
    // ==================== CLIENTS ====================
    
    List<Client> getAllClients();
    
    void addClient(Client client);
    
    void updateClient(Client client);
    
    void deleteClient(Client client);
    
    Client getClientById(int id);
    
    int getClientId(Client client);
    
    // ==================== SERVICES ====================
    
    List<Service> getAllServices();
    
    void addService(Service service);
    
    void updateService(Service service);
    
    void deleteService(Service service);
    
    // ==================== SPARE PARTS ====================
    
    List<SparePart> getAllSpareParts();
    
    void addSparePart(SparePart part);
    
    void updateSparePart(SparePart part);
    
    void deleteSparePart(SparePart part);
    
    void updateSparePartStock(SparePart part, double newStock);
    
    // ==================== ORDERS ====================
    
    List<WorkOrder> getAllOrders();
    
    void addOrder(WorkOrder order);
    
    void updateOrder(WorkOrder order);
    
    void deleteOrder(String orderId);
    
    // ==================== APPOINTMENTS ====================
    
    List<Appointment> getAllAppointments();
    
    void addAppointment(Appointment appointment);
    
    void updateAppointment(Appointment appointment);
    
    void deleteAppointment(int id);
    
    List<Appointment> getAppointmentsByDate(String date);
    
    // ==================== SERVICE-SPARE PART RELATIONSHIPS ====================
    
    List<com.autoservice.model.ServiceSparePart> getServiceSparePartsByServiceId(int serviceId);
    
    void addServiceSparePart(com.autoservice.model.ServiceSparePart relation);
    
    void updateServiceSparePart(com.autoservice.model.ServiceSparePart relation);
    
    void deleteServiceSparePart(com.autoservice.model.ServiceSparePart relation);
    
    void deleteServiceSparePartsByServiceId(int serviceId);
    
    // ==================== TO PARTS RELATIONSHIPS ====================
    
    List<com.autoservice.model.ToPart> getToPartsByCarModel(String carModel);
    
    void addToPart(com.autoservice.model.ToPart part);
    
    void updateToPart(com.autoservice.model.ToPart part);
    
    void deleteToPart(com.autoservice.model.ToPart part);
    
    void deleteToPartsByCarModel(String carModel);
    
    // ==================== SETTINGS ====================
    
    List<com.autoservice.model.Setting> getAllSettings();
    
    void addSetting(com.autoservice.model.Setting setting);
    
    void updateSetting(com.autoservice.model.Setting setting);
    
    void deleteSetting(com.autoservice.model.Setting setting);
    
    com.autoservice.model.Setting getSettingByKey(String key);
}
