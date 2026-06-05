package com.autoservice.views;

import com.autoservice.*;
import com.autoservice.controllers.OrderController;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppointmentView {

    private static final String[] TIME_SLOTS = {
            "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
            "12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30",
            "16:00", "16:30", "17:00", "17:30", "18:00", "18:30", "19:00", "19:30", "20:00"
    };

    private static final String[] MASTERS = {"Иван", "Петр", "Сергей", "Антон"};
    private static String[] SERVICES;

    private static DatePicker datePicker;
    private static ScrollPane scrollPane;
    private static GridPane scheduleGrid;
    private static Label selectedDateLabel;
    private static List<Appointment> currentAppointments;
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

        // Переключатель вида - только Неделя и Месяц
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

        Button todayBtn = new Button("Сегодня");
        todayBtn.getStyleClass().add("today-button");
        todayBtn.setOnAction(e -> {
            datePicker.setValue(LocalDate.now());
            refreshView();
        });

        Button refreshBtn = new Button("Обновить");
        refreshBtn.getStyleClass().add("refresh-button");
        refreshBtn.setOnAction(e -> refreshView());

        Button addBtn = new Button("Новая запись");
        addBtn.getStyleClass().add("add-appointment-button");
        addBtn.setOnAction(e -> showAddAppointmentDialog(null));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topPanel.getChildren().addAll(
                viewBox,
                new Label("Дата:"), datePicker, todayBtn, refreshBtn,
                spacer,
                addBtn,
                selectedDateLabel
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

        for (int i = 0; i < TIME_SLOTS.length; i++) {
            String time = TIME_SLOTS[i];
            int row = i + 1;

            Label timeLabel = new Label(time);
            timeLabel.getStyleClass().add("time-label-week");
            scheduleGrid.add(timeLabel, 0, row);

            for (int j = 0; j < 7; j++) {
                LocalDate day = startDate.plusDays(j);
                List<Appointment> dayAppointments = DataStore.getAppointmentsByDate(day.toString());
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
                    cell.getStyleClass().add("week-cell-booked");

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

                    // Двойной клик по свободной плитке - открыть форму создания заказа
                    final LocalDate finalDay = day;
                    final String finalTime = time;
                    cell.setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2) {
                            // Устанавливаем дату и время
                            datePicker.setValue(finalDay);
                            // Открываем форму создания заказа
                            OrderController.createOrder();
                        }
                    });
                }
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

                List<Appointment> dayAppointments = DataStore.getAppointmentsByDate(cellDate.toString());

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

                        final Appointment currentAppointment = a;
                        appLabel.setOnMouseClicked(event -> {
                            if (event.getClickCount() == 2) {
                                showAppointmentInfoOnly(currentAppointment);
                            }
                        });

                        cell.getChildren().add(appLabel);
                        count++;
                    }
                } else if (isCurrentMonth) {
                    Label freeLabel = new Label("свободно");
                    freeLabel.getStyleClass().add("month-free-label");
                    cell.getChildren().add(freeLabel);
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

    private static void showAppointmentDetails(Appointment appointment) {
        Stage stage = new Stage();
        stage.setTitle("Детали записи");
        stage.setMinWidth(500);
        stage.setMinHeight(500);
        stage.initModality(Modality.WINDOW_MODAL);

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
                            int quantity = order.getSparePartQuantities().get(i);
                            Label partLabel = new Label("• " + part.getName() + " x" + quantity);
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
        closeBtn.getStyleClass().add("cancel-button");
        closeBtn.setOnAction(e -> stage.close());

        btnBox.getChildren().add(closeBtn);

        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(AppointmentView.class.getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
    }

    private static void showAppointmentInfoOnly(Appointment appointment) {
        Stage stage = new Stage();
        stage.setTitle("Информация о записи");
        stage.setMinWidth(500);
        stage.setMinHeight(500);
        stage.initModality(Modality.WINDOW_MODAL);

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
                            int quantity = order.getSparePartQuantities().get(i);
                            Label partLabel = new Label("• " + part.getName() + " x" + quantity);
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
        closeBtn.getStyleClass().add("cancel-button");
        closeBtn.setOnAction(e -> stage.close());

        btnBox.getChildren().add(closeBtn);

        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(AppointmentView.class.getResource("/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
    }

    private static void showEditAppointmentDialog(Appointment appointment) {
        Stage stage = new Stage();
        stage.setTitle("Редактирование записи");
        stage.setMinWidth(500);
        stage.setMinHeight(550);
        stage.initModality(Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label("Редактирование записи");
        titleLabel.getStyleClass().add("dialog-title");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.getStyleClass().add("dialog-grid");

        ComboBox<Client> clientCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getClients()));
        clientCombo.setValue(appointment.getClient());
        clientCombo.setPrefWidth(300);
        clientCombo.getStyleClass().add("dialog-combo");

        ComboBox<String> masterCombo = new ComboBox<>(FXCollections.observableArrayList(MASTERS));
        masterCombo.setValue(appointment.getMasterName());
        masterCombo.setPrefWidth(200);
        masterCombo.getStyleClass().add("dialog-combo");

        ComboBox<String> serviceCombo = new ComboBox<>(FXCollections.observableArrayList(SERVICES));
        serviceCombo.setValue(appointment.getServiceName());
        serviceCombo.setPrefWidth(200);
        serviceCombo.getStyleClass().add("dialog-combo");

        DatePicker datePickerLocal = new DatePicker(LocalDate.parse(appointment.getDate()));
        datePickerLocal.setPrefWidth(200);
        datePickerLocal.getStyleClass().add("dialog-datepicker");

        ComboBox<String> timeCombo = new ComboBox<>(FXCollections.observableArrayList(TIME_SLOTS));
        timeCombo.setValue(appointment.getTime());
        timeCombo.setPrefWidth(100);
        timeCombo.getStyleClass().add("dialog-combo");

        boolean hasOrder = appointment.getOrderId() != null && !appointment.getOrderId().isEmpty();

        grid.add(new Label("Клиент:"), 0, 0);
        grid.add(clientCombo, 1, 0);
        grid.add(new Label("Мастер:"), 0, 1);
        grid.add(masterCombo, 1, 1);
        grid.add(new Label("Услуга:"), 0, 2);
        grid.add(serviceCombo, 1, 2);
        grid.add(new Label("Дата:"), 0, 3);
        grid.add(datePickerLocal, 1, 3);
        grid.add(new Label("Время:"), 0, 4);
        grid.add(timeCombo, 1, 4);

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        Button saveBtn = new Button("Сохранить");
        saveBtn.getStyleClass().add("save-button");

        Button deleteBtn = new Button("Удалить");
        deleteBtn.getStyleClass().add("delete-button");

        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("cancel-button");

        btnBox.getChildren().addAll(saveBtn, deleteBtn, cancelBtn);

        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(AppointmentView.class.getResource("/styles.css").toExternalForm());
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
            String newDate = datePickerLocal.getValue().toString();
            String newTime = timeCombo.getValue();

            if (newTime == null) {
                showAlert("Выберите время");
                return;
            }

            if (!newDate.equals(appointment.getDate()) || !newTime.equals(appointment.getTime())) {
                List<Appointment> existing = DataStore.getAppointmentsByDate(newDate);
                for (Appointment a : existing) {
                    if (a.getTime().equals(newTime) && a.getId() != appointment.getId()) {
                        showAlert("Это время уже занято!");
                        return;
                    }
                }
            }

            appointment.setClient(clientCombo.getValue());
            appointment.setMasterName(masterCombo.getValue());
            appointment.setServiceName(serviceCombo.getValue());
            appointment.setDate(newDate);
            appointment.setTime(newTime);

            DataStore.updateAppointment(appointment);
            refresh();
            stage.close();
        });

        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Удалить запись?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    DataStore.deleteAppointment(appointment.getId());
                    refresh();
                    stage.close();
                }
            });
        });

        cancelBtn.setOnAction(e -> stage.close());

        stage.showAndWait();
    }

    private static void showAddAppointmentDialog(String presetTime) {
        Stage stage = new Stage();
        stage.setTitle("Новая запись");
        stage.setMinWidth(500);
        stage.setMinHeight(600);
        stage.initModality(Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label("Новая запись");
        titleLabel.getStyleClass().add("dialog-title");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.getStyleClass().add("dialog-grid");

        ComboBox<Client> clientCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getClients()));
        clientCombo.setPromptText("Выберите клиента");
        clientCombo.setPrefWidth(300);
        clientCombo.getStyleClass().add("dialog-combo");

        ComboBox<String> masterCombo = new ComboBox<>(FXCollections.observableArrayList(MASTERS));
        masterCombo.setPromptText("Выберите мастера");
        masterCombo.setPrefWidth(200);
        masterCombo.getStyleClass().add("dialog-combo");

        ComboBox<String> serviceCombo = new ComboBox<>(FXCollections.observableArrayList(SERVICES));
        serviceCombo.setPromptText("Выберите услугу");
        serviceCombo.setPrefWidth(200);
        serviceCombo.getStyleClass().add("dialog-combo");

        DatePicker datePickerLocal = new DatePicker(datePicker.getValue());
        datePickerLocal.setPrefWidth(200);
        datePickerLocal.getStyleClass().add("dialog-datepicker");

        ComboBox<String> timeCombo = new ComboBox<>(FXCollections.observableArrayList(TIME_SLOTS));
        if (presetTime != null) {
            timeCombo.setValue(presetTime);
        }
        timeCombo.setPromptText("Выберите время");
        timeCombo.setPrefWidth(100);
        timeCombo.getStyleClass().add("dialog-combo");

        CheckBox createOrderCheck = new CheckBox("Создать заказ");
        createOrderCheck.getStyleClass().add("create-order-checkbox");

        grid.add(new Label("Клиент:"), 0, 0);
        grid.add(clientCombo, 1, 0);
        grid.add(new Label("Мастер:"), 0, 1);
        grid.add(masterCombo, 1, 1);
        grid.add(new Label("Услуга:"), 0, 2);
        grid.add(serviceCombo, 1, 2);
        grid.add(new Label("Дата:"), 0, 3);
        grid.add(datePickerLocal, 1, 3);
        grid.add(new Label("Время:"), 0, 4);
        grid.add(timeCombo, 1, 4);
        grid.add(new Label(""), 0, 5);
        grid.add(createOrderCheck, 1, 5);

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        Button saveBtn = new Button("Создать запись");
        saveBtn.getStyleClass().add("save-button");

        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("cancel-button");

        btnBox.getChildren().addAll(saveBtn, cancelBtn);

        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(AppointmentView.class.getResource("/styles.css").toExternalForm());
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
            if (clientCombo.getValue() == null) { showAlert("Выберите клиента"); return; }
            if (masterCombo.getValue() == null) { showAlert("Выберите мастера"); return; }
            if (serviceCombo.getValue() == null) { showAlert("Выберите услугу"); return; }
            if (timeCombo.getValue() == null) { showAlert("Выберите время"); return; }

            String dateStr = datePickerLocal.getValue().toString();
            String timeStr = timeCombo.getValue();

            List<Appointment> existing = DataStore.getAppointmentsByDate(dateStr);
            for (Appointment a : existing) {
                if (a.getTime().equals(timeStr)) {
                    showAlert("Это время уже занято!");
                    return;
                }
            }

            Appointment appointment = new Appointment(
                    clientCombo.getValue(),
                    masterCombo.getValue(),
                    serviceCombo.getValue(),
                    dateStr,
                    timeStr
            );

            DataStore.addAppointment(appointment);

            if (createOrderCheck.isSelected()) {
                WorkOrder order = new WorkOrder(clientCombo.getValue());
                order.setStatus("Новый");
                double price = 0;
                for (Service s : DataStore.getServices()) {
                    if (s.getName().equals(serviceCombo.getValue())) {
                        price = s.getPrice();
                        break;
                    }
                }
                order.addService(serviceCombo.getValue(), price);
                DataStore.addOrder(order);
                String orderId = order.getId();
                if (orderId != null && !orderId.isEmpty()) {
                    appointment.setOrderId(orderId);
                    DataStore.updateAppointment(appointment);
                }
                OrderController.refreshTable();
            }

            refresh();
            stage.close();
        });

        cancelBtn.setOnAction(e -> stage.close());

        stage.showAndWait();
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }
}