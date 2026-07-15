package com.autoservice.dialogs;

import com.autoservice.Appointment;
import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.DateUtils;
import com.autoservice.Validators;
import com.autoservice.controllers.ClientController;
import com.autoservice.utils.ValidationErrorIndicator;
import com.autoservice.utils.ValidationUtils;
import com.autoservice.utils.TooltipHelper;
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

    private static final List<String> GWM_MODELS = Arrays.asList(
            "Haval Jolion", "Haval F7", "Haval F7x", "Haval Dargo", "Haval Big Dog",
            "Haval H6", "Haval H9", "GWM Poer", "GWM Tank 300", "GWM Tank 500",
            "GWM Wingle 7", "GWM Cannon", "Great Wall Poer"
    );

    public static void show(Client client) {
        boolean isNew = (client.getId() == -1 || client.getId() == 0);

        Stage stage = new Stage();
        stage.setTitle(isNew ? "Новый клиент" : "Редактирование клиента");
        stage.setMinWidth(550);
        stage.setMinHeight(550);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label(isNew ? "Новый клиент" : "Редактирование клиента");
        titleLabel.getStyleClass().add("dialog-title");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));
        grid.getStyleClass().add("dialog-grid");

        // Фамилия
        Label lastNameLabel = new Label("Фамилия:");
        lastNameLabel.getStyleClass().add("label");
        TextField lastNameField = new TextField(client.getLastName());
        lastNameField.setPromptText("Фамилия");
        lastNameField.setPrefWidth(250);

        // Имя
        Label nameLabel = new Label("Имя:");
        nameLabel.getStyleClass().add("label");
        TextField nameField = new TextField(client.getName());
        nameField.setPromptText("Имя");
        nameField.setPrefWidth(250);

        // Телефон
        Label phoneLabel = new Label("Телефон:");
        phoneLabel.getStyleClass().add("label");
        TextField phoneField = new TextField(client.getPhone());
        phoneField.setPromptText("+7XXXXXXXXXX");
        phoneField.setPrefWidth(250);
        TooltipHelper.setToolTip(phoneField, "Формат: +7XXXXXXXXXX (10 цифр)");
        // Настройка поля телефона с автоматической фильтрацией
        Validators.setupPhoneField(phoneField);

        // Модель авто
        Label carModelLabel = new Label("Модель авто:");
        carModelLabel.getStyleClass().add("label");
        ComboBox<String> carModelComboBox = new ComboBox<>();
        carModelComboBox.setPromptText("Выберите или введите модель");
        carModelComboBox.setPrefWidth(300);
        carModelComboBox.setItems(FXCollections.observableArrayList(GWM_MODELS));
        carModelComboBox.setEditable(true);
        if (client.getCarModel() != null && !client.getCarModel().isEmpty()) {
            carModelComboBox.setValue(client.getCarModel());
        }

        // Госномер
        Label carNumberLabel = new Label("Госномер:");
        carNumberLabel.getStyleClass().add("label");
        TextField carNumberField = new TextField(client.getCarNumber());
        carNumberField.setPromptText("А123ВВ777");
        carNumberField.setPrefWidth(200);
        TooltipHelper.setToolTip(carNumberField, "Формат: А123ВВ777 или А123ВВ77");
        // Настройка поля госномера с автоматической фильтрацией
        Validators.setupCarNumberField(carNumberField);

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

        // Информация о последнем ремонте (только для существующих клиентов)
        if (!isNew) {
            Label lastRepairTitle = new Label("Последний ремонт:");
            lastRepairTitle.getStyleClass().add("label");
            Label lastRepairLabel = new Label();
            if (client.getLastRepairDate() != null && !client.getLastRepairDate().isEmpty()) {
                lastRepairLabel.setText(DateUtils.formatDate(client.getLastRepairDate()));
                lastRepairLabel.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold; -fx-background-color: #E8F5E9; -fx-padding: 5 10; -fx-background-radius: 4;");
            } else {
                lastRepairLabel.setText("Нет завершённых заказов");
                lastRepairLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold; -fx-background-color: #FFF3E0; -fx-padding: 5 10; -fx-background-radius: 4;");
            }

            grid.add(lastRepairTitle, 0, 5);
            grid.add(lastRepairLabel, 1, 5);

            // Информация о текущей записи
            Label appointmentTitle = new Label("Текущая запись:");
            appointmentTitle.getStyleClass().add("label");
            Label currentAppointmentLabel = new Label();

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
                currentAppointmentLabel.setStyle("-fx-text-fill: #2E7D32; -fx-background-color: #E3F2FD; -fx-padding: 5 10; -fx-background-radius: 4;");
            } else {
                currentAppointmentLabel.setText("Нет активных записей");
                currentAppointmentLabel.setStyle("-fx-text-fill: #FF9800; -fx-background-color: #FFF3E0; -fx-padding: 5 10; -fx-background-radius: 4;");
            }

            grid.add(appointmentTitle, 0, 6);
            grid.add(currentAppointmentLabel, 1, 6);
        }

        // ====== КНОПКИ БЕЗ ИКОНОК ======
        Button saveBtn = new Button(isNew ? "Создать" : "Сохранить");
        saveBtn.getStyleClass().add("save-button");

        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("cancel-button");

        HBox btnBox = new HBox(15, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(20, 0, 0, 0));

        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                EditClientDialog.class.getResource("/styles.css").toExternalForm()
        );
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
            // Очистка ошибок валидации
            ValidationErrorIndicator.clearAllErrors(root);
            
            boolean isValid = true;
            
            // Валидация обязательных полей
            if (!ValidationUtils.isNotBlank(nameField.getText(), "Имя")) {
                ValidationErrorIndicator.showError(nameField, "Имя обязательно для заполнения");
                isValid = false;
            }
            
            if (!ValidationUtils.isValidPhone(phoneField.getText())) {
                ValidationErrorIndicator.showError(phoneField, "Неверный формат телефона");
                isValid = false;
            }
            
            if (!ValidationUtils.isValidCarNumber(carNumberField.getText())) {
                ValidationErrorIndicator.showError(carNumberField, "Неверный формат госномера");
                isValid = false;
            }
            
            if (!isValid) {
                return;
            }

            client.setName(nameField.getText().trim());
            client.setLastName(lastNameField.getText().trim());
            client.setPhone(phoneField.getText());
            client.setCarModel(carModelComboBox.getValue() != null ? carModelComboBox.getValue() : carModelComboBox.getEditor().getText());
            client.setCarNumber(carNumberField.getText().trim().toUpperCase());

            if (isNew) {
                ClientController.addClient(client);
            } else {
                ClientController.updateClient(client);
            }

            stage.close();
            showInfo("Клиент " + (isNew ? "создан" : "сохранён"));
        });

        cancelBtn.setOnAction(e -> stage.close());

        stage.showAndWait();
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }

    private static void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.showAndWait();
    }
}