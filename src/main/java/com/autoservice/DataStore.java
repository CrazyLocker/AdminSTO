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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Центральное хранилище данных приложения (in-memory кэш над БД).
 * 
 * Ответственность: хранение актуальных копий всех сущностей (клиенты,
 * заказы, услуги, запчасти, записи, настройки и др.) в памяти, загрузка их
 * из БД, сохранение изменённых объектов обратно в БД, а также предоставление
 * CRUD-методов для работы с данными.
 * 
 * Зависимости: Database/DatabaseFactory, модели (Client, WorkOrder, Service,
 * SparePart, Appointment, ServiceSparePart, ServiceSparePartsList, ToPart,
 * Setting, ServicePart), AutoAddSparePartService.
 * 
 * ВАЖНО (известная проблема): часть методов может возвращать null-данные
 * или несинхронизированный кэш. Не полагайтесь на то, что коллекции всегда
 * заполнены — защищайтесь от null перед использованием (TODO: устранить
 * источники null-данных).
 * 
 * @author AdminSTO Team
 * @since 1.0
 * @see Database
 * @see WorkOrder
 * @see Client
 */
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

    /** Флаг наличия несохранённых изменений (dirty-флаг всего хранилища). */
    private static boolean isDirty = false;
    
    // Мьютекс для синхронизации операций сохранения (защита от race condition)
    private static final Object saveLock = new Object();

    /**
     * Полностью перезагружает все коллекции данных из БД. Вызывается при
     * запуске приложения (в фоновом потоке) и после крупных изменений.
     * 
     * ВАЖНО: если БД пуста или данные повреждены, некоторые списки могут
     * остаться пустыми. TODO: добавить защиту от null при обращении к
     * элементам (например, order.getClient() может быть null).
     */
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

    /**
     * Сохраняет все изменённые (dirty) объекты в БД. Операция синхронизирована
     * мьютексом {@code saveLock} для защиты от гонок данных при параллельных
     * вызовах (например, фоновый бэкап и закрытие приложения).
     * Если изменений нет — метод завершается без обращений к БД.
     */
    public static void save() {
        // Синхронизация для предотвращения race condition при параллельных вызовах
        synchronized (saveLock) {
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
    }

    /**
     * Помечает хранилище как содержащее несохранённые изменения.
     * Должен вызываться при любом изменении данных, чтобы {@link #save()}
     * выполнил запись в БД.
     */
    public static void markDirty() {
        isDirty = true;
    }

    // ==================== CLIENTS ====================

    /** @return список всех клиентов в кэше */
    public static List<Client> getClients() { return clients; }

    /**
     * Перезагружает клиентов из БД (с автомобилями).
     * Возвращает свежий список, не затрагивая кэш DataStore.
     */
    public static List<Client> loadClients() {
        return DatabaseFactory.getDatabase().getAllClients();
    }

    /**
     * Добавляет нового клиента в БД и обновляет кэш.
     * 
     * @param c клиент для добавления
     */
    public static void addClient(Client c) {
        DatabaseFactory.getDatabase().addClient(c);
        clients = DatabaseFactory.getDatabase().getAllClients();
        isDirty = true;
    }

    /**
     * Обновляет данные существующего клиента в БД и кэше.
     * 
     * @param client клиент с обновлёнными данными
     */
    public static void updateClient(Client client) {
        client.setDirty(true);
        DatabaseFactory.getDatabase().updateClient(client);
        clients = DatabaseFactory.getDatabase().getAllClients();
        isDirty = true;
    }

    /**
     * Удаляет клиента из БД и обновляет кэш.
     * 
     * @param c удаляемый клиент
     */
    public static void removeClient(Client c) {
        DatabaseFactory.getDatabase().deleteClient(c);
        clients = DatabaseFactory.getDatabase().getAllClients();
        isDirty = true;
    }

    /**
     * Удаляет клиента вместе со всеми его заказами. Сначала находятся и
     * удаляются все заказы клиента, затем сам клиент.
     * 
     * @param client удаляемый клиент
     */
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

    // ==================== CAR ====================

    public static void addCar(Car car) throws java.sql.SQLException {
        DatabaseFactory.getDatabase().addCar(car);
        Client c = getClientById(car.getClientId());
        if (c != null) {
            c.getCars().add(car);
        }
        isDirty = true;
    }

    public static void updateCar(Car car) {
        DatabaseFactory.getDatabase().updateCar(car);
        isDirty = true;
    }

    public static void deleteCar(Car car) {
        DatabaseFactory.getDatabase().deleteCar(car);
        Client c = getClientById(car.getClientId());
        if (c != null) {
            c.getCars().remove(car);
        }
        isDirty = true;
    }

    private static Client getClientById(int id) {
        return clients.stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElse(null);
    }

    // ==================== ORDERS ====================

    /** @return список всех заказов в кэше */
    public static List<WorkOrder> getOrders() { return orders; }

    /**
     * Добавляет заказ в БД и обновляет кэш заказов.
     * 
     * @param o заказ для добавления
     */
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

    /**
     * Обновляет существующий заказ в БД.
     * 
     * @param o заказ с обновлёнными данными
     */
    public static void updateOrder(WorkOrder o) {
        logger.debug("=== DataStore.updateOrder вызван для заказа {} ===", o.getId());
        logger.debug("Услуг в заказе: {}", o.getServices().size());
        for (int i = 0; i < o.getServices().size(); i++) {
            logger.debug("  Услуга {}: {} (price={})", i, o.getServices().get(i), o.getServicePrices().get(i));
        }
        o.setDirty(true);
        DatabaseFactory.getDatabase().updateOrder(o);
        isDirty = true;
    }

    /**
     * Удаляет заказ из БД и кэша. Если идентификатор заказа пуст — операция
     * отклоняется с ошибкой в логе.
     * 
     * @param order удаляемый заказ
     */
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

    /**
     * Подсчитывает количество активных заказов (статус не «Закрыт»).
     * 
     * @return количество активных заказов
     */
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

    /** @return список всех услуг в кэше */
    public static List<Service> getServices() { return services; }

    /**
     * Добавляет услугу в БД и обновляет кэш.
     * 
     * @param s услуга для добавления
     */
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

    /** @return список всех запчастей в кэше */
    public static List<SparePart> getSpareParts() { return spareParts; }

    /**
     * Добавляет запчасть в БД и обновляет кэш.
     * 
     * @param sp запчасть для добавления
     */
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

    /**
     * Списывает количество запчастей со склада по имени услуги.
     * Списывает ВСЕ запчасти, связанные с услугой, а не только первую.
     * 
     * @param serviceName название услуги
     * @param qty         количество, которое нужно списать
     */
    public static void updateSparePartStock(String serviceName, int qty) {
        List<AutoAddSparePartService.SparePartWithQuantity> parts =
                AutoAddSparePartService.getSparePartsByService(serviceName);
        
        for (AutoAddSparePartService.SparePartWithQuantity partInfo : parts) {
            SparePart part = partInfo.getSparePart();
            if (part != null) {
                double newStock = part.getStock() - partInfo.getQuantity() * qty;
                DataStore.updateSparePartStock(part, newStock);
                logger.debug("Списано: {} qty={} newStock={}", part.getName(), qty, newStock);
            }
        }
    }

    // ==================== СКЛАД: ПРОВЕРКИ И УПРАВЛЕНИЕ ====================

    /**
     * Проверяет, достаточно ли запчастей на складе для данного заказа.
     * Использует доступный остаток (stock - reserved).
     * 
     * @param order заказ для проверки
     * @return список строк с описанием недостающих запчастей (пустой, если всё в наличии)
     */
    public static List<String> checkSparePartsAvailability(WorkOrder order) {
        List<String> missing = new ArrayList<>();
        
        for (int i = 0; i < order.getSpareParts().size(); i++) {
            SparePart part = order.getSpareParts().get(i);
            double qty = order.getSparePartQuantities().get(i);
            SparePart current = getSparePartById(part.getId());
            
            if (current != null) {
                double available = current.getAvailableStock();
                if (qty > available) {
                    missing.add(String.format(
                        "«%s»: нужно %.0f %s, доступно %.0f %s (всего: %.0f, резерв: %.0f)",
                        part.getName(), qty, current.getUnitType(), available, current.getUnitType(), current.getStock(), current.getReserved()
                    ));
                }
            } else {
                missing.add(String.format(
                    "«%s»: запчасть не найдена в справочнике",
                    part.getName()
                ));
            }
        }
        
        return missing;
    }

    /**
     * Резервирует запчасти для заказа.
     * 
     * @param order заказ, для которого резервируются запчасти
     * @return true, если все запчасти удалось зарезервировать
     */
    public static boolean reserveSpareParts(WorkOrder order) {
        for (int i = 0; i < order.getSpareParts().size(); i++) {
            SparePart part = order.getSpareParts().get(i);
            double qty = order.getSparePartQuantities().get(i);
            
            SparePart current = getSparePartById(part.getId());
            if (current != null) {
                if (!current.reserve(qty)) {
                    logger.warn("Не удалось зарезервировать: {} (нужно {}, доступно {})", 
                            part.getName(), qty, current.getAvailableStock());
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Снимает резерв с запчастей заказа (при отмене/удалении).
     * 
     * @param order заказ, для которого снимается резерв
     */
    public static void unreserveSpareParts(WorkOrder order) {
        for (int i = 0; i < order.getSpareParts().size(); i++) {
            SparePart part = order.getSpareParts().get(i);
            double qty = order.getSparePartQuantities().get(i);
            
            SparePart current = getSparePartById(part.getId());
            if (current != null) {
                current.unreserve(qty);
                logger.info("Снят резерв: {} -{} (остаток резерва: {})", part.getName(), qty, current.getReserved());
            }
        }
    }

    /**
     * Списывает запчасти со склада при закрытии заказа.
     * Уменьшает stock и снимает резерв.
     * 
     * @param order заказ, запчасти которого нужно списать
     */
    public static void deductSpareParts(WorkOrder order) {
        for (int i = 0; i < order.getSpareParts().size(); i++) {
            SparePart part = order.getSpareParts().get(i);
            double qty = order.getSparePartQuantities().get(i);
            
            SparePart current = getSparePartById(part.getId());
            if (current != null) {
                if (current.deduct(qty)) {
                    logger.info("Списано со склада: {} -{} (остаток: {})", part.getName(), qty, current.getStock());
                } else {
                    logger.warn("Не удалось списать: {} (нужно {}, в наличии {})", part.getName(), qty, current.getStock());
                }
            }
        }
    }

    /**
     * Возвращает запчасти на склад при отмене/удалении заказа (устаревший метод, используйте unreserveSpareParts).
     * 
     * @param order заказ, запчасти которого нужно вернуть
     * @deprecated Используйте {@link #unreserveSpareParts(WorkOrder)}
     */
    @Deprecated
    public static void restoreSpareParts(WorkOrder order) {
        unreserveSpareParts(order);
    }

    /**
     * Проверяет все запчасти на предмет падения ниже минимального остатка.
     * Логирует предупреждения для каждой запчасти, где stock < minStock.
     */
    public static void checkMinStockLevels() {
        List<String> lowStock = new ArrayList<>();
        
        for (SparePart part : getSpareParts()) {
            if (part.getMinStock() > 0 && part.getStock() < part.getMinStock()) {
                lowStock.add(String.format(
                    "«%s»: %.0f %s (мин. %s %.0f %s)",
                    part.getName(),
                    part.getStock(), part.getUnitType(),
                    part.getUnitType(), part.getMinStock(), part.getUnitType()
                ));
            }
        }
        
        if (!lowStock.isEmpty()) {
            logger.warn("⚠️ Запчасти с низким остатком ({}):", lowStock.size());
            for (String msg : lowStock) {
                logger.warn("  - {}", msg);
            }
        }
    }

    /**
     * Списывает запчасти для всех услуг в заказе (автоматически добавленные).
     * Использует {@link AutoAddSparePartService} для определения запчастей по услугам.
     * 
     * @param order заказ, для которого нужно списать запчасти
     */
    public static void deductServiceSpareParts(WorkOrder order) {
        for (int i = 0; i < order.getServices().size(); i++) {
            String serviceName = order.getServices().get(i);
            List<AutoAddSparePartService.SparePartWithQuantity> parts =
                    AutoAddSparePartService.getSparePartsByService(serviceName);
            
            for (AutoAddSparePartService.SparePartWithQuantity partInfo : parts) {
                SparePart part = partInfo.getSparePart();
                if (part != null) {
                    double qtyToDeduct = partInfo.getQuantity();
                    if (part.deductStock(qtyToDeduct)) {
                        logger.debug("Списано из услуги «{}»: {} -{}", 
                                serviceName, part.getName(), qtyToDeduct);
                    } else {
                        logger.warn("Не хватило «{}» для услуги «{}»: доступно {}", 
                                part.getName(), serviceName, part.getStock());
                    }
                }
            }
        }
    }

    // ==================== APPOINTMENTS ====================

    /** @return список всех записей в кэше */
    public static List<Appointment> getAppointments() {
        return appointments;
    }

    /**
     * Возвращает записи на указанную дату.
     * 
     * @param date дата в строковом представлении
     * @return список записей на дату
     */
    public static List<Appointment> getAppointmentsByDate(String date) {
        return Database.getAppointmentsByDate(date);
    }

    /**
     * Ищет запись по идентификатору связанного заказа.
     * 
     * @param orderId идентификатор заказа
     * @return найденная запись или null, если заказ не передан/не найден
     */
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
        logger.info("DataStore.updateSetting: setting id={}, key={}, value=[REDACTED], dirty={}", 
                setting.getId(), maskSensitive(setting.getKey()), setting.isDirty());
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

    /**
     * Маскирует чувствительные ключи настроек (пароли, токены, секреты).
     */
    private static String maskSensitive(String key) {
        if (key == null) return null;
        String lower = key.toLowerCase();
        if (lower.contains("password") || lower.contains("token") || lower.contains("secret")) {
            return "***REDACTED***";
        }
        return key;
    }
}