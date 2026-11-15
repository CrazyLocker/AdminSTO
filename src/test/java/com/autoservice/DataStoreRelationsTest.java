package com.autoservice;

import com.autoservice.model.ServicePart;
import com.autoservice.model.ServiceSparePart;
import com.autoservice.model.ServiceSparePartsList;
import com.autoservice.model.ServiceSparePartsListItem;
import com.autoservice.model.Setting;
import com.autoservice.model.ToPart;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты отношений "услуга — запчасть", автомобилей, расходников ТО, настроек
 * и точечных поисков в кэше {@link DataStore}.
 *
 * <p>Дополняет существующие CRUD-тесты {@code DataStoreTest}: проверяет ветки,
 * которые не покрывались (связи всех трёх структур, каскадное удаление клиента,
 * поиск по идентификатору и по номеру заказа).</p>
 */
@Tag(TestTags.INTEGRATION)
class DataStoreRelationsTest extends BaseTest {

    private Service createService(String name, double price) {
        Service service = new Service(name, price);
        DataStore.addService(service);
        DataStore.load();
        return DataStore.getServiceByName(name);
    }

    private SparePart createSparePart(String name, int stock) {
        DataStore.addSparePart(new SparePart(name, 500, 900, stock));
        DataStore.load();
        return DataStore.getSpareParts().stream()
                .filter(part -> name.equals(part.getName()))
                .findFirst()
                .orElseThrow();
    }

    private Client createClient(String name, String phone) {
        DataStore.addClient(new Client(name, "Тестов", phone, "Haval Jolion", "А123ВС163"));
        DataStore.load();
        return DataStore.getClients().stream()
                .filter(client -> name.equals(client.getName()))
                .findFirst()
                .orElseThrow();
    }

    // ==================== SERVICE SPARE PARTS (старая структура) ====================

    @Test
    @DisplayName("addServiceSparePart() кладёт связь в кэш")
    void addServiceSparePartAddsRelationToCache() {
        Service service = createService("Замена масла", 1500);
        SparePart part = createSparePart("Масло", 10);

        DataStore.addServiceSparePart(new ServiceSparePart(service.getId(), part.getId(), 2, "шт"));

        assertThat(DataStore.getServiceSparePartsByServiceId(-1)).hasSize(1);
        assertThat(DataStore.getServiceSparePartsByServiceId(service.getId())).hasSize(1);
    }

    @Test
    @DisplayName("addServiceSparePart() для существующей связи обновляет количество, не добавляя запись в кэш")
    void addServiceSparePartUpdatesExistingRelation() {
        Service service = createService("Замена масла", 1500);
        SparePart part = createSparePart("Масло", 10);
        DataStore.addServiceSparePart(new ServiceSparePart(service.getId(), part.getId(), 2, "шт"));

        ServiceSparePart duplicate = new ServiceSparePart(service.getId(), part.getId(), 5, "л");
        DataStore.addServiceSparePart(duplicate);

        List<ServiceSparePart> cached = DataStore.getServiceSparePartsByServiceId(-1);
        assertThat(cached).as("дубликат не попадает в кэш").hasSize(1);
        assertThat(cached.get(0).getQuantity()).isEqualTo(5);
        assertThat(cached.get(0).getUnitType()).isEqualTo("л");
    }

    @Test
    @DisplayName("deleteServiceSparePart() и deleteServiceSparePartsByServiceId() чистят кэш")
    void deleteServiceSparePartRemovesRelation() {
        Service service = createService("Замена масла", 1500);
        SparePart first = createSparePart("Масло", 10);
        SparePart second = createSparePart("Фильтр", 4);
        DataStore.addServiceSparePart(new ServiceSparePart(service.getId(), first.getId(), 2, "шт"));
        DataStore.addServiceSparePart(new ServiceSparePart(service.getId(), second.getId(), 1, "шт"));
        DataStore.load();
        assertThat(DataStore.getServiceSparePartsByServiceId(-1)).hasSize(2);

        ServiceSparePart cached = DataStore.getServiceSparePartsByServiceId(-1).get(0);
        DataStore.deleteServiceSparePart(cached);
        assertThat(DataStore.getServiceSparePartsByServiceId(-1)).hasSize(1);

        DataStore.deleteServiceSparePartsByServiceId(service.getId());
        assertThat(DataStore.getServiceSparePartsByServiceId(-1)).isEmpty();
    }

