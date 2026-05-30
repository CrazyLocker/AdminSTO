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
    private static GridPane scheduleGrid;
    private static Label selectedDateLabel;
    private static List<Appointment> currentAppointments;

    private static final DateTimeFormatter HEADER_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy 'г.'", new Locale("ru"));

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
        topPanel.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");

        selectedDateLabel = new Label();
        selectedDateLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        datePicker = new DatePicker(LocalDate.now());
        datePicker.setStyle("-fx-font-size: 14px;");
        datePicker.setOnAction(e -> loadSchedule());

        Button todayBtn = new Button("Сегодня");
        todayBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 5 15; -fx-background-radius: 3;");
        todayBtn.setOnAction(e -> {
            datePicker.setValue(LocalDate.now());
            loadSchedule();
        });

        Button refreshBtn = new Button("🔄 Обновить");
        refreshBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 5 15; -fx-background-radius: 3;");
        refreshBtn.setOnAction(e -> loadSchedule());

        Button addBtn = new Button("+ Новая запись");
        addBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 20; -fx-background-radius: 5; -fx-font-weight: bold;");
        addBtn.setOnAction(e -> showAddAppointmentDialog(null));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topPanel.getChildren().addAll(
                new Label("Дата:"), datePicker, todayBtn, refreshBtn,
                spacer,
                addBtn,
                selectedDateLabel
        );

        // Таблица расписания
        scheduleGrid = new GridPane();
        scheduleGrid.setHgap(5);
        scheduleGrid.setVgap(5);
        scheduleGrid.setPadding(new Insets(10));
        scheduleGrid.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px;");

        VBox.setVgrow(scheduleGrid, Priority.ALWAYS);

        root.getChildren().addAll(topPanel, scheduleGrid);

        loadSchedule();

        return root;
    }

    public static void refresh() {
        if (datePicker != null) {
            loadSchedule();
        }
    }

    private static void loadSchedule() {
        LocalDate date = datePicker.getValue();
        if (date == null) return;

        String dateStr = date.toString();
        selectedDateLabel.setText(HEADER_FORMATTER.format(date));

        currentAppointments = DataStore.getAppointmentsByDate(dateStr);

        scheduleGrid.getChildren().clear();

        // Заголовки
        Label timeHeader = new Label("Время");
        timeHeader.setStyle("-fx-font-weight: bold; -fx-background-color: #dddddd; -fx-padding: 5;");
        scheduleGrid.add(timeHeader, 0, 0);

        Label masterHeader = new Label("Мастер");
        masterHeader.setStyle("-fx-font-weight: bold; -fx-background-color: #dddddd; -fx-padding: 5;");
        scheduleGrid.add(masterHeader, 1, 0);

        Label clientHeader = new Label("Клиент / Авто / Услуга");
        clientHeader.setStyle("-fx-font-weight: bold; -fx-background-color: #dddddd; -fx-padding: 5;");
        scheduleGrid.add(clientHeader, 2, 0);

        Label actionHeader = new Label("");
        actionHeader.setStyle("-fx-font-weight: bold; -fx-background-color: #dddddd; -fx-padding: 5;");
        scheduleGrid.add(actionHeader, 3, 0);

        for (int i = 0; i < TIME_SLOTS.length; i++) {
            String time = TIME_SLOTS[i];
            int row = i + 1;

            Label timeLabel = new Label(time);
            timeLabel.setStyle("-fx-padding: 5; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");
            scheduleGrid.add(timeLabel, 0, row);

            Appointment appointment = findAppointmentByTime(time);

            if (appointment != null) {
                // Занято - красная ячейка
                Label masterLabel = new Label(appointment.getMasterName());
                masterLabel.setStyle("-fx-padding: 5; -fx-background-color: #ffcccc; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");
                scheduleGrid.add(masterLabel, 1, row);

                Client client = appointment.getClient();
                String clientName = (client != null && client.getName() != null) ? client.getName() : "—";
                String carModel = (client != null && client.getCarModel() != null) ? client.getCarModel() : "—";
                String carNumber = (client != null && client.getCarNumber() != null) ? client.getCarNumber() : "—";
                String serviceName = (appointment.getServiceName() != null) ? appointment.getServiceName() : "—";

                VBox infoBox = new VBox(2);
                infoBox.setStyle("-fx-padding: 5; -fx-background-color: #ffcccc; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");

                Label nameLabel = new Label(clientName);
                nameLabel.setStyle("-fx-font-weight: bold;");

                Label carLabel = new Label(carModel + " (" + carNumber + ")");
                carLabel.setStyle("-fx-font-size: 11px;");

                Label serviceLabel = new Label(serviceName);
                serviceLabel.setStyle("-fx-font-size: 11px;");

                infoBox.getChildren().addAll(nameLabel, carLabel, serviceLabel);
                scheduleGrid.add(infoBox, 2, row);

                HBox btnBox = new HBox(5);

                Button editBtn = new Button("✏");
                editBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
                editBtn.setOnAction(e -> showEditAppointmentDialog(appointment));

                Button cancelBtn = new Button("✖");
                cancelBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
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
            } else {
                // Свободно - зелёная ячейка
                Label masterLabel = new Label("свободно");
                masterLabel.setStyle("-fx-padding: 5; -fx-background-color: #ccffcc; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");
                scheduleGrid.add(masterLabel, 1, row);

                Label infoLabel = new Label("");
                infoLabel.setStyle("-fx-padding: 5; -fx-background-color: #ccffcc; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");
                scheduleGrid.add(infoLabel, 2, row);

                Button addSlotBtn = new Button("Записать");
                addSlotBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                final String slotTime = time;
                addSlotBtn.setOnAction(e -> showAddAppointmentDialog(slotTime));
                scheduleGrid.add(addSlotBtn, 3, row);
            }
        }
    }

    private static void showEditAppointmentDialog(Appointment appointment) {
        Stage stage = new Stage();
        stage.setTitle("Редактирование записи");
        stage.setMinWidth(400);
        stage.setMinHeight(450);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        ComboBox<Client> clientCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getClients()));
        clientCombo.setValue(appointment.getClient());
        clientCombo.setPrefWidth(300);

        ComboBox<String> masterCombo = new ComboBox<>(FXCollections.observableArrayList(MASTERS));
        masterCombo.setValue(appointment.getMasterName());
        masterCombo.setPrefWidth(200);

        ComboBox<String> serviceCombo = new ComboBox<>(FXCollections.observableArrayList(SERVICES));
        serviceCombo.setValue(appointment.getServiceName());
        serviceCombo.setPrefWidth(200);

        DatePicker datePickerLocal = new DatePicker(LocalDate.parse(appointment.getDate()));
        datePickerLocal.setPrefWidth(200);

        ComboBox<String> timeCombo = new ComboBox<>(FXCollections.observableArrayList(TIME_SLOTS));
        timeCombo.setValue(appointment.getTime());
        timeCombo.setPrefWidth(100);

        Button saveBtn = new Button("Сохранить");
        Button cancelBtn = new Button("Отмена");
        HBox btnBox = new HBox(15, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(
                new Label("Клиент:"), clientCombo,
                new Label("Мастер:"), masterCombo,
                new Label("Услуга:"), serviceCombo,
                new Label("Дата:"), datePickerLocal,
                new Label("Время:"), timeCombo,
                btnBox
        );

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
        stage.setMinWidth(400);
        stage.setMinHeight(450);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        ComboBox<Client> clientCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getClients()));
        clientCombo.setPromptText("Выберите клиента");
        clientCombo.setPrefWidth(300);

        ComboBox<String> masterCombo = new ComboBox<>(FXCollections.observableArrayList(MASTERS));
        masterCombo.setPromptText("Выберите мастера");
        masterCombo.setPrefWidth(200);

        ComboBox<String> serviceCombo = new ComboBox<>(FXCollections.observableArrayList(SERVICES));
        serviceCombo.setPromptText("Выберите услугу");
        serviceCombo.setPrefWidth(200);

        DatePicker datePickerLocal = new DatePicker(datePicker.getValue());
        datePickerLocal.setPrefWidth(200);

        ComboBox<String> timeCombo = new ComboBox<>(FXCollections.observableArrayList(TIME_SLOTS));
        if (presetTime != null) {
            timeCombo.setValue(presetTime);
        }
        timeCombo.setPrefWidth(100);

        CheckBox createOrderCheck = new CheckBox("Создать заказ");

        Button saveBtn = new Button("Создать запись");
        Button cancelBtn = new Button("Отмена");
        HBox btnBox = new HBox(15, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(
                new Label("Клиент:"), clientCombo,
                new Label("Мастер:"), masterCombo,
                new Label("Услуга:"), serviceCombo,
                new Label("Дата:"), datePickerLocal,
                new Label("Время:"), timeCombo,
                createOrderCheck,
                btnBox
        );

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

    private static Appointment findAppointmentByTime(String time) {
        if (currentAppointments == null) return null;
        for (Appointment a : currentAppointments) {
            if (a.getTime().equals(time)) {
                return a;
            }
        }
        return null;
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }
}