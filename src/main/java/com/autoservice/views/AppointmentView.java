package com.autoservice.views;

import com.autoservice.AppConstants;
import com.autoservice.Appointment;
import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.DateUtils;
import com.autoservice.Service;
import com.autoservice.SparePart;
import com.autoservice.utils.Formatters;
import com.autoservice.WorkOrder;
import com.autoservice.controllers.ClientController;
import com.autoservice.controllers.OrderController;
import com.autoservice.controllers.ServicePanelController;
import com.autoservice.controllers.SparePartPanelController;
import com.autoservice.controllers.StockPanelController;
import com.autoservice.dialogs.CreateOrderDialog;
import com.autoservice.dialogs.EditClientDialog;
import com.autoservice.dialogs.EditOrderDialog;
import com.autoservice.dialogs.PrintOrderDialog;
import com.autoservice.services.TableStateManager;
import com.autoservice.services.WindowStateManager;
import com.autoservice.utils.TooltipHelper;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;
import javafx.scene.input.Dragboard;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Scene;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class AppointmentView {

    // TIME_SLOTS и MASTERS перенесены в AppConstants
    private static String[] SERVICES;

    private static DatePicker datePicker;
    private static ScrollPane scrollPane;
    private static GridPane scheduleGrid;
    private static Label selectedDateLabel;
    private static ToggleGroup viewToggle;
    private static String currentView = "week";

    private static final Locale RU_LOCALE = new Locale.Builder().setLanguage("ru").build();
    private static final DateTimeFormatter HEADER_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy 'г.'", RU_LOCALE);
    private static final DateTimeFormatter WEEK_FORMATTER = DateTimeFormatter.ofPattern("d MMM", RU_LOCALE);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("LLLL", RU_LOCALE);
    /** Ширина колонки времени в недельной сетке, в процентах от ширины таблицы. */
    private static final double TIME_COLUMN_WIDTH_PERCENT = 6.0;

    public static VBox create() {
        SERVICES = DataStore.getServices().stream()
                .map(Service::getName)
                .toArray(String[]::new);

        VBox root = new VBox(15);
        root.getStyleClass().addAll("main-container", "appointment-calendar");
        applyCalendarStylesheet(root);

        // Верхняя панель
        HBox topPanel = new HBox(15);
        topPanel.setAlignment(Pos.CENTER_LEFT);
        topPanel.getStyleClass().add("appointment-top-panel");

        // Переключатель вида
        viewToggle = new ToggleGroup();
        RadioButton weekView = new RadioButton("Неделя");
        RadioButton monthView = new RadioButton("Месяц");

        weekView.setToggleGroup(viewToggle);
        monthView.setToggleGroup(viewToggle);
        // Переключатель должен отражать текущий вид: вкладка пересоздаётся при
        // каждом переключении, а выбор пользователя («Месяц») сохраняется в currentView.
        if ("month".equals(currentView)) {
            monthView.setSelected(true);
        } else {
            weekView.setSelected(true);
        }

        weekView.getStyleClass().add("view-radio");
        monthView.getStyleClass().add("view-radio");

        weekView.setOnAction(e -> {
            currentView = "week";
            loadWeekView();
        });
        monthView.setOnAction(e -> {
            currentView = "month";
            loadMonthView();
        });

        HBox viewBox = new HBox(5, weekView, monthView);
        viewBox.getStyleClass().add("view-box");

        selectedDateLabel = new Label();
        selectedDateLabel.getStyleClass().add("selected-date-label");

        datePicker = new DatePicker(LocalDate.now());
        datePicker.getStyleClass().add("appointment-datepicker");
        datePicker.setOnAction(e -> refreshView());

        // ====== КНОПКИ БЕЗ ИКОНОК ======
        Button todayBtn = new Button("Сегодня");
        todayBtn.getStyleClass().add("today-button");
        todayBtn.setOnAction(e -> {
            datePicker.setValue(LocalDate.now());
            refreshView();
        });

        // Кнопки навигации по неделям
        Button prevWeekBtn = new Button("◀");
        prevWeekBtn.getStyleClass().add("today-button");
        prevWeekBtn.setOnAction(e -> {
            datePicker.setValue(datePicker.getValue().minusWeeks(1));
            refreshView();
        });

        Button nextWeekBtn = new Button("▶");
        nextWeekBtn.getStyleClass().add("today-button");
        nextWeekBtn.setOnAction(e -> {
            datePicker.setValue(datePicker.getValue().plusWeeks(1));
            refreshView();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topPanel.getChildren().addAll(
                viewBox,
                new Label("Дата:"), datePicker, todayBtn,
                prevWeekBtn,
                selectedDateLabel,
                nextWeekBtn,
                spacer
        );

        scheduleGrid = new GridPane();
        scheduleGrid.setHgap(8);
        scheduleGrid.setVgap(8);
        scheduleGrid.setPadding(new Insets(15));
        scheduleGrid.getStyleClass().add("schedule-grid");

        scrollPane = new ScrollPane(scheduleGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("schedule-scrollpane");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        root.getChildren().addAll(topPanel, buildStatusLegend(), scrollPane);

        refreshView();

        return root;
    }

    private static void refreshView() {
        if (datePicker == null || scheduleGrid == null) {
            // Представление ещё не построено (create() не вызывался) — обновлять нечего.
            return;
        }
        if ("week".equals(currentView)) {
            loadWeekView();
        } else {
            loadMonthView();
        }
    }

    public static void refresh() {
        refreshView();
    }

    private static void loadWeekView() {
        LocalDate startDate = datePicker.getValue();
        if (startDate == null) return;

        while (startDate.getDayOfWeek().getValue() != 1) {
            startDate = startDate.minusDays(1);
        }

        selectedDateLabel.setText("Неделя " + startDate.format(WEEK_FORMATTER) + " - " + startDate.plusDays(6).format(WEEK_FORMATTER));
        scheduleGrid.getChildren().clear();

        // Заголовок времени
        Label timeHeader = new Label("Время");
        timeHeader.getStyleClass().add("schedule-header");
        scheduleGrid.add(timeHeader, 0, 0);

        applyWeekColumnConstraints();

        for (int i = 0; i < 7; i++) {
            LocalDate day = startDate.plusDays(i);
            int dayCount = DataStore.getAppointmentsByDate(DateUtils.formatDateForDB(day)).size();
            scheduleGrid.add(buildDayHeader(day, dayCount), i + 1, 0);
        }

        for (int i = 0; i < AppConstants.TIME_SLOTS.size(); i++) {
            String time = AppConstants.TIME_SLOTS.get(i);
            int row = i + 1;

            Label timeLabel = new Label(time);
            timeLabel.getStyleClass().add("time-label-week");
            scheduleGrid.add(timeLabel, 0, row);

            for (int j = 0; j < 7; j++) {
                LocalDate day = startDate.plusDays(j);
                List<Appointment> dayAppointments = DataStore.getAppointmentsByDate(DateUtils.formatDateForDB(day));
                Appointment appointment = findAppointmentByTimeInList(dayAppointments, time);

                VBox cell = new VBox(5);
                cell.getStyleClass().add("week-cell");

                if (appointment != null) {
                    cell.getChildren().add(buildAppointmentCard(appointment));

                    // ====== DRAG AND DROP ДЛЯ СУЩЕСТВУЮЩЕЙ ЗАПИСИ ======
                    cell.setOnDragDetected(e -> {
                        if (appointment != null) {
                            // Создаём Dragboard с ID записи
                            Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                            ClipboardContent content = new ClipboardContent();
                            String contentStr = "appointment_id:" + appointment.getId() + "|" +
                                            "original_date:" + appointment.getDate() + "|" +
                                            "original_time:" + appointment.getTime() + "|" +
                                            "original_master:" + appointment.getMasterName();
                            content.putString(contentStr);
                            db.setContent(content);
                            e.consume();
                        }
                    });
                    cell.setStyle("-fx-cursor: move;");

                    cell.setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2) {
                            showAppointmentDialog(appointment, AppointmentDialogMode.DETAILS);
                        }
                    });
                } else {
                    Label freeLabel = new Label("свободно");
                    freeLabel.getStyleClass().add("week-cell-free-label");
                    cell.getChildren().add(freeLabel);
                    cell.getStyleClass().add("week-cell-free");
                    cell.setStyle("-fx-cursor: pointer;");
                    Tooltip freeTooltip = new Tooltip("Свободно\nДвойной клик — создать запись");
                    freeTooltip.setShowDelay(Duration.millis(300));
                    Tooltip.install(cell, freeTooltip);

                    final LocalDate finalDay = day;
                    final String finalTime = time;
                    cell.setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2) {
                            datePicker.setValue(finalDay);
                            showAddAppointmentWithOrderDialog(finalTime);
                        }
                    });
                }

                // ====== ОБРАБОТКА DRAG AND DROP ДЛЯ ВСЕХ ЯЧЕЕК ======
                final LocalDate cellDate = day;
                final String cellTime = time;
                
                cell.setOnDragOver(e -> {
                    if (e.getDragboard().hasContent(javafx.scene.input.DataFormat.PLAIN_TEXT)) {
                        e.acceptTransferModes(TransferMode.MOVE);
                        e.consume();
                    }
                });
                
                cell.setOnDragDropped(e -> {
                    Dragboard db = e.getDragboard();
                    if (db.hasContent(javafx.scene.input.DataFormat.PLAIN_TEXT)) {
                        String content = db.getString();
                        
                        // Извлекаем данные из Dragboard
                        String appointmentId = null;
                        String originalDate = null;
                        String originalTime = null;
                        String originalMaster = null;
                        
                        String[] parts = content.split("\\|");
                        for (String part : parts) {
                            if (part.startsWith("appointment_id:")) {
                                appointmentId = part.substring("appointment_id:".length());
                            } else if (part.startsWith("original_date:")) {
                                originalDate = part.substring("original_date:".length());
                            } else if (part.startsWith("original_time:")) {
                                originalTime = part.substring("original_time:".length());
                            } else if (part.startsWith("original_master:")) {
                                originalMaster = part.substring("original_master:".length());
                            }
                        }
                        
                        if (appointmentId != null && originalDate != null && originalTime != null && originalMaster != null) {
                            // Показываем диалог редактирования с новой датой и временем
                            showEditAppointmentDialog(appointmentId, originalDate, originalTime, originalMaster, cellDate, cellTime);
                        }
                        
                        e.setDropCompleted(true);
                    } else {
                        e.setDropCompleted(false);
                    }
                    e.consume();
                });
                
                scheduleGrid.add(cell, j + 1, row);
            }
        }

        scrollPane.setVvalue(0);
    }

    private static void loadMonthView() {
        LocalDate currentDate = datePicker.getValue();
        if (currentDate == null) return;

        selectedDateLabel.setText(currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy 'г.'", new java.util.Locale.Builder().setLanguage("ru").build())));
        scheduleGrid.getChildren().clear();
        applyMonthColumnConstraints();

        String[] weekDays = {"Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"};
        for (int i = 0; i < 7; i++) {
            Label dayHeader = new Label(weekDays[i]);
            dayHeader.getStyleClass().add("month-day-header");
            dayHeader.setAlignment(Pos.CENTER);
            scheduleGrid.add(dayHeader, i, 0);
        }

        LocalDate firstOfMonth = currentDate.withDayOfMonth(1);
        int startOffset = firstOfMonth.getDayOfWeek().getValue() - 1;
        LocalDate startDate = firstOfMonth.minusDays(startOffset);

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                LocalDate cellDate = startDate.plusDays(row * 7 + col);
                boolean isCurrentMonth = cellDate.getMonth() == currentDate.getMonth();

                VBox cell = new VBox(5);
                cell.getStyleClass().add("month-cell");
                if (!isCurrentMonth) {
                    cell.getStyleClass().add("month-cell-other-month");
                }
                if (cellDate.equals(LocalDate.now())) {
                    cell.getStyleClass().add("today");
                }

                Label dateLabel = new Label(String.valueOf(cellDate.getDayOfMonth()));
                dateLabel.getStyleClass().add("month-date-label");
                cell.getChildren().add(dateLabel);

                List<Appointment> dayAppointments = DataStore.getAppointmentsByDate(DateUtils.formatDateForDB(cellDate));

                if (!dayAppointments.isEmpty()) {
                    int count = 0;
                    for (Appointment a : dayAppointments) {
                        if (count >= 2) {
                            Label moreLabel = new Label("... и ещё " + (dayAppointments.size() - 2));
                            moreLabel.getStyleClass().add("month-more-label");
                            cell.getChildren().add(moreLabel);
                            break;
                        }
                        cell.getChildren().add(buildMonthAppointmentRow(a));
                        count++;
                    }
                } else if (isCurrentMonth) {
                    Label freeLabel = new Label("свободно");
                    freeLabel.getStyleClass().add("month-free-label");
                    cell.getChildren().add(freeLabel);

                    final LocalDate finalCellDate = cellDate;
                    cell.setStyle("-fx-cursor: pointer;");
                    cell.setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2) {
                            datePicker.setValue(finalCellDate);
                            showAddAppointmentWithOrderDialog(null);
                        }
                    });
                }

                scheduleGrid.add(cell, col, row + 1);
            }
        }
    }

    private static Appointment findAppointmentByTimeInList(List<Appointment> appointments, String time) {
        for (Appointment a : appointments) {
            if (a.getTime().equals(time)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Семантический ключ статуса заказа записи. Сами цвета берутся из темы
     * в appointment-custom.css, поэтому ключ не зависит от палитры.
     *
     * @return {@code "closed"}, {@code "in-progress"}, {@code "new"} или {@code "none"}
     */
    private static String getOrderStatusKey(Appointment appointment) {
        String orderId = appointment.getOrderId();
        if (orderId == null || orderId.isEmpty()) {
            return "none";
        }
        for (WorkOrder order : DataStore.getOrders()) {
            if (order.getId().equals(orderId)) {
                String status = order.getStatus();
                if (WorkOrder.STATUS_CLOSED.equals(status)) {
                    return "closed";
                }
                if (WorkOrder.STATUS_IN_PROGRESS.equals(status)) {
                    return "in-progress";
                }
                return "new";
            }
        }
        return "none";
    }

    /** Подключить стили календаря к корню вкладки (файл может отсутствовать в сборке). */
    private static void applyCalendarStylesheet(VBox root) {
        java.net.URL cssUrl = AppointmentView.class.getResource("/appointment-custom.css");
        if (cssUrl != null) {
            root.getStylesheets().add(cssUrl.toExternalForm());
        }
    }

    /** Легенда статусов — чтобы цвета полосы слева читались без догадок. */
    private static HBox buildStatusLegend() {
        HBox legend = new HBox(16,
                buildLegendItem("status-new", "Запланировано"),
                buildLegendItem("status-in-progress", "В работе"),
                buildLegendItem("status-closed", "Закрыт"),
                buildLegendItem("status-none", "Без заказа"));
        legend.getStyleClass().add("calendar-legend");
        legend.setAlignment(Pos.CENTER_LEFT);
        return legend;
    }

    private static HBox buildLegendItem(String statusClass, String text) {
        Region dot = new Region();
        dot.getStyleClass().addAll("legend-dot", statusClass);
        Label label = new Label(text);
        label.getStyleClass().add("legend-text");
        HBox item = new HBox(6, dot, label);
        item.getStyleClass().add("legend-item");
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    /** Шапка дня недели: короткое имя дня, крупное число, месяц и счётчик записей. */
    private static VBox buildDayHeader(LocalDate day, int appointmentCount) {
        Label weekDay = new Label(day.getDayOfWeek().getDisplayName(TextStyle.SHORT, RU_LOCALE));
        weekDay.getStyleClass().add("week-day-name");

        Label date = new Label(String.valueOf(day.getDayOfMonth()));
        date.getStyleClass().add("week-day-date");

        Label month = new Label(day.format(MONTH_FORMATTER));
        month.getStyleClass().add("week-day-month");

        HBox footer = new HBox(4, month);
        footer.setAlignment(Pos.CENTER);
        if (appointmentCount > 0) {
            Label count = new Label(String.valueOf(appointmentCount));
            count.getStyleClass().add("week-day-count");
            footer.getChildren().add(count);
        }

        VBox card = new VBox(1, weekDay, date, footer);
        card.getStyleClass().add("week-day-card");
        card.setAlignment(Pos.CENTER);
        if (day.getDayOfWeek().getValue() >= 6) {
            card.getStyleClass().add("weekend");
        }
        if (day.equals(LocalDate.now())) {
            card.getStyleClass().add("today");
        }
        return card;
    }

    /** Карточка записи в недельной сетке: полоса статуса + клиент, машина, услуга, мастер. */
    private static HBox buildAppointmentCard(Appointment appointment) {
        Client client = appointment.getClient();

        Region stripe = new Region();
        stripe.getStyleClass().addAll("appointment-stripe", "status-" + getOrderStatusKey(appointment));

        Label name = new Label(formatClientName(client));
        name.getStyleClass().add("appointment-client");
        name.setWrapText(true);
        name.setMinWidth(0);

        VBox content = new VBox(2, name);
        content.getStyleClass().add("appointment-content");

        String carInfo = formatClientCar(client);
        if (!carInfo.isEmpty()) {
            Label car = new Label(carInfo);
            car.getStyleClass().add("appointment-car");
            car.setWrapText(true);
            content.getChildren().add(car);
        }

        String serviceName = appointment.getServiceName();
        if (serviceName != null && !serviceName.isBlank()) {
            Label chip = new Label(serviceName);
            chip.getStyleClass().add("appointment-service-chip");
            chip.setWrapText(true);
            content.getChildren().add(chip);
        }

        String masterName = appointment.getMasterName();
        if (masterName != null && !masterName.isBlank()) {
            Label master = new Label(masterName);
            master.getStyleClass().add("appointment-master");
            master.setWrapText(true);
            content.getChildren().add(master);
        }

        HBox card = new HBox(6, stripe, content);
        card.getStyleClass().add("appointment-card");
        card.setMinWidth(0);
        content.setMinWidth(0);
        HBox.setHgrow(content, Priority.ALWAYS);
        Tooltip.install(card, buildAppointmentTooltip(appointment));
        return card;
    }

    /** Строка записи в месячной сетке: точка статуса, время, клиент. */
    private static HBox buildMonthAppointmentRow(Appointment appointment) {
        Region dot = new Region();
        dot.getStyleClass().addAll("month-status-dot", "status-" + getOrderStatusKey(appointment));

        Label time = new Label(appointment.getTime());
        time.getStyleClass().add("month-appointment-time");

        Label name = new Label(formatClientName(appointment.getClient()));
        name.getStyleClass().add("month-appointment-name");
        name.setWrapText(true);
        // minWidth = 0 разрешает метке сжиматься: иначе длинное имя растягивает
        // строку шире ячейки и та заезжает на соседние столбцы.
        name.setMinWidth(0);
        name.setMaxWidth(Double.MAX_VALUE);

        HBox row = new HBox(5, dot, time, name);
        row.getStyleClass().add("month-appointment-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMinWidth(0);
        HBox.setHgrow(name, Priority.ALWAYS);
        row.setStyle("-fx-cursor: hand;");
        row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                showAppointmentDialog(appointment, AppointmentDialogMode.INFO_ONLY);
            }
        });
        Tooltip.install(row, buildAppointmentTooltip(appointment));
        return row;
    }

    private static Tooltip buildAppointmentTooltip(Appointment appointment) {
        Client client = appointment.getClient();
        String statusText = switch (getOrderStatusKey(appointment)) {
            case "closed" -> "Закрыт";
            case "in-progress" -> "В работе";
            case "new" -> "Запланировано";
            default -> "Без заказа";
        };

        StringBuilder text = new StringBuilder();
        String carText = formatClientCar(client);
        text.append(formatClientName(client)).append('\n')
                .append("Телефон: ").append(formatClientPhone(client)).append('\n')
                .append("Авто: ").append(carText.isEmpty() ? "—" : carText);

        String serviceName = appointment.getServiceName();
        if (serviceName != null && !serviceName.isBlank()) {
            text.append('\n').append("Услуга: ").append(serviceName);
        }
        String masterName = appointment.getMasterName();
        if (masterName != null && !masterName.isBlank()) {
            text.append('\n').append("Мастер: ").append(masterName);
        }
        text.append('\n').append("Статус: ").append(statusText);

        Tooltip tooltip = new Tooltip(text.toString());
        tooltip.setShowDelay(Duration.millis(300));
        return tooltip;
    }

    /** Колонка времени занимает 6% ширины, семь дней делят остальные 94% поровну. */
    private static void applyWeekColumnConstraints() {
        scheduleGrid.getColumnConstraints().clear();
        ColumnConstraints timeColumn = new ColumnConstraints();
        timeColumn.setPercentWidth(TIME_COLUMN_WIDTH_PERCENT);
        scheduleGrid.getColumnConstraints().add(timeColumn);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints dayColumn = new ColumnConstraints();
            dayColumn.setPercentWidth((100.0 - TIME_COLUMN_WIDTH_PERCENT) / 7);
            dayColumn.setFillWidth(true);
            scheduleGrid.getColumnConstraints().add(dayColumn);
        }
    }

    /** Месячная сетка: семь равных колонок. */
    private static void applyMonthColumnConstraints() {
        scheduleGrid.getColumnConstraints().clear();
        for (int i = 0; i < 7; i++) {
            ColumnConstraints dayColumn = new ColumnConstraints();
            dayColumn.setPercentWidth(100.0 / 7);
            dayColumn.setFillWidth(true);
            scheduleGrid.getColumnConstraints().add(dayColumn);
        }
    }

    private static void showAppointmentDialog(Appointment appointment, AppointmentDialogMode mode) {
        Stage stage = new Stage();
        stage.setTitle(mode.windowTitle);
        stage.setMinWidth(500);
        stage.setMinHeight(500);
        stage.initModality(Modality.WINDOW_MODAL);

        WindowStateManager.getInstance().restoreWindowState(mode.windowStateKey, stage);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label("Информация о записи");
        titleLabel.getStyleClass().add("dialog-title");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.getStyleClass().add("dialog-grid");

        Client client = appointment.getClient();
        String fullName = formatClientName(client);

        grid.add(new Label("Клиент:"), 0, 0);
        grid.add(new Label(fullName), 1, 0);

        grid.add(new Label("Телефон:"), 0, 1);
        grid.add(new Label(formatClientPhone(client)), 1, 1);

        grid.add(new Label("Автомобиль:"), 0, 2);
        grid.add(new Label(formatClientCar(client)), 1, 2);

        grid.add(new Label("Мастер:"), 0, 3);
        grid.add(new Label(appointment.getMasterName()), 1, 3);

        grid.add(new Label("Дата:"), 0, 4);
        grid.add(new Label(appointment.getDate()), 1, 4);

        grid.add(new Label("Время:"), 0, 5);
        grid.add(new Label(appointment.getTime()), 1, 5);

        int rowIndex = 6;

        String orderId = appointment.getOrderId();
        if (orderId != null && !orderId.isEmpty()) {
            grid.add(new Label("Номер заказа:"), 0, rowIndex);
            Label orderLabel = new Label(orderId);
            orderLabel.setStyle("-fx-text-fill: #1976d2; -fx-font-weight: bold;");
            grid.add(orderLabel, 1, rowIndex);
            rowIndex++;

            // Статус заказа
            String orderStatus = "—";
            for (WorkOrder order : DataStore.getOrders()) {
                if (orderId.equals(order.getId())) {
                    orderStatus = order.getStatus();
                    break;
                }
            }
            grid.add(new Label("Статус заказа:"), 0, rowIndex);
            Label statusLabel = new Label(orderStatus);
            if ("Новый".equals(orderStatus)) {
                statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            } else if ("Закрыт".equals(orderStatus)) {
                statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            } else if ("В работе".equals(orderStatus)) {
                statusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
            }
            grid.add(statusLabel, 1, rowIndex);
            rowIndex++;
        }

        List<String> allServices = new ArrayList<>();
        double totalPrice = 0;

        if (orderId != null && !orderId.isEmpty()) {
            for (WorkOrder order : DataStore.getOrders()) {
                if (orderId.equals(order.getId())) {
                    allServices.addAll(order.getServices());
                    totalPrice = order.getTotal();
                    break;
                }
            }
        }

        if (allServices.isEmpty()) {
            allServices.add(appointment.getServiceName());
        }

        grid.add(new Label("Услуги:"), 0, rowIndex);
        VBox servicesBox = new VBox(4);
        for (String service : allServices) {
            Label serviceLabel = new Label("• " + service);
            serviceLabel.setWrapText(true);
            serviceLabel.setStyle("-fx-font-size: 13px;");
            servicesBox.getChildren().add(serviceLabel);
        }
        grid.add(servicesBox, 1, rowIndex);
        rowIndex++;

        if (totalPrice > 0) {
            grid.add(new Label("Общая сумма:"), 0, rowIndex);
            Label priceLabel = new Label(String.format("%,.0f руб.", totalPrice));
            priceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60;");
            grid.add(priceLabel, 1, rowIndex);
            rowIndex++;
        }

        if (orderId != null && !orderId.isEmpty()) {
            for (WorkOrder order : DataStore.getOrders()) {
                if (orderId.equals(order.getId())) {
                    if (!order.getSpareParts().isEmpty()) {
                        VBox partsBox = new VBox(4);
                        for (int i = 0; i < order.getSpareParts().size(); i++) {
                            SparePart part = order.getSpareParts().get(i);
                            double quantity = order.getSparePartQuantities().get(i);
                            Label partLabel = new Label("• " + part.getName() + " x" + (int)quantity);
                            partLabel.setWrapText(true);
                            partLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
                            partsBox.getChildren().add(partLabel);
                        }
                        grid.add(new Label("Запчасти:"), 0, rowIndex);
                        grid.add(partsBox, 1, rowIndex);
                    }
                    break;
                }
            }
        }

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        Button closeBtn = new Button("Закрыть");
        closeBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        closeBtn.setOnAction(e -> stage.close());

        btnBox.getChildren().add(closeBtn);
        
        // Кнопка удаления записи (всегда доступна)
        Button deleteBtn = new Button("Удалить запись");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Удалить запись?\n\n" +
                            "Это действие нельзя отменить.",
                    ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Подтверждение удаления");
            
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    DataStore.deleteAppointment(appointment.getId());
                    refresh();
                    stage.close();
                }
            });
        });
        btnBox.getChildren().add(deleteBtn);

        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
        if (mode.attachStylesheet) {
            java.net.URL cssUrl = AppointmentView.class.getResource("/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        }
        stage.setScene(scene);
        stage.setOnHiding(e -> WindowStateManager.getInstance().saveWindowState(mode.windowStateKey, stage));
        stage.showAndWait();
    }

    /**
     * Режим диалога записи: от него зависят заголовок окна, ключ сохранённого
     * состояния и подключение таблицы стилей.
     */
    private enum AppointmentDialogMode {
        DETAILS("Детали записи", "appointmentDetails", false),
        INFO_ONLY("Информация о записи", "appointmentInfoOnly", true);

        private final String windowTitle;
        private final String windowStateKey;
        private final boolean attachStylesheet;

        AppointmentDialogMode(String windowTitle, String windowStateKey, boolean attachStylesheet) {
            this.windowTitle = windowTitle;
            this.windowStateKey = windowStateKey;
            this.attachStylesheet = attachStylesheet;
        }
    }

    /**
     * Состояние диалога создания записи: окно и элементы формы.
     */
    private static final class AddAppointmentForm {
        private final Stage stage;
        private final VBox root;

        private ComboBox<WorkOrder> orderCombo;
        private ComboBox<String> masterCombo;
        private DatePicker datePicker;
        private ComboBox<String> timeCombo;
        private Button saveBtn;
        private Button cancelBtn;

        private AddAppointmentForm(Stage stage) {
            this.stage = stage;
            this.root = (VBox) stage.getScene().getRoot();
        }
    }

    /**
     * Состояние диалога перемещения записи: окно, элементы формы и запись.
     */
    private static final class EditAppointmentForm {
        private final Stage stage;
        private final VBox root;
        private final int appointmentId;

        private Appointment existingAppointment;
        private ComboBox<String> masterCombo;
        private DatePicker datePicker;
        private ComboBox<String> timeCombo;
        private Button saveBtn;
        private Button cancelBtn;

        private EditAppointmentForm(Stage stage, int appointmentId) {
            this.stage = stage;
            this.appointmentId = appointmentId;
            this.root = (VBox) stage.getScene().getRoot();
        }
    }

    private static void showAddAppointmentWithOrderDialog(String presetTime) {
        AddAppointmentForm form = new AddAppointmentForm(createAddAppointmentWindow());
        buildAddAppointmentForm(form, presetTime);
        attachAddAppointmentHandlers(form);
        form.stage.showAndWait();
    }

    /**
     * Создать окно диалога создания записи.
     */
    private static Stage createAddAppointmentWindow() {
        Stage stage = new Stage();
        stage.setTitle("Новая запись");
        stage.setMinWidth(500);
        stage.setMinHeight(400);
        stage.initModality(Modality.WINDOW_MODAL);

        WindowStateManager.getInstance().restoreWindowState("addAppointmentWithOrderDialog", stage);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Scene scene = new Scene(root);
        stage.setScene(scene);
        return stage;
    }

    /**
     * Собрать форму диалога создания записи: заголовок, поля и кнопки.
     */
    private static void buildAddAppointmentForm(AddAppointmentForm form, String presetTime) {
        Label titleLabel = new Label("Новая запись");
        titleLabel.getStyleClass().add("dialog-title");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.getStyleClass().add("dialog-grid");

        buildOrderSection(form, grid);
        buildNewAppointmentFields(form, grid, presetTime);

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        // ====== КНОПКИ БЕЗ ИКОНОК ======
        form.saveBtn = new Button("Создать запись");
        form.saveBtn.getStyleClass().add("save-button");

        form.cancelBtn = new Button("Отмена");
        form.cancelBtn.getStyleClass().add("cancel-button");

        btnBox.getChildren().addAll(form.saveBtn, form.cancelBtn);

        form.root.getChildren().addAll(titleLabel, grid, btnBox);
    }

    /**
     * Секция выбора заказа: список активных (не закрытых) заказов.
     */
    private static void buildOrderSection(AddAppointmentForm form, GridPane grid) {
        // Комбо бокс для выбора заказа (кроме закрытых)
        List<WorkOrder> activeOrders = new ArrayList<>();
        for (WorkOrder order : DataStore.getOrders()) {
            if (!WorkOrder.STATUS_CLOSED.equals(order.getStatus())) {
                activeOrders.add(order);
            }
        }

        form.orderCombo = new ComboBox<>(FXCollections.observableArrayList(activeOrders));
        form.orderCombo.setPromptText("Выберите заказ");
        form.orderCombo.setPrefWidth(300);

        configureOrderComboFormatting(form.orderCombo);

        grid.add(new Label("Заказ:"), 0, 0);
        grid.add(form.orderCombo, 1, 0);
    }

    /**
     * Форматирование отображения заказа: номер и имя клиента.
     */
    private static void configureOrderComboFormatting(ComboBox<WorkOrder> orderCombo) {
        orderCombo.setCellFactory(listView -> new ListCell<WorkOrder>() {
            @Override
            protected void updateItem(WorkOrder item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String clientName = item.getClient() != null
                            ? (item.getClient().getLastName() != null && !item.getClient().getLastName().isEmpty()
                                ? item.getClient().getLastName() + " " + item.getClient().getName()
                                : item.getClient().getName())
                            : "Без клиента";
                    setText(item.getId() + " — " + clientName);
                }
            }
        });

        orderCombo.setButtonCell(new ListCell<WorkOrder>() {
            @Override
            protected void updateItem(WorkOrder item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String clientName = item.getClient() != null
                            ? (item.getClient().getLastName() != null && !item.getClient().getLastName().isEmpty()
                                ? item.getClient().getLastName() + " " + item.getClient().getName()
                                : item.getClient().getName())
                            : "Без клиента";
                    setText(item.getId() + " — " + clientName);
                }
            }
        });
    }

    /**
     * Секция мастера, даты и времени новой записи.
     */
    private static void buildNewAppointmentFields(AddAppointmentForm form, GridPane grid, String presetTime) {
        form.masterCombo = new ComboBox<>(FXCollections.observableArrayList(AppConstants.MASTERS));
        form.masterCombo.setPromptText("Выберите мастера");
        form.masterCombo.setPrefWidth(200);

        form.datePicker = new DatePicker(datePicker.getValue());
        form.datePicker.setPrefWidth(200);

        form.timeCombo = new ComboBox<>(FXCollections.observableArrayList(AppConstants.TIME_SLOTS));
        if (presetTime != null) {
            form.timeCombo.setValue(presetTime);
        }
        form.timeCombo.setPromptText("Выберите время");
        form.timeCombo.setPrefWidth(100);
        TooltipHelper.setToolTip(form.masterCombo, "Выберите мастера сервиса");

        grid.add(new Label("Мастер:"), 0, 1);
        grid.add(form.masterCombo, 1, 1);
        grid.add(new Label("Дата:"), 0, 2);
        grid.add(form.datePicker, 1, 2);
        grid.add(new Label("Время:"), 0, 3);
        grid.add(form.timeCombo, 1, 3);
    }

    /**
     * Повесить обработчики диалога создания записи.
     */
    private static void attachAddAppointmentHandlers(AddAppointmentForm form) {
        form.saveBtn.setOnAction(e -> saveNewAppointment(form));
        form.cancelBtn.setOnAction(e -> form.stage.close());

        form.stage.setOnHiding(e -> WindowStateManager.getInstance().saveWindowState("editAppointmentDialog", form.stage));
    }

    /**
     * Проверить обязательные поля диалога создания записи.
     *
     * @return {@code true}, если все поля заполнены
     */
    private static boolean validateNewAppointment(AddAppointmentForm form) {
        boolean isValid = true;

        if (form.orderCombo.getValue() == null) {
            showAlert("Выберите заказ");
            isValid = false;
        }
        if (form.masterCombo.getValue() == null) {
            showAlert("Выберите мастера");
            isValid = false;
        }
        if (form.timeCombo.getValue() == null) {
            showAlert("Выберите время");
            isValid = false;
        }
        if (form.datePicker.getValue() == null) {
            showAlert("Выберите дату");
            isValid = false;
        }

        return isValid;
    }

    /**
     * Сохранить новую запись: проверить поля, выходной день и занятость времени.
     */
    private static void saveNewAppointment(AddAppointmentForm form) {
        if (!validateNewAppointment(form)) {
            return;
        }

        // ====== ПРОВЕРКА ВЫХОДНЫХ ДНЕЙ ======
        if (!confirmWeekend(form.datePicker, "Подтверждение записи")) {
            return; // Отмена при нажатии "Нет"
        }

        WorkOrder selectedOrder = form.orderCombo.getValue();
        Client orderClient = selectedOrder.getClient();
        if (orderClient == null) {
            showAlert("У выбранного заказа не указан клиент");
            return;
        }
        String selectedServiceName = selectedOrder.getServices().isEmpty()
                ? "Консультация"
                : selectedOrder.getServices().get(0);

        String dateStr = DateUtils.formatDateForDB(form.datePicker.getValue());
        String timeStr = form.timeCombo.getValue();

        if (isTimeSlotTaken(dateStr, timeStr)) {
            showAlert("Это время уже занято!");
            return;
        }

        Service newService = DataStore.getServiceByName(selectedServiceName);
        int newServiceId = (newService != null) ? newService.getId() : 0;

        Appointment appointment = new Appointment(
                orderClient,
                form.masterCombo.getValue(),
                selectedServiceName,
                dateStr,
                timeStr
        );
        appointment.setServiceId(newServiceId);
        appointment.setOrderId(selectedOrder.getId());

        DataStore.addAppointment(appointment);

        refresh();
        form.stage.close();
    }

    /**
     * Проверить, занято ли время в выбранный день (для новой записи).
     */
    private static boolean isTimeSlotTaken(String dateStr, String timeStr) {
        List<Appointment> existing = DataStore.getAppointmentsByDate(dateStr);
        for (Appointment a : existing) {
            if (a.getTime().equals(timeStr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Подтвердить запись в выходной день.
     *
     * @return {@code true}, если пользователь подтвердил запись
     */
    private static boolean confirmWeekend(DatePicker picker, String title) {
        if (!DateUtils.isWeekend(picker.getValue())) {
            return true;
        }

        Alert weekendAlert = new Alert(Alert.AlertType.CONFIRMATION);
        weekendAlert.setTitle(title);
        weekendAlert.setHeaderText("Выбран выходной день!");
        weekendAlert.setContentText("Запись в выходной день (" + picker.getValue().format(java.time.format.DateTimeFormatter.ofPattern("EEEE", new java.util.Locale.Builder().setLanguage("ru").build())) + ") может быть ограничена.\n\nПродолжить?");
        weekendAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        return weekendAlert.showAndWait().orElse(ButtonType.NO) != ButtonType.NO;
    }

    // ====== МЕТОД РЕДАКТИРОВАНИЯ ЗАПИСИ ПОСЛЕ DRAG AND DROP ======
    private static void showEditAppointmentDialog(String appointmentId, String originalDate, String originalTime,
                                                    String originalMaster, LocalDate newDate, String newTime) {
        EditAppointmentForm form = new EditAppointmentForm(
                createEditAppointmentWindow(), Integer.parseInt(appointmentId));
        buildEditAppointmentForm(form, originalDate, originalTime, originalMaster, newDate, newTime);
        attachEditAppointmentHandlers(form);
        form.stage.showAndWait();
    }

    /**
     * Создать окно диалога перемещения записи.
     */
    private static Stage createEditAppointmentWindow() {
        Stage stage = new Stage();
        stage.setTitle("Переместить запись");
        stage.setMinWidth(500);
        stage.setMinHeight(400);
        stage.initModality(Modality.WINDOW_MODAL);

        WindowStateManager.getInstance().restoreWindowState("editAppointmentDialog", stage);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Scene scene = new Scene(root);
        stage.setScene(scene);
        return stage;
    }

    /**
     * Собрать форму диалога перемещения записи: заголовок, информация о текущей
     * записи, новые дата/время и кнопки.
     */
    private static void buildEditAppointmentForm(EditAppointmentForm form, String originalDate,
                                                 String originalTime, String originalMaster,
                                                 LocalDate newDate, String newTime) {
        Label titleLabel = new Label("Переместить запись");
        titleLabel.getStyleClass().add("dialog-title");

        GridPane infoGrid = buildAppointmentInfoSection(form, originalDate, originalTime);

        // Поля для новой даты и времени
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.getStyleClass().add("dialog-grid");

        buildEditScheduleFields(form, grid, originalMaster, newDate, newTime);

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        form.saveBtn = new Button("Сохранить изменения");
        form.saveBtn.getStyleClass().add("save-button");

        form.cancelBtn = new Button("Отмена");
        form.cancelBtn.getStyleClass().add("cancel-button");

        btnBox.getChildren().addAll(form.saveBtn, form.cancelBtn);

        form.root.getChildren().addAll(titleLabel, infoGrid, grid, btnBox);
    }

    /**
     * Секция с информацией о перемещаемой записи.
     */
    private static GridPane buildAppointmentInfoSection(EditAppointmentForm form,
                                                        String originalDate, String originalTime) {
        // Информация о текущей записи
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(15);
        infoGrid.setVgap(8);
        infoGrid.getStyleClass().add("dialog-grid");

        form.existingAppointment = DataStore.getAppointmentById(form.appointmentId);

        if (form.existingAppointment != null) {
            Client client = form.existingAppointment.getClient();
            String fullName = formatClientName(client);

            infoGrid.add(new Label("Клиент:"), 0, 0);
            infoGrid.add(new Label(fullName), 1, 0);

            infoGrid.add(new Label("Текущая дата:"), 0, 1);
            Label oldDateLabel = new Label(DateUtils.formatDate(originalDate));
            oldDateLabel.getStyleClass().add("warning-text");
            infoGrid.add(oldDateLabel, 1, 1);

            infoGrid.add(new Label("Текущее время:"), 0, 2);
            Label oldTimeLabel = new Label(originalTime);
            oldTimeLabel.getStyleClass().add("warning-text");
            infoGrid.add(oldTimeLabel, 1, 2);

            infoGrid.add(new Label("Мастер:"), 0, 3);
            infoGrid.add(new Label(form.existingAppointment.getMasterName()), 1, 3);

            infoGrid.add(new Label("Услуга:"), 0, 4);
            infoGrid.add(new Label(form.existingAppointment.getServiceName()), 1, 4);
        }

        return infoGrid;
    }

    /**
     * Секция новых мастера, даты и времени записи.
     */
    private static void buildEditScheduleFields(EditAppointmentForm form, GridPane grid,
                                                String originalMaster, LocalDate newDate, String newTime) {
        form.masterCombo = new ComboBox<>(FXCollections.observableArrayList(AppConstants.MASTERS));
        form.masterCombo.setValue(originalMaster);
        form.masterCombo.setPromptText("Выберите мастера");
        form.masterCombo.setPrefWidth(200);

        form.datePicker = new DatePicker(newDate);
        form.datePicker.setPrefWidth(200);

        form.timeCombo = new ComboBox<>(FXCollections.observableArrayList(AppConstants.TIME_SLOTS));
        form.timeCombo.setValue(newTime);
        form.timeCombo.setPromptText("Выберите время");
        form.timeCombo.setPrefWidth(100);
        TooltipHelper.setToolTip(form.masterCombo, "Выберите мастера сервиса");

        grid.add(new Label("Мастер:"), 0, 0);
        grid.add(form.masterCombo, 1, 0);
        grid.add(new Label("Дата:"), 0, 1);
        grid.add(form.datePicker, 1, 1);
        grid.add(new Label("Время:"), 0, 2);
        grid.add(form.timeCombo, 1, 2);
    }

    /**
     * Повесить обработчики диалога перемещения записи.
     */
    private static void attachEditAppointmentHandlers(EditAppointmentForm form) {
        form.saveBtn.setOnAction(e -> saveEditedAppointment(form));
        form.cancelBtn.setOnAction(e -> form.stage.close());

        form.stage.setOnHiding(e -> WindowStateManager.getInstance().saveWindowState("editAppointmentDialog", form.stage));
    }

    /**
     * Проверить обязательные поля диалога перемещения записи.
     *
     * @return {@code true}, если все поля заполнены
     */
    private static boolean validateEditedAppointment(EditAppointmentForm form) {
        if (form.masterCombo.getValue() == null) {
            showAlert("Выберите мастера");
            return false;
        }
        if (form.timeCombo.getValue() == null) {
            showAlert("Выберите время");
            return false;
        }
        if (form.datePicker.getValue() == null) {
            showAlert("Выберите дату");
            return false;
        }
        return true;
    }

    /**
     * Сохранить перемещение записи: проверить поля, выходной день и конфликт времени.
     */
    private static void saveEditedAppointment(EditAppointmentForm form) {
        if (!validateEditedAppointment(form)) {
            return;
        }

        // ====== ПРОВЕРКА ВЫХОДНЫХ ДНЕЙ ======
        if (!confirmWeekend(form.datePicker, "Подтверждение")) {
            return;
        }

        String newDateStr = DateUtils.formatDateForDB(form.datePicker.getValue());
        String newTimeStr = form.timeCombo.getValue();
        String newMaster = form.masterCombo.getValue();

        // ====== ПРОВЕРКА КОНФЛИКТА ВРЕМЕНИ ======
        if (isEditTimeSlotTaken(newDateStr, newTimeStr, newMaster, form.existingAppointment.getId())) {
            showAlert("Это время уже занято другим клиентом!");
            return;
        }

        // ====== ОБНОВЛЕНИЕ ЗАПИСИ ======
        form.existingAppointment.setDate(newDateStr);
        form.existingAppointment.setTime(newTimeStr);
        form.existingAppointment.setMasterName(newMaster);
        DataStore.updateAppointment(form.existingAppointment);

        refresh();
        form.stage.close();
    }

    /**
     * Проверить конфликт времени с другой записью того же мастера.
     */
    private static boolean isEditTimeSlotTaken(String dateStr, String timeStr, String masterName,
                                               int appointmentId) {
        List<Appointment> existing = DataStore.getAppointmentsByDate(dateStr);
        for (Appointment a : existing) {
            if (a.getTime().equals(timeStr) && a.getMasterName().equals(masterName)) {
                if (a.getId() != appointmentId) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String formatClientName(Client client) {
        if (client == null) {
            return AppConstants.NO_CLIENT;
        }
        String fullName = client.getFullName();
        return fullName != null && !fullName.isEmpty() ? fullName : AppConstants.NO_CLIENT;
    }

    /**
     * Отображаемый телефон клиента; для записи без клиента возвращает прочерк.
     */
    private static String formatClientPhone(Client client) {
        return client == null ? "—" : Formatters.formatPhoneForDisplay(client.getPhone());
    }

    /**
     * Отображаемая информация об автомобиле; для записи без клиента возвращает прочерк.
     */
    private static String formatClientCar(Client client) {
        if (client == null) {
            return "—";
        }
        String model = client.getCarModel() != null ? client.getCarModel().trim() : "";
        String number = client.getCarNumber() != null ? client.getCarNumber().trim() : "";
        if (model.isEmpty() && number.isEmpty()) {
            return "";
        }
        if (number.isEmpty()) {
            return model;
        }
        if (model.isEmpty()) {
            return number;
        }
        return model + " (" + number + ")";
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }
}