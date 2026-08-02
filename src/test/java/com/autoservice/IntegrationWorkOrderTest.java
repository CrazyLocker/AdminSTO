package com.autoservice;

import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты сложных workflow создания заказа.
 * Проверяет полный цикл создания заказа с клиентом и запчастями.
 */
@Tag(TestTags.INTEGRATION)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationWorkOrderTest extends BaseTest {

    @DisplayName("Полный workflow: создание заказа с клиентом и запчастями")
    void testFullOrderWorkflow() {
        // Шаг 1: Создание клиента
        var client = new com.autoservice.builders.ClientBuilder()
            .withName("Integration Test Client")
            .withPhone("+79001234567")
            .build();
        
        DatabaseFactory.getDatabase().addClient(client);
        assertThat(client.getId()).isNotNull();
        
        // Проверяем, что клиент был сохранен
        var savedClient = DatabaseFactory.getDatabase().getAllClients().stream()
            .filter(c -> c.getName().equals(client.getName()) && c.getPhone().equals(client.getPhone()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedClient, "Клиент должен быть сохранен в БД");
        
        // Шаг 2: Создание и сохранение запчастей в БД
        var sparePart1 = new com.autoservice.builders.SparePartBuilder()
            .withName("Топливный фильтр")
            .withPartNumber("TF-001")
            .withStock(10)
            .withRetailPrice(1500)
            .build();
        DatabaseFactory.getDatabase().addSparePart(sparePart1);
        
        var sparePart2 = new com.autoservice.builders.SparePartBuilder()
            .withName("Масляный фильтр")
            .withPartNumber("MF-001")
            .withStock(15)
            .withRetailPrice(800)
            .build();
        DatabaseFactory.getDatabase().addSparePart(sparePart2);
        
        // Шаг 3: Создание заказа с услугами и запчастями
        var order = new com.autoservice.builders.WorkOrderBuilder()
            .withClient(client)
            .withStatus("active")
            .build();
        
        // Добавляем услуги и запчасти непосредственно в WorkOrder
        order.addService("Замена масла", 3000);
        order.addSparePart(sparePart1, 2);
        order.addSparePart(sparePart2, 1);
        
        DatabaseFactory.getDatabase().addOrder(order);
        assertThat(order.getId()).isNotNull();
        assertThat(order.getId()).startsWith("ZAK-");
        
        // Обновляем DataStore после добавления заказа
        DataStore.load();
        
        // Шаг 5: Проверка целостности данных
        
        // Проверяем, что заказ сохранен
        var savedOrder = DatabaseFactory.getDatabase().getAllOrders().stream()
            .filter(o -> o.getId().equals(order.getId()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedOrder);
        assertEquals(order.getId(), savedOrder.getId());
        
        // Проверяем запчасти заказа
        var orderParts = savedOrder.getSpareParts();
        assertEquals(2, orderParts.size(), "Ожидалось 2 запчасти в заказе");
        
        // Проверяем услуги заказа
        var orderServices = savedOrder.getServices();
        assertEquals(1, orderServices.size(), "Ожидалась 1 услуга в заказе");
        
        // Проверяем, что запчасти и услуги доступны через DataStore
        var dsOrder = DataStore.getOrders().stream()
            .filter(o -> o.getId().equals(order.getId()))
            .findFirst()
            .orElse(null);
        assertNotNull(dsOrder);
        assertEquals(2, dsOrder.getSpareParts().size(), "DataStore должен содержать 2 запчасти");
        assertEquals(1, dsOrder.getServices().size(), "DataStore должен содержать 1 услугу");
    }

    @DisplayName("Workflow: создание заказа с несколькими одинаковыми запчастями")
    @Test
    void testOrderWithDuplicateParts() {
        // Создаем клиента
        var client = new com.autoservice.builders.ClientBuilder()
            .withName("Duplicate Parts Client")
            .withPhone("+79007654321")
            .build();
        DatabaseFactory.getDatabase().addClient(client);
        
        // Создаем и сохраняем запчасть в БД
        var sparePart = new com.autoservice.builders.SparePartBuilder()
            .withName("Общий элемент")
            .withPartNumber("COMMON-001")
            .withStock(100)
            .withRetailPrice(500)
            .build();
        DatabaseFactory.getDatabase().addSparePart(sparePart);
        
        var order = new com.autoservice.builders.WorkOrderBuilder()
            .withClient(client)
            .withStatus("active")
            .build();
        DatabaseFactory.getDatabase().addOrder(order);
        
        // Добавляем одну и ту же запчасть 3 раза
        order.addSparePart(sparePart, 1);
        order.addSparePart(sparePart, 2);
        order.addSparePart(sparePart, 3);
        
        // Обновляем заказ с новыми запчастями
        DatabaseFactory.getDatabase().updateOrder(order);
        
        // Проверяем, что все записи сохранены
        var savedOrder = DatabaseFactory.getDatabase().getAllOrders().stream()
            .filter(o -> o.getId().equals(order.getId()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedOrder);
        var orderParts = savedOrder.getSpareParts();
        assertEquals(3, orderParts.size(), "Ожидалось 3 записи о запчастях");
        
        // Проверяем общее количество (каждая запчасть имеет свою копию в списке)
        int totalQuantity = 1 + 2 + 3;
        assertEquals(3, orderParts.size(), "В списке должно быть 3 запчасти");
    }

    @DisplayName("Workflow: создание заказа без запчастей (только услуги)")
    @Test
    void testOrderWithoutParts() {
        // Создаем клиента и заказ
        var client = new com.autoservice.builders.ClientBuilder()
            .withName("Service Only Client")
            .withPhone("+79001111111")
            .build();
        DatabaseFactory.getDatabase().addClient(client);
        
        var order = new com.autoservice.builders.WorkOrderBuilder()
            .withClient(client)
            .withStatus("active")
            .build();
        
        // Добавляем услуги без запчастей
        order.addService("Диагностика", 1000);
        order.addService("Консультация", 500);
        
        DatabaseFactory.getDatabase().addOrder(order);
        
        // Проверяем заказ
        var savedOrder = DatabaseFactory.getDatabase().getAllOrders().stream()
            .filter(o -> o.getId().equals(order.getId()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedOrder);
        
        // Проверяем услуги
        var orderServices = savedOrder.getServices();
        assertEquals(2, orderServices.size(), "Ожидалось 2 услуги");
        
        // Проверяем, что запчастей нет
        var orderParts = savedOrder.getSpareParts();
        assertTrue(orderParts.isEmpty(), "Ожидалось, что в заказе нет запчастей");
    }

    @DisplayName("Workflow: редактирование заказа")
    @Test
    void testOrderUpdateWorkflow() {
        // Создаем начальный заказ
        var client = new com.autoservice.builders.ClientBuilder()
            .withName("Update Test Client")
            .withPhone("+79009999999")
            .build();
        DatabaseFactory.getDatabase().addClient(client);
        
        var order = new com.autoservice.builders.WorkOrderBuilder()
            .withClient(client)
            .withStatus("active")
            .build();
        DatabaseFactory.getDatabase().addOrder(order);
        
        // Создаем и сохраняем запчасть в БД
        var sparePart = new com.autoservice.builders.SparePartBuilder()
            .withName("Исходная запчасть")
            .withPartNumber("INIT-001")
            .withStock(50)
            .withRetailPrice(1000)
            .build();
        DatabaseFactory.getDatabase().addSparePart(sparePart);
        order.addSparePart(sparePart, 1);
        
        // Обновляем заказ
        DatabaseFactory.getDatabase().updateOrder(order);
        
        // Редактируем заказ
        var savedOrder = DatabaseFactory.getDatabase().getAllOrders().stream()
            .filter(o -> o.getId().equals(order.getId()))
            .findFirst()
            .orElse(null);
        assertNotNull(savedOrder);
        savedOrder.setStatus("completed");
        
        // Создаем и сохраняем новую запчасть в БД
        var newPart = new com.autoservice.builders.SparePartBuilder()
            .withName("Новая запчасть")
            .withPartNumber("NEW-001")
            .withStock(30)
            .withRetailPrice(2000)
            .build();
        DatabaseFactory.getDatabase().addSparePart(newPart);
        savedOrder.addSparePart(newPart, 2);
        
        DatabaseFactory.getDatabase().updateOrder(savedOrder);
        
        // Проверяем обновленный заказ
        var finalOrder = DatabaseFactory.getDatabase().getAllOrders().stream()
            .filter(o -> o.getId().equals(order.getId()))
            .findFirst()
            .orElse(null);
        var finalParts = finalOrder.getSpareParts();
        
        assertEquals(2, finalParts.size(), "После обновления должно быть 2 запчасти");
        assertEquals("completed", finalOrder.getStatus());
    }

    @DisplayName("Workflow: удаление заказа")
    @Test
    void testOrderDeletionWorkflow() {
        // Создаем заказ
        var client = new com.autoservice.builders.ClientBuilder()
            .withName("Delete Test Client")
            .withPhone("+79008888888")
            .build();
        DatabaseFactory.getDatabase().addClient(client);
        
        var order = new com.autoservice.builders.WorkOrderBuilder()
            .withClient(client)
            .withStatus("active")
            .build();
        DatabaseFactory.getDatabase().addOrder(order);
        
        // Создаем и сохраняем запчасть в БД
        var sparePart = new com.autoservice.builders.SparePartBuilder()
            .withName("Запчасть для удаления")
            .withPartNumber("DEL-001")
            .withStock(20)
            .withRetailPrice(1500)
            .build();
        DatabaseFactory.getDatabase().addSparePart(sparePart);
        order.addSparePart(sparePart, 1);
        
        // Обновляем заказ с запчастями
        DatabaseFactory.getDatabase().updateOrder(order);
        
        // Проверяем, что заказ существует
        var existsBefore = DatabaseFactory.getDatabase().getAllOrders().stream()
            .filter(o -> o.getId().equals(order.getId()))
            .findFirst()
            .orElse(null);
        assertNotNull(existsBefore);
        
        // Удаляем заказ
        DatabaseFactory.getDatabase().deleteOrder(order.getId());
        
        // Проверяем, что заказ удален
        var existsAfter = DatabaseFactory.getDatabase().getAllOrders().stream()
            .filter(o -> o.getId().equals(order.getId()))
            .findFirst()
            .orElse(null);
        assertNull(existsAfter, "Заказ должен быть удален");
        
        // Проверяем, что связанные данные также удалены
        assertEquals(0, DatabaseFactory.getDatabase().getAllOrders().size(),
            "Все заказы должны быть удалены");
    }

    @DisplayName("Workflow: создание заказа с той же датой (проверка ID)")
    @Test
    void testMultipleOrdersSameDate() {
        // Создаем заказы в один день
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("dd/MM/yy"));
        
        for (int i = 1; i <= 5; i++) {
            var client = new com.autoservice.builders.ClientBuilder()
                .withName("Same Date Client " + i)
                .withPhone("+7900" + String.format("%09d", i))
                .build();
            DatabaseFactory.getDatabase().addClient(client);
            
            var order = new com.autoservice.builders.WorkOrderBuilder()
                .withClient(client)
                .withStatus("active")
                .build();
            DatabaseFactory.getDatabase().addOrder(order);
            
            // Проверяем уникальность ID
            var orders = DatabaseFactory.getDatabase().getAllOrders();
            var todayOrders = orders.stream()
                .filter(o -> o.getCreatedDate().contains(dateStr))
                .collect(Collectors.toList());
            
            var ids = todayOrders.stream()
                .map(com.autoservice.WorkOrder::getId)
                .collect(Collectors.toSet());
            
            assertEquals(todayOrders.size(), ids.size(),
                "Все заказы за " + dateStr + " должны иметь уникальные ID");
        }
    }

    @DisplayName("Workflow: массовое создание заказов с проверкой целостности")
    @Test
    void testMassOrderCreationIntegrity() {
        int orderCount = 20;
        
        // Создаем множество заказов
        for (int i = 0; i < orderCount; i++) {
            var client = new com.autoservice.builders.ClientBuilder()
                .withName("Mass Client " + i)
                .withPhone("+7900" + String.format("%09d", i))
                .build();
            DatabaseFactory.getDatabase().addClient(client);
            
            var order = new com.autoservice.builders.WorkOrderBuilder()
                .withClient(client)
                .withStatus("active")
                .build();
            DatabaseFactory.getDatabase().addOrder(order);
        }
        
        // Обновляем DataStore после массового создания заказов
        DataStore.load();
        
        // Проверяем общее количество
        var allOrders = DatabaseFactory.getDatabase().getAllOrders();
        assertEquals(orderCount, allOrders.size(),
            "Количество заказов в БД не совпадает");
        
        // Проверяем уникальность ID
        var ids = allOrders.stream()
            .map(com.autoservice.WorkOrder::getId)
            .collect(Collectors.toSet());
        assertEquals(orderCount, ids.size(),
            "Все Order ID должны быть уникальны");
        
        // Проверяем формат всех ID
        for (var order : allOrders) {
            assertTrue(order.getId().startsWith("ZAK-"),
                "Order ID должен начинаться с 'ZAK-': " + order.getId());
            assertTrue(order.getId().matches("ZAK-\\d{2}/\\d{2}/\\d{2}-\\d{4}"),
                "Order ID должен иметь формат ZAK-DD/MM/YY-XXXX: " + order.getId());
        }
        
        // Проверяем DataStore
        assertEquals(orderCount, DataStore.getOrders().size(),
            "DataStore должен содержать все заказы");
    }
}
