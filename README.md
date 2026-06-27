package com.autoservice;

import com.autoservice.services.ImportService;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Тесты импорта запчастей
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ImportServiceTest {

    private static Path tempDir;
    private static File csvFile;
    private static File xmlFile;
    private static File jsonFile;

    @BeforeAll
    static void setup() throws IOException {
        tempDir = Files.createTempDirectory("import-test");
        
        // Создаём тестовый CSV файл
        csvFile = new File(tempDir.toFile(), "test_parts.csv");
        String csvContent = "имя;артикул;производитель;цена;закупка;остаток;место;модели\n" +
                "Масло моторное 5W-30;OF-001;Shell;1500;1000;50;Склад А;Haval F7,Haval Jolion\n" +
                "Фильтр масляный;OF-002;Mann;500;300;100;Склад Б;Haval F7\n" +
                "Колодки тормозные;BP-003;Brembo;2000;1500;25;Склад А;Haval Jolion";
        Files.write(csvFile.toPath(), csvContent.getBytes(StandardCharsets.UTF_8));
        
        // Создаём тестовый XML файл
        xmlFile = new File(tempDir.toFile(), "test_parts.xml");
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<spareParts>\n" +
                "  <part>\n" +
                "    <name>Масло трансмиссионное</name>\n" +
                "    <partNumber>TF-001</partNumber>\n" +
                "    <manufacturer>Castrol</manufacturer>\n" +
                "    <retailPrice>2500</retailPrice>\n" +
                "    <purchasePrice>1800</purchasePrice>\n" +
                "    <stock>30</stock>\n" +
                "    <location>Склад В</location>\n" +
                "    <compatibleModels>Haval Dargo</compatibleModels>\n" +
                "  </part>\n" +
                "</spareParts>";
        Files.write(xmlFile.toPath(), xmlContent.getBytes(StandardCharsets.UTF_8));
        
        // Создаём тестовый JSON файл
        jsonFile = new File(tempDir.toFile(), "test_parts.json");
        String jsonContent = "{\n" +
                "  \"spareParts\": [\n" +
                "    {\n" +
                "      \"name\": \"Фильтр воздушный\",\n" +
                "      \"partNumber\": \"AF-001\",\n" +
                "      \"manufacturer\": \"Bosch\",\n" +
                "      \"retailPrice\": 800,\n" +
                "      \"purchasePrice\": 500,\n" +
                "      \"stock\": 60,\n" +
                "      \"location\": \"Склад А\",\n" +
                "      \"compatibleModels\": \"Haval F7,Haval F5\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        Files.write(jsonFile.toPath(), jsonContent.getBytes(StandardCharsets.UTF_8));
    }

    @AfterAll
    static void cleanup() {
        Database.close();
        try {
            new java.io.File("test.db").delete();
        } catch (Exception e) {
            // Ignored
        }
    }

    @BeforeEach
    void clearData() {
        try {
            var stmt = Database.getConnection().createStatement();
            stmt.execute("DELETE FROM spare_parts");
            stmt.execute("DELETE FROM sqlite_sequence");
            stmt.close();
        } catch (Exception e) {
            System.err.println("Cleanup: " + e.getMessage());
        }
        DataStore.load();
    }
    }

    @Test
    @Order(1)
    void testDetectFormat() {
        assertThat(ImportService.detectFormat(csvFile)).isEqualTo("csv");
        assertThat(ImportService.detectFormat(xmlFile)).isEqualTo("xml");
        assertThat(ImportService.detectFormat(jsonFile)).isEqualTo("json");
        
        File txtFile = new File(tempDir.toFile(), "test.txt");
        assertThat(ImportService.detectFormat(txtFile)).isNull();
    }

    @Test
    @Order(2)
    void testImportFromCsv() throws IOException {
        try (InputStream is = Files.newInputStream(csvFile.toPath())) {
            ImportService.ImportResult result = ImportService.importFromCsv(is);
            
            assertThat(result.getImportedCount()).isEqualTo(3);
            assertThat(result.getSkippedCount()).isEqualTo(0);
            assertThat(result.hasErrors()).isFalse();
            assertThat(result.getErrors()).isEmpty();
        }
    }

    @Test
    @Order(3)
    void testImportFromCsvWithErrors() {
        String csvWithErrors = "имя;артикул;производитель;цена;закупка;остаток;место;модели\n" +
                "Масло;OF-001;Shell;1500;1000;50;Склад;Модель\n" +
                ";OF-002;Mann;500;300;100;Склад;Модель\n" +
                "Фильтр;;Mann;500;300;100;Склад;Модель";
        
        try (InputStream is = new ByteArrayInputStream(csvWithErrors.getBytes(StandardCharsets.UTF_8))) {
            ImportService.ImportResult result = ImportService.importFromCsv(is);
            
            assertThat(result.getImportedCount()).isEqualTo(1);
            assertThat(result.getSkippedCount()).isEqualTo(2);
            assertThat(result.hasErrors()).isTrue();
            assertThat(result.getErrors()).hasSize(2);
        } catch (IOException e) {
            fail("IOException: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    void testImportFromXml() throws IOException {
        try (InputStream is = Files.newInputStream(xmlFile.toPath())) {
            ImportService.ImportResult result = ImportService.importFromXml(is);
            
            assertThat(result.getImportedCount()).isEqualTo(1);
            assertThat(result.getSkippedCount()).isEqualTo(0);
            assertThat(result.hasErrors()).isFalse();
        }
    }

    @Test
    @Order(5)
    void testImportFromXmlWithEmptyName() {
        String xmlWithErrors = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<spareParts>\n" +
                "  <part>\n" +
                "    <name></name>\n" +
                "    <partNumber>TF-001</partNumber>\n" +
                "  </part>\n" +
                "  <part>\n" +
                "    <name>Нормальная запчасть</name>\n" +
                "    <partNumber>TF-002</partNumber>\n" +
                "  </part>\n" +
                "</spareParts>";
        
        try (InputStream is = new ByteArrayInputStream(xmlWithErrors.getBytes(StandardCharsets.UTF_8))) {
            ImportService.ImportResult result = ImportService.importFromXml(is);
            
            assertThat(result.getImportedCount()).isEqualTo(1);
            assertThat(result.getSkippedCount()).isEqualTo(1);
            assertThat(result.hasErrors()).isTrue();
        } catch (IOException e) {
            fail("IOException: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    void testImportFromJson() throws IOException {
        try (InputStream is = Files.newInputStream(jsonFile.toPath())) {
            ImportService.ImportResult result = ImportService.importFromJson(is);
            
            assertThat(result.getImportedCount()).isEqualTo(1);
            assertThat(result.getSkippedCount()).isEqualTo(0);
            assertThat(result.hasErrors()).isFalse();
        }
    }

    @Test
    @Order(7)
    void testImportFromJsonWithArray() {
        String jsonArray = "[\n" +
                "  {\n" +
                "    \"name\": \"Запчасть из массива\",\n" +
                "    \"partNumber\": \"AR-001\",\n" +
                "    \"retailPrice\": 1000,\n" +
                "    \"purchasePrice\": 700,\n" +
                "    \"stock\": 10\n" +
                "  }\n" +
                "]";
        
        try (InputStream is = new ByteArrayInputStream(jsonArray.getBytes(StandardCharsets.UTF_8))) {
            ImportService.ImportResult result = ImportService.importFromJson(is);
            
            assertThat(result.getImportedCount()).isEqualTo(1);
            assertThat(result.hasErrors()).isFalse();
        } catch (IOException e) {
            fail("IOException: " + e.getMessage());
        }
    }

    @Test
    @Order(8)
    void testImportFromJsonWithPartsKey() {
        String jsonWithPartsKey = "{\n" +
                "  \"parts\": [\n" +
                "    {\n" +
                "      \"name\": \"Запчасть из parts\",\n" +
                "      \"partNumber\": \"PK-001\",\n" +
                "      \"retailPrice\": 1200\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        
        try (InputStream is = new ByteArrayInputStream(jsonWithPartsKey.getBytes(StandardCharsets.UTF_8))) {
            ImportService.ImportResult result = ImportService.importFromJson(is);
            
            assertThat(result.getImportedCount()).isEqualTo(1);
            assertThat(result.hasErrors()).isFalse();
        } catch (IOException e) {
            fail("IOException: " + e.getMessage());
        }
    }

    @Test
    @Order(9)
    void testImportFromJsonWithInvalidStructure() {
        String invalidJson = "{\n" +
                "  \"invalidKey\": [\n" +
                "    {\"name\": \"Test\"}\n" +
                "  ]\n" +
                "}";
        
        try (InputStream is = new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8))) {
            ImportService.ImportResult result = ImportService.importFromJson(is);
            
            assertThat(result.getImportedCount()).isEqualTo(0);
            assertThat(result.hasErrors()).isTrue();
            assertThat(result.getErrors()).hasSize(1);
        } catch (IOException e) {
            fail("IOException: " + e.getMessage());
        }
    }

    @Test
    @Order(10)
    void testImportFromFileCsv() throws IOException {
        ImportService.ImportResult result = ImportService.importFromFile(csvFile);
        
        assertThat(result.getImportedCount()).isEqualTo(3);
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    @Order(11)
    void testImportFromFileXml() throws IOException {
        ImportService.ImportResult result = ImportService.importFromFile(xmlFile);
        
        assertThat(result.getImportedCount()).isEqualTo(1);
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    @Order(12)
    void testImportFromFileJson() throws IOException {
        ImportService.ImportResult result = ImportService.importFromFile(jsonFile);
        
        assertThat(result.getImportedCount()).isEqualTo(1);
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    @Order(13)
    void testImportFromFileUnsupportedFormat() {
        File txtFile = new File(tempDir.toFile(), "unsupported.txt");
        try {
            Files.writeString(txtFile.toPath(), "test");
            ImportService.importFromFile(txtFile);
            fail("Expected IllegalArgumentException");
        } catch (IOException e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
            assertThat(e.getMessage()).contains("Неподдерживаемый формат");
        }
    }

    @Test
    @Order(14)
    void testImportResultToString() {
        ImportService.ImportResult result = new ImportService.ImportResult(10, 2, java.util.List.of("Error 1"));
        
        String toString = result.toString();
        assertThat(toString).contains("Импортировано: 10");
        assertThat(toString).contains("Пропущено: 2");
    }

    @Test
    @Order(15)
    void testImportResultHasErrors() {
        ImportService.ImportResult resultNoErrors = new ImportService.ImportResult(10, 0, java.util.List.of());
        ImportService.ImportResult resultWithErrors = new ImportService.ImportResult(5, 3, java.util.List.of("Error"));
        
        assertThat(resultNoErrors.hasErrors()).isFalse();
        assertThat(resultWithErrors.hasErrors()).isTrue();
    }
}
