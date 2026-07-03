package com.autoservice;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Базовый класс для тестов.
 * Инициализирует тестовую базу данных и очищает данные перед каждым тестом.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {

    @BeforeAll
    void setupDatabase() {
        DatabaseFactory.initForTest();
    }

    @AfterAll
    void cleanupDatabase() {
        DatabaseFactory.close();
    }

    @BeforeEach
    void clearDatabase() {
        try (Connection conn = DatabaseFactory.getDatabase().getConnection()) {
            var stmt = conn.createStatement();
            stmt.execute("DELETE FROM order_parts");
            stmt.execute("DELETE FROM order_services");
            stmt.execute("DELETE FROM appointments");
            stmt.execute("DELETE FROM orders");
            stmt.execute("DELETE FROM spare_parts");
            stmt.execute("DELETE FROM services");
            stmt.execute("DELETE FROM clients");
            stmt.close();
            DataStore.load(); // Сброс кэша
        } catch (Exception e) {
            System.err.println("Ошибка очистки БД: " + e.getMessage());
        }
    }

    @AfterEach
    void resetDataStore() {
        // No-op
    }
}
