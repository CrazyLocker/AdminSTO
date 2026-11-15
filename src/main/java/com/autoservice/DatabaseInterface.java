package com.autoservice;

import java.sql.Connection;
import java.sql.SQLException;
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
    
    /**
     * Получить всех клиентов вместе с их автомобилями.
     * @return список клиентов; пустой список, если клиентов нет
     */
    List<Client> getAllClients();
    
    /**
     * Добавить клиента.
     * @param client клиент для добавления; после вызова получает сгенерированный ID
     */
    void addClient(Client client);
    
    /**
     * Обновить данные клиента.
     * @param client клиент с обновлёнными данными
     */
    void updateClient(Client client);
    
    /**
     * Удалить клиента.
     * @param client удаляемый клиент
     */
    void deleteClient(Client client);
    
    /**
     * Получить клиента по идентификатору.
     * @param id идентификатор клиента
     * @return клиент или {@code null}, если не найден
     */
    Client getClientById(int id);
    
    /**
     * Получить идентификатор клиента по его имени, фамилии и телефону.
     * @param client клиент
     * @return идентификатор или {@code -1}, если клиент не найден
     */
    int getClientId(Client client);
    
    // ==================== CAR ====================
    
    /**
     * Получить автомобили клиента.
     * @param clientId идентификатор клиента
     * @return список автомобилей; пустой список, если автомобилей нет
     */
    List<Car> getCarsByClientId(int clientId);
    
    /**
     * Добавить автомобиль клиента.
     * @param car автомобиль для добавления
     * @throws SQLException при ошибке сохранения
     */
    void addCar(Car car) throws SQLException;
    
    /**
     * Обновить данные автомобиля.
     * @param car автомобиль с обновлёнными данными
     */
    void updateCar(Car car);
    
    /**
     * Удалить автомобиль.
     * @param car удаляемый автомобиль
     */
    void deleteCar(Car car);
    
    // ==================== SERVICES ====================
    
    /**
     * Получить все услуги.
     * @return список услуг, отсортированный по названию
     */
    List<Service> getAllServices();
    
    /**
     * Добавить услугу.
     * @param service услуга для добавления; после вызова получает сгенерированный ID
     */
    void addService(Service service);
    
    /**
     * Обновить услугу.
     * @param service услуга с обновлёнными данными
     */
    void updateService(Service service);
    
    /**
     * Удалить услугу.
     * @param service удаляемая услуга
     */
    void deleteService(Service service);
    
    // ==================== SPARE PARTS ====================
    
    /**
     * Получить все запчасти.
     * @return список запчастей, отсортированный по названию
     */
    List<SparePart> getAllSpareParts();
    
    /**
     * Добавить запчасть.
     * @param part запчасть для добавления; после вызова получает сгенерированный ID
     */
    void addSparePart(SparePart part);
    
    /**
     * Обновить запчасть.
     * @param part запчасть с обновлёнными данными
     */
    void updateSparePart(SparePart part);
    
    /**
     * Удалить запчасть.
     * @param part удаляемая запчасть
     */
    void deleteSparePart(SparePart part);
    
    /**
     * Обновить остаток запчасти.
     * @param part запчасть
     * @param newStock новый остаток
     */
    void updateSparePartStock(SparePart part, double newStock);
    
    // ==================== ORDERS ====================
    
    /**
     * Получить все заказы вместе с услугами и запчастями.
     * @return список заказов, отсортированный по дате создания (новые первыми)
     */
    List<WorkOrder> getAllOrders();
    
    /**
     * Добавить заказ.
     * @param order заказ для добавления; после вызова получает сгенерированный ID
     */
    void addOrder(WorkOrder order);
    
    /**
     * Обновить заказ, его услуги и запчасти.
     * @param order заказ с обновлёнными данными
     */
    void updateOrder(WorkOrder order);
    
    /**
     * Удалить заказ.
     * @param orderId идентификатор заказа
     */
    void deleteOrder(String orderId);
    
    // ==================== APPOINTMENTS ====================
    
    /**
     * Получить все записи на обслуживание.
     * @return список записей, отсортированный по дате и времени
     */
    List<Appointment> getAllAppointments();
    
    /**
     * Добавить запись на обслуживание.
     * @param appointment запись для добавления; после вызова получает сгенерированный ID
     */
    void addAppointment(Appointment appointment);
    
    /**
     * Обновить запись на обслуживание.
     * @param appointment запись с обновлёнными данными
     */
    void updateAppointment(Appointment appointment);
    
    /**
     * Удалить запись на обслуживание.
     * @param id идентификатор записи
     */
    void deleteAppointment(int id);
    
    /**
     * Получить запись на обслуживание по идентификатору.
     * @param id идентификатор записи
     * @return запись или {@code null}, если не найдена
     */
    Appointment getAppointmentById(int id);
    
    /**
     * Получить записи на обслуживание на указанную дату.
     * @param date дата в формате {@code dd/MM/yyyy}
     * @return список записей, отсортированный по времени
     */
    List<Appointment> getAppointmentsByDate(String date);
    
    // ==================== SERVICE-SPARE PART RELATIONSHIPS ====================
    
    /**
     * Получить связи услуги с запчастями.
     * @param serviceId идентификатор услуги; {@code -1} — вернуть все связи
     * @return список связей
     */
    List<com.autoservice.model.ServiceSparePart> getServiceSparePartsByServiceId(int serviceId);
    
    /**
     * Добавить связь услуги с запчастью.
     * @param relation связь для добавления; после вызова получает сгенерированный ID
     */
    void addServiceSparePart(com.autoservice.model.ServiceSparePart relation);

    /**
     * Обновить существующую связь услуги с запчастью по её идентификатору.
     *
     * <p>Используется вместо повторного {@code INSERT}, чтобы повторное добавление
     * пары «услуга — запчасть» не создавало дубликат в таблице.
     * @param relation связь с новыми значениями; {@code id} должен быть задан
     */
    void updateServiceSparePart(com.autoservice.model.ServiceSparePart relation);
    
    /**
     * Удалить связь услуги с запчастью.
     * @param relation удаляемая связь
     */
    void deleteServiceSparePart(com.autoservice.model.ServiceSparePart relation);
    
    /**
     * Удалить все связи услуги с запчастями.
     *
     * <p>Вызов с {@code serviceId == -1} блокируется для защиты от массового удаления.
     * @param serviceId идентификатор услуги
     */
    void deleteServiceSparePartsByServiceId(int serviceId);
    
    // ==================== SERVICE-PART RELATIONSHIPS (NEW STRUCTURE) ====================
    
    /**
     * Получить все связи услуг с запчастями (новая структура).
     * @return список связей
     */
    List<com.autoservice.model.ServicePart> getAllServiceParts();
    
    /**
     * Добавить связь услуги с запчастью (новая структура).
     * @param part связь для добавления; после вызова получает сгенерированный ID
     */
    void addServicePart(com.autoservice.model.ServicePart part);
    
    /**
     * Обновить связь услуги с запчастью (новая структура).
     * @param part связь с обновлёнными данными
     */
    void updateServicePart(com.autoservice.model.ServicePart part);
    
    /**
     * Удалить связь услуги с запчастью (новая структура).
     * @param part удаляемая связь
     */
    void deleteServicePart(com.autoservice.model.ServicePart part);
    
    /**
     * Удалить все связи услуги с запчастями (новая структура).
     *
     * <p>Вызов с {@code serviceId == -1} блокируется для защиты от массового удаления.
     * @param serviceId идентификатор услуги
     */
    void deleteServicePartsByServiceId(int serviceId);
    
    /**
     * Удалить все связи запчасти с услугами (новая структура).
     *
     * <p>Вызов с {@code sparePartId == -1} блокируется для защиты от массового удаления.
     * @param sparePartId идентификатор запчасти
     */
    void deleteServicePartsBySparePartId(int sparePartId);
    
    // ==================== SERVICE-SPARE PARTS LISTS (NEW STRUCTURE) ====================
    
    /**
     * Получить списки запчастей услуги.
     * @param serviceId идентификатор услуги; {@code -1} — вернуть все списки
     * @return список списков, отсортированный по дате создания (новые первыми)
     */
    List<com.autoservice.model.ServiceSparePartsList> getServiceSparePartsListsByServiceId(int serviceId);
    
    /**
     * Получить элементы списка запчастей.
     * @param listId идентификатор списка
     * @return список элементов
     */
    List<com.autoservice.model.ServiceSparePartsListItem> getServiceSparePartsListItems(int listId);
    
    /**
     * Добавить список запчастей вместе с его элементами.
     * @param list список для добавления; после вызова получает сгенерированный ID
     */
    void addServiceSparePartsList(com.autoservice.model.ServiceSparePartsList list);
    
    /**
     * Добавить элемент списка запчастей.
     * @param item элемент для добавления; после вызова получает сгенерированный ID
     */
    void addServiceSparePartsListItem(com.autoservice.model.ServiceSparePartsListItem item);
    
    /**
     * Удалить список запчастей вместе с его элементами.
     * @param list удаляемый список
     */
    void deleteServiceSparePartsList(com.autoservice.model.ServiceSparePartsList list);
    
    /**
     * Удалить все списки запчастей услуги вместе с их элементами.
     *
     * <p>Вызов с {@code serviceId == -1} блокируется для защиты от массового удаления.
     * @param serviceId идентификатор услуги
     */
    void deleteServiceSparePartsListsByServiceId(int serviceId);
    
    /**
     * Удалить все элементы списка запчастей.
     * @param listId идентификатор списка
     */
    void deleteServiceSparePartsListItemsByListId(int listId);
    
    // ==================== TO PARTS RELATIONSHIPS ====================
    
    /**
     * Получить расходники TO для модели автомобиля.
     * @param carModel модель автомобиля; {@code null} или пустая строка — вернуть все расходники
     * @return список расходников
     */
    List<com.autoservice.model.ToPart> getToPartsByCarModel(String carModel);
    
    /**
     * Получить все модели автомобилей, для которых заданы расходники TO.
     * @return список моделей без дубликатов
     */
    List<String> getAllCarModels();
    
    /**
     * Добавить расходник TO.
     * @param part расходник для добавления; после вызова получает сгенерированный ID
     */
    void addToPart(com.autoservice.model.ToPart part);
    
    /**
     * Обновить расходник TO.
     * @param part расходник с обновлёнными данными
     */
    void updateToPart(com.autoservice.model.ToPart part);
    
    /**
     * Удалить расходник TO.
     * @param part удаляемый расходник
     */
    void deleteToPart(com.autoservice.model.ToPart part);
    
    /**
     * Удалить расходники TO для модели автомобиля.
     * @param carModel модель автомобиля; {@code null} или пустая строка — удалить все расходники
     */
    void deleteToPartsByCarModel(String carModel);
    
    // ==================== SETTINGS ====================
    
    /**
     * Получить все настройки приложения.
     * @return список настроек
     */
    List<com.autoservice.model.Setting> getAllSettings();
    
    /**
     * Добавить настройку.
     * @param setting настройка для добавления; после вызова получает сгенерированный ID
     */
    void addSetting(com.autoservice.model.Setting setting);
    
    /**
     * Обновить значение настройки.
     * @param setting настройка с новым значением
     */
    void updateSetting(com.autoservice.model.Setting setting);
    
    /**
     * Удалить настройку.
     * @param setting удаляемая настройка
     */
    void deleteSetting(com.autoservice.model.Setting setting);
    
    /**
     * Получить настройку по ключу.
     * @param key ключ настройки
     * @return настройка или {@code null}, если не найдена
     */
    com.autoservice.model.Setting getSettingByKey(String key);
}
