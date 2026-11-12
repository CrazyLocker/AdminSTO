package com.autoservice.services;

import com.autoservice.Client;
import com.autoservice.SparePart;
import com.autoservice.Service;
import com.autoservice.builders.ClientBuilder;
import com.autoservice.builders.ServiceBuilder;
import com.autoservice.builders.SparePartBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Тесты XML-экспорта запчастей, услуг и клиентов.
 */
class ExportServiceTest {

    @TempDir
    Path tempDir;

    // ==================== ЗАПЧАСТИ ====================

    @Test
    void exportSparePartsWritesXmlWithAllFields() throws Exception {
        SparePart part = new SparePartBuilder()
                .withName("Масляный фильтр")
                .withPartNumber("OC-90")
                .withManufacturer("MANN")
                .withCompatibleModels("Haval Jolion")
                .withRetailPrice(750.0)
                .withPurchasePrice(500.0)
                .withStock(12.0)
                .withLocation("A-1")
                .build();

        File file = tempDir.resolve("parts.xml").toFile();
        ExportService.exportToFile(List.of(part), file, "xml");

        String xml = readXml(file);
        assertThat(xml).startsWith("<?xml");
        assertThat(xml).contains("<spareParts>");
        assertThat(xml).contains("<part>");
        assertThat(xml).contains("<name>Масляный фильтр</name>");
        assertThat(xml).contains("<partNumber>OC-90</partNumber>");
        assertThat(xml).contains("<manufacturer>MANN</manufacturer>");
        assertThat(xml).contains("<retailPrice>750.0</retailPrice>");
        assertThat(xml).contains("<purchasePrice>500.0</purchasePrice>");
        assertThat(xml).contains("<stock>12.0</stock>");
        assertThat(xml).contains("<location>A-1</location>");
        assertThat(xml).contains("<compatibleModels>Haval Jolion</compatibleModels>");
    }

    @Test
    void exportSparePartsAcceptsFormatInAnyCase() throws Exception {
        SparePart part = new SparePartBuilder().withName("Свеча").build();
        File file = tempDir.resolve("parts-upper.xml").toFile();

        ExportService.exportToFile(List.of(part), file, "XML");

        assertThat(readXml(file)).contains("<name>Свеча</name>");
    }

    @Test
    void exportSparePartsSkipsEmptyFields() throws Exception {
        SparePart part = new SparePartBuilder()
                .withName("Тормозная колодка")
                .withManufacturer("")
                .build();

        File file = tempDir.resolve("parts-empty-fields.xml").toFile();
        ExportService.exportToFile(List.of(part), file, "xml");

        String xml = readXml(file);
        assertThat(xml).contains("<name>Тормозная колодка</name>");
        assertThat(xml).doesNotContain("<manufacturer>");
        assertThat(xml).doesNotContain("<partNumber>");
        assertThat(xml).doesNotContain("<location>");
    }

    @Test
    void exportEmptySparePartsWritesEmptyRoot() throws Exception {
        File file = tempDir.resolve("parts-empty.xml").toFile();

        ExportService.exportToFile(Collections.emptyList(), file, "xml");

        String xml = readXml(file);
        assertThat(xml).contains("<spareParts");
        assertThat(xml).doesNotContain("<part>");
    }

    @Test
    void exportSparePartsRejectsUnsupportedFormat() {
        File file = tempDir.resolve("parts.csv").toFile();

        assertThatThrownBy(() -> ExportService.exportToFile(List.of(), file, "csv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("csv");
    }

    // ==================== УСЛУГИ ====================

    @Test
    void exportServicesWritesXmlWithAllFields() throws Exception {
        Service service = new ServiceBuilder()
                .withName("Замена масла")
                .withPrice(1500.0)
                .withDuration(60)
                .withPartNumber("SRV-1")
                .build();

        File file = tempDir.resolve("services.xml").toFile();
        ExportService.exportServicesToFile(List.of(service), file, "xml");

        String xml = readXml(file);
        assertThat(xml).contains("<services>");
        assertThat(xml).contains("<service>");
        assertThat(xml).contains("<name>Замена масла</name>");
        assertThat(xml).contains("<duration>60</duration>");
        assertThat(xml).contains("<partNumber>SRV-1</partNumber>");
        assertThat(xml).contains("<price>1500.0</price>");
    }

    @Test
    void exportServicesRejectsUnsupportedFormat() {
        File file = tempDir.resolve("services.xlsx").toFile();

        assertThatThrownBy(() -> ExportService.exportServicesToFile(List.of(), file, "xlsx"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("xlsx");
    }

    // ==================== КЛИЕНТЫ ====================

    @Test
    void exportClientsWritesXmlWithAllFields() throws Exception {
        Client client = new ClientBuilder()
                .withLastName("Петров")
                .withName("Иван")
                .withPhone("+79001234567")
                .withCarModel("Haval Jolion")
                .withCarNumber("А123ВС163")
                .withLastRepairDate("2026-01-15")
                .build();

        File file = tempDir.resolve("clients.xml").toFile();
        ExportService.exportClientsToFile(List.of(client), file, "xml");

        String xml = readXml(file);
        assertThat(xml).contains("<clients>");
        assertThat(xml).contains("<client>");
        assertThat(xml).contains("<lastName>Петров</lastName>");
        assertThat(xml).contains("<name>Иван</name>");
        assertThat(xml).contains("<phone>+79001234567</phone>");
        assertThat(xml).contains("<carModel>Haval Jolion</carModel>");
        assertThat(xml).contains("<carNumber>А123ВС163</carNumber>");
        assertThat(xml).contains("<lastRepairDate>2026-01-15</lastRepairDate>");
    }

    @Test
    void exportClientsRejectsUnsupportedFormat() {
        File file = tempDir.resolve("clients.json").toFile();

        assertThatThrownBy(() -> ExportService.exportClientsToFile(List.of(), file, "json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("json");
    }

    private static String readXml(File file) throws Exception {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }
}
