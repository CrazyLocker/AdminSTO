package com.autoservice.views;

import com.autoservice.DataStore;
import com.autoservice.WorkOrder;
import com.autoservice.services.StatisticsService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;

public class DashboardView {

    private static LineChart<String, Number> revenueChart;
    private static BarChart<String, Number> mastersChart;
    private static PieChart statusChart;
    private static ListView<String> topServicesList;
    private static ListView<String> topPartsList;
    private static Label activeOrdersLabel;
    private static Label totalClientsLabel;
    private static Label totalOrdersLabel;

    private static Label totalRevenueValue;
    private static Label avgPerDayValue;
    private static Label maxDayValue;
    private static Label maxDayDateValue;
    private static Label ordersCountValue;
    private static Label avgCheckValue;

    public static VBox create() {
        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(15));
        mainContainer.setStyle("-fx-background-color: #f0f2f5;");

        Label titleLabel = new Label("Панель управления");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox statsRow = createStatsCards();

        Accordion accordion = new Accordion();

        TitledPane revenuePane = createRevenuePane();
        TitledPane mastersPane = createMastersLoadPane();
        TitledPane analyticsPane = createAnalyticsPane();

        accordion.getPanes().addAll(revenuePane, mastersPane, analyticsPane);
        accordion.setExpandedPane(revenuePane);

        VBox.setVgrow(accordion, Priority.ALWAYS);

        mainContainer.getChildren().addAll(titleLabel, statsRow, accordion);

        PauseTransition pause = new PauseTransition(Duration.millis(100));
        pause.setOnFinished(e -> refreshAll());
        pause.play();

