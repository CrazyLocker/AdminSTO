package com.autoservice.dialogs;

import com.autoservice.DateUtils;
import com.autoservice.SparePart;
import com.autoservice.Validators;
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

        Label headerLabel = new Label(order.getClient().getName() + " | " + Validators.formatPhoneForDisplay(order.getClient().getPhone()));

        Label statusLabel = new Label("Статус: " + order.getStatus());
        // Автомобиль: приоритет — автомобиль из заказа, иначе автомобили клиента
        String carDisplay;
        if (order.getCarModel() != null && !order.getCarModel().isEmpty()) {
            carDisplay = order.getCarModel() + (order.getCarNumber() != null && !order.getCarNumber().isEmpty() ? " (" + order.getCarNumber() + ")" : "");
        } else if (order.getClient() != null) {
            carDisplay = order.getClient().getCarDisplay();
        } else {
            carDisplay = "";
        }
        Label carLabel = new Label("Авто: " + carDisplay);

        Label servicesLabel = new Label("Услуги:");

        ListView<String> servicesList = new ListView<>();
        for (int i = 0; i < order.getServices().size(); i++) {
            servicesList.getItems().add((i+1) + ". " + order.getServices().get(i) + " — " + order.getServicePrices().get(i) + " руб.");
        }
        servicesList.setPrefHeight(100);

        Label partsLabel = new Label("Запчасти:");

        ListView<String> partsList = new ListView<>();
        for (int i = 0; i < order.getSpareParts().size(); i++) {
            SparePart part = order.getSpareParts().get(i);
            double qty = order.getSparePartQuantities().get(i);
            partsList.getItems().add((i+1) + ". " + part.getName() + " — " + part.getRetailPrice() + " руб. x " + (int)qty + " = " + (part.getRetailPrice() * qty) + " руб.");
        }
        partsList.setPrefHeight(100);

        Label totalLabel = new Label("Итого: " + order.getTotal() + " руб.");

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