    @Test
    @DisplayName("Повторный addServiceSparePart() обновляет связь (UPDATE по id), дубликата в БД не появляется")
    void repeatedAddServiceSparePartUpdatesRowInDatabase() {
        Service service = createService("Замена масла", 1500);
        SparePart part = createSparePart("Масло", 10);
        DataStore.addServiceSparePart(new ServiceSparePart(service.getId(), part.getId(), 2, "шт"));
        int relationId = DataStore.getServiceSparePartsByServiceId(-1).get(0).getId();

        DataStore.addServiceSparePart(new ServiceSparePart(service.getId(), part.getId(), 7, "л"));

        List<ServiceSparePart> cached = DataStore.getServiceSparePartsByServiceId(-1);
        assertThat(cached).as("в кэше по-прежнему одна связь").hasSize(1);
        assertThat(cached.get(0).getId()).as("обновлена та же запись").isEqualTo(relationId);
        assertThat(cached.get(0).getQuantity()).isEqualTo(7);

        DataStore.load();   // перечитываем таблицу из БД
        List<ServiceSparePart> stored = DataStore.getServiceSparePartsByServiceId(-1);
        assertThat(stored).as("в базе нет дубликата").hasSize(1);
        assertThat(stored.get(0).getId()).as("id записи не изменился").isEqualTo(relationId);
        assertThat(stored.get(0).getQuantity()).isEqualTo(7);
        assertThat(stored.get(0).getUnitType()).isEqualTo("л");
    }

    @Test
    @DisplayName("addServiceSparePart() обновляет связь, которая уже есть в БД, но отсутствует в кэше")
    void addServiceSparePartUpdatesRelationMissingFromCache() {
        Service service = createService("Замена масла", 1500);
        SparePart part = createSparePart("Масло", 10);
        // Связь вставлена мимо DataStore — кэш о ней не знает.
        DatabaseFactory.getDatabase().addServiceSparePart(
                new ServiceSparePart(service.getId(), part.getId(), 3, "шт"));
        assertThat(DataStore.getServiceSparePartsByServiceId(-1)).isEmpty();

        DataStore.addServiceSparePart(new ServiceSparePart(service.getId(), part.getId(), 9, "л"));

        DataStore.load();
        List<ServiceSparePart> stored = DataStore.getServiceSparePartsByServiceId(-1);
        assertThat(stored).as("дубликат не создан").hasSize(1);
        assertThat(stored.get(0).getQuantity()).isEqualTo(9);
        assertThat(stored.get(0).getUnitType()).isEqualTo("л");
    }

    // ==================== SERVICE PARTS (новая структура) ====================

    @Test
    @DisplayName("Связь «услуга — запчасть»: добавление, поиск, обновление, удаление")
    void servicePartLifecycle() {
        Service service = createService("Замена масла", 1500);
        SparePart part = createSparePart("Масло", 10);

        DataStore.addServicePart(new ServicePart(service.getId(), part.getId(), 2, true));
        DataStore.load();

        List<ServicePart> relations = DataStore.getAllServiceParts();
        assertThat(relations).hasSize(1);
        ServicePart relation = relations.get(0);
        assertThat(DataStore.getServicePartsByServiceId(service.getId())).hasSize(1);
        assertThat(DataStore.getServicePartById(relation.getId())).isSameAs(relation);
        assertThat(DataStore.getServicePartById(-42)).isNull();

        relation.setQuantity(4);
        DataStore.updateServicePart(relation);
        DataStore.load();
        assertThat(DataStore.getAllServiceParts().get(0).getQuantity()).isEqualTo(4);

        DataStore.deleteServicePart(DataStore.getAllServiceParts().get(0));
        assertThat(DataStore.getAllServiceParts()).isEmpty();
    }

