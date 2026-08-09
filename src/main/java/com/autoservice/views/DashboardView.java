package com.autoservice.views;

import com.autoservice.*;
import com.autoservice.controllers.ClientController;
import com.autoservice.controllers.OrderController;
import com.autoservice.dialogs.CreateOrderDialog;
import com.autoservice.dialogs.EditClientDialog;
import com.autoservice.dialogs.OrderDetailsDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DashboardView extends ScrollPane {

    private static DashboardView instance;
    private final GridPane gridPane;
    private final NumberFormat currencyFormat;
    private Stage primaryStage;

    // ==================== СТИЛИ (CSS) ====================

    private static final String DARK_BG = "#15172A";
    private static final String CARD_BG = "#1E2139";
    private static final String TEXT_WHITE = "#FFFFFF";
    private static final String TEXT_GRAY = "#9CA3AF";
    private static final String ACCENT_BLUE = "#3B82F6";
    private static final String ACCENT_CYAN = "#22D3EE";
    private static final String ACCENT_PINK = "#F472B6";
    private static final String ACCENT_ORANGE = "#FB923C";

    private void applyDarkTheme(Node node) {
        if (node instanceof VBox) {
            ((VBox) node).setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4);");
        } else if (node instanceof HBox) {
            ((HBox) node).setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4);");
        }
    }

    private Label styledLabel(String text, String color, double fontSize, boolean bold) {
        Label label = new Label(text);
        label.setTextFill(javafx.scene.paint.Color.web(color));
        label.setFont(javafx.scene.text.Font.font("Segoe UI", bold ? javafx.scene.text.FontWeight.BOLD : javafx.scene.text.FontWeight.NORMAL, fontSize));
        label.setWrapText(true);
        return label;
    }

    // ==================== СТАТИЧЕСКИЕ МЕТОДЫ ====================

    public static DashboardView create() {
        if (instance == null) {
            instance = new DashboardView();
        }
        return instance;
    }

    public static void refresh() {
        if (instance != null) {
            instance.doRefresh();
        }
    }

    public static void setStage(Stage stage) {
        if (instance != null) {
            instance.primaryStage = stage;
        }
    }

    // ==================== КОНСТРУКТОР ====================

    private DashboardView() {
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale.Builder().setLanguage("ru").setRegion("RU").build());

        // Dashboard CSS теперь управляется ThemeManager — не загружаем здесь

        gridPane = new GridPane();
        gridPane.setPadding(new Insets(24));
        gridPane.setHgap(20);
        gridPane.setVgap(20);
        gridPane.setAlignment(Pos.TOP_CENTER);
        // Фон на весь контейнер
        gridPane.setStyle("-fx-background-color: " + DARK_BG + "; -fx-background-insets: 0; -fx-padding: 24;");
        gridPane.setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(javafx.scene.paint.Color.web(DARK_BG), null, null)));

        setContent(gridPane);
        setFitToWidth(true);
        setFitToHeight(true);
        // Фон самого ScrollPane тоже тёмный, чтобы не было светлых полос
        setStyle("-fx-background-color: " + DARK_BG + "; -fx-background-insets: 0; -fx-padding: 0;");

        doRefresh();
    }

    // ==================== ОБНОВЛЕНИЕ ====================

    private void doRefresh() {
        long start = System.currentTimeMillis();
        gridPane.getChildren().clear();
        gridPane.getColumnConstraints().clear();
        gridPane.setStyle("-fx-background-color: " + DARK_BG + "; -fx-background-insets: 0; -fx-padding: 24;");
        gridPane.setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(javafx.scene.paint.Color.web(DARK_BG), null, null)));

        // ====== АДАПТИВНЫЕ КОЛОНКИ: 30% / 40% / 30% ======
        ColumnConstraints colLeft = new ColumnConstraints();
        colLeft.setPercentWidth(30);
        colLeft.setHgrow(Priority.ALWAYS);
        colLeft.setFillWidth(true);

        ColumnConstraints colCenter = new ColumnConstraints();
        colCenter.setPercentWidth(40);
        colCenter.setHgrow(Priority.ALWAYS);
        colCenter.setFillWidth(true);

        ColumnConstraints colRight = new ColumnConstraints();
        colRight.setPercentWidth(30);
        colRight.setHgrow(Priority.ALWAYS);
        colRight.setFillWidth(true);

        gridPane.getColumnConstraints().addAll(colLeft, colCenter, colRight);

        // ====== ЛЕВАЯ КОЛОНКА ======
        VBox leftColumn = new VBox(16);
        leftColumn.setAlignment(Pos.TOP_CENTER);

        // 1. Круговая диаграмма (Бюджет / Выручка)
        double revenue = parseRevenue(getTotalRevenue());
        VBox donutCard = createBudgetDonutChart(revenue, revenue * 1.3);
        leftColumn.getChildren().add(donutCard);

        // 2. Календарь (с уменьшенным размером, перенесён в левую колонку)
        VBox calendarCard = createAppointmentCalendar(LocalDate.now());
        calendarCard.setMaxWidth(280); // Ограничиваем ширину
        leftColumn.getChildren().add(calendarCard);

        // ====== ЦЕНТРАЛЬНАЯ КОЛОНКА ======
        VBox centerColumn = new VBox(16);
        centerColumn.setAlignment(Pos.TOP_CENTER);

        // 1. График выручки (Cashflow)
        VBox lineCard = createLineChart("Динамика выручки", getMonthlyRevenueData());
        centerColumn.getChildren().add(lineCard);

        // 2. Дополнительная информация (нижняя часть центра)
        VBox extraInfo = new VBox(10);
        extraInfo.setAlignment(Pos.CENTER_LEFT);
        extraInfo.setPadding(new Insets(10));
        extraInfo.setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 12px; -fx-padding: 16;");

        Label infoTitle = styledLabel("Общая статистика", TEXT_WHITE, 13, true);
        extraInfo.getChildren().add(infoTitle);

        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER);
        statsRow.getChildren().addAll(
                createStatColumn("Заказов", String.valueOf(DataStore.getOrders().size()), ACCENT_CYAN),
                createStatColumn("Клиентов", String.valueOf(DataStore.getClients().size()), ACCENT_BLUE),
                createStatColumn("В работе", String.valueOf(getActiveOrdersCount()), ACCENT_PINK)
        );
        extraInfo.getChildren().add(statsRow);
        centerColumn.getChildren().add(extraInfo);

        // ====== ПРАВАЯ КОЛОНКА ======
        VBox rightColumn = new VBox(16);
        rightColumn.setAlignment(Pos.TOP_CENTER);

        // 1. Диаграмма статусов (разноцветная)
        VBox statusDonut = createStatusDonutChart();
        rightColumn.getChildren().add(statusDonut);

        // 2. Круговой индикатор "Общая выручка" (вместо кнопок)
        double revenueRight = parseRevenue(getTotalRevenue());
        VBox revenueMiniCard = createRevenueMiniCard(revenueRight);
        rightColumn.getChildren().add(revenueMiniCard);

        gridPane.add(leftColumn, 0, 0);
        gridPane.add(centerColumn, 1, 0);
        gridPane.add(rightColumn, 2, 0);

        long duration = System.currentTimeMillis() - start;
        if (duration > 300) {
            System.out.println("⚠️ Медленная операция в DashboardView.doRefresh: " + duration + " мс");
        }
    }

    // ==================== КНОПКИ БЫСТРЫХ ДЕЙСТВИЙ ====================

    private Button createActionButton(String text, String colorKey) {
        Button btn = new Button(text);
        btn.getStyleClass().add("action-btn");
        btn.getStyleClass().add("action-btn-" + colorKey);
        btn.setPrefHeight(38);

        btn.setOnAction(e -> {
            String btnText = text;
            if (btnText.contains("Новый заказ")) {
                openCreateOrderDialog();
            } else if (btnText.contains("Новый клиент")) {
                openEditClientDialog();
            } else if (btnText.contains("Запись")) {
                openAppointmentView();
            } else if (btnText.contains("Отчёт")) {
                generateReport();
            }
        });

        return btn;
    }

    // ==================== ДЕЙСТВИЯ КНОПОК ====================

    private void openCreateOrderDialog() {
        try {
            CreateOrderDialog.show();
            DataStore.load();
            refresh();
            OrderController.refreshTable();
        } catch (Exception ex) {
            showErrorAlert("Ошибка", "Не удалось открыть диалог создания заказа");
        }
    }

    private void openEditClientDialog() {
        try {
            Client emptyClient = new Client(-1, "", "", "", "", "", "");
            EditClientDialog.show(emptyClient);
            DataStore.load();
            refresh();
            ClientController.refreshTable();
        } catch (Exception ex) {
            showErrorAlert("Ошибка", "Не удалось открыть диалог создания клиента");
        }
    }

    private void openAppointmentView() {
        try {
            Node parent = this;
            while (parent != null && !(parent instanceof TabPane)) {
                parent = parent.getParent();
            }
            if (parent instanceof TabPane tabPane) {
                for (Tab tab : tabPane.getTabs()) {
                    if ("Запись".equals(tab.getText())) {
                        tabPane.getSelectionModel().select(tab);
                        return;
                    }
                }
            }
            showInfoAlert("Переключение", "Вкладка 'Запись' не найдена.");
        } catch (Exception ex) {
            showErrorAlert("Ошибка", "Не удалось переключиться на вкладку 'Запись'.");
        }
    }

    private void generateReport() {
        try {
            ReportView.show();
        } catch (Exception ex) {
            showErrorAlert("Ошибка", "Не удалось открыть отчёт");
        }
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== СТАТИСТИКА ====================

    private String getLowStockCount() {
        int count = 0;
        for (SparePart part : DataStore.getSpareParts()) {
            if (part.getStock() < part.getMinStock()) {
                count++;
            }
        }
        return String.valueOf(count);
    }

    private double parseRevenue(String revenueStr) {
        try {
            String cleaned = revenueStr.replaceAll("[^\\d.,]", "").replace(",", ".");
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private List<Double> getMonthlyRevenueData() {
        List<Double> monthlyRevenue = new ArrayList<>();
        // Заполняем последние 6 месяцев
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = LocalDate.now().minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

            double total = 0;
            for (WorkOrder order : DataStore.getOrders()) {
                if (WorkOrder.STATUS_CLOSED.equals(order.getStatus())) {
                    try {
                        LocalDate orderDate = DateUtils.parseDate(order.getCreatedDate());
                        if (orderDate != null && !orderDate.isBefore(monthStart) && !orderDate.isAfter(monthEnd)) {
                            total += order.getTotal();
                        }
                    } catch (Exception ignored) {}
                }
            }
            monthlyRevenue.add(total);
        }
        return monthlyRevenue;
    }

    /**
     * Возвращает динамику выручки за 6 месяцев: выбранный месяц и 5 предыдущих.
     * Каждый элемент — суммарная выручка закрытых заказов за соответствующий месяц.
     */
    private List<Double> getMonthlyRevenueDataForMonth(YearMonth yearMonth) {
        List<Double> monthlyRevenue = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = yearMonth.minusMonths(i);
            LocalDate monthStart = ym.atDay(1);
            LocalDate monthEnd = ym.atEndOfMonth();

            double total = 0;
            for (WorkOrder order : DataStore.getOrders()) {
                if (WorkOrder.STATUS_CLOSED.equals(order.getStatus())) {
                    try {
                        LocalDate orderDate = DateUtils.parseDate(order.getCreatedDate());
                        if (orderDate != null && !orderDate.isBefore(monthStart) && !orderDate.isAfter(monthEnd)) {
                            total += order.getTotal();
                        }
                    } catch (Exception ignored) {}
                }
            }
            monthlyRevenue.add(total);
        }
        return monthlyRevenue;
    }

    private String getTotalRevenue() {
        double total = 0;
        for (WorkOrder order : DataStore.getOrders()) {
            String status = order.getStatus();
            if (WorkOrder.STATUS_CLOSED.equals(status)) {
                total += order.getTotal();
            }
        }
        return currencyFormat.format(total);
    }

    private int getActiveOrdersCount() {
        int count = 0;
        for (WorkOrder order : DataStore.getOrders()) {
            if (WorkOrder.STATUS_IN_PROGRESS.equals(order.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private int getCompletedOrdersCount() {
        int count = 0;
        for (WorkOrder order : DataStore.getOrders()) {
            if (WorkOrder.STATUS_CLOSED.equals(order.getStatus())) {
                count++;
            }
        }
        return count;
    }

    // ==================== ПРАВАЯ КОЛОНКА (ПРОФИЛЬ) ====================

    // ==================== ВИДЖЕТ: КРУГОВАЯ ДИАГРАММА ====================

    private VBox createBudgetDonutChart(double spent, double total) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4);");

        Label title = styledLabel("Выручка", TEXT_WHITE, 14, true);
        Label subtitle = styledLabel("Потрачено / Бюджет", TEXT_GRAY, 11, false);

        Canvas canvas = new Canvas(180, 180);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double cx = 90, cy = 90, radius = 66;
        double progress = Math.min(spent / total, 1.0);
        double angle = 360 * progress;

        gc.setStroke(javafx.scene.paint.Color.web("#2A2F4F"));
        gc.setLineWidth(16);
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, 360, javafx.scene.shape.ArcType.OPEN);

        // Градиент для выручки (зелёно-синий)
        javafx.scene.paint.Color startColor = javafx.scene.paint.Color.web("#22D3EE");
        javafx.scene.paint.Color endColor = javafx.scene.paint.Color.web("#3B82F6");
        javafx.scene.paint.Stop[] stops = {new javafx.scene.paint.Stop(0, startColor), new javafx.scene.paint.Stop(1, endColor)};
        javafx.scene.paint.LinearGradient gradient = new javafx.scene.paint.LinearGradient(0, 0, 1, 0, true, javafx.scene.paint.CycleMethod.NO_CYCLE, stops);
        gc.setStroke(gradient);
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, -angle, javafx.scene.shape.ArcType.OPEN);

        gc.setFill(javafx.scene.paint.Color.web(TEXT_WHITE));
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 23));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText(String.format("%,.0f ₽", spent), cx, cy - 6);

        gc.setFill(javafx.scene.paint.Color.web(TEXT_GRAY));
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", 12));
        gc.fillText("Общий доход", cx, cy + 20);

        card.getChildren().addAll(title, subtitle, canvas);
        // Фиксированная высота — плитка "Выручка" совпадает по высоте с календарём
        card.setMinHeight(330);
        card.setPrefHeight(330);
        return card;
    }

    private VBox createStatusDonutChart() {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4);");

        Label title = styledLabel("Статусы заказов", TEXT_WHITE, 13, true);

        Canvas canvas = new Canvas(160, 160);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        int active = getActiveOrdersCount();
        int closed = getCompletedOrdersCount();
        int total = DataStore.getOrders().size();

        double cx = 80, cy = 80, radius = 58;
        double activeAngle = total > 0 ? 360 * ((double)active / total) : 0;
        double closedAngle = total > 0 ? 360 * ((double)closed / total) : 0;

        // Фон (серая дуга для "Новый" или без статуса)
        gc.setStroke(javafx.scene.paint.Color.web("#2A2F4F"));
        gc.setLineWidth(14);
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, 360, javafx.scene.shape.ArcType.OPEN);

        // Сектор "В работе" (розовый)
        if (activeAngle > 0) {
            gc.setStroke(javafx.scene.paint.Color.web(ACCENT_PINK));
            gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, -activeAngle, javafx.scene.shape.ArcType.OPEN);
        }
        // Сектор "Закрыт" (синий) — начинается там, где закончился розовый
        if (closedAngle > 0) {
            gc.setStroke(javafx.scene.paint.Color.web(ACCENT_CYAN));
            gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90 - activeAngle, -closedAngle, javafx.scene.shape.ArcType.OPEN);
        }

        gc.setFill(javafx.scene.paint.Color.web(TEXT_WHITE));
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 19));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText(String.valueOf(total), cx, cy - 6);

        gc.setFill(javafx.scene.paint.Color.web(TEXT_GRAY));
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", 10));
        gc.fillText("Всего заказов", cx, cy + 18);

        // ====== Легенда ======
        int newCount = Math.max(total - active - closed, 0);

        VBox legendBox = new VBox(6);
        legendBox.setAlignment(Pos.CENTER_LEFT);
        legendBox.setPadding(new Insets(4, 0, 0, 0));

        legendBox.getChildren().addAll(
                createLegendRow(ACCENT_PINK, "В работе: " + active),
                createLegendRow(ACCENT_CYAN, "Закрыт: " + closed),
                createLegendRow("#2A2F4F", "Новый: " + newCount)
        );

        HBox chartWithLegend = new HBox(12, canvas, legendBox);
        chartWithLegend.setAlignment(Pos.CENTER);

        card.getChildren().addAll(title, chartWithLegend);
        return card;
    }

    /**
     * Строка легенды: цветной кружок + текст.
     */
    private HBox createLegendRow(String color, String text) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(6);
        dot.setFill(javafx.scene.paint.Color.web(color));
        Label lbl = styledLabel(text, TEXT_GRAY, 11, false);
        row.getChildren().addAll(dot, lbl);
        return row;
    }

    private VBox createRevenueMiniCard(double revenue) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4);");

        Label title = styledLabel("Общая выручка", TEXT_WHITE, 13, true);
        Label amount = styledLabel(String.format("%,.0f ₽", revenue), ACCENT_CYAN, 21, true);

        // Маленькая декоративная линия (просто красоты ради)
        javafx.scene.shape.Line line = new javafx.scene.shape.Line(0, 0, 80, 0);
        line.setStroke(javafx.scene.paint.Color.web(ACCENT_CYAN));
        line.setStrokeWidth(2);

        // Декоративный Canvas-индикатор, чтобы плитка визуально совпадала по высоте с календарём
        Canvas miniCanvas = new Canvas(120, 120);
        GraphicsContext gc = miniCanvas.getGraphicsContext2D();
        double cx = 60, cy = 60, radius = 44;
        gc.setStroke(javafx.scene.paint.Color.web("#2A2F4F"));
        gc.setLineWidth(12);
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, 360, javafx.scene.shape.ArcType.OPEN);
        javafx.scene.paint.LinearGradient grad = new javafx.scene.paint.LinearGradient(0, 0, 1, 1, true,
                javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.web(ACCENT_CYAN)),
                new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.web(ACCENT_BLUE)));
        gc.setStroke(grad);
        gc.setLineWidth(12);
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, -270, javafx.scene.shape.ArcType.OPEN);

        card.getChildren().addAll(title, amount, line, miniCanvas);
        // Высота совпадает с календарём (330)
        card.setMinHeight(330);
        card.setPrefHeight(330);
        return card;
    }

    // Вспомогательный метод для центральной колонки
    private VBox createStatColumn(String label, String value, String color) {
        VBox v = new VBox(2);
        v.setAlignment(Pos.CENTER);
        Label val = styledLabel(value, color, 19, true);
        Label lbl = styledLabel(label, TEXT_GRAY, 10, false);
        v.getChildren().addAll(val, lbl);
        return v;
    }

    // ==================== ВИДЖЕТ: СТАТУСЫ ====================

    private VBox createStatusCard() {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4);");

        Label title = styledLabel("Статистика", TEXT_WHITE, 13, true);

        VBox stats = new VBox(6);
        stats.getChildren().addAll(
                createStatRow("В работе", String.valueOf(getActiveOrdersCount()), ACCENT_CYAN),
                createStatRow("Выполнено", String.valueOf(getCompletedOrdersCount()), ACCENT_BLUE),
                createStatRow("Записей", String.valueOf(DataStore.getAppointments().size()), ACCENT_PINK)
        );

        card.getChildren().addAll(title, stats);
        return card;
    }

    private HBox createStatRow(String label, String value, String color) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 4, 0));

        Label lbl = styledLabel(label, TEXT_GRAY, 12, false);
        Label val = styledLabel(value, color, 13, true);

        row.getChildren().addAll(lbl, new javafx.scene.layout.Pane(), val);
        return row;
    }

    // ==================== ВИДЖЕТ: ГРАФИК ВЫРУЧКИ (ЛИНЕЙНЫЙ) ====================

    private VBox createLineChart(String titleText, List<Double> data) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4);");

        Label title = styledLabel(titleText, TEXT_WHITE, 13, true);

        // ====== Переключатель месяца ======
        java.time.YearMonth currentMonth = java.time.YearMonth.now();
        java.time.YearMonth[] selectedMonth = {currentMonth};

        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("LLLL yyyy", new Locale("ru"));
        Label monthLabel = styledLabel(selectedMonth[0].format(monthFmt), TEXT_WHITE, 13, true);
        monthLabel.setMinWidth(120);
        monthLabel.setAlignment(Pos.CENTER);

        Button leftBtn = new Button("←");
        leftBtn.getStyleClass().add("action-btn");
        leftBtn.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 14; -fx-cursor: hand;");
        leftBtn.setPrefSize(36, 30);

        Button rightBtn = new Button("→");
        rightBtn.getStyleClass().add("action-btn");
        rightBtn.setStyle("-fx-background-color: " + ACCENT_BLUE + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-size: 14; -fx-cursor: hand;");
        rightBtn.setPrefSize(36, 30);

        HBox topControls = new HBox(10, leftBtn, monthLabel, rightBtn);
        topControls.setAlignment(Pos.CENTER);

        Canvas canvas = new Canvas(400, 200);

        // Лямбда перерисовки графика для выбранного месяца
        Runnable redraw = () -> {
            List<Double> monthData = getMonthlyRevenueDataForMonth(selectedMonth[0]);
            monthLabel.setText(selectedMonth[0].format(monthFmt));
            drawLineChartCanvas(canvas, monthData);
        };

        leftBtn.setOnAction(e -> {
            selectedMonth[0] = selectedMonth[0].minusMonths(1);
            redraw.run();
        });
        rightBtn.setOnAction(e -> {
            selectedMonth[0] = selectedMonth[0].plusMonths(1);
            redraw.run();
        });

        // Первичная отрисовка
        drawLineChartCanvas(canvas, data);

        card.getChildren().addAll(title, topControls, canvas);
        return card;
    }

    /**
     * Отрисовка линейного графика выручки на переданном Canvas.
     */
    private void drawLineChartCanvas(Canvas canvas, List<Double> data) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (data == null || data.isEmpty()) {
            gc.setFill(javafx.scene.paint.Color.web(TEXT_GRAY));
            gc.setFont(javafx.scene.text.Font.font(13));
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.fillText("Нет данных", canvas.getWidth() / 2, canvas.getHeight() / 2);
            return;
        }

        double max = data.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        double min = data.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double range = Math.max(max - min, 1.0);

        int n = data.size();
        double padding = 20;
        double graphW = canvas.getWidth() - padding * 2;
        double graphH = canvas.getHeight() - padding * 2;

        // Цвета — приглушённый сине-фиолетовый
        javafx.scene.paint.Color lineColor = javafx.scene.paint.Color.web("#7C8BDB");
        javafx.scene.paint.Color areaTop = javafx.scene.paint.Color.web("#7C8BDB");
        javafx.scene.paint.Color areaBottom = javafx.scene.paint.Color.web("#15172A");

        // Расчёт точек
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = padding + (graphW / (n - 1)) * i;
            ys[i] = padding + graphH - ((data.get(i) - min) / range) * graphH;
        }

        // 1. Градиентная заливка под линией (плавный переход к фону)
        javafx.scene.paint.Stop[] stops = {
                new javafx.scene.paint.Stop(0, areaTop),
                new javafx.scene.paint.Stop(0.5, areaTop.deriveColor(0, 0.7, 0.8, 0.5)),
                new javafx.scene.paint.Stop(1, areaBottom)
        };
        javafx.scene.paint.LinearGradient areaGradient = new javafx.scene.paint.LinearGradient(0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE, stops);

        gc.setFill(areaGradient);
        gc.beginPath();
        gc.moveTo(xs[0], padding + graphH);
        for (int i = 0; i < n; i++) {
            gc.lineTo(xs[i], ys[i]);
        }
        gc.lineTo(xs[n - 1], padding + graphH);
        gc.closePath();
        gc.fill();

        // 2. Линия (сглаженная кривая через квадратичную интерполяцию)
        gc.setStroke(lineColor);
        gc.setLineWidth(2.0);
        gc.beginPath();
        gc.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) {
            double midX = (xs[i - 1] + xs[i]) / 2;
            gc.quadraticCurveTo(midX, ys[i - 1], xs[i], ys[i]);
        }
        gc.stroke();

        // 3. Точки (мягкие, полупрозрачные)
        gc.setFill(lineColor.deriveColor(0, 0.5, 1, 0.4));
        gc.setStroke(lineColor.deriveColor(0, 0.5, 1, 0.6));
        gc.setLineWidth(1);
        for (int i = 0; i < n; i++) {
            gc.fillOval(xs[i] - 4, ys[i] - 4, 8, 8);
            gc.strokeOval(xs[i] - 4, ys[i] - 4, 8, 8);
        }

        // 4. Сетка (очень бледная)
        gc.setStroke(javafx.scene.paint.Color.web("#2A2F4F"));
        gc.setLineWidth(0.5);
        for (int i = 0; i < 5; i++) {
            double y = padding + (graphH / 4) * i;
            gc.strokeLine(padding, y, canvas.getWidth() - padding, y);
        }
    }

    // ==================== ВИДЖЕТ: КАЛЕНДАРЬ ====================

    private VBox createAppointmentCalendar(LocalDate currentMonth) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4);");

        Label title = styledLabel("Записи на сервис (" + currentMonth.format(DateTimeFormatter.ofPattern("MMM")) + ")", TEXT_WHITE, 13, true);
        card.getChildren().add(title);

        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        grid.setAlignment(Pos.CENTER);

        // Шрифт дней недели
        String[] days = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = styledLabel(days[i], TEXT_GRAY, 11, true);
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setPrefSize(34, 24);
            grid.add(dayLabel, i, 0);
        }

        List<LocalDate> appointmentDates = new ArrayList<>();
        for (Appointment a : DataStore.getAppointments()) {
            LocalDate d = DateUtils.parseDate(a.getDate());
            if (d != null && d.getMonth() == currentMonth.getMonth() && d.getYear() == currentMonth.getYear()) {
                appointmentDates.add(d);
            }
        }

        LocalDate firstDay = currentMonth.withDayOfMonth(1);
        int dayOfWeekOffset = (firstDay.getDayOfWeek().getValue() - 1 + 7) % 7;

        int daysInMonth = currentMonth.lengthOfMonth();
        int row = 1, col = dayOfWeekOffset;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.withDayOfMonth(day);
            boolean hasAppointment = appointmentDates.contains(date);

            StackPane cell = new StackPane();
            cell.setPrefSize(34, 34); // увеличили размер плиток
            cell.setAlignment(Pos.CENTER);

            // Шрифт чисел тоже увеличили
            Label dayNum = styledLabel(String.valueOf(day), hasAppointment ? ACCENT_CYAN : TEXT_GRAY, 12, hasAppointment);

            if (hasAppointment) {
                javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(13);
                circle.setFill(javafx.scene.paint.Color.web(ACCENT_PINK));
                cell.getChildren().addAll(circle, dayNum);
            } else {
                cell.getChildren().add(dayNum);
            }

            // Кликом по любой ячейке открываем вкладку "Запись"
            cell.setOnMouseClicked(e -> openAppointmentView());
            grid.add(cell, col, row);
            col++;
            if (col >= 7) { col = 0; row++; }
        }

        card.getChildren().add(grid);
        // Фиксированная высота — календарь совпадает по высоте с плиткой "Выручка"
        card.setMinHeight(330);
        card.setPrefHeight(330);
        return card;
    }

    // ==================== ВИДЖЕТ: EARNINGS ====================

    private VBox createEarningsCard() {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4);");

        Label title = styledLabel("Прибыль в этом месяце", TEXT_WHITE, 13, true);

        // Парсим выручку для отображения
        double revenue = parseRevenue(getTotalRevenue());
        Label amount = styledLabel("$" + String.format("%,.0f", revenue), ACCENT_CYAN, 21, true);
        card.getChildren().addAll(title, amount);

        // Маленькая линия (можно использовать заготовку графика)
        List<Double> dummyData = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            dummyData.add(Math.random() * revenue * 0.1 + revenue * 0.8);
        }
        VBox miniChart = createLineChart("", dummyData);
        miniChart.setStyle("-fx-background-color: transparent; -fx-effect: none;");
        card.getChildren().add(miniChart);

        return card;
    }

    /**
     * Возвращает список записей на текущую неделю (с понедельника по воскресенье)
     * Только активные записи (не закрытые и не выполненные)
     */
    private List<Appointment> getWeekAppointments() {
        List<Appointment> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Определяем понедельник текущей недели
        LocalDate monday = today;
        while (monday.getDayOfWeek().getValue() != 1) {
            monday = monday.minusDays(1);
        }
        // Воскресенье
        LocalDate sunday = monday.plusDays(6);

        for (Appointment a : DataStore.getAppointments()) {
            try {
                LocalDate appointmentDate = DateUtils.parseDate(a.getDate());

                // Проверяем, что запись в пределах недели
                if (appointmentDate.isBefore(monday) || appointmentDate.isAfter(sunday)) {
                    continue;
                }

                // Проверяем, что запись активна (не закрыта и не выполнена)
                String status = a.getStatus();
                if (status == null) {
                    continue;
                }

                // Исключаем закрытые и выполненные записи
                if (status.equals("Выполнено") || status.equals("Закрыт")) {
                    continue;
                }

                result.add(a);

            } catch (Exception ignored) {
                // Если дата не парсится — пропускаем
            }
        }

        // Сортируем по дате и времени
        result.sort((a1, a2) -> {
            int dateCompare = a1.getDate().compareTo(a2.getDate());
            if (dateCompare != 0) return dateCompare;
            return a1.getTime().compareTo(a2.getTime());
        });

        return result;
    }

    // ==================== ВНУТРЕННИЙ КЛАСС ДЛЯ ТАБЛИЦЫ ====================

    public static class AppointmentRow {
        private final String orderId;
        private final String clientName;
        private final String carModel;
        private final String serviceName;
        private final String dateTime;
        private final WorkOrder linkedOrder;

        public AppointmentRow(String orderId, String clientName, String carModel,
                              String serviceName, String dateTime, WorkOrder linkedOrder) {
            this.orderId = orderId;
            this.clientName = clientName;
            this.carModel = carModel;
            this.serviceName = serviceName;
            this.dateTime = dateTime;
            this.linkedOrder = linkedOrder;
        }

        public String getOrderId() { return orderId; }
        public String getClientName() { return clientName; }
        public String getCarModel() { return carModel; }
        public String getServiceName() { return serviceName; }
        public String getDateTime() { return dateTime; }
        public WorkOrder getLinkedOrder() { return linkedOrder; }
    }
}