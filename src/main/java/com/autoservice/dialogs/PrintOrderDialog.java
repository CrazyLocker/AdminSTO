package com.autoservice.dialogs;

import com.autoservice.*;
import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
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
    private static PdfFont regularFont;
    private static PdfFont boldFont;

    static {
        try {
            // Используем системный шрифт Arial (поддерживает кириллицу)
            FontProgram fontProgram = FontProgramFactory.createFont("C:/Windows/Fonts/arial.ttf");
            regularFont = PdfFontFactory.createFont(fontProgram, "Identity-H");
            boldFont = PdfFontFactory.createFont(fontProgram, "Identity-H");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

            // Альбомная ориентация
            Document doc = new Document(pdfDoc, PageSize.A4.rotate());
            doc.setMargins(30, 30, 30, 30);

            // Заголовок
            addHeader(doc, order);

            // Информация об исполнителе и заказчике
            addExecutorAndClientInfo(doc, order);

            // Информация об автомобиле
            addCarInfo(doc, order);

            // Работы
            addServicesTable(doc, order);

            // Запчасти
            addPartsTable(doc, order);

            // Итого
            addTotalSection(doc, order);

            // Подписи
            addSignatures(doc);

            doc.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Заказ-наряд сохранён:\n" + filePath, ButtonType.OK);
            alert.showAndWait();

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Ошибка при создании PDF:\n" + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    private static void addHeader(Document doc, WorkOrder order) {
        String safeId = order.getId().replaceAll("[\\\\/:*?\"<>|]", "_");

        Paragraph header = new Paragraph()
                .add(new Text("ЗАКАЗ-НАРЯД " + safeId + "\n").setFont(boldFont).setFontSize(18))
                .add(new Text("Дата приема заказа: " + LocalDate.now().format(DATE_FORMATTER) + "\n").setFont(regularFont))
                .add(new Text("Дата выполнения заказа: " + LocalDate.now().plusDays(3).format(DATE_FORMATTER) + "\n").setFont(regularFont))
                .add(new Text("Заказ принял: Администратор\n\n").setFont(regularFont))
                .setTextAlignment(TextAlignment.CENTER);
        doc.add(header);
    }

    private static void addExecutorAndClientInfo(Document doc, WorkOrder order) {
        Client client = order.getClient();

        // Исполнитель
        Paragraph executorTitle = new Paragraph("ИСПОЛНИТЕЛЬ").setFont(boldFont).setFontSize(12);
        doc.add(executorTitle);

        Paragraph executorInfo = new Paragraph()
                .add(new Text("Автосервис \"GWM MASTER\"\n").setFont(regularFont))
                .add(new Text("г. Москва, ул. Автомобильная, 15\n").setFont(regularFont))
                .add(new Text("ИНН: 1234567890\n").setFont(regularFont))
                .add(new Text("Тел: +7 (495) 123-45-67\n\n").setFont(regularFont));
        doc.add(executorInfo);

        // Заказчик
        Paragraph clientTitle = new Paragraph("ЗАКАЗЧИК").setFont(boldFont).setFontSize(12);
        doc.add(clientTitle);

        String fullName = (client.getLastName() != null && !client.getLastName().isEmpty())
                ? client.getLastName() + " " + client.getName()
                : client.getName();
        Paragraph clientInfo = new Paragraph()
                .add(new Text(fullName + "\n").setFont(regularFont))
                .add(new Text("Тел: " + client.getPhone() + "\n\n").setFont(regularFont));
        doc.add(clientInfo);
    }

    private static void addCarInfo(Document doc, WorkOrder order) {
        Client client = order.getClient();

        Table table = new Table(UnitValue.createPercentArray(new float[]{12, 22, 12, 20, 12, 22}));
        table.setWidth(UnitValue.createPercentValue(100));

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

    private static void addServicesTable(Document doc, WorkOrder order) {
        Paragraph title = new Paragraph("ВЫПОЛНЕННЫЕ РАБОТЫ").setFont(boldFont).setFontSize(12);
        doc.add(title);

        if (order.getServices().isEmpty()) {
            doc.add(new Paragraph("Нет услуг").setFont(regularFont));
        } else {
            Table table = new Table(UnitValue.createPercentArray(new float[]{5, 8, 47, 8, 10, 10, 12}));
            table.setWidth(UnitValue.createPercentValue(100));

            // Заголовки
            table.addCell(new Cell().add(new Paragraph("№").setFont(boldFont)));
            table.addCell(new Cell().add(new Paragraph("Код").setFont(boldFont)));
            table.addCell(new Cell().add(new Paragraph("Наименование работ").setFont(boldFont)));
            table.addCell(new Cell().add(new Paragraph("Кол-во").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            table.addCell(new Cell().add(new Paragraph("Цена, руб").setFont(boldFont)).setTextAlignment(TextAlignment.RIGHT));
            table.addCell(new Cell().add(new Paragraph("Сумма, руб")).setFont(boldFont).setTextAlignment(TextAlignment.RIGHT));
            table.addCell(new Cell().add(new Paragraph("Подпись")).setFont(boldFont).setTextAlignment(TextAlignment.CENTER));

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

    private static void addPartsTable(Document doc, WorkOrder order) {
        Paragraph title = new Paragraph("ИСПОЛЬЗОВАННЫЕ ЗАПЧАСТИ И МАТЕРИАЛЫ").setFont(boldFont).setFontSize(12);
        doc.add(title);

        if (order.getSpareParts().isEmpty()) {
            doc.add(new Paragraph("Нет запчастей").setFont(regularFont));
        } else {
            Table table = new Table(UnitValue.createPercentArray(new float[]{5, 10, 45, 10, 10, 20}));
            table.setWidth(UnitValue.createPercentValue(100));

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

    private static void addTotalSection(Document doc, WorkOrder order) {
        double total = order.getTotal();

        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        table.setWidth(UnitValue.createPercentValue(100));

        table.addCell(new Cell().add(new Paragraph("ИТОГО, ЗА РАБОТЫ И МАТЕРИАЛЫ, РУБ:").setFont(boldFont)));
        table.addCell(new Cell().add(new Paragraph(String.format("%,.2f", total)).setFont(boldFont)).setTextAlignment(TextAlignment.RIGHT));

        doc.add(table);
        doc.add(new Paragraph("\n"));
    }

    private static void addSignatures(Document doc) {
        Paragraph note = new Paragraph("Заказ и замененные дефектные детали (остатки материалов) получил.\n" +
                "Изделие проверено в моем присутствии.\n\n").setFont(regularFont);
        doc.add(note);

        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        table.setWidth(UnitValue.createPercentValue(100));

        table.addCell(new Cell().add(new Paragraph("\nДата: ______________\n\nПодпись заказчика: ______________").setFont(regularFont)));
        table.addCell(new Cell().add(new Paragraph("\nДата: ______________\n\nПодпись исполнителя: ______________").setFont(regularFont)));

        doc.add(table);
    }
}