    @Test
    @DisplayName("deleteServicePartsByServiceId() и deleteServicePartsBySparePartId() удаляют связи")
    void deleteServicePartsByServiceIdAndSparePartId() {
        Service firstService = createService("Первая", 100);
        Service secondService = createService("Вторая", 200);
        SparePart part = createSparePart("Масло", 10);
        DataStore.addServicePart(new ServicePart(firstService.getId(), part.getId(), 1, true));
        DataStore.addServicePart(new ServicePart(secondService.getId(), part.getId(), 1, true));
        assertThat(DataStore.getAllServiceParts()).hasSize(2);

        DataStore.deleteServicePartsByServiceId(firstService.getId());
        assertThat(DataStore.getAllServiceParts()).hasSize(1);

        DataStore.deleteServicePartsBySparePartId(part.getId());
        assertThat(DataStore.getAllServiceParts()).isEmpty();
    }

    @Test
    @DisplayName("updateSparePartStock(serviceName, qty) списывает остаток первой запчасти услуги")
    void updateSparePartStockByServiceName() {
        Service service = createService("Замена масла", 1500);
        SparePart part = createSparePart("Масло", 10);
        DataStore.addServicePart(new ServicePart(service.getId(), part.getId(), 1, true));
        DataStore.load();

        DataStore.updateSparePartStock("Замена масла", 3);

        DataStore.load();
        assertThat(DataStore.getSpareParts().get(0).getStock()).isEqualTo(7);
    }

    @Test
    @DisplayName("updateSparePartStock(serviceName, qty) для услуги без запчастей ничего не делает")
    void updateSparePartStockByUnknownServiceIsNoOp() {
        SparePart part = createSparePart("Масло", 10);

        DataStore.updateSparePartStock("Неизвестная услуга", 3);

        DataStore.load();
        assertThat(DataStore.getSpareParts().get(0).getStock()).isEqualTo(10);
        assertThat(DataStore.getSpareParts().get(0).getId()).isEqualTo(part.getId());
    }

    // ==================== СПИСКИ ЗАПЧАСТЕЙ УСЛУГИ ====================

    @Test
    @DisplayName("Список запчастей услуги: добавление с элементами, удаление")
    void serviceSparePartsListLifecycle() {
        Service service = createService("Замена масла", 1500);
        SparePart part = createSparePart("Масло", 10);

        ServiceSparePartsList list = new ServiceSparePartsList();
        list.setServiceId(service.getId());
        list.setCreatedDate(LocalDate.now().toString());
        list.setActive(true);
        ServiceSparePartsListItem item = new ServiceSparePartsListItem();
        item.setSparePartId(part.getId());
        item.setQuantity(2);
        item.setUnitType("шт");
        list.addItem(item);

        DataStore.addServiceSparePartsList(list);

        assertThat(DataStore.getServiceSparePartsListsByServiceId(-1)).hasSize(1);
        assertThat(list.getItems()).as("элементы перезагружены из базы").hasSize(1);
        assertThat(DataStore.getServiceSparePartsListItems(list.getId())).hasSize(1);

        DataStore.addServiceSparePartsListItem(list.getItems().get(0));
        assertThat(DataStore.getServiceSparePartsListItems(list.getId())).hasSize(2);

        DataStore.deleteServiceSparePartsList(list);
        assertThat(DataStore.getServiceSparePartsListsByServiceId(-1)).isEmpty();
        assertThat(DataStore.getServiceSparePartsListsByServiceId(service.getId())).isEmpty();

        DataStore.addServiceSparePartsList(list);
        DataStore.deleteServiceSparePartsListsByServiceId(service.getId());
        assertThat(DataStore.getServiceSparePartsListsByServiceId(-1)).isEmpty();
    }

    // ==================== УДАЛЕНИЕ ИЗ КЭША ПО ID (регрессия) ====================

    @Test
    @DisplayName("removeClient() со «старым» объектом чистит кэш по id")
    void removeClientWithStaleInstanceRemovesCachedCopy() {
        createClient("Иван", "+79001234567");
        Client stale = DataStore.getClients().get(0);
        DataStore.load();

        DataStore.removeClient(stale);

        assertThat(DataStore.getClients())
                .as("удаление по id чистит кэш, несмотря на другой экземпляр").isEmpty();
    }

