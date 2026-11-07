package com.autoservice.services;

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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Тесты безопасного восстановления БД из бэкапа (пункт K1 ревью).
 *
 * <p>Проверяют, что повреждённый или невалидный архив отвергается до изменения
 * рабочей БД, а корректная база SQLite принимается проверкой целостности.
 */
@Tag(TestTags.UNIT)
class BackupServiceTest {

    private static final byte[] ORIGINAL_DATABASE =
            "original database content".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path tempDir;

    @Test
    void restoreBackupRejectsNonZipFileAndKeepsDatabase() throws IOException {
        Path database = createExistingDatabase();
        Path corruptedArchive = tempDir.resolve("corrupted.zip");
        Files.writeString(corruptedArchive, "not a zip archive", StandardCharsets.UTF_8);

        boolean restored = BackupService.restoreBackup(corruptedArchive, database, backupDir());

        assertThat(restored).isFalse();
        assertDatabaseUnchanged(database);
    }

    @Test
    void restoreBackupRejectsTruncatedArchiveAndKeepsDatabase() throws IOException {
        Path database = createExistingDatabase();
        Path archive = createArchive("backup.zip", Map.of("autoservice.db", ORIGINAL_DATABASE));
        byte[] archiveBytes = Files.readAllBytes(archive);
        Path truncatedArchive = tempDir.resolve("truncated.zip");
        Files.write(truncatedArchive, java.util.Arrays.copyOf(archiveBytes, archiveBytes.length / 2));

        boolean restored = BackupService.restoreBackup(truncatedArchive, database, backupDir());

        assertThat(restored).isFalse();
        assertDatabaseUnchanged(database);
    }

    @Test
    void restoreBackupRejectsArchiveWithoutDatabaseAndKeepsDatabase() throws IOException {
        Path database = createExistingDatabase();
        Path archive = createArchive("backup.zip",
                Map.of("config/table-state/tables.json", "{}".getBytes(StandardCharsets.UTF_8)));

        boolean restored = BackupService.restoreBackup(archive, database, backupDir());

        assertThat(restored).isFalse();
        assertDatabaseUnchanged(database);
    }

    @Test
    void restoreBackupRejectsArchiveWithInvalidDatabaseAndKeepsDatabase() throws IOException {
        Path database = createExistingDatabase();
        Path archive = createArchive("backup.zip",
                Map.of("autoservice.db", "definitely not a SQLite database".getBytes(StandardCharsets.UTF_8)));

        boolean restored = BackupService.restoreBackup(archive, database, backupDir());

        assertThat(restored).isFalse();
        assertDatabaseUnchanged(database);
        assertNoStagedFilesLeft();
    }

    @Test
    void restoreBackupRejectsArchiveWithPathTraversal() throws IOException {
        Path database = createExistingDatabase();
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("../evil.txt", "evil".getBytes(StandardCharsets.UTF_8));
        entries.put("autoservice.db", ORIGINAL_DATABASE);
        Path archive = createArchive("backup.zip", entries);

        boolean restored = BackupService.restoreBackup(archive, database, backupDir());

        assertThat(restored).isFalse();
        assertDatabaseUnchanged(database);
    }

    @Test
    void validateBackupArchiveAcceptsArchiveWithDatabase() throws IOException {
        Path archive = createArchive("backup.zip", Map.of("autoservice.db", ORIGINAL_DATABASE));

        assertThatCode(() -> BackupService.validateBackupArchive(archive)).doesNotThrowAnyException();
    }

    @Test
    void validateBackupArchiveRejectsCorruptedEntryData() throws IOException {
        Path archive = createArchive("backup.zip", Map.of("autoservice.db", ORIGINAL_DATABASE));
        byte[] archiveBytes = Files.readAllBytes(archive);
        // Первые 30 байт — локальный заголовок zip, далее имя записи и сжатые данные.
        int dataOffset = 30 + "autoservice.db".getBytes(StandardCharsets.UTF_8).length;
        assertThat(archiveBytes.length).isGreaterThan(dataOffset + 1);
        archiveBytes[dataOffset + 1] ^= 0x5A;
        Path corruptedArchive = tempDir.resolve("corrupted-entry.zip");
        Files.write(corruptedArchive, archiveBytes);

        assertThatThrownBy(() -> BackupService.validateBackupArchive(corruptedArchive))
                .isInstanceOf(IOException.class);
    }

    @Test
    void verifySqliteDatabaseAcceptsValidDatabase() throws IOException, SQLException {
        Path database = createValidSqliteDatabase();

        assertThatCode(() -> BackupService.verifySqliteDatabase(database)).doesNotThrowAnyException();
    }

    @Test
    void verifySqliteDatabaseRejectsNonDatabaseFile() throws IOException {
        Path notADatabase = tempDir.resolve("not-a-database.db");
        Files.writeString(notADatabase, "plain text instead of a database", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> BackupService.verifySqliteDatabase(notADatabase))
                .isInstanceOf(IOException.class);
    }

    private Path createExistingDatabase() throws IOException {
        Path database = tempDir.resolve("autoservice.db");
        Files.write(database, ORIGINAL_DATABASE);
        return database;
    }

    private Path backupDir() {
        return tempDir.resolve("backups");
    }

    private Path createArchive(String fileName, Map<String, byte[]> entries) throws IOException {
        Path archive = tempDir.resolve(fileName);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
        return archive;
    }

    private Path createValidSqliteDatabase() throws IOException, SQLException {
        Path database = tempDir.resolve("valid.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE clients (id INTEGER PRIMARY KEY, name TEXT NOT NULL)");
            statement.execute("INSERT INTO clients (name) VALUES ('Тест')");
        }
        return database;
    }

    private void assertDatabaseUnchanged(Path database) throws IOException {
        assertThat(database).exists();
        assertThat(Files.readAllBytes(database)).isEqualTo(ORIGINAL_DATABASE);
    }

    private void assertNoStagedFilesLeft() throws IOException {
        try (Stream<Path> files = Files.list(tempDir)) {
            assertThat(files.map(path -> path.getFileName().toString()))
                    .noneMatch(name -> name.startsWith(".autoservice-db-"));
        }
    }
}
