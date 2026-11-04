package com.autoservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Базовый класс для тестов.
 * Инициализирует тестовую базу данных и очищает данные перед каждым тестом.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseTest {

    private static final Logger logger = LoggerFactory.getLogger(BaseTest.class);

    /**
     * Таблицы в порядке «сначала зависимые, потом родительские»:
     * иначе DELETE родителя нарушает FOREIGN KEY (H2 и SQLite проверяют их).
     */
    private static final String[] TABLES_TO_CLEAR = {
            "order_parts",
            "order_services",
            "appointments",
            "orders",
            "service_spare_parts_list_items",
            "service_spare_parts_lists",
            "service_spare_parts",
            "service_parts",
            "to_parts",
            "client_cars",
            "spare_parts",
            "services",
            "clients",
            "app_settings"
    };

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
            for (String table : TABLES_TO_CLEAR) {
                // Часть таблиц может отсутствовать в конкретной схеме тестовой БД —
                // отсутствие таблицы не должно прерывать очистку остальных.
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM " + validateTableName(table))) {
                    stmt.executeUpdate();
                } catch (SQLException ignored) {
                    // таблица отсутствует в тестовой схеме — это нормально
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка очистки БД: {}", e.getMessage());
        }
        // Сброс кэша выполняется в любом случае, чтобы DataStore не хранил
        // данные предыдущего теста даже при частичной очистке.
        DataStore.load();
    }

    @AfterEach
    void resetDataStore() {
        // No-op
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
}
