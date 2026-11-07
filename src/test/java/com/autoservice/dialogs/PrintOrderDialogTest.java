package com.autoservice.dialogs;

import com.autoservice.Client;
import com.autoservice.WorkOrder;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Тесты генерации PDF заказа-наряда без запуска JavaFX-диалогов.
 */
class PrintOrderDialogTest {

    private static final String REGULAR_FONT_RESOURCE = "/fonts/LiberationSans-Regular.ttf";
    private static final String BOLD_FONT_RESOURCE = "/fonts/LiberationSans-Bold.ttf";

    @TempDir
    Path tempDir;

    @Test
    void generatesPdfWithFallbackWhenFontResourcesAreMissing() throws Exception {
        Path pdfPath = tempDir.resolve("order-fallback.pdf");

        assertThatCode(() -> PrintOrderDialog.generatePdf(
                createOrder(), pdfPath, "/fonts/absent-regular.ttf", "/fonts/absent-bold.ttf"))
                .doesNotThrowAnyException();

        assertThat(pdfPath).exists();
        assertThat(Files.size(pdfPath)).isPositive();
        try (PdfDocument pdf = new PdfDocument(new PdfReader(pdfPath.toString()))) {
            assertThat(pdf.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void generatesPdfWithCyrillicTextUsingBundledFonts() throws Exception {
        Path pdfPath = tempDir.resolve("order-bundled.pdf");

        PrintOrderDialog.generatePdf(createOrder(), pdfPath, REGULAR_FONT_RESOURCE, BOLD_FONT_RESOURCE);

        assertThat(pdfPath).exists();
        try (PdfDocument pdf = new PdfDocument(new PdfReader(pdfPath.toString()))) {
            String text = PdfTextExtractor.getTextFromPage(pdf.getPage(1));
            assertThat(text).contains("ЗАКАЗ-НАРЯД", "Замена масла");
        }
    }

    @Test
    void generatesPdfForOrderWithoutClient() throws Exception {
        WorkOrder order = new WorkOrder();
        order.setId("ZAK-NO-CLIENT");
        order.setStatus(WorkOrder.STATUS_NEW);
        order.addService("Замена масла", 1500);
        Path pdfPath = tempDir.resolve("order-no-client.pdf");

        assertThatCode(() -> PrintOrderDialog.generatePdf(
                order, pdfPath, REGULAR_FONT_RESOURCE, BOLD_FONT_RESOURCE))
                .doesNotThrowAnyException();

        assertThat(pdfPath).exists();
        try (PdfDocument pdf = new PdfDocument(new PdfReader(pdfPath.toString()))) {
            String text = PdfTextExtractor.getTextFromPage(pdf.getPage(1));
            assertThat(text).contains("Без клиента", "Замена масла");
        }
    }

    private WorkOrder createOrder() {
        Client client = new Client("Иван", "Петров", "+79001234567", "Haval Jolion", "А123ВС163");
        WorkOrder order = new WorkOrder(client);
        order.setId("ZAK-01/01/24-0001");
        order.setStatus(WorkOrder.STATUS_NEW);
        order.addService("Замена масла", 1500);
        return order;
    }
}
