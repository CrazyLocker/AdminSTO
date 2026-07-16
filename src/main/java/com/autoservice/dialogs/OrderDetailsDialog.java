package com.autoservice.dialogs;

import com.autoservice.DateUtils;
import com.autoservice.SparePart;
import com.autoservice.WorkOrder;
import com.autoservice.services.WindowStateManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class OrderDetailsDialog {

    public static void show(WorkOrder order) {
        Stage stage = new Stage();
        stage.setTitle("Заказ " + order.getId());
        stage.setMinWidth(500);
        stage.setMinHeight(500);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
        
        // Восстановление состояния диалога
        WindowStateManager.getInstance().restoreWindowState("orderDetailsDialog", stage);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label headerLabel = new Label(order.getClient().getName() + " | " + order.getClient().getPhone());
        headerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label statusLabel = new Label("Статус: " + order.getStatus());
        Label carLabel = new Label("Авто: " + order.getClient().getCarModel() + " (" + order.getClient().getCarNumber() + ")");

        Label servicesLabel = new Label("Услуги:");
        servicesLabel.setStyle("-fx-font-weight: bold;");

        ListView<String> servicesList = new ListView<>();
        for (int i = 0; i < order.getServices().size(); i++) {
            servicesList.getItems().add((i+1) + ". " + order.getServices().get(i) + " — " + order.getServicePrices().get(i) + " руб.");
        }
        servicesList.setPrefHeight(100);

        Label partsLabel = new Label("Запчасти:");
        partsLabel.setStyle("-fx-font-weight: bold;");

        ListView<String> partsList = new ListView<>();
        for (int i = 0; i < order.getSpareParts().size(); i++) {
            SparePart part = order.getSpareParts().get(i);
            double qty = order.getSparePartQuantities().get(i);
            partsList.getItems().add((i+1) + ". " + part.getName() + " — " + part.getRetailPrice() + " руб. x " + (int)qty + " = " + (part.getRetailPrice() * qty) + " руб.");
        }
        partsList.setPrefHeight(100);

        Label totalLabel = new Label("Итого: " + order.getTotal() + " руб.");
        totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Button closeBtn = new Button("Закрыть");
        HBox btnBox = new HBox(15, closeBtn);
        btnBox.setAlignment(Pos.CENTER);
        
        closeBtn.setOnAction(e -> stage.close());
        
        stage.setOnHiding(e -> {
            WindowStateManager.getInstance().saveWindowState("orderDetailsDialog", stage);
        });

        content.getChildren().addAll(headerLabel, statusLabel, carLabel,
                servicesLabel, servicesList,
                partsLabel, partsList,
                totalLabel, btnBox);

        Scene scene = new Scene(content);
        stage.setScene(scene);
        closeBtn.setOnAction(e -> stage.close());
        stage.showAndWait();
    }
}