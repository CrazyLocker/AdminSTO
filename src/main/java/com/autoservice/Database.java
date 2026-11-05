package com.autoservice;

import java.util.List;

/**
 * Обертка над DatabaseFactory для удобства использования.
 * Все методы передаются в DatabaseFactory.
 */
public class Database {
    
    // ==================== Инициализация ====================
    
    /**
     * Инициализировать подключение к базе данных для production.
     */
    public static void init() {
        DatabaseFactory.init();
    }
    
    /**
     * Инициализировать подключение к тестовой базе данных (H2 in-memory).
     */
    public static void initForTest() {
        DatabaseFactory.initForTest();
    }
    
    /**
     * Закрыть пул соединений и освободить ресурсы.
     */
    public static void close() {
        DatabaseFactory.close();
    }
    
    // ==================== CLIENTS ====================
    
    /**
     * Получить всех клиентов вместе с их автомобилями.
     * @return список клиентов; пустой список, если клиентов нет
     */
    public static List<Client> getAllClients() {
        return DatabaseFactory.getDatabase().getAllClients();
    }
    
    /**
     * Добавить клиента.
     * @param client клиент для добавления; после вызова получает сгенерированный ID
     */
    public static void addClient(Client client) {
        DatabaseFactory.getDatabase().addClient(client);
    }
    
    /**
     * Обновить данные клиента.
     * @param client клиент с обновлёнными данными
     */
    public static void updateClient(Client client) {
        DatabaseFactory.getDatabase().updateClient(client);
    }
    
    /**
     * Удалить клиента.
     * @param client удаляемый клиент
     */
    public static void deleteClient(Client client) {
        DatabaseFactory.getDatabase().deleteClient(client);
    }
    
    /**
     * Получить клиента по идентификатору.
     * @param id идентификатор клиента
     * @return клиент или {@code null}, если не найден
     */
    public static Client getClientById(int id) {
        return DatabaseFactory.getDatabase().getClientById(id);
    }
    
    /**
     * Получить идентификатор клиента по его имени, фамилии и телефону.
     * @param client клиент
     * @return идентификатор или {@code -1}, если клиент не найден
     */
    public static int getClientId(Client client) {
        return DatabaseFactory.getDatabase().getClientId(client);
    }
    
    // ==================== SERVICES ====================
    
    /**
     * Получить все услуги.
     * @return список услуг, отсортированный по названию
     */
    public static List<Service> getAllServices() {
        return DatabaseFactory.getDatabase().getAllServices();
    }
    
    /**
     * Добавить услугу.
     * @param service услуга для добавления; после вызова получает сгенерированный ID
     */
    public static void addService(Service service) {
        DatabaseFactory.getDatabase().addService(service);
    }
    
    /**
     * Обновить услугу.
     * @param service услуга с обновлёнными данными
     */
    public static void updateService(Service service) {
        DatabaseFactory.getDatabase().updateService(service);
    }
    
    /**
     * Удалить услугу.
     * @param service удаляемая услуга
     */
    public static void deleteService(Service service) {
        DatabaseFactory.getDatabase().deleteService(service);
    }
    
    // ==================== SPARE PARTS ====================
    
    /**
     * Получить все запчасти.
     * @return список запчастей, отсортированный по названию
     */
    public static List<SparePart> getAllSpareParts() {
        return DatabaseFactory.getDatabase().getAllSpareParts();
    }
    
    /**
     * Добавить запчасть.
     * @param part запчасть для добавления; после вызова получает сгенерированный ID
     */
    public static void addSparePart(SparePart part) {
        DatabaseFactory.getDatabase().addSparePart(part);
    }
    
    /**
     * Обновить запчасть.
     * @param part запчасть с обновлёнными данными
     */
    public static void updateSparePart(SparePart part) {
        DatabaseFactory.getDatabase().updateSparePart(part);
    }
    
