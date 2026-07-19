package com.autoservice.views;

import com.autoservice.AppConstants;
import com.autoservice.Appointment;
import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.DateUtils;
import com.autoservice.Service;
import com.autoservice.SparePart;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter HEADER_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy 'г.'", new Locale("ru"));
    private static final DateTimeFormatter WEEK_FORMATTER = DateTimeFormatter.ofPattern("d MMM", new Locale("ru"));

    public static VBox create() {
        SERVICES = DataStore.getServices().stream()
                .map(Service::getName)
                .toArray(String[]::new);

        VBox root = new VBox(15);
        root.getStyleClass().add("main-container");

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
        weekView.setSelected(true);

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
        root.getChildren().addAll(topPanel, scrollPane);

        refreshView();

        return root;
    }

    private static void refreshView() {
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

        for (int i = 0; i < 7; i++) {
            LocalDate day = startDate.plusDays(i);
            String dayOfWeek = day.format(DateTimeFormatter.ofPattern("EEEE", new Locale("ru")));
            String dayOfMonth = day.format(DateTimeFormatter.ofPattern("d MMMM", new Locale("ru")));
            Label dayHeader = new Label(dayOfWeek + "\n" + dayOfMonth);
            dayHeader.getStyleClass().add("week-day-header");
            dayHeader.setWrapText(true);
            dayHeader.setAlignment(Pos.CENTER);
            scheduleGrid.add(dayHeader, i + 1, 0);
        }

        for (int i = 0; i < AppConstants.TIME_SLOTS.length; i++) {
            String time = AppConstants.TIME_SLOTS[i];
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
                    Client client = appointment.getClient();
                    String fullName = (client.getLastName() != null && !client.getLastName().isEmpty())
                            ? client.getLastName() + " " + client.getName()
                            : client.getName();
                    String carInfo = client.getCarModel() + " (" + client.getCarNumber() + ")";

                    Label nameLabel = new Label(fullName);
                    nameLabel.setWrapText(true);
                    nameLabel.getStyleClass().add("week-client-name");
                    Label carLabel = new Label(carInfo);
                    carLabel.setWrapText(true);
                    carLabel.getStyleClass().add("week-car-info");
                    Label serviceLabel = new Label(appointment.getServiceName());
                    serviceLabel.setWrapText(true);
                    serviceLabel.getStyleClass().add("week-service");
                    cell.getChildren().addAll(nameLabel, carLabel, serviceLabel);

                    // Цветовая индикация по статусу заказа
                    String statusColor = getOrderStatusColor(appointment);
                    cell.setStyle("-fx-background-color: " + statusColor + "; -fx-background-radius: 6;");

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
                    cell.setStyle(cell.getStyle() + "; -fx-cursor: move;");

                    cell.setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2) {
                            showAppointmentDetails(appointment);
                        }
                    });
                } else {
                    Label freeLabel = new Label("свободно");
                    freeLabel.getStyleClass().add("week-cell-free-label");
                    cell.getChildren().add(freeLabel);
                    cell.getStyleClass().add("week-cell-free");
                    cell.setStyle(cell.getStyle() + "; -fx-cursor: pointer;");

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

        selectedDateLabel.setText(currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy 'г.'", new Locale("ru"))));
        scheduleGrid.getChildren().clear();

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
                        Client client = a.getClient();
                        String fullName = (client.getLastName() != null && !client.getLastName().isEmpty())
                                ? client.getLastName() + " " + client.getName()
                                : client.getName();

                        Label appLabel = new Label(a.getTime() + " - " + fullName);
                        appLabel.getStyleClass().add("month-appointment-label");
                        appLabel.setWrapText(true);
                        appLabel.setStyle("-fx-cursor: hand;");

                        // Цветной индикатор статуса
                        String statusColor = getOrderStatusColor(a);
                        Circle statusDot = new Circle(3);
                        statusDot.setFill(javafx.scene.paint.Color.web(statusColor));

                        HBox appBox = new HBox(4, statusDot, appLabel);
                        appBox.setAlignment(Pos.CENTER_LEFT);

                        final Appointment currentAppointment = a;
                        appLabel.setOnMouseClicked(event -> {
                            if (event.getClickCount() == 2) {
                                showAppointmentInfoOnly(currentAppointment);
                            }
                        });

                        cell.getChildren().add(appBox);
                        count++;
                    }
                } else if (isCurrentMonth) {
                    Label freeLabel = new Label("свободно");
                    freeLabel.getStyleClass().add("month-free-label");
                    cell.getChildren().add(freeLabel);
                    
                    final LocalDate finalCellDate = cellDate;
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

    private static String getOrderStatusColor(Appointment appointment) {
        String orderId = appointment.getOrderId();
        if (orderId == null || orderId.isEmpty()) {
            return "#95a5a6"; // серый - нет заказа
        }
        
        for (WorkOrder order : DataStore.getOrders()) {
            if (order.getId().equals(orderId)) {
                String status = order.getStatus();
                if (WorkOrder.STATUS_CLOSED.equals(status)) {
                    return "#27ae60"; // зелёный - сделано
                } else if (WorkOrder.STATUS_IN_PROGRESS.equals(status)) {
                    return "#f39c12"; // оранжевый - в работе
                } else {
                    return "#3498db"; // синий - запланировано
                }
            }
        }
        return "#95a5a6"; // серый - заказ не найден
    }

    private static void showAppointmentDetails(Appointment appointment) {
        Stage stage = new Stage();
        stage.setTitle("Детали записи");
        stage.setMinWidth(500);
        stage.setMinHeight(500);
        stage.initModality(Modality.WINDOW_MODAL);

        WindowStateManager.getInstance().restoreWindowState("appointmentDetails", stage);

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
        String fullName = (client.getLastName() != null && !client.getLastName().isEmpty())
                ? client.getLastName() + " " + client.getName()
                : client.getName();

        grid.add(new Label("Клиент:"), 0, 0);
        grid.add(new Label(fullName), 1, 0);

        grid.add(new Label("Телефон:"), 0, 1);
        grid.add(new Label(client.getPhone() != null ? client.getPhone() : "—"), 1, 1);

        grid.add(new Label("Автомобиль:"), 0, 2);
        grid.add(new Label(client.getCarModel() + " (" + client.getCarNumber() + ")"), 1, 2);

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
        stage.setScene(scene);
        stage.setOnHiding(e -> WindowStateManager.getInstance().saveWindowState("appointmentDetails", stage));
        stage.showAndWait();
    }

    private static void showAppointmentInfoOnly(Appointment appointment) {
        Stage stage = new Stage();
        stage.setTitle("Информация о записи");
        stage.setMinWidth(500);
        stage.setMinHeight(500);
        stage.initModality(Modality.WINDOW_MODAL);

        WindowStateManager.getInstance().restoreWindowState("appointmentInfoOnly", stage);

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
        String fullName = (client.getLastName() != null && !client.getLastName().isEmpty())
                ? client.getLastName() + " " + client.getName()
                : client.getName();

        grid.add(new Label("Клиент:"), 0, 0);
        grid.add(new Label(fullName), 1, 0);

        grid.add(new Label("Телефон:"), 0, 1);
        grid.add(new Label(client.getPhone() != null ? client.getPhone() : "—"), 1, 1);

        grid.add(new Label("Автомобиль:"), 0, 2);
        grid.add(new Label(client.getCarModel() + " (" + client.getCarNumber() + ")"), 1, 2);

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
        scene.getStylesheets().add(AppointmentView.class.getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.setOnHiding(e -> WindowStateManager.getInstance().saveWindowState("appointmentInfoOnly", stage));
        stage.showAndWait();
    }

    private static void showAddAppointmentWithOrderDialog(String presetTime) {
        Stage stage = new Stage();
        stage.setTitle("Новая запись");
        stage.setMinWidth(500);
        stage.setMinHeight(400);
        stage.initModality(Modality.WINDOW_MODAL);

        WindowStateManager.getInstance().restoreWindowState("addAppointmentWithOrderDialog", stage);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label("Новая запись");
        titleLabel.getStyleClass().add("dialog-title");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.getStyleClass().add("dialog-grid");

        // Комбо бокс для выбора заказа (кроме закрытых)
        List<WorkOrder> activeOrders = new ArrayList<>();
        for (WorkOrder order : DataStore.getOrders()) {
            if (!WorkOrder.STATUS_CLOSED.equals(order.getStatus())) {
                activeOrders.add(order);
            }
        }
        
        ComboBox<WorkOrder> orderCombo = new ComboBox<>(FXCollections.observableArrayList(activeOrders));
        orderCombo.setPromptText("Выберите заказ");
        orderCombo.setPrefWidth(300);
        
        // Форматирование отображения заказа
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

        ComboBox<String> masterCombo = new ComboBox<>(FXCollections.observableArrayList(AppConstants.MASTERS));
        masterCombo.setPromptText("Выберите мастера");
        masterCombo.setPrefWidth(200);

        DatePicker datePickerLocal = new DatePicker(datePicker.getValue());
        datePickerLocal.setPrefWidth(200);

        ComboBox<String> timeCombo = new ComboBox<>(FXCollections.observableArrayList(AppConstants.TIME_SLOTS));
        if (presetTime != null) {
            timeCombo.setValue(presetTime);
        }
        timeCombo.setPromptText("Выберите время");
        timeCombo.setPrefWidth(100);
        TooltipHelper.setToolTip(masterCombo, "Выберите мастера сервиса");

        grid.add(new Label("Заказ:"), 0, 0);
        grid.add(orderCombo, 1, 0);
        grid.add(new Label("Мастер:"), 0, 1);
        grid.add(masterCombo, 1, 1);
        grid.add(new Label("Дата:"), 0, 2);
        grid.add(datePickerLocal, 1, 2);
        grid.add(new Label("Время:"), 0, 3);
        grid.add(timeCombo, 1, 3);

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        // ====== КНОПКИ БЕЗ ИКОНОК ======
        Button saveBtn = new Button("Создать запись");
        saveBtn.getStyleClass().add("save-button");

        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("cancel-button");

        btnBox.getChildren().addAll(saveBtn, cancelBtn);

        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
            boolean isValid = true;
            
            if (orderCombo.getValue() == null) {
                showAlert("Выберите заказ");
                isValid = false;
            }
            if (masterCombo.getValue() == null) {
                showAlert("Выберите мастера");
                isValid = false;
            }
            if (timeCombo.getValue() == null) {
                showAlert("Выберите время");
                isValid = false;
            }
            if (datePickerLocal.getValue() == null) {
                showAlert("Выберите дату");
                isValid = false;
            }
            
            if (!isValid) {
                return;
            }

            // ====== ПРОВЕРКА ВЫХОДНЫХ ДНЕЙ ======
            if (DateUtils.isWeekend(datePickerLocal.getValue())) {
                Alert weekendAlert = new Alert(Alert.AlertType.CONFIRMATION);
                weekendAlert.setTitle("Подтверждение записи");
                weekendAlert.setHeaderText("Выбран выходной день!");
                weekendAlert.setContentText("Запись в выходной день (" + datePickerLocal.getValue().format(java.time.format.DateTimeFormatter.ofPattern("EEEE", new java.util.Locale("ru"))) + ") может быть ограничена.\n\nПродолжить?");
                weekendAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                
                if (weekendAlert.showAndWait().orElse(ButtonType.NO) == ButtonType.NO) {
                    return; // Отмена при нажатии "Нет"
                }
            }

            WorkOrder selectedOrder = orderCombo.getValue();
            Client orderClient = selectedOrder.getClient();
            String selectedServiceName = selectedOrder.getServices().isEmpty() 
                    ? "Консультация" 
                    : selectedOrder.getServices().get(0);

            String dateStr = DateUtils.formatDateForDB(datePickerLocal.getValue());
            String timeStr = timeCombo.getValue();

            List<Appointment> existing = DataStore.getAppointmentsByDate(dateStr);
            for (Appointment a : existing) {
                if (a.getTime().equals(timeStr)) {
                    showAlert("Это время уже занято!");
                    return;
                }
            }

            Service newService = DataStore.getServiceByName(selectedServiceName);
            int newServiceId = (newService != null) ? newService.getId() : 0;

            Appointment appointment = new Appointment(
                    orderClient,
                    masterCombo.getValue(),
                    selectedServiceName,
                    dateStr,
                    timeStr
            );
            appointment.setServiceId(newServiceId);
            appointment.setOrderId(selectedOrder.getId());

            DataStore.addAppointment(appointment);

            refresh();
            stage.close();
        });

        cancelBtn.setOnAction(e -> stage.close());

        stage.setOnHiding(e -> WindowStateManager.getInstance().saveWindowState("editAppointmentDialog", stage));
        stage.showAndWait();
    }

    // ====== МЕТОД РЕДАКТИРОВАНИЯ ЗАПИСИ ПОСЛЕ DRAG AND DROP ======
    private static void showEditAppointmentDialog(String appointmentId, String originalDate, String originalTime, 
                                                    String originalMaster, LocalDate newDate, String newTime) {
        Stage stage = new Stage();
        stage.setTitle("Переместить запись");
        stage.setMinWidth(500);
        stage.setMinHeight(400);
        stage.initModality(Modality.WINDOW_MODAL);

        WindowStateManager.getInstance().restoreWindowState("editAppointmentDialog", stage);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label("Переместить запись");
        titleLabel.getStyleClass().add("dialog-title");

        // Информация о текущей записи
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(15);
        infoGrid.setVgap(8);
        infoGrid.getStyleClass().add("dialog-grid");
        
        Appointment existingAppointment = DataStore.getAppointmentById(Integer.parseInt(appointmentId));
        
        if (existingAppointment != null) {
            Client client = existingAppointment.getClient();
            String fullName = (client.getLastName() != null && !client.getLastName().isEmpty())
                    ? client.getLastName() + " " + client.getName()
                    : client.getName();

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
            infoGrid.add(new Label(existingAppointment.getMasterName()), 1, 3);

            infoGrid.add(new Label("Услуга:"), 0, 4);
            infoGrid.add(new Label(existingAppointment.getServiceName()), 1, 4);
        }

        // Поля для новой даты и времени
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.getStyleClass().add("dialog-grid");

        ComboBox<String> masterCombo = new ComboBox<>(FXCollections.observableArrayList(AppConstants.MASTERS));
        masterCombo.setValue(originalMaster);
        masterCombo.setPromptText("Выберите мастера");
        masterCombo.setPrefWidth(200);

        DatePicker datePickerLocal = new DatePicker(newDate);
        datePickerLocal.setPrefWidth(200);

        ComboBox<String> timeCombo = new ComboBox<>(FXCollections.observableArrayList(AppConstants.TIME_SLOTS));
        timeCombo.setValue(newTime);
        timeCombo.setPromptText("Выберите время");
        timeCombo.setPrefWidth(100);
        TooltipHelper.setToolTip(masterCombo, "Выберите мастера сервиса");

        grid.add(new Label("Мастер:"), 0, 0);
        grid.add(masterCombo, 1, 0);
        grid.add(new Label("Дата:"), 0, 1);
        grid.add(datePickerLocal, 1, 1);
        grid.add(new Label("Время:"), 0, 2);
        grid.add(timeCombo, 1, 2);

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        Button saveBtn = new Button("Сохранить изменения");
        saveBtn.getStyleClass().add("save-button");

        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("cancel-button");

        btnBox.getChildren().addAll(saveBtn, cancelBtn);

        root.getChildren().addAll(titleLabel, infoGrid, grid, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
            if (masterCombo.getValue() == null) {
                showAlert("Выберите мастера");
                return;
            }
            if (timeCombo.getValue() == null) {
                showAlert("Выберите время");
                return;
            }
            if (datePickerLocal.getValue() == null) {
                showAlert("Выберите дату");
                return;
            }

            // ====== ПРОВЕРКА ВЫХОДНЫХ ДНЕЙ ======
            if (DateUtils.isWeekend(datePickerLocal.getValue())) {
                Alert weekendAlert = new Alert(Alert.AlertType.CONFIRMATION);
                weekendAlert.setTitle("Подтверждение");
                weekendAlert.setHeaderText("Выбран выходной день!");
                weekendAlert.setContentText("Запись в выходной день (" + datePickerLocal.getValue().format(java.time.format.DateTimeFormatter.ofPattern("EEEE", new java.util.Locale("ru"))) + ") может быть ограничена.\n\nПродолжить?");
                weekendAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                
                if (weekendAlert.showAndWait().orElse(ButtonType.NO) == ButtonType.NO) {
                    return;
                }
            }

            String newDateStr = DateUtils.formatDateForDB(datePickerLocal.getValue());
            String newTimeStr = timeCombo.getValue();
            String newMaster = masterCombo.getValue();

            // ====== ПРОВЕРКА КОНФЛИКТА ВРЕМЕНИ ======
            List<Appointment> existing = DataStore.getAppointmentsByDate(newDateStr);
            for (Appointment a : existing) {
                if (a.getTime().equals(newTimeStr) && a.getMasterName().equals(newMaster)) {
                    if (a.getId() != existingAppointment.getId()) {
                        showAlert("Это время уже занято другим клиентом!");
                        return;
                    }
                }
            }

            // ====== ОБНОВЛЕНИЕ ЗАПИСИ ======
            existingAppointment.setDate(newDateStr);
            existingAppointment.setTime(newTimeStr);
            existingAppointment.setMasterName(newMaster);
            DataStore.updateAppointment(existingAppointment);

            refresh();
            stage.close();
        });

        cancelBtn.setOnAction(e -> stage.close());

        stage.setOnHiding(e -> WindowStateManager.getInstance().saveWindowState("editAppointmentDialog", stage));
        stage.showAndWait();
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }
}