    @Test
    @DisplayName("deleteServiceSparePart() со «старым» объектом чистит кэш по id")
    void deleteServiceSparePartWithStaleInstanceRemovesCachedCopy() {
        Service service = createService("Замена масла", 1500);
        SparePart part = createSparePart("Масло", 10);
        DataStore.addServiceSparePart(new ServiceSparePart(service.getId(), part.getId(), 2, "шт"));
        ServiceSparePart stale = DataStore.getServiceSparePartsByServiceId(-1).get(0);
        DataStore.load();

        DataStore.deleteServiceSparePart(stale);

        assertThat(DataStore.getServiceSparePartsByServiceId(-1))
                .as("связь удалена из кэша по id").isEmpty();
        DataStore.load();
        assertThat(DataStore.getServiceSparePartsByServiceId(-1))
                .as("связь удалена из базы").isEmpty();
    }

    @Test
    @DisplayName("deleteServicePart() со «старым» объектом чистит кэш по id")
    void deleteServicePartWithStaleInstanceRemovesCachedCopy() {
        Service service = createService("Замена масла", 1500);
        SparePart part = createSparePart("Масло", 10);
        DataStore.addServicePart(new ServicePart(service.getId(), part.getId(), 2, true));
        ServicePart stale = DataStore.getServicePartsByServiceId(service.getId()).get(0);
        DataStore.load();

        DataStore.deleteServicePart(stale);

        assertThat(DataStore.getServicePartsByServiceId(service.getId()))
                .as("связь удалена из кэша по id").isEmpty();
    }

    @Test
    @DisplayName("deleteServiceSparePartsList() со «старым» объектом чистит кэш по id")
    void deleteServiceSparePartsListWithStaleInstanceRemovesCachedCopy() {
        Service service = createService("Замена масла", 1500);
        ServiceSparePartsList list = new ServiceSparePartsList();
        list.setServiceId(service.getId());
        list.setCreatedDate(LocalDate.now().toString());
        list.setActive(true);
        DataStore.addServiceSparePartsList(list);
        ServiceSparePartsList stale = DataStore.getServiceSparePartsListsByServiceId(-1).get(0);
        DataStore.load();

        DataStore.deleteServiceSparePartsList(stale);

        assertThat(DataStore.getServiceSparePartsListsByServiceId(-1))
                .as("список удалён из кэша по id").isEmpty();
    }

    @Test
    @DisplayName("deleteSetting() со «старым» объектом чистит кэш по id")
    void deleteSettingWithStaleInstanceRemovesCachedCopy() {
        DataStore.addSetting(new Setting("interface_language", "ru", "Язык"));
        Setting stale = DataStore.getAllSettings().get(0);
        DataStore.load();

        DataStore.deleteSetting(stale);

        assertThat(DataStore.getAllSettings())
                .as("настройка удалена из кэша по id").isEmpty();
        DataStore.load();
        assertThat(DataStore.getAllSettings())
                .as("настройка удалена из базы").isEmpty();
    }

    @Test
    @DisplayName("deleteCar() со «старым» объектом чистит кэш клиента по id")
    void deleteCarWithStaleInstanceRemovesCachedCopy() throws SQLException {
        Client client = createClient("Иван", "+79001234567");
        DataStore.addCar(new Car(client.getId(), "Haval Jolion", "А123ВС163", 15000));
        Car stale = DataStore.getClientById(client.getId()).getCars().get(0);
        DataStore.load();

        DataStore.deleteCar(stale);

        assertThat(DataStore.getClientById(client.getId()).getCars())
                .as("автомобиль удалён из кэша клиента по id").isEmpty();
    }

    // ==================== АВТОМОБИЛИ ====================

    @Test
    @DisplayName("Автомобиль: добавление в кэш клиента, обновление, удаление")
    void carLifecycle() throws SQLException {
        Client client = createClient("Иван", "+79001234567");
        Car car = new Car(client.getId(), "Haval Jolion", "А123ВС163", 15000);

        DataStore.addCar(car);

        Client cached = DataStore.getClientById(client.getId());
        assertThat(cached.getCars()).extracting(Car::getCarModel).contains("Haval Jolion");

        car.setMileage(20000);
        DataStore.updateCar(car);
        assertThat(DataStore.getClientById(client.getId()).getCars().get(0).getMileage())
                .isEqualTo(20000);

        DataStore.deleteCar(car);
        assertThat(DataStore.getClientById(client.getId()).getCars()).isEmpty();
    }