    /**
     * Удалить запчасть.
     * @param part удаляемая запчасть
     */
    public static void deleteSparePart(SparePart part) {
        DatabaseFactory.getDatabase().deleteSparePart(part);
    }
    
    /**
     * Обновить остаток запчасти.
     * @param part запчасть
     * @param newStock новый остаток
     */
    public static void updateSparePartStock(SparePart part, double newStock) {
        DatabaseFactory.getDatabase().updateSparePartStock(part, newStock);
    }
    
    // ==================== ORDERS ====================
    
    /**
     * Получить все заказы вместе с услугами и запчастями.
     * @return список заказов, отсортированный по дате создания (новые первыми)
     */
    public static List<WorkOrder> getAllOrders() {
        return DatabaseFactory.getDatabase().getAllOrders();
    }
    
    /**
     * Добавить заказ.
     * @param order заказ для добавления; после вызова получает сгенерированный ID
     */
    public static void addOrder(WorkOrder order) {
        DatabaseFactory.getDatabase().addOrder(order);
    }
    
    /**
     * Обновить заказ, его услуги и запчасти.
     * @param order заказ с обновлёнными данными
     */
    public static void updateOrder(WorkOrder order) {
        DatabaseFactory.getDatabase().updateOrder(order);
    }
    
    /**
     * Удалить заказ.
     * @param orderId идентификатор заказа
     */
    public static void deleteOrder(String orderId) {
        DatabaseFactory.getDatabase().deleteOrder(orderId);
    }
    
    // ==================== APPOINTMENTS ====================
    
    /**
     * Получить все записи на обслуживание.
     * @return список записей, отсортированный по дате и времени
     */
    public static List<Appointment> getAllAppointments() {
        return DatabaseFactory.getDatabase().getAllAppointments();
    }
    
    /**
     * Добавить запись на обслуживание.
     * @param appointment запись для добавления; после вызова получает сгенерированный ID
     */
    public static void addAppointment(Appointment appointment) {
        DatabaseFactory.getDatabase().addAppointment(appointment);
    }
    
    /**
     * Обновить запись на обслуживание.
     * @param appointment запись с обновлёнными данными
     */
    public static void updateAppointment(Appointment appointment) {
        DatabaseFactory.getDatabase().updateAppointment(appointment);
    }
    
    /**
     * Удалить запись на обслуживание.
     * @param id идентификатор записи
     */
    public static void deleteAppointment(int id) {
        DatabaseFactory.getDatabase().deleteAppointment(id);
    }
    
    /**
     * Получить записи на обслуживание на указанную дату.
     * @param date дата в формате {@code dd/MM/yyyy}
     * @return список записей, отсортированный по времени
     */
    public static List<Appointment> getAppointmentsByDate(String date) {
        return DatabaseFactory.getDatabase().getAppointmentsByDate(date);
    }
    
    // ==================== SERVICE-SPARE PART RELATIONSHIPS ====================
    
    /**
     * Получить связи услуги с запчастями.
     * @param serviceId идентификатор услуги; {@code -1} — вернуть все связи
     * @return список связей
     */
    public static List<com.autoservice.model.ServiceSparePart> getServiceSparePartsByServiceId(int serviceId) {
        return DatabaseFactory.getDatabase().getServiceSparePartsByServiceId(serviceId);
    }
    
    /**
     * Добавить связь услуги с запчастью.
     * @param relation связь для добавления; после вызова получает сгенерированный ID
     */
    public static void addServiceSparePart(com.autoservice.model.ServiceSparePart relation) {
        DatabaseFactory.getDatabase().addServiceSparePart(relation);
    }
    
    /**
     * Удалить связь услуги с запчастью.
     * @param relation удаляемая связь
     */
    public static void deleteServiceSparePart(com.autoservice.model.ServiceSparePart relation) {
        DatabaseFactory.getDatabase().deleteServiceSparePart(relation);
    }
    
