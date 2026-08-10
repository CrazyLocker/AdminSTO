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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Главный экран (дашборд) панели администратора СТО.
 * 
 * Ответственность: агрегированное отображение ключевых метрик работы СТО —
 * выручки (круговая диаграмма), динамики выручки за 6 месяцев (линейный
 * график), статусов заказов, статистики по клиентам и заказам, а также
 * календаря записей на сервис.
 * 
 * Зависимости: JavaFX (Canvas, GridPane, VBox, HBox, StackPane), DataStore,
 * WorkOrder, Appointment, DateUtils, ClientController, OrderController,
 * CreateOrderDialog, EditClientDialog, OrderDetailsDialog, ReportView.
 * 
 * Особенности: реализует паттерн «одиночка» (singleton) через статические
 * методы {@code create()} / {@code refresh()}; использует адаптивную верстку
 * на GridPane с колонками 30% / 40% / 30%; диаграммы рисуются вручную на
 * Canvas (без сторонних библиотек); Canvas привязан к ширине контейнера через
 * {@code bind} для адаптивности.
 * 
 * @author AdminSTO Team
 * @since 1.0
 * @see DataStore
 * @see WorkOrder
 * @see Appointment
 * @see DateUtils
 */
public class DashboardView extends ScrollPane {

    /** Единственный экземпляр дашборда (singleton). */
    private static DashboardView instance;

    /** Корневая сетка дашборда, в которую помещаются три колонки виджетов. */
    private final GridPane gridPane;

    /** Форматтер валюты (рубли, локаль ru-RU) для отображения выручки. */
    private final NumberFormat currencyFormat;

    /** Ссылка на главное окно приложения (используется для навигации). */
    private Stage primaryStage;

    // ==================== СТИЛИ (CSS) ====================

    /** Цвет фона дашборда. */
    private static final String DARK_BG = "#15172A";
    /** Цвет фона карточек-виджетов. */
    private static final String CARD_BG = "#1E2139";
    /** Основной цвет текста (белый). */
    private static final String TEXT_WHITE = "#FFFFFF";
    /** Приглушённый серый цвет для второстепенного текста. */
    private static final String TEXT_GRAY = "#9CA3AF";
    /** Акцентный синий цвет. */
    private static final String ACCENT_BLUE = "#3B82F6";
    /** Акцентный голубой (циановый) цвет. */
    private static final String ACCENT_CYAN = "#22D3EE";
    /** Акцентный розовый цвет (статус «В работе»). */
    private static final String ACCENT_PINK = "#F472B6";
    /** Акцентный оранжевый цвет. */
    private static final String ACCENT_ORANGE = "#FB923C";