    @Test
    @DisplayName("getClientById() возвращает null для неизвестного идентификатора")
    void getClientByIdReturnsNullForUnknownId() {
        createClient("Иван", "+79001234567");

        assertThat(DataStore.getClientById(-1)).isNull();
    }

    // ==================== ЗАПИСИ НА ОБСЛУЖИВАНИЕ ====================

    @Test
    @DisplayName("Поиск записи по id и по номеру заказа, удаление записи")
    void appointmentLookupsAndDelete() {
        Client client = createClient("Иван", "+79001234567");
        Appointment appointment = new Appointment(client, "Пётр", "Замена масла", "2026-11-20", "10:00");
        DataStore.addAppointment(appointment);
        DataStore.load();

        Appointment cached = DataStore.getAppointments().get(0);
        assertThat(DataStore.getAppointmentById(cached.getId())).isSameAs(cached);
        assertThat(DataStore.getAppointmentById(-1)).isNull();

        DataStore.addOrder(new WorkOrder(client));
        DataStore.load();
        String orderId = DataStore.getOrders().get(0).getId();
        cached.setOrderId(orderId);
        DataStore.updateAppointment(cached);
        assertThat(DataStore.getAppointmentByOrderId(orderId)).isNotNull();
        assertThat(DataStore.getAppointmentByOrderId(null)).isNull();
        assertThat(DataStore.getAppointmentByOrderId("")).isNull();
        assertThat(DataStore.getAppointmentByOrderId("НЕТ-ТАКОГО")).isNull();

        DataStore.deleteAppointment(cached.getId());
        assertThat(DataStore.getAppointments()).isEmpty();
        assertThat(DataStore.getAppointmentById(cached.getId())).isNull();
    }

    // ==================== РАСХОДНИКИ ТО ====================

    @Test
    @DisplayName("Расходник ТО: добавление, выборка по модели, обновление, удаление")
    void toPartLifecycle() {
        SparePart part = createSparePart("Масло", 10);

        DataStore.addToPart(new ToPart("Haval Jolion", part.getId(), 4, "л"));

        assertThat(DataStore.getToPartsByCarModel("Haval Jolion")).hasSize(1);
        assertThat(DataStore.getToPartsByCarModel("")).as("пустая модель — весь кэш").hasSize(1);
        assertThat(DataStore.getToPartsByCarModel(null)).as("null — весь кэш").hasSize(1);
        assertThat(DataStore.getAllCarModels()).containsExactly("Haval Jolion");

        ToPart cached = DataStore.getToPartsByCarModel("").get(0);
        cached.setQuantity(6);
        DataStore.updateToPart(cached);
        assertThat(DataStore.getToPartsByCarModel("").get(0).getQuantity()).isEqualTo(6);

        // updateToPart() перезагружает кэш, поэтому удалять нужно актуальный объект.
        DataStore.deleteToPart(DataStore.getToPartsByCarModel("").get(0));
        assertThat(DataStore.getToPartsByCarModel("")).isEmpty();

        DataStore.addToPart(new ToPart("Kia Rio", part.getId(), 3, "л"));
        DataStore.deleteToPartsByCarModel("Kia Rio");
        assertThat(DataStore.getToPartsByCarModel("")).isEmpty();
    }

    @Test
    @DisplayName("deleteToPart() со «старым» объектом чистит кэш по id")
    void deleteToPartWithStaleInstanceRemovesCachedCopy() {
        SparePart part = createSparePart("Масло", 10);
        DataStore.addToPart(new ToPart("Haval Jolion", part.getId(), 4, "л"));
        ToPart stale = DataStore.getToPartsByCarModel("").get(0);
        int partId = stale.getId();

        stale.setQuantity(5);
        DataStore.updateToPart(stale);   // кэш заменён новыми объектами
        DataStore.deleteToPart(stale);   // stale уже не в кэше, но id тот же

        assertThat(DataStore.getToPartsByCarModel(""))
                .as("удаление по id очищает кэш, несмотря на устаревший экземпляр").isEmpty();
        DataStore.load();
        assertThat(DataStore.getToPartsByCarModel(""))
                .as("запись удалена и из БД").isEmpty();
    }

