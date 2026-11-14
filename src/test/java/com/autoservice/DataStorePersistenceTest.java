package com.autoservice;

import com.autoservice.model.ServicePart;
import com.autoservice.model.ServiceSparePart;
import com.autoservice.model.Setting;
import com.autoservice.model.ToPart;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты семантики сохранения {@link DataStore#save()}.
 *
 * <p>{@code save()} не пишет в базу, пока кэш не помечен через
 * {@link DataStore#markDirty()} (или одним из CRUD-методов), и сохраняет только
 * объекты с флагом {@code dirty}. Тесты проверяют обе половины этого контракта
 * для каждого типа сущности, который умеет сохранять {@code save()}.</p>
 */
@Tag(TestTags.INTEGRATION)
class DataStorePersistenceTest extends BaseTest {

    @Test
    @DisplayName("save() без markDirty() не пишет даже изменённые объекты")
    void saveWithoutMarkDirtySkipsPersistence() {
        DataStore.addClient(new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163"));
        DataStore.load();

        Client cached = DataStore.getClients().get(0);
        cached.setPhone("+70000000000");

        DataStore.save();

        DataStore.load();
        assertThat(DataStore.getClients().get(0).getPhone())
                .as("изменение без markDirty не сохраняется").isEqualTo("+79001234567");
    }

    @Test
    @DisplayName("save() сохраняет изменённого клиента и сбрасывает его флаг")
    void savePersistsDirtyClient() {
        DataStore.addClient(new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163"));
        DataStore.load();

        Client cached = DataStore.getClients().get(0);
        cached.setPhone("+79990001122");
        DataStore.markDirty();

        DataStore.save();

        assertThat(cached.isDirty()).as("флаг сброшен после сохранения").isFalse();
        DataStore.load();
        assertThat(DataStore.getClients().get(0).getPhone()).isEqualTo("+79990001122");
    }

    @Test
    @DisplayName("save() сохраняет изменённый статус заказа")
    void savePersistsDirtyOrder() {
        DataStore.addClient(new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163"));
        DataStore.load();
        WorkOrder order = new WorkOrder(DataStore.getClients().get(0));
        order.addService("Замена масла", 1500);
        DataStore.addOrder(order);
        DataStore.load();

        WorkOrder cached = DataStore.getOrders().get(0);
        cached.setStatus(WorkOrder.STATUS_IN_PROGRESS);
        DataStore.markDirty();

        DataStore.save();

        assertThat(cached.isDirty()).isFalse();
        DataStore.load();
        assertThat(DataStore.getOrders().get(0).getStatus()).isEqualTo(WorkOrder.STATUS_IN_PROGRESS);
    }

    @Test
    @DisplayName("save() сохраняет изменённую запись на обслуживание")
    void savePersistsDirtyAppointment() {
        DataStore.addClient(new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163"));
        DataStore.load();
        DataStore.addAppointment(new Appointment(DataStore.getClients().get(0), "Пётр",
                "Замена масла", "2026-11-20", "10:00"));
        DataStore.load();

        Appointment cached = DataStore.getAppointments().get(0);
        cached.setStatus(Appointment.STATUS_COMPLETED);
        DataStore.markDirty();

        DataStore.save();

        DataStore.load();
        assertThat(DataStore.getAppointments().get(0).getStatus())
                .isEqualTo(Appointment.STATUS_COMPLETED);
    }

    @Test
    @DisplayName("save() сохраняет остаток запчасти")
    void savePersistsDirtySparePartStock() {
        DataStore.addSparePart(new SparePart("Масло", 800, 1200, 10));
        DataStore.load();

        SparePart cached = DataStore.getSpareParts().get(0);
        cached.setStock(4);
        DataStore.markDirty();

        DataStore.save();

        assertThat(cached.isDirty()).isFalse();
        DataStore.load();
        assertThat(DataStore.getSpareParts().get(0).getStock()).isEqualTo(4);
    }

    @Test
    @DisplayName("save() сохраняет значение настройки")
    void savePersistsDirtySetting() {
        DataStore.addSetting(new Setting("backup_time", "02:00", "Время бэкапа"));
        DataStore.load();

        Setting cached = DataStore.getAllSettings().stream()
                .filter(setting -> "backup_time".equals(setting.getKey()))
                .findFirst()
                .orElseThrow();
        cached.setValue("03:30");
        DataStore.markDirty();

        DataStore.save();

        assertThat(cached.isDirty()).isFalse();
        assertThat(DataStore.getSettingByKey("backup_time").getValue()).isEqualTo("03:30");
    }

    @Test
    @DisplayName("save() сохраняет количество расходника ТО (сеттер помечает объект изменённым)")
    void savePersistsDirtyToPart() {
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        DataStore.addSparePart(part);
        DataStore.load();
        SparePart stored = DataStore.getSpareParts().get(0);

        DataStore.addToPart(new ToPart("Haval Jolion", stored.getId(), 4, "л"));
        DataStore.load();

        ToPart cached = DataStore.getToPartsByCarModel("").get(0);
        // Регрессия: сеттеры ToPart должны сами помечать объект изменённым,
        // иначе save() молча пропускает правку.
        cached.setQuantity(6);
        assertThat(cached.isDirty())
                .as("setQuantity() помечает расходник изменённым").isTrue();
        DataStore.markDirty();

        DataStore.save();

        assertThat(cached.isDirty()).isFalse();
        DataStore.load();
        assertThat(DataStore.getToPartsByCarModel("").get(0).getQuantity()).isEqualTo(6);
    }

    @Test
    @DisplayName("Все сеттеры ToPart помечают объект изменённым")
    void allToPartSettersMarkDirty() {
        record Setter(String name, Consumer<ToPart> apply) {
        }
        List<Setter> setters = List.of(
                new Setter("setCarModel", p -> p.setCarModel("Kia Rio")),
                new Setter("setSparePartId", p -> p.setSparePartId(42)),
                new Setter("setQuantity", p -> p.setQuantity(7)),
                new Setter("setUnitType", p -> p.setUnitType("лл")),
                new Setter("setNote", p -> p.setNote("комментарий")),
                new Setter("setActive", p -> p.setActive(false)));

        for (Setter setter : setters) {
            ToPart toPart = new ToPart("Haval Jolion", 1, 2, "л");
            toPart.setDirty(false);

            setter.apply().accept(toPart);

            assertThat(toPart.isDirty())
                    .as("%s() помечает объект изменённым", setter.name()).isTrue();
        }
    }

    @Test
    @DisplayName("save() сохраняет количество запчасти в связи услуги")
    void savePersistsDirtyServicePart() {
        Service service = new Service("Замена масла", 1500);
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        DataStore.addService(service);
        DataStore.addSparePart(part);
        DataStore.load();
        Service storedService = DataStore.getServiceByName("Замена масла");
        SparePart storedPart = DataStore.getSpareParts().get(0);

        DataStore.addServicePart(new ServicePart(storedService.getId(), storedPart.getId(), 2, true));
        DataStore.load();
        ServicePart cached = DataStore.getAllServiceParts().get(0);
        cached.setQuantity(3);
        DataStore.markDirty();

        DataStore.save();

        assertThat(cached.isDirty()).isFalse();
        DataStore.load();
        assertThat(DataStore.getAllServiceParts().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("save() не падает на изменённой связи услуги-запчасти (старая структура)")
    void saveHandlesDirtyServiceSparePart() {
        Service service = new Service("Замена масла", 1500);
        SparePart part = new SparePart("Масло", 800, 1200, 10);
        DataStore.addService(service);
        DataStore.addSparePart(part);
        DataStore.load();
        Service storedService = DataStore.getServiceByName("Замена масла");
        SparePart storedPart = DataStore.getSpareParts().get(0);

        DataStore.addServiceSparePart(
                new ServiceSparePart(storedService.getId(), storedPart.getId(), 2, "шт"));
        DataStore.load();

        ServiceSparePart cached = DataStore.getServiceSparePartsByServiceId(-1).get(0);
        cached.setQuantity(5);
        DataStore.markDirty();

        DataStore.save();

        assertThat(cached.isDirty()).as("save() отмечает связь сохранённой").isFalse();
    }

    @Test
    @DisplayName("save() после сохранения не переписывает данные повторно")
    void saveIsIdempotentWhenNothingChanged() {
        DataStore.addClient(new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163"));
        DataStore.load();
        Client cached = DataStore.getClients().get(0);
        cached.setPhone("+79990001122");
        DataStore.markDirty();
        DataStore.save();

        cached.setPhone("+79995554433");
        DataStore.save();

        assertThat(cached.isDirty()).as("объект остался несохранённым").isTrue();
        DataStore.load();
        assertThat(DataStore.getClients().get(0).getPhone()).isEqualTo("+79990001122");
    }

    @Test
    @DisplayName("loadClients() возвращает свежий список, не меняя кэш")
    void loadClientsDoesNotTouchCache() {
        DataStore.addClient(new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163"));
        DataStore.load();

        // Второй клиент пишется в базу мимо DataStore — кэш о нём не знает.
        DatabaseFactory.getDatabase().addClient(
                new Client("Анна", "Смирнова", "+79002223333", "Kia Rio", "В456СЕ163"));

        List<Client> fresh = DataStore.loadClients();

        assertThat(fresh).as("в базе два клиента").hasSize(2);
        assertThat(DataStore.getClients()).as("кэш остался прежним").hasSize(1);
        assertThat(DataStore.getClients().get(0).getName()).isEqualTo("Иван");
    }
}
