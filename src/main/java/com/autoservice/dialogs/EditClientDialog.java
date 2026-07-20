package com.autoservice.dialogs;

import com.autoservice.AppConstants;
import com.autoservice.Appointment;
import com.autoservice.Client;
import com.autoservice.DataStore;
import com.autoservice.DateUtils;
import com.autoservice.Validators;
import com.autoservice.controllers.ClientController;
import com.autoservice.services.WindowStateManager;
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
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;

public class EditClientDialog {

    // GWM_MODELS перенесены в AppConstants

    /**
     * Асинхронное открытие диалога (для тестирования).
     * Возвращает CompletableFuture, который завершается при закрытии диалога.
     */
    public static CompletableFuture<DialogResult> showAsync(Client client) {
        CompletableFuture<DialogResult> future = new CompletableFuture<>();
        boolean isNew = (client.getId() == -1 || client.getId() == 0);

        Stage stage = new Stage();
        stage.setTitle(isNew ? "Новый клиент" : "Редактирование клиента");
        stage.setMinWidth(550);
        stage.setMinHeight(550);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        // Восстановление состояния диалога
        WindowStateManager.getInstance().restoreWindowState("editClientDialog", stage);

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
        lastNameField.setId("lastNameField");
        lastNameField.setPromptText("Фамилия");
        lastNameField.setPrefWidth(250);
        Validators.setupLastNameField(lastNameField);

        // Имя
        Label nameLabel = new Label("Имя:");
        nameLabel.getStyleClass().add("label");
        TextField nameField = new TextField(client.getName());
        nameField.setId("nameField");
        nameField.setPromptText("Имя");
        nameField.setPrefWidth(250);
        Validators.setupNameField(nameField);

        // Телефон
        Label phoneLabel = new Label("Телефон:");
        phoneLabel.getStyleClass().add("label");
        TextField phoneField = new TextField(client.getPhone());
        phoneField.setId("phoneField");
        phoneField.setPromptText("Телефон");
        phoneField.setPrefWidth(250);
        Validators.setupPhoneField(phoneField);

        // Модель
        Label carModelLabel = new Label("Марка/Модель:");
        carModelLabel.getStyleClass().add("label");
        ComboBox<String> carModelComboBox = new ComboBox<>(FXCollections.observableArrayList(AppConstants.GWM_MODELS));
        carModelComboBox.setPromptText("Модель");
        carModelComboBox.setEditable(true);
        carModelComboBox.setPrefWidth(250);
        carModelComboBox.getEditor().setText(client.getCarModel());

        // Госномер
        Label carNumberLabel = new Label("Госномер:");
        carNumberLabel.getStyleClass().add("label");
        TextField carNumberField = new TextField(client.getCarNumber());
        carNumberField.setId("carNumberField");
        carNumberField.setPromptText("Госномер");
        carNumberField.setPrefWidth(250);
        Validators.setupCarNumberField(carNumberField);

        // Кнопки
        Button saveBtn = new Button("Сохранить");
        saveBtn.getStyleClass().add("save-btn");
        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("cancel-btn");

        HBox btnBox = new HBox(10, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        // Валидация и сохранение
        Runnable doSave = () -> {
            boolean isValid = true;

            if (!ValidationUtils.isValidPhone(phoneField.getText())) {
                ValidationErrorIndicator.showError(phoneField, "Неверный формат телефона");
                isValid = false;
            }

            // Валидация фамилии: только кириллица, минимум 2 буквы
            String lastName = lastNameField.getText().trim();
            if (lastName.isEmpty()) {
                ValidationErrorIndicator.showError(lastNameField, "Введите фамилию (минимум 2 буквы)");
                isValid = false;
            } else if (!lastName.matches("^[\u0400-\u04FF]{2,}$")) {
                ValidationErrorIndicator.showError(lastNameField, "Фамилия должна содержать только русские буквы (минимум 2)");
                isValid = false;
            }

            // Валидация имени: только кириллица, минимум 2 буквы
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                ValidationErrorIndicator.showError(nameField, "Введите имя (минимум 2 буквы)");
                isValid = false;
            } else if (!name.matches("^[\u0400-\u04FF]{2,}$")) {
                ValidationErrorIndicator.showError(nameField, "Имя должно содержать только русские буквы (минимум 2)");
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
            future.complete(new DialogResult(DialogResult.Action.OK, Map.of("client", client)));
        };

        saveBtn.setOnAction(e -> doSave.run());
        cancelBtn.setOnAction(e -> {
            stage.close();
            future.complete(new DialogResult(DialogResult.Action.CANCEL));
        });

        GridPane.setConstraints(lastNameLabel, 0, 0);
        GridPane.setConstraints(lastNameField, 1, 0);
        GridPane.setConstraints(nameLabel, 0, 1);
        GridPane.setConstraints(nameField, 1, 1);
        GridPane.setConstraints(phoneLabel, 0, 2);
        GridPane.setConstraints(phoneField, 1, 2);
        GridPane.setConstraints(carModelLabel, 0, 3);
        GridPane.setConstraints(carModelComboBox, 1, 3);
        GridPane.setConstraints(carNumberLabel, 0, 4);
        GridPane.setConstraints(carNumberField, 1, 4);

        grid.getChildren().addAll(lastNameLabel, lastNameField, nameLabel, nameField, phoneLabel, phoneField, carModelLabel, carModelComboBox, carNumberLabel, carNumberField);

        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.showAndWait(); // Используем showAndWait вместо show + join()
        return future;
    }

    public static void show(Client client) {
        // Синхронная версия для обратной совместимости
        showAsync(client).join();
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