    // ==================== НАСТРОЙКИ ====================

    @Test
    @DisplayName("Настройки: добавление, обновление, поиск по ключу, удаление")
    void settingLifecycle() {
        DataStore.addSetting(new Setting("interface_language", "ru", "Язык"));
        assertThat(DataStore.getSettingByKey("interface_language").getValue()).isEqualTo("ru");

        Setting cached = DataStore.getAllSettings().get(0);
        cached.setValue("en");
        DataStore.updateSetting(cached);

        assertThat(DataStore.getAllSettings()).hasSize(1);
        assertThat(DataStore.getAllSettings().get(0).getValue()).isEqualTo("en");
        assertThat(DataStore.getSettingByKey("interface_language").getValue()).isEqualTo("en");

        DataStore.deleteSetting(cached);
        assertThat(DataStore.getAllSettings()).isEmpty();
        assertThat(DataStore.getSettingByKey("interface_language")).isNull();
    }

    @Test
    @DisplayName("updateSetting() маскирует чувствительные ключи в логе (smoke)")
    void updateSettingWithSensitiveKeyWorks() {
        DataStore.addSetting(new Setting("api_token", "secret", "Токен"));
        Setting cached = DataStore.getAllSettings().get(0);
        cached.setValue("rotated");
        DataStore.updateSetting(cached);
        DataStore.addSetting(new Setting("db_password", "secret", "Пароль"));
        Setting password = DataStore.getSettingByKey("db_password");
        password.setValue("rotated");
        DataStore.updateSetting(password);

        assertThat(DataStore.getSettingByKey("api_token").getValue()).isEqualTo("rotated");
        assertThat(DataStore.getSettingByKey("db_password").getValue()).isEqualTo("rotated");
    }

    // ==================== КАСКАДНОЕ УДАЛЕНИЕ ====================

    @Test
    @DisplayName("deleteClient() удаляет клиента вместе с его заказами")
    void deleteClientRemovesRelatedOrders() {
        Client client = createClient("Иван", "+79001234567");
        DataStore.addOrder(new WorkOrder(client));
        DataStore.addOrder(new WorkOrder(client));
        DataStore.load();
        assertThat(DataStore.getOrders()).hasSize(2);

        // Заказы ищутся по id, поэтому подойдёт любой инстанс; клиент из кэша —
        // remove() в кэше сравнивает по ссылке.
        DataStore.deleteClient(DataStore.getClientById(client.getId()));

        assertThat(DataStore.getClients()).isEmpty();
        assertThat(DataStore.getOrders()).as("заказы клиента удалены из кэша").isEmpty();
        DataStore.load();
        assertThat(DataStore.getOrders()).isEmpty();
        assertThat(DataStore.getClients()).isEmpty();
    }

    @Test
    @DisplayName("deleteClient() с «устаревшим» инстансом клиента чистит кэш по id")
    void deleteClientWithStaleInstanceRemovesCachedCopy() {
        Client stale = createClient("Иван", "+79001234567");
        DataStore.addOrder(new WorkOrder(stale));
        DataStore.load();

        DataStore.deleteClient(stale);

        assertThat(DataStore.getClients())
                .as("удаление по id очищает кэш, несмотря на другой экземпляр").isEmpty();
        assertThat(DataStore.getOrders()).as("заказы удалены по id").isEmpty();
        DataStore.load();
        assertThat(DataStore.getClients()).as("в базе клиент удалён").isEmpty();
    }

    @Test
    @DisplayName("deleteClient(null) не падает и ничего не меняет")
    void deleteNullClientIsNoOp() {
        createClient("Иван", "+79001234567");

        DataStore.deleteClient(null);

        assertThat(DataStore.getClients()).hasSize(1);
    }

    @Test
    @DisplayName("removeClient() удаляет только клиента")
    void removeClientRemovesOnlyClient() {
        createClient("Иван", "+79001234567");

        DataStore.removeClient(DataStore.getClients().get(0));

        assertThat(DataStore.getClients()).isEmpty();
        DataStore.load();
        assertThat(DataStore.getClients()).isEmpty();
    }
}
