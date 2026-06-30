package com.autoservice.services;

import com.autoservice.DataStore;
import com.autoservice.WorkOrder;
import com.autoservice.SparePart;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportGenerator {

    public static class ReportData {
        public int totalOrders;
        public int totalClients;
        public int totalSpareParts;
        public int totalAppointments;
        public double totalRevenue;
        public int newOrders;
        public int inProgressOrders;
        public int closedOrders;
        public int cancelledOrders;
        public Map<String, Integer> topServices = new LinkedHashMap<>();
        public Map<String, Integer> topParts = new LinkedHashMap<>();
        public String generatedDate;
        public double averageOrderValue;

        @Override
        public String toString() {
            return "ReportData{" +
                    "totalOrders=" + totalOrders +
                    ", totalClients=" + totalClients +
                    ", totalRevenue=" + totalRevenue +
                    ", newOrders=" + newOrders +
                    ", inProgressOrders=" + inProgressOrders +
                    ", closedOrders=" + closedOrders +
                    ", topServices=" + topServices +
                    ", topParts=" + topParts +
                    '}';
        }
    }

    public static ReportData generateReport() {
        ReportData data = new ReportData();
        // Используем LocalDateTime для получения времени
        data.generatedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        // Общая статистика
        data.totalClients = DataStore.getClients().size();
        data.totalSpareParts = DataStore.getSpareParts().size();
        data.totalAppointments = DataStore.getAppointments().size();

        // Статистика по заказам
        List<WorkOrder> orders = DataStore.getOrders();
        data.totalOrders = orders.size();

        double totalRevenue = 0;
        int newOrders = 0, inProgress = 0, closed = 0, cancelled = 0;

        // Сбор статистики по услугам и запчастям
        Map<String, Integer> serviceCount = new HashMap<>();
        Map<String, Integer> partCount = new HashMap<>();

        for (WorkOrder order : orders) {
            // Статусы
            String status = order.getStatus();
            if (status == null) status = "Новый";

            switch (status) {
                case "Новый" -> newOrders++;
                case "В работе" -> inProgress++;
                case "Закрыт" -> {
                    closed++;
                    totalRevenue += order.getTotal();
                }
                default -> cancelled++;
            }

            // Услуги
            for (String service : order.getServices()) {
                serviceCount.put(service, serviceCount.getOrDefault(service, 0) + 1);
            }

            // Запчасти
            for (SparePart part : order.getSpareParts()) {
                partCount.put(part.getName(), partCount.getOrDefault(part.getName(), 0) + 1);
            }
        }

        data.totalRevenue = totalRevenue;
        data.newOrders = newOrders;
        data.inProgressOrders = inProgress;
        data.closedOrders = closed;
        data.cancelledOrders = cancelled;
        data.averageOrderValue = closed > 0 ? totalRevenue / closed : 0;

        // Топ-5 услуг
        data.topServices = serviceCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        // Топ-5 запчастей
        data.topParts = partCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        return data;
    }

    public static void exportToPdf(String filePath, ReportData data) throws IOException {
        try (PdfWriter writer = new PdfWriter(new FileOutputStream(filePath));
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            // Заголовок
            document.add(new Paragraph("ОТЧЕТ АВТОСЕРВИСА")
                    .setFontSize(22)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("Дата генерации: " + data.generatedDate)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("\n"));

            // ====== ОБЩАЯ СТАТИСТИКА ======
            document.add(new Paragraph("ОБЩАЯ СТАТИСТИКА")
                    .setFontSize(16)
                    .setBold());

            Table statsTable = new Table(UnitValue.createPercentArray(new float[]{40, 60}));
            statsTable.setWidth(UnitValue.createPercentValue(100));

            addStatRow(statsTable, "Всего заказов", String.valueOf(data.totalOrders));
            addStatRow(statsTable, "Всего клиентов", String.valueOf(data.totalClients));
            addStatRow(statsTable, "Всего запчастей", String.valueOf(data.totalSpareParts));
            addStatRow(statsTable, "Всего записей", String.valueOf(data.totalAppointments));
            addStatRow(statsTable, "Общая выручка", String.format("%,.0f руб.", data.totalRevenue));
            addStatRow(statsTable, "Средний чек", String.format("%,.0f руб.", data.averageOrderValue));

            document.add(statsTable);
            document.add(new Paragraph("\n"));

            // ====== СТАТИСТИКА ПО СТАТУСАМ ======
            document.add(new Paragraph("СТАТУСЫ ЗАКАЗОВ")
                    .setFontSize(16)
                    .setBold());

            Table statusTable = new Table(UnitValue.createPercentArray(new float[]{40, 60}));
            statusTable.setWidth(UnitValue.createPercentValue(100));

            addStatRow(statusTable, "Новые", String.valueOf(data.newOrders));
            addStatRow(statusTable, "В работе", String.valueOf(data.inProgressOrders));
            addStatRow(statusTable, "Закрытые", String.valueOf(data.closedOrders));
            addStatRow(statusTable, "Отменённые", String.valueOf(data.cancelledOrders));

            document.add(statusTable);
            document.add(new Paragraph("\n"));

            // ====== ТОП УСЛУГ ======
            if (!data.topServices.isEmpty()) {
                document.add(new Paragraph("ТОП-5 УСЛУГ")
                        .setFontSize(16)
                        .setBold());

                Table serviceTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}));
                serviceTable.setWidth(UnitValue.createPercentValue(100));

                serviceTable.addHeaderCell(new Cell().add(new Paragraph("Услуга").setBold()));
                serviceTable.addHeaderCell(new Cell().add(new Paragraph("Количество").setBold()));

                for (Map.Entry<String, Integer> entry : data.topServices.entrySet()) {
                    serviceTable.addCell(new Cell().add(new Paragraph(entry.getKey())));
                    serviceTable.addCell(new Cell().add(new Paragraph(String.valueOf(entry.getValue()))));
                }

                document.add(serviceTable);
                document.add(new Paragraph("\n"));
            }

            // ====== ТОП ЗАПЧАСТЕЙ ======
            if (!data.topParts.isEmpty()) {
                document.add(new Paragraph("ТОП-5 ЗАПЧАСТЕЙ")
                        .setFontSize(16)
                        .setBold());

                Table partTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}));
                partTable.setWidth(UnitValue.createPercentValue(100));

                partTable.addHeaderCell(new Cell().add(new Paragraph("Запчасть").setBold()));
                partTable.addHeaderCell(new Cell().add(new Paragraph("Количество").setBold()));

                for (Map.Entry<String, Integer> entry : data.topParts.entrySet()) {
                    partTable.addCell(new Cell().add(new Paragraph(entry.getKey())));
                    partTable.addCell(new Cell().add(new Paragraph(String.valueOf(entry.getValue()))));
                }

                document.add(partTable);
            }

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Отчёт сгенерирован автоматически")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));

        } catch (Exception e) {
            throw new IOException("Ошибка генерации PDF: " + e.getMessage(), e);
        }
    }

    private static void addStatRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label)));
        table.addCell(new Cell().add(new Paragraph(value).setBold()));
    }
}