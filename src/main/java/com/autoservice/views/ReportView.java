package com.autoservice.views;

import com.autoservice.DataStore;
import com.autoservice.WorkOrder;
import com.autoservice.SparePart;
import com.autoservice.services.WindowStateManager;
import com.autoservice.utils.IconHelper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportView {
    private static final Logger logger = LoggerFactory.getLogger(ReportView.class);

    private static final String[] MONTHS = {
            "Янв", "Фев", "Мар", "Апр", "Май", "Июн",
            "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек"
    };

    private static ReportData currentData;
    private static LineChart<String, Number> revenueChart;
    private static VBox root;

    public static void show() {
        Stage stage = new Stage();
        stage.setTitle("Отчёт автосервиса");
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.setMaximized(true);
        stage.setResizable(true);
        
        // Восстановление состояния диалога
        WindowStateManager.getInstance().restoreWindowState("reportDialog", stage);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);

        root = new VBox(20);
        root.setPadding(new Insets(20));

        // ====== ЗАГОЛОВОК ======
        HBox headerBox = new HBox(20);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("ОТЧЁТ АВТОСЕРВИСА");
        titleLabel.setGraphic(IconHelper.report(24, "#2c3e50"));

        Label dateLabel = new Label("Дата генерации: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));

        headerBox.getChildren().addAll(titleLabel, dateLabel);

        // ====== ПЕРИОД ======
        HBox periodBox = new HBox(15);
        periodBox.setAlignment(Pos.CENTER_LEFT);
        periodBox.setPadding(new Insets(10, 0, 10, 0));

        Label periodLabel = new Label("Период:");

        ComboBox<String> periodCombo = new ComboBox<>();
        periodCombo.getItems().addAll("Последние 3 месяца", "Последние 6 месяцев", "Последние 12 месяцев", "Всё время");
        periodCombo.setValue("Последние 6 месяцев");
        periodCombo.setPrefWidth(200);

        periodBox.getChildren().addAll(periodLabel, periodCombo);

        // ====== СБОР ДАННЫХ ======
        currentData = collectData();

        // ====== СТАТИСТИЧЕСКИЕ КАРТОЧКИ ======
        HBox cardsBox = createStatCards(currentData);
        cardsBox.setAlignment(Pos.CENTER);
        cardsBox.setPadding(new Insets(5, 0, 15, 0));
        cardsBox.setSpacing(10);

        // ====== ГРАФИК ВЫРУЧКИ ======
        revenueChart = createRevenueChart(currentData, "Последние 6 месяцев");
        revenueChart.setPrefHeight(300);
        VBox.setVgrow(revenueChart, Priority.ALWAYS);
        HBox.setHgrow(revenueChart, Priority.ALWAYS);

        VBox revenueBox = new VBox(10);
        revenueBox.setAlignment(Pos.CENTER);
        revenueBox.setPadding(new Insets(5));

        Label revenueLabel = new Label("ДИНАМИКА ВЫРУЧКИ");
        revenueLabel.setGraphic(IconHelper.report(20, "#2c3e50"));
        revenueBox.getChildren().addAll(revenueLabel, revenueChart);
        VBox.setVgrow(revenueBox, Priority.ALWAYS);
        HBox.setHgrow(revenueBox, Priority.ALWAYS);

        // ====== ТРИ БЛОКА (ОДИНАКОВАЯ ВЫСОТА) ======
        VBox statusBox = createStatusListBox(currentData);
        VBox topServicesBox = createTopListBox(currentData.topServices, "ТОП-5 УСЛУГ", IconHelper.settings(20, "#2c3e50"));
        VBox topPartsBox = createTopListBox(currentData.topParts, "ТОП-5 ЗАПЧАСТЕЙ", IconHelper.settings(20, "#2c3e50"));

        // Устанавливаем одинаковую минимальную высоту для всех трёх блоков
        double fixedHeight = 280; // Фиксированная высота для выравнивания
        statusBox.setMinHeight(fixedHeight);
        statusBox.setPrefHeight(fixedHeight);
        topServicesBox.setMinHeight(fixedHeight);
        topServicesBox.setPrefHeight(fixedHeight);
        topPartsBox.setMinHeight(fixedHeight);
        topPartsBox.setPrefHeight(fixedHeight);

        // ====== СЕТКА ======
        GridPane chartsGrid = new GridPane();
        chartsGrid.setHgap(20);
        chartsGrid.setVgap(20);
        chartsGrid.setPadding(new Insets(10, 0, 0, 0));

        chartsGrid.add(revenueBox, 0, 0, 3, 1);
        GridPane.setHgrow(revenueBox, Priority.ALWAYS);
        GridPane.setVgrow(revenueBox, Priority.ALWAYS);

        chartsGrid.add(statusBox, 0, 1);
        chartsGrid.add(topServicesBox, 1, 1);
        chartsGrid.add(topPartsBox, 2, 1);

        for (int i = 0; i < 3; i++) {
            GridPane.setHgrow(chartsGrid.getChildren().get(i), Priority.ALWAYS);
            GridPane.setVgrow(chartsGrid.getChildren().get(i), Priority.ALWAYS);
        }

        // ====== ОБРАБОТЧИК ПЕРИОДА ======
        periodCombo.setOnAction(e -> {
            String period = periodCombo.getValue();
            updateReport(period);
        });

        // ====== СЛУШАТЕЛЬ ИЗМЕНЕНИЯ РАЗМЕРА ======

        Scene scene = new Scene(scrollPane);

        root.getChildren().addAll(headerBox, periodBox, cardsBox, chartsGrid);
        scrollPane.setContent(root);

        stage.setScene(scene);
        stage.showAndWait();
        
        stage.setOnHidden(e -> {
            WindowStateManager.getInstance().saveWindowState("reportDialog", stage);
        });
    }

    // ==================== ОБНОВЛЕНИЕ ОТЧЁТА ====================

    private static void updateReport(String period) {
        revenueChart.getData().clear();
        XYChart.Series<String, Number> series = buildRevenueSeries(currentData, period);
        revenueChart.getData().add(series);
    }

    // ==================== СБОР ДАННЫХ ====================

    private static ReportData collectData() {
        ReportData data = new ReportData();
        List<WorkOrder> orders = DataStore.getOrders();

        data.totalOrders = orders.size();
        data.totalClients = DataStore.getClients().size();
        data.totalSpareParts = DataStore.getSpareParts().size();

        for (WorkOrder order : orders) {
            String status = order.getStatus() != null ? order.getStatus() : "Новый";
            data.statusCount.put(status, data.statusCount.getOrDefault(status, 0) + 1);
            if ("Закрыт".equals(status)) {
                data.totalRevenue += order.getTotal();
                data.closedOrders++;
                String dateStr = order.getCreatedDate();
                if (dateStr != null && !dateStr.isEmpty()) {
                    try {
                        String monthKey = dateStr.substring(0, 7);
                        data.monthlyRevenue.put(monthKey, data.monthlyRevenue.getOrDefault(monthKey, 0.0) + order.getTotal());
                    } catch (Exception ex) { logger.error("Error in report view", ex); }
                }
            }
        }
        data.averageOrderValue = data.closedOrders > 0 ? data.totalRevenue / data.closedOrders : 0;

        Map<String, Integer> serviceCount = new HashMap<>();
        for (WorkOrder order : orders) {
            for (String service : order.getServices()) {
                serviceCount.put(service, serviceCount.getOrDefault(service, 0) + 1);
            }
        }
        data.topServices = serviceCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        Map<String, Integer> partCount = new HashMap<>();
        for (WorkOrder order : orders) {
            for (SparePart part : order.getSpareParts()) {
                partCount.put(part.getName(), partCount.getOrDefault(part.getName(), 0) + 1);
            }
        }
        data.topParts = partCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        return data;
    }

    // ==================== КАРТОЧКИ ====================

    private static HBox createStatCards(ReportData data) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(5, 0, 10, 0));

        box.getChildren().addAll(
                createCard(IconHelper.assignment(24, "#3498db"), "Заказов", String.valueOf(data.totalOrders), "#3498db"),
                createCard(IconHelper.people(24, "#2ecc71"), "Клиентов", String.valueOf(data.totalClients), "#2ecc71"),
                createCard(IconHelper.report(24, "#f39c12"), "Выручка", String.format("%,.0f", data.totalRevenue), "#f39c12"),
                createCard(IconHelper.save(24, "#9b59b6"), "Средний чек", String.format("%,.0f", data.averageOrderValue), "#9b59b6")
        );

        return box;
    }

    private static VBox createCard(javafx.scene.Node icon, String title, String value, String color) {
        VBox card = new VBox(2);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(8, 12, 8, 12));

        Label titleLabel = new Label(title);

        Label valueLabel = new Label(value);

        card.getChildren().addAll(icon, titleLabel, valueLabel);
        return card;
    }

    // ==================== ГРАФИК ВЫРУЧКИ ====================

    private static LineChart<String, Number> createRevenueChart(ReportData data, String period) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Выручка, руб.");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(true);
        chart.setAnimated(false);
        chart.setPrefHeight(300);

        XYChart.Series<String, Number> series = buildRevenueSeries(data, period);
        chart.getData().add(series);

        return chart;
    }

    private static XYChart.Series<String, Number> buildRevenueSeries(ReportData data, String period) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Выручка");

        int monthsLimit = switch (period) {
            case "Последние 3 месяца" -> 3;
            case "Последние 12 месяцев" -> 12;
            case "Всё время" -> Integer.MAX_VALUE;
            default -> 6;
        };

        List<Map.Entry<String, Double>> sorted = data.monthlyRevenue.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();

        int start = Math.max(0, sorted.size() - monthsLimit);
        for (int i = start; i < sorted.size(); i++) {
            Map.Entry<String, Double> entry = sorted.get(i);
            String monthKey = entry.getKey();
            String monthName;
            if (monthKey.length() >= 7) {
                try {
                    monthName = MONTHS[Integer.parseInt(monthKey.substring(5, 7)) - 1] + " " + monthKey.substring(0, 4);
                } catch (Exception e) {
                    monthName = monthKey;
                }
            } else {
                monthName = monthKey;
            }
            series.getData().add(new XYChart.Data<>(monthName, entry.getValue()));
        }

        return series;
    }

    // ==================== СТАТУСЫ (СПИСОК) ====================

    private static VBox createStatusListBox(ReportData data) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.TOP_LEFT);

        Label titleLabel = new Label("СТАТУСЫ ЗАКАЗОВ");
        titleLabel.setGraphic(IconHelper.assignment(20, "#2c3e50"));

        VBox listBox = new VBox(5);
        listBox.setPadding(new Insets(8));
        listBox.setFillWidth(true);

        int totalOrders = data.totalOrders;
        for (Map.Entry<String, Integer> entry : data.statusCount.entrySet()) {
            String status = entry.getKey();
            int count = entry.getValue();
            double percent = totalOrders > 0 ? (count * 100.0 / totalOrders) : 0;

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(2, 0, 2, 0));

            Label statusLabel = new Label(status);

            Label countLabel = new Label(String.valueOf(count));

            Label percentLabel = new Label(String.format("(%.1f%%)", percent));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            row.getChildren().addAll(statusLabel, spacer, countLabel, percentLabel);
            listBox.getChildren().add(row);
        }

        box.getChildren().addAll(titleLabel, listBox);
        return box;
    }

    // ==================== ТОП СПИСКИ ====================

    private static VBox createTopListBox(Map<String, Integer> data, String title, javafx.scene.Node icon) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.TOP_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.setGraphic(icon);

        VBox listBox = new VBox(5);
        listBox.setPadding(new Insets(8));
        listBox.setFillWidth(true);

        int rank = 1;
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            String name = entry.getKey();
            int count = entry.getValue();

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(2, 0, 2, 0));

            Label rankLabel = new Label(String.format("%d.", rank));

            Label nameLabel = new Label(name);
            nameLabel.setWrapText(true);

            Label countLabel = new Label(String.valueOf(count));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            row.getChildren().addAll(rankLabel, nameLabel, spacer, countLabel);
            listBox.getChildren().add(row);
            rank++;
        }

        box.getChildren().addAll(titleLabel, listBox);
        return box;
    }

    // ==================== ВНУТРЕННИЙ КЛАСС ====================

    private static class ReportData {
        int totalOrders = 0;
        int totalClients = 0;
        int totalSpareParts = 0;
        double totalRevenue = 0;
        double averageOrderValue = 0;
        int closedOrders = 0;
        Map<String, Integer> statusCount = new LinkedHashMap<>();
        Map<String, Double> monthlyRevenue = new LinkedHashMap<>();
        Map<String, Integer> topServices = new LinkedHashMap<>();
        Map<String, Integer> topParts = new LinkedHashMap<>();
    }
}