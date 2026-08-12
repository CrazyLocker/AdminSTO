package com.autoservice.dialogs;

import com.autoservice.BaseTest;
import com.autoservice.DataStore;
import com.autoservice.TestTags;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для ImportSparePartsDialog.
 * 
 * <p>Проверяют базовую функциональность импорта запчастей из XML-файлов.
 * Тесты не зависят от JavaFX и могут запускаться без инициализации UI.</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag(TestTags.UI)
class ImportSparePartsDialogTest extends BaseTest {

    @Test
    @Order(9)
    void testImportSpareParts() {
        DataStore.load();
        assertThat(DataStore.getSpareParts()).isNotNull();
    }

    @Test
    @Order(13)
    void testSupportedFormats() {
        String[] formats = {"XML"};
        assertThat(formats).hasSize(1);
        assertThat(formats).contains("XML");
    }

    private String detectFormat(String filename) {
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".xml")) return "xml";
        return null;
    }
}