    /**
     * Удалить все связи услуги с запчастями.
     *
     * <p>Вызов с {@code serviceId == -1} блокируется для защиты от массового удаления.
     * @param serviceId идентификатор услуги
     */
    public static void deleteServiceSparePartsByServiceId(int serviceId) {
        DatabaseFactory.getDatabase().deleteServiceSparePartsByServiceId(serviceId);
    }
    
    /**
     * Удалить дубликаты связей услуги с запчастями, оставив по одной связи на пару «услуга — запчасть».
     */
    public static void deleteDuplicateServiceSpareParts() {
        DatabaseFactory.getDatabase().deleteDuplicateServiceSpareParts();
    }
    
    // ==================== SERVICE-PART RELATIONSHIPS (NEW STRUCTURE) ====================
    
    /**
     * Получить все связи услуг с запчастями (новая структура).
     * @return список связей
     */
    public static List<com.autoservice.model.ServicePart> getAllServiceParts() {
        return DatabaseFactory.getDatabase().getAllServiceParts();
    }
    
    /**
     * Добавить связь услуги с запчастью (новая структура).
     * @param part связь для добавления; после вызова получает сгенерированный ID
     */
    public static void addServicePart(com.autoservice.model.ServicePart part) {
        DatabaseFactory.getDatabase().addServicePart(part);
    }
    
    /**
     * Обновить связь услуги с запчастью (новая структура).
     * @param part связь с обновлёнными данными
     */
    public static void updateServicePart(com.autoservice.model.ServicePart part) {
        DatabaseFactory.getDatabase().updateServicePart(part);
    }
    
    /**
     * Удалить связь услуги с запчастью (новая структура).
     * @param part удаляемая связь
     */
    public static void deleteServicePart(com.autoservice.model.ServicePart part) {
        DatabaseFactory.getDatabase().deleteServicePart(part);
    }
    
    /**
     * Удалить все связи услуги с запчастями (новая структура).
     *
     * <p>Вызов с {@code serviceId == -1} блокируется для защиты от массового удаления.
     * @param serviceId идентификатор услуги
     */
    public static void deleteServicePartsByServiceId(int serviceId) {
        DatabaseFactory.getDatabase().deleteServicePartsByServiceId(serviceId);
    }
    
    /**
     * Удалить все связи запчасти с услугами (новая структура).
     *
     * <p>Вызов с {@code sparePartId == -1} блокируется для защиты от массового удаления.
     * @param sparePartId идентификатор запчасти
     */
    public static void deleteServicePartsBySparePartId(int sparePartId) {
        DatabaseFactory.getDatabase().deleteServicePartsBySparePartId(sparePartId);
    }
    
    /**
     * Получить списки запчастей услуги.
     * @param serviceId идентификатор услуги; {@code -1} — вернуть все списки
     * @return список списков, отсортированный по дате создания (новые первыми)
     */
    public static List<com.autoservice.model.ServiceSparePartsList> getServiceSparePartsListsByServiceId(int serviceId) {
        return DatabaseFactory.getDatabase().getServiceSparePartsListsByServiceId(serviceId);
    }
    
    // ==================== SERVICE-SPARE PARTS LISTS (NEW STRUCTURE) ====================
    
    /**
     * Получить элементы списка запчастей.
     * @param listId идентификатор списка
     * @return список элементов
     */
    public static List<com.autoservice.model.ServiceSparePartsListItem> getServiceSparePartsListItems(int listId) {
        return DatabaseFactory.getDatabase().getServiceSparePartsListItems(listId);
    }
    
    /**
     * Добавить список запчастей вместе с его элементами.
     * @param list список для добавления; после вызова получает сгенерированный ID
     */
    public static void addServiceSparePartsList(com.autoservice.model.ServiceSparePartsList list) {
        DatabaseFactory.getDatabase().addServiceSparePartsList(list);
    }
    
    /**
     * Добавить элемент списка запчастей.
     * @param item элемент для добавления; после вызова получает сгенерированный ID
     */
    public static void addServiceSparePartsListItem(com.autoservice.model.ServiceSparePartsListItem item) {
        DatabaseFactory.getDatabase().addServiceSparePartsListItem(item);
    }
    
