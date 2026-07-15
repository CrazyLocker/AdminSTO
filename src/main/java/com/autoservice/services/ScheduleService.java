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
     * Загрузить настройки бэкапа из SettingService.
     */
    private static void loadSettings() {
        // Загружаем из SettingService (который читает из базы данных)
        backupEnabled = SettingService.isAutoBackupEnabled();
        backupTime = SettingService.getBackupTime();
        retentionDays = SettingService.getBackupRetention();
        
        logger.debug("Настройки бэкапа загружены: enabled={}, time={}, retention={}", 
                    backupEnabled, backupTime, retentionDays);
    }
    
    /**
     * Сохранить настройки бэкапа в базу данных через SettingService.
     */
    public static void saveSettings(boolean enabled, String time, int retention) {
        backupEnabled = enabled;
        backupTime = time;
        retentionDays = retention;
        
        // Сохраняем в SettingService для постоянного хранения в базе данных
        SettingService.saveBackupSettings(enabled, time, retention);
        
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
                logger.info("Запуск автоматического бэкапа...");
                String backupPath = BackupService.createBackup();
                if (backupPath != null) {
                    logger.info("Автоматический бэкап успешно завершен: {}", backupPath);
                } else {
                    logger.error("Автоматический бэкап завершился с ошибкой");
                }
            };
            
            scheduler.scheduleAtFixedRate(backupTask, delay, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
            
            logger.debug("Бэкап запланирован на {} (первая запланированная выполнение через {} сек)", 
                        time, delay);
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
            logger.debug("Запланированный бэкап отменен");
        }
    }
    
    /**
     * Получить текущее состояние авто-бэкапа.
     * @return true если авто-бэкап включен
     */
    public static boolean isBackupEnabled() {
        return backupEnabled;
    }
    
    /**
     * Получить время запланированного бэкапа.
     * @return время в формате "HH:mm"
     */
    public static String getBackupTime() {
        return backupTime;
    }
    
    /**
     * Получить количество сохраняемых бэкапов.
     * @return количество дней хранения
     */
    public static int getRetentionDays() {
        return retentionDays;
    }
    
    /**
     * Остановить планировщик (для тестов или закрытия приложения).
     */
    public static void shutdown() {
        cancelScheduledBackup();
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
    
    /**
     * Проверить и выполнить бэкап при запуске приложения (если время наступило).
     * Используется для выполнения бэкапа сразу при старте, если текущее время совпадает с запланированным.
     */
    public static void checkAndRunBackupOnStartup() {
        if (!backupEnabled) {
            logger.debug("Авто-бэкап не включен, пропуск проверки");
            return;
        }
        
        try {
            LocalTime backupLocalTime = LocalTime.parse(backupTime);
            LocalTime now = LocalTime.now();
            
            // Проверить, что текущее время близко к запланированному (в пределах 5 минут)
            long minutesDiff = Math.abs(now.getHour() * 60 + now.getMinute() - 
                                       backupLocalTime.getHour() * 60 - backupLocalTime.getMinute());
            
            if (minutesDiff <= 5) {
                logger.info("Текущее время совпадает с запланированным бэкапом ({}, отклонение {} мин), запуск...", 
                            backupTime, minutesDiff);
                String backupPath = BackupService.createBackup();
                if (backupPath != null) {
                    logger.info("Бэкап при запуске успешно завершен: {}", backupPath);
                } else {
                    logger.error("Бэкап при запуске завершился с ошибкой");
                }
            } else {
                logger.debug("Текущее время {} не совпадает с запланированным {}, следующий бэкап в {}", 
                            now.format(DateTimeFormatter.ofPattern("HH:mm")), 
                            backupTime, 
                            backupTime);
            }
        } catch (Exception e) {
            logger.error("Ошибка проверки времени для бэкапа при запуске", e);
        }
    }
}
