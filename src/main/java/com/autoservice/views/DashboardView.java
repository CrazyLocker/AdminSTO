package com.autoservice.views;

import com.autoservice.*;
import com.autoservice.controllers.ClientController;
import com.autoservice.controllers.OrderController;
import com.autoservice.dialogs.CreateOrderDialog;
import com.autoservice.dialogs.EditClientDialog;
import com.autoservice.dialogs.OrderDetailsDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.Comparator;
import java.util.stream.Collectors;

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

    /** CSS-класс карточки-виджета: цвета берутся из активной темы. */
    private static final String CARD_CLASS = "dashboard-card";
    /** CSS-класс вложенной карточки. */
    private static final String SUB_CARD_CLASS = "dashboard-subcard";
    /** CSS-класс заголовка. */
    private static final String TITLE_CLASS = "dashboard-title";
    /** CSS-класс обычного текста. */
    private static final String TEXT_CLASS = "dashboard-text";
    /** CSS-класс приглушённого текста. */
    private static final String MUTED_CLASS = "dashboard-muted";
    /** CSS-класс акцентного текста. */
    private static final String ACCENT_CLASS = "dashboard-accent";
    /** CSS-класс успешного значения. */
    private static final String SUCCESS_CLASS = "dashboard-success";
    /** CSS-класс предупреждающего значения. */
    private static final String WARNING_CLASS = "dashboard-warning";
    /** CSS-класс опасного значения. */
    private static final String DANGER_CLASS = "dashboard-danger";

    /**
     * Цвета для Canvas-графиков. Canvas рисует пиксели напрямую и не умеет
     * читать CSS, поэтому палитра разрешается из looked-up переменных темы
     * через пробные узлы (см. {@link DashboardPalette}).
     */
    private DashboardPalette palette = DashboardPalette.fallback();

    /**
     * Создаёт подпись с заданным CSS-классом цвета, размером и жирностью.
     *
     * @param text       отображаемый текст
     * @param styleClass CSS-класс цвета из {@code dashboard-custom.css}
     * @param fontSize   размер шрифта в пунктах
     * @param bold       признак жирного начертания
     * @return настроенная метка {@link Label} с переносом текста
     */
    private Label styledLabel(String text, String styleClass, double fontSize, boolean bold) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setFont(javafx.scene.text.Font.font("Segoe UI",
                bold ? javafx.scene.text.FontWeight.BOLD : javafx.scene.text.FontWeight.NORMAL, fontSize));
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
     * Асинхронное обновление дашборда: тяжёлые операции выполняются в фоновом потоке,
     * обновление UI — в JavaFX-потоке.
     */
    private void doRefreshAsync() {
        CompletableFuture.supplyAsync(this::loadDashboardData)
                .thenAcceptAsync(data -> Platform.runLater(() -> {
                    try {
                        buildDashboardUI(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }));
    }

    /**
     * Обновляет содержимое дашборда, если он уже создан. Вызывается при
     * переключении на вкладку «Дашборд» и после изменения данных.
     * Выполняется асинхронно через CompletableFuture, чтобы не блокировать UI-поток.
     */
    public static void refresh() {
        if (instance != null) {
            instance.doRefreshAsync();
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

        // Класс ограничивает правила dashboard-custom.css только дашбордом,
        // чтобы они не перекрашивали GridPane других вкладок и диалогов.
        getStyleClass().add("dashboard-view");

        // Dashboard CSS теперь управляется ThemeManager — не загружаем здесь

        gridPane = new GridPane();
        gridPane.getStyleClass().add("dashboard-grid");
        gridPane.setPadding(new Insets(24));
        gridPane.setHgap(20);
        gridPane.setVgap(20);
        gridPane.setAlignment(Pos.TOP_CENTER);

        setContent(gridPane);
        setFitToWidth(true);
        setFitToHeight(true);

        // Первичная отрисовка — отложена, чтобы не блокировать создание UI
        javafx.application.Platform.runLater(() -> doRefreshAsync());
    }

    // ==================== ОБНОВЛЕНИЕ ====================

    /**
     * Загружает все данные для дашборда в фоновом потоке.
     */
    private DashboardData loadDashboardData() {
        List<WorkOrder> orders = DataStore.getOrders();
        List<Client> clients = DataStore.getClients();
        List<Appointment> appointments = DataStore.getAppointments();
        List<SparePart> spareParts = DataStore.getSpareParts();

        double totalRevenue = calculateTotalRevenue(orders);
        List<Double> monthlyRevenue = calculateMonthlyRevenue(orders);
        int activeOrders = calculateActiveOrders(orders);
        int completedOrders = calculateCompletedOrders(orders);
        int totalOrders = orders.size();
        int totalClients = clients.size();
        int totalAppointments = appointments.size();
        String formattedTotalRevenue = currencyFormat.format(totalRevenue);

        // Новые вычисления
        Map<String, Integer> topServices = calculateTopServices(orders);
        List<SparePart> lowStockParts = calculateLowStockParts(spareParts);
        double averageCheck = calculateAverageCheck(orders, completedOrders);
        Map<String, Integer> masterLoad = calculateMasterLoad(appointments);
        int todayAppointments = calculateTodayAppointments(appointments);
        Map<String, Integer> ordersByDayOfWeek = calculateOrdersByDayOfWeek(orders);
        double monthForecast = calculateMonthForecast(orders);
        int newClientsThisMonth = calculateNewClientsThisMonth(clients);
        String topClient = calculateTopClient(orders);

        return new DashboardData(orders, clients, appointments, spareParts,
                totalRevenue, monthlyRevenue, activeOrders, completedOrders,
                totalOrders, totalClients, totalAppointments, formattedTotalRevenue,
                topServices, lowStockParts, averageCheck, masterLoad,
                todayAppointments, ordersByDayOfWeek, monthForecast,
                newClientsThisMonth, topClient);
    }

    /**
     * Строит UI дашборда на основе загруженных данных.
     * Вызывается в JavaFX-потоке.
     */
    private void buildDashboardUI(DashboardData data) {
        long start = System.currentTimeMillis();
        gridPane.getChildren().clear();
        gridPane.getColumnConstraints().clear();
        palette = DashboardPalette.resolve(gridPane);

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
        VBox donutCard = createBudgetDonutChart(data.totalRevenue, data.totalRevenue * 1.3);
        leftColumn.getChildren().add(donutCard);

        // 2. Записей на сегодня
        VBox todayWidget = createTodayAppointmentsWidget(data.todayAppointments);
        leftColumn.getChildren().add(todayWidget);

        // 3. Календарь
        VBox calendarCard = createAppointmentCalendar(LocalDate.now(), data.appointments);
        calendarCard.setMaxWidth(280);
        leftColumn.getChildren().add(calendarCard);

        // 4. Загрузка мастеров
        VBox masterChart = createMasterLoadChart(data.masterLoad);
        leftColumn.getChildren().add(masterChart);

        // ====== ЦЕНТРАЛЬНАЯ КОЛОНКА ======
        VBox centerColumn = new VBox(16);
        centerColumn.setAlignment(Pos.TOP_CENTER);

        // 1. График выручки
        VBox lineCard = createLineChart("Динамика выручки", data.monthlyRevenue);
        centerColumn.getChildren().add(lineCard);

        // 2. ТОП-5 услуг
        VBox topServicesCard = createTopServicesChart(data.topServices);
        centerColumn.getChildren().add(topServicesCard);

        // 3. Общая статистика
        VBox extraInfo = new VBox(10);
        extraInfo.setAlignment(Pos.CENTER_LEFT);
        extraInfo.setPadding(new Insets(10));
        extraInfo.getStyleClass().add(SUB_CARD_CLASS);

        Label infoTitle = styledLabel("Общая статистика", TITLE_CLASS, 13, true);
        extraInfo.getChildren().add(infoTitle);

        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER);
        statsRow.getChildren().addAll(
                createStatColumn("Заказов", String.valueOf(data.totalOrders), ACCENT_CLASS),
                createStatColumn("Клиентов", String.valueOf(data.totalClients), ACCENT_CLASS),
                createStatColumn("В работе", String.valueOf(data.activeOrders), DANGER_CLASS)
        );
        extraInfo.getChildren().add(statsRow);

        // 4. Средний чек и прогноз
        HBox statsRow2 = new HBox(20);
        statsRow2.setAlignment(Pos.CENTER);
        statsRow2.getChildren().addAll(
                createStatColumn("Средний чек", String.format("%,.0f ₽", data.averageCheck), SUCCESS_CLASS),
                createStatColumn("Новых клиентов", String.valueOf(data.newClientsThisMonth), WARNING_CLASS),
                createStatColumn("Прогноз на месяц", String.format("%,.0f ₽", data.monthForecast), ACCENT_CLASS)
        );
        extraInfo.getChildren().add(statsRow2);

        // 5. Лучший клиент
        if (data.topClient != null && !data.topClient.isEmpty()) {
            HBox topClientRow = new HBox(10);
            topClientRow.setAlignment(Pos.CENTER_LEFT);
            topClientRow.setPadding(new Insets(4, 0, 0, 0));
            Label topClientLabel = styledLabel("🏆 Лучший клиент: " + data.topClient, WARNING_CLASS, 12, true);
            topClientRow.getChildren().add(topClientLabel);
            extraInfo.getChildren().add(topClientRow);
        }

        centerColumn.getChildren().add(extraInfo);

        // ====== ПРАВАЯ КОЛОНКА ======
        VBox rightColumn = new VBox(16);
        rightColumn.setAlignment(Pos.TOP_CENTER);

        // 1. Диаграмма статусов
        VBox statusDonut = createStatusDonutChart(data.activeOrders, data.completedOrders, data.totalOrders);
        rightColumn.getChildren().add(statusDonut);

        // 2. Общая выручка
        VBox revenueMiniCard = createRevenueMiniCard(data.totalRevenue);
        rightColumn.getChildren().add(revenueMiniCard);

        // 3. Запчасти с низким остатком
        VBox lowStockCard = createLowStockWidget(data.lowStockParts);
        rightColumn.getChildren().add(lowStockCard);

        // 4. Динамика по дням недели
        VBox dayOfWeekCard = createOrdersByDayOfWeekChart(data.ordersByDayOfWeek);
        rightColumn.getChildren().add(dayOfWeekCard);

        gridPane.add(leftColumn, 0, 0);
        gridPane.add(centerColumn, 1, 0);
        gridPane.add(rightColumn, 2, 0);

        long duration = System.currentTimeMillis() - start;
        if (duration > 300) {
            System.out.println("⚠️ Медленная операция в DashboardView.buildDashboardUI: " + duration + " мс");
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

    // ==================== СТАТИЧЕСКИЕ МЕТОДЫ ВЫЧИСЛЕНИЙ ====================

    /**
     * Вычисляет общую выручку (сумма закрытых заказов).
     */
    private static double calculateTotalRevenue(List<WorkOrder> orders) {
        double total = 0;
        for (WorkOrder order : orders) {
            if (WorkOrder.STATUS_CLOSED.equals(order.getStatus())) {
                total += order.getTotal();
            }
        }
        return total;
    }

    /**
     * Вычисляет динамику выручки за последние 6 месяцев.
     */
    private static List<Double> calculateMonthlyRevenue(List<WorkOrder> orders) {
        HashMap<YearMonth, Double> revenueByMonth = new HashMap<>();
        int totalClosed = 0;

        for (WorkOrder order : orders) {
            if (!WorkOrder.STATUS_CLOSED.equals(order.getStatus())) {
                continue;
            }
            totalClosed++;

            String createdDate = order.getCreatedDate();
            if (createdDate == null || createdDate.trim().isEmpty()) {
                continue;
            }

            LocalDate orderDate = DateUtils.parseDate(createdDate);
            if (orderDate == null) {
                continue;
            }
            YearMonth ym = YearMonth.from(orderDate);
            revenueByMonth.merge(ym, order.getTotal(), Double::sum);
        }

        List<Double> monthlyRevenue = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            monthlyRevenue.add(revenueByMonth.getOrDefault(current.minusMonths(i), 0.0));
        }

        return monthlyRevenue;
    }

    /**
     * Подсчитывает количество заказов со статусом «В работе».
     */
    private static int calculateActiveOrders(List<WorkOrder> orders) {
        int count = 0;
        for (WorkOrder order : orders) {
            if (WorkOrder.STATUS_IN_PROGRESS.equals(order.getStatus())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Подсчитывает количество заказов со статусом «Закрыт».
     */
    private static int calculateCompletedOrders(List<WorkOrder> orders) {
        int count = 0;
        for (WorkOrder order : orders) {
            if (WorkOrder.STATUS_CLOSED.equals(order.getStatus())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Вычисляет ТОП-5 услуг по частоте заказов.
     */
    private static Map<String, Integer> calculateTopServices(List<WorkOrder> orders) {
        Map<String, Integer> serviceCount = new HashMap<>();
        for (WorkOrder order : orders) {
            for (String service : order.getServices()) {
                serviceCount.merge(service, 1, Integer::sum);
            }
        }
        return serviceCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Вычисляет запчасти с низким остатком.
     */
    private static List<SparePart> calculateLowStockParts(List<SparePart> spareParts) {
        return spareParts.stream()
                .filter(p -> p.getStock() < p.getMinStock())
                .sorted(Comparator.comparingDouble(SparePart::getStock))
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * Вычисляет средний чек.
     */
    private static double calculateAverageCheck(List<WorkOrder> orders, int completedOrders) {
        if (completedOrders == 0) return 0;
        double total = calculateTotalRevenue(orders);
        return total / completedOrders;
    }

    /**
     * Вычисляет загрузку мастеров.
     */
    private static Map<String, Integer> calculateMasterLoad(List<Appointment> appointments) {
        Map<String, Integer> masterCount = new HashMap<>();
        for (Appointment a : appointments) {
            String master = a.getMasterName();
            if (master != null && !master.isEmpty()) {
                masterCount.merge(master, 1, Integer::sum);
            }
        }
        return masterCount;
    }

    /**
     * Вычисляет количество записей на сегодня.
     */
    private static int calculateTodayAppointments(List<Appointment> appointments) {
        String today = DateUtils.formatDateForDB(LocalDate.now());
        int count = 0;
        for (Appointment a : appointments) {
            if (today.equals(a.getDate())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Вычисляет распределение заказов по дням недели.
     */
    private static Map<String, Integer> calculateOrdersByDayOfWeek(List<WorkOrder> orders) {
        Map<String, Integer> dayCount = new HashMap<>();
        String[] days = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        for (String day : days) {
            dayCount.put(day, 0);
        }

        for (WorkOrder order : orders) {
            String createdDate = order.getCreatedDate();
            if (createdDate == null || createdDate.trim().isEmpty()) {
                continue;
            }
            LocalDate date = DateUtils.parseDate(createdDate);
            if (date != null) {
                int dayOfWeek = date.getDayOfWeek().getValue() - 1; // 0 = Пн
                String dayName = days[dayOfWeek];
                dayCount.merge(dayName, 1, Integer::sum);
            }
        }
        return dayCount;
    }

    /**
     * Вычисляет прогноз выручки на месяц.
     */
    private static double calculateMonthForecast(List<WorkOrder> orders) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate today = LocalDate.now();
        int daysInMonth = currentMonth.lengthOfMonth();
        int daysPassed = today.getDayOfMonth();

        double monthRevenue = 0;
        for (WorkOrder order : orders) {
            if (!WorkOrder.STATUS_CLOSED.equals(order.getStatus())) {
                continue;
            }
            String createdDate = order.getCreatedDate();
            if (createdDate == null || createdDate.trim().isEmpty()) {
                continue;
            }
            LocalDate date = DateUtils.parseDate(createdDate);
            if (date != null && !date.isBefore(monthStart) && !date.isAfter(today)) {
                monthRevenue += order.getTotal();
            }
        }

        if (daysPassed == 0) return 0;
        double dailyAverage = monthRevenue / daysPassed;
        return dailyAverage * daysInMonth;
    }

    /**
     * Вычисляет количество новых клиентов в текущем месяце.
     *
     * <p>Даты регистрации клиента в схеме БД нет, поэтому «новым» считается
     * клиент, чей самый первый заказ создан в текущем месяце.</p>
     */
    private static int calculateNewClientsThisMonth(List<Client> clients) {
        YearMonth currentMonth = YearMonth.now();
        Map<Integer, LocalDate> firstOrderByClient = new HashMap<>();
        for (WorkOrder order : DataStore.getOrders()) {
            int clientId = order.getClientId() > 0 ? order.getClientId()
                    : (order.getClient() != null ? order.getClient().getId() : 0);
            if (clientId <= 0) {
                continue;
            }
            LocalDate date = DateUtils.parseDate(order.getCreatedDate());
            if (date == null) {
                continue;
            }
            firstOrderByClient.merge(clientId, date, (a, b) -> a.isBefore(b) ? a : b);
        }
        int count = 0;
        for (Client client : clients) {
            LocalDate firstOrder = firstOrderByClient.get(client.getId());
            if (firstOrder != null && YearMonth.from(firstOrder).equals(currentMonth)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Вычисляет самого доходного клиента.
     */
    private static String calculateTopClient(List<WorkOrder> orders) {
        Map<Integer, Double> clientRevenue = new HashMap<>();
        Map<Integer, String> clientNames = new HashMap<>();

        for (WorkOrder order : orders) {
            if (!WorkOrder.STATUS_CLOSED.equals(order.getStatus())) {
                continue;
            }
            Client client = order.getClient();
            if (client == null) continue;
            int clientId = client.getId();
            String fullName = (client.getLastName() != null && !client.getLastName().isEmpty())
                    ? client.getLastName() + " " + client.getName()
                    : client.getName();
            clientNames.put(clientId, fullName);
            clientRevenue.merge(clientId, order.getTotal(), Double::sum);
        }

        return clientRevenue.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> clientNames.get(entry.getKey()) + " (" + String.format("%,.0f ₽", entry.getValue()) + ")")
                .orElse(null);
    }

    // ==================== ВИДЖЕТЫ ====================

    /**
     * Создаёт виджет "Записей на сегодня".
     */
    private VBox createTodayAppointmentsWidget(int count) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        card.getStyleClass().add(CARD_CLASS);

        Label title = styledLabel("Записей на сегодня", MUTED_CLASS, 11, false);
        Label value = styledLabel(String.valueOf(count), ACCENT_CLASS, 28, true);

        card.getChildren().addAll(title, value);
        return card;
    }

    /**
     * Создаёт виджет "Загрузка мастеров" (кольцевая диаграмма).
     */
    private VBox createMasterLoadChart(Map<String, Integer> masterLoad) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        card.getStyleClass().add(CARD_CLASS);

        Label title = styledLabel("Загрузка мастеров", TITLE_CLASS, 13, true);

        Canvas canvas = new Canvas(160, 160);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        if (masterLoad.isEmpty()) {
            gc.setFill(palette.muted());
            gc.setFont(Font.font("Segoe UI", 13));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("Нет данных", 80, 85);
            card.getChildren().addAll(title, canvas);
            return card;
        }

        double total = masterLoad.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            gc.setFill(palette.muted());
            gc.setFont(Font.font("Segoe UI", 13));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("Нет данных", 80, 85);
            card.getChildren().addAll(title, canvas);
            return card;
        }

        double cx = 80, cy = 80, radius = 56;
        double startAngle = 90;

        Color[] colors = palette.series();

        int colorIndex = 0;
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(masterLoad.entrySet());
        entries.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        VBox legendBox = new VBox(4);
        legendBox.setAlignment(Pos.CENTER_LEFT);

        for (Map.Entry<String, Integer> entry : entries) {
            double percentage = (entry.getValue() / total) * 360;
            Color color = colors[colorIndex % colors.length];
            gc.setStroke(color);
            gc.setLineWidth(12);
            gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, startAngle, -percentage, javafx.scene.shape.ArcType.OPEN);
            startAngle -= percentage;

            // Легенда
            HBox legendRow = new HBox(8);
            legendRow.setAlignment(Pos.CENTER_LEFT);
            Circle dot = new Circle(5);
            dot.setFill(color);
            Label legendLabel = styledLabel(entry.getKey() + " (" + entry.getValue() + ")", MUTED_CLASS, 10, false);
            legendRow.getChildren().addAll(dot, legendLabel);
            legendBox.getChildren().add(legendRow);

            colorIndex++;
        }

        // Центр
        gc.setFill(palette.text());
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(String.valueOf((int) total), cx, cy - 4);

        gc.setFill(palette.muted());
        gc.setFont(Font.font("Segoe UI", 10));
        gc.fillText("всего записей", cx, cy + 18);

        HBox chartWithLegend = new HBox(12, canvas, legendBox);
        chartWithLegend.setAlignment(Pos.CENTER);

        card.getChildren().addAll(title, chartWithLegend);
        return card;
    }

    /**
     * Создаёт виджет "ТОП-5 услуг" (горизонтальная гистограмма).
     */
    private VBox createTopServicesChart(Map<String, Integer> topServices) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        card.getStyleClass().add(CARD_CLASS);

        Label title = styledLabel("ТОП-5 услуг", TITLE_CLASS, 13, true);
        card.getChildren().add(title);

        if (topServices.isEmpty()) {
            Label empty = styledLabel("Нет данных", MUTED_CLASS, 12, false);
            card.getChildren().add(empty);
            return card;
        }

        int maxCount = topServices.values().stream().max(Integer::compareTo).orElse(1);

        VBox barsBox = new VBox(6);
        barsBox.setAlignment(Pos.CENTER_LEFT);

        int index = 0;
        Color[] barColors = palette.series();

        for (Map.Entry<String, Integer> entry : topServices.entrySet()) {
            HBox barRow = new HBox(8);
            barRow.setAlignment(Pos.CENTER_LEFT);

            String serviceName = entry.getKey();
            if (serviceName.length() > 20) {
                serviceName = serviceName.substring(0, 18) + "...";
            }
            Label nameLabel = styledLabel(serviceName, TEXT_CLASS, 11, false);
            nameLabel.setPrefWidth(100);

            int count = entry.getValue();
            double barWidth = (count / (double) maxCount) * 120;

            StackPane barContainer = new StackPane();
            barContainer.setPrefWidth(120);
            barContainer.setPrefHeight(14);

            javafx.scene.shape.Rectangle bar = new javafx.scene.shape.Rectangle(barWidth, 14);
            bar.setFill(barColors[index % barColors.length]);
            bar.setArcWidth(6);
            bar.setArcHeight(6);

            Label countLabel = styledLabel(String.valueOf(count), MUTED_CLASS, 10, false);
            countLabel.setPrefWidth(25);

            barContainer.getChildren().add(bar);
            barRow.getChildren().addAll(nameLabel, barContainer, countLabel);
            barsBox.getChildren().add(barRow);
            index++;
        }

        card.getChildren().add(barsBox);
        return card;
    }

    /**
     * Создаёт виджет "Запчасти с низким остатком".
     */
    private VBox createLowStockWidget(List<SparePart> lowStockParts) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        card.getStyleClass().add(CARD_CLASS);

        Label title = styledLabel("⚠️ Низкий остаток запчастей", WARNING_CLASS, 13, true);
        card.getChildren().add(title);

        if (lowStockParts.isEmpty()) {
            Label empty = styledLabel("✅ Все запчасти в норме", SUCCESS_CLASS, 12, false);
            card.getChildren().add(empty);
            return card;
        }

        VBox listBox = new VBox(4);
        listBox.setAlignment(Pos.CENTER_LEFT);

        for (SparePart part : lowStockParts) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            Color color = part.getStock() < part.getMinStock() * 0.5 ? palette.danger() : palette.warning();
            Circle indicator = new Circle(4);
            indicator.setFill(color);

            String text = part.getName() + " (" + String.format("%.0f", part.getStock()) + "/" + String.format("%.0f", part.getMinStock()) + " " + part.getUnitType() + ")";
            Label label = styledLabel(text, MUTED_CLASS, 11, false);
            row.getChildren().addAll(indicator, label);
            listBox.getChildren().add(row);
        }

        card.getChildren().add(listBox);
        return card;
    }

    /**
     * Создаёт виджет "Динамика по дням недели".
     */
    private VBox createOrdersByDayOfWeekChart(Map<String, Integer> dayData) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        card.getStyleClass().add(CARD_CLASS);

        Label title = styledLabel("Заказы по дням недели", TITLE_CLASS, 13, true);
        card.getChildren().add(title);

        Canvas canvas = new Canvas(200, 140);
        canvas.widthProperty().bind(card.widthProperty().subtract(32));
        canvas.heightProperty().bind(canvas.widthProperty().multiply(0.6));

        drawDayOfWeekChart(canvas, dayData);

        card.getChildren().add(canvas);
        return card;
    }

    /**
     * Рисует столбчатую диаграмму по дням недели.
     */
    private void drawDayOfWeekChart(Canvas canvas, Map<String, Integer> dayData) {
        if (canvas.getWidth() <= 0 || canvas.getHeight() <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        String[] days = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        int max = dayData.values().stream().max(Integer::compareTo).orElse(1);
        if (max == 0) max = 1;

        double width = canvas.getWidth();
        double height = canvas.getHeight();
        double padding = 20;
        double chartW = width - padding * 2;
        double chartH = height - padding * 2;
        double barWidth = chartW / days.length * 0.6;
        double gap = chartW / days.length;

        for (int i = 0; i < days.length; i++) {
            int count = dayData.getOrDefault(days[i], 0);
            double barHeight = (count / (double) max) * chartH;
            double x = padding + i * gap + (gap - barWidth) / 2;
            double y = padding + chartH - barHeight;

            // Бар
            LinearGradient gradient = new LinearGradient(0, 0, 0, 1, true,
                    CycleMethod.NO_CYCLE,
                    new Stop(0, palette.accent()),
                    new Stop(1, palette.accent().darker()));
            gc.setFill(gradient);
            gc.fillRoundRect(x, y, barWidth, barHeight, 4, 4);

            // Подпись дня
            gc.setFill(palette.muted());
            gc.setFont(Font.font("Segoe UI", 10));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(days[i], x + barWidth / 2, padding + chartH + 16);

            // Значение
            if (count > 0) {
                gc.setFill(palette.text());
                gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
                gc.fillText(String.valueOf(count), x + barWidth / 2, y - 6);
            }
        }
    }

    // ==================== СУЩЕСТВУЮЩИЕ ВИДЖЕТЫ (С ИЗМЕНЕНИЯМИ) ====================

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
        card.getStyleClass().add(CARD_CLASS);

        Label title = styledLabel("Выручка", TITLE_CLASS, 14, true);
        Label subtitle = styledLabel("Потрачено / Бюджет", MUTED_CLASS, 11, false);

        Canvas canvas = new Canvas(180, 180);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double cx = 90, cy = 90, radius = 66;
        double progress = Math.min(spent / total, 1.0);
        double angle = 360 * progress;

        gc.setStroke(palette.grid());
        gc.setLineWidth(16);
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, 360, javafx.scene.shape.ArcType.OPEN);

        // Градиент для выручки в цветах текущей темы
        javafx.scene.paint.Color startColor = palette.accent();
        javafx.scene.paint.Color endColor = palette.accent().darker();
        javafx.scene.paint.Stop[] stops = {new javafx.scene.paint.Stop(0, startColor), new javafx.scene.paint.Stop(1, endColor)};
        javafx.scene.paint.LinearGradient gradient = new javafx.scene.paint.LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops);
        gc.setStroke(gradient);
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, -angle, javafx.scene.shape.ArcType.OPEN);

        gc.setFill(palette.text());
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 23));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText(String.format("%,.0f ₽", spent), cx, cy - 6);

        gc.setFill(palette.muted());
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", 12));
        gc.fillText("Общий доход", cx, cy + 20);

        card.getChildren().addAll(title, subtitle, canvas);
        return card;
    }

    /**
     * Создаёт круговую диаграмму распределения заказов по статусам.
     *
     * @param active количество активных заказов
     * @param closed количество закрытых заказов
     * @param total общее количество заказов
     * @return карточка VBox с диаграммой статусов и легендой
     */
    private VBox createStatusDonutChart(int active, int closed, int total) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        card.getStyleClass().add(CARD_CLASS);

        Label title = styledLabel("Статусы заказов", TITLE_CLASS, 13, true);

        Canvas canvas = new Canvas(160, 160);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double cx = 80, cy = 80, radius = 58;
        double activeAngle = total > 0 ? 360 * ((double)active / total) : 0;
        double closedAngle = total > 0 ? 360 * ((double)closed / total) : 0;

        // Фон (серая дуга для "Новый" или без статуса)
        gc.setStroke(palette.grid());
        gc.setLineWidth(14);
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, 360, javafx.scene.shape.ArcType.OPEN);

        // Сектор "В работе" (розовый)
        if (activeAngle > 0) {
            gc.setStroke(palette.danger());
            gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, -activeAngle, javafx.scene.shape.ArcType.OPEN);
        }
        // Сектор "Закрыт" (синий) — начинается там, где закончился розовый
        if (closedAngle > 0) {
            gc.setStroke(palette.accent());
            gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90 - activeAngle, -closedAngle, javafx.scene.shape.ArcType.OPEN);
        }

        gc.setFill(palette.text());
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 19));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText(String.valueOf(total), cx, cy - 6);

        gc.setFill(palette.muted());
        gc.setFont(javafx.scene.text.Font.font("Segoe UI", 10));
        gc.fillText("Всего заказов", cx, cy + 18);

        // ====== Легенда ======
        int newCount = Math.max(total - active - closed, 0);

        VBox legendBox = new VBox(6);
        legendBox.setAlignment(Pos.CENTER_LEFT);
        legendBox.setPadding(new Insets(4, 0, 0, 0));

        legendBox.getChildren().addAll(
                createLegendRow(palette.danger(), "В работе: " + active),
                createLegendRow(palette.accent(), "Закрыт: " + closed),
                createLegendRow(palette.grid(), "Новый: " + newCount)
        );

        HBox chartWithLegend = new HBox(12, canvas, legendBox);
        chartWithLegend.setAlignment(Pos.CENTER);

        card.getChildren().addAll(title, chartWithLegend);
        return card;
    }

    /**
     * Строка легенды: цветной кружок + текст.
     */
    private HBox createLegendRow(Color color, String text) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(6);
        dot.setFill(color);
        Label lbl = styledLabel(text, MUTED_CLASS, 11, false);
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
        card.getStyleClass().add(CARD_CLASS);

        Label title = styledLabel("Общая выручка", TITLE_CLASS, 13, true);
        Label amount = styledLabel(String.format("%,.0f ₽", revenue), ACCENT_CLASS, 21, true);

        // Маленькая декоративная линия (просто красоты ради)
        javafx.scene.shape.Line line = new javafx.scene.shape.Line(0, 0, 80, 0);
        line.setStroke(palette.accent());
        line.setStrokeWidth(2);

        // Декоративный Canvas-индикатор, чтобы плитка визуально совпадала по высоте с календарём
        Canvas miniCanvas = new Canvas(120, 120);
        GraphicsContext gc = miniCanvas.getGraphicsContext2D();
        double cx = 60, cy = 60, radius = 44;
        gc.setStroke(palette.grid());
        gc.setLineWidth(12);
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, 360, javafx.scene.shape.ArcType.OPEN);
        javafx.scene.paint.LinearGradient grad = new javafx.scene.paint.LinearGradient(0, 0, 1, 1, true,
                CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, palette.accent()),
                new javafx.scene.paint.Stop(1, palette.accent().darker()));
        gc.setStroke(grad);
        gc.setLineWidth(12);
        gc.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 90, -270, javafx.scene.shape.ArcType.OPEN);

        card.getChildren().addAll(title, amount, line, miniCanvas);
        return card;
    }

    // Вспомогательный метод для центральной колонки
    private VBox createStatColumn(String label, String value, String styleClass) {
        VBox v = new VBox(2);
        v.setAlignment(Pos.CENTER);
        Label val = styledLabel(value, styleClass, 19, true);
        Label lbl = styledLabel(label, MUTED_CLASS, 10, false);
        v.getChildren().addAll(val, lbl);
        return v;
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
        card.getStyleClass().add(CARD_CLASS);

        Label title = styledLabel(titleText, TITLE_CLASS, 13, true);

        // ====== Переключатель месяца ======
        java.time.YearMonth currentMonth = java.time.YearMonth.now();
        java.time.YearMonth[] selectedMonth = {currentMonth};

        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("LLLL yyyy", new Locale("ru"));
        Label monthLabel = styledLabel(selectedMonth[0].format(monthFmt), TEXT_CLASS, 13, true);
        monthLabel.setMinWidth(120);
        monthLabel.setAlignment(Pos.CENTER);

        Button leftBtn = new Button("←");
        leftBtn.getStyleClass().add("dashboard-nav-btn");
        leftBtn.setPrefSize(36, 30);

        Button rightBtn = new Button("→");
        rightBtn.getStyleClass().add("dashboard-nav-btn");
        rightBtn.setPrefSize(36, 30);

        // Кнопка создания тестового заказа для проверки графика выручки
        Button testOrderBtn = new Button("Создать тестовый заказ");
        testOrderBtn.getStyleClass().add("dashboard-test-btn");
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
            List<Double> monthData = calculateMonthlyRevenueForMonth(DataStore.getOrders(), selectedMonth[0]);
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
            gc.setFill(palette.muted());
            gc.setFont(javafx.scene.text.Font.font(13));
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.fillText("Нет данных", canvas.getWidth() / 2, canvas.getHeight() / 2);
            return;
        }

        // Если все значения равны 0.0 — нет закрытых заказов за период
        boolean allZero = data.stream().allMatch(v -> v == 0.0);
        if (allZero) {
            gc.setFill(palette.muted());
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

        // Цвета линии и заливки берутся из палитры текущей темы
        javafx.scene.paint.Color lineColor = palette.accent();
        javafx.scene.paint.Color areaTop = palette.accent();
        javafx.scene.paint.Color areaBottom = palette.cardBackground();

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
        javafx.scene.paint.LinearGradient areaGradient = new javafx.scene.paint.LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);

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
        gc.setStroke(palette.grid());
        gc.setLineWidth(0.5);
        for (int i = 0; i < 5; i++) {
            double y = padding + (graphH / 4) * i;
            gc.strokeLine(padding, y, canvas.getWidth() - padding, y);
        }
    }

    // ==================== ВИДЖЕТ: КАЛЕНДАРЬ ====================

    /**
     * Создаёт календарь записей на текущий месяц.
     *
     * @param currentMonth месяц, для которого строится календарь
     * @param appointments список всех записей (фильтрация внутри метода)
     * @return карточка VBox с сеткой календаря
     */
    private VBox createAppointmentCalendar(LocalDate currentMonth, List<Appointment> appointments) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(12));
        card.getStyleClass().add(CARD_CLASS);

        Label title = styledLabel("Записи на сервис (" + currentMonth.format(DateTimeFormatter.ofPattern("MMM")) + ")", TITLE_CLASS, 13, true);
        card.getChildren().add(title);

        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        grid.setAlignment(Pos.CENTER);

        // Шрифт дней недели
        String[] days = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = styledLabel(days[i], MUTED_CLASS, 11, true);
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setPrefSize(34, 24);
            grid.add(dayLabel, i, 0);
        }

        // Фильтрация записей для текущего месяца (без вызова DataStore)
        List<LocalDate> appointmentDates = new ArrayList<>();
        for (Appointment a : appointments) {
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
            cell.setPrefSize(34, 34);
            cell.setAlignment(Pos.CENTER);

            Label dayNum = styledLabel(String.valueOf(day), hasAppointment ? ACCENT_CLASS : MUTED_CLASS, 12, hasAppointment);

            if (hasAppointment) {
                javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(13);
                circle.setFill(palette.danger());
                cell.getChildren().addAll(circle, dayNum);
            } else {
                cell.getChildren().add(dayNum);
            }

            cell.setOnMouseClicked(e -> openAppointmentView());
            grid.add(cell, col, row);
            col++;
            if (col >= 7) { col = 0; row++; }
        }

        card.getChildren().add(grid);
        return card;
    }

    // ==================== УСТАРЕВШИЕ МЕТОДЫ (для совместимости) ====================

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

    /**
     * Вычисляет выручку за конкретный месяц.
     */
    private static List<Double> calculateMonthlyRevenueForMonth(List<WorkOrder> orders, YearMonth yearMonth) {
        List<Double> monthlyRevenue = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = yearMonth.minusMonths(i);
            LocalDate monthStart = ym.atDay(1);
            LocalDate monthEnd = ym.atEndOfMonth();

            double total = 0;
            for (WorkOrder order : orders) {
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

    // ==================== ПАЛИТРА CANVAS-ГРАФИКОВ ====================

    /**
     * Палитра Canvas-графиков, разрешённая из looked-up переменных AtlantaFX.
     *
     * <p>Canvas рисует пиксели напрямую и не участвует в CSS, поэтому цвета
     * берутся из тех же переменных темы через пробные узлы: после
     * {@code applyCss()} у узла уже вычислен итоговый {@link Color}.</p>
     */
    private static final class DashboardPalette {

        private final Color text;
        private final Color muted;
        private final Color cardBackground;
        private final Color grid;
        private final Color accent;
        private final Color success;
        private final Color warning;
        private final Color danger;

        private DashboardPalette(Color text, Color muted, Color cardBackground, Color grid,
                                 Color accent, Color success, Color warning, Color danger) {
            this.text = text;
            this.muted = muted;
            this.cardBackground = cardBackground;
            this.grid = grid;
            this.accent = accent;
            this.success = success;
            this.warning = warning;
            this.danger = danger;
        }

        /**
         * Нейтральная палитра на время до применения CSS. В
         * {@link #buildDashboardUI} она заменяется реальной палитрой темы.
         */
        static DashboardPalette fallback() {
            return new DashboardPalette(Color.BLACK, Color.GRAY, Color.WHITE, Color.LIGHTGRAY,
                    Color.BLUE, Color.GREEN, Color.ORANGE, Color.RED);
        }

        /**
         * Разрешает цвета темы через пробные узлы внутри {@code root}.
         * {@code root} должен быть прикреплён к показанной сцене.
         */
        static DashboardPalette resolve(Pane root) {
            return new DashboardPalette(
                    textFill(root, "-color-fg-default", Color.BLACK),
                    textFill(root, "-color-fg-muted", Color.GRAY),
                    background(root, "-color-bg-subtle", Color.WHITE),
                    background(root, "-color-border-subtle", Color.LIGHTGRAY),
                    background(root, "-color-accent-emphasis", Color.BLUE),
                    background(root, "-color-success-emphasis", Color.GREEN),
                    background(root, "-color-warning-emphasis", Color.ORANGE),
                    background(root, "-color-danger-emphasis", Color.RED));
        }

        private static Color textFill(Pane root, String lookup, Color fallback) {
            Label probe = new Label(" ");
            probe.setStyle("-fx-text-fill: " + lookup + ";");
            root.getChildren().add(probe);
            root.applyCss();
            Paint fill = probe.getTextFill();
            root.getChildren().remove(probe);
            return fill instanceof Color color ? color : fallback;
        }

        private static Color background(Pane root, String lookup, Color fallback) {
            Region probe = new Region();
            probe.setStyle("-fx-background-color: " + lookup + ";");
            root.getChildren().add(probe);
            root.applyCss();
            Background background = probe.getBackground();
            Color color = fallback;
            if (background != null && !background.getFills().isEmpty()
                    && background.getFills().get(0).getFill() instanceof Color fill) {
                color = fill;
            }
            root.getChildren().remove(probe);
            return color;
        }

        Color text() {
            return text;
        }

        Color muted() {
            return muted;
        }

        Color cardBackground() {
            return cardBackground;
        }

        Color grid() {
            return grid;
        }

        Color accent() {
            return accent;
        }

        Color success() {
            return success;
        }

        Color warning() {
            return warning;
        }

        Color danger() {
            return danger;
        }

        /** Цвета серий для диаграмм: акценты текущей темы. */
        Color[] series() {
            return new Color[]{accent, success, warning, danger, muted, text};
        }
    }

    // ==================== КЛАСС ДАННЫХ ДАШБОРДА ====================

    /**
     * Контейнер для всех данных, необходимых для отображения дашборда.
     * Все вычисления выполняются в фоновом потоке, затем данные передаются
     * в buildDashboardUI() для построения UI в JavaFX-потоке.
     */
    public static class DashboardData {
        public final List<WorkOrder> orders;
        public final List<Client> clients;
        public final List<Appointment> appointments;
        public final List<SparePart> spareParts;
        public final double totalRevenue;
        public final List<Double> monthlyRevenue;
        public final int activeOrders;
        public final int completedOrders;
        public final int totalOrders;
        public final int totalClients;
        public final int totalAppointments;
        public final String formattedTotalRevenue;

        // Новые поля
        public final Map<String, Integer> topServices;
        public final List<SparePart> lowStockParts;
        public final double averageCheck;
        public final Map<String, Integer> masterLoad;
        public final int todayAppointments;
        public final Map<String, Integer> ordersByDayOfWeek;
        public final double monthForecast;
        public final int newClientsThisMonth;
        public final String topClient;

        public DashboardData(List<WorkOrder> orders, List<Client> clients,
                             List<Appointment> appointments, List<SparePart> spareParts,
                             double totalRevenue, List<Double> monthlyRevenue,
                             int activeOrders, int completedOrders,
                             int totalOrders, int totalClients,
                             int totalAppointments, String formattedTotalRevenue,
                             Map<String, Integer> topServices, List<SparePart> lowStockParts,
                             double averageCheck, Map<String, Integer> masterLoad,
                             int todayAppointments, Map<String, Integer> ordersByDayOfWeek,
                             double monthForecast, int newClientsThisMonth, String topClient) {
            this.orders = orders;
            this.clients = clients;
            this.appointments = appointments;
            this.spareParts = spareParts;
            this.totalRevenue = totalRevenue;
            this.monthlyRevenue = monthlyRevenue;
            this.activeOrders = activeOrders;
            this.completedOrders = completedOrders;
            this.totalOrders = totalOrders;
            this.totalClients = totalClients;
            this.totalAppointments = totalAppointments;
            this.formattedTotalRevenue = formattedTotalRevenue;
            this.topServices = topServices;
            this.lowStockParts = lowStockParts;
            this.averageCheck = averageCheck;
            this.masterLoad = masterLoad;
            this.todayAppointments = todayAppointments;
            this.ordersByDayOfWeek = ordersByDayOfWeek;
            this.monthForecast = monthForecast;
            this.newClientsThisMonth = newClientsThisMonth;
            this.topClient = topClient;
        }
    }
}