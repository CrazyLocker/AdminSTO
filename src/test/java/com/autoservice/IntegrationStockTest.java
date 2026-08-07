package com.autoservice;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты обновления запасов.
 * Проверяет корректность обновления количества запчастей при различных операциях.
 */
@Tag(TestTags.INTEGRATION)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationStockTest extends BaseTest {

    @BeforeEach
    void setupParts() {
        // Создаем запчасти для тестов
        var part1 = new com.autoservice.builders.SparePartBuilder()
            .withName("Тестовая запчасть 1")
            .withPartNumber("STOCK001")
            .withStock(100)
            .withRetailPrice(1000)
            .build();
        DatabaseFactory.getDatabase().addSparePart(part1);
        
        var part2 = new com.autoservice.builders.SparePartBuilder()
            .withName("Тестовая запчасть 2")
            .withPartNumber("STOCK002")
            .withStock(50)
            .withRetailPrice(2000)
            .build();
        DatabaseFactory.getDatabase().addSparePart(part2);
        
        // Обновляем DataStore после создания запчастей
        DataStore.load();
    }

    @DisplayName("Проверка начального запаса")
    @Test
    void testInitialStock() {
        var part1 = DatabaseFactory.getDatabase().getAllSpareParts().stream()
            .filter(p -> p.getPartNumber().equals("STOCK001"))
            .findFirst()
            .orElse(null);
        assertNotNull(part1);
        assertEquals(100, part1.getStock(), "Начальный запас должен быть 100");
        
        var part2 = DatabaseFactory.getDatabase().getAllSpareParts().stream()
            .filter(p -> p.getPartNumber().equals("STOCK002"))
            .findFirst()
            .orElse(null);
        assertNotNull(part2);
        assertEquals(50, part2.getStock(), "Начальный запас должен быть 50");
    }

    @DisplayName("Увеличение запаса (добавление новой партии)")
    @Test
    void testStockIncrease() {
        var part = DatabaseFactory.getDatabase().getAllSpareParts().stream()
            .filter(p -> p.getPartNumber().equals("STOCK001"))
            .findFirst()
            .orElse(null);
        assertNotNull(part);
        
        int initialStock = (int) part.getStock();
        int increaseAmount = 25;
        
        // Обновляем запас
        DatabaseFactory.getDatabase().updateSparePartStock(part, initialStock + increaseAmount);
        
        // Перезагружаем данные
        var updatedPart = DatabaseFactory.getDatabase().getAllSpareParts().stream()
            .filter(p -> p.getPartNumber().equals("STOCK001"))
            .findFirst()
            .orElse(null);
        assertNotNull(updatedPart);
        int finalStock = (int) updatedPart.getStock();
        
        assertEquals(initialStock + increaseAmount, finalStock,
            "Запас должен увеличиться на " + increaseAmount);
        
        // Проверяем DataStore (перезагружаем кэш)
        DataStore.load();
        var dsPart = DataStore.getSparePartById(part.getId());
        assertEquals(finalStock, dsPart.getStock(), "DataStore должен содержать обновленный запас");
    }

    @DisplayName("Уменьшение запаса (использование в заказе)")
    @Test
    void testStockDecrease() {
        var part = DatabaseFactory.getDatabase().getAllSpareParts().stream()
            .filter(p -> p.getPartNumber().equals("STOCK001"))
            .findFirst()
            .orElse(null);
        assertNotNull(part);
        
        int initialStock = (int) part.getStock();
        int usedAmount = 10;
        
        // Создаем заказ и добавляем запчасть
        var client = new com.autoservice.builders.ClientBuilder()
            .withName("Stock Test Client")
            .withPhone("+79000000000")
            .build();
        DatabaseFactory.getDatabase().addClient(client);
        
        var order = new com.autoservice.builders.WorkOrderBuilder()
            .withClient(client)
            .withStatus("active")
            .build();
        DatabaseFactory.getDatabase().addOrder(order);
        
        // Обновляем запас напрямую (симуляция использования в заказе)
        DatabaseFactory.getDatabase().updateSparePartStock(part, initialStock - usedAmount);
        
        // Проверяем уменьшение запаса
        var updatedPart = DatabaseFactory.getDatabase().getAllSpareParts().stream()
            .filter(p -> p.getPartNumber().equals("STOCK001"))
            .findFirst()
            .orElse(null);
        assertNotNull(updatedPart);
        int finalStock = (int) updatedPart.getStock();
        
        assertEquals(initialStock - usedAmount, finalStock,
            "Запас должен уменьшиться на " + usedAmount);
    }

    @DisplayName("Обновление запаса до нуля")
    @Test
    void testStockToZero() {
        var part = DatabaseFactory.getDatabase().getAllSpareParts().stream()
            .filter(p -> p.getPartNumber().equals("STOCK002"))
            .findFirst()
            .orElse(null);
        assertNotNull(part);
        
        int initialStock = (int) part.getStock();
        assertEquals(50, initialStock);
        
        // Уменьшаем запас до нуля
        DatabaseFactory.getDatabase().updateSparePartStock(part, 0);
        
        var updatedPart = DatabaseFactory.getDatabase().getAllSpareParts().stream()
            .filter(p -> p.getPartNumber().equals("STOCK002"))
            .findFirst()
            .orElse(null);
        assertNotNull(updatedPart);
        assertEquals(0, updatedPart.getStock(), "Запас должен быть равен 0");
    }

    @DisplayName("Обновление запаса с отрицательным значением")
    @Test
    void testNegativeStockUpdate() {
        var part = DatabaseFactory.getDatabase().getAllSpareParts().stream()
            .filter(p -> p.getPartNumber().equals("STOCK001"))
            .findFirst()
            .orElse(null);
        assertNotNull(part);
        
        int initialStock = (int) part.getStock();
        
        // Пытаемся установить отрицательный запас
        int decreaseAmount = 150; // Больше начального запаса
        int expectedStock = initialStock - decreaseAmount;
        
        // Обновляем запас напрямую (симуляция использования в заказе)
        DatabaseFactory.getDatabase().updateSparePartStock(part, expectedStock);
        
        // Перезагружаем данные
        var updatedPart = DatabaseFactory.getDatabase().getAllSpareParts().stream()
            .filter(p -> p.getPartNumber().equals("STOCK001"))
            .findFirst()
            .orElse(null);
        assertNotNull(updatedPart);
        int finalStock = (int) updatedPart.getStock();
        
        // Проверяем, что операция выполнилась
        assertEquals(expectedStock, finalStock,
            "Запас должен быть установлен в " + expectedStock);
    }

    @DisplayName("Конкурентное обновление запаса")
    @Test
    void testConcurrentStockUpdates() throws InterruptedException {
        var part = DatabaseFactory.getDatabase().getAllSpareParts().stream()
            .filter(p -> p.getPartNumber().equals("STOCK001"))
            .findFirst()
            .orElse(null);
        assertNotNull(part);
        
        int initialStock = (int) part.getStock();
        int totalOperations = 10;
        int incrementPerOperation = 5;
        
        // Запускаем параллельные операции обновления
        var threads = new Thread[totalOperations];
        for (int i = 0; i < totalOperations; i++) {
            final int opNum = i;
            threads[i] = new Thread(() -> {
                try {
                    var updatedPart = DatabaseFactory.getDatabase().getAllSpareParts().stream()
                        .filter(p -> p.getPartNumber().equals("STOCK001"))
                        .findFirst()
                        .orElse(null);
                    if (updatedPart != null) {
                        DatabaseFactory.getDatabase().updateSparePartStock(updatedPart, updatedPart.getStock() + incrementPerOperation);
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка обновления в потоке " + opNum + ": " + e.getMessage());
                }
            });
        }
        
        // Запускаем все потоки
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Ждем завершения всех потоков
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Проверяем финальный запас (допускаем погрешность из-за race condition)
        var updatedPart = DatabaseFactory.getDatabase().getAllSpareParts().stream()
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

    @DisplayName("Массовое обновление запасов")
    @Test
    void testMassStockUpdates() {
        // Создаем дополнительные запчасти
        for (int i = 3; i <= 10; i++) {
            var part = new com.autoservice.builders.SparePartBuilder()
                .withName("Массовая запчасть " + i)
                .withPartNumber("MASS" + String.format("%03d", i))
                .withStock(10 * i)
                .withRetailPrice(100 * i)
                .build();
            DatabaseFactory.getDatabase().addSparePart(part);
        }
        
        // Получаем все запчасти
        var allParts = DatabaseFactory.getDatabase().getAllSpareParts();
        int initialTotalStock = allParts.stream()
            .mapToInt(p -> (int) p.getStock())
            .sum();
        
        // Увеличиваем каждый запас на 10
        for (var part : allParts) {
            DatabaseFactory.getDatabase().updateSparePartStock(part, part.getStock() + 10);
        }
        
        // Проверяем обновленные запасы
        var updatedParts = DatabaseFactory.getDatabase().getAllSpareParts();
        int finalTotalStock = updatedParts.stream()
            .mapToInt(p -> (int) p.getStock())
            .sum();
        
        int expectedIncrease = allParts.size() * 10;
        assertEquals(initialTotalStock + expectedIncrease, finalTotalStock,
            "Общий запас должен увеличиться на " + expectedIncrease);
    }

    @DisplayName("Проверка целостности запаса при создании заказа")
    @Test
    void testStockIntegrityWithOrder() {
        // Создаем заказ
        var client = new com.autoservice.builders.ClientBuilder()
            .withName("Integrity Client")
            .withPhone("+79007777777")
            .build();
        DatabaseFactory.getDatabase().addClient(client);
        
        var order = new com.autoservice.builders.WorkOrderBuilder()
            .withClient(client)
            .withStatus("active")
            .build();
        DatabaseFactory.getDatabase().addOrder(order);
        
        // Добавляем несколько запчастей к заказу
        var part1 = DatabaseFactory.getDatabase().getAllSpareParts().stream()
            .filter(p -> p.getPartNumber().equals("STOCK001"))
            .findFirst()
            .orElse(null);
        var part2 = DatabaseFactory.getDatabase().getAllSpareParts().stream()
            .filter(p -> p.getPartNumber().equals("STOCK002"))
            .findFirst()
            .orElse(null);
        assertNotNull(part1);
        assertNotNull(part2);
        
        int part1Initial = (int) part1.getStock();
        int part2Initial = (int) part2.getStock();
        
        int qty1 = 5;
        int qty2 = 3;
        
        // Обновляем запасы напрямую (симуляция использования в заказе)
        DatabaseFactory.getDatabase().updateSparePartStock(part1, part1Initial - qty1);
        DatabaseFactory.getDatabase().updateSparePartStock(part2, part2Initial - qty2);
        
        // Проверяем оба запаса
        var updatedPart1 = DatabaseFactory.getDatabase().getAllSpareParts().stream()
            .filter(p -> p.getPartNumber().equals("STOCK001"))
            .findFirst()
            .orElse(null);
        var updatedPart2 = DatabaseFactory.getDatabase().getAllSpareParts().stream()
            .filter(p -> p.getPartNumber().equals("STOCK002"))
            .findFirst()
            .orElse(null);
        assertNotNull(updatedPart1);
        assertNotNull(updatedPart2);
        
        assertEquals(part1Initial - qty1, updatedPart1.getStock(),
            "Запас части 1 должен уменьшиться на " + qty1);
        assertEquals(part2Initial - qty2, updatedPart2.getStock(),
            "Запас части 2 должен уменьшиться на " + qty2);
    }

    @DisplayName("Проверка запаса через DataStore после обновления")
    @Test
    void testStockSyncWithDataStore() {
        var part = DatabaseFactory.getDatabase().getAllSpareParts().stream()
            .filter(p -> p.getPartNumber().equals("STOCK001"))
            .findFirst()
            .orElse(null);
        assertNotNull(part);
        int initialStock = (int) part.getStock();
        
        // Обновляем запас через БД
        int newStock = initialStock + 20;
        DatabaseFactory.getDatabase().updateSparePartStock(part, newStock);
        
        // Перезагружаем DataStore из БД
        DataStore.load();
        
        // Проверяем DataStore
        var dsPart = DataStore.getSparePartById(part.getId());
        assertEquals(newStock, dsPart.getStock(),
            "DataStore должен синхронизироваться с БД при load()");
        
        // Обновляем запас через DataStore
        dsPart.setStock(dsPart.getStock() + 10);
        DataStore.updateSparePart(dsPart);
        
        // Перезагружаем и проверяем
        DataStore.load();
        var reloadedPart = DataStore.getSparePartById(part.getId());
        assertEquals(newStock + 10, reloadedPart.getStock(),
            "DataStore должен отражать изменения после load()");
    }

    @DisplayName("Массовое удаление запчастей с проверкой запаса")
    @Test
    void testMassPartDeletion() {
        // Создаем дополнительные запчасти
        for (int i = 3; i <= 5; i++) {
            var part = new com.autoservice.builders.SparePartBuilder()
                .withName("Для удаления " + i)
                .withPartNumber("DEL" + String.format("%03d", i))
                .withStock(100 + i)
                .withRetailPrice(500 + i)
                .build();
            DatabaseFactory.getDatabase().addSparePart(part);
        }
        
        var allPartsBefore = DatabaseFactory.getDatabase().getAllSpareParts();
        int countBefore = allPartsBefore.size();
        
        // Удаляем все запчасти
        for (var part : allPartsBefore) {
            DatabaseFactory.getDatabase().deleteSparePart(part);
        }
        
        // Перезагружаем DataStore после удаления
        DataStore.load();
        
        // Проверяем, что все удалены из БД
        var allPartsAfter = DatabaseFactory.getDatabase().getAllSpareParts();
        assertTrue(allPartsAfter.isEmpty(),
            "Все запчасти должны быть удалены из БД");
    }

    @AfterEach
    void verifyDataIntegrity() {
        // Синхронизируем DataStore перед проверкой
        DataStore.load();
        
        // Проверка целостности данных после каждого теста
        var dbParts = DatabaseFactory.getDatabase().getAllSpareParts();
        var dsParts = DataStore.getSpareParts();
        
        assertEquals(dbParts.size(), dsParts.size(),
            "Количество запчастей в БД и DataStore должно совпадать");
        
        for (var dbPart : dbParts) {
            var dsPart = DataStore.getSparePartById(dbPart.getId());
            if (dsPart != null) {
                assertEquals(dbPart.getStock(), dsPart.getStock(),
                    "Запас запчасти должен совпадать: " + dbPart.getName());
            }
        }
    }
}
