package com.autoservice.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Централизованное управление логированием.
 * Предоставляет логгеры для всех классов приложения.
 */
public class LoggerManager {
    
    private static final String LOG_FILE = "logs/autoservice.log";
    
    /**
     * Получить логгер для указанного класса.
     * @param clazz класс для получения логгера
     * @return экземпляр Logger
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
    
    /**
     * Получить логгер для указанного имени.
     * @param name имя логгера
     * @return экземпляр Logger
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }
    
    /**
     * Инициализация логгирования.
     * Настройка местоположения файла логов.
     */
    public static void init() {
        System.setProperty("org.slf4j.simpleLogger.logFile", LOG_FILE);
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "dd.MM.yyyy HH:mm:ss");
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.showLogName", "true");
        System.setProperty("org.slf4j.simpleLogger.logNameLength", "30");
    }
}