        return mainContainer;
    }

    private static HBox createStatsCards() {
        HBox cards = new HBox(15);
        cards.setAlignment(Pos.CENTER);
        cards.setPadding(new Insets(0, 0, 15, 0));

        activeOrdersLabel = createStatCard("Активных заказов", String.valueOf(DataStore.getActiveOrdersCount()), "#3498db");
        totalClientsLabel = createStatCard("Всего клиентов", String.valueOf(DataStore.getClients().size()), "#2ecc71");
        totalOrdersLabel = createStatCard("Всего заказов", String.valueOf(DataStore.getOrders().size()), "#9b59b6");

        cards.getChildren().addAll(
                ((VBox) activeOrdersLabel.getParent()),
                ((VBox) totalClientsLabel.getParent()),
                ((VBox) totalOrdersLabel.getParent())
        );

        return cards;
    }

    private static Label createStatCard(String title, String value, String color) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        VBox card = new VBox(5, titleLabel, valueLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-background-radius: 8;");
        card.setPrefWidth(180);

        return valueLabel;
    }

    private static TitledPane createRevenuePane() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(15));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        Label headerLabel = new Label("Выручка");
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox periodBox = new HBox(10);
        periodBox.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup periodGroup = new ToggleGroup();
        RadioButton weekBtn = new RadioButton("Неделя");
        RadioButton monthBtn = new RadioButton("Месяц");
        RadioButton yearBtn = new RadioButton("Год");

        weekBtn.setToggleGroup(periodGroup);
        monthBtn.setToggleGroup(periodGroup);
        yearBtn.setToggleGroup(periodGroup);
        monthBtn.setSelected(true);

        weekBtn.setOnAction(e -> updateRevenueChart(7));
        monthBtn.setOnAction(e -> updateRevenueChart(30));
        yearBtn.setOnAction(e -> updateRevenueChart(365));

        periodBox.getChildren().addAll(new Label("Период:"), weekBtn, monthBtn, yearBtn);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Сумма, руб");

        revenueChart = new LineChart<>(xAxis, yAxis);
        revenueChart.setTitle("Динамика выручки");
        revenueChart.setAnimated(false);
        revenueChart.setPrefHeight(300);
        revenueChart.setCreateSymbols(true);

        VBox financeInfoBox = new VBox(10);
        financeInfoBox.setPadding(new Insets(10));
        financeInfoBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label financeTitle = new Label("Финансовая сводка");
        financeTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");

        GridPane financeGrid = new GridPane();
        financeGrid.setHgap(20);
        financeGrid.setVgap(10);

        Label totalRevenueLabel = new Label("Общая выручка:");
        totalRevenueLabel.setStyle("-fx-font-weight: bold;");
        totalRevenueValue = new Label("0 руб.");
        totalRevenueValue.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");

        Label avgPerDayLabel = new Label("Средняя выручка в день:");
        avgPerDayLabel.setStyle("-fx-font-weight: bold;");
        avgPerDayValue = new Label("0 руб.");

        Label maxDayLabel = new Label("Максимальная выручка:");
        maxDayLabel.setStyle("-fx-font-weight: bold;");
        maxDayValue = new Label("0 руб.");

        maxDayDateValue = new Label("");
        maxDayDateValue.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

        Label ordersCountLabel = new Label("Количество заказов:");
        ordersCountLabel.setStyle("-fx-font-weight: bold;");
        ordersCountValue = new Label("0");

        Label avgCheckLabel = new Label("Средний чек:");
        avgCheckLabel.setStyle("-fx-font-weight: bold;");
        avgCheckValue = new Label("0 руб.");

        financeGrid.add(totalRevenueLabel, 0, 0);
        financeGrid.add(totalRevenueValue, 1, 0);
        financeGrid.add(avgPerDayLabel, 0, 1);
        financeGrid.add(avgPerDayValue, 1, 1);
        financeGrid.add(maxDayLabel, 0, 2);
        financeGrid.add(maxDayValue, 1, 2);
        financeGrid.add(maxDayDateValue, 1, 3);
        financeGrid.add(ordersCountLabel, 0, 4);
        financeGrid.add(ordersCountValue, 1, 4);
        financeGrid.add(avgCheckLabel, 0, 5);
        financeGrid.add(avgCheckValue, 1, 5);

        financeInfoBox.getChildren().addAll(financeTitle, financeGrid);

        content.getChildren().addAll(headerLabel, periodBox, revenueChart, financeInfoBox);

        TitledPane pane = new TitledPane("График выручки", content);
        pane.setAnimated(true);
        return pane;
    }

    private static TitledPane createMastersLoadPane() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(15));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        Label headerLabel = new Label("Загрузка мастеров");
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Количество заказов");

        mastersChart = new BarChart<>(xAxis, yAxis);
        mastersChart.setTitle("Количество заказов по мастерам");
        mastersChart.setAnimated(false);
        mastersChart.setPrefHeight(300);

        content.getChildren().addAll(headerLabel, mastersChart);

        TitledPane pane = new TitledPane("Загрузка мастеров", content);
        pane.setAnimated(true);
        return pane;
    }

    private static TitledPane createAnalyticsPane() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(15));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        Label headerLabel = new Label("Аналитика");
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox analyticsRow = new HBox(15);

        VBox statusBox = new VBox(10);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setPadding(new Insets(10));
        statusBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 8;");
        statusBox.setPrefWidth(300);

        Label statusLabel = new Label("Статусы заказов");
        statusLabel.setStyle("-fx-font-weight: bold;");

        statusChart = new PieChart();
        statusChart.setPrefHeight(250);
        statusChart.setLabelsVisible(true);
        statusChart.setClockwise(true);

        statusBox.getChildren().addAll(statusLabel, statusChart);

        VBox servicesBox = new VBox(10);
        servicesBox.setPadding(new Insets(10));
        servicesBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 8;");
        servicesBox.setPrefWidth(250);

        Label servicesLabel = new Label("Топ услуг");
        servicesLabel.setStyle("-fx-font-weight: bold;");

        topServicesList = new ListView<>();
        topServicesList.setPrefHeight(200);

        servicesBox.getChildren().addAll(servicesLabel, topServicesList);

        VBox partsBox = new VBox(10);
        partsBox.setPadding(new Insets(10));
        partsBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 8;");
        partsBox.setPrefWidth(250);

        Label partsLabel = new Label("Топ запчастей");
        partsLabel.setStyle("-fx-font-weight: bold;");

        topPartsList = new ListView<>();
        topPartsList.setPrefHeight(200);

        partsBox.getChildren().addAll(partsLabel, topPartsList);

        analyticsRow.getChildren().addAll(statusBox, servicesBox, partsBox);
        HBox.setHgrow(statusBox, Priority.ALWAYS);
        HBox.setHgrow(servicesBox, Priority.ALWAYS);
        HBox.setHgrow(partsBox, Priority.ALWAYS);

        content.getChildren().addAll(headerLabel, analyticsRow);

        TitledPane pane = new TitledPane("Аналитика", content);
        pane.setAnimated(true);
        return pane;
    }

    private static void updateRevenueChart(int days) {
        if (revenueChart == null) return;

        revenueChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Выручка");

        var revenue = StatisticsService.getDailyRevenue(days);

        double totalRevenue = 0;
        double maxRevenue = 0;
        String maxRevenueDate = "";
        int daysWithOrders = 0;

        for (var entry : revenue.entrySet()) {
            double value = entry.getValue();
            series.getData().add(new XYChart.Data<>(entry.getKey(), value));
            totalRevenue += value;
            if (value > 0) {
                daysWithOrders++;
            }
            if (value > maxRevenue) {
                maxRevenue = value;
                maxRevenueDate = entry.getKey();
            }
        }

        // Подсчёт количества заказов за период
        LocalDate startDate = LocalDate.now().minusDays(days);
        int orderCount = 0;
        for (WorkOrder order : DataStore.getOrders()) {
            if (order.getStatus().equals(WorkOrder.STATUS_CLOSED)) {
                String createdDate = order.getCreatedDate();
                if (createdDate != null && !createdDate.isEmpty()) {
                    try {
                        String dateStr = createdDate.length() >= 10 ? createdDate.substring(0, 10) : createdDate;
                        LocalDate orderDate = LocalDate.parse(dateStr);
                        if (!orderDate.isBefore(startDate) && !orderDate.isAfter(LocalDate.now())) {
                            orderCount++;
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }

        final double finalTotalRevenue = totalRevenue;
        final double finalAvgPerDay = daysWithOrders > 0 ? totalRevenue / daysWithOrders : 0;
        final double finalMaxRevenue = maxRevenue;
        final String finalMaxRevenueDate = maxRevenueDate;
        final int finalOrderCount = orderCount;
        final double finalAvgCheck = orderCount > 0 ? totalRevenue / orderCount : 0;

        Platform.runLater(() -> {
            totalRevenueValue.setText(String.format("%,.0f руб.", finalTotalRevenue));
            avgPerDayValue.setText(String.format("%,.0f руб.", finalAvgPerDay));
            maxDayValue.setText(String.format("%,.0f руб.", finalMaxRevenue));
            if (finalMaxRevenueDate != null && !finalMaxRevenueDate.isEmpty()) {
                maxDayDateValue.setText("(" + finalMaxRevenueDate + ")");
            }
            ordersCountValue.setText(String.valueOf(finalOrderCount));
            avgCheckValue.setText(String.format("%,.0f руб.", finalAvgCheck));
        });

        revenueChart.getData().add(series);
    }

    private static void updateMastersChart() {
        if (mastersChart == null) return;

        mastersChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Количество заказов");

        var mastersLoad = StatisticsService.getMastersLoad();
        for (var entry : mastersLoad.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        mastersChart.getData().add(series);
    }

    private static void updateStatusChart() {
        if (statusChart == null) return;

        statusChart.getData().clear();

        var statusStats = StatisticsService.getStatusStats();
        for (var entry : statusStats.entrySet()) {
            PieChart.Data slice = new PieChart.Data(entry.getKey(), entry.getValue());
            statusChart.getData().add(slice);
        }
    }

    private static void updateTopServices() {
        if (topServicesList == null) return;

        topServicesList.getItems().clear();
        var topServices = StatisticsService.getTopServices(10);

        for (int i = 0; i < topServices.size(); i++) {
            var entry = topServices.get(i);
            topServicesList.getItems().add((i + 1) + ". " + entry.getKey() + " — " + entry.getValue() + " заказ(ов)");
        }

        if (topServicesList.getItems().isEmpty()) {
            topServicesList.getItems().add("Нет данных");
        }
    }

    private static void updateTopParts() {
        if (topPartsList == null) return;

        topPartsList.getItems().clear();
        var topParts = StatisticsService.getTopSpareParts(10);

        for (int i = 0; i < topParts.size(); i++) {
            var entry = topParts.get(i);
            topPartsList.getItems().add((i + 1) + ". " + entry.getKey() + " — " + entry.getValue() + " шт.");
        }

        if (topPartsList.getItems().isEmpty()) {
            topPartsList.getItems().add("Нет данных");
        }
    }

    public static void refreshAll() {
        if (activeOrdersLabel != null) {
            activeOrdersLabel.setText(String.valueOf(DataStore.getActiveOrdersCount()));
            totalClientsLabel.setText(String.valueOf(DataStore.getClients().size()));
            totalOrdersLabel.setText(String.valueOf(DataStore.getOrders().size()));
        }

        updateRevenueChart(30);
        updateMastersChart();
        updateStatusChart();
        updateTopServices();
        updateTopParts();
    }
}