package com.autoservice;

/**
 * Фабрика для создания и управления экземплярами Database.
 * Используется для внедрения зависимостей и управления состоянием.
 */
public class DatabaseFactory {
    
    private static DatabaseInterface database;
    
    /**
     * Получить текущий экземпляр Database.
     * По умолчанию возвращает SQLiteDatabase для production.
     */
    public static DatabaseInterface getDatabase() {
        if (database == null) {
            database = new SQLiteDatabase();
        }
        return database;
    }
    
    /**
     * Инициализировать базу данных для тестов (используется H2).
     */
    public static void initForTest() {
        if (database != null) {
            database.close();
        }
        database = new H2Database();
        database.initForTest();
    }
    
    /**
     * Инициализировать базу данных для production (используется SQLite).
     */
    public static void init() {
        if (database != null) {
            database.close();
        }
        database = new SQLiteDatabase();
        database.init();
    }
    
    /**
     * Закрыть базу данных и освободить ресурсы.
     */
    public static void close() {
        if (database != null) {
            database.close();
            database = null;
        }
    }
}
