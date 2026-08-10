package com.autoservice.views;

import com.autoservice.DataStore;
import com.autoservice.SparePart;
import com.autoservice.controllers.StockPanelController;
import com.autoservice.utils.TooltipHelper;
import com.autoservice.utils.ValidationErrorIndicator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Диалог выбора операции со складом (приход/списание).
 */
public class StockOperationDialog {

    /**
     * Показывает диалог операции со складом.
     * @param part запчасть для операции
     * @param defaultOperationType тип операции по умолчанию ("ПРИХОД" или "СПИСАНИЕ")
     */
    public static void show(SparePart part, String defaultOperationType) {
        Stage stage = new Stage();
        stage.setTitle("Операция со складом: " + part.getName());
        stage.setMinWidth(450);
        stage.setMinHeight(300);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-font-size: 12px;");

        // Заголовок
        Label titleLabel = new Label("Запчасть: " + part.getName());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Текущий остаток
        Label stockLabel = new Label(String.format(
            "Текущий остаток: %s (доступно: %s, зарезервировано: %s)",
            part.getStockFormatted(),
            String.format("%.0f %s", part.getAvailableStock(), part.getUnitType()),
            String.format("%.0f %s", part.getReserved(), part.getUnitType())
        ));
        stockLabel.setStyle("-fx-text-fill: #9CA3AF;");

        // Радиокнопки для выбора типа операции
        ToggleGroup operationGroup = new ToggleGroup();
        RadioButton incomeRadio = new RadioButton("Приход (+) — увеличить остаток");
        RadioButton expenseRadio = new RadioButton("Списание (-) — уменьшить остаток");
        incomeRadio.setToggleGroup(operationGroup);
        expenseRadio.setToggleGroup(operationGroup);
        
        // Устанавливаем тип по умолчанию
        if ("СПИСАНИЕ".equalsIgnoreCase(defaultOperationType)) {
            expenseRadio.setSelected(true);
        } else {
            incomeRadio.setSelected(true);
        }
        
        // Блокируем списание, если остатка нет
        if (part.getStock() <= 0) {
            expenseRadio.setDisable(true);
            expenseRadio.setText("Списание (-) — НЕДОСТУПНО (остаток 0)");
        }

        VBox radioBox = new VBox(10, incomeRadio, expenseRadio);
        radioBox.setStyle("-fx-padding: 10; -fx-border-color: #3B82F6; -fx-border-radius: 8; -fx-background-color: #1E2139;");

        // Поле для ввода количества
        TextField amountField = new TextField();
        amountField.setPromptText("Количество");
        amountField.setPrefWidth(150);
        amountField.setPrefHeight(30);
        
        // Валидация ввода
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !newVal.matches("[0-9]*\\.?[0-9]*")) {
                amountField.setStyle("-fx-border-color: red;");
            } else {
                amountField.setStyle("");
            }
        });

        TooltipHelper.setToolTip(amountField, "Введите количество для операции");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));
        grid.add(new Label("Количество:"), 0, 0);
        grid.add(amountField, 1, 0);

        // Предупреждение о текущем остатке
        Label warningLabel = new Label();
        warningLabel.setStyle("-fx-text-fill: #FB923C; -fx-font-size: 11px;");
        
        expenseRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && part.getStock() > 0) {
                warningLabel.setText(String.format("⚠️ Доступно для списания: %.0f %s", part.getStock(), part.getUnitType()));
            } else if (!newVal) {
                warningLabel.setText("");
            }
        });

        // Кнопки
        Button applyBtn = new Button("Применить");
        applyBtn.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-padding: 8 20;");
        
        Button cancelBtn = new Button("Отмена");
        cancelBtn.setStyle("-fx-background-color: #6B7280; -fx-text-fill: white; -fx-padding: 8 20;");

        HBox btnBox = new HBox(10, applyBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(titleLabel, stockLabel, radioBox, grid, warningLabel, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        applyBtn.setOnAction(e -> {
            ValidationErrorIndicator.clearAllErrors(root);
            
            String amountText = amountField.getText().trim();
            if (amountText.isEmpty()) {
                ValidationErrorIndicator.showError(amountField, "Введите количество");
                return;
            }
            
            double amount;
            try {
                amount = Double.parseDouble(amountText);
            } catch (NumberFormatException ex) {
                ValidationErrorIndicator.showError(amountField, "Некорректное число");
                return;
            }
            
            if (amount <= 0) {
                ValidationErrorIndicator.showError(amountField, "Количество должно быть больше нуля");
                return;
            }
            
            boolean isIncome = incomeRadio.isSelected();
            
            if (isIncome) {
                // Приход
                double newStock = part.getStock() + amount;
                part.setStock(newStock);
                DataStore.updateSparePart(part);
                StockPanelController.refreshTable();
                DashboardView.refresh();
                
                showAlert(String.format("Приход выполнен: %s +%.0f = %.0f %s", 
                    part.getName(), amount, newStock, part.getUnitType()), 
                    Alert.AlertType.INFORMATION);
            } else {
                // Списание
                if (amount > part.getStock()) {
                    ValidationErrorIndicator.showError(amountField, 
                        String.format("Недостаточно запчастей! Доступно: %.0f %s", 
                            part.getStock(), part.getUnitType()));
                    return;
                }
                
                double newStock = part.getStock() - amount;
                part.setStock(newStock);
                DataStore.updateSparePart(part);
                StockPanelController.refreshTable();
                DashboardView.refresh();
                
                showAlert(String.format("Списание выполнено: %s -%.0f = %.0f %s", 
                    part.getName(), amount, newStock, part.getUnitType()), 
                    Alert.AlertType.INFORMATION);
            }
            
            stage.close();
        });

        cancelBtn.setOnAction(e -> stage.close());
        stage.showAndWait();
    }

    private static void showAlert(String msg, Alert.AlertType type) {
        Alert alert = new Alert(type, msg, ButtonType.OK);
        alert.showAndWait();
    }
}
