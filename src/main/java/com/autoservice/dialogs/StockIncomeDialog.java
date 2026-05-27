package com.autoservice.dialogs;

import com.autoservice.SparePart;
import com.autoservice.controllers.DictionaryController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class StockIncomeDialog {

    public static void show(SparePart part) {
        Stage stage = new Stage();
        stage.setTitle("Приход запчасти");
        stage.setMinWidth(350);
        stage.setMinHeight(200);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label infoLabel = new Label(part.getName() + "\nТекущий остаток: " + part.getStock());
        infoLabel.setStyle("-fx-font-weight: bold;");

        Label amountLabel = new Label("Количество (приход):");
        TextField amountField = new TextField();
        amountField.setPromptText("Введите количество");

        Button saveBtn = new Button("Оприходовать");
        Button cancelBtn = new Button("Отмена");
        HBox btnBox = new HBox(15, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(infoLabel, amountLabel, amountField, btnBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
            try {
                int amount = Integer.parseInt(amountField.getText());
                if (amount <= 0) {
                    showAlert("Количество должно быть положительным");
                    return;
                }
                DictionaryController.incomeSparePart(part, amount);
                stage.close();
            } catch (NumberFormatException ex) {
                showAlert("Введите корректное количество");
            }
        });

        cancelBtn.setOnAction(e -> stage.close());

        stage.showAndWait();
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }
}