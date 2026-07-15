package com.autoservice.dialogs;

import com.autoservice.DataStore;
import com.autoservice.Service;
import com.autoservice.controllers.ServicePanelController;
import com.autoservice.utils.TooltipHelper;
import com.autoservice.utils.ValidationErrorIndicator;
import com.autoservice.utils.ValidationUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Диалог добавления и редактирования услуги.
 */
public class EditServiceDialog {

    public static void showAdd() {
        Service emptyService = new Service();
        emptyService.setId(-1);
        showEditDialog(emptyService, true);
    }

    public static void showEdit(Service service) {
        showEditDialog(service, false);
    }

    private static void showEditDialog(Service service, boolean isAdd) {
        Stage stage = new Stage();
        stage.setTitle(isAdd ? "Добавление услуги" : "Редактирование услуги");
        stage.setMinWidth(500);
        stage.setMinHeight(350);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("dialog-root");

        Label titleLabel = new Label(isAdd ? "Добавление услуги" : "Редактирование услуги");
        titleLabel.getStyleClass().add("dialog-title");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));

        TextField nameField = new TextField(isAdd ? "" : service.getName());
        nameField.setPrefWidth(250);

        TextField priceField = new TextField(isAdd ? "" : String.valueOf(service.getPrice()));
        priceField.setPrefWidth(150);
        TooltipHelper.setToolTip(priceField, "Обязательное поле, только цифры и десятичная точка");
        
        // Валидация для поля Цена: только цифры и точка
        priceField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !newVal.matches("[0-9.]*")) {
                priceField.setText(newVal.replaceAll("[^0-9.]", ""));
            }
        });

        TextField durationField = new TextField(isAdd ? "" : String.valueOf(service.getDuration()));
        durationField.setPrefWidth(150);
        TooltipHelper.setToolTip(durationField, "Обязательное поле, только цифры");
        
        // Валидация для поля Длительность: только цифры
        durationField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !newVal.matches("[0-9]*")) {
                durationField.setText(newVal.replaceAll("[^0-9]", ""));
            }
        });

        TextField partNumberField = new TextField(isAdd ? "" : service.getPartNumber());
        partNumberField.setPrefWidth(200);
        TooltipHelper.setToolTip(partNumberField, "Только латиница (CAPS), цифры и дефис. Пробелы заменяются на дефисы");
        
        // Валидация для поля Артикул: латиница (CAPS), цифры, дефис
        partNumberField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Заменяем пробелы на дефисы
            String result = newVal.replace(" ", "-");
            // Оставляем только латинские буквы (верхний регистр), цифры и дефисы
            result = result.replaceAll("[^A-Z0-9-]", "");
            // Конвертируем в верхний регистр
            result = result.toUpperCase();
            partNumberField.setText(result);
        });

        grid.add(new Label("Название:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Цена:"), 0, 1);
        grid.add(priceField, 1, 1);
        grid.add(new Label("Длительность (мин):"), 0, 2);
        grid.add(durationField, 1, 2);
        grid.add(new Label("Артикул:"), 0, 3);
        grid.add(partNumberField, 1, 3);

        Button saveBtn = new Button("Сохранить");
        saveBtn.getStyleClass().add("save-button");

        Button cancelBtn = new Button("Отмена");
        cancelBtn.getStyleClass().add("cancel-button");

        HBox btnBox = new HBox(15, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);
        root.getChildren().addAll(titleLabel, grid, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
            ValidationErrorIndicator.clearAllErrors(grid);
            
            boolean isValid = true;
            String name = nameField.getText().trim();
            String priceText = priceField.getText().trim();
            String durationText = durationField.getText().trim();
            String partNumber = partNumberField.getText().trim();
            
            if (!ValidationUtils.isNotBlank(name, "Название")) {
                ValidationErrorIndicator.showError(nameField, "Название услуги обязательно для заполнения");
                isValid = false;
            }
            
            Double price = null;
            try {
                price = priceText.isEmpty() ? null : Double.parseDouble(priceText);
            } catch (NumberFormatException ex) {
                price = null;
            }
            if (price == null || !ValidationUtils.isNonNegativeDouble(price, "Цена")) {
                ValidationErrorIndicator.showError(priceField, "Цена должна быть положительным числом");
                isValid = false;
            }
            
            Integer duration = null;
            try {
                duration = durationText.isEmpty() ? null : Integer.parseInt(durationText);
            } catch (NumberFormatException ex) {
                duration = null;
            }
            if (duration == null || !ValidationUtils.isNonNegativeInteger(duration, "Длительность")) {
                ValidationErrorIndicator.showError(durationField, "Длительность должна быть положительным числом");
                isValid = false;
            }
            
            if (!partNumber.isEmpty()) {
                // Проверяем, что артикул соответствует формату (латиница, цифры, дефис)
                if (!partNumber.matches("[A-Z0-9-]+")) {
                    ValidationErrorIndicator.showError(partNumberField, "Артикул должен содержать только латинские буквы (CAPS), цифры и дефис");
                    isValid = false;
                }
            }
            
            if (!isValid) {
                return;
            }

            service.setName(name);
            service.setPrice(price);
            service.setDuration(duration);
            service.setPartNumber(partNumber);
            
            if (isAdd) {
                ServicePanelController.addService(service);
            } else {
                ServicePanelController.updateService(service);
            }
            
            stage.close();
        });

        cancelBtn.setOnAction(e -> stage.close());
        stage.showAndWait();
    }
}
