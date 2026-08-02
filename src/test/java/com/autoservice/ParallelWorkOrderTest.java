package com.autoservice;

import org.junit.jupiter.api.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты параллельного выполнения для создания заказов.
 * Проверяет безопасность параллельного создания заказов и обновления запасов.
 */
@Tag(TestTags.SLOW)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ParallelWorkOrderTest extends BaseTest {

    private static final int PARALLEL_THREADS = 5;
    private static final int ORDERS_PER_THREAD = 4;

    @BeforeEach
    void setupTestData() {
        // Подготовка тестовых данных: создаем клиентов и запчасти
        for (int i = 0; i < 10; i++) {
            var client = new com.autoservice.builders.ClientBuilder()
                .withName("Client " + i)
                .withPhone("+7900" + String.format("%09d", i))
                .build();
            DatabaseFactory.getDatabase().addClient(client);
        }

        for (int i = 0; i < 5; i++) {
            var part = new com.autoservice.builders.SparePartBuilder()
                .withName("Part " + i)
                .withPartNumber("PART" + String.format("%06d", i))
                .withStock(100)
                .withRetailPrice(1000 + i * 500)
                .build();
            DatabaseFactory.getDatabase().addSparePart(part);
        }
    }

    @DisplayName("Параллельное создание заказов с уникальными ID")
    @Test
    void testParallelOrderCreation() throws InterruptedException {
        int totalOrders = PARALLEL_THREADS * ORDERS_PER_THREAD;
        ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_THREADS);
        CountDownLatch latch = new CountDownLatch(totalOrders);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < totalOrders; i++) {
            final int orderId = i;
            executor.submit(() -> {
                try {
                    // Используем разного клиента для каждого заказа
                    int clientId = (orderId % 10) + 1;
                    
                    var client = DatabaseFactory.getDatabase().getAllClients().stream()
                        .filter(c -> c.getId() == clientId)
                        .findFirst()
                        .orElse(null);
                    assertNotNull(client);
                    
                    var order = new com.autoservice.builders.WorkOrderBuilder()
                        .withClient(client)
                        .withStatus("active")
                        .build();
                    
                    DatabaseFactory.getDatabase().addOrder(order);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("Ошибка создания заказа " + orderId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(45, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        assertTrue(completed, "Не все заказы были созданы за отведённое время");
        assertEquals(totalOrders, successCount.get(),
            "Количество успешно созданных заказов не совпадает");
        assertEquals(0, errorCount.get(), "Обнаружены ошибки при создании заказов");

        // Проверяем, что все заказы имеют уникальные ID
        var orders = DatabaseFactory.getDatabase().getAllOrders();
        assertEquals(totalOrders, orders.size(),
            "Количество заказов в БД не совпадает");
        
        // Проверяем уникальность ID заказов
        var idSet = orders.stream()
            .map(com.autoservice.WorkOrder::getId)
            .distinct()
            .count();
        assertEquals(totalOrders, idSet,
            "Обнаружены дубликаты ID заказов (race condition)");
    }

    @DisplayName("Параллельное создание заказов с запчастями")
    @Test
    void testParallelOrderWithParts() throws InterruptedException {
        int totalOrders = 8;
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(totalOrders);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < totalOrders; i++) {
            final int orderId = i;
            executor.submit(() -> {
                try {
                    // Создание заказа
                    int clientId = (orderId % 10) + 1;
                    var client = DatabaseFactory.getDatabase().getAllClients().stream()
                        .filter(c -> c.getId() == clientId)
                        .findFirst()
                        .orElse(null);
                    assertNotNull(client);
                    
                    var order = new com.autoservice.builders.WorkOrderBuilder()
                        .withClient(client)
                        .withStatus("active")
                        .build();
                    
                    DatabaseFactory.getDatabase().addOrder(order);
                    
                    // Получаем только что созданный заказ
                    var createdOrder = DatabaseFactory.getDatabase().getAllOrders().stream()
                        .filter(o -> o.getId().equals(order.getId()))
                        .findFirst()
                        .orElse(null);
                    assertNotNull(createdOrder);
                    
                    // Добавляем часть к заказу через WorkOrder.addSparePart
                    int partId = (orderId % 5) + 1;
                    var sparePart = DatabaseFactory.getDatabase().getAllSpareParts().stream()
                        .filter(p -> p.getId() == partId)
                        .findFirst()
                        .orElse(null);
                    assertNotNull(sparePart);
                    
                    // WorkOrder хранит запчасти внутри себя, добавляем к существующему заказу
                    createdOrder.addSparePart(sparePart, 1);
                    DatabaseFactory.getDatabase().updateOrder(createdOrder);
                    
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Ошибка создания заказа с запчастями " + orderId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(45, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        assertTrue(completed, "Не все заказы были созданы");
        assertEquals(totalOrders, successCount.get(),
            "Количество заказов с запчастями не совпадает");
    }

    @DisplayName("Конкурентное обновление запасов при создании заказов")
    @Test
    void testConcurrentStockUpdateWithOrders() throws InterruptedException {
        // Создаем запчасть с ограниченным запасом
        var part = new com.autoservice.builders.SparePartBuilder()
            .withName("Limited Stock Part")
            .withPartNumber("LIMITED001")
            .withStock(50)
            .withRetailPrice(2000)
            .build();
        DatabaseFactory.getDatabase().addSparePart(part);
        
        int initialStock = (int) part.getStock();
        int totalOrders = 20;
        int partsPerOrder = 2;
        int expectedRemaining = initialStock - (totalOrders * partsPerOrder);
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(totalOrders);
        AtomicInteger successOrders = new AtomicInteger(0);

        for (int i = 0; i < totalOrders; i++) {
            final int orderId = i;
            executor.submit(() -> {
                try {
                    // Создаем клиента для заказа
                    var client = new com.autoservice.builders.ClientBuilder()
                        .withName("Order Client " + orderId)
                        .withPhone("+7900" + String.format("%09d", orderId))
                        .build();
                    DatabaseFactory.getDatabase().addClient(client);
                    
                    // Создаем заказ
                    var order = new com.autoservice.builders.WorkOrderBuilder()
                        .withClient(client)
                        .withStatus("active")
                        .build();
                    DatabaseFactory.getDatabase().addOrder(order);
                    
                    // Получаем запчасть и обновляем запас
                    var orderParts = DatabaseFactory.getDatabase().getAllSpareParts();
                    var testPart = orderParts.stream()
                        .filter(p -> p.getPartNumber().equals("LIMITED001"))
                        .findFirst()
                        .orElse(null);
                    assertNotNull(testPart);
                    
                    double newStock = testPart.getStock() - partsPerOrder;
                    DatabaseFactory.getDatabase().updateSparePartStock(testPart, newStock);
                    
                    successOrders.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Ошибка заказа " + orderId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(45, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        assertTrue(completed, "Не все заказы были созданы");
        assertEquals(totalOrders, successOrders.get(),
            "Количество созданных заказов не совпадает");

        // Проверяем остаток на складе
        var updatedPart = DatabaseFactory.getDatabase().getAllSpareParts().stream()
            .filter(p -> p.getPartNumber().equals("LIMITED001"))
            .findFirst()
            .orElse(null);
        assertNotNull(updatedPart);
        int finalStock = (int) updatedPart.getStock();
        
        assertEquals(expectedRemaining, finalStock,
            "Финальный запас не совпадает с ожидаемым. Использовано: " + 
            (initialStock - finalStock) + ", Ожидалось: " + (totalOrders * partsPerOrder));
    }

    @DisplayName("Параллельное получение последовательных Order ID")
    @Test
    void testSequentialOrderIdGeneration() throws InterruptedException {
        int totalOrders = 15;
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(totalOrders);
        var orderIds = new java.util.concurrent.ConcurrentLinkedQueue<String>();
        var timestamps = new java.util.concurrent.ConcurrentLinkedQueue<Long>();

        for (int i = 0; i < totalOrders; i++) {
            executor.submit(() -> {
                try {
                    var client = new com.autoservice.builders.ClientBuilder()
                        .withName("ID Test Client " + System.nanoTime())
                        .withPhone("+7900" + System.nanoTime())
                        .build();
                    DatabaseFactory.getDatabase().addClient(client);
                    
                    var order = new com.autoservice.builders.WorkOrderBuilder()
                        .withClient(client)
                        .withStatus("active")
                        .build();
                    
                    long start = System.currentTimeMillis();
                    DatabaseFactory.getDatabase().addOrder(order);
                    long end = System.currentTimeMillis();
                    
                    orderIds.add(order.getId());
                    timestamps.add(end - start);
                } catch (Exception e) {
                    System.err.println("Ошибка генерации ID: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertTrue(completed, "Не все Order ID были сгенерированы");
        
        // Проверяем уникальность
        assertEquals(totalOrders, orderIds.size(),
            "Количество Order ID не совпадает");
        
        var uniqueIds = orderIds.stream().distinct().count();
        assertEquals(totalOrders, uniqueIds,
            "Обнаружены дубликаты Order ID (race condition в генерации)");
        
        // Проверяем формат ID
        for (String orderId : orderIds) {
            assertTrue(orderId.startsWith("ZAK-"),
                "Order ID должен начинаться с 'ZAK-': " + orderId);
            assertTrue(orderId.matches("ZAK-\\d{2}/\\d{2}/\\d{2}-\\d{4}"),
                "Order ID должен иметь формат ZAK-DD/MM/YY-XXXX: " + orderId);
        }
    }

    @AfterEach
    void verifyDataIntegrity() {
        // Проверка целостности данных после каждого теста
        var orders = DatabaseFactory.getDatabase().getAllOrders();
        var clients = DatabaseFactory.getDatabase().getAllClients();
        
        // Проверяем, что количество клиентов и заказов в DataStore совпадает с БД
        assertThat(DataStore.getClients().size()).isEqualTo(clients.size());
        assertThat(DataStore.getOrders().size()).isEqualTo(orders.size());
    }
}
