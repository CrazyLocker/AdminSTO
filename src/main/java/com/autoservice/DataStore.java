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

/**
 * Кэш данных приложения и слой синхронизации с базой данных.
 * Хранит загруженные сущности в памяти и сохраняет только изменённые записи.
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

    private static boolean isDirty = false;
    
    // Мьютекс для синхронизации операций сохранения (защита от race condition)
    private static final Object saveLock = new Object();

    /**
     * Загрузить все данные из базы в кэш DataStore.
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
     * Сохранить в базу только изменённые (dirty) объекты.
     *
     * <p>Если изменений нет, метод ничего не делает.
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
                    DatabaseFactory.getDatabase().updateOrder(order);
                    order.setDirty(false);
                    saved++;
                }
            }

            for (Client client : clients) {
                if (client.isDirty()) {
                    DatabaseFactory.getDatabase().updateClient(client);
                    client.setDirty(false);
                    saved++;
                }
            }

            for (Appointment a : appointments) {
                if (a.isDirty()) {
                    DatabaseFactory.getDatabase().updateAppointment(a);
                    a.setDirty(false);
                    saved++;
                }
            }

            for (SparePart sp : spareParts) {
                if (sp.isDirty()) {
                    DatabaseFactory.getDatabase().updateSparePartStock(sp, sp.getStock());
                    sp.setDirty(false);
                    saved++;
                }
            }

            for (ServiceSparePart ssp : serviceSpareParts) {
                if (ssp.isDirty()) {
                    DatabaseFactory.getDatabase().addServiceSparePart(ssp);
                    ssp.setDirty(false);
                    saved++;
                }
            }

            for (ServiceSparePartsList sspList : serviceSparePartsLists) {
                if (sspList.isDirty()) {
                    DatabaseFactory.getDatabase().addServiceSparePartsList(sspList);
                    // Элементы сохраняются автоматически внутри addServiceSparePartsList
                    sspList.setDirty(false);
                    saved++;
                }
            }

            for (ToPart tp : toParts) {
                if (tp.isDirty()) {
                    DatabaseFactory.getDatabase().updateToPart(tp);
                    tp.setDirty(false);
                    saved++;
                }
            }

            for (Setting setting : settings) {
                if (setting.isDirty()) {
                    DatabaseFactory.getDatabase().updateSetting(setting);
                    setting.markClean();
                    saved++;
                }
            }

            // Сохраняем serviceParts (связи услуги-запчасти)
            for (ServicePart sp : serviceParts) {
                if (sp.isDirty()) {
                    DatabaseFactory.getDatabase().updateServicePart(sp);
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
     * Пометить кэш как содержащий несохранённые изменения.
     */
    public static void markDirty() {
        isDirty = true;
    }

    // ==================== CLIENTS ====================

    /**
     * Получить кэш клиентов.
     * @return список клиентов
     */
    public static List<Client> getClients() { return clients; }

    /**
     * Перезагружает клиентов из БД (с автомобилями).
     * Возвращает свежий список, не затрагивая кэш DataStore.
     */
    public static List<Client> loadClients() {
        return DatabaseFactory.getDatabase().getAllClients();
    }

    /**
     * Добавить клиента в базу и обновить кэш.
     * @param c клиент для добавления
     */
    public static void addClient(Client c) {
        DatabaseFactory.getDatabase().addClient(c);
        clients = DatabaseFactory.getDatabase().getAllClients();
        isDirty = true;
    }

    /**
     * Обновить клиента в базе и обновить кэш.
     * @param client клиент с обновлёнными данными
     */
    public static void updateClient(Client client) {
        client.setDirty(true);
        DatabaseFactory.getDatabase().updateClient(client);
        clients = DatabaseFactory.getDatabase().getAllClients();
        isDirty = true;
    }

    /**
     * Удалить клиента из базы и обновить кэш.
     * @param c удаляемый клиент
     */
    public static void removeClient(Client c) {
        DatabaseFactory.getDatabase().deleteClient(c);
        clients = DatabaseFactory.getDatabase().getAllClients();
        isDirty = true;
    }

    /**
     * Удалить клиента и связанные с ним заказы.
     * @param client удаляемый клиент
     */
    public static void deleteClient(Client client) {
        if (client == null) {
            return;
        }
        List<WorkOrder> ordersToDelete = new ArrayList<>();
        for (WorkOrder order : orders) {
            Client orderClient = order.getClient();
            if (orderClient != null && orderClient.getId() == client.getId()) {
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

    /**
     * Добавить автомобиль клиента в базу и в кэш клиента.
     * @param car автомобиль для добавления
     * @throws java.sql.SQLException при ошибке сохранения
     */
    public static void addCar(Car car) throws java.sql.SQLException {
        DatabaseFactory.getDatabase().addCar(car);
        Client c = getClientById(car.getClientId());
        if (c != null) {
            c.getCars().add(car);
        }
        isDirty = true;
    }

    /**
     * Обновить автомобиль в базе.
     * @param car автомобиль с обновлёнными данными
     */
    public static void updateCar(Car car) {
        DatabaseFactory.getDatabase().updateCar(car);
        isDirty = true;
    }

    /**
     * Удалить автомобиль из базы и из кэша клиента.
     * @param car удаляемый автомобиль
     */
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

    /**
     * Получить кэш заказов.
     * @return список заказов
     */
    public static List<WorkOrder> getOrders() { return orders; }

    /**
     * Добавить заказ в базу и перезагрузить кэш заказов.
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
     * Обновить заказ в базе и перезагрузить кэш заказов.
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
        orders = DatabaseFactory.getDatabase().getAllOrders();
        isDirty = true;
    }

    /**
     * Удалить заказ из базы и кэша.
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
     * Получить количество незакрытых заказов.
     * @return число заказов, статус которых не равен {@link WorkOrder#STATUS_CLOSED}
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

    /**
     * Получить кэш услуг.
     * @return список услуг
     */
    public static List<Service> getServices() { return services; }

    /**
     * Добавить услугу в базу и перезагрузить кэш услуг.
     * @param s услуга для добавления
     */
    public static void addService(Service s) {
        DatabaseFactory.getDatabase().addService(s);
        services = DatabaseFactory.getDatabase().getAllServices();
        isDirty = true;
    }

    /**
     * Обновить услугу в базе и перезагрузить кэш.
     * @param s услуга с обновлёнными данными
     */
    public static void updateService(Service s) {
        DatabaseFactory.getDatabase().updateService(s);
        services = DatabaseFactory.getDatabase().getAllServices();
        isDirty = true;
    }

    /**
     * Удалить услугу из базы и перезагрузить кэш.
     * @param s удаляемая услуга
     */
    public static void removeService(Service s) {
        DatabaseFactory.getDatabase().deleteService(s);
        services = DatabaseFactory.getDatabase().getAllServices();
        isDirty = true;
    }

    /**
     * Найти услугу по названию в кэше.
     * @param name название услуги
     * @return услуга или {@code null}, если не найдена
     */
    public static Service getServiceByName(String name) {
        return services.stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Найти услугу по идентификатору в кэше.
     * @param id идентификатор услуги
     * @return услуга или {@code null}, если не найдена
     */
    public static Service getServiceById(int id) {
        return services.stream()
                .filter(s -> s.getId() == id)
                .findFirst()
                .orElse(null);
    }

    /**
     * Найти запчасть по идентификатору в кэше.
     * @param id идентификатор запчасти
     * @return запчасть или {@code null}, если не найдена
     */
    public static SparePart getSparePartById(int id) {
        return spareParts.stream()
                .filter(s -> s.getId() == id)
                .findFirst()
                .orElse(null);
    }

    // ==================== SPARE PARTS ====================

    /**
     * Получить кэш запчастей.
     * @return список запчастей
     */
    public static List<SparePart> getSpareParts() { return spareParts; }

    /**
     * Добавить запчасть в базу и перезагрузить кэш.
     * @param sp запчасть для добавления
     */
    public static void addSparePart(SparePart sp) {
        DatabaseFactory.getDatabase().addSparePart(sp);
        spareParts = DatabaseFactory.getDatabase().getAllSpareParts();
        isDirty = true;
    }

    /**
     * Обновить запчасть в базе и перезагрузить кэш.
     * @param sp запчасть с обновлёнными данными
     */
    public static void updateSparePart(SparePart sp) {
        sp.setDirty(true);
        DatabaseFactory.getDatabase().updateSparePart(sp);
        spareParts = DatabaseFactory.getDatabase().getAllSpareParts();
        isDirty = true;
    }

    /**
     * Удалить запчасть из базы и перезагрузить кэш.
     * @param sp удаляемая запчасть
     */
    public static void removeSparePart(SparePart sp) {
        DatabaseFactory.getDatabase().deleteSparePart(sp);
        spareParts = DatabaseFactory.getDatabase().getAllSpareParts();
        isDirty = true;
    }

    /**
     * Обновить остаток запчасти в базе и кэше.
     * @param part запчасть
     * @param newStock новый остаток
     */
    public static void updateSparePartStock(SparePart part, double newStock) {
        part.setDirty(true);
        part.setStock(newStock);
        DatabaseFactory.getDatabase().updateSparePartStock(part, newStock);
        spareParts = DatabaseFactory.getDatabase().getAllSpareParts();
        isDirty = true;
    }

    /**
     * Уменьшить остаток запчасти, привязанной к услуге.
     *
     * <p>Берётся первая запчасть из списка запчастей услуги; остаток уменьшается на {@code qty}.
     * @param serviceName название услуги
     * @param qty количество для списания
     */
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

    /**
     * Получить кэш записей на обслуживание.
     * @return список записей
     */
    public static List<Appointment> getAppointments() {
        return appointments;
    }

    /**
     * Получить записи на обслуживание на указанную дату.
     * @param date дата в формате {@code dd/MM/yyyy}
     * @return список записей
     */
    public static List<Appointment> getAppointmentsByDate(String date) {
        return DatabaseFactory.getDatabase().getAppointmentsByDate(date);
    }

    /**
     * Найти запись на обслуживание по идентификатору заказа в кэше.
     * @param orderId идентификатор заказа
     * @return запись или {@code null}, если не найдена
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

    /**
     * Добавить запись на обслуживание и перезагрузить кэш.
     * @param a запись для добавления
     */
    public static void addAppointment(Appointment a) {
        DatabaseFactory.getDatabase().addAppointment(a);
        appointments = DatabaseFactory.getDatabase().getAllAppointments();
        isDirty = true;
    }

    /**
     * Обновить запись на обслуживание и перезагрузить кэш.
     * @param a запись с обновлёнными данными
     */
    public static void updateAppointment(Appointment a) {
        a.setDirty(true);
        DatabaseFactory.getDatabase().updateAppointment(a);
        appointments = DatabaseFactory.getDatabase().getAllAppointments();
        isDirty = true;
    }

    /**
     * Удалить запись на обслуживание из базы и кэша.
     * @param id идентификатор записи
     */
    public static void deleteAppointment(int id) {
        DatabaseFactory.getDatabase().deleteAppointment(id);
        appointments.removeIf(a -> a.getId() == id);
        isDirty = true;
    }

    /**
     * Найти запись на обслуживание по идентификатору в кэше.
     * @param id идентификатор записи
     * @return запись или {@code null}, если не найдена
     */
    public static Appointment getAppointmentById(int id) {
        return appointments.stream()
                .filter(a -> a.getId() == id)
                .findFirst()
                .orElse(null);
    }

    // ==================== SERVICE-SPARE PART RELATIONSHIPS ====================

    /**
     * Получить связи услуги с запчастями.
     * @param serviceId идентификатор услуги; {@code -1} — вернуть кэш всех связей
     * @return список связей
     */
    public static List<ServiceSparePart> getServiceSparePartsByServiceId(int serviceId) {
        if (serviceId == -1) {
            return serviceSpareParts;
        }
        return DatabaseFactory.getDatabase().getServiceSparePartsByServiceId(serviceId);
    }

    /**
     * Добавить или обновить связь услуги с запчастью.
     *
     * <p>Если связь уже есть в кэше, обновляются её количество, единица измерения и признак активности.
     * @param relation связь для добавления
     */
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

    /**
     * Удалить связь услуги с запчастью из базы и кэша.
     * @param relation удаляемая связь
     */
    public static void deleteServiceSparePart(ServiceSparePart relation) {
        DatabaseFactory.getDatabase().deleteServiceSparePart(relation);
        serviceSpareParts.remove(relation);
        isDirty = true;
    }

    /**
     * Удалить все связи услуги с запчастями из базы и кэша.
     * @param serviceId идентификатор услуги
     */
    public static void deleteServiceSparePartsByServiceId(int serviceId) {
        DatabaseFactory.getDatabase().deleteServiceSparePartsByServiceId(serviceId);
        serviceSpareParts.removeIf(s -> s.getServiceId() == serviceId);
        isDirty = true;
    }

    /**
     * Удалить дубликаты связей услуги с запчастями и перезагрузить кэш связей.
     */
    public static void deleteDuplicateServiceSpareParts() {
        DatabaseFactory.getDatabase().deleteDuplicateServiceSpareParts();
        // Перезагружаем данные из БД
        serviceSpareParts = DatabaseFactory.getDatabase().getServiceSparePartsByServiceId(-1);
        isDirty = true;
    }

    // ==================== SERVICE-PART RELATIONSHIPS (NEW STRUCTURE) ====================

    /**
     * Получить кэш связей услуг с запчастями.
     * @return список связей
     */
    public static List<ServicePart> getAllServiceParts() {
        return serviceParts;
    }

    /**
     * Получить связи услуги с запчастями из кэша.
     * @param serviceId идентификатор услуги
     * @return список связей
     */
    public static List<ServicePart> getServicePartsByServiceId(int serviceId) {
        return serviceParts.stream()
                .filter(p -> p.getServiceId() == serviceId)
                .toList();
    }

    /**
     * Добавить связь услуги с запчастью в базу и кэш.
     * @param part связь для добавления
     */
    public static void addServicePart(ServicePart part) {
        DatabaseFactory.getDatabase().addServicePart(part);
        serviceParts.add(part);
        isDirty = true;
    }

    /**
     * Обновить связь услуги с запчастью и перезагрузить кэш.
     * @param part связь с обновлёнными данными
     */
    public static void updateServicePart(ServicePart part) {
        part.setDirty(true);
        DatabaseFactory.getDatabase().updateServicePart(part);
        serviceParts = DatabaseFactory.getDatabase().getAllServiceParts();
        isDirty = true;
    }

    /**
     * Удалить связь услуги с запчастью из базы и кэша.
     * @param part удаляемая связь
     */
    public static void deleteServicePart(ServicePart part) {
        DatabaseFactory.getDatabase().deleteServicePart(part);
        serviceParts.remove(part);
        isDirty = true;
    }

    /**
     * Удалить все связи услуги с запчастями из базы и кэша.
     * @param serviceId идентификатор услуги
     */
    public static void deleteServicePartsByServiceId(int serviceId) {
        DatabaseFactory.getDatabase().deleteServicePartsByServiceId(serviceId);
        serviceParts.removeIf(s -> s.getServiceId() == serviceId);
        isDirty = true;
    }

    /**
     * Удалить все связи запчасти с услугами из базы и кэша.
     * @param sparePartId идентификатор запчасти
     */
    public static void deleteServicePartsBySparePartId(int sparePartId) {
        DatabaseFactory.getDatabase().deleteServicePartsBySparePartId(sparePartId);
        serviceParts.removeIf(s -> s.getSparePartId() == sparePartId);
        isDirty = true;
    }

    /**
     * Найти связь услуги с запчастью по идентификатору в кэше.
     * @param id идентификатор связи
     * @return связь или {@code null}, если не найдена
     */
    public static ServicePart getServicePartById(int id) {
        return serviceParts.stream()
                .filter(s -> s.getId() == id)
                .findFirst()
                .orElse(null);
    }

    // ==================== SERVICE-SPARE PARTS LISTS (NEW STRUCTURE) ====================

    /**
     * Получить списки запчастей услуги.
     * @param serviceId идентификатор услуги; {@code -1} — вернуть кэш всех списков
     * @return список списков
     */
    public static List<ServiceSparePartsList> getServiceSparePartsListsByServiceId(int serviceId) {
        if (serviceId == -1) {
            return serviceSparePartsLists;
        }
        return DatabaseFactory.getDatabase().getServiceSparePartsListsByServiceId(serviceId);
    }

    /**
     * Получить элементы списка запчастей из базы.
     * @param listId идентификатор списка
     * @return список элементов
     */
    public static List<ServiceSparePartsListItem> getServiceSparePartsListItems(int listId) {
        return DatabaseFactory.getDatabase().getServiceSparePartsListItems(listId);
    }

    /**
     * Добавить список запчастей с элементами и перезагрузить элементы из базы.
     * @param list список для добавления
     */
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

    /**
     * Добавить элемент списка запчастей.
     * @param item элемент для добавления
     */
    public static void addServiceSparePartsListItem(ServiceSparePartsListItem item) {
        DatabaseFactory.getDatabase().addServiceSparePartsListItem(item);
        serviceSparePartsListItems.add(item);
        isDirty = true;
    }

    /**
     * Удалить список запчастей из базы и кэша.
     * @param list удаляемый список
     */
    public static void deleteServiceSparePartsList(ServiceSparePartsList list) {
        DatabaseFactory.getDatabase().deleteServiceSparePartsList(list);
        serviceSparePartsLists.remove(list);
        isDirty = true;
    }

    /**
     * Удалить все списки запчастей услуги из базы и кэша.
     * @param serviceId идентификатор услуги
     */
    public static void deleteServiceSparePartsListsByServiceId(int serviceId) {
        DatabaseFactory.getDatabase().deleteServiceSparePartsListsByServiceId(serviceId);
        serviceSparePartsLists.removeIf(s -> s.getServiceId() == serviceId);
        isDirty = true;
    }

    // ==================== TO PARTS ====================

    /**
     * Получить расходники TO для модели автомобиля.
     * @param carModel модель автомобиля; {@code null} или пустая строка — вернуть кэш всех расходников
     * @return список расходников
     */
    public static List<ToPart> getToPartsByCarModel(String carModel) {
        if (carModel == null || carModel.isEmpty()) {
            return toParts;
        }
        return DatabaseFactory.getDatabase().getToPartsByCarModel(carModel);
    }

    /**
     * Получить все модели автомобилей с расходниками TO.
     * @return список моделей без дубликатов
     */
    public static List<String> getAllCarModels() {
        return DatabaseFactory.getDatabase().getAllCarModels();
    }

    /**
     * Добавить расходник TO в базу и кэш.
     * @param part расходник для добавления
     */
    public static void addToPart(ToPart part) {
        DatabaseFactory.getDatabase().addToPart(part);
        toParts.add(part);
        isDirty = true;
    }

    /**
     * Обновить расходник TO и перезагрузить кэш.
     * @param part расходник с обновлёнными данными
     */
    public static void updateToPart(ToPart part) {
        part.setDirty(true);
        DatabaseFactory.getDatabase().updateToPart(part);
        toParts = DatabaseFactory.getDatabase().getToPartsByCarModel("");
        isDirty = true;
    }

    /**
     * Удалить расходник TO из базы и кэша.
     * @param part удаляемый расходник
     */
    public static void deleteToPart(ToPart part) {
        DatabaseFactory.getDatabase().deleteToPart(part);
        toParts.remove(part);
        isDirty = true;
    }

    /**
     * Удалить расходники TO для модели автомобиля.
     * @param carModel модель автомобиля
     */
    public static void deleteToPartsByCarModel(String carModel) {
        DatabaseFactory.getDatabase().deleteToPartsByCarModel(carModel);
        toParts.removeIf(t -> t.getCarModel().equals(carModel));
        isDirty = true;
    }

    // ==================== SETTINGS ====================

    /**
     * Получить кэш настроек.
     * @return список настроек
     */
    public static List<Setting> getAllSettings() {
        return settings;
    }

    /**
     * Добавить настройку в базу и кэш.
     * @param setting настройка для добавления
     */
    public static void addSetting(Setting setting) {
        DatabaseFactory.getDatabase().addSetting(setting);
        settings.add(setting);
        isDirty = true;
    }

    /**
     * Обновить настройку в базе и заменить её в кэше.
     * @param setting настройка с новым значением
     */
    public static void updateSetting(Setting setting) {
        logger.info("DataStore.updateSetting: setting id={}, key={}, value=[REDACTED], dirty={}", 
                setting.getId(), maskSensitive(setting.getKey()), setting.isDirty());
        setting.setDirty(true);
        DatabaseFactory.getDatabase().updateSetting(setting);
        logger.info("  DataStore.updateSetting completed");
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

    /**
     * Удалить настройку из базы и кэша.
     * @param setting удаляемая настройка
     */
    public static void deleteSetting(Setting setting) {
        DatabaseFactory.getDatabase().deleteSetting(setting);
        settings.remove(setting);
        isDirty = true;
    }

    /**
     * Получить настройку по ключу из базы.
     * @param key ключ настройки
     * @return настройка или {@code null}, если не найдена
     */
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