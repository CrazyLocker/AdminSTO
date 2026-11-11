package com.autoservice.dialogs;

import com.autoservice.TestTags;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Тесты для обобщённого диалога экспорта.
 *
 * <p>Проверяется логика, не зависящая от JavaFX: передача данных в функцию
 * экспорта и нормализация расширения файла.</p>
 */
@Tag(TestTags.UNIT)
class EntityExportDialogTest {

    @Test
    void exportItemsPassesDataFileAndFormatToExportAction() throws Exception {
        List<String> items = List.of("client-1", "client-2");
        File file = new File("clients.xml");
        List<String> calls = new ArrayList<>();

        int exported = EntityExportDialog.exportItems(items, file, "xml", (data, target, format) -> {
            assertThat(data).isSameAs(items);
            assertThat(target).isSameAs(file);
            assertThat(format).isEqualTo("xml");
            calls.add("called");
        });

        assertThat(exported).isEqualTo(2);
        assertThat(calls).containsExactly("called");
    }

    @Test
    void exportItemsSkipsExportActionWhenThereIsNoData() throws Exception {
        int exported = EntityExportDialog.<String>exportItems(List.of(), new File("empty.xml"), "xml",
                (data, target, format) -> fail("Для пустого списка экспорт вызываться не должен"));

        assertThat(exported).isZero();
    }

    @Test
    void ensureXmlExtensionAppendsExtensionWhenMissing() {
        File file = EntityExportDialog.ensureXmlExtension(new File("export"));

        assertThat(file.getName()).isEqualTo("export.xml");
    }

    @Test
    void ensureXmlExtensionKeepsExistingExtensionCaseInsensitively() {
        File lowerCase = new File("export.xml");
        File upperCase = new File("EXPORT.XML");

        assertThat(EntityExportDialog.ensureXmlExtension(lowerCase)).isSameAs(lowerCase);
        assertThat(EntityExportDialog.ensureXmlExtension(upperCase)).isSameAs(upperCase);
    }
}
