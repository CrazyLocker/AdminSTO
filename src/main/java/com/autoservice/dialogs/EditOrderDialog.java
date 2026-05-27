package com.autoservice.dialogs;

import com.autoservice.*;
import com.autoservice.controllers.OrderController;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;
import com.autoservice.controllers.DictionaryController;

public class EditOrderDialog {

    public static void show(WorkOrder order) {
        // Проверка: нельзя редактировать закрытый или выданный заказ
        if (order.getStatus().equals(WorkOrder.STATUS_CLOSED) ||
                order.getStatus().equals(WorkOrder.STATUS_COMPLETED)) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Нельзя редактировать закрытый или выданный заказ", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        Stage stage = new Stage();
        stage.setTitle("Редактирование заказа №" + order.getId());
        stage.setMinWidth(650);
        stage.setMinHeight(600);
        stage.initModality(javafx.stage.Modality.WINDOW_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label infoLabel = new Label("Клиент: " + order.getClient().getName() + " (" + order.getClient().getCarModel() + ")");
        infoLabel.setStyle("-fx-font-weight: bold;");

        ListView<String> servicesListView = new ListView<>();
        servicesListView.setPrefHeight(120);
        ListView<String> partsListView = new ListView<>();
        partsListView.setPrefHeight(120);

        List<String> tempServices = new ArrayList<>();
        List<Double> tempServicePrices = new ArrayList<>();
        List<SparePart> tempParts = new ArrayList<>();
        List<Integer> tempPartQuantities = new ArrayList<>();

        tempServices.addAll(order.getServices());
        tempServicePrices.addAll(order.getServicePrices());
        for (int i = 0; i < tempServices.size(); i++) {
            servicesListView.getItems().add((i+1) + ". " + tempServices.get(i) + " — " + tempServicePrices.get(i) + " руб.");
        }

        tempParts.addAll(order.getSpareParts());
        tempPartQuantities.addAll(order.getSparePartQuantities());
        for (int i = 0; i < tempParts.size(); i++) {
            SparePart p = tempParts.get(i);
            int q = tempPartQuantities.get(i);
            partsListView.getItems().add((i+1) + ". " + p.getName() + " — " + p.getRetailPrice() + " руб. x " + q + " = " + (p.getRetailPrice() * q) + " руб.");
        }

        ComboBox<Service> serviceCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getServices()));
        serviceCombo.setPromptText("Выберите услугу");
        serviceCombo.setPrefWidth(300);
        TextField servicePriceField = new TextField();
        servicePriceField.setEditable(false);
        servicePriceField.setPrefWidth(100);

        serviceCombo.setOnAction(e -> {
            Service selected = serviceCombo.getValue();
            if (selected != null) {
                servicePriceField.setText(String.valueOf(selected.getPrice()));
            }
        });

        Button addServiceBtn = new Button("+ Добавить услугу");
        HBox serviceAddBox = new HBox(10, serviceCombo, servicePriceField, addServiceBtn);

        ComboBox<SparePart> partCombo = new ComboBox<>(FXCollections.observableArrayList(DataStore.getSpareParts()));
        partCombo.setPromptText("Выберите запчасть");
        partCombo.setPrefWidth(300);
        TextField partPriceField = new TextField();
        partPriceField.setEditable(false);
        partPriceField.setPrefWidth(100);
        TextField partStockField = new TextField();
        partStockField.setEditable(false);
        partStockField.setPrefWidth(60);
        TextField partQtyField = new TextField();
        partQtyField.setText("1");
        partQtyField.setPrefWidth(60);

        partCombo.setOnAction(e -> {
            SparePart selected = partCombo.getValue();
            if (selected != null) {
                partPriceField.setText(String.valueOf(selected.getRetailPrice()));
                partStockField.setText(String.valueOf(selected.getStock()));
            }
        });

        Button addPartBtn = new Button("+ Добавить запчасть");
        HBox partAddBox = new HBox(10, partCombo, partPriceField, new Label("Остаток:"), partStockField, new Label("Кол-во:"), partQtyField, addPartBtn);

        Button removeServiceBtn = new Button("Удалить выбранную услугу");
        Button removePartBtn = new Button("Удалить выбранную запчасть");

        Label totalLabel = new Label("Итого: " + calculateTotal(tempServicePrices, tempParts, tempPartQuantities) + " руб.");
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Runnable updateTotal = () -> {
            double total = 0;
            for (Double price : tempServicePrices) total += price;
            for (int i = 0; i < tempParts.size(); i++) {
                total += tempParts.get(i).getRetailPrice() * tempPartQuantities.get(i);
            }
            totalLabel.setText("Итого: " + String.format("%.2f", total) + " руб.");
        };

