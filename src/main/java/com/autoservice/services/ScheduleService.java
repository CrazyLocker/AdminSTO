package com.autoservice.services;

import com.autoservice.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Сервис для управления автоматическим резервным копированием.
 * Планирует ежедневные бэкапы и управляет настройками.
 */
public class ScheduleService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);
    
    private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    private static boolean backupEnabled = false;
    private static String backupTime = "02:00";
    private static int retentionDays = 14;
    
    private static Runnable backupTask = null;
    
    /**
     * Инициализация планировщика бэкапов.
     * Загружает настройки из базы данных и планирует бэкапы.
     */
    public static void init() {
        loadSettings();
        
        if (backupEnabled) {
            scheduleDailyBackup(backupTime);
        }
        
        logger.info("ScheduleService инициализирован. Авто-бэкап: {}, время: {}", 
                    backupEnabled, backupTime);
    }
    
    /**
     * Загрузить настройки бэкапа из SettingsService.
     */
    private static void loadSettings() {
        backupEnabled = true; // По умолчанию включено
        backupTime = "02:00";
        retentionDays = 14;
        
        // Загрузить настройки из базы данных если есть
        String enabled = System.getProperty("backup.enabled");
        if (enabled != null) {
            backupEnabled = Boolean.parseBoolean(enabled);
        }
        
        String time = System.getProperty("backup.time");
        if (time != null) {
            backupTime = time;
        }
        
        String retention = System.getProperty("backup.retention");
        if (retention != null) {
            try {
                retentionDays = Integer.parseInt(retention);
            } catch (NumberFormatException e) {
                logger.warn("Неверное значение retentionDays: {}", retention);
            }
        }
    }
    
    /**
     * Сохранить настройки бэкапа.
     */
    public static void saveSettings(boolean enabled, String time, int retention) {
        backupEnabled = enabled;
        backupTime = time;
        retentionDays = retention;
        
        System.setProperty("backup.enabled", String.valueOf(enabled));
        System.setProperty("backup.time", time);
        System.setProperty("backup.retention", String.valueOf(retention));
        
        logger.info("Настройки бэкапа сохранены: enabled={}, time={}, retention={}", 
                    enabled, time, retention);
        
        // Если бэкап был включен и не запланирован - запланировать
        if (enabled && backupTask == null) {
            scheduleDailyBackup(time);
        }
    }
    
    /**
     * Запланировать ежедневный бэкап.
     * @param time время в формате "HH:mm" (например, "02:00")
     */
    public static void scheduleDailyBackup(String time) {
        // Если scheduler был shut down - создать новый
        if (scheduler.isShutdown() || scheduler.isTerminated()) {
            scheduler.shutdownNow();
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }
        
        cancelScheduledBackup();
        
        try {
            LocalTime backupLocalTime = LocalTime.parse(time);
            LocalTime now = LocalTime.now();
            
            // Вычислить задержку до следующего запуска
            long delay;
            if (now.isAfter(backupLocalTime)) {
                // Если время уже прошло сегодня, запланировать на завтра
                delay = TimeUnit.HOURS.toSeconds(24) - 
                        TimeUnit.HOURS.toSeconds(now.getHour()) - 
                        TimeUnit.MINUTES.toSeconds(now.getMinute()) + 
                        TimeUnit.HOURS.toSeconds(backupLocalTime.getHour()) + 
                        TimeUnit.MINUTES.toSeconds(backupLocalTime.getMinute());
            } else {
                // Запланировать сегодня
                delay = TimeUnit.HOURS.toSeconds(backupLocalTime.getHour() - now.getHour()) + 
                        TimeUnit.MINUTES.toSeconds(backupLocalTime.getMinute() - now.getMinute());
            }
            
            // Создать задачу бэкапа
            backupTask = () -> {
                try {
                    logger.info("Начало автоматического бэкапа");
                    String backupPath = BackupService.createBackup();
                    if (backupPath != null) {
                        logger.info("Автоматический бэкап успешно завершен: {}", backupPath);
                    } else {
                        logger.error("Ошибка создания автоматического бэкапа");
                    }
                } catch (Exception e) {
                    logger.error("Ошибка в задаче бэкапа", e);
                }
            };
            
            // Запланировать задачу
            scheduler.scheduleAtFixedRate(backupTask, delay, TimeUnit.DAYS.toSeconds(1), 
                                          TimeUnit.SECONDS);
            
            logger.info("Ежедневный бэкап запланирован на {} (задержка: {} сек)", time, delay);
            
        } catch (Exception e) {
            logger.error("Ошибка планирования бэкапа", e);
        }
    }
    
    /**
     * Отменить запланированный бэкап.
     */
    public static void cancelScheduledBackup() {
        if (backupTask != null) {
            backupTask = null;
            logger.info("Запланированный бэкап отменен");
        }
        // scheduler.shutdownNow() удаляется - он нужен для работы
    }
    
    /**
     * Проверить, запланирован ли бэкап.
     * @return true если бэкап запланирован
     */
    public static boolean isBackupScheduled() {
        return backupTask != null;
    }
    
    /**
     * Получить время запланированного бэкапа.
     * @return время в формате "HH:mm" или null если не запланирован
     */
    public static String getScheduleTime() {
        return backupEnabled ? backupTime : null;
    }
    
    /**
     * Получить текущие настройки бэкапа.
     * @return массив [enabled, time, retention]
     */
    public static Object[] getBackupSettings() {
        return new Object[]{backupEnabled, backupTime, retentionDays};
    }
    
    /**
     * Проверить настройки при запуске приложения.
     * Если авто-бэкап включен и прошло больше 24 часов с последнего,
     * создать бэкап.
     */
    public static void checkAndRunBackupOnStartup() {
        if (!backupEnabled) {
            return;
        }
        
        String lastBackupTime = BackupService.getLastBackupTime();
        if (lastBackupTime == null) {
            // Нет бэкапов - создать первый
            logger.info("Нет резервных копий. Создание первого бэкапа...");
            BackupService.createBackup();
            return;
        }
        
        // Парсить дату из времени бэкапа (yyyyMMdd_HHmmss)
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate lastBackupDate = LocalDate.parse(lastBackupTime.substring(0, 8), formatter);
            LocalDate today = LocalDate.now();
            
            // Если бэкап был сделан сегодня - ничего не делать
            if (lastBackupDate.equals(today) || lastBackupDate.isAfter(today)) {
                logger.info("Бэкап уже был сделан сегодня ({})", lastBackupDate);
                return;
            }
            
            // Проверить разницу в днях
            long daysSinceLastBackup = lastBackupDate.until(today, java.time.temporal.ChronoUnit.DAYS);
            if (daysSinceLastBackup >= 1) {
                logger.info("Последний бэкап был {} дней назад. Создание нового бэкапа...", 
                           daysSinceLastBackup);
                BackupService.createBackup();
            }
            
        } catch (Exception e) {
            logger.error("Ошибка проверки времени последнего бэкапа", e);
        }
    }
    
    /**
     * Остановить планировщик (вызов при закрытии приложения).
     */
    public static void shutdown() {
        cancelScheduledBackup();
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        logger.info("ScheduleService остановлен");
    }
}
