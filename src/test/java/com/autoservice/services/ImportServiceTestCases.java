package com.autoservice.services;

import com.autoservice.BaseTest;
import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.builders.ClientBuilder;
import com.autoservice.builders.ServiceBuilder;
import com.autoservice.builders.SparePartBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Тесты импорта из XML: разбор корректных документов, дубликаты, ошибочные записи.
 */
class ImportServiceTestCases extends BaseTest {

    @TempDir
    Path tempDir;

    // ==================== ЗАПЧАСТИ ====================

    @Test
    void importAddsNewSparePart() throws IOException {
        File file = writeXml("parts.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <spareParts>
                  <part>
                    <name>Тормозная колодка</name>
                    <partNumber>BP-77</partNumber>
                    <manufacturer>TRW</manufacturer>
                    <retailPrice>1200</retailPrice>
                    <purchasePrice>800</purchasePrice>
                    <stock>5</stock>
                    <location>B-2</location>
                    <unitType>компл</unitType>
                  </part>
                </spareParts>
                """);

        ImportService.ImportResult result = ImportService.importFromFile(file);

        assertThat(result.getImportedCount()).isEqualTo(1);
        assertThat(result.getSkippedCount()).isZero();
        assertThat(result.hasErrors()).isFalse();

        SparePart imported = findPart("BP-77");
        assertThat(imported.getName()).isEqualTo("Тормозная колодка");
        assertThat(imported.getManufacturer()).isEqualTo("TRW");
        assertThat(imported.getRetailPrice()).isEqualTo(1200.0);
        assertThat(imported.getPurchasePrice()).isEqualTo(800.0);
        assertThat(imported.getStock()).isEqualTo(5.0);
        assertThat(imported.getLocation()).isEqualTo("B-2");
        assertThat(imported.getUnitType()).isEqualTo("компл");
    }

    @Test
    void repeatedImportMergesStockInsteadOfDuplicating() throws IOException {
        File file = writeXml("parts-twice.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <spareParts>
                  <part>
                    <name>Фильтр</name>
                    <partNumber>F-1</partNumber>
                    <retailPrice>500</retailPrice>
                    <stock>3</stock>
                  </part>
                </spareParts>
                """);
        ImportService.importFromFile(file);

        ImportService.ImportResult second = ImportService.importFromFile(file);

        assertThat(second.getImportedCount()).isZero();
        assertThat(second.getSkippedCount()).isEqualTo(1);
        assertThat(DataStore.getSpareParts()).filteredOn(p -> "F-1".equals(p.getPartNumber())).hasSize(1);
        assertThat(findPart("F-1").getStock()).isEqualTo(6.0);
    }

    @Test
    void importSkipsPartWithoutNameAndReportsError() throws IOException {
        File file = writeXml("parts-noname.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <spareParts>
                  <part>
                    <partNumber>NO-NAME</partNumber>
                    <stock>1</stock>
                  </part>
                </spareParts>
                """);

        ImportService.ImportResult result = ImportService.importFromFile(file);

        assertThat(result.getImportedCount()).isZero();
        assertThat(result.getSkippedCount()).isEqualTo(1);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("пустое наименование"));
    }

    @Test
    void importParsesCommaDecimalSeparatorAndInvalidNumbers() throws IOException {
        File file = writeXml("parts-numbers.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <spareParts>
                  <part>
                    <name>Свеча</name>
                    <partNumber>SP-9</partNumber>
                    <retailPrice>120,5</retailPrice>
                    <purchasePrice>сто</purchasePrice>
                    <stock>много</stock>
                  </part>
                </spareParts>
                """);

        ImportService.importFromFile(file);

        SparePart imported = findPart("SP-9");
        assertThat(imported.getRetailPrice()).isEqualTo(120.5);
        assertThat(imported.getPurchasePrice()).isZero();
        assertThat(imported.getStock()).isZero();
    }

