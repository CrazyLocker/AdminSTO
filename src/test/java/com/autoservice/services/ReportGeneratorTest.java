package com.autoservice.services;

import com.autoservice.WorkOrder;
import com.autoservice.TestTags;
import com.autoservice.model.ReportData;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для ReportGenerator: экспорт отчёта в PDF.
 *
 * <p>Генератор не считает статистику самостоятельно — он рендерит
 * {@link ReportData}, подготовленный {@link StatisticsService}. Поэтому тесты
 * проверяют только рендеринг PDF из готового DTO.
 */
@Tag(TestTags.UNIT)
class ReportGeneratorTest {

    @Test
    void testExportToPdfCreatesFile(@TempDir Path tempDir) throws IOException {
        ReportData data = sampleReportData();

        Path file = tempDir.resolve("report.pdf");
        ReportGenerator.exportToPdf(file.toString(), data);

        assertThat(file).exists();
        byte[] bytes = Files.readAllBytes(file);
        assertThat(bytes.length).isGreaterThan(0);
        assertThat(new String(bytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void testExportToPdfWithoutTopSections(@TempDir Path tempDir) throws IOException {
        ReportData data = emptyReportData();

        Path file = tempDir.resolve("empty-report.pdf");
        ReportGenerator.exportToPdf(file.toString(), data);

        assertThat(file).exists();
        assertThat(Files.size(file)).isGreaterThan(0);
    }

    @Test
    void testExportToPdfWrapsIoException(@TempDir Path tempDir) {
        ReportData data = emptyReportData();

        Path invalidPath = tempDir.resolve("missing-directory/report.pdf");

        assertThatThrownBy(() -> ReportGenerator.exportToPdf(invalidPath.toString(), data))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Ошибка генерации PDF");
    }

    private static ReportData sampleReportData() {
        Map<String, Integer> statusCount = new LinkedHashMap<>();
        statusCount.put(WorkOrder.STATUS_NEW, 1);
        statusCount.put(WorkOrder.STATUS_IN_PROGRESS, 1);
        statusCount.put(WorkOrder.STATUS_CLOSED, 1);

        return new ReportData(
                3,
                2,
                4,
                1,
                4000,
                statusCount,
                Map.of(),
                Map.of("Замена масла", 2),
                Map.of("Масло моторное", 1),
                "06.11.2026 10:00"
        );
    }

    private static ReportData emptyReportData() {
        return new ReportData(
                0,
                0,
                0,
                0,
                0,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                "06.11.2026 10:00"
        );
    }
}
