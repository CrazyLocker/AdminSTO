package com.autoservice.dialogs;

import com.autoservice.Appointment;
import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.DateUtils;
import com.autoservice.Validators;
import com.autoservice.controllers.ClientController;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;

public class EditClientDialog {
    
    // Список моделей GWM для автодополнения
    private static final List<String> GWM_MODELS = Arrays.asList(
        "Haval Jolion",
        "Haval F7",
        "Haval F7x",
        "Haval F5",
        "Haval Dargo",
        "Haval Big Dog",
        "Haval H6",
        "Haval H9",
        "Haval H2",
        "Haval H4",
        "GWM Poer",
        "GWM Tank 300",
        "GWM Tank 500",
        "GWM Wingle 7",
        "GWM Stingray",
        "GWM Cannon"
    );

    public static void show(Client client) {
        Stage stage = new Stage();
        stage.setTitle("Редактирование клиента");
        stage.setMinWidth(600);
        stage.setMinHeight(550);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #fafafa;");

        // Заголовок
        Label titleLabel = new Label("\uD83D\uDCCD Редактирование клиента");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(10, 0, 10, 0));

        // Фамилия
        Label lastNameLabel = new Label("Фамилия:");
        lastNameLabel.setStyle("-fx-font-weight: bold;");
        TextField lastNameField = new TextField(client.getLastName());
        lastNameField.setPromptText("Иванов");
        lastNameField.setPrefWidth(250);
        lastNameField.setStyle(getTextFieldStyle());

        // Имя
        Label nameLabel = new Label("Имя:");
        nameLabel.setStyle("-fx-font-weight: bold;");
        TextField nameField = new TextField(client.getName());
        nameField.setPromptText("Иван");
        nameField.setPrefWidth(250);
        nameField.setStyle(getTextFieldStyle());

        // Телефон
        Label phoneLabel = new Label("Телефон:");
        phoneLabel.setStyle("-fx-font-weight: bold;");
        TextField phoneField = new TextField(client.getPhone());
        Validators.setupPhoneField(phoneField);
        phoneField.setPrefWidth(250);
        phoneField.setStyle(getTextFieldStyle());

        // Модель авто с автодополнением
        Label carModelLabel = new Label("Модель авто:");
        carModelLabel.setStyle("-fx-font-weight: bold;");
        ComboBox<String> carModelComboBox = new ComboBox<>();
        carModelComboBox.setPromptText("Выберите или введите модель");
        carModelComboBox.setPrefWidth(300);
        carModelComboBox.setItems(FXCollections.observableArrayList(GWM_MODELS));
        if (client.getCarModel() != null && !client.getCarModel().isEmpty()) {
            carModelComboBox.getEditor().setText(client.getCarModel());
        }
        carModelComboBox.setStyle("-fx-background-radius: 5; -fx-padding: 5;");

        // Госномер
        Label carNumberLabel = new Label("Госномер:");
        carNumberLabel.setStyle("-fx-font-weight: bold;");
        TextField carNumberField = new TextField(client.getCarNumber());
        Validators.setupCarNumberField(carNumberField);
        carNumberField.setPrefWidth(200);
        carNumberField.setStyle(getTextFieldStyle());

        grid.add(lastNameLabel, 0, 0);
        grid.add(lastNameField, 1, 0);
        grid.add(nameLabel, 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(phoneLabel, 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(carModelLabel, 0, 3);
        grid.add(carModelComboBox, 1, 3);
        grid.add(carNumberLabel, 0, 4);
        grid.add(carNumberField, 1, 4);

        // Информация о последнем ремонте (только для чтения)
        Label lastRepairLabel = new Label();
        Label lastRepairTitle = new Label("Последний ремонт:");
        lastRepairTitle.setStyle("-fx-font-weight: bold;");
        
        if (client.getLastRepairDate() != null && !client.getLastRepairDate().isEmpty()) {
            lastRepairLabel.setText(DateUtils.formatDate(client.getLastRepairDate()));
            lastRepairLabel.setStyle(getTextFieldStyle() + " -fx-text-fill: #2E7D32; -fx-font-weight: bold; -fx-background-color: #E8F5E9;");
        } else {
            lastRepairLabel.setText("Нет завер\u0448\u0451\u043D\u043D\u044B\u0445 заказов");
            lastRepairLabel.setStyle(getTextFieldStyle() + " -fx-text-fill: #FF9800; -fx-background-color: #FFF3E0;");
        }
        lastRepairLabel.setPickOnBounds(true);
        lastRepairLabel.setAlignment(Pos.CENTER_LEFT);

        // Информация о текущей записи
        Label currentAppointmentLabel = new Label();
        Label appointmentTitle = new Label("Текущая запись:");
        appointmentTitle.setStyle("-fx-font-weight: bold;");
        
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
            currentAppointmentLabel.setText(date + ", " + time + ", мастер: " + master + ", услуга: " + service);
            currentAppointmentLabel.setStyle(getTextFieldStyle() + " -fx-text-fill: #2E7D32; -fx-background-color: #E3F2FD;");
        } else {
            currentAppointmentLabel.setText("Нет активных записей");
            currentAppointmentLabel.setStyle(getTextFieldStyle() + " -fx-text-fill: #FF9800; -fx-background-color: #FFF3E0;");
        }
        currentAppointmentLabel.setPickOnBounds(true);
        currentAppointmentLabel.setAlignment(Pos.CENTER_LEFT);

        grid.add(lastRepairTitle, 0, 5);
        grid.add(lastRepairLabel, 1, 5);
        grid.add(appointmentTitle, 0, 6);
        grid.add(currentAppointmentLabel, 1, 6);

        // Кнопки
        Button saveBtn = new Button("\uD83D\uDCCA Сохранить");
        saveBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-background-radius: 5; -fx-padding: 10 25 10 25;");
        Button cancelBtn = new Button("\u2715 Отмена");
        cancelBtn.setStyle("-fx-background-color: #F5F5F5; -fx-text-fill: #666; -fx-font-weight: bold; " +
            "-fx-background-radius: 5; -fx-padding: 10 25 10 25; -fx-border-color: #ddd; -fx-border-radius: 5;");
        
        HBox btnBox = new HBox(15, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(20, 0, 0, 0));

        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
            if (nameField.getText().trim().isEmpty()) {
                showAlert("Введите имя клиента");
                return;
            }
            if (!Validators.isValidPhone(phoneField.getText())) {
                showAlert("Неверный формат телефона. Должно быть +7XXXXXXXXXX (10 цифр)");
                return;
            }
            if (!Validators.isValidCarNumber(carNumberField.getText())) {
                showAlert("Неверный формат госномера.\nПримеры: \u0410123\u0412\u0421163, \u0410123\u0412\u042116");
                return;
            }

            client.setName(nameField.getText().trim());
            client.setLastName(lastNameField.getText().trim());
            client.setPhone(phoneField.getText());
            client.setCarModel(carModelComboBox.getValue() != null ? carModelComboBox.getValue() : carModelComboBox.getEditor().getText());
            client.setCarNumber(carNumberField.getText());

            ClientController.updateClient(client);
            stage.close();
        });

        cancelBtn.setOnAction(e -> stage.close());

        stage.showAndWait();
    }
    
    private static String getTextFieldStyle() {
        return "-fx-background-radius: 5; -fx-padding: 8 12 8 12; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-color: white;";
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }
}
