package com.autoservice.services;

import com.autoservice.BaseTest;
import com.autoservice.TestTags;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Тесты жизненного цикла резервных копий: создание, восстановление и очистка.
 *
 * <p>Все каталоги передаются явно и указывают во {@link TempDir}, поэтому тесты
 * не создают архивов в рабочем каталоге приложения (`backups/`) и не заменяют
 * файл рабочей БД: восстановление выполняется с подменёнными операциями с БД
 * ({@link BackupService.RestoreContext}).
 */
@Tag(TestTags.UNIT)
class BackupServiceLifecycleTest extends BaseTest {

    private static final int MAX_BACKUPS = 14;
    private static final String OLD_DATABASE = "old database content";

    @TempDir
    Path tempDir;

    private Path backupDir() {
        return tempDir.resolve("backups");
    }

    private Path extractionRoot() {
        return tempDir.resolve("extract");
    }

    @Test
    void createBackupWritesDatabaseAndStateEntries() throws IOException {
        Path database = createContentFile("source.db", "database payload");
        Path tableState = createStateDir("table-state", "orders.json", "{\"tableId\":\"orders\"}");
        Path windowState = createStateDir("window-state", "main.json", "{\"windowId\":\"main\"}");

        Path archive = BackupService.createBackup(database, backupDir(), tableState, windowState);

        assertThat(archive).isNotNull();
        assertThat(archive).exists();
        assertThat(archive.getFileName().toString()).matches("backup_\\d{8}_\\d{6}\\.zip");
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            assertThat(zip.getEntry("autoservice.db")).isNotNull();
            assertThat(zip.getEntry("config/table-state/orders.json")).isNotNull();
            assertThat(zip.getEntry("config/window-state/main.json")).isNotNull();
            assertThat(readEntry(zip, "autoservice.db")).isEqualTo("database payload");
            assertThat(readEntry(zip, "config/table-state/orders.json")).isEqualTo("{\"tableId\":\"orders\"}");
        }
    }

    @Test
    void createBackupSkipsMissingStateDirectories() throws IOException {
        Path database = createContentFile("source.db", "database payload");

        Path archive = BackupService.createBackup(database, backupDir(), null, null);

        assertThat(archive).isNotNull();
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            assertThat(entryNames(zip)).containsExactly("autoservice.db");
        }
    }

    @Test
    void createBackupReturnsNullWhenDatabaseFileIsMissing() {
        Path missing = tempDir.resolve("missing.db");

        Path archive = BackupService.createBackup(missing, backupDir(), null, null);

        assertThat(archive).isNull();
    }

    @Test
    void nextBackupPathAddsSuffixWhenNameIsTaken() throws IOException {
        LocalDateTime timestamp = LocalDateTime.of(2026, 11, 12, 10, 15, 30);
        Files.createDirectories(backupDir());
        Files.write(backupDir().resolve("backup_20261112_101530.zip"), new byte[0]);
        Files.write(backupDir().resolve("backup_20261112_101530_1.zip"), new byte[0]);

        Path next = BackupService.nextBackupPath(backupDir(), timestamp);

        assertThat(next.getFileName().toString()).isEqualTo("backup_20261112_101530_2.zip");
    }

    @Test
    void cleanupKeepsOnlyLatestBackups() throws IOException {
        Path database = createContentFile("source.db", "database payload");

        for (int i = 0; i < MAX_BACKUPS + 2; i++) {
            assertThat(BackupService.createBackup(database, backupDir(), null, null)).isNotNull();
        }

        List<String> backups = BackupService.getAvailableBackups(backupDir());
        assertThat(backups).hasSize(MAX_BACKUPS);
        assertThat(backups).allMatch(path -> path.endsWith(".zip"));
    }

    @Test
    void restoreBackupReplacesDatabaseAndRestoresStateFiles() throws IOException, SQLException {
        Path currentDb = createContentFile("autoservice.db", OLD_DATABASE);
        byte[] newDatabase = sqliteDatabaseBytes("new-client");
        Path archive = createArchive("backup.zip", newDatabase,
                "config/table-state/orders.json", "{\"tableId\":\"orders\"}");

        boolean restored = BackupService.restoreBackup(archive, currentDb, backupDir(), noOpContext());

        assertThat(restored).isTrue();
        assertThat(Files.readAllBytes(currentDb)).isEqualTo(newDatabase);
        Path restoredState = extractionRoot().resolve("config/table-state/orders.json");
        assertThat(restoredState).exists();
        assertThat(Files.readString(restoredState, StandardCharsets.UTF_8)).contains("orders");
        // Перед заменой создаётся резервная копия прежней БД для отката.
        assertThat(BackupService.getBackupCount(backupDir())).isEqualTo(1);
        assertNoStagedFilesLeft();
    }

    @Test
    void restoreBackupCreatesDatabaseWhenItDidNotExist() throws IOException, SQLException {
        Path currentDb = tempDir.resolve("autoservice.db");
        byte[] newDatabase = sqliteDatabaseBytes("fresh-client");
        Path archive = createArchive("backup.zip", newDatabase, null, null);

        boolean restored = BackupService.restoreBackup(archive, currentDb, backupDir(), noOpContext());

        assertThat(restored).isTrue();
        assertThat(Files.readAllBytes(currentDb)).isEqualTo(newDatabase);
        // Откатывать нечего — резервная копия прежней БД не создаётся.
        assertThat(BackupService.getBackupCount(backupDir())).isZero();
    }

    @Test
    void restoreBackupRollsBackDatabaseWhenReloadFails() throws IOException, SQLException {
        Path currentDb = createContentFile("autoservice.db", OLD_DATABASE);
        Path archive = createArchive("backup.zip", sqliteDatabaseBytes("new-client"), null, null);
        BackupService.RestoreContext failingContext = new BackupService.RestoreContext(
                extractionRoot(),
                () -> { },
                () -> {
                    throw new IllegalStateException("Перезагрузка данных недоступна");
                });

        boolean restored = BackupService.restoreBackup(archive, currentDb, backupDir(), failingContext);

        assertThat(restored).isFalse();
        assertThat(Files.readAllBytes(currentDb)).isEqualTo(OLD_DATABASE.getBytes(StandardCharsets.UTF_8));
        assertNoStagedFilesLeft();
    }

    @Test
    void restoreBackupAbortsWhenRollbackBackupCannotBeCreated() throws IOException, SQLException {
        Path currentDb = createContentFile("autoservice.db", OLD_DATABASE);
        Path archive = createArchive("backup.zip", sqliteDatabaseBytes("new-client"), null, null);
        // Каталог бэкапов занят файлом: резервную копию создать нельзя.
        Path blockedBackupDir = tempDir.resolve("blocked-backups");
        Files.write(blockedBackupDir, "not a directory".getBytes(StandardCharsets.UTF_8));

        boolean restored = BackupService.restoreBackup(archive, currentDb, blockedBackupDir, noOpContext());

        assertThat(restored).isFalse();
        assertThat(Files.readAllBytes(currentDb)).isEqualTo(OLD_DATABASE.getBytes(StandardCharsets.UTF_8));
        assertNoStagedFilesLeft();
    }

    @Test
    void listingHelpersReportBackupsInOrder() throws IOException {
        Files.createDirectories(backupDir());
        Files.write(backupDir().resolve("backup_20260101_120000.zip"), new byte[0]);
        Files.write(backupDir().resolve("backup_20260305_083000.zip"), new byte[0]);

        assertThat(BackupService.getAvailableBackups(backupDir()))
                .extracting(path -> Path.of(path).getFileName().toString())
                .containsExactly("backup_20260101_120000.zip", "backup_20260305_083000.zip");
        assertThat(BackupService.getBackupCount(backupDir())).isEqualTo(2);
        assertThat(BackupService.getLastBackupTime(backupDir())).isEqualTo("20260305_083000");
        assertThat(BackupService.getLatestBackupPath(backupDir()))
                .endsWith("backup_20260305_083000.zip");
    }

    @Test
    void listingHelpersReturnEmptyResultForMissingDirectory() {
        Path missing = tempDir.resolve("no-backups");

        assertThat(BackupService.getAvailableBackups(missing)).isEmpty();
        assertThat(BackupService.getBackupCount(missing)).isZero();
        assertThat(BackupService.getLastBackupTime(missing)).isNull();
        assertThat(BackupService.getLatestBackupPath(missing)).isNull();
        assertThatCode(() -> BackupService.getAvailableBackups(missing)).doesNotThrowAnyException();
    }

    private BackupService.RestoreContext noOpContext() {
        return new BackupService.RestoreContext(extractionRoot(), () -> { }, () -> { });
    }

    private Path createContentFile(String name, String content) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private Path createStateDir(String name, String fileName, String content) throws IOException {
        Path dir = tempDir.resolve(name);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(fileName), content, StandardCharsets.UTF_8);
        return dir;
    }

    private Path createArchive(String name, byte[] database, String extraEntry, String extraContent)
            throws IOException {
        Path archive = tempDir.resolve(name);
        try (java.util.zip.ZipOutputStream zos =
                     new java.util.zip.ZipOutputStream(Files.newOutputStream(archive))) {
            zos.putNextEntry(new ZipEntry("autoservice.db"));
            zos.write(database);
            zos.closeEntry();
            if (extraEntry != null) {
                zos.putNextEntry(new ZipEntry(extraEntry));
                zos.write(extraContent.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return archive;
    }

    private byte[] sqliteDatabaseBytes(String clientName) throws IOException, SQLException {
        Path database = tempDir.resolve("archive-" + clientName + ".db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE clients (id INTEGER PRIMARY KEY, name TEXT NOT NULL)");
            try (java.sql.PreparedStatement insert =
                         connection.prepareStatement("INSERT INTO clients (name) VALUES (?)")) {
                insert.setString(1, clientName);
                insert.executeUpdate();
            }
        }
        return Files.readAllBytes(database);
    }

    private static String readEntry(ZipFile zip, String name) throws IOException {
        try (java.io.InputStream in = zip.getInputStream(zip.getEntry(name))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> entryNames(ZipFile zip) {
        return zip.stream().map(ZipEntry::getName).toList();
    }

    private void assertNoStagedFilesLeft() throws IOException {
        try (java.util.stream.Stream<Path> files = Files.list(tempDir)) {
            assertThat(files.map(path -> path.getFileName().toString()))
                    .noneMatch(name -> name.startsWith(".autoservice-db-"));
        }
    }
}