    @Test
    void importFromXmlReportsMalformedDocument() throws IOException {
        File file = writeXml("parts-broken.xml", "<spareParts><part><name>обрыв");

        ImportService.ImportResult result = ImportService.importFromFile(file);

        assertThat(result.getImportedCount()).isZero();
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).anyMatch(e -> e.startsWith("Ошибка чтения XML"));
    }

    @Test
    void exportedSparePartsCanBeImportedBack() throws Exception {
        SparePart part = new SparePartBuilder()
                .withName("Масляный фильтр")
                .withPartNumber("OC-90")
                .withManufacturer("MANN")
                .withRetailPrice(750.0)
                .withPurchasePrice(500.0)
                .withLocation("A-1")
                .build();
        File file = tempDir.resolve("round-trip-parts.xml").toFile();
        ExportService.exportToFile(List.of(part), file, "xml");

        ImportService.ImportResult result = ImportService.importFromFile(file);

        assertThat(result.getImportedCount()).isEqualTo(1);
        SparePart imported = findPart("OC-90");
        assertThat(imported.getName()).isEqualTo("Масляный фильтр");
        assertThat(imported.getManufacturer()).isEqualTo("MANN");
        assertThat(imported.getRetailPrice()).isEqualTo(750.0);
        assertThat(imported.getPurchasePrice()).isEqualTo(500.0);
        assertThat(imported.getLocation()).isEqualTo("A-1");
    }

    // ==================== КЛИЕНТЫ ====================

    @Test
    void importAddsNewClient() throws IOException {
        File file = writeXml("clients.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <clients>
                  <client>
                    <lastName>Петров</lastName>
                    <name>Иван</name>
                    <phone>+79001112233</phone>
                    <carModel>Haval Jolion</carModel>
                    <carNumber>А123ВС163</carNumber>
                    <lastRepairDate>2026-02-01</lastRepairDate>
                  </client>
                </clients>
                """);

        ImportService.ImportResult result = ImportService.importClientsFromFile(file);

        assertThat(result.getImportedCount()).isEqualTo(1);
        Client imported = DataStore.getClients().get(0);
        assertThat(imported.getLastName()).isEqualTo("Петров");
        assertThat(imported.getName()).isEqualTo("Иван");
        assertThat(imported.getPhone()).isEqualTo("+79001112233");
        assertThat(imported.getCarModel()).isEqualTo("Haval Jolion");
        assertThat(imported.getCarNumber()).isEqualTo("А123ВС163");
        assertThat(imported.getLastRepairDate()).isEqualTo("2026-02-01");
    }

    @Test
    void duplicateClientByPhoneIsSkipped() throws IOException {
        DataStore.addClient(new ClientBuilder()
                .withName("Иван")
                .withLastName("Петров")
                .withPhone("+79001112233")
                .build());
        DataStore.load();
        File file = writeXml("clients-dup.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <clients>
                  <client>
                    <name>Иван</name>
                    <lastName>Петров</lastName>
                    <phone>+79001112233</phone>
                  </client>
                </clients>
                """);

        ImportService.ImportResult result = ImportService.importClientsFromFile(file);

        assertThat(result.getImportedCount()).isZero();
        assertThat(result.getSkippedCount()).isEqualTo(1);
        assertThat(DataStore.getClients()).hasSize(1);
    }

    @Test
    void clientWithoutPhoneIsImportedEvenIfNamesMatch() throws IOException {
        DataStore.addClient(new ClientBuilder().withName("Иван").withLastName("Петров").build());
        DataStore.load();
        File file = writeXml("clients-nophone.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <clients>
                  <client>
                    <name>Иван</name>
                    <lastName>Петров</lastName>
                  </client>
                </clients>
                """);

        ImportService.ImportResult result = ImportService.importClientsFromFile(file);

        assertThat(result.getImportedCount()).isEqualTo(1);
        assertThat(DataStore.getClients()).hasSize(2);
    }

    @Test
    void clientWithoutNameAndLastNameIsSkipped() throws IOException {
        File file = writeXml("clients-noname.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <clients>
                  <client>
                    <phone>+79005556677</phone>
                  </client>
                </clients>
                """);

        ImportService.ImportResult result = ImportService.importClientsFromFile(file);

        assertThat(result.getSkippedCount()).isEqualTo(1);
        assertThat(result.getErrors()).anyMatch(e -> e.contains("пустое имя/фамилия"));
    }

    @Test
    void exportedClientsCanBeImportedBack() throws Exception {
        Client client = new ClientBuilder()
                .withName("Иван")
                .withLastName("Петров")
                .withPhone("+79001234567")
                .withCarModel("Haval Jolion")
                .withCarNumber("А123ВС163")
                .build();
        File file = tempDir.resolve("round-trip-clients.xml").toFile();
        ExportService.exportClientsToFile(List.of(client), file, "xml");

        ImportService.ImportResult result = ImportService.importClientsFromFile(file);

        assertThat(result.getImportedCount()).isEqualTo(1);
        Client imported = DataStore.getClients().get(0);
        assertThat(imported.getName()).isEqualTo("Иван");
        assertThat(imported.getPhone()).isEqualTo("+79001234567");
    }

    // ==================== УСЛУГИ ====================

    @Test
    void importAddsNewService() throws IOException {
        File file = writeXml("services.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <services>
                  <service>
                    <name>Замена масла</name>
                    <duration>60</duration>
                    <partNumber>SRV-1</partNumber>
                    <price>1500</price>
                  </service>
                </services>
                """);

        ImportService.ImportResult result = ImportService.importServicesFromFile(file);

        assertThat(result.getImportedCount()).isEqualTo(1);
        Service imported = DataStore.getServices().get(0);
        assertThat(imported.getName()).isEqualTo("Замена масла");
        assertThat(imported.getDuration()).isEqualTo(60);
        assertThat(imported.getPrice()).isEqualTo(1500.0);
        assertThat(imported.getPartNumber()).isEqualTo("SRV-1");
    }

    @Test
    void duplicateServiceByNameIsSkipped() throws IOException {
        DataStore.addService(new ServiceBuilder().withName("Диагностика").withPrice(900.0).build());
        DataStore.load();
        File file = writeXml("services-dup.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <services>
                  <service>
                    <name>диагностика</name>
                    <price>900</price>
                  </service>
                </services>
                """);

        ImportService.ImportResult result = ImportService.importServicesFromFile(file);

        assertThat(result.getImportedCount()).isZero();
        assertThat(result.getSkippedCount()).isEqualTo(1);
        assertThat(DataStore.getServices()).hasSize(1);
    }

    @Test
    void serviceWithoutNameIsSkipped() throws IOException {
        File file = writeXml("services-noname.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <services>
                  <service>
                    <price>100</price>
                  </service>
                </services>
                """);

        ImportService.ImportResult result = ImportService.importServicesFromFile(file);

        assertThat(result.getSkippedCount()).isEqualTo(1);
        assertThat(result.getErrors()).anyMatch(e -> e.contains("пустое наименование"));
    }

    // ==================== ФОРМАТЫ И РЕЗУЛЬТАТ ====================

    @Test
    void unsupportedExtensionsAreRejected() throws IOException {
        File csv = tempDir.resolve("data.csv").toFile();
        Files.writeString(csv.toPath(), "a,b", StandardCharsets.UTF_8);

        // detectFormat() возвращает "unknown", поэтому срабатывает ветка default в switch.
        assertThatThrownBy(() -> ImportService.importFromFile(csv))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("формат");
        assertThatThrownBy(() -> ImportService.importClientsFromFile(csv))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("формат");
        assertThatThrownBy(() -> ImportService.importServicesFromFile(csv))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("формат");
    }

    @Test
    void importResultToStringMentionsCounts() throws IOException {
        File file = writeXml("parts-to-string.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <spareParts>
                  <part><name>Фильтр</name><partNumber>T-1</partNumber></part>
                </spareParts>
                """);

        ImportService.ImportResult result = ImportService.importFromFile(file);

        assertThat(result.toString()).contains("1").contains("0");
    }

    private File writeXml(String fileName, String content) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file.toFile();
    }

    private SparePart findPart(String partNumber) {
        Optional<SparePart> found = DataStore.getSpareParts().stream()
                .filter(p -> partNumber.equals(p.getPartNumber()))
                .findFirst();
        assertThat(found).isPresent();
        return found.get();
    }
}
