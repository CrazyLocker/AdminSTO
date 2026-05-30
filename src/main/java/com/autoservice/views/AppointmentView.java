package com.autoservice.views;

import com.autoservice.*;
import com.autoservice.controllers.OrderController;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #f5f7fa;");

        // Верхняя панель
        HBox topPanel = new HBox(15);
        topPanel.setAlignment(Pos.CENTER_LEFT);
        topPanel.setStyle("-fx-background-color: white; -fx-padding: 12; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        // Переключатель вида
        viewToggle = new ToggleGroup();
        RadioButton dayView = new RadioButton("День");
        RadioButton weekView = new RadioButton("Неделя");
        RadioButton monthView = new RadioButton("Месяц");

        dayView.setToggleGroup(viewToggle);
        weekView.setToggleGroup(viewToggle);
        monthView.setToggleGroup(viewToggle);
        weekView.setSelected(true);

        dayView.setStyle("-fx-font-size: 13px; -fx-padding: 5 10;");
        weekView.setStyle("-fx-font-size: 13px; -fx-padding: 5 10;");
        monthView.setStyle("-fx-font-size: 13px; -fx-padding: 5 10;");

        dayView.setOnAction(e -> {
            currentView = "day";
            loadDayView();
        });
        weekView.setOnAction(e -> {
            currentView = "week";
            loadWeekView();
        });
        monthView.setOnAction(e -> {
            currentView = "month";
            loadMonthView();
        });

        HBox viewBox = new HBox(5, dayView, weekView, monthView);
        viewBox.setStyle("-fx-padding: 5; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");

        selectedDateLabel = new Label();
        selectedDateLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        datePicker = new DatePicker(LocalDate.now());
        datePicker.setStyle("-fx-font-size: 13px; -fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 4;");
        datePicker.setOnAction(e -> refreshView());

        Button todayBtn = new Button("Сегодня");
        todayBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 12; -fx-background-radius: 4;");
        todayBtn.setOnAction(e -> {
            datePicker.setValue(LocalDate.now());
            refreshView();
        });

        Button refreshBtn = new Button("🔄 Обновить");
        refreshBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 12; -fx-background-radius: 4;");
        refreshBtn.setOnAction(e -> refreshView());

        Button addBtn = new Button("+ Новая запись");
        addBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 6 18; -fx-background-radius: 4; -fx-font-weight: bold;");
        addBtn.setOnAction(e -> showAddAppointmentDialog(null));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topPanel.getChildren().addAll(
                viewBox,
                new Label("📅 Дата:"), datePicker, todayBtn, refreshBtn,
                spacer,
                addBtn,
                selectedDateLabel
        );

        // ScrollPane для прокрутки
        scheduleGrid = new GridPane();
        scheduleGrid.setHgap(8);
        scheduleGrid.setVgap(8);
        scheduleGrid.setPadding(new Insets(15));

        scrollPane = new ScrollPane(scheduleGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 8;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        root.getChildren().addAll(topPanel, scrollPane);

        refreshView();

        return root;
    }

    private static void refreshView() {
        if ("day".equals(currentView)) {
            loadDayView();
        } else if ("week".equals(currentView)) {
            loadWeekView();
        } else {
            loadMonthView();
        }
    }

    public static void refresh() {
        refreshView();
    }

    private static void loadDayView() {
        LocalDate date = datePicker.getValue();
        if (date == null) return;

        selectedDateLabel.setText(HEADER_FORMATTER.format(date));
        currentAppointments = DataStore.getAppointmentsByDate(date.toString());

        scheduleGrid.getChildren().clear();

        // Заголовки с увеличенным шрифтом
        Label timeHeader = new Label("Время");
        timeHeader.setStyle("-fx-font-weight: bold; -fx-background-color: #34495e; -fx-text-fill: white; -fx-padding: 12; -fx-font-size: 13px; -fx-background-radius: 5 5 0 0;");
        scheduleGrid.add(timeHeader, 0, 0);

        Label masterHeader = new Label("Мастер");
        masterHeader.setStyle("-fx-font-weight: bold; -fx-background-color: #34495e; -fx-text-fill: white; -fx-padding: 12; -fx-font-size: 13px; -fx-background-radius: 5 5 0 0;");
        scheduleGrid.add(masterHeader, 1, 0);

        Label clientHeader = new Label("Клиент / Авто / Услуга");
        clientHeader.setStyle("-fx-font-weight: bold; -fx-background-color: #34495e; -fx-text-fill: white; -fx-padding: 12; -fx-font-size: 13px; -fx-background-radius: 5 5 0 0;");
        scheduleGrid.add(clientHeader, 2, 0);

        Label actionHeader = new Label("Действия");
        actionHeader.setStyle("-fx-font-weight: bold; -fx-background-color: #34495e; -fx-text-fill: white; -fx-padding: 12; -fx-font-size: 13px; -fx-background-radius: 5 5 0 0;");
        scheduleGrid.add(actionHeader, 3, 0);

        for (int i = 0; i < TIME_SLOTS.length; i++) {
            String time = TIME_SLOTS[i];
            int row = i + 1;

            Label timeLabel = new Label(time);
            timeLabel.setStyle("-fx-padding: 12; -fx-background-color: #ecf0f1; -fx-border-color: #dddddd; -fx-border-width: 0 0 1 0; -fx-font-weight: bold; -fx-font-size: 13px;");
            scheduleGrid.add(timeLabel, 0, row);

            Appointment appointment = findAppointmentByTime(time);

            if (appointment != null) {
                addAppointmentRow(row, appointment);
            } else {
                addEmptyRow(row, time);
            }
        }

        scrollPane.setVvalue(0);
    }

    private static void addAppointmentRow(int row, Appointment appointment) {
        Client client = appointment.getClient();

        Label masterLabel = new Label(appointment.getMasterName());
        masterLabel.setStyle("-fx-padding: 12; -fx-background-color: #ffcccc; -fx-border-color: #dddddd; -fx-border-width: 0 0 1 0; -fx-font-weight: bold; -fx-text-fill: #c0392b; -fx-font-size: 13px;");
        scheduleGrid.add(masterLabel, 1, row);

        VBox infoBox = new VBox(5);
        infoBox.setStyle("-fx-padding: 10; -fx-background-color: #ffcccc; -fx-border-color: #dddddd; -fx-border-width: 0 0 1 0;");

        // Фамилия + Имя
        String fullName = (client.getLastName() != null && !client.getLastName().isEmpty())
                ? client.getLastName() + " " + client.getName()
                : client.getName();
        Label nameLabel = new Label("👤 " + fullName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Авто + Госномер
        String carInfo = client.getCarModel() + " (" + client.getCarNumber() + ")";
        Label carLabel = new Label("🚗 " + carInfo);
        carLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        // Услуга
        Label serviceLabel = new Label("🔧 " + appointment.getServiceName());
        serviceLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #2980b9; -fx-font-weight: bold;");

        infoBox.getChildren().addAll(nameLabel, carLabel, serviceLabel);
        scheduleGrid.add(infoBox, 2, row);

        HBox btnBox = new HBox(8);
        btnBox.setAlignment(Pos.CENTER);

        Button editBtn = new Button("✏");
        editBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 6 12; -fx-background-radius: 4;");
        editBtn.setOnAction(e -> showEditAppointmentDialog(appointment));

        Button cancelBtn = new Button("🗑");
        cancelBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 6 12; -fx-background-radius: 4;");
        cancelBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Отменить запись?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    DataStore.deleteAppointment(appointment.getId());
                    refresh();
                }
            });
        });

        btnBox.getChildren().addAll(editBtn, cancelBtn);
        scheduleGrid.add(btnBox, 3, row);
    }

    private static void addEmptyRow(int row, String time) {
        Label masterLabel = new Label("свободно");
        masterLabel.setStyle("-fx-padding: 12; -fx-background-color: #d5f5e3; -fx-border-color: #dddddd; -fx-border-width: 0 0 1 0; -fx-text-fill: #27ae60; -fx-font-style: italic; -fx-font-size: 13px;");
        scheduleGrid.add(masterLabel, 1, row);

        Label infoLabel = new Label("");
        infoLabel.setStyle("-fx-padding: 12; -fx-background-color: #d5f5e3; -fx-border-color: #dddddd; -fx-border-width: 0 0 1 0;");
        scheduleGrid.add(infoLabel, 2, row);

        Button addSlotBtn = new Button("Записать");
        addSlotBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 6 18; -fx-background-radius: 4; -fx-font-weight: bold;");
        addSlotBtn.setOnAction(e -> showAddAppointmentDialog(time));

        HBox btnBox = new HBox(8);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.getChildren().add(addSlotBtn);
        scheduleGrid.add(btnBox, 3, row);
    }

    private static void loadWeekView() {
        LocalDate startDate = datePicker.getValue();
        if (startDate == null) return;

        while (startDate.getDayOfWeek().getValue() != 1) {
            startDate = startDate.minusDays(1);
        }

        selectedDateLabel.setText("Неделя " + startDate.format(WEEK_FORMATTER) + " - " + startDate.plusDays(6).format(WEEK_FORMATTER));
        scheduleGrid.getChildren().clear();

        Label timeHeader = new Label("Время");
        timeHeader.setStyle("-fx-font-weight: bold; -fx-background-color: #34495e; -fx-text-fill: white; -fx-padding: 12; -fx-font-size: 13px;");
        scheduleGrid.add(timeHeader, 0, 0);

        for (int i = 0; i < 7; i++) {
            LocalDate day = startDate.plusDays(i);
            String dayName = day.format(DateTimeFormatter.ofPattern("E\nd.MMM", new Locale("ru")));
            Label dayHeader = new Label(dayName);
            dayHeader.setStyle("-fx-font-weight: bold; -fx-background-color: #34495e; -fx-text-fill: white; -fx-padding: 12; -fx-font-size: 12px; -fx-text-alignment: center;");
            scheduleGrid.add(dayHeader, i + 1, 0);
        }

        for (int i = 0; i < TIME_SLOTS.length; i++) {
            String time = TIME_SLOTS[i];
            int row = i + 1;

            Label timeLabel = new Label(time);
            timeLabel.setStyle("-fx-padding: 10; -fx-background-color: #ecf0f1; -fx-border-color: #dddddd; -fx-border-width: 0 0 1 0; -fx-font-size: 12px;");
            scheduleGrid.add(timeLabel, 0, row);

            for (int j = 0; j < 7; j++) {
                LocalDate day = startDate.plusDays(j);
                List<Appointment> dayAppointments = DataStore.getAppointmentsByDate(day.toString());
                Appointment appointment = findAppointmentByTimeInList(dayAppointments, time);

                VBox cell = new VBox(5);
                cell.setStyle("-fx-padding: 8; -fx-border-color: #dddddd; -fx-border-width: 0 0 1 0; -fx-min-height: 80;");

                if (appointment != null) {
                    Client client = appointment.getClient();
                    String fullName = (client.getLastName() != null && !client.getLastName().isEmpty())
                            ? client.getLastName() + " " + client.getName()
                            : client.getName();
                    String carInfo = client.getCarModel() + " (" + client.getCarNumber() + ")";

                    Label nameLabel = new Label("👤 " + fullName);
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
                    Label carLabel = new Label("🚗 " + carInfo);
                    carLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
                    Label serviceLabel = new Label("🔧 " + appointment.getServiceName());
                    serviceLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #2980b9;");
                    cell.getChildren().addAll(nameLabel, carLabel, serviceLabel);
                    cell.setStyle(cell.getStyle() + "-fx-background-color: #ffcccc;");
                } else {
                    Label freeLabel = new Label("свободно");
                    freeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60; -fx-font-style: italic;");
                    cell.getChildren().add(freeLabel);
                    cell.setStyle(cell.getStyle() + "-fx-background-color: #d5f5e3;");
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

        String[] weekDays = {"ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС"};
        for (int i = 0; i < 7; i++) {
            Label dayHeader = new Label(weekDays[i]);
            dayHeader.setStyle("-fx-font-weight: bold; -fx-background-color: #34495e; -fx-text-fill: white; -fx-padding: 12; -fx-font-size: 13px;");
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
                cell.setStyle("-fx-padding: 8; -fx-border-color: #dddddd; -fx-border-width: 1; -fx-min-height: 100; -fx-min-width: 100;");
                if (!isCurrentMonth) {
                    cell.setStyle(cell.getStyle() + "-fx-background-color: #f5f5f5;");
                }

                Label dateLabel = new Label(String.valueOf(cellDate.getDayOfMonth()));
                dateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                cell.getChildren().add(dateLabel);

                List<Appointment> dayAppointments = DataStore.getAppointmentsByDate(cellDate.toString());
                int count = 0;
                for (Appointment a : dayAppointments) {
                    if (count >= 2) {
                        Label moreLabel = new Label("... и ещё " + (dayAppointments.size() - 2));
                        moreLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #7f8c8d;");
                        cell.getChildren().add(moreLabel);
                        break;
                    }
                    Client client = a.getClient();
                    String fullName = (client.getLastName() != null && !client.getLastName().isEmpty())
                            ? client.getLastName() + " " + client.getName()
                            : client.getName();
                    Label appLabel = new Label(a.getTime() + " - " + fullName);
                    appLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #2980b9;");
                    cell.getChildren().add(appLabel);
                    count++;
                }

                if (dayAppointments.isEmpty() && isCurrentMonth) {
                    Label freeLabel = new Label("свободно");
                    freeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #27ae60; -fx-font-style: italic;");
                    cell.getChildren().add(freeLabel);
                }

                scheduleGrid.add(cell, col, row + 1);
            }
        }
    }

    private static Appointment findAppointmentByTime(String time) {
        if (currentAppointments == null) return null;
        for (Appointment a : currentAppointments) {
            if (a.getTime().equals(time)) {
                return a;
            }
        }
        return null;
    }

    private static Appointment findAppointmentByTimeInList(List<Appointment> appointments, String time) {
        for (Appointment a : appointments) {
            if (a.getTime().equals(time)) {
                return a;
            }
        }
        return null;
    }

    private static void showEditAppointmentDialog(Appointment appointment) {
        Stage stage = new Stage();
        stage.setTitle("Редактирование записи");
        stage.setMinWidth(450);
        stage.setMinHeight(500);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Редактирование записи");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);

        ComboBox<Client> clientCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getClients()));
        clientCombo.setValue(appointment.getClient());
        clientCombo.setPrefWidth(300);
        clientCombo.setStyle("-fx-padding: 6; -fx-font-size: 13px;");

        ComboBox<String> masterCombo = new ComboBox<>(FXCollections.observableArrayList(MASTERS));
        masterCombo.setValue(appointment.getMasterName());
        masterCombo.setPrefWidth(200);
        masterCombo.setStyle("-fx-padding: 6; -fx-font-size: 13px;");

        ComboBox<String> serviceCombo = new ComboBox<>(FXCollections.observableArrayList(SERVICES));
        serviceCombo.setValue(appointment.getServiceName());
        serviceCombo.setPrefWidth(200);
        serviceCombo.setStyle("-fx-padding: 6; -fx-font-size: 13px;");

        DatePicker datePickerLocal = new DatePicker(LocalDate.parse(appointment.getDate()));
        datePickerLocal.setPrefWidth(200);
        datePickerLocal.setStyle("-fx-font-size: 13px;");

        ComboBox<String> timeCombo = new ComboBox<>(FXCollections.observableArrayList(TIME_SLOTS));
        timeCombo.setValue(appointment.getTime());
        timeCombo.setPrefWidth(100);
        timeCombo.setStyle("-fx-font-size: 13px;");

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
        saveBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 4; -fx-font-size: 13px;");

        Button cancelBtn = new Button("Отмена");
        cancelBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 4; -fx-font-size: 13px;");

        btnBox.getChildren().addAll(saveBtn, cancelBtn);

        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
            String newDate = datePickerLocal.getValue().toString();
            String newTime = timeCombo.getValue();

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

        cancelBtn.setOnAction(e -> stage.close());

        stage.showAndWait();
    }

    private static void showAddAppointmentDialog(String presetTime) {
        Stage stage = new Stage();
        stage.setTitle("Новая запись");
        stage.setMinWidth(450);
        stage.setMinHeight(550);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("Новая запись");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);

        ComboBox<Client> clientCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getClients()));
        clientCombo.setPromptText("Выберите клиента");
        clientCombo.setPrefWidth(300);
        clientCombo.setStyle("-fx-padding: 6; -fx-font-size: 13px;");

        ComboBox<String> masterCombo = new ComboBox<>(FXCollections.observableArrayList(MASTERS));
        masterCombo.setPromptText("Выберите мастера");
        masterCombo.setPrefWidth(200);
        masterCombo.setStyle("-fx-padding: 6; -fx-font-size: 13px;");

        ComboBox<String> serviceCombo = new ComboBox<>(FXCollections.observableArrayList(SERVICES));
        serviceCombo.setPromptText("Выберите услугу");
        serviceCombo.setPrefWidth(200);
        serviceCombo.setStyle("-fx-padding: 6; -fx-font-size: 13px;");

        DatePicker datePickerLocal = new DatePicker(datePicker.getValue());
        datePickerLocal.setPrefWidth(200);
        datePickerLocal.setStyle("-fx-font-size: 13px;");

        ComboBox<String> timeCombo = new ComboBox<>(FXCollections.observableArrayList(TIME_SLOTS));
        if (presetTime != null) {
            timeCombo.setValue(presetTime);
        }
        timeCombo.setPromptText("Выберите время");
        timeCombo.setPrefWidth(100);
        timeCombo.setStyle("-fx-padding: 6; -fx-font-size: 13px;");

        CheckBox createOrderCheck = new CheckBox("Создать заказ");
        createOrderCheck.setStyle("-fx-font-size: 13px;");

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
        saveBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 4; -fx-font-size: 13px;");

        Button cancelBtn = new Button("Отмена");
        cancelBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 4; -fx-font-size: 13px;");

        btnBox.getChildren().addAll(saveBtn, cancelBtn);

        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
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
                double price = 0;
                for (Service s : DataStore.getServices()) {
                    if (s.getName().equals(serviceCombo.getValue())) {
                        price = s.getPrice();
                        break;
                    }
                }
                order.addService(serviceCombo.getValue(), price);
                DataStore.addOrder(order);
                appointment.setOrderId(order.getId());
                DataStore.updateAppointment(appointment);
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