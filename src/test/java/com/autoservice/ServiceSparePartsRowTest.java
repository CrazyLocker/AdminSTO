package com.autoservice;

import com.autoservice.model.ServiceSparePart;
import com.autoservice.model.ServiceSparePartsList;
import com.autoservice.model.ServiceSparePartsListItem;
import com.autoservice.views.ServiceSparePartsRow;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для ServiceSparePartsRow и новой структуры отображения связей услуг-запчастей.
 * Проверяет SQLite совместимость и корректность отображения.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceSparePartsRowTest extends BaseTest {

    @BeforeAll
    static void setUpClass() {
        DatabaseFactory.initForTest();
    }

    @BeforeEach
    void setUp() {
        try (Connection conn = DatabaseFactory.getDatabase().getConnection()) {
            var stmt = conn.createStatement();
            stmt.execute("DELETE FROM service_spare_parts_list_items");
            stmt.execute("DELETE FROM service_spare_parts_lists");
            stmt.execute("DELETE FROM service_spare_parts");
            stmt.execute("DELETE FROM spare_parts");
            stmt.execute("DELETE FROM services");
            stmt.close();
            DataStore.load(); // Сброс кэша
        } catch (Exception e) {
            fail("Ошибка очистки БД: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    void testAddServiceSparePartsListWithSQLiteCompatibility() {
        // Создаем услугу
        Service service = new Service();
        service.setName("Тестовая услуга");
        service.setPrice(1000);
        Database.addService(service);
        assertNotNull(service.getId(), "ID услуги должен быть сгенерирован");

        // Создаем список связей
        ServiceSparePartsList list = new ServiceSparePartsList();
        list.setServiceId(service.getId());
        list.setCreatedDate(java.time.LocalDate.now().toString());
        list.setActive(true);

        // Создаем 3 запчасти
        SparePart part1 = createSparePart("Запчасть 1");
        SparePart part2 = createSparePart("Запчасть 2");
        SparePart part3 = createSparePart("Запчасть 3");

        // Добавляем 3 элемента в список
        ServiceSparePartsListItem item1 = new ServiceSparePartsListItem();
        item1.setSparePartId(part1.getId());
        item1.setQuantity(2);
        item1.setUnitType("шт");
        list.addItem(item1);

        ServiceSparePartsListItem item2 = new ServiceSparePartsListItem();
        item2.setSparePartId(part2.getId());
        item2.setQuantity(1);
        item2.setUnitType("шт");
        list.addItem(item2);

        ServiceSparePartsListItem item3 = new ServiceSparePartsListItem();
        item3.setSparePartId(part3.getId());
        item3.setQuantity(500);
        item3.setUnitType("л");
        list.addItem(item3);

        // Сохраняем список - это вызовет last_insert_rowid() для SQLite
        Database.addServiceSparePartsList(list);

        // Проверяем, что список был сохранен с ID (SQLite совместимость)
        assertNotNull(list.getId(), "ID списка должен быть сгенерирован через last_insert_rowid()");
        assertEquals(3, list.getItems().size(), "Должно быть 3 элемента");

        // Проверяем, что каждый элемент получил ID
        for (ServiceSparePartsListItem item : list.getItems()) {
            assertNotNull(item.getId(), "ID элемента должен быть сгенерирован");
        }

        // Проверяем, что данные сохранены в БД
        List<ServiceSparePartsList> loadedLists = Database.getServiceSparePartsListsByServiceId(service.getId());
        assertEquals(1, loadedLists.size(), "Должен быть сохранен 1 список");
        assertEquals(service.getId(), loadedLists.get(0).getServiceId(), "ID услуги должен совпадать");

        // Проверяем элементы
        List<ServiceSparePartsListItem> loadedItems = Database.getServiceSparePartsListItems(list.getId());
        assertEquals(3, loadedItems.size(), "Должно быть 3 элемента");

        // Проверяем содержимое элементов
        assertTrue(loadedItems.stream().anyMatch(i -> 
            i.getSparePartId() == part1.getId() && i.getQuantity() == 2 && "шт".equals(i.getUnitType())),
            "Не найден элемент с запчастью 1");
        assertTrue(loadedItems.stream().anyMatch(i -> 
            i.getSparePartId() == part2.getId() && i.getQuantity() == 1 && "шт".equals(i.getUnitType())),
            "Не найден элемент с запчастью 2");
        assertTrue(loadedItems.stream().anyMatch(i -> 
            i.getSparePartId() == part3.getId() && i.getQuantity() == 500 && "л".equals(i.getUnitType())),
            "Не найден элемент с запчастью 3");
    }

    @Test
    @Order(2)
    void testServiceSparePartsRowModel() {
        // Создаем модель строки
        Service service = new Service();
        service.setId(1);
        service.setName("Замена масла");

        ServiceSparePartsRow row = new ServiceSparePartsRow(
            service,
            "Масло Motul, Фильтр Mann",
            "2 шт, 1 шт"
        );

        // Проверяем поля
        assertEquals(service, row.getService(), "Должна быть установлена услуга");
        assertEquals("Масло Motul, Фильтр Mann", row.getSparePartsList(), "Должен быть список запчастей");
        assertEquals("2 шт, 1 шт", row.getTotalQuantity(), "Должна быть сумма количеств");

        // Проверяем toString
        String toString = row.toString();
        assertTrue(toString.contains("Замена масла"), "toString должен содержать имя услуги");
        assertTrue(toString.contains("Масло Motul, Фильтр Mann"), "toString должен содержать запчасти");
    }

    @Test
    @Order(3)
    void testServiceSparePartsRowWithLongSparePartsList() {
        // Создаем строку с длинным списком запчастей (для теста усечения)
        Service service = new Service();
        service.setId(2);
        service.setName("Ремонт двигателя");

        // Длинный список запчастей
        String longList = "Масло 1, Масло 2, Масло 3, Масло 4, Масло 5, " +
                         "Масло 6, Масло 7, Масло 8, Масло 9, Масло 10, " +
                         "Масло 11, Масло 12, Масло 13, Масло 14, Масло 15";

        ServiceSparePartsRow row = new ServiceSparePartsRow(service, longList, "15 шт");

        assertEquals(longList, row.getSparePartsList(), "Должен быть сохранен полный список");
        assertEquals(15, row.getSparePartsList().split(", ").length, "Должно быть 15 запчастей");
    }

    @Test
    @Order(4)
    void testMultipleServicesWithDifferentSpareParts() {
        // Создаем две услуги
        Service service1 = new Service();
        service1.setName("Услуга 1");
        service1.setPrice(1000);
        Database.addService(service1);

        Service service2 = new Service();
        service2.setName("Услуга 2");
        service2.setPrice(2000);
        Database.addService(service2);

        // Создаем списки для каждой услуги
        ServiceSparePartsList list1 = createServiceSparePartsList(service1.getId(), "Запчасть 1", "Запчасть 2");
        ServiceSparePartsList list2 = createServiceSparePartsList(service2.getId(), "Запчасть 3");

        // Проверяем, что оба списка сохранены
        List<ServiceSparePartsList> lists1 = Database.getServiceSparePartsListsByServiceId(service1.getId());
        List<ServiceSparePartsList> lists2 = Database.getServiceSparePartsListsByServiceId(service2.getId());

        assertEquals(1, lists1.size(), "Услуга 1 должна иметь 1 список");
        assertEquals(1, lists2.size(), "Услуга 2 должна иметь 1 список");
    }

    @Test
    @Order(5)
    void testLoadServiceSparePartsRowsWithGrouping() {
        // Создаем услугу
        Service service = new Service();
        service.setName("Совмещенная услуга");
        service.setPrice(3000);
        Database.addService(service);

        // Создаем две запчасти
        SparePart part1 = createSparePart("Запчасть А");
        SparePart part2 = createSparePart("Запчасть Б");

        // Создаем две связи для одной услуги (должны объединиться в одну строку)
        ServiceSparePart relation1 = new ServiceSparePart();
        relation1.setServiceId(service.getId());
        relation1.setSparePartId(part1.getId());
        relation1.setQuantity(2);
        relation1.setUnitType("шт");
        Database.addServiceSparePart(relation1);

        ServiceSparePart relation2 = new ServiceSparePart();
        relation2.setServiceId(service.getId());
        relation2.setSparePartId(part2.getId());
        relation2.setQuantity(1);
        relation2.setUnitType("шт");
        Database.addServiceSparePart(relation2);

        // Загружаем данные
        DataStore.load();
        
        // Проверяем, что связи загружены
        List<ServiceSparePart> relations = DataStore.getServiceSparePartsByServiceId(-1);
        assertEquals(2, relations.size(), "Должно быть 2 связи");

        // Проверяем, что данные в DataStore корректны
        List<Service> services = DataStore.getServices();
        List<SparePart> parts = DataStore.getSpareParts();
        
        assertEquals(1, services.size(), "Должна быть 1 услуга");
        assertEquals(2, parts.size(), "Должно быть 2 запчасти");
    }

    /**
     * Вспомогательный метод для создания запчасти.
     */
    private SparePart createSparePart(String name) {
        SparePart part = new SparePart();
        part.setName(name);
        part.setRetailPrice(100);
        part.setStock(10);
        part.setUnitType("шт");
        Database.addSparePart(part);
        return part;
    }

    /**
     * Вспомогательный метод для создания ServiceSparePartsList.
     */
    private ServiceSparePartsList createServiceSparePartsList(int serviceId, String... partNames) {
        ServiceSparePartsList list = new ServiceSparePartsList();
        list.setServiceId(serviceId);
        list.setCreatedDate(java.time.LocalDate.now().toString());
        list.setActive(true);

        for (String partName : partNames) {
            SparePart part = createSparePart(partName);
            ServiceSparePartsListItem item = new ServiceSparePartsListItem();
            item.setSparePartId(part.getId());
            item.setQuantity(1);
            item.setUnitType("шт");
            list.addItem(item);
        }

        Database.addServiceSparePartsList(list);
        return list;
    }
}
