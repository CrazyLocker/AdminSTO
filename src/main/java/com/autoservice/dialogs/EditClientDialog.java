package com.autoservice.dialogs;

import com.autoservice.Appointment;
import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.DateUtils;
import com.autoservice.Validators;
import com.autoservice.controllers.ClientController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class EditClientDialog {

    public static void show(Client client) {
        Stage stage = new Stage();
        stage.setTitle("Редактирование клиента");
        stage.setMinWidth(500);
        stage.setMinHeight(450);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(client.getName());
        nameField.setPromptText("Имя");

        TextField phoneField = new TextField(client.getPhone());
        Validators.setupPhoneField(phoneField);

        TextField carModelField = new TextField(client.getCarModel());
        carModelField.setPromptText("Модель авто");

        TextField carNumberField = new TextField(client.getCarNumber());
        Validators.setupCarNumberField(carNumberField);

        // Информация о последнем ремонте (только для чтения)
        Label lastRepairLabel = new Label();
        if (client.getLastRepairDate() != null && !client.getLastRepairDate().isEmpty()) {
            lastRepairLabel.setText(DateUtils.formatDate(client.getLastRepairDate()));
            lastRepairLabel.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
        } else {
            lastRepairLabel.setText("Нет завершённых заказов");
            lastRepairLabel.setStyle("-fx-text-fill: #FF9800;");
        }

        // Информация о текущей записи
        Label currentAppointmentLabel = new Label();
        currentAppointmentLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-size: 12px; -fx-padding: 5 0 5 0;");

        // Загружаем текущую запись клиента
        Appointment currentAppointment = null;
        for (Appointment a : DataStore.getAppointments()) {
            if (a.getClient().getId() == client.getId() &&
                    a.getStatus().equals(Appointment.STATUS_SCHEDULED)) {
                currentAppointment = a;
                break;
            }
        }

        if (currentAppointment != null) {
            String date = DateUtils.formatDate(currentAppointment.getDate());
            String time = currentAppointment.getTime();
            String master = currentAppointment.getMasterName();
            String service = currentAppointment.getServiceName();
            currentAppointmentLabel.setText("Текущая запись: " + date + ", " + time + ", мастер: " + master + ", услуга: " + service);
        } else {
            currentAppointmentLabel.setText("Нет активных записей");
            currentAppointmentLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-size: 12px; -fx-padding: 5 0 5 0;");
        }

        grid.add(new Label("Имя:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Телефон:"), 0, 1);
        grid.add(phoneField, 1, 1);
        grid.add(new Label("Модель:"), 0, 2);
        grid.add(carModelField, 1, 2);
        grid.add(new Label("Госномер:"), 0, 3);
        grid.add(carNumberField, 1, 3);
        grid.add(new Label("Последний ремонт:"), 0, 4);
        grid.add(lastRepairLabel, 1, 4);
        grid.add(new Label("Текущая запись:"), 0, 5);
        grid.add(currentAppointmentLabel, 1, 5);

        Button saveBtn = new Button("Сохранить");
        Button cancelBtn = new Button("Отмена");
        HBox btnBox = new HBox(15, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(grid, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty()) {
                showAlert("Введите имя клиента");
                return;
            }
            if (!Validators.isValidPhone(phoneField.getText())) {
                showAlert("Неверный формат телефона. Должно быть +7XXXXXXXXXX (10 цифр)");
                return;
            }
            if (!Validators.isValidCarNumber(carNumberField.getText())) {
                showAlert("Неверный формат госномера.\nПримеры: А123ВС163, А123ВС16");
                return;
            }

            client.setName(nameField.getText());
            client.setPhone(phoneField.getText());
            client.setCarModel(carModelField.getText());
            client.setCarNumber(carNumberField.getText());

            ClientController.updateClient(client);
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