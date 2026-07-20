package com.autoservice.dialogs;

import com.autoservice.*;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PrintOrderDialog {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public static void show(WorkOrder order) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить заказ-наряд");

        String safeId = order.getId().replaceAll("[\\\\/:*?\"<>|]", "_");
        fileChooser.setInitialFileName("Заказ-наряд_" + safeId + ".pdf");

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF файлы", "*.pdf")
        );

        File file = fileChooser.showSaveDialog(new Stage());
        if (file != null) {
            generatePDF(order, file.getAbsolutePath());
        }
    }

    private static void generatePDF(WorkOrder order, String filePath) {
        try {
            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdfDoc = new PdfDocument(writer);

            // Альбомная ориентация A5 для экономии места
            Document doc = new Document(pdfDoc, PageSize.A5.rotate());
            doc.setMargins(15, 15, 15, 15);

            // Шрифты с поддержкой кириллицы через TTF (Arial)
            String windowsFontsPath = "C:/Windows/Fonts/";
            String arialPath = windowsFontsPath + "arial.ttf";
            String arialBoldPath = windowsFontsPath + "arialbd.ttf";
            
            PdfFont regularFont = PdfFontFactory.createFont(arialPath, "Identity-H");
            PdfFont boldFont = PdfFontFactory.createFont(arialBoldPath, "Identity-H");

            // Заголовок
            addHeader(doc, order, regularFont, boldFont);

            // Информация об исполнителе и заказчике
            addExecutorAndClientInfo(doc, order, regularFont, boldFont);

            // Информация об автомобиле
            addCarInfo(doc, order, regularFont, boldFont);

            // Работы
            addServicesTable(doc, order, regularFont, boldFont);

            // Запчасти
            addPartsTable(doc, order, regularFont, boldFont);

            // Итого
            addTotalSection(doc, order, regularFont, boldFont);

            // Подписи
            addSignatures(doc, regularFont);

            doc.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Заказ-наряд сохранён:\n" + filePath, ButtonType.OK);
            alert.showAndWait();

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ошибка при создании PDF:\n" + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    private static void addHeader(Document doc, WorkOrder order, PdfFont regularFont, PdfFont boldFont) {
        String safeId = order.getId().replaceAll("[\\\\/:*?\"<>|]", "_");

        Paragraph header = new Paragraph()
                .add(new Text("ЗАКАЗ-НАРЯД " + safeId + "\n").setFont(boldFont).setFontSize(10))
                .add(new Text("Дата приема: " + LocalDate.now().format(DATE_FORMATTER) + "\n").setFont(regularFont).setFontSize(8))
                .add(new Text("Дата выполнения: " + LocalDate.now().plusDays(3).format(DATE_FORMATTER) + "\n").setFont(regularFont).setFontSize(8))
                .add(new Text("Заказ принял: Администратор\n\n").setFont(regularFont).setFontSize(8))
                .setTextAlignment(TextAlignment.CENTER);
        doc.add(header);
    }

    private static void addExecutorAndClientInfo(Document doc, WorkOrder order, PdfFont regularFont, PdfFont boldFont) {
        Client client = order.getClient();

        // Исполнитель
        Paragraph executorTitle = new Paragraph("ИСПОЛНИТЕЛЬ").setFont(boldFont).setFontSize(9);
        doc.add(executorTitle);

        Paragraph executorInfo = new Paragraph()
                .add(new Text("Автосервис \"GWM MASTER\"\n").setFont(regularFont).setFontSize(7))
                .add(new Text("г. Москва, ул. Автомобильная, 15\n").setFont(regularFont).setFontSize(7))
                .add(new Text("ИНН: 1234567890\n").setFont(regularFont).setFontSize(7))
                .add(new Text("Тел: +7 (495) 123-45-67\n\n").setFont(regularFont).setFontSize(7));
        doc.add(executorInfo);

        // Заказчик
        Paragraph clientTitle = new Paragraph("ЗАКАЗЧИК").setFont(boldFont).setFontSize(9);
        doc.add(clientTitle);

        String fullName = (client.getLastName() != null && !client.getLastName().isEmpty())
                ? client.getLastName() + " " + client.getName()
                : client.getName();
        Paragraph clientInfo = new Paragraph()
                .add(new Text(fullName + "\n").setFont(regularFont).setFontSize(7))
                .add(new Text("Тел: " + client.getPhone() + "\n\n").setFont(regularFont).setFontSize(7));
        doc.add(clientInfo);
    }

    private static void addCarInfo(Document doc, WorkOrder order, PdfFont regularFont, PdfFont boldFont) {
        Client client = order.getClient();

        Table table = new Table(UnitValue.createPercentArray(new float[]{15, 25, 10, 18, 10, 22}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(7);

        // Заголовки
        table.addCell(new Cell().add(new Paragraph("Марка").setFont(boldFont)));
        table.addCell(new Cell().add(new Paragraph(client.getCarModel()).setFont(regularFont)));
        table.addCell(new Cell().add(new Paragraph("Гос. номер").setFont(boldFont)));
        table.addCell(new Cell().add(new Paragraph(client.getCarNumber()).setFont(regularFont)));
        table.addCell(new Cell().add(new Paragraph("VIN").setFont(boldFont)));
        table.addCell(new Cell().add(new Paragraph("—").setFont(regularFont)));

        table.addCell(new Cell().add(new Paragraph("Описание дефектов:").setFont(boldFont)));
        table.addCell(new Cell().add(new Paragraph(order.getServices().isEmpty() ? "—" : order.getServices().get(0)).setFont(regularFont)));
        table.addCell(new Cell().add(new Paragraph("").setFont(regularFont)));
        table.addCell(new Cell().add(new Paragraph("").setFont(regularFont)));
        table.addCell(new Cell().add(new Paragraph("").setFont(boldFont)));
        table.addCell(new Cell().add(new Paragraph("").setFont(regularFont)));

        doc.add(table);
        doc.add(new Paragraph("\n"));
    }

    private static void addServicesTable(Document doc, WorkOrder order, PdfFont regularFont, PdfFont boldFont) {
        Paragraph title = new Paragraph("ВЫПОЛНЕННЫЕ РАБОТЫ").setFont(boldFont).setFontSize(9);
        doc.add(title);

        if (order.getServices().isEmpty()) {
            doc.add(new Paragraph("Нет услуг").setFont(regularFont).setFontSize(7));
        } else {
            Table table = new Table(UnitValue.createPercentArray(new float[]{4, 6, 45, 7, 9, 9, 10}));
            table.setWidth(UnitValue.createPercentValue(100));
            table.setFontSize(7);

            // Заголовки
            table.addCell(new Cell().add(new Paragraph("№").setFont(boldFont)));
            table.addCell(new Cell().add(new Paragraph("Код").setFont(boldFont)));
            table.addCell(new Cell().add(new Paragraph("Наименование работ").setFont(boldFont)));
            table.addCell(new Cell().add(new Paragraph("Кол-во").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            table.addCell(new Cell().add(new Paragraph("Цена, руб").setFont(boldFont)).setTextAlignment(TextAlignment.RIGHT));
            table.addCell(new Cell().add(new Paragraph("Сумма, руб")).setFont(boldFont).setTextAlignment(TextAlignment.RIGHT));
            table.addCell(new Cell().add(new Paragraph("Подпись").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));

            for (int i = 0; i < order.getServices().size(); i++) {
                table.addCell(new Cell().add(new Paragraph(String.valueOf(i + 1)).setFont(regularFont)));
                table.addCell(new Cell().add(new Paragraph("—").setFont(regularFont)));
                table.addCell(new Cell().add(new Paragraph(order.getServices().get(i)).setFont(regularFont)));
                table.addCell(new Cell().add(new Paragraph("1").setFont(regularFont)).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(String.format("%,.0f", order.getServicePrices().get(i))).setFont(regularFont)).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(String.format("%,.0f", order.getServicePrices().get(i))).setFont(regularFont)).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(" ").setFont(regularFont)));
            }

            doc.add(table);
        }

        // Стоимость работ
        double servicesTotal = 0;
        for (Double price : order.getServicePrices()) {
            servicesTotal += price;
        }
        Paragraph servicesSum = new Paragraph("Стоимость работ, руб: " + String.format("%,.2f", servicesTotal))
                .setFont(boldFont).setTextAlignment(TextAlignment.RIGHT);
        doc.add(servicesSum);
        doc.add(new Paragraph("\n"));
    }

    private static void addPartsTable(Document doc, WorkOrder order, PdfFont regularFont, PdfFont boldFont) {
        Paragraph title = new Paragraph("ИСПОЛЬЗОВАННЫЕ ЗАПЧАСТИ И МАТЕРИАЛЫ").setFont(boldFont).setFontSize(9);
        doc.add(title);

        if (order.getSpareParts().isEmpty()) {
            doc.add(new Paragraph("Нет запчастей").setFont(regularFont).setFontSize(7));
        } else {
            Table table = new Table(UnitValue.createPercentArray(new float[]{4, 8, 40, 8, 9, 21}));
            table.setWidth(UnitValue.createPercentValue(100));
            table.setFontSize(7);

            // Заголовки
            table.addCell(new Cell().add(new Paragraph("№").setFont(boldFont)));
            table.addCell(new Cell().add(new Paragraph("Код").setFont(boldFont)));
            table.addCell(new Cell().add(new Paragraph("Наименование материала").setFont(boldFont)));
            table.addCell(new Cell().add(new Paragraph("Количество").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            table.addCell(new Cell().add(new Paragraph("Цена, руб")).setFont(boldFont).setTextAlignment(TextAlignment.RIGHT));
            table.addCell(new Cell().add(new Paragraph("Сумма, руб")).setFont(boldFont).setTextAlignment(TextAlignment.RIGHT));

            for (int i = 0; i < order.getSpareParts().size(); i++) {
                SparePart part = order.getSpareParts().get(i);
                double qty = order.getSparePartQuantities().get(i);
                double price = part.getRetailPrice();
                double sum = price * qty;

                table.addCell(new Cell().add(new Paragraph(String.valueOf(i + 1)).setFont(regularFont)));
                table.addCell(new Cell().add(new Paragraph(part.getPartNumber() != null ? part.getPartNumber() : "—").setFont(regularFont)));
                table.addCell(new Cell().add(new Paragraph(part.getName()).setFont(regularFont)));
                table.addCell(new Cell().add(new Paragraph(((int)qty) + " шт").setFont(regularFont)).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(String.format("%,.0f", price)).setFont(regularFont)).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(String.format("%,.0f", sum)).setFont(regularFont)).setTextAlignment(TextAlignment.RIGHT));
            }

            doc.add(table);
        }

        // Стоимость материалов
        double partsTotal = 0;
        for (int i = 0; i < order.getSpareParts().size(); i++) {
            partsTotal += order.getSpareParts().get(i).getRetailPrice() * order.getSparePartQuantities().get(i);
        }
        Paragraph partsSum = new Paragraph("Стоимость материалов, руб: " + String.format("%,.2f", partsTotal))
                .setFont(boldFont).setTextAlignment(TextAlignment.RIGHT);
        doc.add(partsSum);
        doc.add(new Paragraph("\n"));
    }

    private static void addTotalSection(Document doc, WorkOrder order, PdfFont regularFont, PdfFont boldFont) {
        double total = order.getTotal();

        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(9);

        table.addCell(new Cell().add(new Paragraph("ИТОГО, ЗА РАБОТЫ И МАТЕРИАЛЫ, РУБ:").setFont(boldFont)));
        table.addCell(new Cell().add(new Paragraph(String.format("%,.2f", total)).setFont(boldFont)).setTextAlignment(TextAlignment.RIGHT));

        doc.add(table);
        doc.add(new Paragraph("\n"));
    }

    private static void addSignatures(Document doc, PdfFont regularFont) {
        Paragraph note = new Paragraph("Заказ и замененные дефектные детали (остатки материалов) получил.\n" +
                "Изделие проверено в моем присутствии.\n\n").setFont(regularFont).setFontSize(7);
        doc.add(note);

        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        table.setWidth(UnitValue.createPercentValue(100));
        table.setFontSize(7);

        table.addCell(new Cell().add(new Paragraph("\nДата: ______________\n\nПодпись заказчика: ______________").setFont(regularFont)));
        table.addCell(new Cell().add(new Paragraph("\nДата: ______________\n\nПодпись исполнителя: ______________").setFont(regularFont)));

        doc.add(table);
    }
}