        addServiceBtn.setOnAction(e -> {
            Service selected = serviceCombo.getValue();
            if (selected == null) {
                showAlert("Выберите услугу");
                return;
            }
            tempServices.add(selected.getName());
            tempServicePrices.add(selected.getPrice());
            servicesListView.getItems().add(tempServices.size() + ". " + selected.getName() + " — " + selected.getPrice() + " руб.");
            serviceCombo.setValue(null);
            servicePriceField.clear();
            updateTotal.run();
        });

        addPartBtn.setOnAction(e -> {
            SparePart selected = partCombo.getValue();
            if (selected == null) {
                showAlert("Выберите запчасть");
                return;
            }
            int qty;
            try {
                qty = Integer.parseInt(partQtyField.getText());
                if (qty <= 0) {
                    showAlert("Количество должно быть положительным");
                    return;
                }
                if (qty > selected.getStock()) {
                    showAlert("Недостаточно запчастей. Доступно: " + selected.getStock());
                    return;
                }
            } catch (NumberFormatException ex) {
                showAlert("Введите корректное количество");
                return;
            }
            tempParts.add(selected);
            tempPartQuantities.add(qty);
            selected.setStock(selected.getStock() - qty);
            partsListView.getItems().add(tempParts.size() + ". " + selected.getName() + " — " + selected.getRetailPrice() + " руб. x " + qty + " = " + (selected.getRetailPrice() * qty) + " руб.");
            partStockField.setText(String.valueOf(selected.getStock()));
            partCombo.setValue(null);
            partPriceField.clear();
            partQtyField.setText("1");
            updateTotal.run();
        });

        removeServiceBtn.setOnAction(e -> {
            int idx = servicesListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < tempServices.size()) {
                tempServices.remove(idx);
                tempServicePrices.remove(idx);
                servicesListView.getItems().clear();
                for (int i = 0; i < tempServices.size(); i++) {
                    servicesListView.getItems().add((i+1) + ". " + tempServices.get(i) + " — " + tempServicePrices.get(i) + " руб.");
                }
                updateTotal.run();
            } else {
                showAlert("Выберите услугу");
            }
        });

        removePartBtn.setOnAction(e -> {
            int idx = partsListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < tempParts.size()) {
                SparePart part = tempParts.get(idx);
                int qty = tempPartQuantities.get(idx);
                part.setStock(part.getStock() + qty);
                tempParts.remove(idx);
                tempPartQuantities.remove(idx);
                partsListView.getItems().clear();
                for (int i = 0; i < tempParts.size(); i++) {
                    SparePart p = tempParts.get(i);
                    int q = tempPartQuantities.get(i);
                    partsListView.getItems().add((i+1) + ". " + p.getName() + " — " + p.getRetailPrice() + " руб. x " + q + " = " + (p.getRetailPrice() * q) + " руб.");
                }
                partCombo.setItems(FXCollections.observableArrayList(DataStore.getSpareParts()));
                updateTotal.run();
            } else {
                showAlert("Выберите запчасть");
            }
        });

        Button saveBtn = new Button("Сохранить изменения");
        Button cancelBtn = new Button("Отмена");
        HBox btnBox = new HBox(15, saveBtn, cancelBtn);
        btnBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(
                infoLabel,
                new Separator(),
                new Label("Услуги:"), servicesListView,
                serviceAddBox, removeServiceBtn,
                new Separator(),
                new Label("Запчасти:"), partsListView,
                partAddBox, removePartBtn,
                new Separator(),
                totalLabel, btnBox
        );

        Scene scene = new Scene(root);
        stage.setScene(scene);

        saveBtn.setOnAction(e -> {
            if (tempServices.isEmpty() && tempParts.isEmpty()) {
                showAlert("Должна быть хотя бы одна услуга или запчасть");
                return;
            }

            while (order.getServices().size() > 0) {
                order.removeService(0);
            }
            while (order.getSpareParts().size() > 0) {
                order.removeSparePart(0);
            }

            for (int i = 0; i < tempServices.size(); i++) {
                order.addService(tempServices.get(i), tempServicePrices.get(i));
            }
            for (int i = 0; i < tempParts.size(); i++) {
                order.addSparePart(tempParts.get(i), tempPartQuantities.get(i));
                DataStore.updateSparePartStock(tempParts.get(i), tempParts.get(i).getStock());
            }

            DataStore.updateOrder(order);
            OrderController.refreshTable();
            DictionaryController.refreshAll();
            stage.close();
        });

        cancelBtn.setOnAction(e -> {
            for (int i = 0; i < tempParts.size(); i++) {
                tempParts.get(i).setStock(tempParts.get(i).getStock() + tempPartQuantities.get(i));
            }
            stage.close();
        });

        stage.showAndWait();
    }

    private static double calculateTotal(List<Double> prices, List<SparePart> parts, List<Integer> quantities) {
        double total = 0;
        for (Double p : prices) total += p;
        for (int i = 0; i < parts.size(); i++) {
            total += parts.get(i).getRetailPrice() * quantities.get(i);
        }
        return total;
    }

    private static void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.showAndWait();
    }
}