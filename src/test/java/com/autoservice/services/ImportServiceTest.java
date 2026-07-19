package com.autoservice.services;

import com.autoservice.BaseTest;
import com.autoservice.TestTags;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Тесты для класса ImportService
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.INTEGRATION)
class ImportServiceTest extends BaseTest {

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("import-test");
    }

    @AfterEach
    void tearDown() throws IOException {
        deleteDirectory(tempDir.toFile());
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
            dir.delete();
        }
    }

    @Test
    @Order(1)
    void testDetectFormatCSV() throws IOException {
        File file = createTempFile("test.csv", "name,price\nМасло,1500\n");
        assertThat(ImportService.detectFormat(file)).isEqualTo("csv");
    }

    @Test
    @Order(2)
    void testDetectFormatXML() throws IOException {
        File file = createTempFile("test.xml", "<?xml version=\"1.0\"?><root></root>");
        assertThat(ImportService.detectFormat(file)).isEqualTo("xml");
    }

    @Test
    @Order(3)
    void testDetectFormatJSON() throws IOException {
        File file = createTempFile("test.json", "{\"name\":\"Масло\"}");
        assertThat(ImportService.detectFormat(file)).isEqualTo("json");
    }

    @Test
    @Order(4)
    void testDetectFormatUnsupported() {
        File file = new File("test.txt");
        assertThat(ImportService.detectFormat(file)).isEqualTo("unknown");
    }

    @Test
    @Order(5)
    void testDetectFormatNullFile() {
        assertThat(ImportService.detectFormat(null)).isEqualTo("unknown");
    }

    @Test
    @Order(6)
    void testDetectFormatEmptyName() {
        File file = new File("");
        assertThat(ImportService.detectFormat(file)).isEqualTo("unknown");
    }

    @Test
    @Order(7)
    void testDetectFormatNoExtension() {
        File file = new File("testfile");
        assertThat(ImportService.detectFormat(file)).isEqualTo("unknown");
    }

    @Test
    @Order(8)
    void testImportFromCsvEmptyFile() throws IOException {
        File file = createTempFile("empty.csv", "");
        try (InputStream is = new java.io.FileInputStream(file)) {
            var result = ImportService.importFromCsv(is);
            assertThat(result.getImportedCount()).isEqualTo(0);
        }
    }

    @Test
    @Order(9)
    void testImportFromCsvWithUnicode() throws IOException {
        File file = createTempFile("unicode.csv", "name;partNumber;manufacturer;retailPrice;purchasePrice;stock;location;compatibleModels\nМасло моторное;MO-123;Shell;1500;1200;20;Склад 1;Haval\nФильтр масляный;FL-456;Bosch;500;300;15;Склад 2;Haval\n");
        try (InputStream is = new java.io.FileInputStream(file)) {
            var result = ImportService.importFromCsv(is);
            assertThat(result.getImportedCount()).isEqualTo(2);
        }
    }

    @Test
    @Order(10)
    void testImportFromXmlEmptyFile() throws IOException {
        File file = createTempFile("empty.xml", "<?xml version=\"1.0\"?><root></root>");
        try (InputStream is = new java.io.FileInputStream(file)) {
            var result = ImportService.importFromXml(is);
            assertThat(result.getImportedCount()).isEqualTo(0);
        }
    }

    @Test
    @Order(11)
    void testImportFromXmlWithEncoding() throws IOException {
        File file = createTempFile("utf8.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><part><name>Масло</name></part></root>");
        try (InputStream is = new java.io.FileInputStream(file)) {
            var result = ImportService.importFromXml(is);
            assertThat(result.getImportedCount()).isEqualTo(1);
        }
    }

    @Test
    @Order(12)
    void testImportFromJsonEmptyFile() throws IOException {
        File file = createTempFile("empty.json", "{}");
        try (InputStream is = new java.io.FileInputStream(file)) {
            var result = ImportService.importFromJson(is);
            assertThat(result.getImportedCount()).isEqualTo(0);
        }
    }

    @Test
    @Order(13)
    void testImportFromJsonWithEncoding() throws IOException {
        File file = createTempFile("utf8.json", "{\"parts\":[{\"name\":\"Масло\"},{\"name\":\"Фильтр\"}]}");
        try (InputStream is = new java.io.FileInputStream(file)) {
            var result = ImportService.importFromJson(is);
            assertThat(result.getImportedCount()).isEqualTo(2);
        }
    }

    private File createTempFile(String name, String content) throws IOException {
        File file = new File(tempDir.toFile(), name);
        // ВАЖНО: пишем в UTF-8 явно. FileWriter использует кодировку системы по умолчанию
        // (windows-1251 на Windows), из-за чего XML-декларация encoding="UTF-8"
        // перестаёт соответствовать реальным байтам файла, и парсер падает с
        // "Invalid byte N of N-byte UTF-8 sequence".
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        return file;
    }
}