    /**
     * Удалить список запчастей вместе с его элементами.
     * @param list удаляемый список
     */
    public static void deleteServiceSparePartsList(com.autoservice.model.ServiceSparePartsList list) {
        DatabaseFactory.getDatabase().deleteServiceSparePartsList(list);
    }
    
    /**
     * Удалить все списки запчастей услуги вместе с их элементами.
     *
     * <p>Вызов с {@code serviceId == -1} блокируется для защиты от массового удаления.
     * @param serviceId идентификатор услуги
     */
    public static void deleteServiceSparePartsListsByServiceId(int serviceId) {
        DatabaseFactory.getDatabase().deleteServiceSparePartsListsByServiceId(serviceId);
    }
    
    /**
     * Удалить все элементы списка запчастей.
     * @param listId идентификатор списка
     */
    public static void deleteServiceSparePartsListItemsByListId(int listId) {
        DatabaseFactory.getDatabase().deleteServiceSparePartsListItemsByListId(listId);
    }
    
    // ==================== TO PARTS RELATIONSHIPS ====================
    
    /**
     * Получить расходники TO для модели автомобиля.
     * @param carModel модель автомобиля; {@code null} или пустая строка — вернуть все расходники
     * @return список расходников
     */
    public static List<com.autoservice.model.ToPart> getToPartsByCarModel(String carModel) {
        return DatabaseFactory.getDatabase().getToPartsByCarModel(carModel);
    }
    
    /**
     * Добавить расходник TO.
     * @param part расходник для добавления; после вызова получает сгенерированный ID
     */
    public static void addToPart(com.autoservice.model.ToPart part) {
        DatabaseFactory.getDatabase().addToPart(part);
    }
    
    /**
     * Обновить расходник TO.
     * @param part расходник с обновлёнными данными
     */
    public static void updateToPart(com.autoservice.model.ToPart part) {
        DatabaseFactory.getDatabase().updateToPart(part);
    }
    
    /**
     * Удалить расходник TO.
     * @param part удаляемый расходник
     */
    public static void deleteToPart(com.autoservice.model.ToPart part) {
        DatabaseFactory.getDatabase().deleteToPart(part);
    }
    
    /**
     * Удалить расходники TO для модели автомобиля.
     * @param carModel модель автомобиля; {@code null} или пустая строка — удалить все расходники
     */
    public static void deleteToPartsByCarModel(String carModel) {
        DatabaseFactory.getDatabase().deleteToPartsByCarModel(carModel);
    }
    
    // ==================== SETTINGS ====================
    
    /**
     * Получить все настройки приложения.
     * @return список настроек
     */
    public static List<com.autoservice.model.Setting> getAllSettings() {
        return DatabaseFactory.getDatabase().getAllSettings();
    }
    
    /**
     * Добавить настройку.
     * @param setting настройка для добавления; после вызова получает сгенерированный ID
     */
    public static void addSetting(com.autoservice.model.Setting setting) {
        DatabaseFactory.getDatabase().addSetting(setting);
    }
    
    /**
     * Обновить значение настройки.
     * @param setting настройка с новым значением
     */
    public static void updateSetting(com.autoservice.model.Setting setting) {
        DatabaseFactory.getDatabase().updateSetting(setting);
    }
    
    /**
     * Удалить настройку.
     * @param setting удаляемая настройка
     */
    public static void deleteSetting(com.autoservice.model.Setting setting) {
        DatabaseFactory.getDatabase().deleteSetting(setting);
    }
    
    /**
     * Получить настройку по ключу.
     * @param key ключ настройки
     * @return настройка или {@code null}, если не найдена
     */
    public static com.autoservice.model.Setting getSettingByKey(String key) {
        return DatabaseFactory.getDatabase().getSettingByKey(key);
    }
}

