package com.autoservice.services;

import com.autoservice.Appointment;
import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.SparePart;
import com.autoservice.TestTags;
import com.autoservice.WorkOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для ReportGenerator: сводная статистика и экспорт отчёта в PDF.
 *
 * <p>Данные подготавливаются прямо в кэше {@link DataStore}, поэтому тесты
 * не требуют базы данных. Отдельно проверяется ветка {@code default} в
 * подсчёте статусов, добавленная в PR #15.
 */
@Tag(TestTags.UNIT)
class ReportGeneratorTest {

    @BeforeEach
    void setUp() {
        clearDataStore();
    }

    @AfterEach
    void tearDown() {
        clearDataStore();
    }

    @Test
    void testGenerateReportOnEmptyDataStore() {
        ReportGenerator.ReportData data = ReportGenerator.generateReport();

        assertThat(data.totalOrders).isZero();
        assertThat(data.totalClients).isZero();
        assertThat(data.totalSpareParts).isZero();
        assertThat(data.totalAppointments).isZero();
        assertThat(data.totalRevenue).isZero();
        assertThat(data.newOrders).isZero();
        assertThat(data.inProgressOrders).isZero();
        assertThat(data.closedOrders).isZero();
        assertThat(data.averageOrderValue).isZero();
        assertThat(data.topServices).isEmpty();
        assertThat(data.topParts).isEmpty();
        assertThat(data.generatedDate)
                .matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}");
    }

    @Test
    void testGenerateReportCountsStatusesAndRevenue() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        WorkOrder newOrder = orderWithStatus(client, WorkOrder.STATUS_NEW);
        WorkOrder inProgressOrder = orderWithStatus(client, WorkOrder.STATUS_IN_PROGRESS);
        WorkOrder closedOrder1 = closedOrder(client, 1500);
        WorkOrder closedOrder2 = closedOrder(client, 2500);
        WorkOrder unknownStatusOrder = orderWithStatus(client, "Отменён");
        WorkOrder nullStatusOrder = orderWithStatus(client, null);

        DataStore.getClients().add(client);
        DataStore.getOrders().addAll(List.of(
                newOrder,
                inProgressOrder,
                closedOrder1,
                closedOrder2,
                unknownStatusOrder,
                nullStatusOrder
        ));

        ReportGenerator.ReportData data = ReportGenerator.generateReport();

        assertThat(data.totalOrders).isEqualTo(6);
        assertThat(data.totalClients).isEqualTo(1);
        assertThat(data.newOrders).isEqualTo(2);
        assertThat(data.inProgressOrders).isEqualTo(1);
        assertThat(data.closedOrders).isEqualTo(2);
        assertThat(data.totalRevenue).isEqualTo(4000.0);
        // Неизвестный статус не попадает ни в одну из категорий и не влияет на выручку.
        assertThat(data.newOrders + data.inProgressOrders + data.closedOrders).isEqualTo(5);
        assertThat(data.averageOrderValue).isEqualTo(2000.0);
    }

    @Test
    void testGenerateReportCountsClientsSparePartsAndAppointments() {
        DataStore.getClients().addAll(List.of(
                new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163"),
                new Client("Анна", "Смирнова", "+79002223333", "Haval F7", "В456СЕ163")
        ));
        DataStore.getSpareParts().addAll(List.of(
                new SparePart("Масло моторное", 800, 1200, 20),
                new SparePart("Фильтр масляный", 300, 500, 15),
                new SparePart("Колодки тормозные", 1000, 2000, 5)
        ));
        DataStore.getAppointments().add(new Appointment(
                new Client("Пётр", "Сидоров", "+79112223333", "Haval F7", "С789ЕЕ163"),
                "Саныч",
                "Замена масла",
                "2025-01-01",
                "10:00"
        ));

        ReportGenerator.ReportData data = ReportGenerator.generateReport();

        assertThat(data.totalClients).isEqualTo(2);
        assertThat(data.totalSpareParts).isEqualTo(3);
        assertThat(data.totalAppointments).isEqualTo(1);
    }

    @Test
    void testGenerateReportBuildsTopServicesAndParts() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        SparePart oil = new SparePart("Масло моторное", 800, 1200, 20);
        SparePart filter = new SparePart("Фильтр масляный", 300, 500, 15);

        WorkOrder order1 = new WorkOrder(client);
        order1.addService("Замена масла", 1500);
        order1.addService("Диагностика", 500);
        // Количество запчасти не должно увеличивать счётчик её упоминаний.
        order1.addSparePart(oil, 5);

        WorkOrder order2 = new WorkOrder(client);
        order2.addService("Замена масла", 1500);
        order2.addSparePart(oil, 1);
        order2.addSparePart(filter, 2);

        DataStore.getOrders().addAll(List.of(order1, order2));

        ReportGenerator.ReportData data = ReportGenerator.generateReport();

        assertThat(data.topServices)
                .hasSize(2)
                .containsEntry("Замена масла", 2)
                .containsEntry("Диагностика", 1);
        assertThat(data.topServices.keySet())
                .containsExactly("Замена масла", "Диагностика");

        assertThat(data.topParts)
                .hasSize(2)
                .containsEntry("Масло моторное", 2)
                .containsEntry("Фильтр масляный", 1);
        assertThat(data.topParts.keySet())
                .containsExactly("Масло моторное", "Фильтр масляный");
    }

    @Test
    void testGenerateReportLimitsTopServicesToFive() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");

        for (int i = 1; i <= 6; i++) {
            WorkOrder order = new WorkOrder(client);
            order.addService("Услуга " + i, 100);
            DataStore.getOrders().add(order);
        }

        ReportGenerator.ReportData data = ReportGenerator.generateReport();

        assertThat(data.topServices).hasSize(5);
    }

    @Test
    void testExportToPdfCreatesFile(@TempDir Path tempDir) throws IOException {
        ReportGenerator.ReportData data = new ReportGenerator.ReportData();
        data.generatedDate = "06.11.2026 10:00";
        data.totalOrders = 3;
        data.totalClients = 2;
        data.totalSpareParts = 4;
        data.totalAppointments = 1;
        data.totalRevenue = 4000;
        data.averageOrderValue = 2000;
        data.newOrders = 1;
        data.inProgressOrders = 1;
        data.closedOrders = 1;
        data.topServices.put("Замена масла", 2);
        data.topParts.put("Масло моторное", 1);

        Path file = tempDir.resolve("report.pdf");
        ReportGenerator.exportToPdf(file.toString(), data);

        assertThat(file).exists();
        byte[] bytes = Files.readAllBytes(file);
        assertThat(bytes.length).isGreaterThan(0);
        assertThat(new String(bytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void testExportToPdfWithoutTopSections(@TempDir Path tempDir) throws IOException {
        ReportGenerator.ReportData data = new ReportGenerator.ReportData();
        data.generatedDate = "06.11.2026 10:00";

        Path file = tempDir.resolve("empty-report.pdf");
        ReportGenerator.exportToPdf(file.toString(), data);

        assertThat(file).exists();
        assertThat(Files.size(file)).isGreaterThan(0);
    }

    @Test
    void testExportToPdfWrapsIoException(@TempDir Path tempDir) {
        ReportGenerator.ReportData data = new ReportGenerator.ReportData();
        data.generatedDate = "06.11.2026 10:00";

        Path invalidPath = tempDir.resolve("missing-directory/report.pdf");

        assertThatThrownBy(() -> ReportGenerator.exportToPdf(invalidPath.toString(), data))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Ошибка генерации PDF");
    }

    private static WorkOrder orderWithStatus(Client client, String status) {
        WorkOrder order = new WorkOrder(client);
        order.setStatus(status);
        return order;
    }

    private static WorkOrder closedOrder(Client client, double amount) {
        WorkOrder order = new WorkOrder(client);
        order.addService("Услуга", amount);
        order.setStatus(WorkOrder.STATUS_CLOSED);
        return order;
    }

    private static void clearDataStore() {
        DataStore.getClients().clear();
        DataStore.getOrders().clear();
        DataStore.getSpareParts().clear();
        DataStore.getAppointments().clear();
    }
}
