package com.autoservice.services;

import com.autoservice.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Сервис для управления резервным копированием.
 * Позволяет создавать ручные и автоматические бэкапы базы данных.
 */
public class BackupService {
    
    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);
    
    private static final String BACKUP_DIR = "backups";
    private static final String BACKUP_PREFIX = "backup_";
    private static final String BACKUP_EXTENSION = ".zip";
    private static final int MAX_BACKUPS = 14;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    /**
     * Создать резервную копию базы данных.
     * @return путь к созданному бэкапу или null при ошибке
     */
    public static String createBackup() {
        try {
            String backupFileName = BACKUP_PREFIX + java.time.LocalDateTime.now().format(DATE_FORMATTER) + BACKUP_EXTENSION;
            String backupPath = BACKUP_DIR + File.separator + backupFileName;
            
            // Создать директорию backups если её нет
            Files.createDirectories(Paths.get(BACKUP_DIR));
            
            // Сначала сохранить все данные в базу
            DataStore.save();
            
            // Скопировать базу данных
            Path sourceDb = Paths.get("autoservice.db");
            Path backupZip = Paths.get(backupPath);
            
            if (!Files.exists(sourceDb)) {
                logger.error("База данных не найдена: {}", sourceDb);
                return null;
            }
            
            // Создать zip архив с базой данных
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(backupZip))) {
                // Добавить базу данных
                ZipEntry dbEntry = new ZipEntry(sourceDb.getFileName().toString());
                zos.putNextEntry(dbEntry);
                byte[] bytes = Files.readAllBytes(sourceDb);
                zos.write(bytes);
                zos.closeEntry();
                
                // Добавить файлы настроек таблиц
                String tableStateDir = "config" + File.separator + "table-state";
                Path tableStatePath = Paths.get(tableStateDir);
                if (Files.exists(tableStatePath)) {
                    Files.walk(tableStatePath)
                        .filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json"))
                        .forEach(p -> {
                            try {
                                String entryName = tableStateDir + File.separator + tableStatePath.relativize(p);
                                ZipEntry tableEntry = new ZipEntry(entryName.replace(File.separator, "/"));
                                zos.putNextEntry(tableEntry);
                                zos.write(Files.readAllBytes(p));
                                zos.closeEntry();
                            } catch (IOException e) {
                                logger.warn("Не удалось добавить в бэкап: {}", p, e);
                            }
                        });
                }
            }
            
            logger.info("Резервная копия создана: {}", backupPath);
            
            // Очистить старые бэкапы
            cleanupOldBackups();
            
            return backupPath;
            
        } catch (IOException e) {
            logger.error("Ошибка создания резервной копии", e);
            return null;
        }
    }
    
    /**
     * Восстановить базу данных из резервной копии.
     * @param backupPath путь к файлу бэкапа
     * @return true если восстановление успешно, false иначе
     */
    public static boolean restoreBackup(String backupPath) {
        try {
            Path backupZip = Paths.get(backupPath);
            
            if (!Files.exists(backupZip)) {
                logger.error("Файл бэкапа не найден: {}", backupPath);
                return false;
            }
            
            // Сначала сделать резервную копию текущей базы
            String currentBackup = createBackup();
            if (currentBackup == null) {
                logger.warn("Не удалось создать резервную копию текущей базы перед восстановлением");
            }
            
            // Удалить текущую базу данных
            Path currentDb = Paths.get("autoservice.db");
            if (Files.exists(currentDb)) {
                Files.delete(currentDb);
                logger.info("Удалена текущая база данных");
            }
            
            // Распаковать бэкап
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(backupZip))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path newPath = Paths.get(entry.getName());
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            
            logger.info("Восстановление из бэкапа успешно завершено: {}", backupPath);
            
            // Перезагрузить данные
            DataStore.load();
            
            return true;
            
        } catch (IOException e) {
            logger.error("Ошибка восстановления из резервной копии", e);
            return false;
        }
    }
    
    /**
     * Удалить старые резервные копии, оставить только MAX_BACKUPS последних.
     */
    private static void cleanupOldBackups() {
        try {
            Path backupDir = Paths.get(BACKUP_DIR);
            if (!Files.exists(backupDir)) {
                return;
            }
            
            List<Path> backupFiles = new ArrayList<>();
            Files.walk(backupDir)
                .filter(p -> p.toString().endsWith(BACKUP_EXTENSION))
                .forEach(backupFiles::add);
            
            // Отсортировать по дате (новые в конце)
            backupFiles.sort(Comparator.naturalOrder());
            
            // Удалить старые если их больше MAX_BACKUPS
            while (backupFiles.size() > MAX_BACKUPS) {
                Path fileToDelete = backupFiles.remove(0);
                Files.delete(fileToDelete);
                logger.info("Удалена старая резервная копия: {}", fileToDelete);
            }
            
        } catch (IOException e) {
            logger.error("Ошибка очистки старых бэкапов", e);
        }
    }
    
    /**
     * Получить список всех доступных резервных копий.
     * @return список путей к бэкапам (новые в конце)
     */
    public static List<String> getAvailableBackups() {
        List<String> backups = new ArrayList<>();
        
        try {
            Path backupDir = Paths.get(BACKUP_DIR);
            if (!Files.exists(backupDir)) {
                return backups;
            }
            
            Files.walk(backupDir)
                .filter(p -> p.toString().endsWith(BACKUP_EXTENSION))
                .map(p -> p.toString())
                .sorted()
                .forEach(backups::add);
            
        } catch (IOException e) {
            logger.error("Ошибка получения списка бэкапов", e);
        }
        
        return backups;
    }
    
    /**
     * Проверить наличие базы данных.
     * @return true если база данных существует
     */
    public static boolean hasDatabase() {
        return Files.exists(Paths.get("autoservice.db"));
    }
    
    /**
     * Получить количество доступных резервных копий.
     * @return количество бэкапов
     */
    public static int getBackupCount() {
        return getAvailableBackups().size();
    }
    
    /**
     * Получить время последнего бэкапа.
     * @return дата и время последнего бэкапа или null если нет бэкапов
     */
    public static String getLastBackupTime() {
        List<String> backups = getAvailableBackups();
        if (backups.isEmpty()) {
            return null;
        }
        // Получить последний бэкап (он в конце списка после сортировки)
        String lastBackup = backups.get(backups.size() - 1);
        // Извлечь дату из имени файла: backup_yyyyMMdd_HHmmss.zip
        String fileName = Paths.get(lastBackup).getFileName().toString();
        String timePart = fileName.replace(BACKUP_PREFIX, "").replace(BACKUP_EXTENSION, "");
        return timePart;
    }
    
    /**
     * Получить путь к последнему бэкапу.
     * @return путь к последнему бэкапу или null если нет бэкапов
     */
    public static String getLatestBackupPath() {
        List<String> backups = getAvailableBackups();
        if (backups.isEmpty()) {
            return null;
        }
        return backups.get(backups.size() - 1);
    }
}
