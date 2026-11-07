package com.autoservice.services;

import com.autoservice.DataStore;
import com.autoservice.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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

    /** Имя рабочей БД; под этим же именем файл хранится внутри zip-архива бэкапа. */
    private static final String DATABASE_FILE_NAME = "autoservice.db";

    private static final String TABLE_STATE_DIR = "config" + File.separator + "table-state";
    private static final String WINDOW_STATE_DIR = "config" + File.separator + "window-state";
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    /**
     * Создать резервную копию базы данных.
     * @return путь к созданному бэкапу или null при ошибке
     */
    public static String createBackup() {
        Path backup = createBackup(Paths.get(DATABASE_FILE_NAME), Paths.get(BACKUP_DIR));
        return backup == null ? null : backup.toString();
    }

    /**
     * Создать zip-архив с указанной базой данных и файлами состояния UI.
     *
     * @param sourceDb файл базы данных, который нужно заархивировать
     * @param backupDir каталог, в который сохраняется архив
     * @return путь к созданному архиву или {@code null} при ошибке
     */
    private static Path createBackup(Path sourceDb, Path backupDir) {
        try {
            String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
            Path backupZip = backupDir.resolve(BACKUP_PREFIX + timestamp + BACKUP_EXTENSION);
            // Имена бэкапов имеют точность до секунды: не перезаписываем уже
            // существующий архив (например, бэкап, который сейчас восстанавливают).
            int suffix = 1;
            while (Files.exists(backupZip)) {
                backupZip = backupDir.resolve(BACKUP_PREFIX + timestamp + "_" + suffix + BACKUP_EXTENSION);
                suffix++;
            }
            
            // Создать директорию backups если её нет
            Files.createDirectories(backupDir);
            
            // Сначала сохранить все данные в базу
            DataStore.save();
            
            if (!Files.exists(sourceDb)) {
                logger.error("База данных не найдена: {}", sourceDb);
                return null;
            }
            
            // Создать zip архив с базой данных
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(backupZip))) {
                // Добавить базу данных
                ZipEntry dbEntry = new ZipEntry(DATABASE_FILE_NAME);
                zos.putNextEntry(dbEntry);
                byte[] bytes = Files.readAllBytes(sourceDb);
                zos.write(bytes);
                zos.closeEntry();
                
                // Добавить файлы настроек таблиц
                Path tableStatePath = Paths.get(TABLE_STATE_DIR);
                if (Files.exists(tableStatePath)) {
                    Files.walk(tableStatePath)
                        .filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json"))
                        .forEach(p -> {
                            try {
                                String entryName = TABLE_STATE_DIR + File.separator + tableStatePath.relativize(p);
                                ZipEntry tableEntry = new ZipEntry(entryName.replace(File.separator, "/"));
                                zos.putNextEntry(tableEntry);
                                zos.write(Files.readAllBytes(p));
                                zos.closeEntry();
                            } catch (IOException e) {
                                logger.warn("Не удалось добавить в бэкап: {}", p, e);
                            }
                        });
                }
                
                // Добавить файлы настроек окон
                Path windowStatePath = Paths.get(WINDOW_STATE_DIR);
                if (Files.exists(windowStatePath)) {
                    Files.walk(windowStatePath)
                        .filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".json"))
                        .forEach(p -> {
                            try {
                                String entryName = WINDOW_STATE_DIR + File.separator + windowStatePath.relativize(p);
                                ZipEntry windowEntry = new ZipEntry(entryName.replace(File.separator, "/"));
                                zos.putNextEntry(windowEntry);
                                zos.write(Files.readAllBytes(p));
                                zos.closeEntry();
                            } catch (IOException e) {
                                logger.warn("Не удалось добавить в бэкап: {}", p, e);
                            }
                        });
                }
            }
            
            logger.info("Резервная копия создана: {}", backupZip);
            
            // Очистить старые бэкапы
            cleanupOldBackups(backupDir);
            
            return backupZip;
            
        } catch (IOException e) {
            logger.error("Ошибка создания резервной копии", e);
            return null;
        }
    }
    
    /**
     * Восстановить базу данных из резервной копии.
     *
     * <p>Порядок действий:
     * <ol>
     *     <li>архив проверяется на целостность до любых изменений рабочей БД;</li>
     *     <li>создаётся резервная копия текущей БД для отката;</li>
     *     <li>БД распаковывается во временный файл рядом с рабочей БД;</li>
     *     <li>пул соединений закрывается, файл БД заменяется атомарно;</li>
     *     <li>БД переинициализируется ({@link Database#init()}), данные
     *         перезагружаются ({@link DataStore#load()});</li>
     *     <li>при любой ошибке после начала замены выполняется откат из
     *         резервной копии, созданной в начале метода.</li>
     * </ol>
     *
     * @param backupPath путь к файлу бэкапа
     * @return true если восстановление успешно, false иначе
     */
    public static boolean restoreBackup(String backupPath) {
        if (backupPath == null || backupPath.isBlank()) {
            logger.error("Не указан путь к файлу резервной копии");
            return false;
        }
        return restoreBackup(Paths.get(backupPath), Paths.get(DATABASE_FILE_NAME), Paths.get(BACKUP_DIR));
    }

    /**
     * Реализация восстановления с явными путями (используется в тестах).
     *
     * @param backupZip архив с бэкапом
     * @param currentDb путь к текущей базе данных
     * @param backupDir каталог для резервной копии, создаваемой перед заменой
     * @return true если восстановление успешно, false иначе
     */
    static boolean restoreBackup(Path backupZip, Path currentDb, Path backupDir) {
        // Шаг 1. Проверка архива до каких-либо изменений БД.
        try {
            validateBackupArchive(backupZip);
        } catch (IOException e) {
            logger.error("Архив повреждён или невалиден, восстановление отменено: {}", backupZip, e);
            return false;
        }

        // Шаг 2. Распаковка БД во временный файл и проверка её содержимого.
        Path stagedDatabase;
        try {
            stagedDatabase = stageDatabaseFile(backupZip, currentDb, true);
        } catch (IOException e) {
            logger.error("Не удалось подготовить базу данных из архива, восстановление отменено: {}", backupZip, e);
            return false;
        }

        Path rollbackBackup = null;
        boolean replacementStarted = false;
        try {
            // Шаг 3. Резервная копия текущей БД для отката.
            if (Files.exists(currentDb)) {
                rollbackBackup = createBackup(currentDb, backupDir);
                if (rollbackBackup == null) {
                    logger.error("Не удалось создать резервную копию текущей БД — восстановление отменено");
                    return false;
                }
            }

            // Шаг 4. Закрыть пул соединений и атомарно заменить файл БД.
            replacementStarted = true;
            Database.close();
            moveAtomically(stagedDatabase, currentDb.toAbsolutePath().normalize());
            stagedDatabase = null;

            // Шаг 5. Восстановить файлы состояния UI (как и в предыдущей версии).
            restoreConfigEntries(backupZip);

            // Шаг 6. Переинициализировать БД и перезагрузить данные.
            Database.init();
            DataStore.load();

            logger.info("Восстановление из бэкапа успешно завершено: {}", backupZip);
            return true;
        } catch (IOException | RuntimeException e) {
            logger.error("Ошибка восстановления из резервной копии", e);
            if (replacementStarted) {
                rollbackRestore(currentDb, rollbackBackup);
            }
            return false;
        } finally {
            deleteQuietly(stagedDatabase);
        }
    }

    /**
     * Проверить целостность zip-архива бэкапа.
     *
     * <p>Архив должен открываться как zip, все записи должны читаться без
     * ошибок CRC, имена записей не должны выходить за пределы каталога
     * восстановления (Zip Slip), а в архиве должна присутствовать запись
     * с базой данных.
     *
     * @param backupZip проверяемый архив
     * @throws IOException если архив отсутствует, повреждён или не содержит БД
     */
    static void validateBackupArchive(Path backupZip) throws IOException {
        if (!Files.exists(backupZip)) {
            throw new IOException("Файл бэкапа не найден: " + backupZip);
        }
        if (!Files.isRegularFile(backupZip)) {
            throw new IOException("Путь к бэкапу не является файлом: " + backupZip);
        }

        boolean containsDatabase = false;
        try (ZipFile zipFile = new ZipFile(backupZip.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                validateEntryName(entry.getName());
                if (entry.isDirectory()) {
                    continue;
                }
                if (DATABASE_FILE_NAME.equals(entry.getName())) {
                    containsDatabase = true;
                }
                // Полное чтение записи проверяет CRC и реальное содержимое архива.
                try (InputStream in = zipFile.getInputStream(entry)) {
                    verifyEntryCrc(entry, in);
                }
            }
        }

        if (!containsDatabase) {
            throw new IOException("В архиве отсутствует файл базы данных: " + DATABASE_FILE_NAME);
        }
    }

    /**
     * Прочитать запись архива целиком и сверить её контрольную сумму с заголовком.
     *
     * <p>{@link ZipFile} сам не проверяет CRC при чтении, поэтому повреждение
     * данных внутри корректно открывающегося zip-архива обнаруживается только
     * явным сравнением.
     *
     * @param entry запись архива
     * @param in поток с содержимым записи
     * @throws IOException если размер или CRC записи не совпадают
     */
    private static void verifyEntryCrc(ZipEntry entry, InputStream in) throws IOException {
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            crc.update(buffer, 0, read);
            total += read;
        }
        if (entry.getSize() >= 0 && total != entry.getSize()) {
            throw new IOException("Размер записи архива не совпадает с заголовком: " + entry.getName());
        }
        if (entry.getCrc() >= 0 && crc.getValue() != entry.getCrc()) {
            throw new IOException("Контрольная сумма записи архива не совпадает: " + entry.getName());
        }
    }

    /**
     * Проверить, что файл является целостной базой данных SQLite.
     *
     * <p>Zip-архив может быть корректным, но содержать чужой или повреждённый
     * файл вместо базы данных. Проверка выполняется до замены рабочей БД.
     *
     * @param databaseFile распакованный файл базы данных
     * @throws IOException если файл не является корректной базой SQLite
     */
    static void verifySqliteDatabase(Path databaseFile) throws IOException {
        String url = "jdbc:sqlite:" + databaseFile.toAbsolutePath();
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA quick_check")) {
            boolean healthy = false;
            while (resultSet.next()) {
                String result = resultSet.getString(1);
                if ("ok".equalsIgnoreCase(result)) {
                    healthy = true;
                    break;
                }
                throw new IOException("Файл базы данных повреждён: " + result);
            }
            if (!healthy) {
                throw new IOException("Файл базы данных не прошёл проверку целостности");
            }
        } catch (SQLException e) {
            throw new IOException("Файл не является корректной базой данных SQLite", e);
        }
    }

    /**
     * Распаковать базу данных из архива во временный файл рядом с целевой БД.
     *
     * <p>Временный файл создаётся в каталоге целевой БД: атомарное перемещение
     * ({@link StandardCopyOption#ATOMIC_MOVE}) возможно только в пределах одной
     * файловой системы, а системный каталог временных файлов может быть на
     * другом разделе.
     *
     * @param backupZip архив с бэкапом
     * @param targetDb путь к рабочей базе данных
     * @param verifyContents проверять ли распакованный файл как базу SQLite
     * @return путь к временному файлу с базой
     * @throws IOException если распаковка или проверка не удались
     */
    private static Path stageDatabaseFile(Path backupZip, Path targetDb, boolean verifyContents) throws IOException {
        Path target = targetDb.toAbsolutePath().normalize();
        Path targetDir = target.getParent();
        if (targetDir == null) {
            throw new IOException("Не удалось определить каталог базы данных: " + targetDb);
        }
        Files.createDirectories(targetDir);

        Path tempFile = Files.createTempFile(targetDir, ".autoservice-db-", ".tmp");
        try (ZipFile zipFile = new ZipFile(backupZip.toFile())) {
            ZipEntry dbEntry = zipFile.getEntry(DATABASE_FILE_NAME);
            if (dbEntry == null) {
                throw new IOException("В архиве отсутствует файл базы данных: " + DATABASE_FILE_NAME);
            }
            try (InputStream in = zipFile.getInputStream(dbEntry)) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            deleteQuietly(tempFile);
            throw e;
        }

        if (verifyContents) {
            try {
                verifySqliteDatabase(tempFile);
            } catch (IOException | RuntimeException e) {
                deleteQuietly(tempFile);
                throw e;
            }
        }

        return tempFile;
    }

    /**
     * Атомарно переместить подготовленный файл на место рабочей БД.
     *
     * @param source подготовленный файл БД
     * @param target путь к рабочей БД
     * @throws IOException если перемещение не удалось
     */
    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            // Часть файловых систем (и ОС) не поддерживает атомарную замену
            // существующего файла. Данные уже полностью записаны, поэтому
            // обычная замена безопаснее, чем отказ от восстановления.
            logger.warn("Атомарное перемещение недоступно ({}), выполняется замена файла: {}",
                    e.getMessage(), target);
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Восстановить из архива все записи, кроме файла базы данных.
     *
     * <p>Файл БД заменяется отдельно и атомарно, поэтому здесь он пропускается.
     *
     * @param backupZip архив с бэкапом
     * @throws IOException если запись не удалось извлечь
     */
    private static void restoreConfigEntries(Path backupZip) throws IOException {
        Path extractionRoot = Paths.get(".").toAbsolutePath().normalize();
        try (ZipFile zipFile = new ZipFile(backupZip.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entry.isDirectory() || DATABASE_FILE_NAME.equals(entryName)) {
                    continue;
                }
                validateEntryName(entryName);
                Path entryPath = extractionRoot.resolve(entryName).normalize();
                if (!entryPath.startsWith(extractionRoot)) {
                    throw new IOException("Запись архива выходит за пределы каталога восстановления: " + entryName);
                }
                Files.createDirectories(entryPath.getParent());
                try (InputStream in = zipFile.getInputStream(entry)) {
                    Files.copy(in, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * Проверить имя записи архива на path traversal и абсолютные пути (Zip Slip).
     *
     * @param entryName имя записи в архиве
     * @throws IOException если имя небезопасно
     */
    private static void validateEntryName(String entryName) throws IOException {
        if (entryName == null || entryName.isEmpty()) {
            throw new IOException("Пустое имя записи в архиве бэкапа");
        }
        // 1. Запрет перехода по родительским каталогам
        if (entryName.contains("..")) {
            throw new IOException("Недопустимая запись архива (path traversal): " + entryName);
        }
        // 2. Запрет абсолютных путей
        if (entryName.startsWith("/") || entryName.startsWith("\\")) {
            throw new IOException("Недопустимая запись архива (абсолютный путь): " + entryName);
        }
        // 3. Запрет путей с буквой диска Windows (например, "C:\\" или "C:/")
        if (entryName.matches("^[A-Za-z]:.*") || entryName.contains(":/")) {
            throw new IOException("Недопустимая запись архива (путь с диском): " + entryName);
        }
    }

    /**
     * Откатить восстановление: вернуть БД из резервной копии и переинициализировать пул.
     *
     * @param currentDb путь к рабочей БД
     * @param rollbackBackup архив с копией БД, созданной перед заменой
     */
    private static void rollbackRestore(Path currentDb, Path rollbackBackup) {
        try {
            Database.close();
        } catch (RuntimeException e) {
            logger.warn("Не удалось закрыть пул соединений перед откатом", e);
        }

        boolean restored = true;
        if (rollbackBackup != null) {
            try {
                replaceDatabaseFromArchive(rollbackBackup, currentDb, false);
                logger.info("База данных восстановлена из резервной копии: {}", rollbackBackup);
            } catch (IOException | RuntimeException e) {
                restored = false;
                logger.error("Не удалось восстановить БД из резервной копии: {}", rollbackBackup, e);
            }
        }

        try {
            Database.init();
            DataStore.load();
        } catch (RuntimeException e) {
            restored = false;
            logger.error("Не удалось переинициализировать базу данных после отката", e);
        }

        if (!restored) {
            logger.error("Откат после неудачного восстановления выполнен не полностью — "
                    + "требуется ручная проверка базы данных");
        }
    }

    /**
     * Заменить файл БД, извлекая его из архива.
     *
     * @param backupZip архив с базой данных
     * @param targetDb путь к рабочей БД
     * @param verifyContents проверять ли распакованный файл как базу SQLite
     * @throws IOException если замена не удалась
     */
    private static void replaceDatabaseFromArchive(Path backupZip, Path targetDb, boolean verifyContents)
            throws IOException {
        Path stagedDatabase = stageDatabaseFile(backupZip, targetDb, verifyContents);
        boolean moved = false;
        try {
            moveAtomically(stagedDatabase, targetDb.toAbsolutePath().normalize());
            moved = true;
        } finally {
            if (!moved) {
                deleteQuietly(stagedDatabase);
            }
        }
    }

    /**
     * Удалить временный файл, не прерывая восстановление из-за ошибки удаления.
     *
     * @param file файл для удаления; {@code null} игнорируется
     */
    private static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            logger.warn("Не удалось удалить временный файл: {}", file, e);
        }
    }
    
    /**
     * Удалить старые резервные копии, оставить только MAX_BACKUPS последних.
     *
     * @param backupDir каталог с резервными копиями
     */
    private static void cleanupOldBackups(Path backupDir) {
        try {
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
        return Files.exists(Paths.get(DATABASE_FILE_NAME));
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
