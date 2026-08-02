package com.autoservice;

import com.autoservice.model.ServiceSparePartsList;
import com.autoservice.model.ServiceSparePartsListItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для новой структуры service_spare_parts_lists.
 */
public class ServiceSparePartsListTest extends BaseTest {

    private static final Logger logger = LoggerFactory.getLogger(ServiceSparePartsListTest.class);

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
            stmt.execute("DELETE FROM spare_parts");
            stmt.execute("DELETE FROM services");
            stmt.close();
            DataStore.load(); // Сброс кэша
        } catch (Exception e) {
            logger.error("Ошибка очистки БД: {}", e.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        // No-op
    }

    @Test
    void testAddServiceSparePartsList() {
        // Создаем услугу
        Service service = new Service();
        service.setName("Тестовая услуга");
        service.setPrice(1000);
        Database.addService(service);
        assertNotNull(service.getId());

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

        // Сохраняем список
        Database.addServiceSparePartsList(list);

        // Проверяем, что список был сохранен с ID
        assertNotNull(list.getId());
        assertEquals(3, list.getItems().size());

        // Проверяем, что каждый элемент получил ID
        for (ServiceSparePartsListItem item : list.getItems()) {
            assertNotNull(item.getId());
        }

        // Проверяем, что данные сохранены в БД
        List<ServiceSparePartsList> loadedLists = Database.getServiceSparePartsListsByServiceId(service.getId());
        assertEquals(1, loadedLists.size());
        assertEquals(service.getId(), loadedLists.get(0).getServiceId());

        // Проверяем элементы
        List<ServiceSparePartsListItem> loadedItems = Database.getServiceSparePartsListItems(list.getId());
        assertEquals(3, loadedItems.size());

        // Проверяем содержимое элементов
        assertTrue(loadedItems.stream().anyMatch(i -> 
            i.getSparePartId() == part1.getId() && i.getQuantity() == 2 && "шт".equals(i.getUnitType())));
        assertTrue(loadedItems.stream().anyMatch(i -> 
            i.getSparePartId() == part2.getId() && i.getQuantity() == 1 && "шт".equals(i.getUnitType())));
        assertTrue(loadedItems.stream().anyMatch(i -> 
            i.getSparePartId() == part3.getId() && i.getQuantity() == 500 && "л".equals(i.getUnitType())));
    }

    @Test
    void testGetServiceSparePartsListsByServiceId() {
        // Создаем услугу
        Service service = new Service();
        service.setName("Тестовая услуга 2");
        service.setPrice(2000);
        Database.addService(service);
        assertNotNull(service.getId());

        // Создаем первый список
        ServiceSparePartsList list1 = new ServiceSparePartsList();
        list1.setServiceId(service.getId());
        list1.setCreatedDate(java.time.LocalDate.now().toString());
        list1.setActive(true);

        SparePart part1 = createSparePart("Запчасть для списка 1");
        ServiceSparePartsListItem item1 = new ServiceSparePartsListItem();
        item1.setSparePartId(part1.getId());
        item1.setQuantity(1);
        item1.setUnitType("шт");
        list1.addItem(item1);

        Database.addServiceSparePartsList(list1);

        // Создаем второй список для той же услуги
        ServiceSparePartsList list2 = new ServiceSparePartsList();
        list2.setServiceId(service.getId());
        list2.setCreatedDate(java.time.LocalDate.now().toString());
        list2.setActive(true);

        SparePart part2 = createSparePart("Запчасть для списка 2");
        ServiceSparePartsListItem item2 = new ServiceSparePartsListItem();
        item2.setSparePartId(part2.getId());
        item2.setQuantity(2);
        item2.setUnitType("шт");
        list2.addItem(item2);

        Database.addServiceSparePartsList(list2);

        // Получаем все списки для услуги
        List<ServiceSparePartsList> lists = Database.getServiceSparePartsListsByServiceId(service.getId());
        assertEquals(2, lists.size());

        // Проверяем, что списки содержат правильные ID
        assertTrue(lists.stream().anyMatch(l -> l.getId() == list1.getId()));
        assertTrue(lists.stream().anyMatch(l -> l.getId() == list2.getId()));
    }

    @Test
    void testDeleteServiceSparePartsList() {
        // Создаем услугу
        Service service = new Service();
        service.setName("Тестовая услуга для удаления");
        service.setPrice(3000);
        Database.addService(service);
        assertNotNull(service.getId());

        // Создаем список
        ServiceSparePartsList list = new ServiceSparePartsList();
        list.setServiceId(service.getId());
        list.setCreatedDate(java.time.LocalDate.now().toString());
        list.setActive(true);

        SparePart part = createSparePart("Запчасть для удаления");
        ServiceSparePartsListItem item = new ServiceSparePartsListItem();
        item.setSparePartId(part.getId());
        item.setQuantity(1);
        item.setUnitType("шт");
        list.addItem(item);

        Database.addServiceSparePartsList(list);

        // Проверяем, что список существует
        List<ServiceSparePartsList> lists = Database.getServiceSparePartsListsByServiceId(service.getId());
        assertEquals(1, lists.size());

        // Удаляем список
        Database.deleteServiceSparePartsList(list);

        // Проверяем, что список удален
        lists = Database.getServiceSparePartsListsByServiceId(service.getId());
        assertEquals(0, lists.size());

        // Проверяем, что элементы также удалены
        List<ServiceSparePartsListItem> items = Database.getServiceSparePartsListItems(list.getId());
        assertEquals(0, items.size());
    }

    @Test
    void testDeleteServiceSparePartsListsByServiceId() {
        // Создаем услугу
        Service service = new Service();
        service.setName("Тестовая услуга для массового удаления");
        service.setPrice(4000);
        Database.addService(service);
        assertNotNull(service.getId());

        // Создаем два списка
        ServiceSparePartsList list1 = new ServiceSparePartsList();
        list1.setServiceId(service.getId());
        list1.setCreatedDate(java.time.LocalDate.now().toString());
        list1.setActive(true);

        SparePart part1 = createSparePart("Запчасть 1 для массового удаления");
        ServiceSparePartsListItem item1 = new ServiceSparePartsListItem();
        item1.setSparePartId(part1.getId());
        item1.setQuantity(1);
        item1.setUnitType("шт");
        list1.addItem(item1);

        Database.addServiceSparePartsList(list1);

        ServiceSparePartsList list2 = new ServiceSparePartsList();
        list2.setServiceId(service.getId());
        list2.setCreatedDate(java.time.LocalDate.now().toString());
        list2.setActive(true);

        SparePart part2 = createSparePart("Запчасть 2 для массового удаления");
        ServiceSparePartsListItem item2 = new ServiceSparePartsListItem();
        item2.setSparePartId(part2.getId());
        item2.setQuantity(2);
        item2.setUnitType("шт");
        list2.addItem(item2);

        Database.addServiceSparePartsList(list2);

        // Проверяем, что оба списка существуют
        List<ServiceSparePartsList> lists = Database.getServiceSparePartsListsByServiceId(service.getId());
        assertEquals(2, lists.size());

        // Удаляем все списки для услуги
        Database.deleteServiceSparePartsListsByServiceId(service.getId());

        // Проверяем, что списки удалены
        lists = Database.getServiceSparePartsListsByServiceId(service.getId());
        assertEquals(0, lists.size());
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
}
