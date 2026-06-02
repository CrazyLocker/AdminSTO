package com.autoservice.views;

import com.autoservice.DataStore;
import com.autoservice.WorkOrder;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DashboardView {

    private static VBox root;
    private static BarChart<String, Number> revenueChart;
    private static ComboBox<String> periodComboBox;
    private static int currentDays = 30;

    public static VBox create() {
        root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f0f2f5;");
        refresh();
        return root;
    }

    public static void refresh() {
        if (root != null) {
            root.getChildren().clear();
            buildDashboard();
        }
    }

    private static void buildDashboard() {
        Label titleLabel = new Label("Панель управления СТО");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        GridPane statsGrid = createStatsGrid();

        Separator separator = new Separator();
        separator.setPadding(new Insets(10, 0, 10, 0));

        // Панель выбора периода
        HBox periodPanel = createPeriodPanel();

        // График выручки
        revenueChart = createRevenueChart();

        root.getChildren().addAll(titleLabel, statsGrid, separator, periodPanel, revenueChart);
    }

    private static HBox createPeriodPanel() {
        HBox panel = new HBox(15);
        panel.setAlignment(Pos.CENTER_RIGHT);
        panel.setPadding(new Insets(5, 0, 10, 0));

        Label periodLabel = new Label("Период:");
        periodLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        periodComboBox = new ComboBox<>();
        periodComboBox.getItems().addAll("7 дней", "14 дней", "30 дней", "60 дней", "90 дней");
        periodComboBox.setValue("30 дней");
        periodComboBox.setStyle("-fx-font-size: 13px;");
        periodComboBox.setOnAction(e -> {
            String selected = periodComboBox.getValue();
            if (selected.equals("7 дней")) currentDays = 7;
            else if (selected.equals("14 дней")) currentDays = 14;
            else if (selected.equals("30 дней")) currentDays = 30;
            else if (selected.equals("60 дней")) currentDays = 60;
            else if (selected.equals("90 дней")) currentDays = 90;
            updateChart();
        });

        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 15 5 15;");
        refreshBtn.setOnAction(e -> updateChart());

        panel.getChildren().addAll(periodLabel, periodComboBox, refreshBtn);
        return panel;
    }

    private static void updateChart() {
        if (revenueChart != null) {
            revenueChart.getData().clear();

            CategoryAxis xAxis = (CategoryAxis) revenueChart.getXAxis();
            NumberAxis yAxis = (NumberAxis) revenueChart.getYAxis();

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Выручка");

            Map<String, Double> dailyRevenue = getDailyRevenue(currentDays);

            for (Map.Entry<String, Double> entry : dailyRevenue.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }

            revenueChart.getData().add(series);
        }
    }

    private static GridPane createStatsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(10));

        List<WorkOrder> orders = DataStore.getOrders();

        System.out.println("Всего заказов в DataStore: " + orders.size());

        int totalOrders = orders.size();
        int newOrders = 0;
        int inProgressOrders = 0;
        int closedOrders = 0;
        double totalRevenue = 0;

        Set<String> uniqueClients = new HashSet<>();

        for (WorkOrder order : orders) {
            String status = order.getStatus();

            System.out.println("Заказ " + order.getId() + " статус: '" + status + "'");

            if (status != null) {
                if (status.equals("Новый") || status.equals("НОВЫЙ")) {
                    newOrders++;
                } else if (status.equals("В работе") || status.equals("В РАБОТЕ")) {
                    inProgressOrders++;
                } else if (status.equals("Закрыт") || status.equals("ЗАКРЫТ") || status.equals("Завершён")) {
                    closedOrders++;
                    totalRevenue += order.getTotal();
                }
            }

            if (order.getClient() != null) {
                uniqueClients.add(order.getClient().getName());
            }
        }

        System.out.println("Статистика:");
        System.out.println("  Новые: " + newOrders);
        System.out.println("  В работе: " + inProgressOrders);
        System.out.println("  Закрытые: " + closedOrders);
        System.out.println("  Выручка: " + totalRevenue);

        int uniqueClientsCount = uniqueClients.size();
        double averageOrderValue = closedOrders > 0 ? totalRevenue / closedOrders : 0;

        VBox revenueCard = createStatCard("Общая выручка", formatMoney(totalRevenue), "#27ae60");
        VBox avgOrderCard = createStatCard("Средний чек", formatMoney(averageOrderValue), "#3498db");
        VBox ordersCard = createStatCard("Всего заказов", String.valueOf(totalOrders), "#e67e22");
        VBox newOrdersCard = createStatCard("Новые заказы", String.valueOf(newOrders), "#f39c12");
        VBox progressCard = createStatCard("В работе", String.valueOf(inProgressOrders), "#1abc9c");
        VBox closedOrdersCard = createStatCard("Завершено", String.valueOf(closedOrders), "#2ecc71");
        VBox clientsCard = createStatCard("Клиентов", String.valueOf(uniqueClientsCount), "#3498db");
        VBox revenuePerClient = createStatCard("Средняя выручка на клиента", formatMoney(uniqueClientsCount > 0 ? totalRevenue / uniqueClientsCount : 0), "#e74c3c");

        grid.add(revenueCard, 0, 0);
        grid.add(avgOrderCard, 1, 0);
        grid.add(ordersCard, 2, 0);
        grid.add(newOrdersCard, 3, 0);

        grid.add(progressCard, 0, 1);
        grid.add(closedOrdersCard, 1, 1);
        grid.add(clientsCard, 2, 1);
        grid.add(revenuePerClient, 3, 1);

        return grid;
    }

    private static VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-radius: 12; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);");
        card.setPrefWidth(180);
        card.setMinHeight(100);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private static BarChart<String, Number> createRevenueChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Дата");
        yAxis.setLabel("Выручка (руб.)");
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis) {
            @Override
            public String toString(Number object) {
                return String.format("%,.0f", object);
            }
        });

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Выручка по дням");
        chart.setPrefHeight(400);
        chart.setStyle("-fx-background-color: white; -fx-border-radius: 10; -fx-background-radius: 10;");
        chart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Выручка");

        Map<String, Double> dailyRevenue = getDailyRevenue(currentDays);

        for (Map.Entry<String, Double> entry : dailyRevenue.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        chart.getData().add(series);

        return chart;
    }

    private static Map<String, Double> getDailyRevenue(int days) {
        Map<String, Double> dailyRevenue = new LinkedHashMap<>();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM");

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dailyRevenue.put(date.format(formatter), 0.0);
        }

        List<WorkOrder> orders = DataStore.getOrders();

        for (WorkOrder order : orders) {
            String status = order.getStatus();
            boolean isClosed = false;

            if (status != null) {
                isClosed = status.equals("Закрыт") || status.equals("ЗАКРЫТ") || status.equals("Завершён");
            }

            if (isClosed) {
                String dateStr = order.getCreatedDate();
                if (dateStr != null && !dateStr.isEmpty()) {
                    try {
                        String datePart = dateStr.split(" ")[0];
                        LocalDate orderDate = LocalDate.parse(datePart);
                        if (!orderDate.isBefore(startDate) && !orderDate.isAfter(endDate)) {
                            String key = orderDate.format(formatter);
                            dailyRevenue.put(key, dailyRevenue.get(key) + order.getTotal());
                        }
                    } catch (Exception e) {
                        System.err.println("Ошибка парсинга даты: " + dateStr);
                    }
                }
            }
        }

        return dailyRevenue;
    }

    private static String formatMoney(double amount) {
        return String.format("%,.0f руб.", amount);
    }
}