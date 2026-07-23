package com.autoservice;

import com.autoservice.model.ServiceSparePart;
import com.autoservice.model.ServiceSparePartsList;
import com.autoservice.model.ServiceSparePartsListItem;
import com.autoservice.model.ServicePart;
import com.autoservice.model.ToPart;
import com.autoservice.model.Setting;
import com.autoservice.services.AutoAddSparePartService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DataStore {
    private static final Logger logger = LoggerFactory.getLogger(DataStore.class);
    
    private static List<Client> clients = new ArrayList<>();
    private static List<WorkOrder> orders = new ArrayList<>();
    private static List<Service> services = new ArrayList<>();
    private static List<SparePart> spareParts = new ArrayList<>();
    private static List<Appointment> appointments = new ArrayList<>();
    private static List<ServiceSparePart> serviceSpareParts = new ArrayList<>();
    private static List<ServiceSparePartsList> serviceSparePartsLists = new ArrayList<>();
    private static List<ServiceSparePartsListItem> serviceSparePartsListItems = new ArrayList<>();
    private static List<ServicePart> serviceParts = new ArrayList<>();
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
        serviceSparePartsLists = DatabaseFactory.getDatabase().getServiceSparePartsListsByServiceId(-1);
        // Загружаем элементы для каждого списка
        for (ServiceSparePartsList list : serviceSparePartsLists) {
            List<ServiceSparePartsListItem> items = DatabaseFactory.getDatabase().getServiceSparePartsListItems(list.getId());
            list.setItems(items);
        }
        serviceParts = DatabaseFactory.getDatabase().getAllServiceParts();
        toParts = DatabaseFactory.getDatabase().getToPartsByCarModel("");
        settings = DatabaseFactory.getDatabase().getAllSettings();
        isDirty = false;
        logger.info("DataStore загружен: {} клиентов, {} заказов, {} услуг, {} запчастей, {} записей, {} связей услуги-запчасти, {} расходников TO, {} настроек", 
                clients.size(), orders.size(), services.size(), spareParts.size(), appointments.size(), 
                serviceSpareParts.size(), toParts.size(), settings.size());
    }

    public static void save() {
        if (!isDirty) {
            logger.info("Нет изменений для сохранения");
            return;
        }

        logger.info("Сохранение изменений...");
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
                Database.addServiceSparePart(ssp);
                ssp.setDirty(false);
                saved++;
            }
        }

        for (ServiceSparePartsList sspList : serviceSparePartsLists) {
            if (sspList.isDirty()) {
                Database.addServiceSparePartsList(sspList);
                // Элементы сохраняются автоматически внутри addServiceSparePartsList
                sspList.setDirty(false);
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

        // Сохраняем serviceParts (связи услуги-запчасти)
        for (ServicePart sp : serviceParts) {
            if (sp.isDirty()) {
                Database.updateServicePart(sp);
                sp.setDirty(false);
                saved++;
            }
        }

        isDirty = false;
        long endTime = System.currentTimeMillis();
        logger.info("Сохранено {} элементов в БД за {} мс", saved, (endTime - startTime));
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
        logger.debug("=== DataStore.addOrder вызван для заказа {} ===", o.getId());
        logger.debug("Услуг в заказе: {}", o.getServices().size());
        for (int i = 0; i < o.getServices().size(); i++) {
            logger.debug("  Услуга {}: {} (price={})", i, o.getServices().get(i), o.getServicePrices().get(i));
        }
        DatabaseFactory.getDatabase().addOrder(o);
        orders = DatabaseFactory.getDatabase().getAllOrders();
        isDirty = true;
        logger.debug("Заказов после добавления: {}", orders.size());
    }

    public static void updateOrder(WorkOrder o) {
        logger.debug("=== DataStore.updateOrder вызван для заказа {} ===", o.getId());
        logger.debug("Услуг в заказе: {}", o.getServices().size());
        for (int i = 0; i < o.getServices().size(); i++) {
            logger.debug("  Услуга {}: {} (price={})", i, o.getServices().get(i), o.getServicePrices().get(i));
        }
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
            logger.error("Нельзя удалить заказ с ID={}", orderId);
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

    public static void updateService(Service s) {
        DatabaseFactory.getDatabase().updateService(s);
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

    public static Service getServiceById(int id) {
        return services.stream()
                .filter(s -> s.getId() == id)
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

    public static void updateSparePart(SparePart sp) {
        sp.setDirty(true);
        DatabaseFactory.getDatabase().updateSparePart(sp);
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

    public static Appointment getAppointmentByOrderId(String orderId) {
        if (orderId == null || orderId.isEmpty()) return null;
        for (Appointment a : appointments) {
            if (orderId.equals(a.getOrderId())) {
                return a;
            }
        }
        return null;
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

    public static Appointment getAppointmentById(int id) {
        return appointments.stream()
                .filter(a -> a.getId() == id)
                .findFirst()
                .orElse(null);
    }

    // ==================== SERVICE-SPARE PART RELATIONSHIPS ====================

    public static List<ServiceSparePart> getServiceSparePartsByServiceId(int serviceId) {
        if (serviceId == -1) {
            return serviceSpareParts;
        }
        return DatabaseFactory.getDatabase().getServiceSparePartsByServiceId(serviceId);
    }

    public static void addServiceSparePart(ServiceSparePart relation) {
        // Проверка на дубликат: связь с такими же serviceId и sparePartId уже существует?
        boolean exists = serviceSpareParts.stream()
                .filter(s -> s.getServiceId() == relation.getServiceId() && s.getSparePartId() == relation.getSparePartId())
                .findFirst()
                .orElse(null) != null;
        
        if (exists) {
            logger.warn("Связь услуги-запчасти уже существует: serviceId={}, sparePartId={}", 
                    relation.getServiceId(), relation.getSparePartId());
            // Обновляем количество, если связь уже существует
            ServiceSparePart existing = serviceSpareParts.stream()
                    .filter(s -> s.getServiceId() == relation.getServiceId() && s.getSparePartId() == relation.getSparePartId())
                    .findFirst()
                    .orElse(null);
            if (existing != null) {
                existing.setQuantity(relation.getQuantity());
                existing.setUnitType(relation.getUnitType());
                existing.setActive(relation.isActive());
                DatabaseFactory.getDatabase().addServiceSparePart(existing);
            }
        } else {
            DatabaseFactory.getDatabase().addServiceSparePart(relation);
            serviceSpareParts.add(relation);
        }
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

    public static void deleteDuplicateServiceSpareParts() {
        DatabaseFactory.getDatabase().deleteDuplicateServiceSpareParts();
        // Перезагружаем данные из БД
        serviceSpareParts = DatabaseFactory.getDatabase().getServiceSparePartsByServiceId(-1);
        isDirty = true;
    }

    // ==================== SERVICE-PART RELATIONSHIPS (NEW STRUCTURE) ====================

    public static List<ServicePart> getAllServiceParts() {
        return serviceParts;
    }

    public static List<ServicePart> getServicePartsByServiceId(int serviceId) {
        return serviceParts.stream()
                .filter(p -> p.getServiceId() == serviceId)
                .toList();
    }

    public static void addServicePart(ServicePart part) {
        DatabaseFactory.getDatabase().addServicePart(part);
        serviceParts.add(part);
        isDirty = true;
    }

    public static void updateServicePart(ServicePart part) {
        part.setDirty(true);
        DatabaseFactory.getDatabase().updateServicePart(part);
        serviceParts = DatabaseFactory.getDatabase().getAllServiceParts();
        isDirty = true;
    }

    public static void deleteServicePart(ServicePart part) {
        DatabaseFactory.getDatabase().deleteServicePart(part);
        serviceParts.remove(part);
        isDirty = true;
    }

    public static void deleteServicePartsByServiceId(int serviceId) {
        DatabaseFactory.getDatabase().deleteServicePartsByServiceId(serviceId);
        serviceParts.removeIf(s -> s.getServiceId() == serviceId);
        isDirty = true;
    }

    public static void deleteServicePartsBySparePartId(int sparePartId) {
        DatabaseFactory.getDatabase().deleteServicePartsBySparePartId(sparePartId);
        serviceParts.removeIf(s -> s.getSparePartId() == sparePartId);
        isDirty = true;
    }

    public static ServicePart getServicePartById(int id) {
        return serviceParts.stream()
                .filter(s -> s.getId() == id)
                .findFirst()
                .orElse(null);
    }

    // ==================== SERVICE-SPARE PARTS LISTS (NEW STRUCTURE) ====================

    public static List<ServiceSparePartsList> getServiceSparePartsListsByServiceId(int serviceId) {
        if (serviceId == -1) {
            return serviceSparePartsLists;
        }
        return DatabaseFactory.getDatabase().getServiceSparePartsListsByServiceId(serviceId);
    }

    public static List<ServiceSparePartsListItem> getServiceSparePartsListItems(int listId) {
        return DatabaseFactory.getDatabase().getServiceSparePartsListItems(listId);
    }

    public static void addServiceSparePartsList(ServiceSparePartsList list) {
        DatabaseFactory.getDatabase().addServiceSparePartsList(list);
        serviceSparePartsLists.add(list);
        
        // Помечаем список как dirty для сохранения
        list.setDirty(true);
        
        // Загружаем элементы из БД, так как они получили ID
        List<ServiceSparePartsListItem> items = DatabaseFactory.getDatabase().getServiceSparePartsListItems(list.getId());
        list.setItems(items);
        
        isDirty = true;
    }

    public static void addServiceSparePartsListItem(ServiceSparePartsListItem item) {
        DatabaseFactory.getDatabase().addServiceSparePartsListItem(item);
        serviceSparePartsListItems.add(item);
        isDirty = true;
    }

    public static void deleteServiceSparePartsList(ServiceSparePartsList list) {
        DatabaseFactory.getDatabase().deleteServiceSparePartsList(list);
        serviceSparePartsLists.remove(list);
        isDirty = true;
    }

    public static void deleteServiceSparePartsListsByServiceId(int serviceId) {
        DatabaseFactory.getDatabase().deleteServiceSparePartsListsByServiceId(serviceId);
        serviceSparePartsLists.removeIf(s -> s.getServiceId() == serviceId);
        isDirty = true;
    }

    // ==================== TO PARTS ====================

    public static List<ToPart> getToPartsByCarModel(String carModel) {
        if (carModel == null || carModel.isEmpty()) {
            return toParts;
        }
        return DatabaseFactory.getDatabase().getToPartsByCarModel(carModel);
    }

    public static List<String> getAllCarModels() {
        return DatabaseFactory.getDatabase().getAllCarModels();
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
        logger.info("DataStore.updateSetting: setting id={}, key={}, value={}, dirty={}", 
                setting.getId(), setting.getKey(), setting.getValue(), setting.isDirty());
        setting.setDirty(true);
        DatabaseFactory.getDatabase().updateSetting(setting);
        logger.info("  Database.updateSetting completed");
        // Обновляем значение в существующем объекте, чтобы избежать создания новых объектов
        // Найдем объект в списке по ID и обновим его значение
        boolean found = false;
        for (int i = 0; i < settings.size(); i++) {
            if (settings.get(i).getId() == setting.getId()) {
                settings.set(i, setting);
                found = true;
                logger.info("  Found setting in DataStore.settings at index {}, replaced", i);
                break;
            }
        }
        if (!found) {
            logger.warn("  Setting with id={} NOT FOUND in DataStore.settings! Size={}", setting.getId(), settings.size());
        }
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