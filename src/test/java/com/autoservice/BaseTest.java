package com.autoservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

/**
 * Базовый класс для тестов.
 * Инициализирует тестовую базу данных и очищает данные перед каждым тестом.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {

    private static final Logger logger = LoggerFactory.getLogger(BaseTest.class);

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
            // Очищаем все таблицы по отдельности: часть таблиц (settings,
            // to_parts, service_spare_parts) может отсутствовать в тестовой
            // H2-схеме — игнорируем ошибки отсутствующих таблиц.
            for (String table : new String[]{
                    "order_parts", "order_services", "appointments", "orders",
                    "spare_parts", "services", "clients", "settings",
                    "to_parts", "service_spare_parts"
            }) {
                try (var stmt = conn.prepareStatement("DELETE FROM " + validateTableName(table))) {
                    stmt.executeUpdate();
                } catch (SQLException ignored) {
                    // таблица отсутствует в тестовой схеме — это нормально
                }
            }
            DataStore.load(); // Сброс кэша
        } catch (Exception e) {
            logger.error("Ошибка очистки БД: {}", e.getMessage());
        }
    }

    /**
     * Валидация имени таблицы перед использованием в SQL.
     * Разрешён только алфавитно-подчёркивающий формат для защиты от SQL-инъекций.
     */
    private static String validateTableName(String table) {
        if (table == null || !table.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("Недопустимое имя таблицы: " + table);
        }
        return table;
    }

    @AfterEach
    void resetDataStore() {
        // No-op
    }
}