    /**
     * Применяет тёмную тему (цвет фона и скругление) к карточкам VBox/HBox.
     * 
     * @param node узел, к которому применяется стиль карточки
     */
    private void applyDarkTheme(Node node) {
        if (node instanceof VBox) {
            ((VBox) node).setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4);");
        } else if (node instanceof HBox) {
            ((HBox) node).setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4);");
        }
    }

    /**
     * Создаёт подпись с заданным текстом, цветом, размером и жирностью шрифта.
     * 
     * @param text   отображаемый текст
     * @param color  цвет текста в hex-формате (например, "#FFFFFF")
     * @param fontSize размер шрифта в пунктах
     * @param bold   признак жирного начертания
     * @return настроенная метка {@link Label} с переносом текста
     */
    private Label styledLabel(String text, String color, double fontSize, boolean bold) {
        Label label = new Label(text);
        label.setTextFill(javafx.scene.paint.Color.web(color));
        label.setFont(javafx.scene.text.Font.font("Segoe UI", bold ? javafx.scene.text.FontWeight.BOLD : javafx.scene.text.FontWeight.NORMAL, fontSize));
        label.setWrapText(true);
        return label;
    }

    // ==================== СТАТИЧЕСКИЕ МЕТОДЫ ====================

    /**
     * Возвращает единственный экземпляр дашборда, создавая его при первом
     * обращении (ленивая инициализация).
     * 
     * @return экземпляр {@link DashboardView}
     */
    public static DashboardView create() {
        if (instance == null) {
            instance = new DashboardView();
        }
        return instance;
    }

    /**
     * Обновляет содержимое дашборда, если он уже создан. Вызывается при
     * переключении на вкладку «Дашборд» и после изменения данных.
     */
    public static void refresh() {
        if (instance != null) {
            instance.doRefresh();
        }
    }

    /**
     * Задаёт ссылку на главное окно приложения для навигации.
     * 
     * @param stage главная сцена приложения
     */
    public static void setStage(Stage stage) {
        if (instance != null) {
            instance.primaryStage = stage;
        }
    }

    // ==================== КОНСТРУКТОР ====================

    /**
     * Приватный конструктор (singleton). Настраивает корневую сетку,
     * валютный формат и выполняет первичную отрисовку дашборда.
     */
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

    /**
     * Перестраивает всё содержимое дашборда: очищает сетку и заново
     * формирует три колонки (левая — выручка и календарь, центральная —
     * график выручки и статистика, правая — статусы и общая выручка).
     * Колонки настраиваются адаптивно в пропорции 30% / 40% / 30%.
     */
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
            // TODO: операция перерисовки занимает >300 мс — рассмотреть кэширование
            // графиков и отложенную отрисовку тяжелых виджетов
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

    /**
     * Подсчитывает количество запчастей с остатком ниже минимального уровня.
     * 
     * @return строковое представление количества «низких» остатков
     */
    private String getLowStockCount() {
        int count = 0;
        for (SparePart part : DataStore.getSpareParts()) {
            if (part.getAvailableStock() < part.getMinStock()) {
                count++;
            }
        }
        return String.valueOf(count);
    }

    /**
     * Преобразует строку с числом валюты (например, "12 345,00 ₽") в число
     * типа {@code double}. При ошибке парсинга возвращает 0.
     * 
     * @param revenueStr строка с суммой, возможно с форматированием
     * @return числовое значение суммы или 0.0 при неудачном разборе
     */
    private double parseRevenue(String revenueStr) {
        try {
            String cleaned = revenueStr.replaceAll("[^0-9.,]", "").replace(",", ".");
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Вычисляет динамику выручки за последние 6 месяцев (текущий и 5
     * предыдущих). Учитываются только заказы со статусом «Закрыт».
     * 
     * Алгоритм: все закрытые заказы группируются по месяцу (YearMonth),
     * суммы суммируются; затем формируется список из 6 значений, где для
     * месяцев без закрытых заказов подставляется 0.
     * 
     * @return список из 6 значений выручки по месяцам (от старых к новым)
     */
    private List<Double> getMonthlyRevenueData() {
        // Замена 1: лог общего числа заказов
        System.out.println("🔍 Всего заказов в БД: " + DataStore.getOrders().size());

        // Группируем ВСЕ закрытые заказы по месяцу (YearMonth) — независимо от давности
        HashMap<YearMonth, Double> revenueByMonth = new HashMap<>();
        int totalClosed = 0;

        for (WorkOrder order : DataStore.getOrders()) {
            if (!WorkOrder.STATUS_CLOSED.equals(order.getStatus())) {
                continue;
            }
            totalClosed++;

            // Замена 2: защита от null/пустой даты
            String createdDate = order.getCreatedDate();
            if (createdDate == null || createdDate.trim().isEmpty()) {
                System.out.println("⚠️ Пропущен заказ без даты: " + order.getId());
                continue;
            }

            LocalDate orderDate = DateUtils.parseDate(createdDate);
            if (orderDate == null) {
                System.out.println("⚠️ Пропущен заказ (дата не распарсилась): " + order.getId());
                continue; // дата не распарсилась — пропускаем
            }
            YearMonth ym = YearMonth.from(orderDate);
            revenueByMonth.merge(ym, order.getTotal(), Double::sum);
        }

        // Замена 3: формируем результат за последние 6 месяцев.
        // Если закрытых заказов нет — по всем месяцам будут нули (ровная линия на 0).
        List<Double> monthlyRevenue = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            monthlyRevenue.add(revenueByMonth.getOrDefault(current.minusMonths(i), 0.0));
        }

        // Замена 4: итоговый лог
        System.out.println("📊 Итоговый массив выручки: " + Arrays.toString(monthlyRevenue.toArray()));
        System.out.println("📊 Всего закрытых заказов учтено: " + totalClosed);
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

    /**
     * Подсчитывает количество заказов со статусом «В работе».
     * 
     * @return количество активных заказов
     */
    private int getActiveOrdersCount() {
        int count = 0;
        for (WorkOrder order : DataStore.getOrders()) {
            if (WorkOrder.STATUS_IN_PROGRESS.equals(order.getStatus())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Подсчитывает количество заказов со статусом «Закрыт».
     * 
     * @return количество закрытых заказов
     */
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

    /**
     * Создаёт карточку-круговую диаграмму «Выручка» с градиентной дугой
     * прогресса (потрачено относительно бюджета) и подписью суммы.
     * 
     * @param spent текущая выручка
     * @param total бюджет (целевая сумма), от которого считается прогресс
     * @return карточка VBox с диаграммой
     */
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
        return card;
    }

    /**
     * Создаёт круговую диаграмму распределения заказов по статусам
     * («В работе» — розовый, «Закрыт» — голубой, «Новый» — серый фон)
     * с легендой и общим количеством заказов в центре.
     * 
     * @return карточка VBox с диаграммой статусов и легендой
     */
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

    /**
     * Создаёт мини-карточку «Общая выручка» с декоративным круговым
     * индикатором (заполненная на ~270° дуга с градиентом).
     * 
     * @param revenue сумма выручки для отображения
     * @return карточка VBox с общей выручкой
     */
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

    /**
     * Создаёт карточку линейного графика выручки. График адаптивен: Canvas
     * привязан к ширине карточки через {@code bind} (widthProperty), а высота
     * вычисляется как 55% от ширины. Сверху есть переключатель месяца
     * (кнопки ← / →), при нажатии которых график перерисовывается для
     * выбранного месяца.
     * 
     * @param titleText заголовок карточки
     * @param data      исходные данные для первичной отрисовки
     * @return карточка VBox с графиком
     */
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

        // Инлайн-стиль для кнопок навигации по месяцам (CSS-файл отсутствует)
        String btnStyle = "-fx-background-color: #2A2F4F; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 4 10 4 10; -fx-background-radius: 8px; -fx-cursor: hand;";
        String btnHoverStyle = "-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 4 10 4 10; -fx-background-radius: 8px; -fx-cursor: hand;";

        Button leftBtn = new Button("←");
        leftBtn.setStyle(btnStyle);
        leftBtn.setPrefSize(36, 30);
        leftBtn.setOnMouseEntered(e -> leftBtn.setStyle(btnHoverStyle));
        leftBtn.setOnMouseExited(e -> leftBtn.setStyle(btnStyle));

        Button rightBtn = new Button("→");
        rightBtn.setStyle(btnStyle);
        rightBtn.setPrefSize(36, 30);
        rightBtn.setOnMouseEntered(e -> rightBtn.setStyle(btnHoverStyle));
        rightBtn.setOnMouseExited(e -> rightBtn.setStyle(btnStyle));

        // Кнопка создания тестового заказа для проверки графика выручки
        Button testOrderBtn = new Button("Создать тестовый заказ");
        testOrderBtn.setStyle("-fx-background-color: #22D3EE; -fx-text-fill: #15172A; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10 4 10; -fx-background-radius: 8px; -fx-cursor: hand;");
        testOrderBtn.setOnMouseEntered(e -> testOrderBtn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10 4 10; -fx-background-radius: 8px; -fx-cursor: hand;"));
        testOrderBtn.setOnMouseExited(e -> testOrderBtn.setStyle("-fx-background-color: #22D3EE; -fx-text-fill: #15172A; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10 4 10; -fx-background-radius: 8px; -fx-cursor: hand;"));
        testOrderBtn.setOnAction(e -> createTestOrder());

        HBox topControls = new HBox(10, leftBtn, monthLabel, rightBtn, testOrderBtn);
        topControls.setAlignment(Pos.CENTER);

        // Адаптивный Canvas: растягивается по ширине и высоте плитки
        Canvas canvas = new Canvas();
        VBox.setVgrow(canvas, Priority.ALWAYS);
        canvas.widthProperty().bind(card.widthProperty().subtract(32));
        canvas.heightProperty().bind(canvas.widthProperty().multiply(0.55));

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
     * Создаёт тестовый заказ со статусом «Закрыт», сегодняшней датой и
     * суммой 5000 руб. для проверки отображения графика выручки.
     * Используется первый клиент из DataStore; если клиентов нет — заказ
     * не создаётся.
     */
    private void createTestOrder() {
        try {
            List<Client> clients = DataStore.getClients();
            if (clients == null || clients.isEmpty()) {
                showErrorAlert("Нет клиентов", "Создайте хотя бы одного клиента перед тестовым заказом.");
                return;
            }
            Client client = clients.get(0);

            WorkOrder testOrder = new WorkOrder();
            testOrder.setClient(client);
            testOrder.setClientId(client.getId());
            testOrder.setStatus(WorkOrder.STATUS_CLOSED);
            testOrder.setCreatedDate(DateUtils.formatDateForDB(LocalDate.now()));
            testOrder.setClosedDate(DateUtils.formatDateForDB(LocalDate.now()));
            testOrder.setTotal(5000);
            testOrder.setNotes("Тестовый заказ для проверки графика выручки");

            DataStore.addOrder(testOrder);
            DataStore.save();
            refresh();
        } catch (Exception ex) {
            showErrorAlert("Ошибка", "Не удалось создать тестовый заказ: " + ex.getMessage());
        }
    }

    /**
     * Отрисовка линейного графика выручки на переданном Canvas.
     */
    private void drawLineChartCanvas(Canvas canvas, List<Double> data) {
        // Защита от отрисовки при нулевом размере Canvas
        if (canvas.getWidth() <= 0 || canvas.getHeight() <= 0) return;

        // Отладочный вывод данных графика
        if (data != null) {
            System.out.println("График: количество точек = " + data.size());
            for (int i = 0; i < data.size(); i++) {
                System.out.println("  Точка " + i + ": " + data.get(i));
            }
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (data == null || data.isEmpty()) {
            gc.setFill(javafx.scene.paint.Color.web(TEXT_GRAY));
            gc.setFont(javafx.scene.text.Font.font(13));
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.fillText("Нет данных", canvas.getWidth() / 2, canvas.getHeight() / 2);
            return;
        }

        // Если все значения равны 0.0 — нет закрытых заказов за период
        boolean allZero = data.stream().allMatch(v -> v == 0.0);
        if (allZero) {
            gc.setFill(javafx.scene.paint.Color.web(TEXT_GRAY));
            gc.setFont(javafx.scene.text.Font.font("Segoe UI", 14));
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.fillText("Нет закрытых заказов", canvas.getWidth() / 2, canvas.getHeight() / 2);
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

    /**
     * Создаёт календарь записей на текущий месяц. Дни с записями выделяются
     * розовым кружком с голубой цифрой. Клик по любому дню открывает вкладку
     * «Запись».
     * 
     * @param currentMonth месяц, для которого строится календарь
     * @return карточка VBox с сеткой календаря
     */
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

    /**
     * Модель строки таблицы записей (Appointment). Содержит отображаемые
     * поля записи и ссылку на связанный заказ (если есть).
     */
    public static class AppointmentRow {
        /** Идентификатор записи (заказа). */
        private final String orderId;
        /** Имя клиента. */
        private final String clientName;
        /** Модель автомобиля. */
        private final String carModel;
        /** Название услуги. */
        private final String serviceName;
        /** Дата и время записи. */
        private final String dateTime;
        /** Связанный заказ (может быть null, если запись не привязана к заказу). */
        private final WorkOrder linkedOrder;

        /**
         * Создаёт строку таблицы записей.
         * 
         * @param orderId    идентификатор записи
         * @param clientName имя клиента
         * @param carModel   модель автомобиля
         * @param serviceName название услуги
         * @param dateTime   дата и время
         * @param linkedOrder связанный заказ (или null)
         */
        public AppointmentRow(String orderId, String clientName, String carModel,
                              String serviceName, String dateTime, WorkOrder linkedOrder) {
            this.orderId = orderId;
            this.clientName = clientName;
            this.carModel = carModel;
            this.serviceName = serviceName;
            this.dateTime = dateTime;
            this.linkedOrder = linkedOrder;
        }

        /** @return идентификатор записи */
        public String getOrderId() { return orderId; }
        /** @return имя клиента */
        public String getClientName() { return clientName; }
        /** @return модель автомобиля */
        public String getCarModel() { return carModel; }
        /** @return название услуги */
        public String getServiceName() { return serviceName; }
        /** @return дата и время записи */
        public String getDateTime() { return dateTime; }
        /** @return связанный заказ (может быть null) */
        public WorkOrder getLinkedOrder() { return linkedOrder; }
    }
}