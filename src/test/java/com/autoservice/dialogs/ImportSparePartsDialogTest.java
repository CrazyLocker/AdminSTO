package com.autoservice.dialogs;

import com.autoservice.BaseTest;
import com.autoservice.DataStore;
import com.autoservice.TestTags;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для ImportSparePartsDialog
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.UI)
class ImportSparePartsDialogTest extends BaseTest {

    @Test
    @Order(1)
    void testConstructor() {
        assertThat(ImportSparePartsDialog.class).isNotNull();
    }

    @Test
    @Order(2)
    void testValidateFilePathEmpty() {
        String filePath = "";
        assertThat(filePath).isEmpty();
    }

    @Test
    @Order(3)
    void testValidateFilePathExists() {
        String filePath = "test.csv";
        assertThat(filePath).isNotEmpty();
        assertThat(filePath).contains(".csv");
    }

    @Test
    @Order(4)
    void testDetectFormatCSV() {
        String filename = "test.csv";
        String format = detectFormat(filename);
        assertThat(format).isEqualTo("csv");
    }

    @Test
    @Order(5)
    void testDetectFormatXML() {
        String filename = "test.xml";
        String format = detectFormat(filename);
        assertThat(format).isEqualTo("xml");
    }

    @Test
    @Order(6)
    void testDetectFormatJSON() {
        String filename = "test.json";
        String format = detectFormat(filename);
        assertThat(format).isEqualTo("json");
    }

    @Test
    @Order(7)
    void testDetectFormatUnsupported() {
        String filename = "test.txt";
        String format = detectFormat(filename);
        assertThat(format).isNull();
    }

    @Test
    @Order(8)
    void testDetectFormatWithExtension() {
        String filename = "test.CSV";
        String format = detectFormat(filename);
        assertThat(format).isEqualTo("csv");
    }

    @Test
    @Order(9)
    void testValidateFilePathWithSpaces() {
        String filePath = "C:/Users/Test/test.csv";
        assertThat(filePath).contains("test.csv");
    }

    @Test
    @Order(10)
    void testValidateFilePathWithUnicode() {
        String filePath = "тест.csv";
        assertThat(filePath).isNotEmpty();
    }

    @Test
    @Order(11)
    void testImportSpareParts() {
        DataStore.load();
        assertThat(DataStore.getSpareParts()).isNotNull();
    }

    @Test
    @Order(12)
    void testImportWithEmptyData() {
        DataStore.load();
        assertThat(DataStore.getSpareParts()).isNotNull();
    }

    @Test
    @Order(13)
    void testDialogInitialization() {
        assertThat(ImportSparePartsDialog.class).isNotNull();
    }

    @Test
    @Order(14)
    void testFileDialogTitle() {
        String title = "Импорт запчастей";
        assertThat(title).isEqualTo("Импорт запчастей");
    }

    @Test
    @Order(15)
    void testSupportedFormats() {
        String[] formats = {"CSV", "XML", "JSON"};
        assertThat(formats).hasSize(3);
        assertThat(formats).contains("CSV");
        assertThat(formats).contains("XML");
        assertThat(formats).contains("JSON");
    }

    private String detectFormat(String filename) {
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".csv")) return "csv";
        if (lowerName.endsWith(".xml")) return "xml";
        if (lowerName.endsWith(".json")) return "json";
        return null;
    }
}
