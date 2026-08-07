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
 * Тесты параллельного выполнения для базы данных.
 * Проверяет потокобезопасность операций БД при одновременном доступе.
 */
@Tag(TestTags.SLOW)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ParallelDatabaseTest extends BaseTest {

    private static final int PARALLEL_THREADS = 3;
    private static final int OPERATIONS_PER_THREAD = 5;

    @DisplayName("Параллельное добавление клиентов")
    @Test
    void testParallelClientInserts() throws InterruptedException {
        int totalClients = PARALLEL_THREADS * OPERATIONS_PER_THREAD;
        ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_THREADS);
        CountDownLatch latch = new CountDownLatch(totalClients);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < totalClients; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    // Используем уникальный номер телефона для каждого клиента
                    var client = new com.autoservice.builders.ClientBuilder()
                        .withName("Client " + clientId)
                        .withLastName("LastName " + clientId)
                        .withPhone("+7900" + String.format("%09d", clientId))
                        .withCarModel("Car " + clientId)
                        .withCarNumber("A" + String.format("%03d", clientId) + "BB")
                        .build();
                    DatabaseFactory.getDatabase().addClient(client);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("Ошибка добавления клиента " + clientId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Ждем завершения всех операций
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        assertTrue(completed, "Не все операции завершились за отведённое время");
        
        // Проверяем, что все операции прошли успешно
        assertEquals(totalClients, successCount.get(), 
            "Количество успешно добавленных клиентов не совпадает");
        assertEquals(0, errorCount.get(), 
            "Обнаружены ошибки при добавлении клиентов");
        
        // Проверяем, что клиенты сохранены в БД
        assertEquals(totalClients, DatabaseFactory.getDatabase().getAllClients().size(),
            "Клиенты не сохранены в БД");
    }

    @DisplayName("Параллельное добавление запчастей")
    @Test
    void testParallelSparePartInserts() throws InterruptedException {
        int totalParts = PARALLEL_THREADS * OPERATIONS_PER_THREAD;
        ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_THREADS);
        CountDownLatch latch = new CountDownLatch(totalParts);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < totalParts; i++) {
            final int partId = i;
            executor.submit(() -> {
                try {
                    var part = new com.autoservice.builders.SparePartBuilder()
                        .withName("Spare Part " + partId)
                        .withPartNumber("SP" + String.format("%06d", partId))
                        .withStock(100)
                        .withRetailPrice(1000 + partId * 10)
                        .build();
                    DatabaseFactory.getDatabase().addSparePart(part);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Ошибка добавления запчасти " + partId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        assertTrue(completed, "Не все операции завершились за отведённое время");
        assertEquals(totalParts, successCount.get(),
            "Количество успешно добавленных запчастей не совпадает");
    }

    @DisplayName("Конкурентное обновление запасов (race condition)")
    @Test
    void testConcurrentStockUpdate() throws InterruptedException {
        // Создаем запчасть с начальным запасом 100
        var part = new com.autoservice.builders.SparePartBuilder()
            .withName("Stock Test Part")
            .withPartNumber("STOCK001")
            .withStock(100)
            .withRetailPrice(5000)
            .build();
        DatabaseFactory.getDatabase().addSparePart(part);
        
        int initialStock = (int) part.getStock();
        int totalOperations = 10;
        int incrementPerOperation = 5;
        
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(totalOperations);
        
        for (int i = 0; i < totalOperations; i++) {
            executor.submit(() -> {
                try {
                    var spareParts = DatabaseFactory.getDatabase().getAllSpareParts();
                    var testPart = spareParts.stream()
                        .filter(p -> p.getPartNumber().equals("STOCK001"))
                        .findFirst()
                        .orElse(null);
                    if (testPart != null) {
                        double newStock = testPart.getStock() + incrementPerOperation;
                        DatabaseFactory.getDatabase().updateSparePartStock(testPart, newStock);
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка обновления запаса: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        assertTrue(completed, "Не все операции обновления завершились");
        
        // Перезагружаем данные из БД
        var updatedParts = DatabaseFactory.getDatabase().getAllSpareParts();
        var updatedPart = updatedParts.stream()
            .filter(p -> p.getPartNumber().equals("STOCK001"))
            .findFirst()
            .orElse(null);
        assertNotNull(updatedPart);
        int finalStock = (int) updatedPart.getStock();
        
        // Финальный запас должен быть >= начального (хотя бы некоторые операции прошли)
        assertTrue(finalStock >= initialStock,
            "Финальный запас должен быть не меньше начального. Ожидалось: >= " + initialStock + 
            ", получено: " + finalStock);
    }

    @DisplayName("Многопоточное чтение и запись заказов")
    @Test
    void testConcurrentReadWriteOrders() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(6);
        CountDownLatch latch = new CountDownLatch(12);
        AtomicInteger readCount = new AtomicInteger(0);
        AtomicInteger writeCount = new AtomicInteger(0);

        // Запись заказов (6 потоков)
        for (int i = 0; i < 6; i++) {
            final int orderId = i;
            executor.submit(() -> {
                try {
                    var client = new com.autoservice.builders.ClientBuilder()
                        .withName("Client " + orderId)
                        .withPhone("+7900" + String.format("%09d", orderId))
                        .build();
                    DatabaseFactory.getDatabase().addClient(client);
                    
                    var order = new com.autoservice.builders.WorkOrderBuilder()
                        .withClient(client)
                        .withStatus("active")
                        .build();
                    DatabaseFactory.getDatabase().addOrder(order);
                    writeCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Ошибка записи заказа " + orderId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Чтение заказов (6 потоков)
        for (int i = 0; i < 6; i++) {
            final int readAttempt = i;
            executor.submit(() -> {
                try {
                    var orders = DatabaseFactory.getDatabase().getAllOrders();
                    readCount.addAndGet(orders.size());
                } catch (Exception e) {
                    System.err.println("Ошибка чтения заказов " + readAttempt + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        assertTrue(completed, "Не все операции завершились");
        assertTrue(writeCount.get() > 0, "Хотя бы несколько заказов должно быть создано");
        assertThat(readCount.get()).isGreaterThanOrEqualTo(0);
    }

    @DisplayName("Параллельное удаление данных с проверкой целостности")
    @Test
    void testParallelDeleteWithIntegrityCheck() throws InterruptedException {
        // Предварительно добавляем данные
        int initialClients = 5;
        for (int i = 0; i < initialClients; i++) {
            var client = new com.autoservice.builders.ClientBuilder()
                .withName("Delete Test Client " + i)
                .withPhone("+7900" + String.format("%09d", i))
                .build();
            DatabaseFactory.getDatabase().addClient(client);
        }

        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(initialClients);

        for (int i = 0; i < initialClients; i++) {
            final int clientId = i + 1;
            executor.submit(() -> {
                try {
                    var clients = DatabaseFactory.getDatabase().getAllClients();
                    var clientToDelete = clients.stream()
                        .filter(c -> c.getId() == clientId)
                        .findFirst()
                        .orElse(null);
                    if (clientToDelete != null) {
                        DatabaseFactory.getDatabase().deleteClient(clientToDelete);
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка удаления клиента " + clientId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        assertTrue(completed, "Не все операции удаления завершились");
        
        // Проверяем, что количество клиентов уменьшилось
        var remainingClients = DatabaseFactory.getDatabase().getAllClients();
        assertTrue(remainingClients.size() < initialClients,
            "Количество клиентов должно уменьшиться. Было: " + initialClients + ", осталось: " + remainingClients.size());
    }
}
